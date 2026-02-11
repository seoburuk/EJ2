package com.ej2.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Service for AWS S3 operations.
 * Handles file upload, deletion, and URL generation.
 */
@Service
public class S3Service {

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Upload file to S3 with organized folder structure.
     *
     * @param file   MultipartFile to upload
     * @param postId Post ID for folder organization (posts/{postId}/{uuid}.ext)
     * @return S3 object key
     * @throws IOException If file upload fails
     */
    public String uploadFile(MultipartFile file, Long postId) throws IOException {
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Create S3 key with folder structure: posts/{postId}/{uuid}.ext
        String s3Key = String.format("posts/%d/%s", postId, uniqueFilename);

        // Prepare metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        // Upload to S3
        InputStream inputStream = file.getInputStream();
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    s3Key,
                    inputStream,
                    metadata
            );
            amazonS3.putObject(putObjectRequest);
        } finally {
            inputStream.close();
        }

        return s3Key;
    }

    /**
     * Get public URL for S3 object.
     *
     * @param s3Key S3 object key
     * @return Full S3 URL
     */
    public String getFileUrl(String s3Key) {
        return amazonS3.getUrl(bucketName, s3Key).toString();
    }

    /**
     * Delete file from S3.
     *
     * @param s3Key S3 object key to delete
     */
    public void deleteFile(String s3Key) {
        amazonS3.deleteObject(bucketName, s3Key);
    }

    /**
     * Delete all files in a folder (for post deletion).
     * Deletes all objects with prefix: posts/{postId}/
     *
     * @param postId Post ID whose images should be deleted
     */
    public void deleteFolder(Long postId) {
        String folderPrefix = String.format("posts/%d/", postId);

        ListObjectsV2Request listRequest = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(folderPrefix);

        ListObjectsV2Result result = amazonS3.listObjectsV2(listRequest);

        for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
            amazonS3.deleteObject(bucketName, objectSummary.getKey());
        }
    }

    /**
     * Extract file extension from filename.
     *
     * @param filename Original filename
     * @return File extension including dot (e.g., ".jpg"), or empty string if no extension
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}
