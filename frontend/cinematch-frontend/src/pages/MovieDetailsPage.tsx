import { useParams, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import SentimentWidget from "../components/SentimentWidget";
import {
  fetchStarPower,
  fetchAudienceEngagement,
} from "../api/kpi";

type MovieDetails = {
  title: string;
  overview: string;
  poster_path: string;
  release_date: string;
  runtime: number;
  popularity: number;
  genres: string[];
};

type MovieVideo = {
  name: string;
  key: string;
  site: string;
  type: string;
};

type MovieKpiState = {
  starPower: number | null;
  audienceEngagement: number | null;
  loading: boolean;
  error: string | null;
};

export default function MovieDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [movie, setMovie] = useState<MovieDetails | null>(null);
  const [trailerKey, setTrailerKey] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [kpis, setKpis] = useState<MovieKpiState>({
    starPower: null,
    audienceEngagement: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    if (!id) return;

    // reset movie & kpis όταν αλλάζει η ταινία
    setLoading(true);
    setMovie(null);
    setTrailerKey(null);

    setKpis({
      starPower: null,
      audienceEngagement: null,
      loading: true,
      error: null,
    });

    // --------- Movie details + videos ----------
    Promise.all([
      fetch(`http://localhost:8080/movies/${id}`),
      fetch(`http://localhost:8080/movies/${id}/videos`),
    ])
      .then(async ([detailsRes, videosRes]) => {
        // DETAILS
        if (!detailsRes.ok) throw new Error("Movie not found");
        const detailsData: MovieDetails = await detailsRes.json();
        setMovie(detailsData);

        // VIDEOS
        if (videosRes.ok) {
          const videos: MovieVideo[] = await videosRes.json();

          const youtube = videos.filter(
            (v) => v.site === "YouTube" && v.type === "Trailer"
          );

          if (youtube.length > 0) {
            const official =
              youtube.find((v) =>
                v.name.toLowerCase().includes("official")
              ) || youtube[0];

            setTrailerKey(official.key);
          }
        }

        setLoading(false);
      })
      .catch((err) => {
        console.error("Error fetching movie details or videos:", err);
        setMovie(null);
        setTrailerKey(null);
        setLoading(false);
      });

    // --------- KPIs (Star Power & Audience Engagement) ----------
    Promise.all([
      fetchStarPower(id),
      fetchAudienceEngagement(id),
    ])
      .then(([starPower, audienceEngagement]) => {
        setKpis({
          starPower,
          audienceEngagement,
          loading: false,
          error: null,
        });
      })
      .catch((err) => {
        console.error("Error fetching KPIs:", err);
        setKpis((prev) => ({
          ...prev,
          loading: false,
          error: "Failed to load KPIs",
        }));
      });
  }, [id]);

  // LOADING
  if (loading) {
    return <h2 style={{ padding: "20px", color: "#fff" }}>Loading...</h2>;
  }

  // ERROR
  if (!movie) {
    return (
      <h2 style={{ padding: "20px", color: "#fff" }}>
        Movie not found
      </h2>
    );
  }

  const kpiCardStyle = {
    flex: "1 1 220px",
    minWidth: "220px",
    padding: "16px 20px",
    borderRadius: "12px",
    background:
      "linear-gradient(135deg, rgba(40,40,40,0.95), rgba(20,20,20,0.95))",
    boxShadow: "0 4px 15px rgba(0,0,0,0.5)",
    display: "flex",
    flexDirection: "column" as const,
    gap: "8px",
  };

  const formatKpi = (value: number | null) => {
    if (value === null || Number.isNaN(value)) return "N/A";
    return `${Math.round(value)} / 100`;
  };

  return (
    <div
      style={{
        padding: "40px",
        maxWidth: "1200px",
        margin: "0 auto",
        color: "#fff",
      }}
    >
      {/* Back Button */}
      <button
        onClick={() => navigate(-1)}
        style={{
          marginBottom: "20px",
          padding: "8px 18px",
          borderRadius: "6px",
          border: "none",
          backgroundColor: "#555",
          color: "#fff",
          cursor: "pointer",
          fontSize: "15px",
          transition: "0.2s",
        }}
        onMouseOver={(e) =>
          (e.currentTarget.style.backgroundColor = "#777")
        }
        onMouseOut={(e) =>
          (e.currentTarget.style.backgroundColor = "#555")
        }
      >
        ← Back
      </button>

      {/* Main container */}
      <div
        style={{
          display: "flex",
          gap: "40px",
          flexWrap: "wrap",
        }}
      >
        {/* Poster */}
        <div style={{ flex: "0 0 300px" }}>
          {movie.poster_path ? (
            <img
              src={`https://image.tmdb.org/t/p/w500${movie.poster_path}`}
              alt={movie.title}
              style={{
                width: "100%",
                borderRadius: "12px",
                boxShadow: "0 4px 20px rgba(0,0,0,0.4)",
              }}
            />
          ) : (
            <div
              style={{
                width: "100%",
                height: "450px",
                background: "#333",
                borderRadius: "12px",
              }}
            />
          )}
        </div>

        {/* Movie details */}
        <div style={{ flex: 1, minWidth: "300px" }}>
          <h1 style={{ marginBottom: "20px", fontSize: "42px" }}>
            {movie.title}
          </h1>

          <p>
            <strong>Release date:</strong> {movie.release_date}
          </p>
          <p>
            <strong>Runtime:</strong> {movie.runtime} min
          </p>
          <p>
            <strong>Popularity:</strong> {movie.popularity}
          </p>

          {/* KPI SECTION */}
          <div style={{ marginTop: "30px" }}>
            <h3 style={{ marginBottom: "16px" }}>Quick KPIs</h3>

            {kpis.loading && (
              <p style={{ opacity: 0.8 }}>Loading KPIs...</p>
            )}

            {kpis.error && (
              <p style={{ color: "#ff6b6b" }}>{kpis.error}</p>
            )}

            {!kpis.loading && !kpis.error && (
              <div
                style={{
                  display: "flex",
                  flexWrap: "wrap",
                  gap: "16px",
                }}
              >
                {/* Star Power Card */}
                <div style={kpiCardStyle}>
                  <div
                    style={{
                      fontSize: "18px",
                      fontWeight: 600,
                      display: "flex",
                      alignItems: "center",
                      gap: "8px",
                    }}
                  >
                    <span role="img" aria-label="Star">
                      ⭐
                    </span>
                    Star Power
                  </div>
                  <div
                    style={{
                      fontSize: "26px",
                      fontWeight: 700,
                      marginTop: "4px",
                    }}
                  >
                    {formatKpi(kpis.starPower)}
                  </div>
                  <p
                    style={{
                      fontSize: "13px",
                      opacity: 0.85,
                      marginTop: "4px",
                    }}
                  >
                    Συνδυάζει δημοφιλία ηθοποιών, βραβεία και buzz
                    γύρω από το cast.
                  </p>
                </div>

                {/* Audience Engagement Card */}
                <div style={kpiCardStyle}>
                  <div
                    style={{
                      fontSize: "18px",
                      fontWeight: 600,
                      display: "flex",
                      alignItems: "center",
                      gap: "8px",
                    }}
                  >
                    <span role="img" aria-label="Audience">
                      🎭
                    </span>
                    Audience Engagement
                  </div>
                  <div
                    style={{
                      fontSize: "26px",
                      fontWeight: 700,
                      marginTop: "4px",
                    }}
                  >
                    {formatKpi(kpis.audienceEngagement)}
                  </div>
                  <p
                    style={{
                      fontSize: "13px",
                      opacity: 0.85,
                      marginTop: "4px",
                    }}
                  >
                    Δείχνει πόσο ενεργά ασχολείται το κοινό με την
                    ταινία (ψήφοι, reviews, social activity).
                  </p>
                </div>
              </div>
            )}
          </div>

          <h3 style={{ marginTop: "30px" }}>Genres</h3>
          <ul style={{ lineHeight: "1.8" }}>
            {movie.genres.map((g, index) => (
              <li key={index}>{g}</li>
            ))}
          </ul>

          <h3 style={{ marginTop: "30px" }}>Overview</h3>
          <p style={{ lineHeight: "1.7", opacity: 0.9 }}>{movie.overview}</p>
          <div style={{ marginTop: "40px" }}>
            <SentimentWidget />
          </div>
        </div>
      </div>

      {/* Trailer */}
      {trailerKey && (
        <div style={{ marginTop: "50px" }}>
          <h2 style={{ marginBottom: "16px" }}>Trailer</h2>

          <div
            style={{
              position: "relative",
              paddingBottom: "56.25%",
              height: 0,
              overflow: "hidden",
              borderRadius: "12px",
              boxShadow: "0 4px 20px rgba(0,0,0,0.4)",
            }}
          >
            <iframe
              src={`https://www.youtube.com/embed/${trailerKey}`}
              title="Movie trailer"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowFullScreen
              style={{
                position: "absolute",
                top: 0,
                left: 0,
                width: "100%",
                height: "100%",
                border: "none",
                borderRadius: "12px",
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
}
