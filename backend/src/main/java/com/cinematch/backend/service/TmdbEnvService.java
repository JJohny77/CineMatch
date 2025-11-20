package com.cinematch.backend.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;

@Service
// @Service λέει στο Spring ότι αυτή η κλάση είναι Service Bean
public class TmdbEnvService {

    private final String apiKey;

    // Constructor injection: το Spring δίνει το Dotenv bean που φτιάξαμε στο EnvConfig
    public TmdbEnvService(Dotenv dotenv) {

        // Παίρνουμε την τιμή TMDB_API_KEY από το αρχείο .env
        this.apiKey = dotenv.get("TMDB_API_KEY");

        // Test print για να δούμε αν φορτώθηκε σωστά το .env
        System.out.println("Loaded TMDB API key: " + apiKey);
    }

    // Μέθοδος που επιστρέφει το TMDB API key
    public String getApiKey() {
        return apiKey;
    }
}

