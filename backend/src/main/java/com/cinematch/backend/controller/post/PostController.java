package com.cinematch.backend.controller.post;

import com.cinematch.backend.dto.post.*;
import com.cinematch.backend.model.User;
import com.cinematch.backend.model.post.Post;
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
    private Long getCurrentUserIdOrNull(Authentication auth) {
        if (auth == null) return null;
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }

    private Long getCurrentUserId(Authentication auth) {
        Long id = getCurrentUserIdOrNull(auth);
        if (id == null) throw new RuntimeException("User not found");
        return id;
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
    // FEED
    // =====================================
    @GetMapping("/feed")
    public ResponseEntity<List<PostFeedDto>> getFeed(Authentication authentication) {

        Long viewerUserId = getCurrentUserIdOrNull(authentication);

        List<Post> feed = postService.getFeed();

        List<PostFeedDto> mapped = feed.stream()
                .map(p -> toFeedPostDto(p, viewerUserId))
                .toList();

        return ResponseEntity.ok(mapped);
    }

    // =====================================
    // TOGGLE LIKE
    // frontend calls POST /posts/{id}/like
    // =====================================
    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeResponseDto> toggleLike(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        LikeResponseDto dto = ratingService.toggleLike(userId, postId);
        return ResponseEntity.ok(dto);
    }

    // =====================================
    // ADD COMMENT
    // =====================================
    @PostMapping("/{postId}/comments")
    public ResponseEntity<PostCommentDto> addComment(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);

        String text = (String) body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        PostCommentDto comment = commentService.addComment(userId, postId, text);
        return ResponseEntity.ok(comment);
    }

    // =====================================
    // GET COMMENTS FOR POST
    // =====================================
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<PostCommentDto>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    // =====================================
    // ✅ DELETE POST (ONLY OWNER)
    // =====================================
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);
        postService.deletePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    // =====================================
    // MAPPER
    // =====================================
    private PostFeedDto toFeedPostDto(Post p, Long viewerUserId) {

        User author = userRepository.findById(p.getUserId()).orElse(null);

        int likesCount = ratingService.countLikes(p.getId());
        boolean likedByMe = (viewerUserId != null) && ratingService.likedByUser(p.getId(), viewerUserId);
        boolean ownedByMe = (viewerUserId != null) && p.getUserId().equals(viewerUserId);

        return PostFeedDto.builder()
                .id(p.getId())
                .mediaUrl(p.getMediaUrl())
                .mediaType(p.getMediaType())
                .caption(p.getCaption())
                .createdAt(p.getCreatedAt())
                .movieId(p.getMovieId())
                .author(PostAuthorDto.builder()
                        .id(p.getUserId())
                        .username(author != null && author.getUsername() != null ? author.getUsername() : "unknown")
                        .build())
                .likesCount(likesCount)
                .likedByMe(likedByMe)
                .ownedByMe(ownedByMe)
                .build();
    }
}
