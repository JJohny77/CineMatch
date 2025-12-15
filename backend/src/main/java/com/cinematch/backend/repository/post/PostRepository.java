package com.cinematch.backend.repository.post;

import com.cinematch.backend.model.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Post> findByMovieIdOrderByCreatedAtDesc(Long movieId);
}
