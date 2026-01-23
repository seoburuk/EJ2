package com.ej2.service;

import com.ej2.model.Board;
import com.ej2.model.Post;
import com.ej2.model.PostViewLog;
import com.ej2.repository.BoardRepository;
import com.ej2.repository.PostRepository;
import com.ej2.repository.PostViewLogRepository;
import com.ej2.util.AnonymousIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostViewLogRepository postViewLogRepository;

    // Get all posts ordered by creation date (newest first)
    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get post by ID
    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    // Create new post
    public Post createPost(Post post) {
        // Check if this is an anonymous board
        Optional<Board> boardOpt = boardRepository.findById(post.getBoardId());
        if (boardOpt.isPresent() && Boolean.TRUE.equals(boardOpt.get().getIsAnonymous())) {
            // Save first to get post ID, then generate anonymous ID
            Post savedPost = postRepository.save(post);
            String anonymousId = AnonymousIdGenerator.generateAnonymousId(
                savedPost.getUserId(),
                savedPost.getId()
            );
            savedPost.setAnonymousId(anonymousId);
            return postRepository.save(savedPost);
        }
        return postRepository.save(post);
    }

    // Update existing post
    public Post updatePost(Long id, Post postDetails) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        post.setTitle(postDetails.getTitle());
        post.setContent(postDetails.getContent());

        return postRepository.save(post);
    }

    // Delete post
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        postRepository.delete(post);
    }

    // Search posts by title
    public List<Post> searchPostsByTitle(String keyword) {
        return postRepository.findByTitleContaining(keyword);
    }

    // Get posts by board ID
    public List<Post> getPostsByBoardId(Long boardId) {
        return postRepository.findByBoardIdOrderByCreatedAtDesc(boardId);
    }

    // Increment view count with IP-based duplicate prevention
    public void incrementViewCount(Long postId, Long userId, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // Check if this user/IP has viewed this post in the last 24 hours
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        boolean hasViewed = false;

        if (userId != null) {
            // Check by user ID (for logged-in users)
            Optional<PostViewLog> userViewLog = postViewLogRepository
                    .findByPostIdAndUserIdAndViewedAtAfter(postId, userId, oneDayAgo);
            hasViewed = userViewLog.isPresent();
        } else if (ipAddress != null) {
            // Check by IP address (for non-logged-in users)
            Optional<PostViewLog> ipViewLog = postViewLogRepository
                    .findByPostIdAndIpAddressAndViewedAtAfter(postId, ipAddress, oneDayAgo);
            hasViewed = ipViewLog.isPresent();
        }

        // Only increment if not viewed recently
        if (!hasViewed) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);

            // Log this view
            PostViewLog viewLog = new PostViewLog(postId, userId, ipAddress);
            postViewLogRepository.save(viewLog);
        }
    }

    // Legacy method for backward compatibility
    public void incrementViewCount(Long id) {
        incrementViewCount(id, null, null);
    }

    // Increment like count
    public void incrementLikeCount(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
    }
}
