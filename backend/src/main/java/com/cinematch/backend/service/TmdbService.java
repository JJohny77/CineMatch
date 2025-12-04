package com.cinematch.backend.service;

import com.cinematch.backend.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    // ============================================================
    //  UNIVERSAL TMDB CALLER (WITH v4 TOKEN)
    // ============================================================
    public String fetchFromTmdb(String path, Map<String, String> queryParams) {

        String accessToken = envService.getAccessToken();  // TMDB v4 token

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + path);

        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        URI uri = builder.build().encode().toUri();
        logger.info("Calling TMDb API: {}", uri);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            logger.error("TMDb HTTP Error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("TMDb HTTP Error: " + ex.getStatusCode());
        } catch (RestClientException ex) {
            logger.error("TMDb request failed", ex);
            throw new RuntimeException("Failed to call TMDb API");
        }
    }

    // ============================================================
    //  US11 — SEARCH MOVIES
    // ============================================================
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

            logger.info("TMDb Search Request URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readValue(response.getBody(), MovieSearchResponse.class);

        } catch (Exception e) {
            logger.error("Search error: {}", e.getMessage());
            throw new RuntimeException("Failed to search movies");
        }
    }

    // ============================================================
    //  US45 — EXPLORE MOVIES (DISCOVER ENDPOINT)
    // ============================================================
    public MovieSearchResponse exploreMovies(
            int page,
            String sortBy,
            Integer yearFrom,
            Integer yearTo,
            Double minRating,
            Long castId,
            Long crewId,
            Integer genreId
    ) {
        try {
            if (page < 1) page = 1;
            if (sortBy == null || sortBy.isBlank()) sortBy = "popularity.desc";

            Map<String, String> params = new HashMap<>();
            params.put("language", "en-US");
            params.put("include_adult", "false");
            params.put("page", String.valueOf(page));
            params.put("sort_by", sortBy);

            if (yearFrom != null) params.put("primary_release_date.gte", yearFrom + "-01-01");
            if (yearTo != null) params.put("primary_release_date.lte", yearTo + "-12-31");
            if (minRating != null) params.put("vote_average.gte", String.valueOf(minRating));
            if (castId != null) params.put("with_cast", String.valueOf(castId));
            if (crewId != null) params.put("with_crew", String.valueOf(crewId));
            if (genreId != null) params.put("with_genres", String.valueOf(genreId));

            String json = fetchFromTmdb("/discover/movie", params);
            return objectMapper.readValue(json, MovieSearchResponse.class);

        } catch (Exception e) {
            logger.error("Explore error: {}", e.getMessage());
            throw new RuntimeException("Failed to explore movies");
        }
    }

    // ============================================================
    // GENRES
    // ============================================================
    public GenreListResponse getMovieGenres() {
        try {
            String json = fetchFromTmdb("/genre/movie/list", Map.of("language", "en-US"));
            return objectMapper.readValue(json, GenreListResponse.class);

        } catch (Exception e) {
            logger.error("Failed to load genres: {}", e.getMessage());
            throw new RuntimeException("Failed to load movie genres");
        }
    }

    // ============================================================
    // PERSON SEARCH (ACTORS / DIRECTORS)
    // ============================================================
    public PersonSearchResponse searchPerson(String query) {
        try {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Query cannot be empty");
            }

            Map<String, String> params = new HashMap<>();
            params.put("language", "en-US");
            params.put("query", query);
            params.put("include_adult", "false");
            // προαιρετικό, για σταθερότητα
            params.put("page", "1");

            String json = fetchFromTmdb("/search/person", params);

            // μικρό debug – αν θες, μπορείς να το σβήσεις μετά
            logger.info("TMDb /search/person raw JSON for '{}': {}", query, json);

            return objectMapper.readValue(json, PersonSearchResponse.class);

        } catch (Exception e) {
            logger.error("Failed to search person with query '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Failed to search person", e);
        }
    }


    // ============================================================
    //  TRENDING MOVIES
    // ============================================================
    public List<TrendingMovieDto> getTrendingMovies(String timeWindow) {
        try {
            if (!timeWindow.equals("day") && !timeWindow.equals("week")) {
                timeWindow = "day";
            }

            String url = baseUrl + "/trending/movie/" + timeWindow +
                    "?api_key=" + apiKey +
                    "&language=en-US";

            logger.info("TMDb Trending Request URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            Map<String, Object> json = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) json.get("results");

            List<TrendingMovieDto> output = new ArrayList<>();

            for (Map<String, Object> movie : results) {
                Long id = ((Number) movie.get("id")).longValue();

                TrendingMovieDto dto = new TrendingMovieDto(
                        id,
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
            logger.error("Trending parsing error: {}", e.getMessage());
            throw new RuntimeException("Failed to load trending movies");
        }
    }

    // ============================================================
    // MOVIE DETAILS
    // ============================================================
    public MovieDetailsDto getMovieDetails(Long id) {
        try {
            if (id == null) throw new IllegalArgumentException("Movie id cannot be null");

            String jsonString = fetchFromTmdb("/movie/" + id, Map.of("language", "en-US"));
            Map<String, Object> json = objectMapper.readValue(jsonString, Map.class);

            String title = (String) json.get("title");
            String overview = (String) json.get("overview");
            String posterPath = (String) json.get("poster_path");
            String releaseDate = (String) json.get("release_date");

            Integer runtime = json.get("runtime") != null
                    ? ((Number) json.get("runtime")).intValue()
                    : null;

            Double popularity = json.get("popularity") != null
                    ? ((Number) json.get("popularity")).doubleValue()
                    : null;

            List<String> genres = new ArrayList<>();
            List<Map<String, Object>> genreList =
                    (List<Map<String, Object>>) json.get("genres");

            if (genreList != null) {
                for (Map<String, Object> g : genreList) {
                    genres.add((String) g.get("name"));
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
            logger.error("Error loading movie details for {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to load movie details");
        }
    }

    // ============================================================
    // MOVIE VIDEOS (TRAILERS)
    // ============================================================
    public List<MovieVideoDto> getMovieVideos(Long id) {
        try {
            if (id == null) throw new IllegalArgumentException("Movie id cannot be null");

            String path = "/movie/" + id + "/videos";
            String jsonString = fetchFromTmdb(path, Map.of("language", "en-US"));

            Map<String, Object> json = objectMapper.readValue(jsonString, Map.class);

            List<MovieVideoDto> videos = new ArrayList<>();
            List<Map<String, Object>> results = (List<Map<String, Object>>) json.get("results");

            if (results != null) {
                for (Map<String, Object> videoMap : results) {
                    String site = (String) videoMap.get("site");
                    String type = (String) videoMap.get("type");
                    String key = (String) videoMap.get("key");
                    String name = (String) videoMap.get("name");

                    if ("YouTube".equalsIgnoreCase(site)
                            && "Trailer".equalsIgnoreCase(type)
                            && key != null) {

                        videos.add(new MovieVideoDto(name, key, site, type));
                    }
                }
            }

            return videos;

        } catch (Exception e) {
            logger.error("Video load error for {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to load movie videos");
        }
    }

    // ============================================================
    // RAW PERSON INFO
    // ============================================================
    public String getPersonDetails(Long id) {
        return fetchFromTmdb("/person/" + id, Map.of("language", "en-US"));
    }

    public String getPersonMovieCredits(Long id) {
        return fetchFromTmdb("/person/" + id + "/movie_credits", Map.of("language", "en-US"));
    }
}
