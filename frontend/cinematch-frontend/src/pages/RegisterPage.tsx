import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import axios from "axios";

export default function RegisterPage() {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!username.trim() || !email.trim() || !password.trim()) {
      setError("Please fill in all fields.");
      return;
    }

    setLoading(true);

    try {
      await axios.post("http://localhost:8080/auth/register", {
        username,
        email,
        password,
      });

      navigate("/login");
    } catch (err) {
      console.error(err);
      setError("Registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      style={{
        width: "100%",
        height: "calc(100vh - 70px)", // πλήρες ύψος κάτω από το navbar
        display: "flex",
        justifyContent: "center", // κεντράρισμα οριζόντια
        alignItems: "center", // κεντράρισμα κάθετα
        color: "white",
        padding: "20px",
      }}
    >
      <div
        style={{
          width: "100%",
          maxWidth: "400px",
          background: "#181818",
          padding: "30px",
          borderRadius: "12px",
          boxShadow: "0 4px 18px rgba(0,0,0,0.6)",
        }}
      >
        <h1
          style={{
            marginBottom: "20px",
            fontSize: "28px",
            textAlign: "center",
          }}
        >
          Register
        </h1>

        {error && (
          <div
            style={{
              marginBottom: "15px",
              padding: "10px",
              borderRadius: "8px",
              background: "#3b1313",
              color: "#ffb3b3",
              fontSize: "14px",
            }}
          >
            {error}
          </div>
        )}

        <form
          onSubmit={handleSubmit}
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "15px",
          }}
        >
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <label>Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              style={inputStyle}
              disabled={loading}
            />
          </div>

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
              marginTop: "10px",
              padding: "10px",
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
            {loading ? "Registering..." : "Register"}
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
          Already have an account?{" "}
          <Link to="/login" style={{ color: "#e50914" }}>
            Login
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
};
