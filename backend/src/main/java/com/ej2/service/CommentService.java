package com.ej2.service;

import com.ej2.dto.CommentDTO;
import com.ej2.model.Board;
import com.ej2.model.Comment;
import com.ej2.model.CommentLikeLog;
import com.ej2.model.Post;
import com.ej2.model.User;
import com.ej2.repository.BoardRepository;
import com.ej2.repository.CommentLikeLogRepository;
import com.ej2.repository.CommentRepository;
import com.ej2.repository.PostRepository;
import com.ej2.repository.UserRepository;
import com.ej2.util.AnonymousIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeLogRepository commentLikeLogRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    public List<CommentDTO> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return convertToCommentDTOList(comments);
    }

    public List<CommentDTO> getCommentsByPostIdDesc(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return convertToCommentDTOList(comments);
    }

    public List<CommentDTO> getTopLevelComments(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId);
        return convertToCommentDTOList(comments);
    }

    public List<CommentDTO> getReplies(Long parentId) {
        List<Comment> comments = commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
        return convertToCommentDTOList(comments);
    }

    public List<CommentDTO> getRepliesDesc(Long parentId) {
        List<Comment> comments = commentRepository.findByParentIdOrderByCreatedAtDesc(parentId);
        return convertToCommentDTOList(comments);
    }

    private List<CommentDTO> convertToCommentDTOList(List<Comment> comments) {
        List<CommentDTO> dtoList = new ArrayList<CommentDTO>();
        for (Comment comment : comments) {
            String authorNickname = getAuthorNickname(comment);
            dtoList.add(new CommentDTO(comment, authorNickname));
        }
        return dtoList;
    }

    private String getAuthorNickname(Comment comment) {
        User user = userRepository.findById(comment.getUserId());
        if (user != null) {
            return user.getName();
        }
        return "Unknown User";
    }

    public Optional<Comment> getCommentById(Long id) {
        return commentRepository.findById(id);
    }

    // コメントエンティティを直接取得（権限検証用）
    public Comment getCommentEntityById(Long id) {
        return commentRepository.findById(id).orElse(null);
    }

    public Comment createComment(Comment comment) {
        // [추가된 부분 1] 부모 댓글(답글 대상)이 삭제되었는지 확인
        if (comment.getParentId() != null) {
            Comment parentComment = commentRepository.findById(comment.getParentId())
                .orElseThrow(() -> new RuntimeException("Parent comment not found with id: " + comment.getParentId()));
            
            if (Boolean.TRUE.equals(parentComment.getIsDeleted())) {
                throw new RuntimeException("削除されたコメントには返信できません。");
            }
        }

        // Check if this post is in an anonymous board
        Optional<Post> postOpt = postRepository.findById(comment.getPostId());
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            Optional<Board> boardOpt = boardRepository.findById(post.getBoardId());

            if (boardOpt.isPresent() && Boolean.TRUE.equals(boardOpt.get().getIsAnonymous())) {
                // Generate anonymous ID for comment (consistent with post)
                String anonymousId = AnonymousIdGenerator.generateAnonymousId(
                    comment.getUserId(),
                    comment.getPostId()
                );
                comment.setAnonymousId(anonymousId);
            }

            post.setCommentCount(post.getCommentCount() + 1);
            postRepository.save(post);
        }

        return commentRepository.save(comment);
    }

    public Comment updateComment(Long id, Comment commentDetails) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        comment.setContent(commentDetails.getContent());
        return commentRepository.save(comment);
    }

    // コメントを削除（ソフトデリート）
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        comment.setIsDeleted(true);
        comment.setContent("削除されたコメントです。");

        Optional<Post> postOpt = postRepository.findById(comment.getPostId());
        Post post = postOpt.get();
        post.setCommentCount(post.getCommentCount() - 1);
        postRepository.save(post);
        commentRepository.save(comment);
    }

    public Long getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    /**
     * Toggle like on a comment. Returns true if liked, false if unliked.
     */
    public boolean toggleLike(Long commentId, Long userId, String ipAddress) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        // [추가된 부분 2] 삭제된 댓글 좋아요 금지
        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new RuntimeException("削除されたコメントには「いいね」を押すことができません。");
        }

        Optional<CommentLikeLog> existingLike;

        if (userId != null) {
            existingLike = commentLikeLogRepository.findByCommentIdAndUserId(commentId, userId);
        } else {
            existingLike = commentLikeLogRepository.findByCommentIdAndIpAddress(commentId, ipAddress);
        }

        if (existingLike.isPresent()) {
            // Already liked - remove like (toggle off)
            commentLikeLogRepository.delete(existingLike.get());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentRepository.save(comment);
            return false;
        } else {
            // Not liked yet - add like
            CommentLikeLog likeLog = new CommentLikeLog(commentId, userId, ipAddress);
            commentLikeLogRepository.save(likeLog);
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);
            return true;
        }
    }

    /**
     * Check if user has liked a specific comment
     */
    public boolean hasUserLiked(Long commentId, Long userId, String ipAddress) {
        if (userId != null) {
            return commentLikeLogRepository.findByCommentIdAndUserId(commentId, userId).isPresent();
        } else {
            return commentLikeLogRepository.findByCommentIdAndIpAddress(commentId, ipAddress).isPresent();
        }
    }
}