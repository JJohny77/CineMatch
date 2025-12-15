// src/api/directors.ts
import api from "./httpClient";

export async function getDirectorDetails(id: number) {
  const res = await api.get(`/api/directors/${id}`);
  return res.data;
}
