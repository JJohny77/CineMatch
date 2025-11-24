import { useState } from "react";
import { getStoredUser } from "../utils/auth";
import { useNavigate } from "react-router-dom";
import axios from "axios";

export default function EditProfilePage() {
  const user = getStoredUser();
  const [username, setUsername] = useState(user?.username || "");
  const navigate = useNavigate();

  async function handleSave() {
    if (!username.trim()) {
      alert("Username cannot be empty");
      return;
    }

    try {
      const token = localStorage.getItem("token");

      const response = await axios.put(
        "http://localhost:8080/user/update",
        {
          username: username,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      // ενημέρωση localStorage με όλα τα νέα δεδομένα
      localStorage.setItem("user", JSON.stringify(response.data));

      alert("Profile updated!");
      navigate("/profile");
    } catch (error) {
      console.error(error);
      alert("Failed to update profile");
    }
  }

  return (
    <div
      style={{
        paddingTop: "40px",
        color: "white",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: "20px",
      }}
    >
      <h1>Edit Profile</h1>

      <div style={{ width: "300px", display: "flex", flexDirection: "column", gap: "15px" }}>
        <div>
          <label>Username</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={{
              width: "100%",
              padding: "10px",
              borderRadius: "8px",
              border: "1px solid #555",
              background: "#222",
              color: "white",
            }}
          />
        </div>

        <div>
          <label>Email</label>
          <input
            type="text"
            defaultValue={user?.email}
            disabled
            style={{
              width: "100%",
              padding: "10px",
              borderRadius: "8px",
              border: "1px solid #555",
              background: "#333",
              color: "white",
              opacity: 0.6,
            }}
          />
        </div>

        <button
          onClick={handleSave}
          style={{
            padding: "10px",
            width: "100%",
            borderRadius: "8px",
            border: "none",
            background: "#444",
            color: "white",
            cursor: "pointer",
          }}
        >
          Save Changes
        </button>
      </div>
    </div>
  );
}
