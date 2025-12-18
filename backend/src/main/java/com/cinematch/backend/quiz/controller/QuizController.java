package com.cinematch.backend.quiz.controller;

import com.cinematch.backend.model.User;
import com.cinematch.backend.model.UserEventType;
import com.cinematch.backend.quiz.dto.*;
import com.cinematch.backend.quiz.service.QuizService;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.service.CurrentUserService;
import com.cinematch.backend.service.UserEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;
    private final UserRepository userRepository;
    private final UserEventService userEventService;
    private final CurrentUserService currentUserService;

    // ================================
    // Start Quiz
    // ================================
    @PostMapping("/start")
    public ResponseEntity<QuizResponse> startQuiz() {
        QuizResponse response = quizService.startQuiz(); // ✅ no args
        return ResponseEntity.ok(response);
    }

    // ================================
    // Submit Answer
    // ================================
    @PostMapping("/answer")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @RequestBody SubmitAnswerRequest request
    ) {
        boolean isCorrect = quizService.checkAnswer(
                request.getQuestion(),
                request.getSelectedOption()
        );

        String correct = quizService.getCorrectAnswer(request.getQuestion());

        // log event
        User user = currentUserService.getCurrentUserOrNull();
        if (user != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("question", request.getQuestion());
            payload.put("selectedOption", request.getSelectedOption());
            payload.put("correctAnswer", correct);

            userEventService.logEvent(
                    user,
                    isCorrect ? UserEventType.QUIZ_CORRECT : UserEventType.QUIZ_WRONG,
                    payload
            );
        }

        return ResponseEntity.ok(
                new SubmitAnswerResponse(
                        isCorrect,
                        request.getSelectedOption(),
                        correct,
                        true
                )
        );
    }

    // ================================
    // Finish Quiz
    // ================================
    @PostMapping("/finish")
    public ResponseEntity<FinishQuizResponse> finishQuiz(
            @RequestBody FinishQuizRequest request
    ) {
        quizService.saveQuizScore(request.getScore(), userRepository);
        return ResponseEntity.ok(new FinishQuizResponse(true));
    }

    // ================================
    // Leaderboard
    // ================================
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        return ResponseEntity.ok(quizService.getLeaderboard(userRepository));
    }
}
