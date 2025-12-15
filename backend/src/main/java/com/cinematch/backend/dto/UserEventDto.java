package com.cinematch.backend.dto;

import com.cinematch.backend.model.UserEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class UserEventDto {
    private Long id;
    private Long userId;
    private UserEventType type;
    private String payload;
    private Instant createdAt;
}
