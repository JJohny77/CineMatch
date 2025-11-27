package com.cinematch.backend.controller.upload;

import com.cinematch.backend.dto.ContentUploadResponse;
import com.cinematch.backend.service.upload.ContentUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentUploadController {

    private final ContentUploadService contentUploadService;

    @PostMapping("/upload")
    public ContentUploadResponse uploadContent(@RequestParam("file") MultipartFile file) {
        return contentUploadService.handleUpload(file);
    }

    @GetMapping("/test")
    public String test() {
        return "Content Upload API is working!";
    }
}
