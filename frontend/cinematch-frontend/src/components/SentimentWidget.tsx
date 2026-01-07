import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/httpClient";

interface MovieCard {
  adult?: boolean;
  backdrop_path?: string | null;
  genre_ids?: number[];
  id: number;
  original_language?: string;
  original_title?: string;
  overview?: string;
  popularity?: number;
  poster_path?: string | null;
  release_date?: string;
  title?: string;
  video?: boolean;
  vote_average?: number;
  vote_count?: number;
}

interface RecommendByMoodResponse {
  sentiment: string; // positive / negative / neutral / loading
  score: number;
  tag: string; // uplifting / dark / neutral
  results: MovieCard[];
}

const SentimentWidget: React.FC = () => {
  const navigate = useNavigate();

  const [text, setText] = useState("");
  const [data, setData] = useState<RecommendByMoodResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFindMovies = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmed = text.trim();
    if (!trimmed) {
      setData(null);
      setError("Please describe your mood first.");
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const res = await api.post<RecommendByMoodResponse>("/ai/recommend-by-mood", {
        text: trimmed,
      });
      setData(res.data);
    } catch (err) {
      setError("⚠ Something went wrong. Please try again.");
      setData(null);
    } finally {
      setIsLoading(false);
    }
  };

  const confidencePct = data ? (data.score * 100).toFixed(1) : null;

  return (
    <div style={{ marginTop: "40px" }}>
      <div style={{ display: "flex", alignItems: "baseline", gap: "10px" }}>
        <h2 style={{ marginBottom: "12px", fontSize: "26px", color: "#fff" }}>
          What are you in the mood for?
        </h2>

        {/* Optional label/tooltip */}
        <span
          title="Powered by Sentiment Analysis (HuggingFace)"
          style={{
            fontSize: "12px",
            opacity: 0.75,
            color: "#ddd",
            cursor: "help",
          }}
        >
          Powered by Sentiment Analysis (HuggingFace)
        </span>
      </div>

      <form onSubmit={handleFindMovies}>
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Describe what you're in the mood for..."
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

        <button
          type="submit"
          disabled={isLoading || !text.trim()}
          style={{
            marginTop: "12px",
            padding: "10px 16px",
            borderRadius: "8px",
            border: "none",
            cursor: isLoading || !text.trim() ? "default" : "pointer",
            backgroundColor: isLoading || !text.trim() ? "#333" : "#e50914",
            color: "#fff",
            fontSize: "15px",
            fontWeight: 600,
            opacity: isLoading || !text.trim() ? 0.7 : 1,
            transition: "0.2s",
          }}
        >
          {isLoading ? "Finding..." : "Find Movies"}
        </button>
      </form>

      {/* Error */}
      {error && (
        <div style={{ marginTop: 10, color: "#ff6b6b", fontSize: 14 }}>
          {error}
        </div>
      )}

      {/* Meta info */}
      {data && !error && (
        <div
          style={{
            marginTop: 16,
            background: "rgba(255, 255, 255, 0.06)",
            padding: "12px 14px",
            borderRadius: 8,
            border: "1px solid rgba(255,255,255,0.12)",
            color: "#fff",
            fontSize: 14,
            display: "flex",
            flexWrap: "wrap",
            gap: "10px",
            alignItems: "center",
          }}
        >
          <span style={{ opacity: 0.9 }}>
            Sentiment: <strong>{data.sentiment}</strong>
          </span>
          <span style={{ opacity: 0.9 }}>
            Tag: <strong>{data.tag}</strong>
          </span>
          {confidencePct !== null && (
            <span style={{ opacity: 0.9 }}>
              Confidence: <strong>{confidencePct}%</strong>
            </span>
          )}
        </div>
      )}

      {/* Results grid */}
      {data?.results && data.results.length > 0 && (
        <div style={{ marginTop: "18px" }}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))",
              gap: "18px",
            }}
          >
            {data.results.map((movie) => (
              <div
                key={movie.id}
                onClick={() => navigate(`/movie/${movie.id}`)}
                style={{
                  cursor: "pointer",
                  textAlign: "center",
                  transition: "transform 0.2s ease, opacity 0.2s ease",
                }}
                onMouseEnter={(e) => {
                  (e.currentTarget as HTMLDivElement).style.transform = "scale(1.03)";
                  (e.currentTarget as HTMLDivElement).style.opacity = "0.95";
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLDivElement).style.transform = "scale(1)";
                  (e.currentTarget as HTMLDivElement).style.opacity = "1";
                }}
              >
                {movie.poster_path ? (
                  <img
                    src={`https://image.tmdb.org/t/p/w300${movie.poster_path}`}
                    alt={movie.title || "Movie poster"}
                    style={{
                      width: "100%",
                      borderRadius: "10px",
                      marginBottom: "8px",
                      boxShadow: "0 4px 12px rgba(0,0,0,0.4)",
                    }}
                  />
                ) : (
                  <div
                    style={{
                      width: "100%",
                      height: "240px",
                      borderRadius: "10px",
                      background: "#333",
                      marginBottom: "8px",
                    }}
                  />
                )}

                <p style={{ marginTop: "4px", fontSize: "14px", fontWeight: 600 }}>
                  {movie.title || movie.original_title || "Untitled"}
                </p>

                {movie.release_date && (
                  <p style={{ fontSize: "12px", opacity: 0.7 }}>
                    {movie.release_date}
                  </p>
                )}

                {movie.vote_average != null && (
                  <p style={{ fontSize: "12px", opacity: 0.85 }}>
                    ⭐ {Number(movie.vote_average).toFixed(1)}
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Empty state */}
      {data && (!data.results || data.results.length === 0) && !error && (
        <p style={{ marginTop: "16px", opacity: 0.8, color: "#fff" }}>
          No movies found for this mood. Try a different description.
        </p>
      )}
    </div>
  );
};

export default SentimentWidget;
