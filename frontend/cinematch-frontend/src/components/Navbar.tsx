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
      {/* NAVBAR */}
      <nav
        style={{
          width: "100vw",
          maxWidth: "100%",
          height: "70px",
          backgroundColor: "#111",
          padding: "15px 20px",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          position: "fixed",
          top: 0,
          left: 0,
          zIndex: 99999,
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
          <Link style={linkStyle} to="/recast">Recast-It</Link>
          {isLoggedIn && <Link style={linkStyle} to="/profile">Profile</Link>}
          {isLoggedIn && (
            <>
              <Link style={linkStyle} to="/upload">Upload</Link>
              <Link style={linkStyle} to="/gallery">My Gallery</Link>
            </>
          )}
        </div>

        {/* RIGHT DESKTOP LINKS */}
        <div
          className="desktop-nav"
          style={{
            display: "flex",
            gap: "15px",
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
                background: "transparent",
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
            fontSize: "28px",
            cursor: "pointer",
            color: "white",
          }}
          onClick={() => setMobileOpen(true)}
        >
          ☰
        </div>
      </nav>

      {/* MOBILE OVERLAY */}
      {mobileOpen && (
        <div
          onClick={() => setMobileOpen(false)}
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            width: "100vw",
            height: "100vh",
            background: "rgba(0,0,0,0.45)",
            zIndex: 99997,
          }}
        ></div>
      )}

      {/* MOBILE SIDE MENU */}
      <div
        style={{
          position: "fixed",
          top: 0,
          right: mobileOpen ? 0 : "-70%",
          height: "100vh",
          width: "70%",
          backgroundColor: "#111",
          borderLeft: "1px solid #222",
          paddingTop: "90px",
          paddingLeft: "25px",
          display: "flex",
          flexDirection: "column",
          gap: "18px",
          zIndex: 99998,
          transition: "right 0.3s ease",
        }}
      >

        <MobileItem to="/" close={setMobileOpen}>Home</MobileItem>
        <MobileItem to="/search" close={setMobileOpen}>Search</MobileItem>
        <MobileItem to="/trending" close={setMobileOpen}>Trending</MobileItem>
        <MobileItem to="/quiz" close={setMobileOpen}>Quiz</MobileItem>
        <MobileItem to="/leaderboard" close={setMobileOpen}>Leaderboard</MobileItem>
        <MobileItem to="/recast" close={setMobileOpen}>Recast-It</MobileItem>

        {isLoggedIn && (
           <>
             <MobileItem to="/profile" close={setMobileOpen}>Profile</MobileItem>
             <MobileItem to="/upload" close={setMobileOpen}>Upload</MobileItem>
             <MobileItem to="/gallery" close={setMobileOpen}>My Gallery</MobileItem>
           </>
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
              padding: "12px 4px",
              color: "white",
              fontSize: "18px",
              cursor: "pointer",
              borderBottom: "1px solid #222",
            }}
          >
            Logout
          </div>
        )}
      </div>
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
        padding: "12px 4px",
        color: "white",
        fontSize: "18px",
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
