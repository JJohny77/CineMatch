import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getActorDetails } from "../api/actors";
import "../styles/ActorPage.css";

interface KnownFor {
  movieId: number;
  title: string;
  posterPath: string | null;
}

interface Filmography {
  movieId: number;
  title: string;
  character: string | null;
  releaseYear: number | null;
}

interface ActorDetails {
  id: number;
  name: string;
  profilePath: string | null;
  biography: string;
  birthday: string | null;
  placeOfBirth: string | null;
  knownFor: KnownFor[];
  filmography: Filmography[];
}

const ActorPage: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [actor, setActor] = useState<ActorDetails | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      if (!id) return;
      try {
        const data = await getActorDetails(Number(id));
        setActor(data);
      } catch (err) {
        console.error("Failed to load actor", err);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  if (loading) return <div style={{ color: "white" }}>Loading...</div>;
  if (!actor) return <div style={{ color: "white" }}>Actor not found.</div>;

  return (
    <div className="actor-page-container">

      {/* MAIN HEADER */}
      <div className="actor-header">
        {actor.profilePath && (
          <img
            src={`https://image.tmdb.org/t/p/w500${actor.profilePath}`}
            alt={actor.name}
            className="actor-poster"
          />
        )}

        <div className="actor-info">
          <h1>{actor.name}</h1>
          <p><b>Born:</b> {actor.birthday ?? "Unknown"}</p>
          <p><b>Place of Birth:</b> {actor.placeOfBirth ?? "Unknown"}</p>
        </div>
      </div>

      {/* BIO */}
      <div className="actor-section">
        <h2>Biography</h2>
        <p className="actor-bio">{actor.biography}</p>
      </div>

      {/* KNOWN FOR */}
      <div className="actor-section">
        <h2>Known For</h2>
        <div className="known-for-carousel">
          {actor.knownFor.map((movie) => (
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

      {/* FILMOGRAPHY */}
      <div className="actor-section">
        <h2>Filmography</h2>
        <ul className="filmography-list">
          {actor.filmography.map((f) => (
            <li
              key={f.movieId}
              className="filmography-item"
              onClick={() => navigate(`/movie/${f.movieId}`)}
            >
              <span className="filmography-title">{f.title}</span>
              {f.releaseYear && <span className="filmography-year">({f.releaseYear})</span>}
              {f.character && <span className="filmography-character"> as {f.character}</span>}
            </li>
          ))}
        </ul>
      </div>

    </div>
  );
};

export default ActorPage;
