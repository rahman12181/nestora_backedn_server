package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.RentPaymentResponse;
import com.nestora.nestora_app.enums.RentPaymentStatus;
import com.nestora.nestora_app.service.RentPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminRentPaymentController {

    private final RentPaymentService rentPaymentService;

    // 14.7 Get All Rent Payments (Admin) — full financial history, optional ?status= filter
    @GetMapping("/admin/rent-payments/all")
    public ResponseEntity<ApiResponse<List<RentPaymentResponse>>> getAllPayments(
            @RequestParam(required = false) RentPaymentStatus status) {

        List<RentPaymentResponse> payments = rentPaymentService.getAllPayments(status);
        return ResponseEntity.ok(ApiResponse.success("Rent payments fetched", payments));
    }
}