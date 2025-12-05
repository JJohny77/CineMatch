package com.cinematch.backend.controller.ai;

import com.cinematch.backend.dto.FaceMatchResponse;
import com.cinematch.backend.service.ai.FaceIdentifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ai/face")
@RequiredArgsConstructor
@Slf4j
public class FaceIdentifyController {

    private final FaceIdentifyService faceIdentifyService;

    /**
     * POST /ai/face/identify
     * Body: form-data -> image: <file>
     *
     * Response: List<FaceMatchResponse> (Top-5 ηθοποιοί)
     */
    @PostMapping(
            value = "/identify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<FaceMatchResponse>> identifyFace(
            @RequestPart("image") MultipartFile image
    ) throws IOException {

        log.info("[FaceIdentifyController] New identify request, file name = {}", image.getOriginalFilename());
        List<FaceMatchResponse> matches = faceIdentifyService.identify(image);
        return ResponseEntity.ok(matches);
    }
}
