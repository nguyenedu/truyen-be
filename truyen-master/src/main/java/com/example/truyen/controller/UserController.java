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
     * Lấy danh sách người dùng với phân trang.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy danh sách người dùng thành công", userService.getAllUsers(page, size)));
    }

    /**
     * Lấy chi tiết người dùng theo ID.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin người dùng thành công", userService.getUserById(id)));
    }

    /**
     * Lấy thông tin người dùng hiện tại đang đăng nhập.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin người dùng thành công", userService.getCurrentUser()));
    }

    /**
     * Cập nhật thông tin người dùng với ảnh đại diện (Multipart).
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

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật thông tin người dùng thành công", userService.updateUser(id, request)));
    }

    /**
     * Cập nhật thông tin người dùng (JSON).
     */
    @PutMapping(value = "/{id}", consumes = { "application/json" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật thông tin người dùng thành công", userService.updateUser(id, request)));
    }

    /**
     * Thay đổi quyền người dùng (Chỉ dành cho Super Admin).
     */
    @PutMapping("/change-role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(@Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật quyền người dùng thành công", userService.changeUserRole(request)));
    }

    /**
     * Bật/Tắt trạng thái hoạt động của người dùng (Chặn/Bỏ chặn).
     */
    @PutMapping({ "/{id}/toggle-status" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
        UserResponse user = userService.toggleUserStatus(id);
        String action = user.getIsActive() ? "Đã kích hoạt" : "Đã vô hiệu hóa";
        return ResponseEntity.ok(ApiResponse.success(action + " người dùng thành công", user));
    }

    /**
     * Xóa một người dùng.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công", null));
    }

    /**
     * Đếm số lượng người dùng theo quyền.
     */
    @GetMapping("/count-by-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countUserByRole(@PathVariable String role) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy số lượng thành công", userService.countUsersByRole(role)));
    }

    /**
     * Tạo người dùng mới với ảnh đại diện (Multipart).
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

        return ApiResponse.success("Tạo người dùng thành công", userService.createUser(request));
    }

    /**
     * Tạo người dùng mới (JSON).
     */
    @PostMapping(consumes = { "application/json" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ApiResponse.success("Tạo người dùng thành công", userService.createUser(request));
    }

    /**
     * Tìm kiếm người dùng theo từ khóa.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm hoàn tất",
                userService.searchUsers(keyword, page, size, sortField, sortDir)));
    }
}
