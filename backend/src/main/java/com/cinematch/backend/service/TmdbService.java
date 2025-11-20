package com.cinematch.backend.service;

import com.cinematch.backend.dto.MovieSearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Πραγματικό TMDb Client Service.
 * Ανήκει στα US10 (low-level) + US11 (search handler).
 */
@Service
public class TmdbService {

    private static final Logger logger = LoggerFactory.getLogger(TmdbService.class);

    private final String baseUrl;
    private final TmdbEnvService envService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TmdbService(
            @Value("${tmdb.api.base-url}") String baseUrl,
            TmdbEnvService envService
    ) {
        this.baseUrl = baseUrl;
        this.envService = envService;
    }

    /**
     * LOW LEVEL METHOD (US10)
     * Παίρνει raw JSON σαν String από οποιοδήποτε endpoint του TMDb.
     */
    public String fetchFromTmdb(String path, Map<String, String> queryParams) {

        String apiKey = envService.getApiKey();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + path)
                .queryParam("api_key", apiKey);

        // extra params όπως ?query=... κλπ
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        URI uri = builder.build().encode().toUri();
        logger.info("Calling TMDb API: {}", uri);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            logger.error("TMDb HTTP error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("TMDb HTTP Error: " + ex.getStatusCode());
        } catch (RestClientException ex) {
            logger.error("TMDb client error", ex);
            throw new RuntimeException("Failed to call TMDb API");
        }
    }

    /**
     * HIGH LEVEL METHOD (US11)
     * Κάνει search ταινιών στο TMDb και επιστρέφει DTO για το backend.
     *
     * Endpoint στο backend:
     *   GET /movies/search?query=...
     */
    public MovieSearchResponse searchMovies(String query) {

        // 1. endpoint path του TMDb
        String path = "/search/movie";

        // 2. extra query params
        Map<String, String> params = Map.of(
                "query", query,
                "language", "en-US",
                "include_adult", "false"
        );

        // 3. Παίρνουμε raw JSON από το TMDb
        String json = fetchFromTmdb(path, params);

        // 4. Κάνουμε map το JSON στα DTO μας
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, MovieSearchResponse.class);

        } catch (Exception e) {
            logger.error("Failed to parse TMDb response", e);
            throw new RuntimeException("Failed to parse TMDb response");
        }
    }

}
