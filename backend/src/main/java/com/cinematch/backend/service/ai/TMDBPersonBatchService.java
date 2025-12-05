package com.cinematch.backend.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class TMDBPersonBatchService {

    @Value("${TMDB_API_KEY}")
    private String tmdbApiKey;

    private final ActorEmbeddingService actorEmbeddingService;
    private final RecastService recastService;

    private final HttpClient http = HttpClient.newHttpClient();

    private static final String TMDB_URL =
            "https://api.themoviedb.org/3/person/popular?page=%d&api_key=%s";

    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/w500";

    /**
     * Κάνει FULL batch processing όλων των TMDB popular actor pages.
     */
    public String processAllPages() {

        int page = 1;
        int totalSaved = 0;

        log.info("=== Starting FULL TMDB People Batch Build ===");

        while (true) {
            try {
                log.info("Fetching TMDB page {}", page);

                String url = TMDB_URL.formatted(page, tmdbApiKey);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() != 200) {
                    log.error("TMDB error {}: {}", resp.statusCode(), resp.body());
                    break;
                }

                JSONObject json = new JSONObject(resp.body());
                JSONArray results = json.getJSONArray("results");

                if (results.isEmpty()) {
                    log.info("Page {} was empty. Stopping batch.", page);
                    break;
                }

                for (int i = 0; i < results.length(); i++) {
                    JSONObject p = results.getJSONObject(i);

                    long actorId = p.getLong("id");
                    String name = p.optString("name", "Unknown");

                    String profilePath = p.optString("profile_path", null);
                    if (profilePath == null) {
                        log.warn("Skipping {} (no photo)", name);
                        continue;
                    }

                    if (actorEmbeddingService.exists(actorId)) {
                        log.info("Skipping {} — already in DB", name);
                        continue;
                    }

                    String imageUrl = IMAGE_BASE + profilePath;

                    log.info("Downloading image for {} ({})", name, actorId);
                    byte[] imageBytes = recastService.downloadImage(imageUrl);
                    if (imageBytes == null) {
                        log.warn("Failed to download {} — skipping", name);
                        continue;
                    }

                    // Extract embedding
                    double[] emb = recastService.extractEmbedding(imageBytes);

                    // Save
                    actorEmbeddingService.saveOrUpdateEmbedding(
                            actorId,
                            name,
                            imageUrl,
                            emb
                    );

                    totalSaved++;

                    // Avoid rate limits
                    Thread.sleep(200);
                }

                page++;

            } catch (Exception e) {
                log.error("Error on page {}: {}", page, e.getMessage());
                break;
            }
        }

        log.info("=== TMDB Batch Completed. Saved total {} new actors ===", totalSaved);

        return "Batch completed. Saved " + totalSaved + " actors.";
    }
}
