package com.cinematch.backend.service.post;

import com.cinematch.backend.dto.post.PostAuthorDto;
import com.cinematch.backend.dto.post.PostCommentDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.model.post.PostComment;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.repository.post.PostCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;

    public PostCommentDto addComment(Long userId, Long postId, String text) {

        PostComment comment = PostComment.builder()
                .postId(postId)
                .userId(userId)
                .text(text)
                .build();

        PostComment saved = commentRepository.save(comment);
        return toDto(saved);
    }

    public List<PostCommentDto> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private PostCommentDto toDto(PostComment c) {

        User u = userRepository.findById(c.getUserId()).orElse(null);

        PostAuthorDto author = PostAuthorDto.builder()
                .id(c.getUserId())
                .username(u != null ? u.getUsername() : "unknown")
                .build();

        return PostCommentDto.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .userId(c.getUserId())
                .text(c.getText())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .author(author)
                .build();
    }
}
