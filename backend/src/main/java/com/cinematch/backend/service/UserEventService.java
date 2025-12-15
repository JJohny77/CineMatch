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

    private static final int DEDUPE_WINDOW_SECONDS = 5;
    private static final int PREFS_THROTTLE_SECONDS = 10; // ✅ μην υπολογίζεις preferences σε κάθε click
    private static final int PREFS_TOP_N = 5;

    private final UserEventRepository userEventRepository;
    private final ObjectMapper objectMapper;

    // ✅ add
    private final UserPreferenceService userPreferenceService;

    @Transactional
    public void logEvent(User user, UserEventType type, Map<String, Object> payload) {
        if (user == null) {
            log.debug("Skipping user event {} because user is null", type);
            return;
        }

        try {
            String payloadJson;
            if (payload == null || payload.isEmpty()) payloadJson = "{}";
            else payloadJson = objectMapper.writeValueAsString(payload);

            Instant threshold = Instant.now().minusSeconds(DEDUPE_WINDOW_SECONDS);

            boolean existsRecently =
                    userEventRepository.existsByUser_IdAndTypeAndPayloadAndCreatedAtAfter(
                            user.getId(),
                            type,
                            payloadJson,
                            threshold
                    );

            if (existsRecently) {
                log.debug("Skipping duplicate user event {} for user {}", type, user.getId());
                return;
            }

            UserEvent event = UserEvent.builder()
                    .user(user)
                    .type(type)
                    .payload(payloadJson)
                    .build();

            userEventRepository.save(event);

            // ✅ AUTO-COMPUTE preferences όταν υπάρχει relevant click/filter
            if (type == UserEventType.CHOOSE_FILTER
                    || type == UserEventType.OPEN_ACTOR
                    || type == UserEventType.OPEN_DIRECTOR
                    || type == UserEventType.OPEN_MOVIE) {

                Instant last = user.getPreferencesLastUpdated();
                boolean tooSoon = last != null && last.isAfter(Instant.now().minusSeconds(PREFS_THROTTLE_SECONDS));

                if (!tooSoon) {
                    userPreferenceService.computeAndPersist(user, PREFS_TOP_N);
                }
            }

        } catch (Exception e) {
            log.error("Failed to log user event {} for user {}: {}", type, user.getId(), e.getMessage(), e);
        }
    }
}
