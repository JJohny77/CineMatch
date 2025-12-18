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
  isCorrect: boolean;
  correctAnswer: string | null;
  selectedAnswer: string | null;

  // fallback keys (αν backend τα λέει αλλιώς)
  correct?: string | null;
  selectedOption?: string | null;
};

const API_BASE_URL = "http://localhost:8080";

const QuizPage: React.FC = () => {
  const navigate = useNavigate();

  const [questions, setQuestions] = useState<QuizQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [score, setScore] = useState<number>(0);

  const [selectedOption, setSelectedOption] = useState<string | null>(null);
  const [isAnswerCorrect, setIsAnswerCorrect] = useState<boolean | null>(null);
  const [correctAnswer, setCorrectAnswer] = useState<string | null>(null);

  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [hasStarted, setHasStarted] = useState<boolean>(false);
  const [isFinished, setIsFinished] = useState<boolean>(false);

  const [isSavingScore, setIsSavingScore] = useState<boolean>(false);
  const [saveMessage, setSaveMessage] = useState<string>("");

  // Redirect if no token
  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
    }
  }, [navigate]);

  const totalQuestions = questions.length || 10;

  // ---------------------------
  // START QUIZ
  // ---------------------------
  async function handleStartQuiz() {
    const token = localStorage.getItem("token");
    if (!token) return navigate("/login");

    try {
      setHasStarted(true);
      setIsFinished(false);
      setIsProcessing(false);

      setScore(0);
      setCurrentIndex(0);
      setSelectedOption(null);
      setIsAnswerCorrect(null);
      setCorrectAnswer(null);
      setSaveMessage("");

      const response = await fetch(`${API_BASE_URL}/quiz/start`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({}),
      });

      if (!response.ok) throw new Error("Failed to start quiz");

      const data: any = await response.json();

      // backend μπορεί να επιστρέψει είτε {questions:[...]} είτε απευθείας λίστα
      const questionsList: QuizQuestion[] = data?.questions ?? data;

      if (!Array.isArray(questionsList) || questionsList.length === 0) {
        throw new Error("Empty quiz questions");
      }

      setQuestions(questionsList);
    } catch (err) {
      console.error(err);
      setHasStarted(false);
      alert("Could not start quiz. Please try again.");
    }
  }

  // ---------------------------
  // SUBMIT ANSWER
  // ---------------------------
  async function handleSelectOption(option: string) {
    if (!questions[currentIndex] || isProcessing) return;

    const token = localStorage.getItem("token");
    if (!token) return navigate("/login");

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

      if (!response.ok) throw new Error("Failed to submit answer");

      const data: SubmitAnswerResponse = await response.json();

      const isCorrect = !!data.isCorrect;

      // fallback mapping για keys
      const correct =
        (data.correctAnswer ?? (data as any).correct ?? null) as string | null;

      const selected =
        (data.selectedAnswer ??
          (data as any).selectedOption ??
          option) as string | null;

      setIsAnswerCorrect(isCorrect);
      setCorrectAnswer(correct);
      setSelectedOption(selected);

      // κρατάμε “σίγουρο” nextScore για το finish
      const nextScore = isCorrect ? score + 1 : score;
      if (isCorrect) {
        setScore(nextScore); // κάν’ το explicit για να μη φαίνεται λάθος στο τέλος
      }

      setTimeout(() => {
        const isLast = currentIndex === questions.length - 1;

        if (isLast) {
          // αν ήταν λάθος, το score μένει όπως είναι
          finishQuiz(nextScore);
        } else {
          setCurrentIndex((prev) => prev + 1);
          setSelectedOption(null);
          setCorrectAnswer(null);
          setIsAnswerCorrect(null);
          setIsProcessing(false);
        }
      }, 1100);
    } catch (err) {
      console.error(err);
      alert("Could not submit answer. Try again.");
      setIsProcessing(false);
    }
  }

  // ---------------------------
  // FINISH QUIZ
  // ---------------------------
  async function finishQuiz(finalScore: number) {
    const token = localStorage.getItem("token");
    if (!token) return navigate("/login");

    setIsFinished(true);
    setIsSavingScore(true);
    setIsProcessing(false);

    // Σιγουρεύουμε ότι το UI δείχνει το τελικό score
    setScore(finalScore);

    try {
      const response = await fetch(`${API_BASE_URL}/quiz/finish`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ score: finalScore }),
      });

      if (!response.ok) throw new Error("Failed to save quiz score");

      setSaveMessage("Score saved!");
    } catch (err) {
      console.error(err);
      setSaveMessage("Could not save score.");
    } finally {
      setIsSavingScore(false);
    }
  }

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

      {/* Start Screen */}
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

      {/* Quiz Questions */}
      {hasStarted && !isFinished && questions.length > 0 && (
        <div>
          {/* Progress bar */}
          <div style={{ marginBottom: "20px" }}>
            <div style={{ marginBottom: "8px", fontSize: "16px" }}>
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
              const isUserChoice = option === selectedOption;
              const isCorrectOption = option === correctAnswer;

              let background = "#222";

              if (isAnswerCorrect !== null) {
                if (isAnswerCorrect && isUserChoice) {
                  background = "#1a7f37";
                }

                if (!isAnswerCorrect) {
                  if (isUserChoice) background = "#b81d24";
                  if (isCorrectOption) background = "#1a7f37";
                }
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
                    transition: "0.2s ease",
                  }}
                >
                  {option}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Finished */}
      {isFinished && (
        <div style={{ marginTop: "30px" }}>
          <h2 style={{ fontSize: "28px", marginBottom: "10px" }}>
            Quiz Finished!
          </h2>
          <p style={{ fontSize: "20px", marginBottom: "10px" }}>
            Your score: <strong>{score}</strong> / {totalQuestions}
          </p>
          <p style={{ fontSize: "16px" }}>
            {isSavingScore ? "Saving score..." : saveMessage}
          </p>

          <div style={{ display: "flex", gap: "12px", marginTop: "20px" }}>
            <button
              onClick={handleStartQuiz}
              style={{
                padding: "10px 20px",
                fontSize: "16px",
                backgroundColor: "#e50914",
                borderRadius: "6px",
                border: "none",
                color: "#fff",
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
                borderRadius: "6px",
                border: "none",
                color: "#fff",
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
