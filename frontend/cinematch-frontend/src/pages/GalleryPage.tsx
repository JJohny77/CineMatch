import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

interface ContentItem {
  filename: string;
  type: "image" | "video";
  url: string;
}

const GalleryPage: React.FC = () => {
  const [items, setItems] = useState<ContentItem[]>([]);
  const [loading, setLoading] = useState(true);

  const [previewItem, setPreviewItem] = useState<ContentItem | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ContentItem | null>(null);

  const navigate = useNavigate();

  async function loadContent(token: string) {
    try {
      const response = await axios.get(
        "http://localhost:8080/content/list",
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      setItems(response.data);
    } catch (error) {
      console.error("Failed to load gallery", error);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }
    loadContent(token);
  }, [navigate]);

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") setPreviewItem(null);
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  async function deleteItem(filename: string) {
    const token = localStorage.getItem("token");
    if (!token) {
      navigate("/login");
      return;
    }

    try {
      const response = await axios.delete(
        "http://localhost:8080/content/delete/" + filename,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.status !== 200) {
        alert("Failed to delete file.");
        return;
      }

      await loadContent(token);
      setDeleteTarget(null);
    } catch (err) {
      console.error(err);
      alert("Error deleting file.");
    }
  }

  return (
    <div style={{ padding: "20px", color: "white" }}>
      <h1>My Gallery</h1>

      {loading && <p>Loading...</p>}
      {!loading && items.length === 0 && <p>No uploaded content found.</p>}

      <div
        style={{
          marginTop: "20px",
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
          gap: "20px",
        }}
      >
        {items.map((item, index) => (
          <div
            key={index}
            onClick={() => setPreviewItem(item)}
            style={{
              backgroundColor: "#111",
              borderRadius: "10px",
              padding: "10px",
              boxShadow: "0 4px 12px rgba(0,0,0,0.4)",
              cursor: "pointer",
              border: "1px solid #333",
              position: "relative",
            }}
          >
            <button
              onClick={(e) => {
                e.stopPropagation();
                setDeleteTarget(item);
              }}
              style={{
                position: "absolute",
                top: "8px",
                right: "8px",
                background: "rgba(255,0,0,0.75)",
                border: "none",
                padding: "6px 10px",
                borderRadius: "6px",
                color: "white",
                cursor: "pointer",
                fontWeight: "bold",
                zIndex: 20,
              }}
            >
              ✕
            </button>

            <div
              style={{
                width: "100%",
                height: "200px",
                backgroundColor: "#000",
                borderRadius: "8px",
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                overflow: "hidden",
              }}
            >
              {item.type === "image" ? (
                <img
                  src={"http://localhost:8080" + item.url}
                  alt={item.filename}
                  style={{
                    maxWidth: "100%",
                    maxHeight: "100%",
                    objectFit: "contain",
                  }}
                />
              ) : (
                <video
                  src={"http://localhost:8080" + item.url}
                  muted
                  style={{
                    maxWidth: "100%",
                    maxHeight: "100%",
                    objectFit: "contain",
                    backgroundColor: "#000",
                  }}
                />
              )}
            </div>

            <p style={{ fontSize: "14px", marginTop: "8px", color: "#bbb" }}>
              {item.filename}
            </p>
          </div>
        ))}
      </div>

      {deleteTarget && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            width: "100vw",
            height: "100vh",
            background: "rgba(0,0,0,0.7)",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            zIndex: 200000,
          }}
          onClick={() => setDeleteTarget(null)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              background: "#181818",
              padding: "30px",
              borderRadius: "10px",
              width: "90%",
              maxWidth: "400px",
              textAlign: "center",
              border: "1px solid #333",
            }}
          >
            <h2 style={{ color: "white", marginBottom: "20px" }}>
              Delete "{deleteTarget.filename}"?
            </h2>

            <p style={{ color: "#bbb", marginBottom: "25px" }}>
              Are you sure you want to delete this file?
            </p>

            <div style={{ display: "flex", justifyContent: "center", gap: "20px" }}>
              <button
                onClick={() => setDeleteTarget(null)}
                style={{
                  padding: "10px 20px",
                  background: "#555",
                  border: "none",
                  borderRadius: "6px",
                  color: "white",
                  cursor: "pointer",
                }}
              >
                Cancel
              </button>

              <button
                onClick={() => deleteItem(deleteTarget.filename)}
                style={{
                  padding: "10px 20px",
                  background: "#E50914",
                  border: "none",
                  borderRadius: "6px",
                  color: "white",
                  cursor: "pointer",
                  fontWeight: "bold",
                }}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {previewItem && (
        <div
          onClick={() => setPreviewItem(null)}
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            width: "100vw",
            height: "100vh",
            background: "rgba(0,0,0,0.85)",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            zIndex: 100000,
            padding: "20px",
          }}
        >
          <div onClick={(e) => e.stopPropagation()}>
            {previewItem.type === "image" ? (
              <img
                src={"http://localhost:8080" + previewItem.url}
                style={{
                  maxWidth: "90vw",
                  maxHeight: "90vh",
                  borderRadius: "10px",
                }}
              />
            ) : (
              <video
                src={"http://localhost:8080" + previewItem.url}
                controls
                autoPlay
                style={{
                  maxWidth: "90vw",
                  maxHeight: "90vh",
                  borderRadius: "10px",
                }}
              />
            )}
          </div>

          <div
            onClick={() => setPreviewItem(null)}
            style={{
              position: "fixed",
              top: "20px",
              right: "30px",
              fontSize: "32px",
              color: "white",
              cursor: "pointer",
              fontWeight: "bold",
            }}
          >
            ✕
          </div>
        </div>
      )}
    </div>
  );
};

export default GalleryPage;
