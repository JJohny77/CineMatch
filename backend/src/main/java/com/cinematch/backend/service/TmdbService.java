package com.cinematch.backend.service;

import com.cinematch.backend.dto.TrendingMovieDto;
import com.cinematch.backend.dto.MovieDetailsDto;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.util.UriUtils;
import com.cinematch.backend.dto.MovieVideoDto;

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

    @Value("${tmdb.api.key}")
    private String apiKey;


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
        try {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Query cannot be empty");
            }

            String url = baseUrl + "/search/movie" +
                    "?api_key=" + apiKey +
                    "&query=" + UriUtils.encode(query, StandardCharsets.UTF_8) +
                    "&language=en-US" +
                    "&include_adult=false";

            logger.info("TMDb Search Request URL: " + url);

            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            // Μετατροπή JSON → DTO (όπως στο trending αλλά με structured DTO)
            return objectMapper.readValue(response.getBody(), MovieSearchResponse.class);

        } catch (Exception e) {
            logger.error("Error while searching movies: " + e.getMessage());
            throw new RuntimeException("Failed to search movies");
        }
    }
    public List<TrendingMovieDto> getTrendingMovies(String timeWindow) {
        try {
            if (!timeWindow.equals("day") && !timeWindow.equals("week")) {
                timeWindow = "day";
            }

            String url = baseUrl + "/trending/movie/" + timeWindow +
                    "?api_key=" + apiKey +
                    "&language=en-US";


            logger.info("TMDb Trending Request URL: " + url);

            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            Map<String, Object> json =
                    objectMapper.readValue(response.getBody(), Map.class);

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) json.get("results");

            List<TrendingMovieDto> output = new ArrayList<>();

            for (Map<String, Object> movie : results) {
                TrendingMovieDto dto = new TrendingMovieDto(
                        (String) movie.get("title"),
                        (String) movie.get("overview"),
                        (String) movie.get("poster_path"),
                        movie.get("popularity") != null ?
                                ((Number) movie.get("popularity")).doubleValue() : 0.0,
                        (String) movie.get("release_date")
                );

                output.add(dto);
            }

            return output;

        } catch (Exception e) {
            logger.error("Error while parsing Trending Movies: " + e.getMessage());
            throw new RuntimeException("Failed to load trending movies");
        }
    }

    public MovieDetailsDto getMovieDetails(Long id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Movie id cannot be null");
            }

            // Χρησιμοποιούμε το low-level fetchFromTmdb
            String path = "/movie/" + id;

            String jsonString = fetchFromTmdb(path, Map.of(
                    "language", "en-US"
            ));

            Map<String, Object> json =
                    objectMapper.readValue(jsonString, Map.class);

            String title = (String) json.get("title");
            String overview = (String) json.get("overview");
            String posterPath = (String) json.get("poster_path");
            String releaseDate = (String) json.get("release_date");

            Integer runtime = null;
            if (json.get("runtime") != null) {
                runtime = ((Number) json.get("runtime")).intValue();
            }

            Double popularity = null;
            if (json.get("popularity") != null) {
                popularity = ((Number) json.get("popularity")).doubleValue();
            }

            // genres: έρχονται ως λίστα από objects { id, name }
            List<String> genres = new ArrayList<>();
            Object genresObj = json.get("genres");
            if (genresObj instanceof List<?> list) {
                for (Object g : list) {
                    if (g instanceof Map<?, ?> genreMap) {
                        Object nameObj = genreMap.get("name");
                        if (nameObj != null) {
                            genres.add(nameObj.toString());
                        }
                    }
                }
            }

            return new MovieDetailsDto(
                    title,
                    overview,
                    posterPath,
                    releaseDate,
                    runtime,
                    popularity,
                    genres
            );

        } catch (Exception e) {
            logger.error("Error while fetching movie details for id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to load movie details");
        }
    }

    public List<MovieVideoDto> getMovieVideos(Long id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Movie id cannot be null");
            }

            // TMDb endpoint: /movie/{id}/videos
            String path = "/movie/" + id + "/videos";

            String jsonString = fetchFromTmdb(path, Map.of(
                    "language", "en-US"
            ));

            Map<String, Object> json =
                    objectMapper.readValue(jsonString, Map.class);

            Object resultsObj = json.get("results");
            List<MovieVideoDto> videos = new ArrayList<>();

            if (resultsObj instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> videoMap) {

                        String site = (String) videoMap.get("site");
                        String type = (String) videoMap.get("type");
                        String key = (String) videoMap.get("key");
                        String name = (String) videoMap.get("name");

                        // Κρατάμε μόνο YouTube trailers
                        if ("YouTube".equalsIgnoreCase(site)
                                && "Trailer".equalsIgnoreCase(type)
                                && key != null) {

                            videos.add(new MovieVideoDto(
                                    name,
                                    key,
                                    site,
                                    type
                            ));
                        }
                    }
                }
            }

            return videos;

        } catch (Exception e) {
            logger.error("Error while fetching movie videos for id {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to load movie videos");
        }
    }

}
