package com.cinematch.backend.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void health_ReturnsStatusOk() {
        Map<String, String> response = controller.health();
        assertEquals("OK", response.get("status"));
    }
}
