// src/pages/HomePage.tsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  fetchExplore,
  type ExploreMovie,
  type ExploreResponse,
} from "../api/movies";

// Helper types
type TmdbPerson = {
  id: number;
  name: string;
  // Υποστηρίζουμε ΚΑΙ snake_case ΚΑΙ camelCase
  known_for_department?: string;
  knownForDepartment?: string;
};

type TmdbGenre = {
  id: number;
  name: string;
};

// ===============================
//  Helper API calls (actors / directors / genres)
// ===============================
async function searchPerson(query: string): Promise<TmdbPerson[]> {
  if (!query.trim()) return [];
  const res = await fetch(
    `http://localhost:8080/movies/person/search?query=${encodeURIComponent(
      query
    )}`
  );
  if (!res.ok) return [];
  const data = await res.json();
  return (data.results || []) as TmdbPerson[];
}

async function fetchGenres(): Promise<TmdbGenre[]> {
  const res = await fetch("http://localhost:8080/movies/genres");
  if (!res.ok) return [];
  const data = await res.json();
  return (data.genres || []) as TmdbGenre[];
}

const VISIBLE_COUNT = 5;

// helper για το department
function getDepartment(p: TmdbPerson): string {
  return p.known_for_department ?? p.knownForDepartment ?? "";
}

export default function HomePage() {
  const navigate = useNavigate();

  // Genres sidebar
  const [genres, setGenres] = useState<TmdbGenre[]>([]);
  const [selectedGenreId, setSelectedGenreId] = useState<number | null>(null); // null = All

  // Base filters
  const [sortBy, setSortBy] = useState<string>("popularity.desc");
  const [yearFrom, setYearFrom] = useState<string>("");
  const [yearTo, setYearTo] = useState<string>("");
  const [minRating, setMinRating] = useState<string>("");

  // Actor / Director filters
  const [actorQuery, setActorQuery] = useState("");
  const [directorQuery, setDirectorQuery] = useState("");
  const [actorId, setActorId] = useState<number | null>(null);
  const [directorId, setDirectorId] = useState<number | null>(null);
  const [actorSuggestions, setActorSuggestions] = useState<TmdbPerson[]>([]);
  const [directorSuggestions, setDirectorSuggestions] =
    useState<TmdbPerson[]>([]);

  // Movies per genre row
  const [moviesByGenre, setMoviesByGenre] = useState<
    Record<number, ExploreMovie[]>
  >({});
  const [pageByGenre, setPageByGenre] = useState<Record<number, number>>({});
  const [totalPagesByGenre, setTotalPagesByGenre] = useState<
    Record<number, number>
  >({});
  const [offsetByGenre, setOffsetByGenre] = useState<Record<number, number>>(
    {}
  );
  const [rowLoading, setRowLoading] = useState<Record<number, boolean>>({});

  // Global UI state
  const [initialLoading, setInitialLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // ===============================
  //  Load genres once
  // ===============================
  useEffect(() => {
    let mounted = true;
    fetchGenres()
      .then((list) => {
        if (!mounted) return;
        setGenres(list);
      })
      .catch(() => {
        // ignore
      });
    return () => {
      mounted = false;
    };
  }, []);

  // ===============================
  //  Utility: parse numeric filters
  // ===============================
  const getNumericFilters = () => {
    const numericYearFrom = yearFrom.trim() !== "" ? Number(yearFrom) : null;
    const numericYearTo = yearTo.trim() !== "" ? Number(yearTo) : null;
    const numericMinRating =
      minRating.trim() !== "" ? Number(minRating) : null;

    return { numericYearFrom, numericYearTo, numericMinRating };
  };

  // ===============================
  //  Load movies for a single genre row
  // ===============================
  const loadGenreMovies = async (genreId: number, pageToLoad: number) => {
    const { numericYearFrom, numericYearTo, numericMinRating } =
      getNumericFilters();

    setRowLoading((prev) => ({ ...prev, [genreId]: true }));

    try {
      const data: ExploreResponse = await fetchExplore({
        page: pageToLoad,
        sortBy,
        yearFrom: numericYearFrom ?? undefined,
        yearTo: numericYearTo ?? undefined,
        minRating: numericMinRating ?? undefined,
        castId: actorId ?? undefined,
        crewId: directorId ?? undefined,
        genreId,
      });

      const results = data.results || [];

      setMoviesByGenre((prev) => ({
        ...prev,
        [genreId]:
          pageToLoad === 1 ? results : [...(prev[genreId] || []), ...results],
      }));

      setPageByGenre((prev) => ({ ...prev, [genreId]: pageToLoad }));
      setTotalPagesByGenre((prev) => ({
        ...prev,
        [genreId]: data.total_pages ?? 1,
      }));

      if (pageToLoad === 1) {
        setOffsetByGenre((prev) => ({ ...prev, [genreId]: 0 }));
      }
    } catch (e) {
      console.error("Failed to load movies for genre", genreId, e);
      setError("Something went wrong while loading movies.");
    } finally {
      setRowLoading((prev) => ({ ...prev, [genreId]: false }));
    }
  };

  // ===============================
  //  Load initial movies whenever filters or selected genre change
  // ===============================
  useEffect(() => {
    if (genres.length === 0) return;

    setInitialLoading(true);
    setError(null);

    // Reset όλων των rows
    setMoviesByGenre({});
    setPageByGenre({});
    setTotalPagesByGenre({});
    setOffsetByGenre({});
    setRowLoading({});

    const genreIdsToLoad: number[] = selectedGenreId
      ? [selectedGenreId]
      : genres.map((g) => g.id);

    Promise.all(genreIdsToLoad.map((id) => loadGenreMovies(id, 1))).finally(
      () => setInitialLoading(false)
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    sortBy,
    yearFrom,
    yearTo,
    minRating,
    actorId,
    directorId,
    selectedGenreId,
    genres,
  ]);

  // ===============================
  //  Actor autocomplete
  // ===============================
  useEffect(() => {
    if (actorQuery.length < 2) {
      setActorSuggestions([]);
      return;
    }

    const timeout = setTimeout(() => {
      searchPerson(actorQuery).then((list) => {
        setActorSuggestions(
          list.filter((p) => getDepartment(p) === "Acting")
        );
      });
    }, 300);

    return () => clearTimeout(timeout);
  }, [actorQuery]);

  // ===============================
  //  Director autocomplete
  // ===============================
  useEffect(() => {
    if (directorQuery.length < 2) {
      setDirectorSuggestions([]);
      return;
    }

    const timeout = setTimeout(() => {
      searchPerson(directorQuery).then((list) => {
        setDirectorSuggestions(
          list.filter((p) => getDepartment(p) === "Directing")
        );
      });
    }, 300);

    return () => clearTimeout(timeout);
  }, [directorQuery]);

  // ===============================
  //  Row navigation handlers
  // ===============================
  const handlePrev = (genreId: number) => {
    setOffsetByGenre((prev) => {
      const current = prev[genreId] ?? 0;
      const next = Math.max(0, current - VISIBLE_COUNT);
      return { ...prev, [genreId]: next };
    });
  };

  const handleNext = async (genreId: number) => {
    const movies = moviesByGenre[genreId] || [];
    const offset = offsetByGenre[genreId] ?? 0;
    const page = pageByGenre[genreId] ?? 1;
    const totalPages = totalPagesByGenre[genreId] ?? 1;

    const canScroll = offset + VISIBLE_COUNT < movies.length;
    if (canScroll) {
      setOffsetByGenre((prev) => ({
        ...prev,
        [genreId]: (prev[genreId] ?? 0) + VISIBLE_COUNT,
      }));
      return;
    }

    if (page < totalPages && !rowLoading[genreId]) {
      await loadGenreMovies(genreId, page + 1);
      setOffsetByGenre((prev) => ({
        ...prev,
        [genreId]: (prev[genreId] ?? 0) + VISIBLE_COUNT,
      }));
    }
  };

  const canGoPrev = (genreId: number) =>
    (offsetByGenre[genreId] ?? 0) > 0;

  const canGoNext = (genreId: number) => {
    const movies = moviesByGenre[genreId] || [];
    const offset = offsetByGenre[genreId] ?? 0;
    const page = pageByGenre[genreId] ?? 1;
    const totalPages = totalPagesByGenre[genreId] ?? 1;

    return offset + VISIBLE_COUNT < movies.length || page < totalPages;
  };

  // Genres που φαίνονται στο κέντρο
  const visibleGenres: TmdbGenre[] = selectedGenreId
    ? genres.filter((g) => g.id === selectedGenreId)
    : genres;

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "260px 1fr",
        gap: "24px",
        padding: "40px 24px",
        color: "white",
        // full-bleed ώστε το sidebar να πάει πιο αριστερά
        width: "100vw",
        marginLeft: "calc(50% - 50vw)",
        marginRight: "calc(50% - 50vw)",
      }}
    >
      {/* ================== SIDEBAR ================== */}
      <aside
        style={{
          borderRight: "1px solid #333",
          paddingRight: "16px",
        }}
      >
        <h2 style={{ marginBottom: "16px", fontSize: "24px" }}>Genres</h2>
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "6px",
            maxHeight: "70vh",
            overflowY: "auto",
          }}
        >
          <button
            onClick={() => setSelectedGenreId(null)}
            style={{
              textAlign: "left",
              padding: "8px 12px",
              borderRadius: "6px",
              border: "none",
              cursor: "pointer",
              background:
                selectedGenreId === null ? "#e50914" : "transparent",
              color: selectedGenreId === null ? "white" : "#ddd",
              fontWeight: selectedGenreId === null ? "bold" : "normal",
            }}
          >
            All genres
          </button>
          {genres.map((g) => (
            <button
              key={g.id}
              onClick={() => setSelectedGenreId(g.id)}
              style={{
                textAlign: "left",
                padding: "8px 12px",
                borderRadius: "6px",
                border: "none",
                cursor: "pointer",
                background:
                  selectedGenreId === g.id ? "#e50914" : "transparent",
                color: selectedGenreId === g.id ? "white" : "#ddd",
                fontWeight: selectedGenreId === g.id ? "bold" : "normal",
              }}
            >
              {g.name}
            </button>
          ))}
        </div>
      </aside>

      {/* ================== MAIN CONTENT ================== */}
      <main>
        {/* HERO με CineMatch + Explore */}
        <div
          style={{
            textAlign: "center",
            marginBottom: "40px",
            marginTop: "-16px",
          }}
        >
          <h1
            style={{
              fontSize: "64px",
              fontWeight: "bold",
              marginBottom: "8px",
            }}
          >
            CineMatch
          </h1>
          <p
            style={{
              opacity: 0.8,
              fontSize: "18px",
              marginBottom: "24px",
            }}
          >
            Discover the world of movies, tailored for you.
          </p>

          <h2
            style={{
              fontSize: "32px",
              fontWeight: "bold",
              marginBottom: "6px",
            }}
          >
            Explore Movies
          </h2>
          <p style={{ opacity: 0.8 }}>
            Filter by year, rating, actors, directors & genres.
          </p>
        </div>

        {/* FILTERS ROW */}
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "16px",
            marginBottom: "24px",
            alignItems: "flex-start",
          }}
        >
          {/* Sort By */}
          <div style={{ minWidth: "200px" }}>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontSize: "14px",
              }}
            >
              Sort by
            </label>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              style={{
                width: "100%",
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #444",
                background: "#222",
                color: "white",
                fontSize: "14px",
              }}
            >
              <option value="popularity.desc">Popularity ↓</option>
              <option value="popularity.asc">Popularity ↑</option>
              <option value="vote_average.desc">Rating ↓</option>
              <option value="vote_average.asc">Rating ↑</option>
              <option value="primary_release_date.desc">Newest</option>
              <option value="primary_release_date.asc">Oldest</option>
            </select>
          </div>

          {/* Year From */}
          <div style={{ minWidth: "120px" }}>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontSize: "14px",
              }}
            >
              Year from
            </label>
            <input
              type="number"
              value={yearFrom}
              onChange={(e) => setYearFrom(e.target.value)}
              placeholder="e.g. 2000"
              style={{
                width: "100%",
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #444",
                background: "#222",
                color: "white",
                fontSize: "14px",
              }}
            />
          </div>

          {/* Year To */}
          <div style={{ minWidth: "120px" }}>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontSize: "14px",
              }}
            >
              Year to
            </label>
            <input
              type="number"
              value={yearTo}
              onChange={(e) => setYearTo(e.target.value)}
              placeholder="e.g. 2025"
              style={{
                width: "100%",
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #444",
                background: "#222",
                color: "white",
                fontSize: "14px",
              }}
            />
          </div>

          {/* Min Rating */}
          <div style={{ minWidth: "140px" }}>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontSize: "14px",
              }}
            >
              Min rating (0–10)
            </label>
            <input
              type="number"
              min={0}
              max={10}
              step={0.1}
              value={minRating}
              onChange={(e) => setMinRating(e.target.value)}
              placeholder="e.g. 7.5"
              style={{
                width: "100%",
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #444",
                background: "#222",
                color: "white",
                fontSize: "14px",
              }}
            />
          </div>

          {/* Actor */}
          <div style={{ minWidth: "200px", position: "relative" }}>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontSize: "14px",
              }}
            >
              Actors
            </label>
            <input
              type="text"
              value={actorQuery}
              onChange={(e) => {
                setActorQuery(e.target.value);
                setActorId(null);
              }}
              placeholder="Search actor..."
              style={{
                width: "100%",
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #444",
                background: "#222",
                color: "white",
                fontSize: "14px",
              }}
            />
            {actorSuggestions.length > 0 && (
              <div
                style={{
                  position: "absolute",
                  top: "60px",
                  left: 0,
                  width: "100%",
                  background: "#111",
                  border: "1px solid #444",
                  borderRadius: "8px",
                  maxHeight: "200px",
                  overflowY: "auto",
                  zIndex: 20,
                }}
              >
                {actorSuggestions.map((p) => (
                  <div
                    key={p.id}
                    onClick={() => {
                      setActorId(p.id);
                      setActorQuery(p.name);
                      setActorSuggestions([]);
                    }}
                    style={{
                      padding: "8px",
                      cursor: "pointer",
                      borderBottom: "1px solid #333",
                    }}
                  >
                    {p.name}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Director */}
          <div style={{ minWidth: "200px", position: "relative" }}>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontSize: "14px",
              }}
            >
              Directors
            </label>
            <input
              type="text"
              value={directorQuery}
              onChange={(e) => {
                setDirectorQuery(e.target.value);
                setDirectorId(null);
              }}
              placeholder="Search director..."
              style={{
                width: "100%",
                padding: "8px 10px",
                borderRadius: "8px",
                border: "1px solid #444",
                background: "#222",
                color: "white",
                fontSize: "14px",
              }}
            />
            {directorSuggestions.length > 0 && (
              <div
                style={{
                  position: "absolute",
                  top: "60px",
                  left: 0,
                  width: "100%",
                  background: "#111",
                  border: "1px solid #444",
                  borderRadius: "8px",
                  maxHeight: "200px",
                  overflowY: "auto",
                  zIndex: 20,
                }}
              >
                {directorSuggestions.map((p) => (
                  <div
                    key={p.id}
                    onClick={() => {
                      setDirectorId(p.id);
                      setDirectorQuery(p.name);
                      setDirectorSuggestions([]);
                    }}
                    style={{
                      padding: "8px",
                      cursor: "pointer",
                      borderBottom: "1px solid #333",
                    }}
                  >
                    {p.name}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* GLOBAL ERROR / LOADING */}
        {error && (
          <p style={{ color: "#ff6b6b", marginBottom: "16px" }}>{error}</p>
        )}
        {initialLoading && !error && (
          <p style={{ opacity: 0.8, marginBottom: "16px" }}>
            Loading movies...
          </p>
        )}

        {/* GENRE ROWS */}
        {!initialLoading &&
          visibleGenres.map((genre) => {
            const movies = moviesByGenre[genre.id] || [];
            const offset = offsetByGenre[genre.id] ?? 0;
            const visible = movies.slice(offset, offset + VISIBLE_COUNT);

            if (movies.length === 0) {
              return null;
            }

            return (
              <section key={genre.id} style={{ marginBottom: "40px" }}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    marginBottom: "10px",
                  }}
                >
                  <h2
                    style={{
                      fontSize: "24px",
                      borderLeft: "4px solid #e50914",
                      paddingLeft: "10px",
                    }}
                  >
                    {genre.name}
                  </h2>

                  <div style={{ display: "flex", gap: "8px" }}>
                    <button
                      onClick={() => handlePrev(genre.id)}
                      disabled={!canGoPrev(genre.id)}
                      style={{
                        width: "32px",
                        height: "32px",
                        borderRadius: "16px",
                        border: "none",
                        cursor: canGoPrev(genre.id)
                          ? "pointer"
                          : "default",
                        background: canGoPrev(genre.id) ? "#333" : "#222",
                        color: "white",
                        opacity: canGoPrev(genre.id) ? 1 : 0.5,
                      }}
                    >
                      ⟵
                    </button>
                    <button
                      onClick={() => handleNext(genre.id)}
                      disabled={!canGoNext(genre.id)}
                      style={{
                        width: "32px",
                        height: "32px",
                        borderRadius: "16px",
                        border: "none",
                        cursor: canGoNext(genre.id)
                          ? "pointer"
                          : "default",
                        background: canGoNext(genre.id) ? "#333" : "#222",
                        color: "white",
                        opacity: canGoNext(genre.id) ? 1 : 0.5,
                      }}
                    >
                      {rowLoading[genre.id] ? "…" : "⟶"}
                    </button>
                  </div>
                </div>

                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
                    gap: "20px",
                  }}
                >
                  {visible.map((movie) => (
                    <div
                      key={movie.id}
                      onClick={() => navigate(`/movie/${movie.id}`)}
                      style={{
                        cursor: "pointer",
                        textAlign: "center",
                        transition:
                          "transform 0.2s ease, opacity 0.2s ease",
                      }}
                      onMouseEnter={(e) => {
                        (e.currentTarget as HTMLDivElement).style.transform =
                          "scale(1.03)";
                        (e.currentTarget as HTMLDivElement).style.opacity =
                          "0.95";
                      }}
                      onMouseLeave={(e) => {
                        (e.currentTarget as HTMLDivElement).style.transform =
                          "scale(1)";
                        (e.currentTarget as HTMLDivElement).style.opacity =
                          "1";
                      }}
                    >
                      {movie.poster_path ? (
                        <img
                          src={`https://image.tmdb.org/t/p/w300${movie.poster_path}`}
                          alt={movie.title}
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
                            height: "270px",
                            borderRadius: "10px",
                            background: "#333",
                            marginBottom: "8px",
                          }}
                        />
                      )}

                      <p
                        style={{
                          marginTop: "4px",
                          fontSize: "14px",
                          fontWeight: 500,
                        }}
                      >
                        {movie.title}
                      </p>
                      {movie.release_date && (
                        <p
                          style={{
                            fontSize: "12px",
                            opacity: 0.7,
                          }}
                        >
                          {movie.release_date}
                        </p>
                      )}
                      {movie.vote_average != null && (
                        <p
                          style={{
                            fontSize: "12px",
                            opacity: 0.8,
                          }}
                        >
                          ⭐ {movie.vote_average.toFixed(1)}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </section>
            );
          })}
      </main>
    </div>
  );
}