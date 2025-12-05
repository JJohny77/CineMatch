package com.cinematch.backend.repository;

import com.cinematch.backend.model.ai.ActorEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActorEmbeddingRepository extends JpaRepository<ActorEmbedding, Long> {
}
