package com.cinematch.backend.dto.post;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentDto {
    private Long id;
    private Long postId;
    private Long userId;

    private String text;
    private String createdAt;

    private PostAuthorDto author;
}
