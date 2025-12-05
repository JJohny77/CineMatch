import React, { useState } from "react";
import axios from "axios";

export default function FaceIdentifyPage() {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [results, setResults] = useState<any[]>([]);

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setResults([]);

    if (!file) {
      setError("Please upload an image.");
      return;
    }

    const formData = new FormData();
    formData.append("image", file);

    try {
      setLoading(true);

      const res = await axios.post(
        "http://localhost:8080/ai/face/identify",
        formData
      );

      setResults(res.data); // ARRAY of results
    } catch (err) {
      console.error(err);
      setError("Failed to analyze image.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ color: "white", paddingTop: "100px", textAlign: "center" }}>
      <h1>Actor Lookalike Finder</h1>

      <form onSubmit={handleUpload} style={{ marginTop: "20px" }}>
        {/* File input */}
        <div style={{ marginBottom: "20px" }}>
          <input
            type="file"
            accept="image/*"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />
        </div>

        {/* Submit */}
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

      {/* Error */}
      {error && (
        <p style={{ marginTop: "20px", color: "#ff6b6b" }}>{error}</p>
      )}

      {/* RESULTS */}
      {results.length > 0 && (
        <div style={{ marginTop: "40px" }}>
          <h2>Top Matches</h2>

          <div
            style={{
              display: "flex",
              justifyContent: "center",
              gap: "30px",
              flexWrap: "wrap",
              marginTop: "20px",
            }}
          >
            {results.map((r, idx) => (
              <div
                key={idx}
                style={{
                  background: "#222",
                  padding: "15px",
                  borderRadius: "12px",
                  width: "200px",
                  textAlign: "center",
                }}
              >
                <h3 style={{ marginBottom: "10px" }}>{r.actor}</h3>
                <p style={{ margin: 0 }}>
                  Similarity: {(r.similarity * 100).toFixed(1)}%
                </p>

                <img
                  src={r.actorImage}
                  alt="actor"
                  style={{
                    width: "150px",
                    height: "150px",
                    borderRadius: "50%",
                    objectFit: "cover",
                    marginTop: "20px",
                  }}
                />

                <p style={{ marginTop: "10px", fontSize: "14px" }}>
                  TMDb ID: {r.actorId}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
