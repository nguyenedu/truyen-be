package com.example.truyen.service;

import com.example.truyen.dto.request.AuthorRequest;
import com.example.truyen.dto.response.AuthorResponse;

import java.util.List;

// Interface AuthorService
public interface AuthorService {

    // Lấy tất cả tác giả
    List<AuthorResponse> getAllAuthors();

    // Lấy thông tin chi tiết của tác giả
    AuthorResponse getAuthorById(Long id);

    // Tạo mới tác giả
    AuthorResponse createAuthor(AuthorRequest request);

    // Cập nhật thông tin tác giả
    AuthorResponse updateAuthor(Long id, AuthorRequest request);

    // Xóa tác giả
    void deleteAuthor(Long id);

    // Tìm kiếm tác giả theo tên
    List<AuthorResponse> searchAuthors(String name);
}