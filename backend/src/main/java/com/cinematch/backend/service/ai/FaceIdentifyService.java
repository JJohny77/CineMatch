package com.cinematch.backend.service.ai;

import com.cinematch.backend.dto.FaceMatchResponse;
import com.cinematch.backend.model.ai.ActorEmbedding;
import com.cinematch.backend.repository.ActorEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceIdentifyService {

    private static final int TOP_K = 5;

    private final ActorEmbeddingRepository actorEmbeddingRepository;
    private final RecastService recastService;

    public List<FaceMatchResponse> identify(MultipartFile imageFile) throws IOException {

        if (imageFile == null || imageFile.isEmpty()) {
            log.warn("[FaceIdentifyService] Empty image file received");
            return List.of();
        }

        // 1) Embedding από το ανεβασμένο πρόσωπο
        byte[] imageBytes = imageFile.getBytes();
        double[] queryEmbeddingRaw = recastService.extractEmbedding(imageBytes);
        double[] queryEmbedding = normalize(queryEmbeddingRaw);

        // 2) Όλα τα embeddings από DB
        List<ActorEmbedding> allActors = actorEmbeddingRepository.findAll();
        log.info("[FaceIdentifyService] Found {} actor embeddings in DB", allActors.size());

        if (allActors.isEmpty()) {
            return List.of();
        }

        // 3) Similarity με όλους
        List<FaceMatchResponse> matches = new ArrayList<>();

        for (ActorEmbedding actorEmbedding : allActors) {

            double[] actorVectorRaw = parseEmbeddingJson(actorEmbedding.getEmbeddingJson());
            if (actorVectorRaw.length == 0) {
                continue;
            }

            double[] actorVector = normalize(actorVectorRaw);

            double sim = cosineSimilarity(queryEmbedding, actorVector);

            FaceMatchResponse dto = new FaceMatchResponse(
                    actorEmbedding.getName(),
                    sim,
                    actorEmbedding.getActorId(),
                    actorEmbedding.getImageUrl()
            );

            matches.add(dto);
        }

        // 4) Sort desc & top-k
        return matches.stream()
                .sorted(Comparator.comparingDouble(FaceMatchResponse::getSimilarity).reversed())
                .limit(TOP_K)
                .toList();
    }

    // JSON array -> double[]
    private double[] parseEmbeddingJson(String json) {
        if (json == null || json.isBlank()) {
            return new double[0];
        }
        JSONArray arr = new JSONArray(json);
        double[] v = new double[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            v[i] = arr.getDouble(i);
        }
        return v;
    }

    // L2 normalization
    private double[] normalize(double[] v) {
        double normSq = 0.0;
        for (double x : v) {
            normSq += x * x;
        }
        if (normSq == 0.0) {
            return v;
        }
        double norm = Math.sqrt(normSq);
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = v[i] / norm;
        }
        return out;
    }

    // Cosine similarity
    private double cosineSimilarity(double[] a, double[] b) {
        int len = Math.min(a.length, b.length);
        if (len == 0) return 0.0;

        double dot = 0.0;
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
        }
        // επειδή είναι ήδη normalized, δεν χρειάζεται ξανά normA/normB
        return dot;
    }
}
