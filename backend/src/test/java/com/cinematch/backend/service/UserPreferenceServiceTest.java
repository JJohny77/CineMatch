package com.cinematch.backend.service;

import com.cinematch.backend.dto.UserPreferencesResponseDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.model.UserEvent;
import com.cinematch.backend.model.UserEventType;
import com.cinematch.backend.repository.UserEventRepository;
import com.cinematch.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserEventRepository userEventRepository;

    @Mock
    private UserRepository userRepository;

    // ✔️ Spy για να γίνει σωστό inject
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void computeAndPersist_withChooseFilterGenre_shouldReturnTopGenre() {

        UserEvent event = new UserEvent();
        event.setType(UserEventType.CHOOSE_FILTER);
        event.setPayload("{\"genreId\":28}");
        event.setCreatedAt(Instant.now());

        when(userEventRepository
                .findTop2000ByUser_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(event));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferencesResponseDto response =
                userPreferenceService.computeAndPersist(user, 5);

        assertEquals(1, response.getTopGenres().size());
        assertEquals(28L, response.getTopGenres().get(0).getId());
        assertEquals(1.0, response.getTopGenres().get(0).getScore());
    }

    @Test
    void computeAndPersist_shouldSortByScoreDesc_thenById() {

        UserEvent e1 = new UserEvent();
        e1.setType(UserEventType.CHOOSE_FILTER);
        e1.setPayload("{\"genreId\":10}");

        UserEvent e2 = new UserEvent();
        e2.setType(UserEventType.CHOOSE_FILTER);
        e2.setPayload("{\"genreId\":5}");

        when(userEventRepository
                .findTop2000ByUser_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(e1, e2));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferencesResponseDto response =
                userPreferenceService.computeAndPersist(user, 5);

        assertEquals(2, response.getTopGenres().size());
        assertEquals(5L, response.getTopGenres().get(0).getId());
        assertEquals(10L, response.getTopGenres().get(1).getId());
    }

    @Test
    void computeAndPersist_withTopNLimit_shouldRespectLimit() {

        UserEvent e1 = new UserEvent();
        e1.setType(UserEventType.CHOOSE_FILTER);
        e1.setPayload("{\"genreId\":1}");

        UserEvent e2 = new UserEvent();
        e2.setType(UserEventType.CHOOSE_FILTER);
        e2.setPayload("{\"genreId\":2}");

        UserEvent e3 = new UserEvent();
        e3.setType(UserEventType.CHOOSE_FILTER);
        e3.setPayload("{\"genreId\":3}");

        when(userEventRepository
                .findTop2000ByUser_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(e1, e2, e3));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferencesResponseDto response =
                userPreferenceService.computeAndPersist(user, 2);

        assertEquals(2, response.getTopGenres().size());
    }

    @Test
    void computeAndPersist_withEmptyEvents_shouldReturnEmptyLists() {

        when(userEventRepository
                .findTop2000ByUser_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        UserPreferencesResponseDto response =
                userPreferenceService.computeAndPersist(user, 5);

        assertTrue(response.getTopGenres().isEmpty());
        assertTrue(response.getTopActors().isEmpty());
        assertTrue(response.getTopDirectors().isEmpty());
    }
}
