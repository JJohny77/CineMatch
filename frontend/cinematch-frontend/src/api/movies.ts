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
// Explore movie types
// ----------------------------
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

const API_URL = "http://localhost:8080";

// ----------------------------
// Trending
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

// ----------------------------
// Explore parameters (EXTENDED)
// ----------------------------
export type ExploreParams = {
  page?: number;
  sortBy?: string;
  yearFrom?: number | null;
  yearTo?: number | null;
  minRating?: number | null;

  // NEW filters
  castId?: number | null;     // actor ID
  crewId?: number | null;     // director ID
  genreId?: number | null;    // TMDb genre ID
};

// ----------------------------
// Explore API call
// ----------------------------
export async function fetchExplore(
  params: ExploreParams = {}
): Promise<ExploreResponse> {
  const searchParams = new URLSearchParams();

  // Page
  searchParams.set("page", params.page ? String(params.page) : "1");

  // Sort
  if (params.sortBy) searchParams.set("sortBy", params.sortBy);

  // Year filters
  if (params.yearFrom != null) searchParams.set("yearFrom", String(params.yearFrom));
  if (params.yearTo != null) searchParams.set("yearTo", String(params.yearTo));

  // Minimum rating
  if (params.minRating != null) searchParams.set("minRating", String(params.minRating));

  // NEW FILTERS
  if (params.castId != null) searchParams.set("castId", String(params.castId));
  if (params.crewId != null) searchParams.set("crewId", String(params.crewId));
  if (params.genreId != null) searchParams.set("genreId", String(params.genreId));

  const queryString = searchParams.toString();
  const url = `${API_URL}/movies/explore?${queryString}`;

  const response = await fetch(url);
  if (!response.ok) throw new Error("Failed to explore movies");

  return (await response.json()) as ExploreResponse;
}
