package com.ej2.service;

import com.ej2.dto.PostDTO;
import com.ej2.model.Board;
import com.ej2.model.Post;
import com.ej2.model.PostViewLog;
import com.ej2.model.PostLikeLog;
import com.ej2.model.User;
import com.ej2.repository.BoardRepository;
import com.ej2.repository.PostRepository;
import com.ej2.repository.PostViewLogRepository;
import com.ej2.repository.PostLikeLogRepository;
import com.ej2.repository.UserRepository;
import com.ej2.util.AnonymousIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private PostLikeLogRepository postLikeLogRepository;

    @Autowired
    private UserRepository userRepository;

    // Get all posts ordered by creation date (newest first)
    public List<PostDTO> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return convertToPostDTOList(posts);
    }

    // Get post by ID (DTO形式で返す)
    public Optional<PostDTO> getPostById(Long id) {
        Optional<Post> postOpt = postRepository.findById(id);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            String authorNickname = getAuthorNickname(post);
            return Optional.of(new PostDTO(post, authorNickname));
        }
        return Optional.empty();
    }

    // 投稿エンティティを直接取得（権限検証用）
    public Post getPostEntityById(Long id) {
        return postRepository.findById(id).orElse(null);
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
    public List<PostDTO> getPostsByBoardId(Long boardId) {
        List<Post> posts = postRepository.findByBoardIdOrderByCreatedAtDesc(boardId);
        return convertToPostDTOList(posts);
    }

    // Helper method to convert Post list to PostDTO list
    private List<PostDTO> convertToPostDTOList(List<Post> posts) {
        List<PostDTO> postDTOs = new ArrayList<PostDTO>();
        for (Post post : posts) {
            String authorNickname = getAuthorNickname(post);
            postDTOs.add(new PostDTO(post, authorNickname));
        }
        return postDTOs;
    }

    // Helper method to get author nickname (or username if nickname doesn't exist)
    private String getAuthorNickname(Post post) {
        User user = userRepository.findById(post.getUserId());
        if (user != null) {
            // Use name field as nickname (display name)
            return user.getName();
        }
        return "Unknown User";
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

    // Increment like count with IP-based duplicate prevention
    public void incrementLikeCount(Long postId, Long userId, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // Check if this user/IP has liked this post in the last 24 hours
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        boolean hasLiked = false;

        if (userId != null) {
            // Check by user ID (for logged-in users)
            Optional<PostLikeLog> userLikeLog = postLikeLogRepository
                    .findByPostIdAndUserIdAndLikedAtAfter(postId, userId, oneDayAgo);
            hasLiked = userLikeLog.isPresent();
        } else if (ipAddress != null) {
            // Check by IP address (for non-logged-in users)
            Optional<PostLikeLog> ipLikeLog = postLikeLogRepository
                    .findByPostIdAndIpAddressAndLikedAtAfter(postId, ipAddress, oneDayAgo);
            hasLiked = ipLikeLog.isPresent();
        }

        // Only increment if not liked recently
        if (!hasLiked) {
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);

            // Log this like
            PostLikeLog likeLog = new PostLikeLog(postId, userId, ipAddress);
            postLikeLogRepository.save(likeLog);
        }
    }

    // Legacy method for backward compatibility
    public void incrementLikeCount(Long id) {
        incrementLikeCount(id, null, null);
    }
}
