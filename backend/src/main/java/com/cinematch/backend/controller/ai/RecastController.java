package com.cinematch.backend.controller.ai;

import com.cinematch.backend.dto.RecastResponseDto;
import com.cinematch.backend.service.ai.RecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class RecastController {

    private final RecastService recastService;

    @PostMapping(
            value = "/recast",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public RecastResponseDto recast(
            @RequestPart("image") MultipartFile image
    ) {
        return recastService.analyzeFace(image);
    }
}
