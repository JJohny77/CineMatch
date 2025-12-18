// src/pages/HomePage.tsx
import React, { useEffect, useState, useRef } from "react";
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
  known_for_department?: string;
  knownForDepartment?: string;
};

type TmdbGenre = {
  id: number;
  name: string;
};

type PreferenceScoreDto = {
  id: number;
  score: number;
};

type UserPreferencesResponseDto = {
  topGenres?: PreferenceScoreDto[];
  topActors?: PreferenceScoreDto[];
  topDirectors?: PreferenceScoreDto[];
  lastUpdated?: string;
};

type PersonCard = {
  id: number;
  name: string;
  profile_path?: string | null;
  profilePath?: string | null;
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

function getAuthToken(): string | null {
  return (
    localStorage.getItem("token") ||
    localStorage.getItem("jwt") ||
    localStorage.getItem("accessToken")
  );
}

async function fetchRecommendations(
  token: string | null
): Promise<ExploreMovie[]> {
  if (!token) return [];
  const res = await fetch("http://localhost:8080/movies/recommendations", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  if (!res.ok) return [];
  const data = await res.json();
  return (data.results || []) as ExploreMovie[];
}

async function fetchMyPreferences(
  token: string | null,
  topN: number = 5
): Promise<UserPreferencesResponseDto | null> {
  if (!token) return null;
  const res = await fetch(
    `http://localhost:8080/users/me/preferences?topN=${topN}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
  if (!res.ok) return null;
  return (await res.json()) as UserPreferencesResponseDto;
}

async function fetchActorDetails(
  token: string | null,
  personId: number
): Promise<PersonCard | null> {
  if (!token) return null;
  const res = await fetch(`http://localhost:8080/api/actors/${personId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) return null;
  const data = await res.json();
  return data as PersonCard;
}

async function fetchDirectorDetails(
  token: string | null,
  personId: number
): Promise<PersonCard | null> {
  if (!token) return null;
  const res = await fetch(`http://localhost:8080/api/directors/${personId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) return null;
  const data = await res.json();
  return data as PersonCard;
}

const VISIBLE_COUNT = 5;

// helper για το department
function getDepartment(p: TmdbPerson): string {
  return p.known_for_department ?? p.knownForDepartment ?? "";
}

function getProfilePath(p: PersonCard): string | null {
  return p.profile_path ?? p.profilePath ?? null;
}

export default function HomePage() {
  const navigate = useNavigate();

  // ✅ όταν ο χρήστης αλλάξει κάτι, στο επόμενο reload θα στείλουμε event=true ΜΟΝΟ 1 φορά
  const shouldLogNextReload = useRef(false);

  const markUserAction = () => {
    shouldLogNextReload.current = true;
  };

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
  //  US58: Recommended for You state
  // ===============================
  const [recLoading, setRecLoading] = useState(false);
  const [recError, setRecError] = useState<string | null>(null);

  const [recommendedMovies, setRecommendedMovies] = useState<ExploreMovie[]>(
    []
  );
  const [recMoviesOffset, setRecMoviesOffset] = useState(0);

  const [topActors, setTopActors] = useState<PersonCard[]>([]);
  const [actorsOffset, setActorsOffset] = useState(0);

  const [topDirectors, setTopDirectors] = useState<PersonCard[]>([]);
  const [directorsOffset, setDirectorsOffset] = useState(0);

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
      .catch(() => {});
    return () => {
      mounted = false;
    };
  }, []);

  // ===============================
  //  US58: load recommendations + preferences once
  // ===============================
  useEffect(() => {
    let mounted = true;

    const run = async () => {
      const token = getAuthToken();
      if (!token) return;

      setRecLoading(true);
      setRecError(null);

      try {
        const [movies, prefs] = await Promise.all([
          fetchRecommendations(token),
          fetchMyPreferences(token, 5),
        ]);

        if (!mounted) return;
        setRecommendedMovies(movies || []);
        setRecMoviesOffset(0);

        const actorIds = (prefs?.topActors || []).map((x) => x.id).slice(0, 10);
        const directorIds = (prefs?.topDirectors || [])
          .map((x) => x.id)
          .slice(0, 10);

        const [actors, directors] = await Promise.all([
          Promise.all(actorIds.map((id) => fetchActorDetails(token, id))),
          Promise.all(directorIds.map((id) => fetchDirectorDetails(token, id))),
        ]);

        if (!mounted) return;

        setTopActors((actors.filter(Boolean) as PersonCard[]) || []);
        setActorsOffset(0);

        setTopDirectors((directors.filter(Boolean) as PersonCard[]) || []);
        setDirectorsOffset(0);
      } catch (e) {
        console.error("Failed to load recommendations/preferences", e);
        if (!mounted) return;
        setRecError("Could not load personalized recommendations.");
      } finally {
        if (!mounted) return;
        setRecLoading(false);
      }
    };

    run();

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
  const loadGenreMovies = async (
    genreId: number,
    pageToLoad: number,
    event: boolean
  ) => {
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
        event, // ✅ μόνο όταν θέλουμε να γραφτεί CHOOSE_FILTER
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

    // ✅ αν είναι user action -> γράφουμε event=true ΜΟΝΟ στο 1ο request
    const logThisReload = shouldLogNextReload.current;
    shouldLogNextReload.current = false;

    Promise.all(
      genreIdsToLoad.map((id, idx) =>
        loadGenreMovies(id, 1, logThisReload && idx === 0)
      )
    ).finally(() => setInitialLoading(false));
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
        setActorSuggestions(list.filter((p) => getDepartment(p) === "Acting"));
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
      // ✅ pagination -> event=false
      await loadGenreMovies(genreId, page + 1, false);
      setOffsetByGenre((prev) => ({
        ...prev,
        [genreId]: (prev[genreId] ?? 0) + VISIBLE_COUNT,
      }));
    }
  };

  const canGoPrev = (genreId: number) => (offsetByGenre[genreId] ?? 0) > 0;

  const canGoNext = (genreId: number) => {
    const movies = moviesByGenre[genreId] || [];
    const offset = offsetByGenre[genreId] ?? 0;
    const page = pageByGenre[genreId] ?? 1;
    const totalPages = totalPagesByGenre[genreId] ?? 1;

    return offset + VISIBLE_COUNT < movies.length || page < totalPages;
  };

  // US58 slider helpers (simple slice, no pagination on endpoint)
  const canGoPrevSimple = (offset: number) => offset > 0;
  const canGoNextSimple = (offset: number, len: number) =>
    offset + VISIBLE_COUNT < len;

  // Genres που φαίνονται στο κέντρο
  const visibleGenres: TmdbGenre[] = selectedGenreId
    ? genres.filter((g) => g.id === selectedGenreId)
    : genres;

  const token = getAuthToken();
  const showPersonalizedSection = !!token;

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "260px 1fr",
        gap: "24px",
        padding: "40px 24px",
        color: "white",
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
            onClick={() => {
              markUserAction();
              setSelectedGenreId(null);
            }}
            style={{
              textAlign: "left",
              padding: "8px 12px",
              borderRadius: "6px",
              border: "none",
              cursor: "pointer",
              background: selectedGenreId === null ? "#e50914" : "transparent",
              color: selectedGenreId === null ? "white" : "#ddd",
              fontWeight: selectedGenreId === null ? "bold" : "normal",
            }}
          >
            All genres
          </button>
          {genres.map((g) => (
            <button
              key={g.id}
              onClick={() => {
                markUserAction();
                setSelectedGenreId(g.id);
              }}
              style={{
                textAlign: "left",
                padding: "8px 12px",
                borderRadius: "6px",
                border: "none",
                cursor: "pointer",
                background: selectedGenreId === g.id ? "#e50914" : "transparent",
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
        <div
          style={{
            textAlign: "center",
            marginBottom: "24px",
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

        {/* ✅ FILTERS ROW (moved here: between Explore Movies and Recommended for You) */}
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "16px",
            marginBottom: "28px",
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
              onChange={(e) => {
                markUserAction();
                setSortBy(e.target.value);
              }}
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
            <label style={{ display: "block", marginBottom: "6px", fontSize: "14px" }}>
              Year from
            </label>
            <input
              type="number"
              value={yearFrom}
              onChange={(e) => {
                markUserAction();
                setYearFrom(e.target.value);
              }}
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
            <label style={{ display: "block", marginBottom: "6px", fontSize: "14px" }}>
              Year to
            </label>
            <input
              type="number"
              value={yearTo}
              onChange={(e) => {
                markUserAction();
                setYearTo(e.target.value);
              }}
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
            <label style={{ display: "block", marginBottom: "6px", fontSize: "14px" }}>
              Min rating (0–10)
            </label>
            <input
              type="number"
              min={0}
              max={10}
              step={0.1}
              value={minRating}
              onChange={(e) => {
                markUserAction();
                setMinRating(e.target.value);
              }}
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
            <label style={{ display: "block", marginBottom: "6px", fontSize: "14px" }}>
              Actors
            </label>
            <input
              type="text"
              value={actorQuery}
              onChange={(e) => {
                markUserAction();
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
                      markUserAction();
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
            <label style={{ display: "block", marginBottom: "6px", fontSize: "14px" }}>
              Directors
            </label>
            <input
              type="text"
              value={directorQuery}
              onChange={(e) => {
                markUserAction();
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
                      markUserAction();
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

        {/* ================== US58: RECOMMENDED FOR YOU ================== */}
        {showPersonalizedSection ? (
          <section style={{ marginBottom: "40px" }}>
            <div style={{ marginBottom: "14px" }}>
              <h2
                style={{
                  fontSize: "26px",
                  borderLeft: "4px solid #e50914",
                  paddingLeft: "10px",
                  marginBottom: "6px",
                }}
              >
                Recommended for You
              </h2>
              <p style={{ opacity: 0.75, margin: 0 }}>Based on your taste</p>
              {recError && (
                <p style={{ color: "#ff6b6b", marginTop: "10px" }}>{recError}</p>
              )}
              {recLoading && !recError && (
                <p style={{ opacity: 0.8, marginTop: "10px" }}>
                  Loading personalized content...
                </p>
              )}
            </div>

            {/* Top Movies slider */}
            {recommendedMovies.length > 0 && (
              <div style={{ marginBottom: "26px" }}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    marginBottom: "10px",
                  }}
                >
                  <h3 style={{ fontSize: "20px", margin: 0, opacity: 0.95 }}>
                    Top Movies
                  </h3>

                  <div style={{ display: "flex", gap: "8px" }}>
                    <button
                      onClick={() =>
                        setRecMoviesOffset((cur) =>
                          Math.max(0, cur - VISIBLE_COUNT)
                        )
                      }
                      disabled={recMoviesOffset <= 0}
                      style={{
                        width: "32px",
                        height: "32px",
                        borderRadius: "16px",
                        border: "none",
                        cursor: recMoviesOffset > 0 ? "pointer" : "default",
                        background: recMoviesOffset > 0 ? "#333" : "#222",
                        color: "white",
                        opacity: recMoviesOffset > 0 ? 1 : 0.5,
                      }}
                    >
                      ⟵
                    </button>
                    <button
                      onClick={() =>
                        setRecMoviesOffset((cur) =>
                          canGoNextSimple(cur, recommendedMovies.length)
                            ? cur + VISIBLE_COUNT
                            : cur
                        )
                      }
                      disabled={!canGoNextSimple(recMoviesOffset, recommendedMovies.length)}
                      style={{
                        width: "32px",
                        height: "32px",
                        borderRadius: "16px",
                        border: "none",
                        cursor: canGoNextSimple(recMoviesOffset, recommendedMovies.length)
                          ? "pointer"
                          : "default",
                        background: canGoNextSimple(recMoviesOffset, recommendedMovies.length)
                          ? "#333"
                          : "#222",
                        color: "white",
                        opacity: canGoNextSimple(recMoviesOffset, recommendedMovies.length)
                          ? 1
                          : 0.5,
                      }}
                    >
                      ⟶
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
                  {recommendedMovies
                    .slice(recMoviesOffset, recMoviesOffset + VISIBLE_COUNT)
                    .map((movie) => (
                      <div
                        key={movie.id}
                        onClick={() => navigate(`/movie/${movie.id}`)}
                        style={{
                          cursor: "pointer",
                          textAlign: "center",
                          transition: "transform 0.2s ease, opacity 0.2s ease",
                        }}
                        onMouseEnter={(e) => {
                          (e.currentTarget as HTMLDivElement).style.transform =
                            "scale(1.03)";
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

                        <p style={{ marginTop: "4px", fontSize: "14px", fontWeight: 500 }}>
                          {movie.title}
                        </p>
                        {movie.release_date && (
                          <p style={{ fontSize: "12px", opacity: 0.7 }}>
                            {movie.release_date}
                          </p>
                        )}
                        {movie.vote_average != null && (
                          <p style={{ fontSize: "12px", opacity: 0.8 }}>
                            ⭐ {movie.vote_average.toFixed(1)}
                          </p>
                        )}
                      </div>
                    ))}
                </div>
              </div>
            )}

            {/* Top Actors slider */}
            {topActors.length > 0 && (
              <div style={{ marginBottom: "26px" }}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    marginBottom: "10px",
                  }}
                >
                  <h3 style={{ fontSize: "20px", margin: 0, opacity: 0.95 }}>
                    Top Actors
                  </h3>

                  <div style={{ display: "flex", gap: "8px" }}>
                    <button
                      onClick={() =>
                        setActorsOffset((cur) => Math.max(0, cur - VISIBLE_COUNT))
                      }
                      disabled={actorsOffset <= 0}
                      style={{
                        width: "32px",
                        height: "32px",
                        borderRadius: "16px",
                        border: "none",
                        cursor: actorsOffset > 0 ? "pointer" : "default",
                        background: actorsOffset > 0 ? "#333" : "#222",
                        color: "white",
                        opacity: actorsOffset > 0 ? 1 : 0.5,
                      }}
                    >
                      ⟵
                    </button>
                    <button
                      onClick={() =>
                        setActorsOffset((cur) =>
                          canGoNextSimple(cur, topActors.length) ? cur + VISIBLE_COUNT : cur
                        )
                      }
                      disabled={!canGoNextSimple(actorsOffset, topActors.length)}
                      style={{
                        width: "32px",
                        height: "32px",
                        borderRadius: "16px",
                        border: "none",
                        cursor: canGoNextSimple(actorsOffset, topActors.length)
                          ? "pointer"
                          : "default",
                        background: canGoNextSimple(actorsOffset, topActors.length)
                          ? "#333"
                          : "#222",
                        color: "white",
                        opacity: canGoNextSimple(actorsOffset, topActors.length) ? 1 : 0.5,
                      }}
                    >
                      ⟶
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
                  {topActors
                    .slice(actorsOffset, actorsOffset + VISIBLE_COUNT)
                    .map((p) => {
                      const path = getProfilePath(p);
                      return (
                        <div
                          key={p.id}
                          onClick={() => navigate(`/actor/${p.id}`)}
                          style={{
                            cursor: "pointer",
                            textAlign: "center",
                            transition: "transform 0.2s ease, opacity 0.2s ease",
                          }}
                          onMouseEnter={(e) => {
                            (e.currentTarget as HTMLDivElement).style.transform =
                              "scale(1.03)";
                            (e.currentTarget as HTMLDivElement).style.opacity = "0.95";
                          }}
                          onMouseLeave={(e) => {
                            (e.currentTarget as HTMLDivElement).style.transform = "scale(1)";
                            (e.currentTarget as HTMLDivElement).style.opacity = "1";
                          }}
                        >
                          {path ? (
                            <img
                              src={`https://image.tmdb.org/t/p/w300${path}`}
                              alt={p.name}
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
                          <p style={{ marginTop: "4px", fontSize: "14px", fontWeight: 600 }}>
                            {p.name}
                          </p>
                        </div>
                      );
                    })}
                </div>
              </div>
            )}

            {/* Top Directors slider */}
            {topDirectors.length > 0 && (
              <div style={{ marginBottom: "10px" }}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    marginBottom: "10px",
                  }}
                >
                  <h3 style={{ fontSize: "20px", margin: 0, opacity: 0.95 }}>
                    Top Directors
                  </h3>

                  <div style={{ display: "flex", gap: "8px" }}>
                    <button
                      onClick={() =>
                        setDirectorsOffset((cur) => Math.max(0, cur - VISIBLE_COUNT))
                      }
                      disabled={directorsOffset <= 0}
                      style={{
                        width: "32px",
                        height: "32px",
                        borderRadius: "16px",
                        border: "none",
                        cursor: directorsOffset > 0 ? "pointer" : "default",
                        background: directorsOffset > 0 ? "#333" : "#222",
                        color: "white",
                        opacity: directorsOffset > 0 ? 1 : 0.5,
                      }}
                    >
                      ⟵
                    </button>
                    <button
                      onClick={() =>
                        setDirectorsOffset((cur) =>
                          canGoNextSimple(cur, topDirectors.length)
                            ? cur + VISIBLE_COUNT
                            : cur
                        )
                      }
                      disabled={!canGoNextSimple(directorsOffset, topDirectors.length)}
                      style={{
                        width: "32px",
                        height: "32px",
                        borderRadius: "16px",
                        border: "none",
                        cursor: canGoNextSimple(directorsOffset, topDirectors.length)
                          ? "pointer"
                          : "default",
                        background: canGoNextSimple(directorsOffset, topDirectors.length)
                          ? "#333"
                          : "#222",
                        color: "white",
                        opacity: canGoNextSimple(directorsOffset, topDirectors.length)
                          ? 1
                          : 0.5,
                      }}
                    >
                      ⟶
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
                  {topDirectors
                    .slice(directorsOffset, directorsOffset + VISIBLE_COUNT)
                    .map((p) => {
                      const path = getProfilePath(p);
                      return (
                        <div
                          key={p.id}
                          onClick={() => navigate(`/director/${p.id}`)}
                          style={{
                            cursor: "pointer",
                            textAlign: "center",
                            transition: "transform 0.2s ease, opacity 0.2s ease",
                          }}
                          onMouseEnter={(e) => {
                            (e.currentTarget as HTMLDivElement).style.transform =
                              "scale(1.03)";
                            (e.currentTarget as HTMLDivElement).style.opacity = "0.95";
                          }}
                          onMouseLeave={(e) => {
                            (e.currentTarget as HTMLDivElement).style.transform = "scale(1)";
                            (e.currentTarget as HTMLDivElement).style.opacity = "1";
                          }}
                        >
                          {path ? (
                            <img
                              src={`https://image.tmdb.org/t/p/w300${path}`}
                              alt={p.name}
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
                          <p style={{ marginTop: "4px", fontSize: "14px", fontWeight: 600 }}>
                            {p.name}
                          </p>
                        </div>
                      );
                    })}
                </div>
              </div>
            )}

            {!recLoading &&
              !recError &&
              recommendedMovies.length === 0 &&
              topActors.length === 0 &&
              topDirectors.length === 0 && (
                <p style={{ opacity: 0.75 }}>
                  Not enough preference data yet — try browsing and clicking more movies/people.
                </p>
              )}
          </section>
        ) : (
          <section style={{ marginBottom: "40px" }}>
            <h2
              style={{
                fontSize: "26px",
                borderLeft: "4px solid #e50914",
                paddingLeft: "10px",
                marginBottom: "6px",
              }}
            >
              Recommended for You
            </h2>
            <p style={{ opacity: 0.75, margin: 0 }}>
              Login to see personalized recommendations.
            </p>
          </section>
        )}

        {error && <p style={{ color: "#ff6b6b", marginBottom: "16px" }}>{error}</p>}
        {initialLoading && !error && (
          <p style={{ opacity: 0.8, marginBottom: "16px" }}>Loading movies...</p>
        )}

        {!initialLoading &&
          visibleGenres.map((genre) => {
            const movies = moviesByGenre[genre.id] || [];
            const offset = offsetByGenre[genre.id] ?? 0;
            const visible = movies.slice(offset, offset + VISIBLE_COUNT);

            if (movies.length === 0) return null;

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
                        cursor: canGoPrev(genre.id) ? "pointer" : "default",
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
                        cursor: canGoNext(genre.id) ? "pointer" : "default",
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

                      <p style={{ marginTop: "4px", fontSize: "14px", fontWeight: 500 }}>
                        {movie.title}
                      </p>
                      {movie.release_date && (
                        <p style={{ fontSize: "12px", opacity: 0.7 }}>
                          {movie.release_date}
                        </p>
                      )}
                      {movie.vote_average != null && (
                        <p style={{ fontSize: "12px", opacity: 0.8 }}>
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
