import React, { useEffect, useState } from "react";

type LeaderboardEntry = {
  email: string;
  score: number;
};

const LeaderboardPage: React.FC = () => {
  const [data, setData] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("token");

    if (!token) {
      console.error("No token found!");
      setLoading(false);
      return;
    }

    fetch("http://localhost:8080/quiz/leaderboard", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then((res) => res.json())
      .then((json) => {
        setData(json);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  if (loading) return <div style={{ color: "white" }}>Loading...</div>;

  return (
    <div style={{ color: "white", padding: "20px" }}>
      <h1>Leaderboard</h1>
      {data.length === 0 && <p>No scores yet.</p>}

      <ul>
        {data.map((item, index) => (
          <li key={index}>
            {index + 1}. {item.email} — {item.score} pts
          </li>
        ))}
      </ul>
    </div>
  );
};

export default LeaderboardPage;
