package com.cinematch.backend.service.post;

import com.cinematch.backend.dto.post.PostCommentDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.model.post.PostComment;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.repository.post.PostCommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Mock
    private PostCommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostCommentService postCommentService;

    @Test
    void addComment_shouldSaveAndReturnDto() {
        PostComment saved = PostComment.builder()
                .id(1L)
                .postId(10L)
                .userId(5L)
                .text("hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(commentRepository.save(any(PostComment.class))).thenReturn(saved);
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        PostCommentDto dto = postCommentService.addComment(5L, 10L, "hello");

        assertNotNull(dto);
        assertEquals(10L, dto.getPostId());
        assertEquals(5L, dto.getUserId());
        assertEquals("hello", dto.getText());

        verify(commentRepository).save(any(PostComment.class));
    }

    @Test
    void getComments_shouldReturnDtosWithAuthor() {
        PostComment comment = PostComment.builder()
                .id(1L)
                .postId(10L)
                .userId(5L)
                .text("test")
                .createdAt(LocalDateTime.now())
                .build();

        User user = User.builder()
                .id(5L)
                .username("john")
                .build();

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(comment));
        when(userRepository.findById(5L))
                .thenReturn(Optional.of(user));

        List<PostCommentDto> result = postCommentService.getComments(10L);

        assertEquals(1, result.size());
        assertEquals("john", result.get(0).getAuthor().getUsername());
    }

    @Test
    void getComments_userNotFound_shouldReturnUnknownAuthor() {
        PostComment comment = PostComment.builder()
                .id(1L)
                .postId(10L)
                .userId(99L)
                .text("test")
                .createdAt(LocalDateTime.now())
                .build();

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(comment));
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        List<PostCommentDto> result = postCommentService.getComments(10L);

        assertEquals("unknown", result.get(0).getAuthor().getUsername());
    }
}
