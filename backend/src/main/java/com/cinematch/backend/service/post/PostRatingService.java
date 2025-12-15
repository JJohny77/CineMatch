package com.cinematch.backend.service.post;

import com.cinematch.backend.model.post.PostRating;
import com.cinematch.backend.repository.post.PostRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostRatingService {

    private final PostRatingRepository ratingRepository;

    public PostRating ratePost(Long userId, Long postId, Integer value) {

        return ratingRepository.findByPostIdAndUserId(postId, userId)
                .map(existing -> {
                    existing.setValue(value);
                    return ratingRepository.save(existing);
                })
                .orElseGet(() -> {
                    PostRating rating = PostRating.builder()
                            .postId(postId)
                            .userId(userId)
                            .value(value)
                            .build();
                    return ratingRepository.save(rating);
                });
    }

    public int countRatings(Long postId) {
        return ratingRepository.countByPostId(postId);
    }
}
