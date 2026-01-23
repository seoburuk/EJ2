package com.ej2.repository;

import com.ej2.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // Basic query methods - Spring Data JPA will implement these automatically
    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByTitleContaining(String keyword);

    List<Post> findByBoardIdOrderByCreatedAtDesc(Long boardId);
}
