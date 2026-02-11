package com.ej2.repository;

import com.ej2.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // Basic query methods - Spring Data JPA will implement these automatically
    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByTitleContaining(String keyword);

    List<Post> findByTitleContainingOrderByCreatedAtDesc(String keyword);

    List<Post> findByBoardIdAndIsBlindedFalseOrderByCreatedAtDesc(Long boardId);

    List<Post> findAllByOrderByViewCountDesc();

    List<Post> findByBoardIdAndIsBlindedFalseOrderByViewCountDesc(Long boardId);

    @Query(value = "select p.* from posts p left join post_like_logs ll on p.id = ll.post_id and ll.liked_at >= date_sub(now(), interval 1 day) where p.board_id = :boardId and p.is_blinded = 0 group by p.id order by count(ll.id) desc", nativeQuery = true)
    List<Post> findAllOrderByDayLikeCount(@Param("boardId") Long boardId);

    @Query(value = "select p.* from posts p left join post_like_logs ll on p.id = ll.post_id and ll.liked_at >= date_sub(now(), interval 7 day) where p.board_id = :boardId and p.is_blinded = 0 group by p.id order by count(ll.id) desc", nativeQuery = true)
    List<Post> findAllOrderByWeekLikeCount(@Param("boardId") Long boardId);

    @Query(value = "select p.* from posts p left join post_like_logs ll on p.id = ll.post_id and ll.liked_at >= date_sub(now(), interval 30 day) where p.board_id = :boardId and p.is_blinded = 0 group by p.id order by count(ll.id) desc", nativeQuery = true)
    List<Post> findAllOrderByMonthLikeCount(@Param("boardId") Long boardId);
}
