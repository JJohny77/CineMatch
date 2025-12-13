package com.cinematch.backend.service;

import com.cinematch.backend.model.User;
import com.cinematch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final UserRepository userRepository;

    /**
     * Επιστρέφει τον τρέχοντα authenticated χρήστη ή null
     */
    public User getCurrentUserOrNull() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            log.info("[CurrentUserService] auth is null");
            return null;
        }

        log.info(
                "[CurrentUserService] authClass={}, principal={}",
                auth.getClass().getSimpleName(),
                auth.getPrincipal()
        );

        Object principal = auth.getPrincipal();
        String email = null;

        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            String s = (String) principal;
            if (!"anonymousUser".equals(s)) {
                email = s;
            }
        }

        if (email == null) {
            log.info("[CurrentUserService] email is null → returning null");
            return null;
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.info(
                    "[CurrentUserService] user not found for email={}",
                    email
            );
        }

        return user;
    }
}
