package com.cinematch.backend.model.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "actor_embeddings")
@Getter
@Setter
public class ActorEmbedding {

    @Id
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "name")
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    // raw JSON array με τα 512 doubles του embedding
    @Column(name = "embedding_json", columnDefinition = "text")
    private String embeddingJson;
}
