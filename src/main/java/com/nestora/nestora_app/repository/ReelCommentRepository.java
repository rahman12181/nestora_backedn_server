package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.ReelComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReelCommentRepository extends JpaRepository<ReelComment, Long> {

    List<ReelComment> findByReel_IdOrderByCreatedAtDesc(Long reelId);
}