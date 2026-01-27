package com.ej2.service;

import com.ej2.model.Board;
import com.ej2.model.Comment;
import com.ej2.model.Post;
import com.ej2.repository.BoardRepository;
import com.ej2.repository.CommentRepository;
import com.ej2.repository.PostRepository;
import com.ej2.util.AnonymousIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BoardRepository boardRepository;

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public List<Comment> getTopLevelComments(Long postId) {
        return commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId);
    }

    public List<Comment> getReplies(Long parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    public Optional<Comment> getCommentById(Long id) {
        return commentRepository.findById(id);
    }

    // コメントエンティティを直接取得（権限検証用）
    public Comment getCommentEntityById(Long id) {
        return commentRepository.findById(id).orElse(null);
    }

    public Comment createComment(Comment comment) {
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
        commentRepository.save(comment);
    }

    public Long getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    public void incrementLikeCount(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentRepository.save(comment);
    }
}
