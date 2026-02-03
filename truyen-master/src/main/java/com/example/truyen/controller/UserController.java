package com.example.truyen.controller;

import com.example.truyen.dto.request.CreateUserRequest;
import com.example.truyen.service.MinIoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.truyen.dto.request.ChangeRoleRequest;
import com.example.truyen.dto.request.UpdateUserRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.UserResponse;
import com.example.truyen.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MinIoService minIoService;

    /**
     * Get all users with pagination.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return ResponseEntity
                .ok(ApiResponse.success("Users retrieved successfully", userService.getAllUsers(page, size)));
    }

    /**
     * Get user details by ID.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userService.getUserById(id)));
    }

    /**
     * Get current authenticated user details.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userService.getCurrentUser()));
    }

    /**
     * Update user details with avatar (Multipart).
     */
    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserWithAvatar(
            @PathVariable Long id,
            @RequestParam(required = false) String fullname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) MultipartFile avatar,
            @RequestParam(required = false) Boolean isActive) {

        String avatarUrl = (avatar != null && !avatar.isEmpty()) ? minIoService.uploadFile(avatar, "avatars") : null;

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullname(fullname);
        request.setEmail(email);
        request.setPhone(phone);
        request.setPassword(password);
        request.setAvatar(avatarUrl);
        request.setIsActive(isActive);

        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userService.updateUser(id, request)));
    }

    /**
     * Update user details (JSON).
     */
    @PutMapping(value = "/{id}", consumes = { "application/json" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userService.updateUser(id, request)));
    }

    /**
     * Change user role (Super Admin only).
     */
    @PutMapping("/change-role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(@Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("User role updated successfully", userService.changeUserRole(request)));
    }

    /**
     * Toggle user active status (Ban/Unban).
     */
    @PutMapping({ "/{id}/toggle-status" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
        UserResponse user = userService.toggleUserStatus(id);
        String action = user.getIsActive() ? "Activated" : "Deactivated";
        return ResponseEntity.ok(ApiResponse.success("User " + action + " successfully", user));
    }

    /**
     * Delete a user.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    /**
     * Count users by role.
     */
    @GetMapping("/count-by-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countUserByRole(@PathVariable String role) {
        return ResponseEntity
                .ok(ApiResponse.success("Count retrieved successfully", userService.countUsersByRole(role)));
    }

    /**
     * Create a new user with avatar (Multipart).
     */
    @PostMapping(consumes = { "multipart/form-data" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserResponse> createUserWithAvatar(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String fullname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) MultipartFile avatar,
            @RequestParam(required = false) String role) {

        String avatarUrl = (avatar != null && !avatar.isEmpty()) ? minIoService.uploadFile(avatar, "avatars") : null;

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        request.setFullname(fullname);
        request.setPhone(phone);
        request.setAvatar(avatarUrl);
        request.setRole(role);

        return ApiResponse.success("User created successfully", userService.createUser(request));
    }

    /**
     * Create a new user (JSON).
     */
    @PostMapping(consumes = { "application/json" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created successfully", userService.createUser(request));
    }

    /**
     * Search users by keyword.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully",
                userService.searchUsers(keyword, page, size, sortField, sortDir)));
    }
}
