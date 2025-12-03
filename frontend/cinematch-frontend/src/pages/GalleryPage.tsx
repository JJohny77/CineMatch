import React, { useEffect, useState } from "react";

interface ContentItem {
  filename: string;
  type: "image" | "video";
  url: string;
}

const GalleryPage: React.FC = () => {
  const [items, setItems] = useState<ContentItem[]>([]);
  const [loading, setLoading] = useState(true);

  const token = localStorage.getItem("token");

  async function loadContent() {
    try {
      const response = await fetch("http://localhost:8080/content/my-uploads", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        console.error("Failed to load gallery");
        setItems([]);
        return;
      }

      const data = await response.json();
      setItems(data);
    } catch (err) {
      console.error("Gallery error:", err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadContent();
  }, []);

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
        {items.map((item) => (
          <div
            key={item.filename}
            style={{
              backgroundColor: "#111",
              padding: "10px",
              borderRadius: "10px",
            }}
          >
            {item.type === "image" ? (
              <img
                src={"http://localhost:8080" + item.url}
                style={{
                  width: "100%",
                  borderRadius: "10px",
                  objectFit: "cover",
                }}
              />
            ) : (
              <video
                src={"http://localhost:8080" + item.url}
                controls
                style={{
                  width: "100%",
                  borderRadius: "10px",
                }}
              ></video>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default GalleryPage;
