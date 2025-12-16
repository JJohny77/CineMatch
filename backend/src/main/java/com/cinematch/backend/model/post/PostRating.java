package com.cinematch.backend.model.post;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_ratings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long userId;

    // 1–5 rating or boolean like. We keep integer because it's more flexible.
    @Column(nullable = false)
    private Integer value;
}
