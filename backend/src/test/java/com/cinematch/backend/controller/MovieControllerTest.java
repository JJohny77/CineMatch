package com.cinematch.backend.controller;

import com.cinematch.backend.dto.MovieSearchResponse;
import com.cinematch.backend.service.TmdbService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovieControllerTest {

    @Test
    void searchMovies_ReturnsResponse() {
        TmdbService tmdbService = mock(TmdbService.class);

        MovieSearchResponse response = new MovieSearchResponse();
        response.setResults(Collections.emptyList());

        when(tmdbService.searchMovies("matrix")).thenReturn(response);

        MovieController controller = new MovieController(tmdbService);
        MovieSearchResponse result = controller.searchMovies("matrix");

        assertNotNull(result);
        assertEquals(0, result.getResults().size());
    }
}
