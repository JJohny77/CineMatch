package com.cinematch.backend.controller.ai;

import com.cinematch.backend.service.ai.ActorEmbeddingService;
import com.cinematch.backend.service.ai.RecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/ai/admin")
@RequiredArgsConstructor
@Slf4j
public class AiAdminController {

    private final RecastService recastService;
    private final ActorEmbeddingService actorEmbeddingService;

    /**
     * Admin endpoint για να δημιουργείς / ενημερώνεις embedding ηθοποιού
     *
     * POST /ai/admin/actor-embedding
     *
     * form-data:
     *   actorId   (Long)
     *   name      (String)
     *   imageUrl  (String)
     *   image     (file)
     */
    @PostMapping(
            value = "/actor-embedding",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> createOrUpdateActorEmbedding(
            @RequestParam("actorId") Long actorId,
            @RequestParam("name") String name,
            @RequestParam("imageUrl") String imageUrl,
            @RequestPart("image") MultipartFile imageFile
    ) throws IOException {

        log.info("[AiAdminController] Creating/updating embedding for actor {} ({})", actorId, name);

        byte[] imageBytes = imageFile.getBytes();

        // Βγάζουμε embedding ΜΕ το ArcFace pipeline που ήδη έχεις
        double[] embedding = recastService.extractEmbedding(imageBytes);

        // Το σώζουμε κανονικά σε DB + cache
        actorEmbeddingService.saveOrUpdateEmbedding(actorId, name, imageUrl, embedding);

        log.info("[AiAdminController] Saved embedding for actor {} ({})", actorId, name);
        return ResponseEntity.ok("Saved embedding for " + name + " (" + actorId + ")");
    }
}
