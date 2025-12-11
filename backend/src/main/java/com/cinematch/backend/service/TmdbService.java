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

import java.net.URI;
import java.util.*;

@Service
public class TmdbService {

    private static final Logger logger = LoggerFactory.getLogger(TmdbService.class);

    private final String baseUrl;
    private final TmdbEnvService envService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tmdb.api.key:}")
    private String apiKey;

    public TmdbService(
            @Value("${tmdb.api.base-url}") String baseUrl,
            TmdbEnvService envService
    ) {
        this.baseUrl = baseUrl;
        this.envService = envService;
    }

    // ============================================================
    // UNIVERSAL TMDB CALLER (WITH v4 TOKEN)
    // ============================================================
    public String fetchFromTmdb(String path, Map<String, String> queryParams) {

        String accessToken = envService.getAccessToken();

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(baseUrl + path);

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
    // US11 — SEARCH MOVIES
    // ============================================================
    public MovieSearchResponse searchMovies(String query) {
        try {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Query cannot be empty");
            }

            Map<String, String> params = new HashMap<>();
            params.put("query", query);
            params.put("language", "en-US");
            params.put("include_adult", "false");

            String json = fetchFromTmdb("/search/movie", params);
            return objectMapper.readValue(json, MovieSearchResponse.class);

        } catch (Exception e) {
            logger.error("Search error: {}", e.getMessage());
            throw new RuntimeException("Failed to search movies");
        }
    }

    // ============================================================
    // US45 — EXPLORE MOVIES
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
            String json =
                    fetchFromTmdb("/genre/movie/list", Map.of("language", "en-US"));
            return objectMapper.readValue(json, GenreListResponse.class);

        } catch (Exception e) {
            logger.error("Failed to load genres: {}", e.getMessage());
            throw new RuntimeException("Failed to load movie genres");
        }
    }

    // ============================================================
    // TRENDING MOVIES
    // ============================================================
    public List<TrendingMovieDto> getTrendingMovies(String timeWindow) {
        try {
            if (!"day".equals(timeWindow) && !"week".equals(timeWindow)) {
                timeWindow = "day";
            }

            String json = fetchFromTmdb(
                    "/trending/movie/" + timeWindow,
                    Map.of("language", "en-US")
            );

            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) map.get("results");

            List<TrendingMovieDto> output = new ArrayList<>();

            if (results != null) {
                for (Map<String, Object> movie : results) {
                    output.add(new TrendingMovieDto(
                            ((Number) movie.get("id")).longValue(),
                            (String) movie.get("title"),
                            (String) movie.get("overview"),
                            (String) movie.get("poster_path"),
                            movie.get("popularity") != null
                                    ? ((Number) movie.get("popularity")).doubleValue()
                                    : 0.0,
                            (String) movie.get("release_date")
                    ));
                }
            }

            return output;

        } catch (Exception e) {
            logger.error("Trending error: {}", e.getMessage());
            throw new RuntimeException("Failed to load trending movies");
        }
    }

    public List<TrendingPersonDto> getTrendingActors(String timeWindow) {
        try {
            if (!"day".equals(timeWindow) && !"week".equals(timeWindow)) {
                timeWindow = "day";
            }

            String json = fetchFromTmdb(
                    "/trending/person/" + timeWindow,
                    Map.of("language", "en-US")
            );

            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) map.get("results");

            List<TrendingPersonDto> output = new ArrayList<>();

            if (results != null) {
                for (Map<String, Object> person : results) {

                    String department = (String) person.get("known_for_department");

                    // ONLY ACTORS
                    if (!"Acting".equalsIgnoreCase(department)) continue;

                    output.add(new TrendingPersonDto(
                            ((Number) person.get("id")).longValue(),
                            (String) person.get("name"),
                            (String) person.get("profile_path"),
                            department,
                            person.get("popularity") != null
                                    ? ((Number) person.get("popularity")).doubleValue()
                                    : 0.0
                    ));
                }
            }

            return output;

        } catch (Exception e) {
            logger.error("Trending actors error: {}", e.getMessage());
            throw new RuntimeException("Failed to load trending actors");
        }
    }

    public List<TrendingPersonDto> getTrendingDirectors(String timeWindow) {
        try {
            if (!"day".equals(timeWindow) && !"week".equals(timeWindow)) {
                timeWindow = "day";
            }

            String json = fetchFromTmdb(
                    "/trending/person/" + timeWindow,
                    Map.of("language", "en-US")
            );

            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) map.get("results");

            List<TrendingPersonDto> output = new ArrayList<>();

            if (results != null) {
                for (Map<String, Object> person : results) {

                    String department = (String) person.get("known_for_department");

                    // ONLY DIRECTORS
                    if (!"Directing".equalsIgnoreCase(department)) continue;

                    output.add(new TrendingPersonDto(
                            ((Number) person.get("id")).longValue(),
                            (String) person.get("name"),
                            (String) person.get("profile_path"),
                            department,
                            person.get("popularity") != null
                                    ? ((Number) person.get("popularity")).doubleValue()
                                    : 0.0
                    ));
                }
            }

            return output;

        } catch (Exception e) {
            logger.error("Trending directors error: {}", e.getMessage());
            throw new RuntimeException("Failed to load trending directors");
        }
    }


    // ============================================================
    // MOVIE DETAILS
    // ============================================================
    public MovieDetailsDto getMovieDetails(Long id) {
        try {
            if (id == null) throw new IllegalArgumentException("Movie id cannot be null");

            String jsonString =
                    fetchFromTmdb("/movie/" + id, Map.of("language", "en-US"));

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
                    title, overview, posterPath, releaseDate,
                    runtime, popularity, genres
            );

        } catch (Exception e) {
            logger.error("Error loading movie details for {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to load movie details");
        }
    }

    // ============================================================
    // MOVIE VIDEOS
    // ============================================================
    public List<MovieVideoDto> getMovieVideos(Long id) {
        try {
            if (id == null) throw new IllegalArgumentException("Movie id cannot be null");

            String jsonString =
                    fetchFromTmdb("/movie/" + id + "/videos", Map.of("language", "en-US"));

            Map<String, Object> json =
                    objectMapper.readValue(jsonString, Map.class);

            List<MovieVideoDto> videos = new ArrayList<>();
            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) json.get("results");

            if (results != null) {
                for (Map<String, Object> v : results) {
                    String site = (String) v.get("site");
                    String type = (String) v.get("type");
                    String key = (String) v.get("key");
                    String name = (String) v.get("name");

                    if ("YouTube".equalsIgnoreCase(site)
                            && "Trailer".equalsIgnoreCase(type)
                            && key != null) {

                        videos.add(new MovieVideoDto(name, key, site, type));
                    }
                }
            }

            return videos;

        } catch (Exception e) {
            logger.error("Video load error: {}", e.getMessage());
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
        return fetchFromTmdb("/person/" + id + "/movie_credits",
                Map.of("language", "en-US"));
    }

    // ============================================================
    // US47 — PERSON SEARCH (ACTORS / DIRECTORS)
    // ============================================================
    public PersonSearchResponseDto searchPerson(String query, int page) {
        try {
            if (query == null || query.trim().length() < 2) {
                throw new IllegalArgumentException("Query must be at least 2 characters");
            }
            if (page < 1) page = 1;

            Map<String, String> params = new HashMap<>();
            params.put("language", "en-US");
            params.put("query", query.trim());
            params.put("include_adult", "false");
            params.put("page", String.valueOf(page));

            String json = fetchFromTmdb("/search/person", params);
            logger.info("TMDb /search/person response: {}", json);

            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            PersonSearchResponseDto response = new PersonSearchResponseDto();
            response.setPage(((Number) map.get("page")).intValue());
            response.setTotalPages(((Number) map.get("total_pages")).intValue());
            response.setTotalResults(((Number) map.get("total_results")).longValue());

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) map.get("results");

            List<PersonSearchResultDto> dtoList = new ArrayList<>();

            if (results != null) {
                for (Map<String, Object> p : results) {

                    PersonSearchResultDto dto = new PersonSearchResultDto();
                    dto.setId(((Number) p.get("id")).longValue());
                    dto.setName((String) p.get("name"));
                    dto.setProfilePath((String) p.get("profile_path"));
                    dto.setKnownForDepartment((String) p.get("known_for_department"));
                    dto.setPopularity(
                            p.get("popularity") != null
                                    ? ((Number) p.get("popularity")).doubleValue()
                                    : 0.0
                    );

                    // Extract known_for (top 3)
                    List<String> titles = new ArrayList<>();

                    List<Map<String, Object>> knownFor =
                            (List<Map<String, Object>>) p.get("known_for");

                    if (knownFor != null) {
                        for (int i = 0; i < knownFor.size() && i < 3; i++) {
                            Map<String, Object> k = knownFor.get(i);

                            String title =
                                    k.get("title") != null
                                            ? (String) k.get("title")
                                            : (String) k.get("name");

                            if (title != null) titles.add(title);
                        }
                    }

                    dto.setKnownFor(titles);
                    dtoList.add(dto);
                }
            }

            response.setResults(dtoList);
            return response;

        } catch (Exception e) {
            logger.error("Failed to search person '{}': {}", query, e.getMessage());
            throw new RuntimeException("Failed to search persons");
        }
    }

    // ============================================================
// US49 — ACTOR DETAILS + CREDITS
// ============================================================
    public ActorDetailsDto getActorDetails(Long id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Actor id cannot be null");
            }

            // 1) Load raw person details
            String personJson = getPersonDetails(id);
            Map<String, Object> person = objectMapper.readValue(personJson, Map.class);

            // Αν δεν υπάρχει το άτομο → 404
            if (person.get("id") == null) {
                throw new RuntimeException("Actor not found");
            }

            // 2) Load raw movie credits
            String creditsJson = getPersonMovieCredits(id);
            Map<String, Object> credits = objectMapper.readValue(creditsJson, Map.class);

            // ===========================
            // BUILD DTO
            // ===========================
            ActorDetailsDto dto = new ActorDetailsDto();

            dto.setId(((Number) person.get("id")).longValue());
            dto.setName((String) person.get("name"));
            dto.setProfilePath((String) person.get("profile_path"));
            dto.setBiography((String) person.get("biography"));
            dto.setBirthday((String) person.get("birthday"));
            dto.setPlaceOfBirth((String) person.get("place_of_birth"));

            // ---------------------------
            // KNOWN FOR: top 5–10 movies
            // ---------------------------
            List<Map<String, Object>> castCredits =
                    (List<Map<String, Object>>) credits.get("cast");

            List<KnownForDto> knownForList = new ArrayList<>();

            if (castCredits != null) {
                int limit = Math.min(castCredits.size(), 10);

                for (int i = 0; i < limit; i++) {
                    Map<String, Object> m = castCredits.get(i);

                    KnownForDto k = new KnownForDto();
                    k.setMovieId(((Number) m.get("id")).longValue());
                    k.setTitle((String) m.getOrDefault("title", m.get("original_title")));
                    k.setPosterPath((String) m.get("poster_path"));

                    knownForList.add(k);
                }
            }

            dto.setKnownFor(knownForList);

            // ---------------------------
            // FILMOGRAPHY (full cast roles)
            // ---------------------------
            List<FilmographyDto> filmography = new ArrayList<>();

            if (castCredits != null) {
                for (Map<String, Object> m : castCredits) {
                    FilmographyDto f = new FilmographyDto();

                    f.setMovieId(((Number) m.get("id")).longValue());
                    f.setTitle((String) m.getOrDefault("title", m.get("original_title")));
                    f.setCharacter((String) m.get("character"));

                    // Extract release year (nullable)
                    String date = (String) m.get("release_date");
                    if (date != null && date.length() >= 4) {
                        f.setReleaseYear(Integer.valueOf(date.substring(0, 4)));
                    }

                    filmography.add(f);
                }
            }

            dto.setFilmography(filmography);

            return dto;

        } catch (Exception e) {
            logger.error("Failed to load actor details: {}", e.getMessage());
            throw new RuntimeException("Failed to load actor details");
        }
    }

}
