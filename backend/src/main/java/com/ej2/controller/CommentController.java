package com.ej2.controller;

import com.ej2.model.Comment;
import com.ej2.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "http://localhost:3000")
public class CommentController {

    @Autowired
    private CommentService commentService;

    // GET /api/comments/post/{postId} - Get all comments for a post
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPostId(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    // GET /api/comments/post/{postId}/top - Get top-level comments only
    @GetMapping("/post/{postId}/top")
    public ResponseEntity<List<Comment>> getTopLevelComments(@PathVariable Long postId) {
        List<Comment> comments = commentService.getTopLevelComments(postId);
        return ResponseEntity.ok(comments);
    }

    // GET /api/comments/{id}/replies - Get replies for a comment
    @GetMapping("/{id}/replies")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable Long id) {
        List<Comment> replies = commentService.getReplies(id);
        return ResponseEntity.ok(replies);
    }

    // GET /api/comments/{id} - Get comment by ID
    @GetMapping("/{id}")
    public ResponseEntity<Comment> getCommentById(@PathVariable Long id) {
        return commentService.getCommentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/comments - Create new comment
    @PostMapping
    public ResponseEntity<Comment> createComment(@RequestBody Comment comment) {
        Comment createdComment = commentService.createComment(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    // PUT /api/comments/{id} - Update comment
    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(@PathVariable Long id, @RequestBody Comment commentDetails) {
        try {
            Comment updatedComment = commentService.updateComment(id, commentDetails);
            return ResponseEntity.ok(updatedComment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/comments/{id} - Soft delete comment
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/comments/{id}/like - Increment like count
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> incrementLikeCount(@PathVariable Long id) {
        commentService.incrementLikeCount(id);
        return ResponseEntity.ok().build();
    }

    // GET /api/comments/post/{postId}/count - Get comment count
    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long postId) {
        Long count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(count);
    }
}
