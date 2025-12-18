package com.cinematch.backend.dto.post;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostAuthorDto {
    private Long id;
    private String username;
}
