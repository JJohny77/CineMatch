package com.cinematch.backend.dto.post;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostFeedDto {

    private Long id;
    private String mediaUrl;
    private String mediaType;
    private String caption;
    private LocalDateTime createdAt;

    private Long movieId;

    private PostAuthorDto author;

    private int likesCount;
    private boolean likedByMe;

    private boolean ownedByMe;

    public boolean isOwnedByMe() {
        return ownedByMe;
    }
}
