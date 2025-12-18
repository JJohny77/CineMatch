import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

interface Post {
  id: number;
  mediaUrl: string;
  mediaType: "image" | "video";
  caption?: string;
  createdAt: string;
  movieId?: number;
  author?: {
    id: number;
    username: string | null;
  };
  likesCount: number;
  likedByMe: boolean;
}

interface LikeState {
  liked: boolean;
  likesCount: number;
}

interface Comment {
  id: number;
  text: string;
  createdAt: string;
  author: {
    id: number;
    username: string;
  };
}

const FeedPage: React.FC = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  const [posts, setPosts] = useState<Post[]>([]);
  const [likes, setLikes] = useState<Record<number, LikeState>>({});
  const [comments, setComments] = useState<Record<number, Comment[]>>({});
  const [commentText, setCommentText] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);

  // ===== UPLOAD STATE =====
  const [showUpload, setShowUpload] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [caption, setCaption] = useState("");
  const [movieId, setMovieId] = useState<string>("");

  // =========================
  // LOAD FEED
  // =========================
  async function loadFeed() {
    try {
      const res = await axios.get("http://localhost:8080/posts/feed", {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });

      setPosts(res.data);

      const initialLikes: Record<number, LikeState> = {};
      res.data.forEach((p: Post) => {
        initialLikes[p.id] = {
          liked: p.likedByMe,
          likesCount: p.likesCount,
        };
      });
      setLikes(initialLikes);
    } catch (e) {
      console.error("Load feed failed", e);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadFeed();
  }, []);

  // =========================
  // LOAD COMMENTS
  // =========================
  async function loadComments(postId: number) {
    const res = await axios.get(
      `http://localhost:8080/posts/${postId}/comments`
    );
    setComments((prev) => ({ ...prev, [postId]: res.data }));
  }

  // =========================
  // ADD COMMENT
  // =========================
  async function submitComment(postId: number) {
    if (!token) return navigate("/login");
    if (!commentText[postId]) return;

    await axios.post(
      `http://localhost:8080/posts/${postId}/comments`,
      { text: commentText[postId] },
      { headers: { Authorization: `Bearer ${token}` } }
    );

    setCommentText((prev) => ({ ...prev, [postId]: "" }));
    loadComments(postId);
  }

  // =========================
  // TOGGLE LIKE
  // =========================
  async function toggleLike(postId: number) {
    if (!token) return navigate("/login");

    const res = await axios.post(
      `http://localhost:8080/posts/${postId}/like`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    );

    setLikes((prev) => ({
      ...prev,
      [postId]: {
        liked: res.data.liked,
        likesCount: res.data.likesCount,
      },
    }));
  }

  // =========================
  // UPLOAD
  // =========================
  async function handleUpload() {
    if (!file || !token) return;

    const formData = new FormData();
    formData.append("file", file);

    const uploadRes = await axios.post(
      "http://localhost:8080/content/upload",
      formData,
      { headers: { Authorization: `Bearer ${token}` } }
    );

    await axios.post(
      "http://localhost:8080/posts",
      {
        mediaUrl: uploadRes.data.message,
        mediaType: file.type.startsWith("image") ? "image" : "video",
        caption,
        movieId: movieId ? Number(movieId) : null,
      },
      { headers: { Authorization: `Bearer ${token}` } }
    );

    setShowUpload(false);
    setFile(null);
    setPreviewUrl(null);
    setCaption("");
    setMovieId("");
    loadFeed();
  }

  return (
    <div style={{ padding: "20px", color: "white" }}>
      <h1 style={{ marginBottom: "30px" }}>Feed</h1>

      {/* + BUTTON */}
      <button
        onClick={() => setShowUpload(true)}
        style={{
          position: "fixed",
          top: "90px",
          right: "25px",
          width: "52px",
          height: "52px",
          borderRadius: "50%",
          backgroundColor: "#E50914",
          color: "white",
          fontSize: "32px",
          border: "none",
          cursor: "pointer",
          zIndex: 100000,
        }}
      >
        +
      </button>

      {loading && <p>Loading...</p>}

      <div style={{ maxWidth: "600px", margin: "0 auto" }}>
        {posts.map((post) => (
          <div
            key={post.id}
            style={{
              marginBottom: "50px",
              background: "#111",
              borderRadius: "14px",
              padding: "14px",
              border: "1px solid #222",
            }}
          >
            <div style={{ color: "#aaa" }}>
              @{post.author?.username ?? "unknown"}
            </div>

            {/* 🎬 MOVIE TAG */}
            {post.movieId && (
              <div style={{ color: "#888", fontSize: "13px" }}>
                🎬 Movie Tag: {post.movieId}
              </div>
            )}

            {post.mediaType === "image" ? (
              <img
                src={"http://localhost:8080" + post.mediaUrl}
                style={{ width: "100%", borderRadius: "12px" }}
              />
            ) : (
              <video
                src={"http://localhost:8080" + post.mediaUrl}
                controls
                style={{ width: "100%", borderRadius: "12px" }}
              />
            )}

            <div style={{ display: "flex", gap: "10px", marginTop: "10px" }}>
              <button
                onClick={() => toggleLike(post.id)}
                style={{
                  background: "transparent",
                  border: "none",
                  fontSize: "22px",
                  cursor: "pointer",
                  color: likes[post.id]?.liked ? "#E50914" : "white",
                }}
              >
                ♥
              </button>
              <span>{likes[post.id]?.likesCount ?? 0} likes</span>
            </div>

            {post.caption && <p>{post.caption}</p>}

            <button onClick={() => loadComments(post.id)}>View comments</button>

            {comments[post.id]?.map((c) => (
              <div key={c.id}>
                @{c.author.username}: {c.text}
              </div>
            ))}

            <input
              placeholder="Add a comment..."
              value={commentText[post.id] ?? ""}
              onChange={(e) =>
                setCommentText((p) => ({ ...p, [post.id]: e.target.value }))
              }
              onKeyDown={(e) => e.key === "Enter" && submitComment(post.id)}
            />
          </div>
        ))}
      </div>

      {/* UPLOAD MODAL */}
      {showUpload && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.85)",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            zIndex: 200000,
          }}
        >
          <div style={{ background: "#111", padding: "20px", borderRadius: "12px" }}>
            <input
              type="file"
              accept="image/*,video/*"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (!f) return;
                setFile(f);
                setPreviewUrl(URL.createObjectURL(f));
              }}
            />

            {previewUrl && <img src={previewUrl} style={{ width: "100%" }} />}

            <input
              placeholder="Movie ID (TMDb)"
              value={movieId}
              onChange={(e) => setMovieId(e.target.value)}
              style={{ width: "100%", marginTop: "10px" }}
            />

            <textarea
              placeholder="Caption..."
              value={caption}
              onChange={(e) => setCaption(e.target.value)}
            />

            <button onClick={handleUpload}>Upload</button>
            <button onClick={() => setShowUpload(false)}>Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default FeedPage;
