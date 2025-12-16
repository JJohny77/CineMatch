package com.cinematch.backend.quiz.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQuizQuestionDto {
    private String questionText;
    private List<String> options;          // exactly 4
    private Integer correctOptionIndex;    // 0..3
    private Long movieId;                 // optional
}
