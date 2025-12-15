import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getDirectorDetails } from "../api/directors";
import "../styles/ActorPage.css"; // reuse ίδιο css

interface DirectedMovie {
  movieId: number;
  title: string;
  posterPath: string | null;
}

interface DirectorDetails {
  id: number;
  name: string;
  profilePath: string | null;
  biography: string;
  birthday: string | null;
  placeOfBirth: string | null;
  directedMovies: DirectedMovie[];
}

const DirectorPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [director, setDirector] = useState<DirectorDetails | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      if (!id) return;
      try {
        const data = await getDirectorDetails(Number(id));
        setDirector(data);
      } catch (err) {
        console.error("Failed to load director", err);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  if (loading) return <div style={{ color: "white" }}>Loading...</div>;
  if (!director) return <div style={{ color: "white" }}>Director not found.</div>;

  return (
    <div className="actor-page-container">
      {/* MAIN HEADER */}
      <div className="actor-header">
        {director.profilePath && (
          <img
            src={`https://image.tmdb.org/t/p/w500${director.profilePath}`}
            alt={director.name}
            className="actor-poster"
          />
        )}

        <div className="actor-info">
          <h1>{director.name}</h1>
          <p>
            <b>Born:</b> {director.birthday ?? "Unknown"}
          </p>
          <p>
            <b>Place of Birth:</b> {director.placeOfBirth ?? "Unknown"}
          </p>
        </div>
      </div>

      {/* BIO */}
      <div className="actor-section">
        <h2>Biography</h2>
        <p className="actor-bio">{director.biography}</p>
      </div>

      {/* DIRECTED MOVIES */}
      <div className="actor-section">
        <h2>Directed Movies</h2>
        <div className="known-for-carousel">
          {director.directedMovies.map((movie) => (
            <div
              key={movie.movieId}
              className="known-for-item"
              onClick={() => navigate(`/movie/${movie.movieId}`)}
            >
              {movie.posterPath ? (
                <img
                  src={`https://image.tmdb.org/t/p/w300${movie.posterPath}`}
                  alt={movie.title}
                />
              ) : (
                <div className="no-poster">No Image</div>
              )}
              <p>{movie.title}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default DirectorPage;
