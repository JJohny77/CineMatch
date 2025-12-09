import { useState } from "react";
import { useNavigate } from "react-router-dom";

type SearchMovie = {
  id: number;
  title: string;
  poster_path: string | null;
  overview: string;
};

type PersonResult = {
  id: number;
  name: string;
  profilePath: string | null;
  knownForDepartment: string;
  knownFor: string[];
  popularity: number;
};

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [searchType, setSearchType] =
    useState<"movie" | "actors" | "directors">("movie");

  const [movieResults, setMovieResults] = useState<SearchMovie[]>([]);
  const [personResults, setPersonResults] = useState<PersonResult[]>([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  // ============================
  // HANDLERS
  // ============================
  const handleSearch = async () => {
    const trimmed = query.trim();
    if (!trimmed) return;

    setLoading(true);
    setError(null);

    try {
      if (searchType === "movie") {
        const res = await fetch(
          `http://localhost:8080/movies/search?query=${encodeURIComponent(
            trimmed
          )}`
        );

        if (!res.ok) throw new Error("Movie search failed");

        const data = await res.json();
        setMovieResults(data.results || []);
        setPersonResults([]);
      } else {
        const res = await fetch(
          `http://localhost:8080/movies/person/search?query=${encodeURIComponent(
            trimmed
          )}&page=1`
        );

        if (!res.ok) throw new Error("Person search failed");

        const data = await res.json();
        let people: PersonResult[] = data.results || [];

        // Φιλτράρισμα ανά tab
        if (searchType === "actors") {
          people = people.filter(
            (p) => p.knownForDepartment === "Acting"
          );
        } else if (searchType === "directors") {
          people = people.filter(
            (p) => p.knownForDepartment === "Directing"
          );
        }

        setPersonResults(people);
        setMovieResults([]);
      }
    } catch (e) {
      console.error("Search error:", e);
      setError("Something went wrong. Please try again.");
      setMovieResults([]);
      setPersonResults([]);
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
      <h1 style={{ marginBottom: "10px", fontSize: "32px" }}>Search</h1>

      {/* ============================ */}
      {/* TABS: MOVIES | ACTORS | DIRECTORS */}
      {/* ============================ */}
      <div style={{ display: "flex", gap: "20px", marginBottom: "25px" }}>
        <button
          onClick={() => setSearchType("movie")}
          style={{
            padding: "10px 16px",
            background: searchType === "movie" ? "#e50914" : "#444",
            border: "none",
            borderRadius: "6px",
            color: "white",
            cursor: "pointer",
            fontWeight: "bold",
          }}
        >
          Movies
        </button>

        <button
          onClick={() => setSearchType("actors")}
          style={{
            padding: "10px 16px",
            background: searchType === "actors" ? "#e50914" : "#444",
            border: "none",
            borderRadius: "6px",
            color: "white",
            cursor: "pointer",
            fontWeight: "bold",
          }}
        >
          Actors
        </button>

        <button
          onClick={() => setSearchType("directors")}
          style={{
            padding: "10px 16px",
            background: searchType === "directors" ? "#e50914" : "#444",
            border: "none",
            borderRadius: "6px",
            color: "white",
            cursor: "pointer",
            fontWeight: "bold",
          }}
        >
          Directors
        </button>
      </div>

      {/* ============================ */}
      {/* Search Bar */}
      {/* ============================ */}
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
          placeholder={
            searchType === "movie"
              ? "Search for a movie..."
              : searchType === "actors"
              ? "Search for actors..."
              : "Search for directors..."
          }
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
          }}
        >
          {loading ? "Searching..." : "Search"}
        </button>
      </div>

      {/* ============================ */}
      {/* Error */}
      {/* ============================ */}
      {error && (
        <p style={{ color: "#ff6b6b", marginBottom: "10px" }}>{error}</p>
      )}

      {/* ============================ */}
      {/* Loading */}
      {/* ============================ */}
      {loading && <p style={{ opacity: 0.8 }}>Loading...</p>}

      {/* ============================ */}
      {/* MOVIE RESULTS */}
      {/* ============================ */}
      {searchType === "movie" && movieResults.length > 0 && (
        <div>
          <p style={{ opacity: 0.8, marginBottom: "15px" }}>
            Found {movieResults.length} movies
          </p>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
              gap: "20px",
            }}
          >
            {movieResults.map((movie) => (
              <div
                key={movie.id}
                onClick={() => navigate(`/movie/${movie.id}`)}
                style={{
                  cursor: "pointer",
                  textAlign: "center",
                  color: "white",
                }}
              >
                {movie.poster_path ? (
                  <img
                    src={`https://image.tmdb.org/t/p/w300${movie.poster_path}`}
                    style={{
                      width: "100%",
                      borderRadius: "10px",
                      marginBottom: "10px",
                    }}
                    alt={movie.title}
                  />
                ) : (
                  <div
                    style={{
                      width: "100%",
                      height: "270px",
                      background: "#333",
                      borderRadius: "10px",
                      marginBottom: "10px",
                    }}
                  />
                )}
                <p>{movie.title}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ============================ */}
      {/* PERSON RESULTS */}
      {/* ============================ */}
      {searchType !== "movie" && personResults.length > 0 && (
        <div>
          <p style={{ opacity: 0.8, marginBottom: "15px" }}>
            Found {personResults.length}{" "}
            {searchType === "actors" ? "actors" : "directors"}
          </p>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))",
              gap: "20px",
            }}
          >
            {personResults.map((person) => (
              <div
                key={person.id}
                onClick={() =>
                  navigate(
                    person.knownForDepartment === "Directing"
                      ? `/director/${person.id}`
                      : `/actor/${person.id}`
                  )
                }
                style={{
                  cursor: "pointer",
                  padding: "10px",
                  background: "#222",
                  borderRadius: "10px",
                  textAlign: "center",
                }}
              >
                {person.profilePath ? (
                  <img
                    src={`https://image.tmdb.org/t/p/w300${person.profilePath}`}
                    style={{
                      width: "100%",
                      borderRadius: "10px",
                      marginBottom: "10px",
                    }}
                    alt={person.name}
                  />
                ) : (
                  <div
                    style={{
                      width: "100%",
                      height: "250px",
                      background: "#333",
                      borderRadius: "10px",
                      marginBottom: "10px",
                    }}
                  />
                )}

                <p style={{ fontSize: "18px", fontWeight: "bold" }}>
                  {person.name}
                </p>

                <p style={{ fontSize: "14px", opacity: 0.7 }}>
                  {person.knownForDepartment}
                </p>

                {person.knownFor.length > 0 && (
                  <p style={{ fontSize: "14px", marginTop: "6px" }}>
                    <strong>Known for:</strong> {person.knownFor.join(", ")}
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
