package com.cinematch.backend.dto;                        // Πακέτο για DTO αντικείμενα

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                                                    // Lombok: δημιουργεί getters/setters/toString
@AllArgsConstructor                                      // Lombok: constructor με όλα τα πεδία
@NoArgsConstructor                                       // Lombok: empty constructor
public class UserProfileResponse {

    private String email;                                // Το email του χρήστη
    private String role;                                 // Ο ρόλος του χρήστη (USER / ADMIN)
    private Integer quizScore;                           // Οι συνολικοί πόντοι του quiz
    private String createdAt;                            // Η ημερομηνία δημιουργίας λογαριασμού (String για πιο εύκολο JSON)
}

