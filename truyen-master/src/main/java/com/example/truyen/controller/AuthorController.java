package com.example.truyen.controller;

import com.example.truyen.dto.request.AuthorRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.AuthorResponse;
import com.example.truyen.service.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class    AuthorController {

    private final AuthorService authorService;

    // Lấy tất cả tác giả
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> getAllAuthors() {
        List<AuthorResponse> authors = authorService.getAllAuthors();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tác giả thành công", authors));
    }

    // Lấy chi tiết 1 tác giả
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthorResponse>> getAuthorById(@PathVariable Long id) {
        AuthorResponse author = authorService.getAuthorById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tác giả thành công", author));
    }

    // Tạo tác giả mới (CHỈ ADMIN và SUPER_ADMIN)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthor(
            @Valid @RequestBody AuthorRequest request
    ) {
        AuthorResponse author = authorService.createAuthor(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tác giả thành công", author));
    }

    // Cập nhật tác giả (CHỈ ADMIN  và SUPER_ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthor(
            @PathVariable Long id,
            @Valid @RequestBody AuthorRequest request
    ) {
        AuthorResponse author = authorService.updateAuthor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tác giả thành công", author));
    }

    // Xóa tác giả (CHỈ ADMIN và SUPER_ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAuthor(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tác giả thành công", null));
    }
}