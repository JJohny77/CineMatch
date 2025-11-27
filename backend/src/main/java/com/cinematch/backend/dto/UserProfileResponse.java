package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private String email;
    private String username;   // <-- ΝΕΟ πεδίο
    private String role;
    private Integer quizScore;
    private String createdAt;
}
