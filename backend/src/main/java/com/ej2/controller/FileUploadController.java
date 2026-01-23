package com.ej2.controller;

import com.ej2.util.FileUploadUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:3000")
public class FileUploadController {

    /**
     * Upload a single image
     * POST /api/upload/image
     */
    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = FileUploadUtil.saveFile(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("url", fileUrl);
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Upload multiple images
     * POST /api/upload/images
     */
    @PostMapping("/images")
    public ResponseEntity<?> uploadMultipleImages(@RequestParam("files") MultipartFile[] files) {
        List<Map<String, Object>> uploadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String fileUrl = FileUploadUtil.saveFile(file);

                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("url", fileUrl);
                fileInfo.put("filename", file.getOriginalFilename());
                fileInfo.put("size", file.getSize());

                uploadedFiles.add(fileInfo);
            } catch (IOException e) {
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", errors.isEmpty());
        response.put("files", uploadedFiles);
        if (!errors.isEmpty()) {
            response.put("errors", errors);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete an image
     * DELETE /api/upload/image?url=/uploads/images/xxx.jpg
     */
    @DeleteMapping("/image")
    public ResponseEntity<?> deleteImage(@RequestParam("url") String fileUrl) {
        boolean deleted = FileUploadUtil.deleteFile(fileUrl);

        Map<String, Object> response = new HashMap<>();
        response.put("success", deleted);

        if (deleted) {
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "File not found or could not be deleted");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}
