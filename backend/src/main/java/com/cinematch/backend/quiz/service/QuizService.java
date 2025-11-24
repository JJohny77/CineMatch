package com.cinematch.backend.quiz.service;

import com.cinematch.backend.quiz.dto.LeaderboardEntry;
import java.util.stream.Collectors;
import com.cinematch.backend.quiz.dto.QuizQuestion;
import com.cinematch.backend.quiz.dto.QuizResponse;
import com.cinematch.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class QuizService {

    // ================================
    // US21 – Start Quiz
    // ================================
    public QuizResponse startQuiz() {

        // Παίρνουμε το full pool (με correct answers backend-only)
        List<FullQuestion> fullPool = getFullQuestionPool();

        // Shuffle
        Collections.shuffle(fullPool, new Random());

        // Πάρε τις πρώτες 10
        List<FullQuestion> selected = fullPool.subList(0, 10);

        // Μετατροπή σε "safe" QuizQuestion DTO (χωρίς σωστή απάντηση)
        List<QuizQuestion> safeQuestions = new ArrayList<>();
        for (FullQuestion fq : selected) {
            safeQuestions.add(
                    new QuizQuestion(fq.question(), fq.options())
            );
        }

        return new QuizResponse(safeQuestions);
    }


    // ================================
    // US22 – Check Answer
    // ================================
    public boolean checkAnswer(String question, String selectedOption) {

        // Βρίσκουμε στο backend ποια είναι η σωστή απάντηση
        for (FullQuestion fq : getFullQuestionPool()) {
            if (fq.question().equals(question)) {
                return fq.correctAnswer().equals(selectedOption);
            }
        }

        // Αν δεν βρέθηκε η ερώτηση (δεν θα συμβεί ποτέ στο demo)
        return false;
    }


    // ================================
    // BACKEND-ONLY QUESTION POOL
    // ================================
    private List<FullQuestion> getFullQuestionPool() {

        List<FullQuestion> list = new ArrayList<>();

        list.add(new FullQuestion(
                "Ποιος σκηνοθέτησε το Inception;",
                "Christopher Nolan",
                List.of("Christopher Nolan", "Steven Spielberg", "James Cameron", "Ridley Scott")
        ));

        list.add(new FullQuestion(
                "Ποια χρονιά κυκλοφόρησε το Matrix;",
                "1999",
                List.of("1999", "2003", "2001", "1995")
        ));

        list.add(new FullQuestion(
                "Ποιος ηθοποιός υποδύεται τον Joker (2019);",
                "Joaquin Phoenix",
                List.of("Joaquin Phoenix", "Heath Ledger", "Jack Nicholson", "Jared Leto")
        ));

        list.add(new FullQuestion(
                "Ποια ταινία έχει την ατάκα «May the Force be with you»;",
                "Star Wars",
                List.of("Star Wars", "Star Trek", "Dune", "The Matrix")
        ));

        list.add(new FullQuestion(
                "Ποιος σκηνοθέτησε το Pulp Fiction;",
                "Quentin Tarantino",
                List.of("Quentin Tarantino", "Guy Ritchie", "Martin Scorsese", "David Fincher")
        ));

        list.add(new FullQuestion(
                "Σε ποια πόλη διαδραματίζεται ο Joker (2019);",
                "Gotham City",
                List.of("Gotham City", "Metropolis", "New York", "Chicago")
        ));

        list.add(new FullQuestion(
                "Ποιος ηθοποιός παίζει τον Neo στο Matrix;",
                "Keanu Reeves",
                List.of("Keanu Reeves", "Brad Pitt", "Johnny Depp", "Matt Damon")
        ));

        list.add(new FullQuestion(
                "Ποια ταινία κέρδισε Oscar Καλύτερης Ταινίας το 2020;",
                "Parasite",
                List.of("Parasite", "1917", "Joker", "Ford v Ferrari")
        ));

        list.add(new FullQuestion(
                "Ποιος σκηνοθέτησε το Avatar;",
                "James Cameron",
                List.of("James Cameron", "Peter Jackson", "George Lucas", "Denis Villeneuve")
        ));

        list.add(new FullQuestion(
                "Ποια είναι η πιο εμπορική ταινία όλων των εποχών;",
                "Avatar",
                List.of("Avatar", "Avengers: Endgame", "Titanic", "Star Wars: The Force Awakens")
        ));

        return list;
    }
    // ================================
// US23 — Finish quiz & save score
// ================================
    public void saveQuizScore(int score, UserRepository userRepository) {

        // 1. Πάρε email από το security context
        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        // 2. Φέρε τον χρήστη
        com.cinematch.backend.model.User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Πρόσθεσε το score
        user.setQuizScore(user.getQuizScore() + score);

        // 4. Αποθήκευσε
        userRepository.save(user);
    }

    public List<LeaderboardEntry> getLeaderboard(UserRepository userRepository) {
        return userRepository.findAll()
                .stream()
                .sorted((u1, u2) -> u2.getQuizScore() - u1.getQuizScore()) // descending
                .map(u -> new LeaderboardEntry(u.getEmail(), u.getQuizScore()))
                .collect(Collectors.toList());
    }


}
