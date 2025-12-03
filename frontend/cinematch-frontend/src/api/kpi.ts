// src/api/kpi.ts
import axios from "axios";

const API_URL = "http://localhost:8080";

export type RawKpiResponse = any;

function extractScore(data: RawKpiResponse): number {
  console.log("KPI raw data:", data); // optional debug

  // 1) Σκέτος αριθμός
  if (typeof data === "number") return data;

  if (!data) return 0;

  // 2) Αν είναι array, ψάχνουμε μέσα του
  if (Array.isArray(data)) {
    for (const item of data) {
      const val = extractScore(item);
      if (val !== 0) return val;
    }
    return 0;
  }

  // 3) Προτιμητέα keys για KPIs
  const candidateKeys = [
    "score",
    "value",
    "starPower",
    "star_power",
    "starPowerScore",
    "star_power_score",
    "audienceEngagement",
    "audience_engagement",
    "audienceEngagementScore",
    "audience_engagement_score",
    "engagement",          // 👈 DTO του AudienceEngagementResponse
    "kpi",
    "kpiScore",
    "rating",
  ];

  for (const key of candidateKeys) {
    if (typeof (data as any)[key] === "number") {
      return (data as any)[key];
    }
  }

  // 4) Fallback: πάρε το πρώτο αριθμητικό ΠΛΗΝ id fields
  const ignoreKeys = [
    "id",
    "movieId",
    "movieID",
    "tmdbId",
    "tmdbID",
    "tmdb_id",
  ];

  for (const key in data as any) {
    if (ignoreKeys.includes(key)) continue;
    if (typeof (data as any)[key] === "number") {
      return (data as any)[key];
    }
  }

  // Αν δεν βρούμε τίποτα
  return 0;
}

// ⭐ Star Power (MOVIE)
export async function fetchStarPower(
  movieId: string | number
): Promise<number> {
  const response = await axios.get<RawKpiResponse>(
    `${API_URL}/kpi/star-power/movie/${movieId}`
  );
  return extractScore(response.data);
}

// 🎭 Audience Engagement
export async function fetchAudienceEngagement(
  movieId: string | number
): Promise<number> {
  const response = await axios.get<RawKpiResponse>(
    `${API_URL}/kpi/audience-engagement/${movieId}`
  );
  return extractScore(response.data);
}
