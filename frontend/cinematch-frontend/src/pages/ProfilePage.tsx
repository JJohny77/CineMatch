import { useEffect, useState } from "react";
import { getStoredUser, logoutUser } from "../utils/auth";
import { useNavigate } from "react-router-dom";
import axios from "axios";

export default function ProfilePage() {
  const navigate = useNavigate();

  const [user, setUser] = useState(getStoredUser());

  // Load real user from backend
  useEffect(() => {
    const token = localStorage.getItem("token");

    if (!token) {
      navigate("/");
      return;
    }

    axios
      .get("http://localhost:8080/user/profile", {
        headers: { Authorization: `Bearer ${token}` },
      })
      .then((res) => {
        setUser(res.data);
        localStorage.setItem("user", JSON.stringify(res.data));
      })
      .catch(() => {
        logoutUser();
        navigate("/");
      });
  }, []);

  function handleLogout() {
    logoutUser();
    navigate("/");
  }

  if (!user) return null;

  return (
    <div
      style={{
        color: "white",
        paddingTop: "40px",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: "20px",
      }}
    >
      {/* Avatar */}
      <div
        style={{
          width: "120px",
          height: "120px",
          borderRadius: "50%",
          backgroundColor: "#333",
        }}
      />

      {/* Username */}
      <div style={{ fontSize: "28px", fontWeight: "bold" }}>
        {user.username}
      </div>

      {/* Email */}
      <div style={{ fontSize: "18px", color: "#ccc" }}>
        {user.email}
      </div>

      {/* Buttons */}
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "15px",
          marginTop: "20px",
        }}
      >
        <button
          onClick={() => navigate("/profile/edit")}
          style={{
            padding: "10px 20px",
            backgroundColor: "#444",
            color: "white",
            border: "none",
            borderRadius: "8px",
            fontSize: "16px",
            cursor: "pointer",
          }}
        >
          Edit Profile
        </button>

        <button
          onClick={handleLogout}
          style={{
            padding: "10px 20px",
            backgroundColor: "#444",
            color: "white",
            border: "none",
            borderRadius: "8px",
            fontSize: "16px",
            cursor: "pointer",
          }}
        >
          Logout
        </button>
      </div>
    </div>
  );
}
