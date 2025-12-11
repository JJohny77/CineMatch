package com.cinematch.backend.service;

import com.cinematch.backend.model.User;
import com.cinematch.backend.model.UserEvent;
import com.cinematch.backend.model.UserEventType;
import com.cinematch.backend.repository.UserEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventService {

    private final UserEventRepository userEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Βασική μέθοδος logging. Αν user == null, απλά το αγνοεί.
     */
    public void logEvent(User user, UserEventType type, Map<String, Object> payload) {
        if (user == null) {
            // Αν ο χρήστης δεν είναι logged-in, δεν γράφουμε event.
            log.debug("Skipping user event {} because user is null", type);
            return;
        }

        try {
            String payloadJson;
            if (payload == null || payload.isEmpty()) {
                payloadJson = "{}";
            } else {
                payloadJson = objectMapper.writeValueAsString(payload);
            }

            UserEvent event = UserEvent.builder()
                    .user(user)
                    .type(type)
                    .payload(payloadJson)
                    .build();

            userEventRepository.save(event);
        } catch (Exception e) {
            log.error(
                    "Failed to log user event {} for user {}: {}",
                    type,
                    user.getId(),
                    e.getMessage(),
                    e
            );
        }
    }
}
