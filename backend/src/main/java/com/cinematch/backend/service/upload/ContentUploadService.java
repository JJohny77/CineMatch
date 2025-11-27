package com.cinematch.backend.service.upload;

import com.cinematch.backend.dto.ContentUploadResponse;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ContentUploadService {

    private static final String BASE_UPLOAD_DIR = "C:\\CineMatch\\backend\\uploads\\";
    private static final String IMAGE_DIR = BASE_UPLOAD_DIR + "images\\";
    private static final String VIDEO_DIR = BASE_UPLOAD_DIR + "videos\\";

    public ContentUploadResponse handleUpload(MultipartFile file) {

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

        // safe random filename
        String randomId = UUID.randomUUID().toString();
        String safeFilename = randomId + "_" + originalName;

        // determine save location
        String targetDir = isImage ? IMAGE_DIR : VIDEO_DIR;
        Path uploadPath = Paths.get(targetDir);
        Path fullPath = uploadPath.resolve(safeFilename);

        try {
            Files.createDirectories(uploadPath);
            file.transferTo(fullPath.toFile());
        } catch (IOException e) {
            return new ContentUploadResponse("Failed to save file: " + e.getMessage(), null, null, null, null);
        }

        Double duration = null;

        // If video: validate duration
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

        // success response
        return new ContentUploadResponse(
                "success",
                safeFilename,
                isImage ? "image" : "video",
                "/uploads/" + (isImage ? "images/" : "videos/") + safeFilename,
                duration
        );
    }

    private void deleteQuietly(Path p) {
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
    }

    // ===========================
    // FFmpeg duration reader
    // ===========================
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
