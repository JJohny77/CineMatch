package com.cinematch.backend.repository.post;

import com.cinematch.backend.model.post.PostRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRatingRepository extends JpaRepository<PostRating, Long> {

    Optional<PostRating> findByPostIdAndUserId(Long postId, Long userId);

    int countByPostId(Long postId);
}
