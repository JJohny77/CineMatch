package com.cinematch.backend.controller;

import com.cinematch.backend.dto.DirectorDetailsDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.model.UserEventType;
import com.cinematch.backend.service.CurrentUserService;
import com.cinematch.backend.service.TmdbService;
import com.cinematch.backend.service.UserEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/directors")
@RequiredArgsConstructor
public class DirectorController {

    private final TmdbService tmdbService;
    private final CurrentUserService currentUserService;
    private final UserEventService userEventService;

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

            User user = currentUserService.getCurrentUserOrNull();
            if (user != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("directorId", id);
                payload.put("source", "DIRECTOR_DETAILS");

                userEventService.logEvent(
                        user,
                        UserEventType.OPEN_DIRECTOR,
                        payload
                );
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.status(404).body("Director not found");
        }
    }
}
