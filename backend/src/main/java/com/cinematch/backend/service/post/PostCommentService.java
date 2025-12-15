package com.cinematch.backend.service.post;

import com.cinematch.backend.model.post.PostComment;
import com.cinematch.backend.repository.post.PostCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostCommentRepository commentRepository;

    public PostComment addComment(Long userId, Long postId, String text) {

        PostComment comment = PostComment.builder()
                .postId(postId)
                .userId(userId)
                .text(text)
                .build();

        return commentRepository.save(comment);
    }

    public List<PostComment> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }
}
