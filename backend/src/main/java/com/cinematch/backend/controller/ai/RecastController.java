package com.cinematch.backend.controller.ai;

import com.cinematch.backend.dto.RecastResponseDto;
import com.cinematch.backend.service.ai.RecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class RecastController {

    private final RecastService recastService;

    @PostMapping("/recast")
    public RecastResponseDto recast(
            @RequestParam("image") MultipartFile image
    ) {
        return recastService.analyzeFace(image);
    }
}

