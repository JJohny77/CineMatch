package com.cinematch.backend.repository;

import com.cinematch.backend.model.UserEvent;
import com.cinematch.backend.model.UserEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    // Τελευταία 200 events για έναν χρήστη
    List<UserEvent> findTop200ByUser_IdOrderByCreatedAtDesc(Long userId);

    // Τελευταία 200 events για έναν χρήστη συγκεκριμένου τύπου
    List<UserEvent> findTop200ByUser_IdAndTypeOrderByCreatedAtDesc(Long userId, UserEventType type);
}
