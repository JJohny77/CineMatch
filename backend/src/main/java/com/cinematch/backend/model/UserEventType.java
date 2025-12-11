package com.cinematch.backend.model;

public enum UserEventType {
    // Movies / People
    OPEN_MOVIE,
    OPEN_ACTOR,
    OPEN_DIRECTOR,

    // Explicit feedback
    LIKE_MOVIE,

    // Quiz
    QUIZ_CORRECT,
    QUIZ_WRONG,

    // UI actions
    CHOOSE_FILTER,

    // Προϋπάρχοντα / compatibility (αν θες να τα κρατήσουμε)
    SEARCH_MOVIE,
    SEARCH_PERSON
}
