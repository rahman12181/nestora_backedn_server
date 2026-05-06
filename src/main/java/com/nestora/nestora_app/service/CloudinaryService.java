package com.nestora.nestora_app.service;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nestora.nestora_app.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file, String folder) {
        try {
            Map result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "nestora/" + folder,
                            "resource_type", "image"
                    )
            );
            return (String) result.get("secure_url");
        } catch (Exception e) {
            log.error("Image upload failed: {}", e.getMessage());
            throw new AppException(
                    "Failed to upload image. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public String uploadDocument(MultipartFile file, String folder) {
        try {
            Map result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "nestora/" + folder,
                            "resource_type", "auto"
                    )
            );
            return (String) result.get("secure_url");
        } catch (Exception e) {
            log.error("Document upload failed: {}", e.getMessage());
            throw new AppException(
                    "Failed to upload document. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public String uploadVideo(MultipartFile file, String folder) {
        try {
            Map result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "nestora/" + folder,
                            "resource_type", "video"
                    )
            );
            return (String) result.get("secure_url");
        } catch (Exception e) {
            log.error("Video upload failed: {}", e.getMessage());
            throw new AppException(
                    "Failed to upload video. Please try again.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String publicId = extractPublicId(fileUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("File delete failed: {}", e.getMessage());
        }
    }

    private String extractPublicId(String url) {
        String[] parts = url.split("/upload/");
        String afterUpload = parts[1];
        if (afterUpload.startsWith("v")) {
            afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
        }
        return afterUpload.substring(0, afterUpload.lastIndexOf("."));
    }
}