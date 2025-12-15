package com.cinematch.backend.service;

import com.cinematch.backend.dto.PreferenceScoreDto;
import com.cinematch.backend.dto.UserPreferencesResponseDto;
import com.cinematch.backend.model.User;
import com.cinematch.backend.model.UserEvent;
import com.cinematch.backend.model.UserEventType;
import com.cinematch.backend.repository.UserEventRepository;
import com.cinematch.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {

    private final UserEventRepository userEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final double FILTER_WEIGHT = 1.0;

    @Transactional
    public UserPreferencesResponseDto computeAndPersist(User user, int topN) {

        List<UserEvent> events =
                userEventRepository.findTop2000ByUser_IdOrderByCreatedAtDesc(user.getId());

        Map<Long, Double> genreScores = new HashMap<>();     // key = genreId
        Map<Long, Double> actorScores = new HashMap<>();     // key = personId
        Map<Long, Double> directorScores = new HashMap<>();  // key = personId

        for (UserEvent e : events) {
            if (e.getPayload() == null || e.getPayload().isBlank()) continue;

            JsonNode node;
            try {
                node = objectMapper.readTree(e.getPayload());
            } catch (Exception ex) {
                continue;
            }

            // Σήμερα έχεις σίγουρα CHOOSE_FILTER. (OPEN_MOVIE μπορεί να εμπλουτιστεί αργότερα)
            if (e.getType() == UserEventType.CHOOSE_FILTER) {

                // genreId
                if (node.hasNonNull("genreId")) {
                    long genreId = node.get("genreId").asLong();
                    addScore(genreScores, genreId, FILTER_WEIGHT);
                }

                // castId (actor)
                if (node.hasNonNull("castId")) {
                    long actorId = node.get("castId").asLong();
                    addScore(actorScores, actorId, FILTER_WEIGHT);
                }

                // crewId (director)
                if (node.hasNonNull("crewId")) {
                    long directorId = node.get("crewId").asLong();
                    addScore(directorScores, directorId, FILTER_WEIGHT);
                }
            }

            // Optional: αν στο μέλλον βάλεις OPEN_MOVIE payload με genreIds/castId/crewId, θα “πιάσει” εδώ.
            if (e.getType() == UserEventType.OPEN_MOVIE) {
                if (node.has("genreIds") && node.get("genreIds").isArray()) {
                    for (JsonNode g : node.get("genreIds")) {
                        if (g != null && g.isNumber()) addScore(genreScores, g.asLong(), 1.0);
                    }
                }
                if (node.hasNonNull("castId")) addScore(actorScores, node.get("castId").asLong(), 1.0);
                if (node.hasNonNull("crewId")) addScore(directorScores, node.get("crewId").asLong(), 1.0);
            }
            // actor click event (payload: personId ή actorId)
            if (e.getType() == UserEventType.OPEN_ACTOR) {
                Long pid = null;

                if (node.hasNonNull("personId")) pid = node.get("personId").asLong();
                else if (node.hasNonNull("actorId")) pid = node.get("actorId").asLong();
                else if (node.hasNonNull("castId")) pid = node.get("castId").asLong();

                if (pid != null) addScore(actorScores, pid, 1.0);
            }
            if (e.getType() == UserEventType.OPEN_DIRECTOR) {
                Long pid = null;

                if (node.hasNonNull("personId")) pid = node.get("personId").asLong();
                else if (node.hasNonNull("directorId")) pid = node.get("directorId").asLong();
                else if (node.hasNonNull("crewId")) pid = node.get("crewId").asLong();

                if (pid != null) addScore(directorScores, pid, 1.0);
            }
        }

        List<PreferenceScoreDto> topGenres = toTopList(genreScores, topN);
        List<PreferenceScoreDto> topActors = toTopList(actorScores, topN);
        List<PreferenceScoreDto> topDirectors = toTopList(directorScores, topN);

        Instant now = Instant.now();

        // Persist στο users table ως JSON string
        try {
            user.setTopGenres(objectMapper.writeValueAsString(topGenres));
            user.setTopActors(objectMapper.writeValueAsString(topActors));
            user.setTopDirectors(objectMapper.writeValueAsString(topDirectors));
            user.setPreferencesLastUpdated(now);
            userRepository.save(user);
        } catch (Exception ex) {
            log.error("Failed to persist preferences for user {}: {}", user.getId(), ex.getMessage());
        }

        return UserPreferencesResponseDto.builder()
                .topGenres(topGenres)
                .topActors(topActors)
                .topDirectors(topDirectors)
                .lastUpdated(now)
                .build();
    }

    private void addScore(Map<Long, Double> map, long key, double delta) {
        map.put(key, map.getOrDefault(key, 0.0) + delta);
    }

    private List<PreferenceScoreDto> toTopList(Map<Long, Double> scores, int topN) {
        return scores.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Double.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return Long.compare(a.getKey(), b.getKey());
                })
                .limit(topN)
                .map(e -> PreferenceScoreDto.builder()
                        .id(e.getKey())
                        .score(e.getValue())
                        .build())
                .toList();
    }
}
