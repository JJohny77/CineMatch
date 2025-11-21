import { useState } from "react";
import { useNavigate } from "react-router-dom";

type SearchMovie = {
  id: number;
  title: string;
  poster_path: string | null;
  overview: string;
};

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchMovie[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  const handleSearch = async () => {
    const trimmed = query.trim();
    if (!trimmed) return;

    setLoading(true);
    setError(null);

    try {
      const res = await fetch(
        `http://localhost:8080/movies/search?query=${encodeURIComponent(
          trimmed
        )}`
      );

      if (!res.ok) {
        throw new Error("Search request failed");
      }

      const data = await res.json();
      setResults(data.results || []);
    } catch (e) {
      console.error("Search error:", e);
      setError("Something went wrong while searching. Please try again.");
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const isSearchDisabled = loading || !query.trim();

  return (
    <div
      style={{
        padding: "40px 24px",
        color: "white",
        maxWidth: "1200px",
        margin: "0 auto",
      }}
    >
      <h1 style={{ marginBottom: "10px", fontSize: "32px" }}>Search Movies</h1>
      <p style={{ opacity: 0.8, marginBottom: "25px" }}>
        Type a movie title to explore details, genres and trailers.
      </p>

      {/* Search Bar */}
      <div
        style={{
          display: "flex",
          gap: "12px",
          marginBottom: "20px",
          alignItems: "center",
        }}
      >
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSearch()}
          placeholder="Search for a movie..."
          style={{
            padding: "12px 16px",
            flex: 1,
            maxWidth: "420px",
            borderRadius: "8px",
            border: "1px solid #444",
            background: "#222",
            color: "white",
            fontSize: "16px",
          }}
          disabled={loading}
        />

        <button
          onClick={handleSearch}
          disabled={isSearchDisabled}
          style={{
            padding: "12px 20px",
            borderRadius: "8px",
            background: "#e50914",
            color: "white",
            border: "none",
            cursor: isSearchDisabled ? "default" : "pointer",
            fontWeight: "bold",
            fontSize: "16px",
            opacity: isSearchDisabled ? 0.6 : 1,
            transition: "opacity 0.2s ease, transform 0.2s ease",
          }}
          onMouseEnter={(e) => {
            if (isSearchDisabled) return;
            (e.currentTarget as HTMLButtonElement).style.opacity = "0.85";
            (e.currentTarget as HTMLButtonElement).style.transform =
              "translateY(-1px)";
          }}
          onMouseLeave={(e) => {
            (e.currentTarget as HTMLButtonElement).style.opacity = "1";
            (e.currentTarget as HTMLButtonElement).style.transform =
              "translateY(0)";
          }}
        >
          {loading ? "Searching..." : "Search"}
        </button>
      </div>

      {/* Error */}
      {error && (
        <p style={{ color: "#ff6b6b", marginBottom: "10px" }}>{error}</p>
      )}

      {/* Loading */}
      {loading && !error && (
        <p style={{ opacity: 0.8, marginBottom: "10px" }}>Loading...</p>
      )}

      {/* No Results */}
      {!loading && !error && results.length === 0 && query.trim() !== "" && (
        <p style={{ opacity: 0.7, marginBottom: "10px" }}>No movies found.</p>
      )}

      {/* Results title */}
      {!loading && !error && results.length > 0 && (
        <p style={{ opacity: 0.8, marginBottom: "15px" }}>
          Results for <strong>"{query.trim()}"</strong> — {results.length}{" "}
          {results.length === 1 ? "movie" : "movies"}
        </p>
      )}

      {/* Results Grid */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
          gap: "20px",
        }}
      >
        {results.map((movie) => (
          <div
            key={movie.id}
            onClick={() => navigate(`/movie/${movie.id}`)}
            style={{
              cursor: "pointer",
              textAlign: "center",
              color: "white",
              transition: "transform 0.2s ease, opacity 0.2s ease",
            }}
            onMouseEnter={(e) => {
              (e.currentTarget as HTMLDivElement).style.transform =
                "scale(1.06)";
              (e.currentTarget as HTMLDivElement).style.opacity = "0.9";
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLDivElement).style.transform =
                "scale(1)";
              (e.currentTarget as HTMLDivElement).style.opacity = "1";
            }}
          >
            {movie.poster_path ? (
              <img
                src={`https://image.tmdb.org/t/p/w300${movie.poster_path}`}
                alt={movie.title}
                style={{
                  width: "100%",
                  borderRadius: "10px",
                  marginBottom: "10px",
                  boxShadow: "0 4px 12px rgba(0,0,0,0.4)",
                }}
              />
            ) : (
              <div
                style={{
                  width: "100%",
                  height: "270px",
                  borderRadius: "10px",
                  background: "#333",
                  marginBottom: "10px",
                }}
              />
            )}

            <p style={{ marginTop: "8px", fontSize: "16px" }}>{movie.title}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
