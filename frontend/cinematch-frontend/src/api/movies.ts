// src/api/movies.ts
import api from "./httpClient";

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
  const response = await api.get<TrendingMovie[]>(
    "/movies/trending",
    {
      params: { time_window: timeWindow },
    }
  );

  return response.data;
}

// =========================
// TRENDING ACTORS
// =========================
export async function fetchTrendingActors(
  timeWindow: "day" | "week" = "day"
) {
  const response = await api.get<TrendingPerson[]>(
    "/movies/trending-actors",
    {
      params: { time_window: timeWindow },
    }
  );

  return response.data;
}

// =========================
// TRENDING DIRECTORS
// =========================
export async function fetchTrendingDirectors(
  timeWindow: "day" | "week" = "week"
) {
  const response = await api.get<TrendingPerson[]>(
    "/movies/trending-directors",
    {
      params: { time_window: timeWindow },
    }
  );

  return response.data;
}

// =========================
// EXPLORE MOVIES
// =========================
export async function fetchExplore(
  params: ExploreParams = {}
): Promise<ExploreResponse> {
  const query: Record<string, string> = {};

  query.page = params.page ? String(params.page) : "1";
  if (params.sortBy) query.sortBy = params.sortBy;
  if (params.yearFrom != null) query.yearFrom = String(params.yearFrom);
  if (params.yearTo != null) query.yearTo = String(params.yearTo);
  if (params.minRating != null) query.minRating = String(params.minRating);
  if (params.castId != null) query.castId = String(params.castId);
  if (params.crewId != null) query.crewId = String(params.crewId);
  if (params.genreId != null) query.genreId = String(params.genreId);

  const response = await api.get<ExploreResponse>("/movies/explore", {
    params: query,
  });

  return response.data;
}
