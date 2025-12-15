// src/api/httpClient.ts
import axios from "axios";

// Σταθερό base URL όπως πριν
const API_URL = "http://localhost:8080";

const api = axios.create({
  baseURL: API_URL,
});

// Request interceptor: βάζει ΠΑΝΤΑ το JWT token αν υπάρχει
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      if (!config.headers) {
        config.headers = {};
      }
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default api;
