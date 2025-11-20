package com.cinematch.backend.quiz.service;

import com.cinematch.backend.quiz.dto.QuizQuestion;
import com.cinematch.backend.quiz.dto.QuizResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class QuizService {

    public QuizResponse startQuiz() {

        // 1. Φτιάχνουμε ένα pool από πιθανές ερωτήσεις
        List<QuizQuestion> allQuestions = getQuestionPool();

        // 2. Ανακατεύουμε το pool
        Collections.shuffle(allQuestions, new Random());

        // 3. Παίρνουμε τις 10 πρώτες
        List<QuizQuestion> selected = allQuestions.subList(0, 10);

        // 4. Επιστρέφουμε response
        return new QuizResponse(selected);
    }


    // ---------------------------
    //  PRIVATE METHOD
    // ---------------------------
    private List<QuizQuestion> getQuestionPool() {

        List<QuizQuestion> list = new ArrayList<>();

        list.add(new QuizQuestion(
                "Ποιος σκηνοθέτησε το Inception;",
                List.of("Christopher Nolan", "Steven Spielberg", "James Cameron", "Ridley Scott")
        ));

        list.add(new QuizQuestion(
                "Ποια χρονιά κυκλοφόρησε το Matrix;",
                List.of("1999", "2003", "2001", "1995")
        ));

        list.add(new QuizQuestion(
                "Ποιος ηθοποιός υποδύεται τον Joker (2019);",
                List.of("Joaquin Phoenix", "Heath Ledger", "Jack Nicholson", "Jared Leto")
        ));

        list.add(new QuizQuestion(
                "Ποια ταινία έχει την ατάκα «May the Force be with you»;",
                List.of("Star Wars", "Star Trek", "Dune", "The Matrix")
        ));

        list.add(new QuizQuestion(
                "Ποιος σκηνοθέτησε το Pulp Fiction;",
                List.of("Quentin Tarantino", "Guy Ritchie", "Martin Scorsese", "David Fincher")
        ));

        list.add(new QuizQuestion(
                "Σε ποια πόλη διαδραματίζεται ο Joker (2019);",
                List.of("Gotham City", "Metropolis", "New York", "Chicago")
        ));

        list.add(new QuizQuestion(
                "Ποιος ηθοποιός παίζει τον Neo στο Matrix;",
                List.of("Keanu Reeves", "Brad Pitt", "Johnny Depp", "Matt Damon")
        ));

        list.add(new QuizQuestion(
                "Ποια ταινία κέρδισε Oscar Καλύτερης Ταινίας το 2020;",
                List.of("Parasite", "1917", "Joker", "Ford v Ferrari")
        ));

        list.add(new QuizQuestion(
                "Ποιος σκηνοθέτησε το Avatar;",
                List.of("James Cameron", "Peter Jackson", "George Lucas", "Denis Villeneuve")
        ));

        list.add(new QuizQuestion(
                "Ποια είναι η πιο εμπορική ταινία όλων των εποχών;",
                List.of("Avatar", "Avengers: Endgame", "Titanic", "Star Wars: The Force Awakens")
        ));

        return list;
    }
}
