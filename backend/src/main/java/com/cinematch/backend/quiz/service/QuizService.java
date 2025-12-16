package com.cinematch.backend.quiz.service;

import com.cinematch.backend.dto.MovieResultDto;
import com.cinematch.backend.dto.UserPreferencesResponseDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.quiz.dto.LeaderboardEntry;
import com.cinematch.backend.quiz.dto.QuizQuestion;
import com.cinematch.backend.quiz.dto.QuizResponse;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.service.CurrentUserService;
import com.cinematch.backend.service.MovieRecommendationService;
import com.cinematch.backend.service.UserPreferenceService;
import com.cinematch.backend.service.ai.AiQuizGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final AiQuizGenerator aiQuizGenerator;
    private final MovieRecommendationService movieRecommendationService;
    private final UserPreferenceService userPreferenceService;
    private final CurrentUserService currentUserService;

    private final Map<Long, Map<String, String>> answerKeyByUser = new ConcurrentHashMap<>();
    private static final long ANON_USER_KEY = -1L;

    // ================================
    // Start Quiz
    // ================================
    public QuizResponse startQuiz() {

        User user = currentUserService.getCurrentUserOrNull();

        UserPreferencesResponseDto prefs = null;
        List<MovieResultDto> candidates;

        if (user != null) {
            // prefs may still be empty if user has no events yet (cold start) -> ok
            prefs = userPreferenceService.computeAndPersist(user, 5);
            candidates = movieRecommendationService.getQuizCandidatesForUser(user, 160);
        } else {
            candidates = movieRecommendationService.getQuizCandidatesForUser(null, 160);
        }

        List<FullQuestion> fullPool = aiQuizGenerator.generateFullQuestions(prefs, candidates, 10);

        // safety fallback (static pool)
        if (fullPool == null || fullPool.size() < 10) {
            List<FullQuestion> staticPool = new ArrayList<>(getStaticQuestionPool());
            Collections.shuffle(staticPool, new Random());
            fullPool = staticPool.subList(0, Math.min(10, staticPool.size()));
        }

        long key = (user != null && user.getId() != null) ? user.getId() : ANON_USER_KEY;

        Map<String, String> answerKey = new HashMap<>();
        for (FullQuestion fq : fullPool) {
            answerKey.put(fq.question(), fq.correctAnswer());
        }
        answerKeyByUser.put(key, answerKey);

        List<QuizQuestion> safeQuestions = new ArrayList<>();
        for (FullQuestion fq : fullPool) {
            List<String> shuffled = new ArrayList<>(fq.options());
            Collections.shuffle(shuffled);
            safeQuestions.add(new QuizQuestion(fq.question(), shuffled));
        }

        return new QuizResponse(safeQuestions);
    }

    // ================================
    // Check Answer
    // ================================
    public boolean checkAnswer(String question, String selectedOption) {
        String correct = getCorrectAnswer(question);
        return correct != null && correct.equals(selectedOption);
    }

    public String getCorrectAnswer(String question) {
        User user = currentUserService.getCurrentUserOrNull();
        long key = (user != null && user.getId() != null) ? user.getId() : ANON_USER_KEY;

        Map<String, String> map = answerKeyByUser.get(key);
        if (map != null && map.containsKey(question)) return map.get(question);

        // fallback to static pool
        for (FullQuestion fq : getStaticQuestionPool()) {
            if (fq.question().equals(question)) return fq.correctAnswer();
        }

        throw new RuntimeException("Question not found: " + question);
    }

    // ================================
    // Static pool (last resort)
    // ================================
    private List<FullQuestion> getStaticQuestionPool() {

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
    // Finish quiz & save score
    // ================================
    public void saveQuizScore(int score, UserRepository userRepository) {
        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        com.cinematch.backend.model.User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setQuizScore(user.getQuizScore() + score);
        userRepository.save(user);
    }

    public List<LeaderboardEntry> getLeaderboard(UserRepository userRepository) {
        return userRepository.findAll()
                .stream()
                .sorted((u1, u2) -> u2.getQuizScore() - u1.getQuizScore())
                .map(u -> new LeaderboardEntry(u.getEmail(), u.getQuizScore()))
                .collect(Collectors.toList());
    }
}
