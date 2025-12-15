package com.cinematch.backend.model.post;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // optional movie reference (TMDb id)
    private Long movieId;

    @Column(nullable = false)
    private String mediaUrl;

    @Column(nullable = false)
    private String mediaType; // image | video

    @Column(columnDefinition = "TEXT")
    private String caption;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
