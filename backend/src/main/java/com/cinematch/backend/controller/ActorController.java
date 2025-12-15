package com.cinematch.backend.controller;

import com.cinematch.backend.dto.ActorDetailsDto;
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
@RequestMapping("/api/actors")
@RequiredArgsConstructor
public class ActorController {

    private final TmdbService tmdbService;
    private final CurrentUserService currentUserService;
    private final UserEventService userEventService;

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

            User user = currentUserService.getCurrentUserOrNull();
            if (user != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("actorId", id);
                payload.put("source", "ACTOR_DETAILS");

                userEventService.logEvent(
                        user,
                        UserEventType.OPEN_ACTOR,
                        payload
                );
            }

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.status(404).body("Actor not found");
        }
    }
}
