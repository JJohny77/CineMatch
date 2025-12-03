import React, { useState } from "react";

const UploadPage: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  // ================================
  // FILE SELECT
  // ================================
  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = e.target.files?.[0] || null;
    setFile(selected);

    if (selected) {
      setPreviewUrl(URL.createObjectURL(selected));
    } else {
      setPreviewUrl(null);
    }
  }

  // ================================
  // UPLOAD HANDLER – FIXED 🔥
  // ================================
  async function handleUpload() {
    if (!file) {
      setMessage("Please select a file.");
      return;
    }

    const token = localStorage.getItem("token");
    if (!token) {
      alert("No token found, please log in again.");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    setUploading(true);
    setMessage(null);

    try {
      const res = await fetch("http://localhost:8080/content/upload", {
        method: "POST",
        body: formData,
        headers: {
          Authorization: `Bearer ${token}`,      // 🔥 REQUIRED
        },
        credentials: "include",                  // 🔥 NECESSARY WHEN allowCredentials=true
      });

      if (!res.ok) {
        const errorData = await res.json();
        console.error("UPLOAD ERROR:", errorData);
        alert("Upload error");
        setMessage("Upload failed.");
        setUploading(false);
        return;
      }

      const data = await res.json();
      console.log("UPLOAD SUCCESS:", data);
      setMessage("Upload complete!");
      setFile(null);
      setPreviewUrl(null);

    } catch (err) {
      console.error("FETCH ERROR:", err);
      setMessage("Error uploading file.");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div style={{ padding: "20px", color: "white" }}>
      <h1>Upload Content</h1>

      {/* FILE INPUT */}
      <input
        type="file"
        accept="image/*,video/mp4"
        onChange={handleFileSelect}
        style={{
          marginTop: "20px",
          marginBottom: "20px",
          padding: "10px",
          fontSize: "16px",
        }}
      />

      {/* PREVIEW */}
      {previewUrl && (
        <div
          style={{
            marginTop: "20px",
            marginBottom: "20px",
            background: "#000",
            padding: "10px",
            borderRadius: "10px",
            width: "320px",
          }}
        >
          {file?.type.startsWith("image") ? (
            <img
              src={previewUrl}
              style={{
                width: "100%",
                borderRadius: "10px",
                objectFit: "contain",
              }}
            />
          ) : (
            <video
              src={previewUrl}
              controls
              style={{
                width: "100%",
                borderRadius: "10px",
                background: "#000",
              }}
            />
          )}
        </div>
      )}

      {/* UPLOAD BUTTON */}
      <button
        onClick={handleUpload}
        disabled={uploading}
        style={{
          padding: "12px 20px",
          background: uploading ? "#555" : "#E50914",
          color: "white",
          border: "none",
          borderRadius: "6px",
          cursor: uploading ? "not-allowed" : "pointer",
          fontWeight: "bold",
        }}
      >
        {uploading ? "Uploading..." : "Upload"}
      </button>

      {/* STATUS MESSAGE */}
      {message && (
        <p style={{ marginTop: "20px", fontSize: "16px", color: "#bbb" }}>
          {message}
        </p>
      )}
    </div>
  );
};

export default UploadPage;