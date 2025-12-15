package com.cinematch.backend.repository;

import com.cinematch.backend.model.UserEvent;
import com.cinematch.backend.model.UserEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    List<UserEvent> findTop200ByUser_IdOrderByCreatedAtDesc(Long userId);

    List<UserEvent> findTop200ByUser_IdAndTypeOrderByCreatedAtDesc(Long userId, UserEventType type);

    boolean existsByUser_IdAndTypeAndPayloadAndCreatedAtAfter(
            Long userId,
            UserEventType type,
            String payload,
            Instant createdAfter
    );

    // US54: παίρνουμε αρκετά events για scoring
    List<UserEvent> findTop2000ByUser_IdOrderByCreatedAtDesc(Long userId);
}
