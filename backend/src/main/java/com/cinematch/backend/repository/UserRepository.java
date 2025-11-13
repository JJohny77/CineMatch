package com.cinematch.backend.repository;

import com.cinematch.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Αναζήτηση χρήστη με βάση το email
    Optional<User> findByEmail(String email);

    // Προαιρετικό αλλά πρακτικό: έλεγχος αν υπάρχει email
    boolean existsByEmail(String email);
}
