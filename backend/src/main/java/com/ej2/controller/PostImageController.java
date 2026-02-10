package com.ej2.controller;

import com.ej2.model.PostImage;
import com.ej2.service.PostImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for post image operations.
 * Handles image upload, retrieval, and deletion for bulletin board posts.
 */
@RestController
@RequestMapping("/api/posts")
public class PostImageController {

    @Autowired
    private PostImageService postImageService;

    /**
     * Upload multiple images for a post.
     * Maximum 5 images per post.
     *
     * @param postId Post ID
     * @param images Array of image files
     * @return List of uploaded PostImage entities
     */
    @PostMapping("/{postId}/images")
    public ResponseEntity<?> uploadImages(
            @PathVariable Long postId,
            @RequestParam("images") MultipartFile[] images) {

        try {
            // Validate image count
            if (images.length > 5) {
                return ResponseEntity.badRequest()
                        .body("Maximum 5 images allowed per post");
            }

            List<PostImage> uploadedImages = new ArrayList<PostImage>();

            // Upload each image with display order
            for (int i = 0; i < images.length; i++) {
                MultipartFile file = images[i];

                // Validate file is not empty
                if (file.isEmpty()) {
                    continue;
                }

                // Upload image
                PostImage postImage = postImageService.uploadImage(file, postId, i);
                uploadedImages.add(postImage);
            }

            return ResponseEntity.ok(uploadedImages);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload images: " + e.getMessage());
        }
    }

    /**
     * Get all images for a specific post.
     *
     * @param postId Post ID
     * @return List of PostImage entities ordered by display order
     */
    @GetMapping("/{postId}/images")
    public ResponseEntity<List<PostImage>> getImages(@PathVariable Long postId) {
        List<PostImage> images = postImageService.getImagesByPostId(postId);
        return ResponseEntity.ok(images);
    }

    /**
     * Delete a single image.
     *
     * @param postId  Post ID (for path consistency)
     * @param imageId Image ID to delete
     * @return Success message
     */
    @DeleteMapping("/{postId}/images/{imageId}")
    public ResponseEntity<String> deleteImage(
            @PathVariable Long postId,
            @PathVariable Long imageId) {

        try {
            postImageService.deleteImage(imageId);
            return ResponseEntity.ok("Image deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete image: " + e.getMessage());
        }
    }
}
