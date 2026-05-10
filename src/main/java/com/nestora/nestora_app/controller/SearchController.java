package com.nestora.nestora_app.controller;


import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PropertySearchResponse>>> searchProperties(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) String sort) {

        List<PropertySearchResponse> results = searchService.searchProperties(
                city, type, gender, minRent, maxRent,
                pincode, lat, lng, radius, sort
        );

        if (results.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success("No properties found", results)
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success("Properties found", results)
        );
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<PropertySearchResponse>> getPropertyDetail(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(
                ApiResponse.success("Property fetched",
                        searchService.getPropertyDetail(propertyId))
        );
    }

    @GetMapping("/{propertyId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getPropertyRooms(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(
                ApiResponse.success("Rooms fetched",
                        searchService.getPropertyRooms(propertyId))
        );
    }

    @GetMapping("/{propertyId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getPropertyReviews(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(
                ApiResponse.success("Reviews fetched",
                        searchService.getPropertyReviews(propertyId))
        );
    }
}