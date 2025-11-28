import React, { useState } from "react";
import axios from "axios";

export default function RecastPage() {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<any>(null);

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setResult(null);

    if (!file) {
      setError("Please upload an image.");
      return;
    }

    const formData = new FormData();
    formData.append("image", file);

    try {
      setLoading(true);

      const res = await axios.post(
        "http://localhost:8080/ai/recast",
        formData
      );

      setResult(res.data);
    } catch (err) {
      console.error(err);
      setError("Failed to analyze image.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ color: "white", paddingTop: "100px", textAlign: "center" }}>
      <h1>Recast-It (Actor Lookalike)</h1>

      <form onSubmit={handleUpload} style={{ marginTop: "20px" }}>

        {/* File Input */}
        <div style={{ marginBottom: "20px" }}>
          <input
            type="file"
            accept="image/*"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />
        </div>

        {/* Button BELOW file input */}
        <button
          type="submit"
          disabled={loading}
          style={{
            padding: "10px 20px",
            background: "#e50914",
            border: "none",
            borderRadius: 8,
            color: "white",
            cursor: "pointer",
          }}
        >
          {loading ? "Analyzing..." : "Find Match"}
        </button>
      </form>

      {error && (
        <p style={{ marginTop: "20px", color: "#ff6b6b" }}>{error}</p>
      )}

      {result && (
        <div style={{ marginTop: "40px" }}>
          <h2>{result.actor}</h2>
          <p>Similarity: {(result.similarity * 100).toFixed(1)}%</p>
          <img
            src={`https://image.tmdb.org/t/p/w300${result.actorImage}`}
            alt="actor"
            style={{
              width: "150px",
              height: "150px",
              borderRadius: "50%",
              objectFit: "cover",
              marginTop: "20px",
            }}
          />
          <p style={{ marginTop: "10px" }}>TMDb ID: {result.actorId}</p>
        </div>
      )}
    </div>
  );
}
