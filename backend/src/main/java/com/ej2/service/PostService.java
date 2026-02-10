package com.ej2.service;

import com.ej2.dto.PostDTO;
import com.ej2.model.Board;
import com.ej2.model.Post;
import com.ej2.model.PostViewLog;
import com.ej2.model.PostLikeLog;
import com.ej2.model.PostDislikeLog;
import com.ej2.model.User;
import com.ej2.repository.BoardRepository;
import com.ej2.repository.PostRepository;
import com.ej2.repository.PostViewLogRepository;
import com.ej2.repository.PostLikeLogRepository;
import com.ej2.repository.PostDislikeLogRepository;
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

    @Autowired
    private PostDislikeLogRepository postDislikeLogRepository;

    @Autowired
    private PostImageService postImageService; 

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

    // 조회수순 정렬
    public List<PostDTO> getByBoardIdOrderByViewCount(Long boardId) {
        List<Post> posts = postRepository.findByBoardIdAndIsBlindedFalseOrderByViewCountDesc(boardId);
        return convertToPostDTOList(posts);
    }

    public List<PostDTO> getAllOrderByDayLikeCount(Long boardId) {
        List<Post> posts = postRepository.findAllOrderByDayLikeCount(boardId);
        return convertToPostDTOList(posts);
    }

    public List<PostDTO> getAllOrderByWeekLikeCount(Long boardId) {
        List<Post> posts = postRepository.findAllOrderByWeekLikeCount(boardId);
        return convertToPostDTOList(posts);
    }

    public List<PostDTO> getAllOrderByMonthLikeCount(Long boardId) {
        List<Post> posts = postRepository.findAllOrderByMonthLikeCount(boardId);
        return convertToPostDTOList(posts);
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

        // Delete all images associated with this post (from S3 and database)
        postImageService.deleteImagesByPostId(id);

        // Delete the post
        postRepository.delete(post);
    }

    // Search posts by title
    public List<PostDTO> searchPostsByTitle(String keyword) {
        List<Post> posts = postRepository.findByTitleContainingOrderByCreatedAtDesc(keyword);
        return convertToPostDTOList(posts);
    }

    // Get posts by board ID
    public List<PostDTO> getPostsByBoardId(Long boardId) {
        List<Post> posts = postRepository.findByBoardIdAndIsBlindedFalseOrderByCreatedAtDesc(boardId);
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

    // 조회수 증가 (IP/사용자 기반 중복 방지)
    public void incrementViewCount(Long postId, Long userId, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // 24시간 이내 동일 사용자/IP의 조회 이력 확인
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        boolean hasViewed = false;

        if (userId != null) {
            // 로그인 사용자: userId로 중복 조회 확인
            List<PostViewLog> userViewLogs = postViewLogRepository
                    .findByPostIdAndUserIdAndViewedAtAfter(postId, userId, oneDayAgo);
            hasViewed = !userViewLogs.isEmpty();
        } else if (ipAddress != null) {
            // 비로그인 사용자: IP 주소로 중복 조회 확인
            List<PostViewLog> ipViewLogs = postViewLogRepository
                    .findByPostIdAndIpAddressAndViewedAtAfter(postId, ipAddress, oneDayAgo);
            hasViewed = !ipViewLogs.isEmpty();
        }

        // 최근 조회 이력이 없을 때만 조회수 증가
        if (!hasViewed) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);

            // 조회 로그 저장
            PostViewLog viewLog = new PostViewLog(postId, userId, ipAddress);
            postViewLogRepository.save(viewLog);
        }
    }

    // 하위 호환용 조회수 증가 메서드
    public void incrementViewCount(Long id) {
        incrementViewCount(id, null, null);
    }

    // 좋아요 수 증가 (IP/사용자 기반 중복 방지)
    public void incrementLikeCount(Long postId, Long userId, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // 24시간 이내 동일 사용자/IP의 좋아요 이력 확인
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        boolean hasLiked = false;

        if (userId != null) {
            // 로그인 사용자: userId로 중복 좋아요 확인
            List<PostLikeLog> userLikeLogs = postLikeLogRepository
                    .findByPostIdAndUserIdAndLikedAtAfter(postId, userId, oneDayAgo);
            hasLiked = !userLikeLogs.isEmpty();
        } else if (ipAddress != null) {
            // 비로그인 사용자: IP 주소로 중복 좋아요 확인
            List<PostLikeLog> ipLikeLogs = postLikeLogRepository
                    .findByPostIdAndIpAddressAndLikedAtAfter(postId, ipAddress, oneDayAgo);
            hasLiked = !ipLikeLogs.isEmpty();
        }

        // 최근 좋아요 이력이 없을 때만 좋아요 수 증가
        if (!hasLiked) {
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);

            // 좋아요 로그 저장
            PostLikeLog likeLog = new PostLikeLog(postId, userId, ipAddress);
            postLikeLogRepository.save(likeLog);
        }
    }

    // 하위 호환용 좋아요 증가 메서드
    public void incrementLikeCount(Long id) {
        incrementLikeCount(id, null, null);
    }

    // 싫어요 수 증가 (IP/사용자 기반 중복 방지)
    public void incrementDislikeCount(Long postId, Long userId, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // 24시간 이내 동일 사용자/IP의 싫어요 이력 확인
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        boolean hasDisliked = false;

        if (userId != null) {
            // 로그인 사용자: userId로 중복 싫어요 확인
            List<PostDislikeLog> userDislikeLogs = postDislikeLogRepository
                    .findByPostIdAndUserIdAndDislikedAtAfter(postId, userId, oneDayAgo);
            hasDisliked = !userDislikeLogs.isEmpty();
        } else if (ipAddress != null) {
            // 비로그인 사용자: IP 주소로 중복 싫어요 확인
            List<PostDislikeLog> ipDislikeLogs = postDislikeLogRepository
                    .findByPostIdAndIpAddressAndDislikedAtAfter(postId, ipAddress, oneDayAgo);
            hasDisliked = !ipDislikeLogs.isEmpty();
        }

        // 최근 싫어요 이력이 없을 때만 싫어요 수 증가
        if (!hasDisliked) {
            post.setDislikeCount(post.getDislikeCount() + 1);
            postRepository.save(post);

            // 싫어요 로그 저장
            PostDislikeLog dislikeLog = new PostDislikeLog(postId, userId, ipAddress);
            postDislikeLogRepository.save(dislikeLog);
        }
    }

    // 하위 호환용 싫어요 증가 메서드
    public void incrementDislikeCount(Long id) {
        incrementDislikeCount(id, null, null);
    }
}
