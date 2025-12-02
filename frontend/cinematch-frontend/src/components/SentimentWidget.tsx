import React, { useEffect, useState } from "react";
import axios from "axios";

type SentimentLabel = "POSITIVE" | "NEGATIVE" | "NEUTRAL";

interface SentimentResponse {
  sentiment: string;
  score: number;
}

const DEBOUNCE_DELAY = 500;

const SentimentWidget: React.FC = () => {
  const [text, setText] = useState("");
  const [sentiment, setSentiment] = useState<SentimentResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!text.trim()) {
      setSentiment(null);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    const controller = new AbortController();
    const timeoutId = window.setTimeout(async () => {
      try {
        const response = await axios.post<SentimentResponse>(
          "/ai/sentiment",
          { text },
          { signal: controller.signal }
        );
        setSentiment(response.data);
      } catch (err) {
        if (!controller.signal.aborted) {
          setError("⚠ Κάτι πήγε στραβά, δοκίμασε ξανά.");
          setSentiment(null);
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }, DEBOUNCE_DELAY);

    return () => {
      clearTimeout(timeoutId);
      controller.abort();
    };
  }, [text]);

  const getSentimentDisplay = (label: SentimentLabel) => {
    switch (label) {
      case "POSITIVE":
        return { text: "Θετικό", color: "#4CAF50" };
      case "NEGATIVE":
        return { text: "Αρνητικό", color: "#FF5252" };
      case "NEUTRAL":
      default:
        return { text: "Ουδέτερο", color: "#FBC02D" };
    }
  };

  const renderResult = () => {
    if (error) {
      return (
        <div style={{ marginTop: 10, color: "#ff6b6b", fontSize: 14 }}>
          {error}
        </div>
      );
    }

    if (!sentiment) return null;

    const normalized = sentiment.sentiment.toUpperCase() as SentimentLabel;
    const { text: labelText, color } = getSentimentDisplay(normalized);
    const confidence = (sentiment.score * 100).toFixed(1);

    return (
      <div
        style={{
          marginTop: 16,
          background: "rgba(255, 255, 255, 0.06)",
          padding: "12px 14px",
          borderRadius: 8,
          border: `1px solid ${color}55`,
          color: "#fff",
          fontSize: 15,
        }}
      >
        <strong style={{ color }}>{labelText}</strong>
        <span style={{ marginLeft: 8, opacity: 0.9 }}>
          (Confidence: {confidence}%)
        </span>
      </div>
    );
  };

  return (
    <div style={{ marginTop: "40px" }}>
      <h2 style={{ marginBottom: "12px", fontSize: "26px", color: "#fff" }}>
        Sentiment Analysis
      </h2>

      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Γράψε τη γνώμη σου για την ταινία..."
        rows={4}
        style={{
          width: "100%",
          padding: "12px",
          borderRadius: "8px",
          border: "1px solid #444",
          backgroundColor: "#1e1e1e",
          color: "#fff",
          resize: "vertical",
          outline: "none",
          fontSize: "15px",
        }}
      />

      {isLoading && text.trim() && (
        <div style={{ marginTop: "10px", fontSize: "14px", opacity: 0.8 }}>
          Αναλύω το κείμενό σου…
        </div>
      )}

      {renderResult()}
    </div>
  );
};

export default SentimentWidget;
