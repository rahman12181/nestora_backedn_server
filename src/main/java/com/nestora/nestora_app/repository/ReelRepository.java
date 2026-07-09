package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.Reel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReelRepository extends JpaRepository<Reel, Long> {

    // Main feed — newest published reels first
    Page<Reel> findByIsPublishedTrueOrderByCreatedAtDesc(Pageable pageable);

    // Owner's own uploaded reels (published or not)
    List<Reel> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    // Reels for one property (shown on property detail screen)
    List<Reel> findByProperty_IdAndIsPublishedTrueOrderByCreatedAtDesc(Long propertyId);

    @Modifying
    @Query("UPDATE Reel r SET r.viewCount = r.viewCount + 1 WHERE r.id = :reelId")
    void incrementViewCount(@Param("reelId") Long reelId);

    @Modifying
    @Query("UPDATE Reel r SET r.shareCount = r.shareCount + 1 WHERE r.id = :reelId")
    void incrementShareCount(@Param("reelId") Long reelId);

    @Modifying
    @Query("UPDATE Reel r SET r.likeCount = r.likeCount + 1 WHERE r.id = :reelId")
    void incrementLikeCount(@Param("reelId") Long reelId);

    @Modifying
    @Query("UPDATE Reel r SET r.likeCount = CASE WHEN r.likeCount > 0 THEN r.likeCount - 1 ELSE 0 END WHERE r.id = :reelId")
    void decrementLikeCount(@Param("reelId") Long reelId);

    @Modifying
    @Query("UPDATE Reel r SET r.commentCount = r.commentCount + 1 WHERE r.id = :reelId")
    void incrementCommentCount(@Param("reelId") Long reelId);
}