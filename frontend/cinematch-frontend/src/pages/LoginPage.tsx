import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import axios from "axios";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !password.trim()) {
      setError("Please fill in all fields.");
      return;
    }

    setLoading(true);

    try {
      const res = await axios.post("http://localhost:8080/auth/login", {
        email,
        password,
      });

      const data = res.data;
      const token = typeof data === "string" ? data : data.token || null;

      if (!token) throw new Error("No token returned");

      localStorage.setItem("token", token);
      localStorage.removeItem("user");

      navigate("/profile");
    } catch (err) {
      console.error(err);
      setError("Login failed. Please check your credentials.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      style={{
        height: "calc(100vh - 70px)",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        padding: "0 0px",
        color: "white",
      }}
    >
      <div
        style={{
          width: "100%",
          maxWidth: "420px",
          background: "#181818",
          padding: "40px 32px",
          borderRadius: "12px",
          boxShadow: "0 4px 18px rgba(0,0,0,0.6)",
        }}
      >
        <h1 style={{ marginBottom: "25px", fontSize: "28px", textAlign: "center" }}>
          Login
        </h1>

        {error && (
          <div
            style={{
              marginBottom: "15px",
              padding: "10px",
              borderRadius: "8px",
              background: "#3b1313",
              color: "#ffb3b3",
              textAlign: "center",
              fontSize: "14px",
            }}
          >
            {error}
          </div>
        )}

        <form
          onSubmit={handleSubmit}
          style={{ display: "flex", flexDirection: "column", gap: "15px" }}
        >
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <label>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              style={inputStyle}
              disabled={loading}
            />
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={inputStyle}
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: "100%",
              padding: "12px",
              borderRadius: "8px",
              border: "none",
              background: "#e50914",
              color: "white",
              fontWeight: "bold",
              fontSize: "16px",
              cursor: loading ? "default" : "pointer",
              opacity: loading ? 0.7 : 1,
            }}
          >
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>

        <p
          style={{
            marginTop: "15px",
            fontSize: "14px",
            textAlign: "center",
            opacity: 0.8,
          }}
        >
          Don't have an account?{" "}
          <Link to="/register" style={{ color: "#e50914" }}>
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  width: "100%",
  padding: "10px",
  borderRadius: "8px",
  border: "1px solid #444",
  background: "#222",
  color: "white",
  fontSize: "14px",
  boxSizing: "border-box", // 🔥 FIX
};

