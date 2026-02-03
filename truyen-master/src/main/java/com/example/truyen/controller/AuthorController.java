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
     * Get all authors.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> getAllAuthors() {
        return ResponseEntity.ok(ApiResponse.success("Authors retrieved successfully", authorService.getAllAuthors()));
    }

    /**
     * Get author details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthorResponse>> getAuthorById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Author retrieved successfully", authorService.getAuthorById(id)));
    }

    /**
     * Create author with avatar (Multipart).
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
                .body(ApiResponse.success("Author created successfully", authorService.createAuthor(request)));
    }

    /**
     * Create author (JSON).
     */
    @PostMapping(consumes = { "application/json" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> createAuthor(@Valid @RequestBody AuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Author created successfully", authorService.createAuthor(request)));
    }

    /**
     * Update author with avatar (Multipart).
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
                .ok(ApiResponse.success("Author updated successfully", authorService.updateAuthor(id, request)));
    }

    /**
     * Update author (JSON).
     */
    @PutMapping(value = "/{id}", consumes = { "application/json" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthorResponse>> updateAuthor(
            @PathVariable Long id,
            @Valid @RequestBody AuthorRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Author updated successfully", authorService.updateAuthor(id, request)));
    }

    /**
     * Delete an author.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAuthor(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok(ApiResponse.success("Author deleted successfully", null));
    }

    /**
     * Search authors by name.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AuthorResponse>>> searchAuthors(
            @RequestParam(required = false) String name) {
        return ResponseEntity
                .ok(ApiResponse.success("Search completed successfully", authorService.searchAuthors(name)));
    }
}