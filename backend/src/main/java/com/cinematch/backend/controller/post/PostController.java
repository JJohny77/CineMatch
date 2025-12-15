package com.cinematch.backend.controller.post;

import com.cinematch.backend.model.User;
import com.cinematch.backend.model.post.Post;
import com.cinematch.backend.model.post.PostComment;
import com.cinematch.backend.model.post.PostRating;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.service.post.PostCommentService;
import com.cinematch.backend.service.post.PostRatingService;
import com.cinematch.backend.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final UserRepository userRepository;
    private final PostService postService;
    private final PostRatingService ratingService;
    private final PostCommentService commentService;

    // =====================================
    // Helper to get logged-in userId
    // =====================================
    private Long getCurrentUserId(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    // =====================================
    // CREATE POST
    // =====================================
    @PostMapping
    public ResponseEntity<Post> createPost(
            Authentication authentication,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = getCurrentUserId(authentication);

        String mediaUrl = (String) body.get("mediaUrl");
        String mediaType = (String) body.get("mediaType");
        String caption = (String) body.getOrDefault("caption", null);

        Long movieId = null;
        if (body.containsKey("movieId")) {
            Object movieIdObj = body.get("movieId");
            if (movieIdObj instanceof Number) {
                movieId = ((Number) movieIdObj).longValue();
            }
        }

        Post post = postService.createPost(
                userId,
                movieId,
                mediaUrl,
                mediaType,
                caption
        );

        return ResponseEntity.ok(post);
    }

    // =====================================
    // FEED (global)
    // =====================================
    @GetMapping("/feed")
    public ResponseEntity<List<Post>> getFeed() {
        return ResponseEntity.ok(postService.getFeed());
    }

    // =====================================
    // RATE POST (like or 1–5)
    // =====================================
    @PostMapping("/{postId}/rate")
    public ResponseEntity<PostRating> ratePost(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);

        Integer value = (Integer) body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().build();
        }

        PostRating rating = ratingService.ratePost(userId, postId, value);
        return ResponseEntity.ok(rating);
    }

    // =====================================
    // ADD COMMENT
    // =====================================
    @PostMapping("/{postId}/comments")
    public ResponseEntity<PostComment> addComment(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);

        String text = (String) body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        PostComment comment = commentService.addComment(userId, postId, text);
        return ResponseEntity.ok(comment);
    }

    // =====================================
    // GET COMMENTS FOR POST
    // =====================================
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<PostComment>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }
}
    