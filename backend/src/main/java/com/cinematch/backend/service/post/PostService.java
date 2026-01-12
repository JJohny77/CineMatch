package com.cinematch.backend.service.post;

import com.cinematch.backend.model.post.Post;
import com.cinematch.backend.repository.post.PostCommentRepository;
import com.cinematch.backend.repository.post.PostRatingRepository;
import com.cinematch.backend.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostRatingRepository ratingRepository;

    public Post createPost(Long userId, Long movieId, String mediaUrl, String mediaType, String caption) {

        Post post = Post.builder()
                .userId(userId)
                .movieId(movieId)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .caption(caption)
                .build();

        return postRepository.save(post);
    }

    public List<Post> getFeed() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Post> getUserPosts(Long userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Post getById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    // ==========================
    // DELETE POST (ONLY OWNER)
    // ==========================
    @Transactional
    public void deletePost(Long requesterUserId, Long postId) {

        Post post = getById(postId);

        if (!post.getUserId().equals(requesterUserId)) {
            throw new RuntimeException("Forbidden: you can delete only your own posts");
        }

        // cleanup relations (since we don't have JPA relations/cascades here)
        commentRepository.deleteByPostId(postId);
        ratingRepository.deleteByPostId(postId);

        postRepository.deleteById(postId);
    }
}
