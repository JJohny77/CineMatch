package com.cinematch.backend.controller;

import com.cinematch.backend.dto.UserEventDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.model.UserEvent;
import com.cinematch.backend.model.UserEventType;
import com.cinematch.backend.repository.UserEventRepository;
import com.cinematch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug/user-events")
@RequiredArgsConstructor
public class UserEventAdminController {

    private final UserEventRepository userEventRepository;
    private final UserRepository userRepository;

    // ==========================================
    // GET /debug/user-events/{userId}
    // ==========================================
    @GetMapping("/{userId}")
    public List<UserEventDto> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(required = false) UserEventType type
    ) {
        List<UserEvent> events;

        if (type == null) {
            events = userEventRepository
                    .findTop200ByUser_IdOrderByCreatedAtDesc(userId);
        } else {
            events = userEventRepository
                    .findTop200ByUser_IdAndTypeOrderByCreatedAtDesc(userId, type);
        }

        return events.stream()
                .map(e -> UserEventDto.builder()
                        .userId(e.getUser().getId())
                        .type(e.getType())
                        .payload(e.getPayload())
                        .createdAt(e.getCreatedAt())
                        .build()
                )
                .collect(Collectors.toList());
    }

    // ==========================================
    // DEBUG: γράφει ένα test event
    // GET /debug/user-events/test?userId=1
    // ==========================================
    @GetMapping("/test")
    public String createTestEvent(@RequestParam Long userId) {
        return userRepository.findById(userId)
                .map((User user) -> {
                    String payload = "{\"query\":\"test-from-debug\"}";

                    UserEvent event = UserEvent.builder()
                            .user(user)
                            .type(UserEventType.SEARCH_MOVIE)
                            .payload(payload)
                            .build();

                    userEventRepository.save(event);

                    return "OK: created test event for user " + userId;
                })
                .orElse("User " + userId + " not found");
    }
}
