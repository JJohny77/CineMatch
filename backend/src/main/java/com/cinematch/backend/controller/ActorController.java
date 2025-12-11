package com.cinematch.backend.controller;

import com.cinematch.backend.dto.ActorDetailsDto;
import com.cinematch.backend.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/actors")
public class ActorController {

    private final TmdbService tmdbService;

    public ActorController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    // ============================================================
    // GET /api/actors/{id}
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getActorDetails(@PathVariable Long id) {
        try {
            ActorDetailsDto dto = tmdbService.getActorDetails(id);

            if (dto == null) {
                return ResponseEntity.status(404).body("Actor not found");
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.status(404).body("Actor not found");
        }
    }
}
