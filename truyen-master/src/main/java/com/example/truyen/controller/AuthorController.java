package com.example.truyen.controller;

import com.example.truyen.dto.request.AuthorRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.AuthorResponse;
import com.example.truyen.service.AuthorService;
import com.example.truyen.service.MinIoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@CrossOrigin(origins = "http://localhost:5174")
@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class    AuthorController {

    private final AuthorService authorService;
    private final MinIoService minIoService;

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

    // Tạo tác giả mới với avatar (multipart/form-data)
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthorWithAvatar(
            @RequestParam String name,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile avatar
    ) {
        // Upload avatar nếu có
        String avatarUrl = null;
        if (avatar != null && !avatar.isEmpty()) {
            avatarUrl = minIoService.uploadFile(avatar, "avatars");
        }

        // Tạo request DTO
        AuthorRequest request = new AuthorRequest();
        request.setName(name);
        request.setBio(bio);
        request.setAvatar(avatarUrl);

        AuthorResponse author = authorService.createAuthor(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tác giả thành công", author));
    }

    // Tạo tác giả mới (JSON - giữ lại endpoint cũ)
    @PostMapping(consumes = {"application/json"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthor(
            @Valid @RequestBody AuthorRequest request
    ) {
        AuthorResponse author = authorService.createAuthor(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tác giả thành công", author));
    }

    // Cập nhật tác giả với avatar (multipart/form-data)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthorWithAvatar(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile avatar
    ) {
        // Upload avatar mới nếu có
        String avatarUrl = null;
        if (avatar != null && !avatar.isEmpty()) {
            avatarUrl = minIoService.uploadFile(avatar, "avatars");
        }

        AuthorRequest request = new AuthorRequest();
        request.setName(name);
        request.setBio(bio);
        request.setAvatar(avatarUrl);

        AuthorResponse author = authorService.updateAuthor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tác giả thành công", author));
    }

    // Cập nhật tác giả (JSON - giữ lại endpoint cũ)
    @PutMapping(value = "/{id}", consumes = {"application/json"})
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

    // Tìm kiếm tác giả theo tên
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> searchAuthors(
            @RequestParam(required = false) String name
    ) {
        List<AuthorResponse> authors = authorService.searchAuthors(name);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm tác giả thành công", authors));
    }
}