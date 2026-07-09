package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.ReelCommentResponse;
import com.nestora.nestora_app.dto.response.ReelResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.VerificationStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ASSUMPTIONS — please verify these two repositories exist with these exact method names
 * (they almost certainly already do, since PropertyService resolves the same relationships):
 *
 *   OwnerProfileRepository.findByUser(User user)  -> Optional<OwnerProfile>
 *   PropertyRepository.findById(Long id)          -> Optional<Property>   (standard JpaRepository method)
 *
 * If your OwnerProfileRepository uses a different method name (e.g. findByUser_Id(Long)),
 * just change the one call in resolveOwnerProfile() below.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReelServiceImpl implements ReelService {

    private static final String CLOUDINARY_FOLDER = "reels"; // -> stored under nestora/reels

    private final ReelRepository reelRepository;
    private final ReelLikeRepository reelLikeRepository;
    private final ReelCommentRepository reelCommentRepository;
    private final PropertyRepository propertyRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final CloudinaryService cloudinaryService;

    // ================= OWNER SIDE =================

    @Override
    @Transactional
    public ReelResponse uploadReel(User currentUser, Long propertyId, String caption, MultipartFile file) {

        OwnerProfile owner = resolveOwnerProfile(currentUser);

        if (owner.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AppException("Only verified owners can upload reels.", HttpStatus.FORBIDDEN);
        }

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException("Property not found", HttpStatus.NOT_FOUND));

        if (!property.getOwner().getId().equals(owner.getId())) {
            throw new AppException("You are not authorized to access this property.", HttpStatus.FORBIDDEN);
        }

        // CloudinaryService.uploadVideo already validates type (mp4/mov/avi/mpeg) and 50MB max,
        // and throws AppException itself on empty file / wrong type / oversized / upload failure.
        String videoUrl = cloudinaryService.uploadVideo(file, CLOUDINARY_FOLDER);

        Reel reel = Reel.builder()
                .property(property)
                .owner(owner)
                .videoUrl(videoUrl)
                .caption(caption)
                .isPublished(true) // flip to false here if you want admin moderation before going live
                .build();

        reel = reelRepository.save(reel);
        log.info("Reel {} uploaded by owner {} for property {}", reel.getId(), owner.getId(), propertyId);

        return toResponse(reel, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReelResponse> getMyReels(User currentUser) {
        OwnerProfile owner = resolveOwnerProfile(currentUser);

        return reelRepository.findByOwner_IdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(r -> toResponse(r, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String deleteReel(User currentUser, Long reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new AppException("Reel not found", HttpStatus.NOT_FOUND));

        if (!reel.getOwner().getUser().getId().equals(currentUser.getId())) {
            throw new AppException("You are not authorized to access this reel.", HttpStatus.FORBIDDEN);
        }

        // Optional: also remove the video asset from Cloudinary
        // cloudinaryService.deleteFile(reel.getVideoUrl());

        reelRepository.delete(reel);
        return "Reel deleted successfully";
    }

    // ================= USER / PUBLIC SIDE =================

    @Override
    @Transactional(readOnly = true)
    public Page<ReelResponse> getFeed(User currentUser, Pageable pageable) {
        Page<Reel> page = reelRepository.findByIsPublishedTrueOrderByCreatedAtDesc(pageable);
        Set<Long> likedIds = resolveLikedIds(page.getContent(), currentUser);
        return page.map(r -> toResponse(r, currentUser != null && likedIds.contains(r.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReelResponse> getReelsForProperty(Long propertyId, User currentUser) {
        List<Reel> reels = reelRepository.findByProperty_IdAndIsPublishedTrueOrderByCreatedAtDesc(propertyId);
        Set<Long> likedIds = resolveLikedIds(reels, currentUser);

        return reels.stream()
                .map(r -> toResponse(r, currentUser != null && likedIds.contains(r.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String likeReel(User currentUser, Long reelId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new AppException("Reel not found", HttpStatus.NOT_FOUND));

        if (reelLikeRepository.existsByReel_IdAndUser_Id(reelId, currentUser.getId())) {
            throw new AppException("You have already liked this reel", HttpStatus.CONFLICT);
        }

        ReelLike like = ReelLike.builder()
                .reel(reel)
                .user(currentUser)
                .build();

        reelLikeRepository.save(like);
        reelRepository.incrementLikeCount(reelId);
        return "Reel liked";
    }

    @Override
    @Transactional
    public String unlikeReel(User currentUser, Long reelId) {
        if (!reelRepository.existsById(reelId)) {
            throw new AppException("Reel not found", HttpStatus.NOT_FOUND);
        }
        if (!reelLikeRepository.existsByReel_IdAndUser_Id(reelId, currentUser.getId())) {
            throw new AppException("You have not liked this reel", HttpStatus.NOT_FOUND);
        }

        reelLikeRepository.deleteByReel_IdAndUser_Id(reelId, currentUser.getId());
        reelRepository.decrementLikeCount(reelId);
        return "Reel unliked";
    }

    @Override
    @Transactional
    public ReelCommentResponse addComment(User currentUser, Long reelId, String content) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new AppException("Reel not found", HttpStatus.NOT_FOUND));

        ReelComment comment = ReelComment.builder()
                .reel(reel)
                .user(currentUser)
                .content(content)
                .build();

        comment = reelCommentRepository.save(comment);
        reelRepository.incrementCommentCount(reelId);

        return ReelCommentResponse.builder()
                .commentId(comment.getId())
                .userId(currentUser.getId())
                .userName(currentUser.getName())
                .userPic(currentUser.getProfilePic())
                .content(comment.getContent())
                .isMine(true)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReelCommentResponse> getComments(Long reelId, User currentUser) {
        if (!reelRepository.existsById(reelId)) {
            throw new AppException("Reel not found", HttpStatus.NOT_FOUND);
        }

        return reelCommentRepository.findByReel_IdOrderByCreatedAtDesc(reelId)
                .stream()
                .map(c -> ReelCommentResponse.builder()
                        .commentId(c.getId())
                        .userId(c.getUser().getId())
                        .userName(c.getUser().getName())
                        .userPic(c.getUser().getProfilePic())
                        .content(c.getContent())
                        .isMine(currentUser != null && currentUser.getId().equals(c.getUser().getId()))
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String registerView(Long reelId) {
        if (!reelRepository.existsById(reelId)) {
            throw new AppException("Reel not found", HttpStatus.NOT_FOUND);
        }
        reelRepository.incrementViewCount(reelId);
        return "View registered";
    }

    @Override
    @Transactional
    public String registerShare(Long reelId) {
        if (!reelRepository.existsById(reelId)) {
            throw new AppException("Reel not found", HttpStatus.NOT_FOUND);
        }
        reelRepository.incrementShareCount(reelId);
        return "Share registered";
    }

    // ================= HELPERS =================

    private OwnerProfile resolveOwnerProfile(User currentUser) {
        return ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found. Please apply as owner first.", HttpStatus.NOT_FOUND));
    }

    private Set<Long> resolveLikedIds(List<Reel> reels, User currentUser) {
        if (currentUser == null || reels.isEmpty()) {
            return Set.of();
        }
        List<Long> reelIds = reels.stream().map(Reel::getId).collect(Collectors.toList());
        return Set.copyOf(reelLikeRepository.findLikedReelIds(reelIds, currentUser.getId()));
    }

    private ReelResponse toResponse(Reel reel, Boolean isLikedByMe) {
        Property property = reel.getProperty();
        OwnerProfile owner = reel.getOwner();
        User ownerUser = owner.getUser();

        return ReelResponse.builder()
                .reelId(reel.getId())
                .propertyId(property.getId())
                .propertyTitle(property.getTitle())
                .propertyCity(property.getCity())
                .ownerUserId(ownerUser.getId())
                .ownerName(ownerUser.getName())
                .ownerDisplayId(ownerUser.getDisplayId())
                .ownerProfilePic(ownerUser.getProfilePic())
                .isVerifiedOwner(owner.getVerificationStatus() == VerificationStatus.VERIFIED)
                .videoUrl(reel.getVideoUrl())
                .thumbnailUrl(reel.getThumbnailUrl())
                .caption(reel.getCaption())
                .durationSec(reel.getDurationSec())
                .viewCount(reel.getViewCount())
                .likeCount(reel.getLikeCount())
                .commentCount(reel.getCommentCount())
                .shareCount(reel.getShareCount())
                .isLikedByMe(isLikedByMe)
                .createdAt(reel.getCreatedAt())
                .build();
    }
}