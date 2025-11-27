package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StarPowerResponse {
    private Long actorId;
    private Integer starPower; // 0–100
}

