// src/api/movies.ts

// =========================
// CONSTANTS
// =========================
const API_URL = "http://localhost:8080";

// =========================
// TYPES
// =========================

export type TrendingMovie = {
  id: number;
  title: string;
  overview: string;
  posterPath: string;
  popularity: number;
  releaseDate: string;
};

export type TrendingPerson = {
  id: number;
  name: string;
  profilePath: string | null;
  department: string;
  popularity: number;
};

export type ExploreMovie = {
  id: number;
  title: string;
  overview: string;
  poster_path: string | null;
  backdrop_path?: string | null;
  original_title?: string;
  original_language?: string;
  popularity?: number;
  release_date?: string;
  vote_average?: number;
  vote_count?: number;
  genre_ids?: number[];
};

export type ExploreResponse = {
  page: number;
  results: ExploreMovie[];
  total_pages: number;
  total_results: number;
};

export type ExploreParams = {
  page?: number;
  sortBy?: string;
  yearFrom?: number | null;
  yearTo?: number | null;
  minRating?: number | null;
  castId?: number | null;
  crewId?: number | null;
  genreId?: number | null;
};

// =========================
// TRENDING MOVIES
// =========================
export async function fetchTrending(
  timeWindow: "day" | "week" = "day"
): Promise<TrendingMovie[]> {
  const response = await fetch(
    `${API_URL}/movies/trending?time_window=${timeWindow}`
  );

  if (!response.ok) throw new Error("Failed to fetch trending movies");

  return response.json();
}

// =========================
// TRENDING ACTORS
// =========================
export async function fetchTrendingActors(
  timeWindow: "day" | "week" = "day"
) {
  const res = await fetch(
    `${API_URL}/movies/trending-actors?time_window=${timeWindow}`
  );
  if (!res.ok) throw new Error("Failed to fetch trending actors");
  return res.json();
}

// =========================
// TRENDING DIRECTORS
// =========================
export async function fetchTrendingDirectors(
  timeWindow: "day" | "week" = "week"
) {
  const res = await fetch(
    `${API_URL}/movies/trending-directors?time_window=${timeWindow}`
  );
  if (!res.ok) throw new Error("Failed to fetch trending directors");
  return res.json();
}

// =========================
// EXPLORE MOVIES
// =========================
export async function fetchExplore(
  params: ExploreParams = {}
): Promise<ExploreResponse> {
  const searchParams = new URLSearchParams();

  searchParams.set("page", params.page ? String(params.page) : "1");
  if (params.sortBy) searchParams.set("sortBy", params.sortBy);
  if (params.yearFrom != null) searchParams.set("yearFrom", String(params.yearFrom));
  if (params.yearTo != null) searchParams.set("yearTo", String(params.yearTo));
  if (params.minRating != null) searchParams.set("minRating", String(params.minRating));

  if (params.castId != null) searchParams.set("castId", String(params.castId));
  if (params.crewId != null) searchParams.set("crewId", String(params.crewId));
  if (params.genreId != null) searchParams.set("genreId", String(params.genreId));

  const url = `${API_URL}/movies/explore?${searchParams.toString()}`;

  const response = await fetch(url);
  if (!response.ok) throw new Error("Failed to explore movies");

  return response.json();
}
