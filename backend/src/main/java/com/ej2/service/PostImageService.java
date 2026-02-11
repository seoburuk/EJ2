package com.ej2.service;

import com.ej2.model.PostImage;
import com.ej2.repository.PostImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing post images.
 * Handles image upload to S3 and database persistence.
 */
@Service
@Transactional
public class PostImageService {

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private S3Service s3Service;

    /**
     * Upload image to S3 and save metadata to database.
     *
     * @param file         MultipartFile to upload
     * @param postId       ID of the post this image belongs to
     * @param displayOrder Display order (0-4) for the image
     * @return Saved PostImage entity
     * @throws IOException If S3 upload fails
     */
    public PostImage uploadImage(MultipartFile file, Long postId, Integer displayOrder) throws IOException {
        // Upload to S3
        String s3Key = s3Service.uploadFile(file, postId);
        String s3Url = s3Service.getFileUrl(s3Key);

        // Create PostImage entity
        PostImage postImage = new PostImage();
        postImage.setPostId(postId);
        postImage.setS3Key(s3Key);
        postImage.setS3Url(s3Url);
        postImage.setOriginalFilename(file.getOriginalFilename());
        postImage.setFileSize(file.getSize());
        postImage.setContentType(file.getContentType());
        postImage.setDisplayOrder(displayOrder);
        postImage.setUploadedAt(LocalDateTime.now());

        // Save to database
        return postImageRepository.save(postImage);
    }

    /**
     * Get all images for a specific post, ordered by display order.
     *
     * @param postId ID of the post
     * @return List of PostImage entities
     */
    public List<PostImage> getImagesByPostId(Long postId) {
        return postImageRepository.findByPostIdOrderByDisplayOrder(postId);
    }

    /**
     * Delete all images for a post (cascade delete).
     * Deletes from both S3 and database.
     *
     * @param postId ID of the post
     */
    public void deleteImagesByPostId(Long postId) {
        List<PostImage> images = postImageRepository.findByPostIdOrderByDisplayOrder(postId);

        // Delete from S3
        for (PostImage image : images) {
            s3Service.deleteFile(image.getS3Key());
        }

        // Delete from database
        postImageRepository.deleteByPostId(postId);
    }

    /**
     * Delete a single image by ID.
     * Deletes from both S3 and database.
     *
     * @param imageId ID of the image to delete
     */
    public void deleteImage(Long imageId) {
        PostImage image = postImageRepository.findById(imageId).orElse(null);
        if (image != null) {
            // Delete from S3
            s3Service.deleteFile(image.getS3Key());

            // Delete from database
            postImageRepository.delete(image);
        }
    }
}
