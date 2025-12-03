package com.cinematch.backend.service.upload;

import com.cinematch.backend.dto.ContentUploadResponse;
import com.cinematch.backend.model.User;
import com.cinematch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentUploadService {

    private static final String BASE_UPLOAD_DIR = "C:\\CineMatch\\backend\\uploads\\";

    private final UserRepository userRepository;

    // =========================================
    // UPLOAD (USER-SCOPED)
    // =========================================
    public ContentUploadResponse handleUpload(MultipartFile file) {

        // Get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? (String) auth.getPrincipal() : null;

        if (email == null) {
            return new ContentUploadResponse("User not authenticated", null, null, null, null);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new ContentUploadResponse("User not found", null, null, null, null);
        }

        Long userId = user.getId();

        // Validate file
        if (file == null || file.isEmpty()) {
            return new ContentUploadResponse("error", null, null, null, null);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return new ContentUploadResponse("error", null, null, null, null);
        }

        String extension = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();

        boolean isImage = extension.equals("jpg") ||
                extension.equals("jpeg") ||
                extension.equals("png");

        boolean isVideo = extension.equals("mp4");

        if (extension.equals("mov")) {
            return new ContentUploadResponse("MOV is not supported. Please upload MP4.", null, null, null, null);
        }

        if (!isImage && !isVideo) {
            return new ContentUploadResponse("File type not allowed: " + extension, null, null, null, null);
        }

        // Safe filename
        String safeFilename = java.util.UUID.randomUUID() + "_" + originalName;

        // User-specific folder
        String typeFolder = isImage ? "images" : "videos";
        Path userFolder = Paths.get(BASE_UPLOAD_DIR, String.valueOf(userId), typeFolder);

        try {
            Files.createDirectories(userFolder);
        } catch (IOException e) {
            return new ContentUploadResponse("Failed to create user directory", null, null, null, null);
        }

        Path fullPath = userFolder.resolve(safeFilename);

        // Save file
        try {
            file.transferTo(fullPath.toFile());
        } catch (IOException e) {
            return new ContentUploadResponse("Failed to save file: " + e.getMessage(), null, null, null, null);
        }

        Double duration = null;

        // Video checks
        if (isVideo) {
            duration = getVideoDurationInSeconds(fullPath);

            if (duration < 0) {
                deleteQuietly(fullPath);
                return new ContentUploadResponse("Could not read video duration.", null, null, null, null);
            }

            if (duration > 60) {
                deleteQuietly(fullPath);
                return new ContentUploadResponse("Video too long. Max allowed = 60 seconds.", null, null, null, null);
            }
        }

        // Save metadata
        saveMetadataJson(userId, safeFilename, typeFolder, duration);

        // Return response
        String fileUrl = "/uploads/" + userId + "/" + typeFolder + "/" + safeFilename;

        return new ContentUploadResponse(
                "success",
                safeFilename,
                isImage ? "image" : "video",
                fileUrl,
                duration
        );
    }

    // =========================================
    // LIST USER UPLOADS (USER-SCOPED)
    // =========================================
    public List<Map<String, Object>> listUserUploads() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? (String) auth.getPrincipal() : null;

        if (email == null) return List.of();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return List.of();

        Long userId = user.getId();

        Path imagesDir = Paths.get(BASE_UPLOAD_DIR, String.valueOf(userId), "images");
        Path videosDir = Paths.get(BASE_UPLOAD_DIR, String.valueOf(userId), "videos");

        List<Map<String, Object>> result = new ArrayList<>();

        try {
            if (Files.exists(imagesDir)) {
                Files.list(imagesDir).forEach(path -> {
                    result.add(Map.of(
                            "filename", path.getFileName().toString(),
                            "type", "image",
                            "url", "/uploads/" + userId + "/images/" + path.getFileName().toString()
                    ));
                });
            }

            if (Files.exists(videosDir)) {
                Files.list(videosDir).forEach(path -> {
                    result.add(Map.of(
                            "filename", path.getFileName().toString(),
                            "type", "video",
                            "url", "/uploads/" + userId + "/videos/" + path.getFileName().toString()
                    ));
                });
            }

        } catch (Exception ignored) { }

        return result;
    }

    // =========================================
    // DELETE (USER-SCOPED)
    // =========================================
    public boolean deleteContent(String filename) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? (String) auth.getPrincipal() : null;

        if (email == null) return false;

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        Long userId = user.getId();

        // Paths
        Path imagePath = Paths.get(BASE_UPLOAD_DIR, String.valueOf(userId), "images", filename);
        Path videoPath = Paths.get(BASE_UPLOAD_DIR, String.valueOf(userId), "videos", filename);

        try {
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                return true;
            }

            if (Files.exists(videoPath)) {
                Files.delete(videoPath);
                return true;
            }

        } catch (Exception ignored) {}

        return false;
    }

    // =========================================
    // HELPERS
    // =========================================
    private void deleteQuietly(Path p) {
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
    }

    private void saveMetadataJson(Long userId, String filename, String type, Double duration) {
        try {
            Path jsonPath = Paths.get(BASE_UPLOAD_DIR, String.valueOf(userId), "metadata.json");

            String entry = String.format(
                    "{\"filename\":\"%s\",\"type\":\"%s\",\"duration\":%s},",
                    filename, type, duration == null ? "null" : duration
            );

            FileWriter writer = new FileWriter(jsonPath.toFile(), true);
            writer.write(entry + "\n");
            writer.close();

        } catch (Exception ignored) {}
    }

    private double getVideoDurationInSeconds(Path path) {
        try {
            FFprobe ffprobe = new FFprobe("C:\\ffmpeg\\bin\\ffprobe.exe");
            FFmpegProbeResult probeResult = ffprobe.probe(path.toString());
            return probeResult.getFormat().duration;
        } catch (Exception e) {
            return -1;
        }
    }
}
