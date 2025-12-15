package com.cinematch.backend.service.post;

import com.cinematch.backend.model.post.Post;
import com.cinematch.backend.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

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
}
