package com.nestora.nestora_app.controller;


import com.nestora.nestora_app.dto.request.*;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    // =============================================
    // OWNER — Property CRUD
    // =============================================

    @PostMapping("/owner/properties")
    public ResponseEntity<ApiResponse<PropertyResponse>> addProperty(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PropertyCreateRequest request) {

        PropertyResponse response = propertyService.addProperty(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Property added successfully. Admin will review and publish it.", response));
    }

    @GetMapping("/owner/properties")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getMyProperties(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                ApiResponse.success("Properties fetched",
                        propertyService.getMyProperties(currentUser))
        );
    }

    @GetMapping("/owner/properties/{propertyId}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getPropertyById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId) {

        return ResponseEntity.ok(
                ApiResponse.success("Property fetched",
                        propertyService.getPropertyById(currentUser, propertyId))
        );
    }

    @PutMapping("/owner/properties/{propertyId}")
    public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @RequestBody PropertyUpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Property updated successfully",
                        propertyService.updateProperty(currentUser, propertyId, request))
        );
    }

    @DeleteMapping("/owner/properties/{propertyId}")
    public ResponseEntity<ApiResponse<String>> deleteProperty(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId) {

        String message = propertyService.deleteProperty(currentUser, propertyId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // =============================================
    // OWNER — Media Upload
    // =============================================

    @PostMapping(
            value = "/owner/properties/{propertyId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "IMAGE") String mediaType,
            @RequestParam(defaultValue = "false") Boolean isPrimary) {

        MediaResponse response = propertyService.uploadMedia(
                currentUser, propertyId, file, mediaType, isPrimary
        );
        return ResponseEntity.ok(ApiResponse.success("Media uploaded successfully", response));
    }

    @DeleteMapping("/owner/properties/{propertyId}/media/{mediaId}")
    public ResponseEntity<ApiResponse<String>> deleteMedia(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @PathVariable Long mediaId) {

        String message = propertyService.deleteMedia(currentUser, propertyId, mediaId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // =============================================
    // OWNER — Room Management
    // =============================================

    @PostMapping("/owner/properties/{propertyId}/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> addRoom(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @Valid @RequestBody RoomCreateRequest request) {

        RoomResponse response = propertyService.addRoom(currentUser, propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room added successfully", response));
    }

    @GetMapping("/owner/properties/{propertyId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRooms(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId) {

        return ResponseEntity.ok(
                ApiResponse.success("Rooms fetched",
                        propertyService.getRooms(currentUser, propertyId))
        );
    }

    @PutMapping("/owner/properties/{propertyId}/rooms/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @PathVariable Long roomId,
            @RequestBody RoomUpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Room updated successfully",
                        propertyService.updateRoom(currentUser, propertyId, roomId, request))
        );
    }

    @PatchMapping("/owner/properties/{propertyId}/rooms/{roomId}/status")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomStatusRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Room status updated",
                        propertyService.updateRoomStatus(currentUser, propertyId, roomId, request))
        );
    }

    @DeleteMapping("/owner/properties/{propertyId}/rooms/{roomId}")
    public ResponseEntity<ApiResponse<String>> deleteRoom(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @PathVariable Long roomId) {

        String message = propertyService.deleteRoom(currentUser, propertyId, roomId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
