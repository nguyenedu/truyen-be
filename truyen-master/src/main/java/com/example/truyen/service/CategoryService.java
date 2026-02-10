package com.example.truyen.service;

import com.example.truyen.dto.request.CategoryRequest;
import com.example.truyen.dto.response.CategoryResponse;

import java.util.List;

// Interface CategoryService
public interface CategoryService {

    // Lấy tất cả danh mục
    List<CategoryResponse> getAllCategories();

    // Lấy chi tiết danh mục theo ID
    CategoryResponse getCategoryById(Long id);

    // Tạo danh mục mới
    CategoryResponse createCategory(CategoryRequest request);

    // Cập nhật thông tin danh mục
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    // Xóa danh mục theo ID
    void deleteCategory(Long id);
}