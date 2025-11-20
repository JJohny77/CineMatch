package com.cinematch.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Skip heavy context load test for Sprint 1 – DB & env dependent")
class CineMatchApplicationTests {

    @Test
    void contextLoads() {
        // empty on purpose
    }
}
