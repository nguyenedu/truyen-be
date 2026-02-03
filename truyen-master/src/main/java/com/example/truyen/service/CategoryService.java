package com.example.truyen.service;

import com.example.truyen.dto.request.CategoryRequest;
import com.example.truyen.dto.response.CategoryResponse;
import com.example.truyen.entity.Category;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Lấy danh sách tất cả thể loại.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết thể loại theo ID.
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return convertToResponse(category);
    }

    /**
     * Tạo thể loại mới.
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Thể loại '" + request.getName() + "' đã tồn tại");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return convertToResponse(savedCategory);
    }

    /**
     * Cập nhật thông tin thể loại theo ID.
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (request.getName() != null && !category.getName().equals(request.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new BadRequestException("Thể loại '" + request.getName() + "' đã tồn tại");
            }
            category.setName(request.getName());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        Category updatedCategory = categoryRepository.save(category);
        return convertToResponse(updatedCategory);
    }

    /**
     * Xóa thể loại theo ID.
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        categoryRepository.delete(category);
    }

    /**
     * Chuyển đổi Category sang CategoryResponse DTO.
     */
    private CategoryResponse convertToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}