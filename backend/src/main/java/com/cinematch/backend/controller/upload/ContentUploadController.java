package com.cinematch.backend.controller.upload;

import com.cinematch.backend.dto.ContentUploadResponse;
import com.cinematch.backend.service.upload.ContentUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/content")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ContentUploadController {

    private final ContentUploadService contentUploadService;

    // =========================================
    // UPLOAD (USER-SCOPED)
    // =========================================
    @PostMapping("/upload")
    public ResponseEntity<ContentUploadResponse> uploadContent(
            @RequestParam("file") MultipartFile file) {

        ContentUploadResponse res = contentUploadService.handleUpload(file);

        if ("success".equals(res.getStatus())) {
            return ResponseEntity.ok(res);
        }

        return ResponseEntity.badRequest().body(res);
    }

    // =========================================
    // PERSONAL GALLERY (ONLY MY FILES)
    // =========================================
    @GetMapping("/my-uploads")
    public ResponseEntity<List<Map<String, Object>>> getMyUploads() {
        List<Map<String, Object>> result = contentUploadService.listUserUploads();
        return ResponseEntity.ok(result);
    }

    // =========================================
    // DELETE (USER-SCOPED)
    // =========================================
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteContent(@PathVariable String filename) {

        boolean deleted = contentUploadService.deleteContent(filename);

        if (deleted) {
            return ResponseEntity.ok("Deleted");
        } else {
            return ResponseEntity.status(404).body("File not found");
        }
    }

    // =========================================
    // DOWNLOAD (RECURSIVE SEARCH)
    // =========================================
    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String filename) {

        File root = new File("C:\\CineMatch\\backend\\uploads");
        File target = findRecursively(root, filename);

        if (target == null) {
            return ResponseEntity.status(404).body("File not found");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(new FileSystemResource(target));
    }

    // Recursive helper: finds files inside any user folder
    private File findRecursively(File dir, String filename) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                File found = findRecursively(f, filename);
                if (found != null) return found;
            } else {
                if (f.getName().equals(filename)) {
                    return f;
                }
            }
        }

        return null;
    }
}