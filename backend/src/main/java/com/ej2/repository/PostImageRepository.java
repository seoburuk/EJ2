package com.ej2.repository;

import com.ej2.model.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for PostImage entity.
 * Provides CRUD operations and custom query methods for post images.
 */
@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    /**
     * Find all images for a specific post, ordered by display order.
     *
     * @param postId The ID of the post
     * @return List of PostImage entities ordered by displayOrder
     */
    List<PostImage> findByPostIdOrderByDisplayOrder(Long postId);

    /**
     * Delete all images associated with a specific post.
     * Used for cascade deletion when a post is deleted.
     *
     * @param postId The ID of the post
     */
    void deleteByPostId(Long postId);
}
