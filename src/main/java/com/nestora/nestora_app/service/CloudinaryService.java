package com.nestora.nestora_app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nestora.nestora_app.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Allowed file types
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/jpg"
    );

    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "application/pdf", "image/jpg"
    );

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/quicktime", "video/x-msvideo", "video/mpeg"
    );

    // Max sizes
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;   // 5MB
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;  // 50MB
    private static final long MAX_DOC_SIZE = 5 * 1024 * 1024;     // 5MB

    /**
     * Validate file before upload
     */
    private void validateFile(MultipartFile file,
                              List<String> allowedTypes,
                              long maxSize,
                              String context) {

        if (file == null || file.isEmpty()) {
            throw new AppException("File is empty", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > maxSize) {
            throw new AppException(
                    "File too large. Max size: " + (maxSize / 1024 / 1024) + "MB",
                    HttpStatus.BAD_REQUEST
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new AppException(
                    "Invalid file type for " + context +
                            ". Allowed: " + String.join(", ", allowedTypes),
                    HttpStatus.BAD_REQUEST
            );
        }

        log.info("File validation passed: {} ({})", file.getOriginalFilename(), contentType);
    }

    /**
     * Upload Image with validation
     */
    public String uploadImage(MultipartFile file, String folder) {
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE, "image");

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "nestora/" + folder,
                            "resource_type", "image"
                    )
            );
            log.info("Image uploaded successfully: {}", result.get("secure_url"));
            return (String) result.get("secure_url");
        } catch (Exception e) {
            log.error("Image upload failed: {}", e.getMessage());
            throw new AppException(
                    "Failed to upload image. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Upload Document with validation
     */
    public String uploadDocument(MultipartFile file, String folder) {
        validateFile(file, ALLOWED_DOC_TYPES, MAX_DOC_SIZE, "document");

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "nestora/" + folder,
                            "resource_type", "auto"
                    )
            );
            log.info("Document uploaded successfully: {}", result.get("secure_url"));
            return (String) result.get("secure_url");
        } catch (Exception e) {
            log.error("Document upload failed: {}", e.getMessage());
            throw new AppException(
                    "Failed to upload document. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Upload Video with validation
     */
    public String uploadVideo(MultipartFile file, String folder) {
        validateFile(file, ALLOWED_VIDEO_TYPES, MAX_VIDEO_SIZE, "video");

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "nestora/" + folder,
                            "resource_type", "video"
                    )
            );
            log.info("Video uploaded successfully: {}", result.get("secure_url"));
            return (String) result.get("secure_url");
        } catch (Exception e) {
            log.error("Video upload failed: {}", e.getMessage());
            throw new AppException(
                    "Failed to upload video. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Generic upload method that detects file type automatically
     */
    public String uploadFile(MultipartFile file, String folder) {
        String contentType = file.getContentType();

        if (contentType == null) {
            throw new AppException("Unknown file type", HttpStatus.BAD_REQUEST);
        }

        if (contentType.startsWith("image/")) {
            return uploadImage(file, folder);
        } else if (contentType.startsWith("video/")) {
            return uploadVideo(file, folder);
        } else {
            return uploadDocument(file, folder);
        }
    }

    /**
     * Delete file from Cloudinary
     */
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                log.warn("Cannot delete: File URL is null or empty");
                return;
            }

            String publicId = extractPublicId(fileUrl);
            if (publicId != null && !publicId.isEmpty()) {
                Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("File deleted successfully: {} - Result: {}", publicId, result.get("result"));
            }
        } catch (Exception e) {
            log.error("File delete failed for URL {}: {}", fileUrl, e.getMessage());
            // Don't throw exception for delete failures
        }
    }

    /**
     * Extract public ID from Cloudinary URL
     */
    private String extractPublicId(String url) {
        try {
            // Handle different URL formats
            String[] parts = url.split("/upload/");
            if (parts.length < 2) {
                log.warn("Invalid Cloudinary URL format: {}", url);
                return null;
            }

            String afterUpload = parts[1];

            // Remove version prefix if present (e.g., v1234567/)
            if (afterUpload.startsWith("v")) {
                int slashIndex = afterUpload.indexOf("/");
                if (slashIndex != -1) {
                    afterUpload = afterUpload.substring(slashIndex + 1);
                }
            }

            // Remove file extension
            int lastDotIndex = afterUpload.lastIndexOf(".");
            if (lastDotIndex != -1) {
                afterUpload = afterUpload.substring(0, lastDotIndex);
            }

            return afterUpload;
        } catch (Exception e) {
            log.error("Failed to extract public ID from URL: {}", url, e);
            return null;
        }
    }

    /**
     * Get file size in human readable format
     */
    public String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}