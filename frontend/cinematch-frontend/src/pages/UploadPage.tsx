import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const UploadPage: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [isUploading, setIsUploading] = useState<boolean>(false);

  const navigate = useNavigate();

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = e.target.files?.[0];
    if (!selected) return;

    if (selected.type.startsWith("video")) {
      const video = document.createElement("video");
      video.src = URL.createObjectURL(selected);

      video.onloadedmetadata = () => {
        if (video.duration > 60) {
          alert("Video cannot exceed 60 seconds");
          return;
        }

        setFile(selected);
        setPreviewUrl(URL.createObjectURL(selected));
      };
    } else {
      setFile(selected);
      setPreviewUrl(URL.createObjectURL(selected));
    }
  }

  function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    const selected = e.dataTransfer.files?.[0];
    if (!selected) return;

    if (selected.type.startsWith("video")) {
      const video = document.createElement("video");
      video.src = URL.createObjectURL(selected);

      video.onloadedmetadata = () => {
        if (video.duration > 60) {
          alert("Video cannot exceed 60 seconds");
          return;
        }

        setFile(selected);
        setPreviewUrl(URL.createObjectURL(selected));
      };
    } else {
      setFile(selected);
      setPreviewUrl(URL.createObjectURL(selected));
    }
  }

  function handleDragOver(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
  }

  async function handleUpload() {
    if (!file) {
      alert("Please select a file first");
      return;
    }

    const token = localStorage.getItem("token");
    if (!token) {
      alert("You must be logged in to upload");
      navigate("/login");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
      setIsUploading(true);

      await axios.post("http://localhost:8080/content/upload", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
          Authorization: `Bearer ${token}`,
        },
        onUploadProgress: (progressEvent) => {
          const percent = Math.round(
            (progressEvent.loaded * 100) / (progressEvent.total ?? 1)
          );
          setUploadProgress(percent);
        },
      });

      alert("Upload successful!");
      navigate("/gallery");
    } catch (error) {
      console.error(error);
      alert("Upload error");
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <div style={{ padding: "20px", color: "white", position: "relative" }}>
      <h1>Upload Content</h1>

      {isUploading && (
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            width: "100%",
            height: "100%",
            backgroundColor: "rgba(0,0,0,0.7)",
            zIndex: 999,
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            backdropFilter: "blur(3px)",
          }}
        >
          <div
            style={{
              width: "60px",
              height: "60px",
              border: "6px solid #444",
              borderTop: "6px solid #E50914",
              borderRadius: "50%",
              animation: "spin 0.8s linear infinite",
            }}
          />
        </div>
      )}

      <style>
        {`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}
      </style>

      <div
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        style={{
          marginTop: "20px",
          padding: "60px 40px",
          border: "2px dashed #888",
          borderRadius: "12px",
          textAlign: "center",
          cursor: "pointer",
          backgroundColor: "#111",
          transition: "0.3s",
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.border = "2px dashed #E50914";
          e.currentTarget.style.backgroundColor = "#151515";
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.border = "2px dashed #888";
          e.currentTarget.style.backgroundColor = "#111";
        }}
        onClick={() => document.getElementById("fileInput")?.click()}
      >
        <p style={{ fontSize: "18px", color: "#ccc" }}>
          Drag & Drop a file here
          <br />
          or <span style={{ color: "#E50914" }}>click</span> to choose
        </p>

        <input
          id="fileInput"
          type="file"
          accept="image/*,video/*"
          onChange={handleFileSelect}
          style={{ display: "none" }}
        />
      </div>

      {previewUrl && (
        <div style={{ marginTop: "20px", textAlign: "center" }}>
          {file?.type.startsWith("image") ? (
            <img
              src={previewUrl}
              style={{
                width: "100%",
                maxWidth: "500px",
                borderRadius: "12px",
                boxShadow: "0 4px 20px rgba(0,0,0,0.5)",
                objectFit: "cover",
              }}
            />
          ) : (
            <video
              src={previewUrl}
              controls
              style={{
                width: "100%",
                maxWidth: "500px",
                borderRadius: "12px",
                boxShadow: "0 4px 20px rgba(0,0,0,0.5)",
              }}
            />
          )}

          <p style={{ color: "#aaa", marginTop: "10px", fontSize: "14px" }}>
            {file?.type.startsWith("video")
              ? "Video preview (max 60 seconds)"
              : "Image preview"}
          </p>

          <button
            onClick={handleUpload}
            style={{
              marginTop: "20px",
              padding: "12px 32px",
              fontSize: "18px",
              backgroundColor: "#E50914",
              border: "none",
              borderRadius: "6px",
              color: "white",
              cursor: "pointer",
              fontWeight: "bold",
              boxShadow: "0 0 15px rgba(229,9,20,0.6)",
              transition: "0.25s",
              transform: "scale(1)",
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = "scale(1.07)";
              e.currentTarget.style.boxShadow =
                "0 0 25px rgba(229,9,20,0.8)";
              e.currentTarget.style.backgroundColor = "#f6121d";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = "scale(1)";
              e.currentTarget.style.boxShadow =
                "0 0 15px rgba(229,9,20,0.6)";
              e.currentTarget.style.backgroundColor = "#E50914";
            }}
          >
            Upload
          </button>

          {uploadProgress > 0 && uploadProgress < 100 && (
            <div
              style={{
                marginTop: "15px",
                width: "100%",
                maxWidth: "500px",
                height: "10px",
                backgroundColor: "#333",
                borderRadius: "5px",
                overflow: "hidden",
                marginLeft: "auto",
                marginRight: "auto",
              }}
            >
              <div
                style={{
                  height: "100%",
                  width: `${uploadProgress}%`,
                  backgroundColor: "#E50914",
                  transition: "width 0.2s ease",
                }}
              />
            </div>
          )}

          {uploadProgress === 100 && (
            <p style={{ marginTop: "10px", color: "#0f0" }}>
              Upload complete!
            </p>
          )}
        </div>
      )}
    </div>
  );
};

export default UploadPage;
