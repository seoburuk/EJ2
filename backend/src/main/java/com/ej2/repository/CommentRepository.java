package com.ej2.repository;

import com.ej2.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);

    List<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    List<Comment> findByParentIdOrderByCreatedAtDesc(Long parentId);

    Long countByPostId(Long postId);
}
