import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";      // 👈 ΧΡΕΙΑΖΕΤΑΙ!!!
import { fetchTrending } from "../api/movies";
import type { TrendingMovie } from "../api/movies";
import "../styles/TrendingPage.css";

export default function TrendingPage() {
  const [movies, setMovies] = useState<TrendingMovie[]>([]);
  const [timeWindow, setTimeWindow] = useState<"day" | "week">("day");
  const navigate = useNavigate();  // 👈 σωστό

  useEffect(() => {
    fetchTrending(timeWindow)
      .then((data) => setMovies(data))
      .catch(() => alert("Failed to load movies"));
  }, [timeWindow]);

  return (
    <div className="trending-container">
      <h1 className="title">Trending Movies</h1>

      <div className="movies-grid">
        {movies.map((movie) => (
          <div
            className="movie-card"
            key={movie.id}
            onClick={() => navigate(`/movie/${movie.id}`)}   // 👈 ΤΩΡΑ ΣΩΣΤΟ
            style={{ cursor: "pointer" }}
          >
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
