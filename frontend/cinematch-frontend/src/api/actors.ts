// src/api/actors.ts
import api from "./httpClient";

export async function getActorDetails(id: number) {
  const res = await api.get(`/api/actors/${id}`);
  return res.data;
}
