package com.example.truyen.controller;

import com.example.truyen.dto.request.CoinPackageRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.CoinPackageResponse;
import com.example.truyen.service.CoinPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coin-packages")
@RequiredArgsConstructor
public class CoinPackageController {

    private final CoinPackageService coinPackageService;

    // Lấy danh sách gói xu đang active
    @GetMapping
    public ResponseEntity<ApiResponse<List<CoinPackageResponse>>> getAllActivePackages() {
        List<CoinPackageResponse> packages = coinPackageService.getAllActivePackages();
        return ResponseEntity.ok(ApiResponse.success("Get all active packages successfully", packages));
    }

    // Xem chi tiết gói xu
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CoinPackageResponse>> getById(@PathVariable Long id) {
        CoinPackageResponse coinPackage = coinPackageService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Get package details successfully", coinPackage));
    }

    // Tạo gói xu mới (Admin, Super Admin)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CoinPackageResponse>> create(
            @Valid @RequestBody CoinPackageRequest request) {
        CoinPackageResponse coinPackage = coinPackageService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create coin package successfully", coinPackage));
    }

    // Cập nhật gói xu (Admin, Super Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CoinPackageResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CoinPackageRequest request) {
        CoinPackageResponse coinPackage = coinPackageService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Update coin package successfully", coinPackage));
    }

    // Xóa gói xu (Admin, Super Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        coinPackageService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Delete coin package successfully", null));
    }
}
