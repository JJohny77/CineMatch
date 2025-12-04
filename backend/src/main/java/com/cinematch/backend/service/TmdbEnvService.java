package com.cinematch.backend.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;

@Service
public class TmdbEnvService {

    private final Dotenv dotenv;
    private final String apiKey;
    private final String accessToken;

    public TmdbEnvService(Dotenv dotenv) {
        this.dotenv = dotenv;

        // Load TMDB API key (v3)
        this.apiKey = dotenv.get("TMDB_API_KEY");
        System.out.println("Loaded TMDB API key: " + apiKey);

        // Load TMDB Access Token (v4)
        this.accessToken = dotenv.get("tmdb.access.token");
        System.out.println("Loaded TMDB v4 Access Token: " + accessToken);
    }

    // v3 API KEY
    public String getApiKey() {
        return apiKey;
    }

    // v4 ACCESS TOKEN (Bearer)
    public String getAccessToken() {
        return accessToken;
    }
}
