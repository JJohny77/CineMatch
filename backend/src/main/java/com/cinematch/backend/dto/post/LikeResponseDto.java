package com.cinematch.backend.dto.post;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponseDto {
    private boolean liked;
    private int likesCount;
}
