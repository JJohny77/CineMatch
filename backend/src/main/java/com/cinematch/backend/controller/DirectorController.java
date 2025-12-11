package com.cinematch.backend.controller;

import com.cinematch.backend.dto.DirectorDetailsDto;
import com.cinematch.backend.service.TmdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/directors")
public class DirectorController {

    private final TmdbService tmdbService;

    public DirectorController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    // ============================================================
    // GET /api/directors/{id}
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getDirectorDetails(@PathVariable Long id) {
        try {
            DirectorDetailsDto dto = tmdbService.getDirectorDetails(id);

            if (dto == null) {
                return ResponseEntity.status(404).body("Director not found");
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.status(404).body("Director not found");
        }
    }
}
