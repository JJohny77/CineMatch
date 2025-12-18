import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

interface MovieSuggestion {
  id: number;
  title: string;
  release_date?: string;
}

interface Post {
  id: number;

  // ✅ backend μπορεί να επιστρέφει και userId (όπως στο screenshot σου)
  userId?: number;

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

  // ✅ αν το backend στέλνει ownedByMe, το πιάνουμε κατευθείαν (χωρίς JWT decode)
  ownedByMe?: boolean;
}

interface LikeState {
  liked: boolean;
  likesCount: number;
}

interface Comment {
  id: number;
  text: string;
  createdAt: string;
  author?: {
    id: number;
    username: string | null;
  };
}

const API = "http://localhost:8080";

function getUserIdFromJwt(token: string | null): number | null {
  if (!token) return null;
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;

    const payloadBase64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(payloadBase64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );

    const payload = JSON.parse(json);

    // ✅ Προσθήκη: payload.sub (πολλές φορές το userId είναι εκεί σαν "4")
    const candidate =
      payload.userId ??
      payload.id ??
      payload.uid ??
      payload.user_id ??
      payload.sub ??
      null;

    if (typeof candidate === "number") return candidate;
    if (typeof candidate === "string" && candidate.trim()) {
      const n = Number(candidate);
      return Number.isFinite(n) ? n : null;
    }
    return null;
  } catch {
    return null;
  }
}

const FeedPage: React.FC = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  const viewerUserId = useMemo(() => getUserIdFromJwt(token), [token]);

  const [posts, setPosts] = useState<Post[]>([]);
  const [likes, setLikes] = useState<Record<number, LikeState>>({});
  const [comments, setComments] = useState<Record<number, Comment[]>>({});
  const [commentText, setCommentText] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);

  // ✅ MovieId -> Movie Title cache
  const [movieTitleById, setMovieTitleById] = useState<Record<number, string>>(
    {}
  );

  // ===== UPLOAD STATE =====
  const [showUpload, setShowUpload] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [caption, setCaption] = useState("");

  // ✅ Movie tag autocomplete state
  const [movieQuery, setMovieQuery] = useState("");
  const [movieId, setMovieId] = useState<number | null>(null);
  const [movieSuggestions, setMovieSuggestions] = useState<MovieSuggestion[]>(
    []
  );
  const [movieLoading, setMovieLoading] = useState(false);

  const authHeaders = useMemo(() => {
    return token ? { Authorization: `Bearer ${token}` } : {};
  }, [token]);

  // =========================
  // LOAD FEED
  // =========================
  async function loadFeed() {
    try {
      const res = await axios.get(`${API}/posts/feed`, {
        headers: authHeaders,
      });

      const data = res.data as Post[];
      setPosts(data);

      const initialLikes: Record<number, LikeState> = {};
      data.forEach((p) => {
        initialLikes[p.id] = {
          liked: !!p.likedByMe,
          likesCount: p.likesCount ?? 0,
        };
      });
      setLikes(initialLikes);

      // ✅ prefetch movie titles for visible posts
      const ids = Array.from(
        new Set(
          data
            .map((p) => p.movieId)
            .filter((x): x is number => typeof x === "number")
        )
      );

      const missing = ids.filter((id) => movieTitleById[id] == null);
      if (missing.length > 0) {
        const results = await Promise.all(
          missing.map(async (id) => {
            try {
              const r = await axios.get(`${API}/movies/${id}`, {
                params: { source: "FEED_TAG" },
              });
              const title: string =
                r.data?.title ??
                r.data?.name ??
                r.data?.original_title ??
                `#${id}`;
              return { id, title };
            } catch {
              return { id, title: `#${id}` };
            }
          })
        );

        setMovieTitleById((prev) => {
          const next = { ...prev };
          results.forEach((x) => (next[x.id] = x.title));
          return next;
        });
      }
    } catch (e) {
      console.error("Load feed failed", e);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadFeed();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // =========================
  // MOVIE AUTOCOMPLETE (TMDb via backend)
  // =========================
  useEffect(() => {
    if (!showUpload) return;
    if (movieQuery.trim().length < 2) {
      setMovieSuggestions([]);
      return;
    }

    const t = setTimeout(async () => {
      try {
        setMovieLoading(true);
        const res = await axios.get(`${API}/movies/search`, {
          params: { query: movieQuery },
        });

        const results = (res.data?.results ?? []) as any[];
        const mapped: MovieSuggestion[] = results.slice(0, 8).map((m) => ({
          id: m.id,
          title: m.title,
          release_date: m.release_date,
        }));

        setMovieSuggestions(mapped);
      } catch (e) {
        setMovieSuggestions([]);
      } finally {
        setMovieLoading(false);
      }
    }, 300);

    return () => clearTimeout(t);
  }, [movieQuery, showUpload]);

  // =========================
  // LOAD COMMENTS
  // =========================
  async function loadComments(postId: number) {
    const res = await axios.get(`${API}/posts/${postId}/comments`);
    setComments((prev) => ({ ...prev, [postId]: res.data }));
  }

  // =========================
  // ADD COMMENT
  // =========================
  async function submitComment(postId: number) {
    if (!token) return navigate("/login");
    if (!commentText[postId]?.trim()) return;

    await axios.post(
      `${API}/posts/${postId}/comments`,
      { text: commentText[postId] },
      { headers: authHeaders }
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
      `${API}/posts/${postId}/like`,
      {},
      { headers: authHeaders }
    );

    setLikes((prev) => ({
      ...prev,
      [postId]: {
        liked: !!res.data.liked,
        likesCount: res.data.likesCount ?? 0,
      },
    }));
  }

  // =========================
  // DELETE POST (only mine)
  // =========================
  async function deletePost(postId: number) {
    if (!token) return navigate("/login");

    const ok = window.confirm("Delete this post?");
    if (!ok) return;

    await axios.delete(`${API}/posts/${postId}`, { headers: authHeaders });

    // remove instantly from UI
    setPosts((prev) => prev.filter((p) => p.id !== postId));
    setLikes((prev) => {
      const next = { ...prev };
      delete next[postId];
      return next;
    });
    setComments((prev) => {
      const next = { ...prev };
      delete next[postId];
      return next;
    });
    setCommentText((prev) => {
      const next = { ...prev };
      delete next[postId];
      return next;
    });
  }

  // =========================
  // UPLOAD
  // =========================
  async function handleUpload() {
    if (!file || !token) return;

    const formData = new FormData();
    formData.append("file", file);

    const uploadRes = await axios.post(`${API}/content/upload`, formData, {
      headers: authHeaders,
    });

    // ✅ πιο safe extract url
    let mediaUrl: string =
      uploadRes.data?.url ??
      uploadRes.data?.mediaUrl ??
      uploadRes.data?.path ??
      uploadRes.data?.message;

    if (!mediaUrl) {
      alert("Upload failed: no media url returned from backend.");
      return;
    }

    if (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://")) {
      if (mediaUrl.startsWith(API)) {
        mediaUrl = mediaUrl.replace(API, "");
      }
    }

    await axios.post(
      `${API}/posts`,
      {
        mediaUrl,
        mediaType: file.type.startsWith("image") ? "image" : "video",
        caption: caption.trim() ? caption.trim() : null,
        movieId: movieId ?? null,
      },
      { headers: authHeaders }
    );

    setShowUpload(false);
    setFile(null);
    setPreviewUrl(null);
    setCaption("");
    setMovieQuery("");
    setMovieId(null);
    setMovieSuggestions([]);

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
        {posts.map((post) => {
          // ✅ πιο robust check για "δικό μου post"
          const isMine =
            post.ownedByMe === true ||
            (viewerUserId != null &&
              (post.author?.id === viewerUserId || post.userId === viewerUserId));

          return (
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
              {/* top row: author + delete (only mine) */}
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  gap: "12px",
                }}
              >
                <div style={{ color: "#aaa" }}>
                  @{post.author?.username ?? "unknown"}
                </div>

                {isMine && (
                  <button
                    onClick={() => deletePost(post.id)}
                    title="Delete"
                    style={{
                      background: "transparent",
                      border: "1px solid #333",
                      color: "#fff",
                      cursor: "pointer",
                      borderRadius: "10px",
                      padding: "6px 10px",
                      fontSize: "12px",
                    }}
                  >
                    🗑 Delete
                  </button>
                )}
              </div>

              {/* 🎬 MOVIE TAG */}
              {post.movieId && (
                <div style={{ color: "#888", fontSize: "13px" }}>
                  🎬 Movie Tag:{" "}
                  {movieTitleById[post.movieId] ?? `#${post.movieId}`}
                </div>
              )}

              {post.mediaType === "image" ? (
                <img
                  src={API + post.mediaUrl}
                  alt="post"
                  style={{ width: "100%", borderRadius: "12px" }}
                />
              ) : (
                <video
                  src={API + post.mediaUrl}
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
                  title="Like"
                >
                  ♥
                </button>
                <span>{likes[post.id]?.likesCount ?? 0} likes</span>
              </div>

              {post.caption && <p>{post.caption}</p>}

              <button onClick={() => loadComments(post.id)}>View comments</button>

              {comments[post.id]?.map((c) => (
                <div key={c.id}>
                  @{c.author?.username ?? "unknown"}: {c.text}
                </div>
              ))}

              <input
                placeholder="Add a comment..."
                value={commentText[post.id] ?? ""}
                onChange={(e) =>
                  setCommentText((p) => ({ ...p, [post.id]: e.target.value }))
                }
                onKeyDown={(e) => e.key === "Enter" && submitComment(post.id)}
                style={{
                  width: "100%",
                  marginTop: "10px",
                  padding: "10px",
                  borderRadius: "10px",
                  border: "1px solid #333",
                  background: "#0c0c0c",
                  color: "white",
                }}
              />
            </div>
          );
        })}
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
            padding: "20px",
          }}
        >
          <div
            style={{
              width: "520px",
              maxWidth: "95vw",
              background: "#111",
              padding: "20px",
              borderRadius: "12px",
              border: "1px solid #222",
            }}
          >
            <input
              type="file"
              accept="image/*,video/*"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (!f) return;
                setFile(f);
                setPreviewUrl(URL.createObjectURL(f));
              }}
              style={{ color: "white" }}
            />

            {previewUrl && file?.type.startsWith("image") && (
              <img
                src={previewUrl}
                alt="preview"
                style={{
                  width: "100%",
                  marginTop: "12px",
                  borderRadius: "12px",
                  maxHeight: "420px",
                  objectFit: "contain",
                  background: "#000",
                }}
              />
            )}

            {previewUrl && file?.type.startsWith("video") && (
              <video
                src={previewUrl}
                controls
                style={{
                  width: "100%",
                  marginTop: "12px",
                  borderRadius: "12px",
                  maxHeight: "420px",
                  background: "#000",
                }}
              />
            )}

            {/* ✅ Movie tag by name (autocomplete) */}
            <div style={{ marginTop: "12px", position: "relative" }}>
              <input
                placeholder="Tag movie by name (start typing...)"
                value={movieQuery}
                onChange={(e) => {
                  setMovieQuery(e.target.value);
                  setMovieId(null);
                }}
                style={{
                  width: "100%",
                  padding: "10px",
                  borderRadius: "10px",
                  border: "1px solid #333",
                  background: "#0c0c0c",
                  color: "white",
                }}
              />

              {(movieLoading || movieSuggestions.length > 0) && (
                <div
                  style={{
                    position: "absolute",
                    top: "44px",
                    left: 0,
                    right: 0,
                    background: "#0c0c0c",
                    border: "1px solid #333",
                    borderRadius: "10px",
                    overflow: "hidden",
                    zIndex: 50,
                  }}
                >
                  {movieLoading && (
                    <div style={{ padding: "10px", color: "#aaa" }}>
                      Searching...
                    </div>
                  )}

                  {!movieLoading &&
                    movieSuggestions.map((m) => (
                      <div
                        key={m.id}
                        onClick={() => {
                          setMovieId(m.id);
                          setMovieQuery(
                            m.release_date
                              ? `${m.title} (${m.release_date.slice(0, 4)})`
                              : m.title
                          );
                          setMovieSuggestions([]);
                        }}
                        style={{
                          padding: "10px",
                          cursor: "pointer",
                          borderBottom: "1px solid #222",
                          color: "white",
                        }}
                      >
                        {m.title}
                        {m.release_date ? (
                          <span style={{ color: "#888" }}>
                            {" "}
                            ({m.release_date.slice(0, 4)})
                          </span>
                        ) : null}
                      </div>
                    ))}
                </div>
              )}

              {movieId != null && (
                <div style={{ marginTop: "8px", color: "#888", fontSize: "13px" }}>
                  Selected movieId: {movieId}
                </div>
              )}
            </div>

            <textarea
              placeholder="Caption..."
              value={caption}
              onChange={(e) => setCaption(e.target.value)}
              style={{
                width: "100%",
                marginTop: "12px",
                padding: "10px",
                borderRadius: "10px",
                border: "1px solid #333",
                background: "#0c0c0c",
                color: "white",
                minHeight: "90px",
                resize: "vertical",
              }}
            />

            <div style={{ display: "flex", gap: "10px", marginTop: "14px" }}>
              <button
                onClick={handleUpload}
                style={{
                  padding: "10px 16px",
                  borderRadius: "10px",
                  border: "none",
                  background: "#E50914",
                  color: "white",
                  cursor: "pointer",
                  fontWeight: 600,
                }}
              >
                Upload
              </button>
              <button
                onClick={() => setShowUpload(false)}
                style={{
                  padding: "10px 16px",
                  borderRadius: "10px",
                  border: "1px solid #333",
                  background: "transparent",
                  color: "white",
                  cursor: "pointer",
                }}
              >
                Cancel
              </button>
            </div>

            <div style={{ marginTop: "10px", color: "#666", fontSize: "12px" }}>
              Tip: γράψε 2+ γράμματα στο movie field για προτάσεις από TMDb.
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FeedPage;
