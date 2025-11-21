import { useEffect, useState } from "react";
import { fetchTrending } from "../api/movies";        // κανονικό value import
import type { TrendingMovie } from "../api/movies";   // type-only import
import "../styles/TrendingPage.css";

export default function TrendingPage() {
  const [movies, setMovies] = useState<TrendingMovie[]>([]);
  const [timeWindow, setTimeWindow] = useState<"day" | "week">("day");
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);

    fetchTrending(timeWindow)
      .then((data) => setMovies(data))
      .catch(() => setError("Failed to load trending movies"))
      .finally(() => setLoading(false));
  }, [timeWindow]);

  return (
    <div className="trending-container">
      <h1 className="title">Trending Movies</h1>

      <div className="filter-buttons">
        <button
          className={timeWindow === "day" ? "active" : ""}
          onClick={() => setTimeWindow("day")}
        >
          Day
        </button>
        <button
          className={timeWindow === "week" ? "active" : ""}
          onClick={() => setTimeWindow("week")}
        >
          Week
        </button>
      </div>

      {loading && <p>Loading...</p>}
      {error && <p className="error">{error}</p>}

      <div className="movies-grid">
        {!loading &&
          !error &&
          movies.map((movie, index) => (
            <div className="movie-card" key={index}>
              <img
                src={`https://image.tmdb.org/t/p/w500${movie.posterPath}`}
                alt={movie.title}
                className="movie-poster"
              />
              <h3>{movie.title}</h3>
            </div>
          ))}
      </div>
    </div>
  );
}

