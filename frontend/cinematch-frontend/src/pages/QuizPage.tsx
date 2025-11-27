import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

type QuizQuestion = {
  question: string;
  options: string[];
};

type StartQuizResponse = {
  questions: QuizQuestion[];
};

type SubmitAnswerResponse = {
  isCorrect: boolean; // προσαρμόζεται αν το backend στέλνει άλλο όνομα
};

const API_BASE_URL = "http://localhost:8080";

const QuizPage: React.FC = () => {
  const navigate = useNavigate();

  const [questions, setQuestions] = useState<QuizQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [score, setScore] = useState<number>(0);
  const [selectedOption, setSelectedOption] = useState<string | null>(null);
  const [isAnswerCorrect, setIsAnswerCorrect] = useState<boolean | null>(null);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [hasStarted, setHasStarted] = useState<boolean>(false);
  const [isFinished, setIsFinished] = useState<boolean>(false);
  const [isSavingScore, setIsSavingScore] = useState<boolean>(false);
  const [saveMessage, setSaveMessage] = useState<string>("");

  // 🔐 Αν δεν υπάρχει token → redirect σε login
  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
    }
  }, [navigate]);

  const totalQuestions = questions.length || 10; // default 10 για το progress text

  // 🟢 Start Quiz
  async function handleStartQuiz() {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }

    try {
      setHasStarted(true);
      setIsFinished(false);
      setSaveMessage("");
      setScore(0);
      setCurrentIndex(0);
      setSelectedOption(null);
      setIsAnswerCorrect(null);

      const response = await fetch(`${API_BASE_URL}/quiz/start`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({}), // κενό body
      });

      if (!response.ok) {
        throw new Error("Failed to start quiz");
      }

      const data: StartQuizResponse = await response.json();
      // backend επιστρέφει new QuizResponse(List<QuizQuestion>) -> πιθανό όνομα "questions"
      const questionsFromBackend =
        (data as any).questions ?? (data as any).quizQuestions ?? data;

      setQuestions(questionsFromBackend);
    } catch (err) {
      console.error(err);
      setHasStarted(false);
      alert("Could not start quiz. Please try again.");
    }
  }

  // 🟡 Submit Answer
  async function handleSelectOption(option: string) {
    if (!questions[currentIndex] || isProcessing) return;

    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }

    setIsProcessing(true);
    setSelectedOption(option);

    try {
      const body = {
        question: questions[currentIndex].question,
        selectedOption: option,
      };

      const response = await fetch(`${API_BASE_URL}/quiz/answer`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        throw new Error("Failed to submit answer");
      }

      const data: SubmitAnswerResponse = await response.json();
      const correct =
        (data as any).isCorrect ?? (data as any).correct ?? false;

      setIsAnswerCorrect(correct);
      if (correct) {
        setScore((prev) => prev + 1);
      }

      // Μετά από 1 δευτερόλεπτο → επόμενη ερώτηση ή τελικό σκορ
      setTimeout(() => {
        const isLastQuestion =
          currentIndex === questions.length - 1 || questions.length === 0;

        if (isLastQuestion) {
          finishQuiz(correct ? score + 1 : score);
        } else {
          setCurrentIndex((prev) => prev + 1);
          setSelectedOption(null);
          setIsAnswerCorrect(null);
          setIsProcessing(false);
        }
      }, 1000);
    } catch (err) {
      console.error(err);
      alert("Could not submit answer. Please try again.");
      setIsProcessing(false);
    }
  }

  // 🔴 Finish Quiz & save score
  async function finishQuiz(finalScore: number) {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }

    setIsFinished(true);
    setIsSavingScore(true);
    setIsProcessing(false);

    try {
      const response = await fetch(`${API_BASE_URL}/quiz/finish`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ score: finalScore }),
      });

      if (!response.ok) {
        throw new Error("Failed to save score");
      }

      setSaveMessage("Score saved!");
    } catch (err) {
      console.error(err);
      setSaveMessage("Could not save score. Please try again later.");
    } finally {
      setIsSavingScore(false);
    }
  }

  // 🔢 Progress helpers
  const currentQuestionNumber = Math.min(currentIndex + 1, totalQuestions);
  const progressPercent = hasStarted
    ? ((currentQuestionNumber - 1) / totalQuestions) * 100
    : 0;

  return (
    <div
      style={{
        maxWidth: "900px",
        margin: "0 auto",
        padding: "40px 20px",
        color: "white",
      }}
    >
      <h1 style={{ fontSize: "36px", marginBottom: "20px" }}>Quiz</h1>

      {/* Αν δεν έχει ξεκινήσει */}
      {!hasStarted && !isFinished && (
        <div>
          <p style={{ marginBottom: "20px", fontSize: "18px" }}>
            Test your movie knowledge with 10 questions!
          </p>
          <button
            onClick={handleStartQuiz}
            style={{
              padding: "12px 24px",
              fontSize: "18px",
              backgroundColor: "#e50914",
              color: "#fff",
              border: "none",
              borderRadius: "6px",
              cursor: "pointer",
            }}
          >
            Start Quiz
          </button>
        </div>
      )}

      {/* Εμφάνιση ερωτήσεων */}
      {hasStarted && !isFinished && questions.length > 0 && (
        <div>
          {/* Progress bar */}
          <div style={{ marginBottom: "20px" }}>
            <div
              style={{
                fontSize: "16px",
                marginBottom: "8px",
              }}
            >
              Question {currentQuestionNumber} of {totalQuestions}
            </div>
            <div
              style={{
                width: "100%",
                height: "8px",
                backgroundColor: "#333",
                borderRadius: "4px",
                overflow: "hidden",
              }}
            >
              <div
                style={{
                  height: "100%",
                  width: `${progressPercent}%`,
                  backgroundColor: "#e50914",
                  transition: "width 0.3s ease",
                }}
              />
            </div>
          </div>

          {/* Question text */}
          <div
            style={{
              fontSize: "22px",
              marginBottom: "20px",
              fontWeight: 500,
            }}
          >
            {questions[currentIndex].question}
          </div>

          {/* Options */}
          <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            {questions[currentIndex].options.map((option) => {
              const isSelected = option === selectedOption;
              let background = "#222";
              if (isSelected && isAnswerCorrect === true) {
                background = "#1a7f37"; // πράσινο
              } else if (isSelected && isAnswerCorrect === false) {
                background = "#b81d24"; // κόκκινο
              }

              return (
                <button
                  key={option}
                  onClick={() => handleSelectOption(option)}
                  disabled={isProcessing}
                  style={{
                    textAlign: "left",
                    width: "100%",
                    padding: "12px 16px",
                    fontSize: "16px",
                    borderRadius: "6px",
                    border: "1px solid #444",
                    backgroundColor: background,
                    color: "#fff",
                    cursor: isProcessing ? "default" : "pointer",
                    transition: "background-color 0.2s ease, transform 0.1s ease",
                  }}
                >
                  {option}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Τελικό σκορ */}
      {isFinished && (
        <div style={{ marginTop: "30px" }}>
          <h2 style={{ fontSize: "28px", marginBottom: "10px" }}>
            Quiz Finished!
          </h2>
          <p style={{ fontSize: "20px", marginBottom: "10px" }}>
            Your score: <strong>{score}</strong> / {totalQuestions}
          </p>
          <p style={{ fontSize: "16px", marginBottom: "20px" }}>
            {isSavingScore ? "Saving score..." : saveMessage}
          </p>

          <div style={{ display: "flex", gap: "12px", marginTop: "10px" }}>
            <button
              onClick={handleStartQuiz}
              style={{
                padding: "10px 20px",
                fontSize: "16px",
                backgroundColor: "#e50914",
                color: "#fff",
                border: "none",
                borderRadius: "6px",
                cursor: "pointer",
              }}
            >
              Play Again
            </button>

            <button
              onClick={() => navigate("/leaderboard")}
              style={{
                padding: "10px 20px",
                fontSize: "16px",
                backgroundColor: "#444",
                color: "#fff",
                border: "none",
                borderRadius: "6px",
                cursor: "pointer",
              }}
            >
              View Leaderboard
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default QuizPage;
