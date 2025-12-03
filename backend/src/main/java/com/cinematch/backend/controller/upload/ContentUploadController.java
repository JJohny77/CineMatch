package com.cinematch.backend.controller.upload;

import com.cinematch.backend.dto.ContentUploadResponse;
import com.cinematch.backend.model.User;
import com.cinematch.backend.repository.UserRepository;
import com.cinematch.backend.service.upload.ContentUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/content")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ContentUploadController {

    private final ContentUploadService contentUploadService;
    private final UserRepository userRepository;

    // =========================================
    // helper: παίρνουμε userId από Authentication
    // =========================================
    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    // =========================================
    // UPLOAD ENDPOINT (per user)
    // =========================================
    @PostMapping("/upload")
    public ResponseEntity<ContentUploadResponse> uploadContent(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        ContentUploadResponse res = contentUploadService.handleUpload(file, userId);

        if ("success".equals(res.getStatus())) {
            return ResponseEntity.ok(res);
        }

        return ResponseEntity.badRequest().body(res);
    }

    @GetMapping("/test")
    public String test() {
        return "Content Upload API is working!";
    }

    // =========================================
    // LIST CONTENT (current user only)
    // =========================================
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, String>>> listContent(Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        List<Map<String, String>> result = contentUploadService.listUserContent(userId);
        return ResponseEntity.ok(result);
    }

    // =========================================
    // DELETE CONTENT (current user only)
    // =========================================
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteContent(
            @PathVariable String filename,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        boolean deleted = contentUploadService.deleteContent(userId, filename);

        if (deleted) {
            return ResponseEntity.ok("Deleted");
        } else {
            return ResponseEntity.status(404).body("File not found");
        }
    }

    // =========================================
    // DOWNLOAD CONTENT (current user only)
    // =========================================
    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadFile(
            @PathVariable String filename,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        File target = contentUploadService.findUserFile(userId, filename);

        if (target == null) {
            return ResponseEntity.status(404).body("File not found");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(new FileSystemResource(target));
    }
}
