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

    /**
     * Lấy danh sách tất cả thể loại.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy danh sách thể loại thành công", categoryService.getAllCategories()));
    }

    /**
     * Lấy chi tiết thể loại theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin thể loại thành công", categoryService.getCategoryById(id)));
    }

    /**
     * Tạo thể loại mới.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thể loại thành công", categoryService.createCategory(request)));
    }

    /**
     * Cập nhật thể loại đã tồn tại.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật thể loại thành công", categoryService.updateCategory(id, request)));
    }

    /**
     * Xóa một thể loại.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thể loại thành công", null));
    }
}