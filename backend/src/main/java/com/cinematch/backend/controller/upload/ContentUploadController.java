package com.cinematch.backend.controller.upload;

import com.cinematch.backend.dto.ContentUploadResponse;
import com.cinematch.backend.service.upload.ContentUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
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

    // =========================================
    // UPLOAD ENDPOINT
    // =========================================
    @PostMapping("/upload")
    public ResponseEntity<ContentUploadResponse> uploadContent(
            @RequestParam("file") MultipartFile file) {

        ContentUploadResponse res = contentUploadService.handleUpload(file);

        if (res.getStatus().equals("success")) {
            return ResponseEntity.ok(res);
        }

        return ResponseEntity.badRequest().body(res);
    }

    @GetMapping("/test")
    public String test() {
        return "Content Upload API is working!";
    }

    // =========================================
    // LIST CONTENT
    // =========================================
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, String>>> listContent() {

        List<Map<String, String>> result = new ArrayList<>();

        // IMAGE DIRECTORY
        File imageDir = new File("C:\\CineMatch\\backend\\uploads\\images\\");
        if (imageDir.exists()) {
            for (File f : Objects.requireNonNull(imageDir.listFiles())) {
                if (!f.isDirectory()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("filename", f.getName());
                    item.put("type", "image");
                    item.put("url", "/uploads/images/" + f.getName());
                    result.add(item);
                }
            }
        }

        // VIDEO DIRECTORY
        File videoDir = new File("C:\\CineMatch\\backend\\uploads\\videos\\");
        if (videoDir.exists()) {
            for (File f : Objects.requireNonNull(videoDir.listFiles())) {
                if (!f.isDirectory()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("filename", f.getName());
                    item.put("type", "video");
                    item.put("url", "/uploads/videos/" + f.getName());
                    result.add(item);
                }
            }
        }

        return ResponseEntity.ok(result);
    }

    // =========================================
    // DELETE CONTENT
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
    // DOWNLOAD CONTENT
    // =========================================
    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String filename) {

        String imagePath = "C:\\CineMatch\\backend\\uploads\\images\\" + filename;
        String videoPath = "C:\\CineMatch\\backend\\uploads\\videos\\" + filename;

        File imageFile = new File(imagePath);
        File videoFile = new File(videoPath);

        File target = imageFile.exists() ? imageFile :
                videoFile.exists() ? videoFile : null;

        if (target == null) {
            return ResponseEntity.status(404).body("File not found");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(new FileSystemResource(target));
    }
}
