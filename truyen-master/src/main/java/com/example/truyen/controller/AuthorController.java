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
public class AuthorController {

    private final AuthorService authorService;
    private final MinIoService minIoService;

    /**
     * Lấy danh sách tất cả tác giả.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> getAllAuthors() {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy danh sách tác giả thành công", authorService.getAllAuthors()));
    }

    /**
     * Lấy chi tiết tác giả theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthorResponse>> getAuthorById(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin tác giả thành công", authorService.getAuthorById(id)));
    }

    /**
     * Tạo tác giả mới với ảnh đại diện (Multipart).
     */
    @PostMapping(consumes = { "multipart/form-data" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthorWithAvatar(
            @RequestParam String name,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile avatar) {
        String avatarUrl = (avatar != null && !avatar.isEmpty()) ? minIoService.uploadFile(avatar, "avatars") : null;

        AuthorRequest request = new AuthorRequest();
        request.setName(name);
        request.setBio(bio);
        request.setAvatar(avatarUrl);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tác giả thành công", authorService.createAuthor(request)));
    }

    /**
     * Tạo tác giả mới (JSON).
     */
    @PostMapping(consumes = { "application/json" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthor(@Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tác giả thành công", authorService.createAuthor(request)));
    }

    /**
     * Cập nhật thông tin tác giả với ảnh đại diện (Multipart).
     */
    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthorWithAvatar(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile avatar) {
        String avatarUrl = (avatar != null && !avatar.isEmpty()) ? minIoService.uploadFile(avatar, "avatars") : null;

        AuthorRequest request = new AuthorRequest();
        request.setName(name);
        request.setBio(bio);
        request.setAvatar(avatarUrl);

        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật tác giả thành công", authorService.updateAuthor(id, request)));
    }

    /**
     * Cập nhật thông tin tác giả (JSON).
     */
    @PutMapping(value = "/{id}", consumes = { "application/json" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthor(
            @PathVariable Long id,
            @Valid @RequestBody AuthorRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật tác giả thành công", authorService.updateAuthor(id, request)));
    }

    /**
     * Xóa một tác giả.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAuthor(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tác giả thành công", null));
    }

    /**
     * Tìm kiếm tác giả theo tên.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> searchAuthors(
            @RequestParam(required = false) String name) {
        return ResponseEntity
                .ok(ApiResponse.success("Tìm kiếm hoàn tất", authorService.searchAuthors(name)));
    }
}