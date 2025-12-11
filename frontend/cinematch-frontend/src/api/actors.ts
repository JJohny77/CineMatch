import axios from "axios";

const API_URL = "http://localhost:8080";

export async function getActorDetails(id: number) {
  const res = await axios.get(`${API_URL}/api/actors/${id}`);
  return res.data;
}
