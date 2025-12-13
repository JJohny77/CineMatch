package com.cinematch.backend.service;

import com.cinematch.backend.model.User;
import com.cinematch.backend.model.UserEvent;
import com.cinematch.backend.model.UserEventType;
import com.cinematch.backend.repository.UserEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventService {

    private static final int DEDUPE_WINDOW_SECONDS = 5; // παράθυρο 5"

    private final UserEventRepository userEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Βασική μέθοδος logging.
     * - Αν user == null → αγνοούμε.
     * - Αν υπάρχει ήδη ΙΔΙΟ event (user + type + payload) στα τελευταία 5" → αγνοούμε.
     */
    @Transactional
    public void logEvent(User user, UserEventType type, Map<String, Object> payload) {
        if (user == null) {
            log.debug("Skipping user event {} because user is null", type);
            return;
        }

        try {
            // 1) Serialize payload σε JSON
            String payloadJson;
            if (payload == null || payload.isEmpty()) {
                payloadJson = "{}";
            } else {
                payloadJson = objectMapper.writeValueAsString(payload);
            }

            // 2) DEDUPE: υπάρχει ήδη ίδιο event τελευταία 5" ;
            Instant threshold = Instant.now().minusSeconds(DEDUPE_WINDOW_SECONDS);

            boolean existsRecently =
                    userEventRepository.existsByUser_IdAndTypeAndPayloadAndCreatedAtAfter(
                            user.getId(),
                            type,
                            payloadJson,
                            threshold
                    );

            if (existsRecently) {
                log.debug(
                        "Skipping duplicate user event {} for user {} (same payload within {}s)",
                        type,
                        user.getId(),
                        DEDUPE_WINDOW_SECONDS
                );
                return;
            }

            // 3) Αποθήκευση event
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
