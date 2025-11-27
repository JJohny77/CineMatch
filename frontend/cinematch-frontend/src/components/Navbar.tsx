import { Link, useNavigate } from "react-router-dom";
import type React from "react";
import { useState } from "react";
import { logoutUser } from "../utils/auth";

export default function Navbar() {
  const navigate = useNavigate();
  const isLoggedIn = !!localStorage.getItem("token");

  const [mobileOpen, setMobileOpen] = useState(false);

  function handleLogout() {
    logoutUser();
    navigate("/");
    setMobileOpen(false);
  }

  return (
    <>
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
        {/* DESKTOP LINKS */}
        <div className="desktop-nav" style={{ display: "flex", gap: "25px" }}>
          <Link style={linkStyle} to="/">Home</Link>
          <Link style={linkStyle} to="/search">Search</Link>
          <Link style={linkStyle} to="/trending">Trending</Link>
          <Link style={linkStyle} to="/quiz">Quiz</Link>
          <Link style={linkStyle} to="/leaderboard">Leaderboard</Link>
          {isLoggedIn && <Link style={linkStyle} to="/profile">Profile</Link>}
        </div>

        {/* RIGHT SIDE FOR DESKTOP */}
        <div
          className="desktop-nav"
          style={{
            marginLeft: "auto",
            display: "flex",
            gap: "15px",
            paddingRight: "40px",
          }}
        >
          {!isLoggedIn ? (
            <>
              <Link style={linkStyle} to="/login">Login</Link>
              <Link style={linkStyle} to="/register">Register</Link>
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

        {/* MOBILE BURGER */}
        <div
          className="mobile-burger"
          style={{
            marginLeft: "auto",
            display: "none",
            fontSize: "28px",
            cursor: "pointer",
            color: "white",
          }}
          onClick={() => setMobileOpen((prev) => !prev)}
        >
          ☰
        </div>
      </nav>

      {/* MOBILE DROPDOWN MENU */}
      {mobileOpen && (
        <div
          className="mobile-menu"
          style={{
            position: "fixed",
            top: "70px",
            right: "15px",
            width: "190px",
            background: "#111",
            border: "1px solid #333",
            borderRadius: "8px",
            zIndex: 999,
            padding: "10px 0",
          }}
        >
          <MobileItem to="/" close={setMobileOpen}>Home</MobileItem>
          <MobileItem to="/search" close={setMobileOpen}>Search</MobileItem>
          <MobileItem to="/trending" close={setMobileOpen}>Trending</MobileItem>
          <MobileItem to="/quiz" close={setMobileOpen}>Quiz</MobileItem>
          <MobileItem to="/leaderboard" close={setMobileOpen}>Leaderboard</MobileItem>

          {isLoggedIn && (
            <MobileItem to="/profile" close={setMobileOpen}>Profile</MobileItem>
          )}

          {!isLoggedIn ? (
            <>
              <MobileItem to="/login" close={setMobileOpen}>Login</MobileItem>
              <MobileItem to="/register" close={setMobileOpen}>Register</MobileItem>
            </>
          ) : (
            <div
              onClick={handleLogout}
              style={{
                padding: "12px 18px",
                color: "white",
                fontSize: "16px",
                borderBottom: "1px solid #222",
                cursor: "pointer",
              }}
            >
              Logout
            </div>
          )}
        </div>
      )}
    </>
  );
}

function MobileItem({ to, children, close }) {
  return (
    <Link
      to={to}
      onClick={() => close(false)}
      style={{
        display: "block",
        padding: "12px 18px",
        color: "white",
        fontSize: "16px",
        textDecoration: "none",
        borderBottom: "1px solid #222",
      }}
    >
      {children}
    </Link>
  );
}

const linkStyle: React.CSSProperties = {
  color: "white",
  fontSize: "18px",
  textDecoration: "none",
  fontWeight: 500,
};
