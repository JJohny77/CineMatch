package com.cinematch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContentUploadResponse {

    private String status;     // "success" or "error"
    private String filename;   // on success
    private String type;       // image | video
    private String message;    // success message or error message
    private Double duration;   // null for images
}