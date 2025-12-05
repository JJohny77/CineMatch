package com.cinematch.backend.service.ai;

import ai.onnxruntime.*;
import com.cinematch.backend.dto.RecastResponseDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

@Slf4j
@Service
public class RecastService {

    private final ActorEmbeddingService actorEmbeddingService;

    private OrtEnvironment env;
    private OrtSession session;
    private String inputName;

    private static final int IMG_SIZE = 112;
    private static final String MODEL_PATH = "models/arcface.onnx";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ======== SPRING-VALID CONSTRUCTOR ========
    public RecastService(ActorEmbeddingService actorEmbeddingService) {
        this.actorEmbeddingService = actorEmbeddingService;
    }

    // ======== ONNX LOAD AFTER BEAN INITIALIZATION ========
    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();

            InputStream is = getClass()
                    .getClassLoader()
                    .getResourceAsStream(MODEL_PATH);

            if (is == null) {
                throw new IllegalStateException("ONNX model not found: " + MODEL_PATH);
            }

            byte[] modelBytes = is.readAllBytes();
            is.close();

            session = env.createSession(modelBytes, new OrtSession.SessionOptions());
            inputName = session.getInputNames().iterator().next();

            log.info("[RecastService] ArcFace model loaded successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to load ArcFace model", e);
        }
    }

    // ======== MAIN ENTRY POINT ========
    public RecastResponseDto analyzeFace(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Invalid file");
            }

            byte[] bytes = file.getBytes();
            double[] embedding = extractEmbedding(bytes);

            if (!actorEmbeddingService.hasEmbeddings()) {
                return new RecastResponseDto("AI-Embedding-Ready", 1.0, 0L, null);
            }

            var matchOpt = actorEmbeddingService.findBestMatch(embedding);

            return matchOpt.map(match ->
                    new RecastResponseDto(
                            match.getName(),
                            match.getSimilarity(),
                            match.getActorId(),
                            match.getImageUrl()
                    )
            ).orElse(new RecastResponseDto("No match", 0, null, null));

        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze face", e);
        }
    }

    // ======== FOR TMDB BATCH SERVICE ========
    public double[] extractEmbedding(byte[] imageBytes) {
        try {
            float[] raw = embedRaw(imageBytes);

            // normalize
            double norm = 0;
            for (float f : raw) norm += f * f;
            norm = Math.sqrt(norm);

            double[] out = new double[raw.length];
            for (int i = 0; i < raw.length; i++)
                out[i] = raw[i] / norm;

            return out;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Κατέβασμα εικόνας από URL (για TMDB batch)
    public byte[] downloadImage(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .build();

            HttpResponse<byte[]> resp =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (resp.statusCode() != 200) {
                log.warn("[RecastService] Failed to download image {} (status {})",
                        imageUrl, resp.statusCode());
                return null;
            }

            return resp.body();

        } catch (Exception e) {
            log.error("[RecastService] Error downloading image {}", imageUrl, e);
            return null;
        }
    }

    // ======== INTERNAL ONNX EXECUTION ========
    private float[] embedRaw(byte[] imageBytes) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null) throw new RuntimeException("Invalid image file");

        BufferedImage resized = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(img, 0, 0, IMG_SIZE, IMG_SIZE, null);
        g.dispose();

        float[][][][] input = new float[1][IMG_SIZE][IMG_SIZE][3];
        for (int y = 0; y < IMG_SIZE; y++) {
            for (int x = 0; x < IMG_SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                input[0][y][x][0] = (((rgb >> 16) & 0xFF) - 127.5f) / 128f; // R
                input[0][y][x][1] = (((rgb >> 8) & 0xFF) - 127.5f) / 128f;  // G
                input[0][y][x][2] = (((rgb) & 0xFF) - 127.5f) / 128f;       // B
            }
        }

        try (OnnxTensor tensor = OnnxTensor.createTensor(env, input)) {
            OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor));
            float[][] output = (float[][]) result.get(0).getValue();
            return output[0];
        }
    }
}
