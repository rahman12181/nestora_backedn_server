package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.ReelLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface ReelLikeRepository extends JpaRepository<ReelLike, Long> {

    boolean existsByReel_IdAndUser_Id(Long reelId, Long userId);

    @Transactional
    void deleteByReel_IdAndUser_Id(Long reelId, Long userId);

    // Bulk-resolve which reels (out of a page/list) the current user has liked
    @Query("SELECT rl.reel.id FROM ReelLike rl WHERE rl.user.id = :userId AND rl.reel.id IN :reelIds")
    List<Long> findLikedReelIds(@Param("reelIds") Collection<Long> reelIds, @Param("userId") Long userId);
}