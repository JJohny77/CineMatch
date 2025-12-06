package com.cinematch.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TmdbEnvService {

    private static final Logger logger = LoggerFactory.getLogger(TmdbEnvService.class);

    private final String apiKeyV3;
    private final String accessTokenV4;

    public TmdbEnvService(
            @Value("${tmdb.api.key:}") String apiKeyV3,
            @Value("${tmdb.access.token:}") String accessTokenV4
    ) {
        this.apiKeyV3 = apiKeyV3;
        this.accessTokenV4 = accessTokenV4;

        // Μικρό debug για να είμαστε σίγουροι τι διαβάζει
        if (this.accessTokenV4 == null || this.accessTokenV4.isBlank()) {
            logger.error("TMDb v4 access token (tmdb.access.token) ΔΕΝ είναι ρυθμισμένο!");
        } else {
            logger.info("TMDb v4 access token φορτώθηκε ({} χαρακτήρες).", this.accessTokenV4.length());
        }

        if (this.apiKeyV3 == null || this.apiKeyV3.isBlank()) {
            logger.warn("TMDb v3 api key (tmdb.api.key) δεν είναι ρυθμισμένο (ίσως να μην το χρειάζεσαι).");
        }
    }

    public String getApiKeyV3() {
        return apiKeyV3;
    }

    public String getAccessToken() {
        if (accessTokenV4 == null || accessTokenV4.isBlank()) {
            throw new IllegalStateException("TMDb v4 access token is not configured (tmdb.access.token).");
        }
        return accessTokenV4;
    }
}
