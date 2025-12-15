package com.cinematch.backend.service.ai;

import com.cinematch.backend.model.ai.ActorEmbedding;
import com.cinematch.backend.repository.ActorEmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActorEmbeddingService {

    private final ActorEmbeddingRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // cache στη μνήμη
    private final Map<Long, CachedEmbedding> cache = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    public static class CachedEmbedding {
        private Long actorId;
        private String name;
        private String imageUrl;
        private double[] embedding; // L2-normalized vector
    }

    @Data
    @AllArgsConstructor
    public static class MatchResult {
        private Long actorId;
        private String name;
        private String imageUrl;
        private double similarity;
    }

    // =====================================================
    //          CACHE LOAD ΣΤΟ STARTUP  (+ RDJ DEBUG)
    // =====================================================
    @PostConstruct
    public void loadCache() {
        List<ActorEmbedding> all = repository.findAll();

        for (ActorEmbedding e : all) {
            double[] emb = parseEmbedding(e.getEmbeddingJson());

            cache.put(
                    e.getActorId(),
                    new CachedEmbedding(
                            e.getActorId(),
                            e.getName(),
                            e.getImageUrl(),
                            emb
                    )
            );
        }

        log.info("[ActorEmbeddingService] Loaded {} embeddings into cache", cache.size());

        // 🔍 EXTRA DEBUG – για RDJ
        if (cache.containsKey(3223L)) {
            log.info("[ActorEmbeddingService] RDJ (3223) FOUND in cache at startup.");
        } else {
            log.warn("[ActorEmbeddingService] RDJ (3223) NOT FOUND in cache at startup.");
        }
    }

    // Χρησιμοποιείται από το batch για να μη διπλο-σώζουμε actors
    public boolean exists(Long actorId) {
        return cache.containsKey(actorId);
    }

    public boolean hasEmbeddings() {
        return !cache.isEmpty();
    }

    /**
     * Καλείται από το batch που τραβάει actors από TMDb
     */
    public void saveOrUpdateEmbedding(Long actorId,
                                      String name,
                                      String imageUrl,
                                      double[] embedding) {

        ActorEmbedding entity = repository.findById(actorId).orElse(null);

        if (entity == null) {
            entity = new ActorEmbedding();
            entity.setActorId(actorId);
        }

        entity.setName(name);
        entity.setImageUrl(imageUrl);
        entity.setEmbeddingJson(embeddingToJson(embedding));
        repository.save(entity);

        cache.put(actorId, new CachedEmbedding(actorId, name, imageUrl, embedding));

        // 🔍 EXTRA DEBUG για RDJ
        if (actorId == 3223L) {
            log.info("[ActorEmbeddingService] RDJ (3223) saved/updated in cache.");
        }
    }

    /**
     * Βρίσκει τον καλύτερο ηθοποιό με βάση cosine similarity
     */
    public Optional<MatchResult> findBestMatch(double[] queryEmbedding) {
        if (cache.isEmpty()) {
            return Optional.empty();
        }

        normalizeInPlace(queryEmbedding);

        CachedEmbedding best = null;
        double bestScore = -1.0;

        for (CachedEmbedding candidate : cache.values()) {
            double score = cosineSimilarity(queryEmbedding, candidate.getEmbedding());

            // 🔍 DEBUG similarity για RDJ
            if (candidate.getActorId() == 3223L) {
                log.info("[DEBUG] Similarity with RDJ (3223) = {}", score);
            }

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best == null) {
            return Optional.empty();
        }

        return Optional.of(new MatchResult(
                best.getActorId(),
                best.getName(),
                best.getImageUrl(),
                bestScore
        ));
    }

    // =================== helpers ===================

    private void normalizeInPlace(double[] v) {
        double norm = 0.0;
        for (double d : v) {
            norm += d * d;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) return;
        for (int i = 0; i < v.length; i++) {
            v[i] /= norm;
        }
    }

    private double cosineSimilarity(double[] a, double[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;

        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }

        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double[] parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return new double[0];
        }
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (Exception e) {
            log.error("[ActorEmbeddingService] Failed to parse embedding JSON", e);
            return new double[0];
        }
    }

    private String embeddingToJson(double[] embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize embedding", e);
        }
    }
}
