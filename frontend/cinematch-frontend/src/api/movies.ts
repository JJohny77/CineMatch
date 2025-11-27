// src/api/movies.ts

// ----------------------------
// Τύπος για κάθε trending movie
// ----------------------------
export type TrendingMovie = {
  id: number;
  title: string;
  overview: string;
  posterPath: string;
  popularity: number;
  releaseDate: string;
};

// ----------------------------
// Βασικό URL του backend
// ----------------------------
const API_URL = "http://localhost:8080";

// ----------------------------
// Κλήση στο backend για trending movies
// ----------------------------
export async function fetchTrending(
  timeWindow: "day" | "week" = "day"
): Promise<TrendingMovie[]> {
  const response = await fetch(
    `${API_URL}/movies/trending?time_window=${timeWindow}`
  );

  if (!response.ok) {
    throw new Error("Failed to fetch trending movies");
  }

  return response.json();
}
