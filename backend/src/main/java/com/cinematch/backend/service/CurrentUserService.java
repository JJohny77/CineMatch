package com.cinematch.backend.service;

import com.cinematch.backend.model.User;
import com.cinematch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Επιστρέφει τον τρέχοντα authenticated χρήστη ή null αν:
     * - δεν υπάρχει Authentication
     * - είναι anonymousUser
     * - δεν βρέθηκε στο UserRepository
     */
    public User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        String email = null;

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String s && !"anonymousUser".equals(s)) {
            email = s;
        }

        if (email == null) {
            return null;
        }

        return userRepository.findByEmail(email).orElse(null);
    }
}
