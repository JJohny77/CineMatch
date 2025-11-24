import { Link } from "react-router-dom";

export default function Navbar() {
  return (
    <nav
      style={{
        width: "100%",
        height: "70px",
        backgroundColor: "#111",
        padding: "15px 25px",
        display: "flex",
        alignItems: "center",
        gap: "25px",
        position: "fixed",
        top: 0,
        left: 0,
        zIndex: 1000,
        borderBottom: "1px solid #222",
      }}
    >
      <Link style={linkStyle} to="/">Home</Link>
      <Link style={linkStyle} to="/movies">Movies</Link>
      <Link style={linkStyle} to="/search">Search</Link>
      <Link style={linkStyle} to="/trending">Trending</Link>
      <Link style={linkStyle} to="/quiz">Quiz</Link>
      <Link style={linkStyle} to="/leaderboard">Leaderboard</Link>
      <Link style={linkStyle} to="/profile">Profile</Link>
      <div style={{ marginLeft: "auto" }}>
        <Link style={linkStyle} to="/login">Login / Logout</Link>
      </div>
    </nav>
  );
}

const linkStyle: React.CSSProperties = {
  color: "white",
  fontSize: "18px",
  textDecoration: "none",
  fontWeight: 500,
};
