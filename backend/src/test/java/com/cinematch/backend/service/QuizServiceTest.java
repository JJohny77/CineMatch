package com.cinematch.backend.service;

import com.cinematch.backend.quiz.service.QuizService;
import com.cinematch.backend.dto.MovieResultDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.quiz.dto.LeaderboardEntry;
import com.cinematch.backend.quiz.dto.QuizResponse;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.service.CurrentUserService;
import com.cinematch.backend.service.MovieRecommendationService;
import com.cinematch.backend.service.UserPreferenceService;
import com.cinematch.backend.service.ai.AiQuizGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private AiQuizGenerator aiQuizGenerator;

    @Mock
    private MovieRecommendationService movieRecommendationService;

    @Mock
    private UserPreferenceService userPreferenceService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuizService quizService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@mail.com");
        user.setQuizScore(10);
    }

    // 1️⃣ startQuiz επιστρέφει ερωτήσεις
    @Test
    void startQuiz_returnsQuestions() {
        when(currentUserService.getCurrentUserOrNull()).thenReturn(user);
        when(movieRecommendationService.getQuizCandidatesForUser(any(), anyInt()))
                .thenReturn(List.of(new MovieResultDto()));

        when(aiQuizGenerator.generateFullQuestions(any(), any(), anyInt()))
                .thenReturn(Collections.emptyList()); // force static pool

        QuizResponse response = quizService.startQuiz();

        assertNotNull(response);
        assertEquals(10, response.getQuestions().size());
    }


    // 2️⃣ checkAnswer σωστή απάντηση
    @Test
    void checkAnswer_correctAnswer_returnsTrue() {
        when(currentUserService.getCurrentUserOrNull()).thenReturn(null);

        quizService.startQuiz(); // γεμίζει static pool

        boolean result = quizService.checkAnswer(
                "Ποιος σκηνοθέτησε το Inception;",
                "Christopher Nolan"
        );

        assertTrue(result);
    }

    // 3️⃣ checkAnswer λάθος απάντηση
    @Test
    void checkAnswer_wrongAnswer_returnsFalse() {
        when(currentUserService.getCurrentUserOrNull()).thenReturn(null);

        quizService.startQuiz();

        boolean result = quizService.checkAnswer(
                "Ποιος σκηνοθέτησε το Inception;",
                "James Cameron"
        );

        assertFalse(result);
    }

    // 4️⃣ saveQuizScore αυξάνει το σκορ
    @Test
    void saveQuizScore_increasesUserScore() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@mail.com", null)
        );

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        quizService.saveQuizScore(5, userRepository);

        assertEquals(15, user.getQuizScore());
        verify(userRepository).save(user);
    }

    // 5️⃣ leaderboard επιστρέφει σωστή σειρά
    @Test
    void getLeaderboard_returnsSortedLeaderboard() {
        User u1 = new User();
        u1.setEmail("a@mail.com");
        u1.setQuizScore(5);

        User u2 = new User();
        u2.setEmail("b@mail.com");
        u2.setQuizScore(20);

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<LeaderboardEntry> leaderboard =
                quizService.getLeaderboard(userRepository);

        assertEquals("b@mail.com", leaderboard.get(0).getEmail());
        assertEquals(20, leaderboard.get(0).getScore());
    }
}
