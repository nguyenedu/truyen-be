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

    // Lấy danh sách tất cả người dùng (Admin, Super Admin)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return ResponseEntity
                .ok(ApiResponse.success("Get all users successfully", userService.getAllUsers(page, size)));
    }

    // Lấy thông tin người dùng theo ID (Admin, Super Admin)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Get user details successfully", userService.getUserById(id)));
    }

    // Lấy thông tin người dùng hiện tại
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity
                .ok(ApiResponse.success("Get current user successfully", userService.getCurrentUser()));
    }

    // Cập nhật thông tin người dùng với avater
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

        var request = new UpdateUserRequest();
        request.setFullname(fullname);
        request.setEmail(email);
        request.setPhone(phone);
        request.setPassword(password);
        request.setAvatar(avatarUrl);
        request.setIsActive(isActive);

        return ResponseEntity.ok(
                ApiResponse.success("Update user info successfully", userService.updateUser(id, request)));
    }

    // Cập nhật thông tin người dùng với dữ liệu JSON
    @PutMapping(value = "/{id}", consumes = { "application/json" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Update user successfully", userService.updateUser(id, request)));
    }

    // Thay đổi quyền người dùng
    @PutMapping("/change-role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(@Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Change user role successfully", userService.changeUserRole(request)));
    }

    // Khóa/Mở khóa tài khoản người dùng
    @PutMapping({ "/{id}/toggle-status" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
        var user = userService.toggleUserStatus(id);
        var action = user.getIsActive() ? "Activated" : "Deactivated";
        return ResponseEntity.ok(ApiResponse.success(action + " user successfully", user));
    }

    // Xóa người dùng (Admin, Super Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Delete user successfully", null));
    }

    // Đếm số lượng người dùng theo vai trò (Admin, Super Admin)
    @GetMapping("/count-by-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countUserByRole(@PathVariable String role) {
        return ResponseEntity
                .ok(ApiResponse.success("Count users successfully", userService.countUsersByRole(role)));
    }

    // Tạo người dùng mới kèm avatar (Admin, Super Admin)
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

        var request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        request.setFullname(fullname);
        request.setPhone(phone);
        request.setAvatar(avatarUrl);
        request.setRole(role);

        return ApiResponse.success("Create user successfully", userService.createUser(request));
    }

    // Tạo người dùng mới với dữ liệu JSON (Admin, Super Admin)
    @PostMapping(consumes = { "application/json" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ApiResponse.success("Create user successfully", userService.createUser(request));
    }

    // Tìm kiếm người dùng (Admin, Super Admin)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success("Search users successfully",
                userService.searchUsers(keyword, page, size, sortField, sortDir)));
    }
}
