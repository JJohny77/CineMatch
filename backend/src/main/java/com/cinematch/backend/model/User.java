package com.cinematch.backend.model;

import com.cinematch.backend.auth.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private Integer quizScore = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // =========================
    // US54: preference profile
    // =========================

    // JSON array string: [{ "genreId": 28, "score": 3.0 }, ...]
    @Column(name = "top_genres", columnDefinition = "TEXT")
    private String topGenres;

    // JSON array string: [{ "personId": 287, "score": 2.0 }, ...]
    @Column(name = "top_actors", columnDefinition = "TEXT")
    private String topActors;

    // JSON array string: [{ "personId": 488, "score": 4.0 }, ...]
    @Column(name = "top_directors", columnDefinition = "TEXT")
    private String topDirectors;

    @Column(name = "preferences_last_updated")
    private Instant preferencesLastUpdated;
}
