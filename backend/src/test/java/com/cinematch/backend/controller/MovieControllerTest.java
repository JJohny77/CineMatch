package com.cinematch.backend.controller;

import com.cinematch.backend.dto.MovieSearchResponse;
import com.cinematch.backend.service.CurrentUserService;
import com.cinematch.backend.service.TmdbService;
import com.cinematch.backend.service.UserEventService;
import com.cinematch.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MovieController.class)
@AutoConfigureMockMvc(addFilters = false) // δεν τρέχουν τα security filters στο MockMvc
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TmdbService tmdbService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private UserEventService userEventService;

    // ✅ ΑΠΑΡΑΙΤΗΤΟ για να φορτώσει το JwtAuthenticationFilter bean χωρίς να σκάσει
    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void searchMovies_Returns200_AndJson() throws Exception {
        MovieSearchResponse response = new MovieSearchResponse();
        response.setResults(Collections.emptyList());

        when(tmdbService.searchMovies("matrix")).thenReturn(response);

        mockMvc.perform(get("/movies/search").param("query", "matrix"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.results").isArray());
    }
}
