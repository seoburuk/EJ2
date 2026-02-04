package com.ej2.controller;

import com.ej2.dto.PostDTO;
import com.ej2.model.Post;
import com.ej2.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {

    @Autowired
    private PostService postService;

    // GET /api/posts - Get all posts
    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        List<PostDTO> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    // GET /api/posts/{id} - Get post by ID
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        return postService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/posts - Create new post
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody Post post) {
        Post createdPost = postService.createPost(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    // PUT /api/posts/{id} - 投稿を更新（本人のみ編集可能）
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(
            @PathVariable Long id,
            @RequestBody Post postDetails,
            @RequestParam Long userId) {
        try {
            // 権限検証：本人の投稿かどうか確認
            Post existingPost = postService.getPostEntityById(id);
            if (existingPost == null) {
                return ResponseEntity.notFound().build();
            }
            if (!existingPost.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Post updatedPost = postService.updatePost(id, postDetails);
            return ResponseEntity.ok(updatedPost);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/posts/{id} - 投稿を削除（本人のみ削除可能）
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @RequestParam Long userId) {
        try {
            // 権限検証：本人の投稿かどうか確認
            Post existingPost = postService.getPostEntityById(id);
            if (existingPost == null) {
                return ResponseEntity.notFound().build();
            }
            if (!existingPost.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            postService.deletePost(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/posts/search?keyword=xxx - Search posts by title
    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPosts(@RequestParam String keyword) {
        List<Post> posts = postService.searchPostsByTitle(keyword);
        return ResponseEntity.ok(posts);
    }

    // GET /api/posts/board/{boardId} - Get posts by board ID
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<PostDTO>> getPostsByBoardId(@PathVariable Long boardId) {
        List<PostDTO> posts = postService.getPostsByBoardId(boardId);
        return ResponseEntity.ok(posts);
    }

    // POST /api/posts/{id}/view - Increment view count with IP tracking
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementViewCount(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {

        // Get client IP address
        String ipAddress = getClientIpAddress(request);

        postService.incrementViewCount(id, userId, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Get the real client IP address from the request
     * Handles proxy headers (X-Forwarded-For, X-Real-IP)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_FORWARDED");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    // POST /api/posts/{id}/like - Increment like count with IP tracking
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> incrementLikeCount(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {

        // Get client IP address
        String ipAddress = getClientIpAddress(request);

        postService.incrementLikeCount(id, userId, ipAddress);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<Void> incrementDislikeCount(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {

        // Get client IP address
        String ipAddress = getClientIpAddress(request);

        postService.incrementDislikeCount(id, userId, ipAddress);
        return ResponseEntity.ok().build();
    }
}
