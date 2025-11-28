package com.cinematch.backend.service.ai;

import com.cinematch.backend.dto.RecastResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RecastService {

    private final RecastEnvService env;

    public RecastResponseDto analyzeFace(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Invalid file");
        }

        if (file.getSize() > 3 * 1024 * 1024) {
            throw new RuntimeException("Max image size is 3MB");
        }

        // =============================
        // 👉 FAKE AI LOGIC FOR NOW
        // =============================
        // Μέχρι να ενσωματώσουμε HuggingFace API
        // το endpoint θα επιστρέφει dummy απάντηση
        return new RecastResponseDto(
                "Tom Holland",
                0.78,
                1136406
        );
    }
}
