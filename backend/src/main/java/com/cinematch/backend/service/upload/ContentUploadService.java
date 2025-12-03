package com.cinematch.backend.service.upload;

import com.cinematch.backend.dto.ContentUploadResponse;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class ContentUploadService {

    private static final String BASE_UPLOAD_DIR = "C:\\CineMatch\\backend\\uploads\\"; // base
    private static final String IMAGES_SUBDIR = "images\\";
    private static final String VIDEOS_SUBDIR = "videos\\";

    // =========================================
    // UPLOAD ΓΙΑ ΣΥΓΚΕΚΡΙΜΕΝΟ USER
    // =========================================
    public ContentUploadResponse handleUpload(MultipartFile file, Long userId) {

        if (file == null || file.isEmpty()) {
            return new ContentUploadResponse("error", null, null, "Empty file", null);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return new ContentUploadResponse("error", null, null, "Missing filename", null);
        }

        String extension = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();

        boolean isImage = extension.equals("jpg") ||
                extension.equals("jpeg") ||
                extension.equals("png");

        boolean isVideo = extension.equals("mp4");

        if (extension.equals("mov")) {
            return new ContentUploadResponse("error", null, null, "MOV is not supported. Please upload MP4.", null);
        }

        if (!isImage && !isVideo) {
            return new ContentUploadResponse("error", null, null, "File type not allowed: " + extension, null);
        }

        String safeFilename = java.util.UUID.randomUUID() + "_" + originalName;

        // user-specific directories: /uploads/<userId>/images|videos/
        String userBaseDir = BASE_UPLOAD_DIR + userId + "\\";
        String targetDir = userBaseDir + (isImage ? IMAGES_SUBDIR : VIDEOS_SUBDIR);

        Path uploadPath = Paths.get(targetDir);
        Path fullPath = uploadPath.resolve(safeFilename);

        try {
            Files.createDirectories(uploadPath);
            file.transferTo(fullPath.toFile());
        } catch (IOException e) {
            return new ContentUploadResponse("error", null, null, "Failed to save file: " + e.getMessage(), null);
        }

        Double duration = null;

        if (isVideo) {
            duration = getVideoDurationInSeconds(fullPath);

            if (duration < 0) {
                deleteQuietly(fullPath);
                return new ContentUploadResponse("error", null, null, "Could not read video duration.", null);
            }

            if (duration > 60) {
                deleteQuietly(fullPath);
                return new ContentUploadResponse("error", null, null, "Video too long. Max allowed = 60 seconds.", null);
            }
        }

        String url = "/uploads/" + userId + "/" + (isImage ? "images/" : "videos/") + safeFilename;

        return new ContentUploadResponse(
                "success",
                safeFilename,
                isImage ? "image" : "video",
                url,
                duration
        );
    }

    // =========================================
    // LIST για συγκεκριμένο user
    // =========================================
    public List<Map<String, String>> listUserContent(Long userId) {

        List<Map<String, String>> result = new ArrayList<>();

        String userBaseDir = BASE_UPLOAD_DIR + userId + "\\";

        // IMAGE DIRECTORY
        File imageDir = new File(userBaseDir + IMAGES_SUBDIR);
        if (imageDir.exists()) {
            for (File f : Objects.requireNonNull(imageDir.listFiles())) {
                if (!f.isDirectory()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("filename", f.getName());
                    item.put("type", "image");
                    item.put("url", "/uploads/" + userId + "/images/" + f.getName());
                    result.add(item);
                }
            }
        }

        // VIDEO DIRECTORY
        File videoDir = new File(userBaseDir + VIDEOS_SUBDIR);
        if (videoDir.exists()) {
            for (File f : Objects.requireNonNull(videoDir.listFiles())) {
                if (!f.isDirectory()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("filename", f.getName());
                    item.put("type", "video");
                    item.put("url", "/uploads/" + userId + "/videos/" + f.getName());
                    result.add(item);
                }
            }
        }

        return result;
    }

    // =========================================
    // DELETE συγκεκριμένου user
    // =========================================
    public boolean deleteContent(Long userId, String filename) {

        String userBaseDir = BASE_UPLOAD_DIR + userId + "\\";

        File img = new File(userBaseDir + IMAGES_SUBDIR + filename);
        if (img.exists()) {
            return img.delete();
        }

        File vid = new File(userBaseDir + VIDEOS_SUBDIR + filename);
        if (vid.exists()) {
            return vid.delete();
        }

        return false;
    }

    // =========================================
    // FIND FILE για download
    // =========================================
    public File findUserFile(Long userId, String filename) {
        String userBaseDir = BASE_UPLOAD_DIR + userId + "\\";

        File img = new File(userBaseDir + IMAGES_SUBDIR + filename);
        if (img.exists()) return img;

        File vid = new File(userBaseDir + VIDEOS_SUBDIR + filename);
        if (vid.exists()) return vid;

        return null;
    }

    // =========================================

    private void deleteQuietly(Path p) {
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
    }

    private double getVideoDurationInSeconds(Path path) {
        try {
            FFprobe ffprobe = new FFprobe("C:\\ffmpeg\\bin\\ffprobe.exe");

            FFmpegProbeResult probeResult = ffprobe.probe(path.toString());
            return probeResult.getFormat().duration;
        } catch (Exception e) {
            System.out.println("FFmpeg failed: " + e.getMessage());
            return -1;
        }
    }
}
