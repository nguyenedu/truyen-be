package com.example.truyen.controller;

import com.example.truyen.dto.request.CategoryRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.CategoryResponse;
import com.example.truyen.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // Lấy danh sách tất cả thể loại
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity
                .ok(ApiResponse.success("Get all categories successfully", categoryService.getAllCategories()));
    }

    // Lấy chi tiết thể loại theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Get category details successfully", categoryService.getCategoryById(id)));
    }

    // Tạo thể loại mới (Admin, Super Admin)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create category successfully", categoryService.createCategory(request)));
    }

    // Cập nhật thể loại (Admin, Super Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Update category successfully", categoryService.updateCategory(id, request)));
    }

    // Xóa thể loại (Admin, Super Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Delete category successfully", null));
    }
}