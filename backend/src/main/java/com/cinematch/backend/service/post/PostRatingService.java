package com.cinematch.backend.service.post;

import com.cinematch.backend.dto.post.LikeResponseDto;
import com.cinematch.backend.model.post.PostRating;
import com.cinematch.backend.repository.post.PostRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostRatingService {

    private final PostRatingRepository ratingRepository;

    // ==========================
    // TOGGLE LIKE
    // ==========================
    @Transactional
    public LikeResponseDto toggleLike(Long userId, Long postId) {

        return ratingRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    ratingRepository.delete(existing);

                    int likesCount = ratingRepository.countByPostIdAndValue(postId, 1);

                    return LikeResponseDto.builder()
                            .liked(false)
                            .likesCount(likesCount)
                            .build();
                })
                .orElseGet(() -> {

                    PostRating rating = PostRating.builder()
                            .postId(postId)
                            .userId(userId)
                            .value(1) // LIKE
                            .build();

                    ratingRepository.save(rating);

                    int likesCount = ratingRepository.countByPostIdAndValue(postId, 1);

                    return LikeResponseDto.builder()
                            .liked(true)
                            .likesCount(likesCount)
                            .build();
                });
    }

    // ==========================
    // HELPERS
    // ==========================
    public int countLikes(Long postId) {
        return ratingRepository.countByPostIdAndValue(postId, 1);
    }

    public boolean likedByUser(Long postId, Long userId) {
        return ratingRepository.existsByPostIdAndUserId(postId, userId);
    }
}
