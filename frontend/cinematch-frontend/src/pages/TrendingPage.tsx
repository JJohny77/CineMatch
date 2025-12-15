import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  fetchTrending,
  fetchTrendingActors,
  fetchTrendingDirectors,
} from "../api/movies";
import type { TrendingMovie, TrendingPerson } from "../api/movies";

import "../styles/TrendingPage.css";

export default function TrendingPage() {
  const navigate = useNavigate();

  // 3 tabs
  const [tab, setTab] = useState<"movies" | "actors" | "directors">("movies");

  // time window
  const [timeWindow, setTimeWindow] = useState<"day" | "week">("day");

  // data states
  const [movies, setMovies] = useState<TrendingMovie[]>([]);
  const [actors, setActors] = useState<TrendingPerson[]>([]);
  const [directors, setDirectors] = useState<TrendingPerson[]>([]);

  // Load data whenever tab or timeWindow changes
  useEffect(() => {
    if (tab === "movies") {
      fetchTrending(timeWindow)
        .then((data) => setMovies(data))
        .catch(() => alert("Failed to load trending movies"));
    } else if (tab === "actors") {
      fetchTrendingActors(timeWindow)
        .then((data) => setActors(data))
        .catch(() => alert("Failed to load trending actors"));
    } else if (tab === "directors") {
      fetchTrendingDirectors(timeWindow)
        .then((data) => setDirectors(data))
        .catch(() => alert("Failed to load trending directors"));
    }
  }, [tab, timeWindow]);

  return (
    <div className="trending-container">
      <h1 className="title">Trending</h1>

      {/* TAB BUTTONS */}
      <div className="filter-buttons">
        <button
          className={tab === "movies" ? "active" : ""}
          onClick={() => setTab("movies")}
        >
          Movies
        </button>

        <button
          className={tab === "actors" ? "active" : ""}
          onClick={() => setTab("actors")}
        >
          Actors
        </button>

        <button
          className={tab === "directors" ? "active" : ""}
          onClick={() => setTab("directors")}
        >
          Directors
        </button>
      </div>

      {/* TIME WINDOW BUTTONS */}
      <div className="filter-buttons">
        <button
          className={timeWindow === "day" ? "active" : ""}
          onClick={() => setTimeWindow("day")}
        >
          Today
        </button>

        <button
          className={timeWindow === "week" ? "active" : ""}
          onClick={() => setTimeWindow("week")}
        >
          This Week
        </button>
      </div>

      {/* MOVIES GRID */}
      {tab === "movies" && (
        <div className="movies-grid">
          {movies.map((movie) => (
            <div
              className="movie-card"
              key={movie.id}
              onClick={() => navigate(`/movie/${movie.id}`)}
            >
              <img
                src={`https://image.tmdb.org/t/p/w500${movie.posterPath}`}
                alt={movie.title}
              />
              <h3>{movie.title}</h3>
            </div>
          ))}
        </div>
      )}

      {/* ACTORS GRID */}
      {tab === "actors" && (
        <div className="movies-grid">
          {actors.map((actor) => (
            <div
              className="movie-card"
              key={actor.id}
              onClick={() => navigate(`/actor/${actor.id}`)}
            >
              <img
                src={
                  actor.profilePath
                    ? `https://image.tmdb.org/t/p/w500${actor.profilePath}`
                    : "/no-image.png"
                }
                alt={actor.name}
              />
              <h3>{actor.name}</h3>
            </div>
          ))}
        </div>
      )}

      {/* DIRECTORS GRID */}
      {tab === "directors" && (
        <div className="movies-grid">
          {directors.map((director) => (
            <div
              className="movie-card"
              key={director.id}
              onClick={() => navigate(`/director/${director.id}`)}
            >
              <img
                src={
                  director.profilePath
                    ? `https://image.tmdb.org/t/p/w500${director.profilePath}`
                    : "/no-image.png"
                }
                alt={director.name}
              />
              <h3>{director.name}</h3>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
