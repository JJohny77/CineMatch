import { Link, useNavigate } from "react-router-dom";
import type React from "react";
import { logoutUser } from "../utils/auth";

export default function Navbar() {
      const navigate = useNavigate();

      const isLoggedIn = !!localStorage.getItem("token");

      function handleLogout() {
        logoutUser();       // καθαρίζει token + user
        navigate("/");      // γυρνάει στην αρχική
      }
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
      <Link style={linkStyle} to="/trending">Trending</Link>
      <Link style={linkStyle} to="/quiz">Quiz</Link>
      <Link style={linkStyle} to="/profile">Profile</Link>

       {/* Δεξιά πλευρά */}
            <div style={{ marginLeft: "auto", display: "flex", gap: "15px", paddingRight: "40px" }}>
              {!isLoggedIn ? (
                <>
                  <Link style={linkStyle} to="/login">
                    Login
                  </Link>
                  <Link style={linkStyle} to="/register">
                    Register
                  </Link>
                </>
                ) : (
                <button
                  onClick={handleLogout}
                  style={{
                    ...linkStyle,
                    backgroundColor: "transparent",
                    border: "none",
                    cursor: "pointer",
                    padding: 0,
                  }}
                >
                  Logout
                </button>
              )}
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
