package com.cinematch.backend.controller.ai;

import com.cinematch.backend.service.ai.ActorEmbeddingService;
import com.cinematch.backend.service.ai.RecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/ai/recast/admin")
@RequiredArgsConstructor
public class RecastAdminAddController {

    private final RecastService recastService;
    private final ActorEmbeddingService actorEmbeddingService;

    private static final String RDJ_IMAGE =
            "https://image.tmdb.org/t/p/w500/5qHNjhtjMD4YWH3UP0rm4tKwxCL.jpg";

    // Robert Downey Jr. = TMDB ID 3223
    @PostMapping("/add-rdj")
    public ResponseEntity<String> addRDJ() {
        try {
            RestTemplate rest = new RestTemplate();
            byte[] imageBytes = rest.getForObject(RDJ_IMAGE, byte[].class);

            if (imageBytes == null || imageBytes.length == 0) {
                return ResponseEntity.badRequest().body("Failed to download photo");
            }

            // 1) Extract embedding
            double[] emb = recastService.extractEmbedding(imageBytes);

            // 2) Save to DB
            actorEmbeddingService.saveOrUpdateEmbedding(
                    3223L,
                    "Robert Downey Jr.",
                    RDJ_IMAGE,
                    emb
            );

            return ResponseEntity.ok("RDJ added successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
