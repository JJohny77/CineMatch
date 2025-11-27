package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {

    private String username;
    private String email;
    private String role;
    private Integer quizScore;
    private String createdAt;
}
