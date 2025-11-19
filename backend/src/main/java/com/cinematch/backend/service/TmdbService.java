package com.cinematch.backend.service;

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
 * Ανήκει στο US10 – Πασχάλης.
 */
@Service
public class TmdbService {

    private static final Logger logger = LoggerFactory.getLogger(TmdbService.class);

    private final String baseUrl;
    private final TmdbEnvService envService;
    private final RestTemplate restTemplate = new RestTemplate();

    public TmdbService(
            @Value("${tmdb.api.base-url}") String baseUrl,
            TmdbEnvService envService
    ) {
        this.baseUrl = baseUrl;
        this.envService = envService;
    }

    public String fetchFromTmdb(String path, Map<String, String> queryParams) {

        String apiKey = envService.getApiKey();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + path)
                .queryParam("api_key", apiKey);

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
}
