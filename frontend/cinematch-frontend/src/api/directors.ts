// src/api/directors.ts
import axios from "axios";

const API_URL = "http://localhost:8080";

export async function getDirectorDetails(id: number) {
  const res = await axios.get(`${API_URL}/api/directors/${id}`);
  return res.data;
}
