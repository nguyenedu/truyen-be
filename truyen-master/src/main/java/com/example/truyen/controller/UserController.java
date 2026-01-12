package com.example.truyen.controller;


import com.example.truyen.dto.request.ChangeRoleRequest;
import com.example.truyen.dto.request.UpdateUserRequest;
import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.UserResponse;
import com.example.truyen.entity.User;
import com.example.truyen.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //Lấy danh sách thông tin của tất cả User(Chỉ ADMIN và SUPER_ADMIN)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<UserResponse> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", users));
    }

    //Lấy chi tiết thông tin của 1 User theo ID (chỉ ADMIN và SUPER_ADMIN)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    //Lấy thông tin của User hiện tại
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", user));
    }

    //Cập nhật thông tin của User
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin người dùng thành công", user));
    }

    //Thay đổi role của User (chỉ SUPER_ADMIN)
    @PutMapping("/change-role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        UserResponse user = userService.changeUserRole(request);
        return ResponseEntity.ok(ApiResponse.success("Thay đổi vai trò người dùng thành công", user));
    }

    //Ban, Unban User (Chỉ ADMIN, SUPER_ADMIN) (ADMIN được quyền ban user, SUPER_ADMIN được quyền làm hết)
    @PutMapping({"/{id}/toggle-status"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
            UserResponse user = userService.toggleUserStatus(id);
            String statusMessage = user.getIsActive() ? "Ban" : "UnBan";
            return ResponseEntity.ok(ApiResponse.success(statusMessage +"tài khoản thành công", user));
    }

    //Xóa User (Chỉ ADMIN, SUPER_ADMIN) (SUPER ADMIN xóa được mọi thứ, ADMIN chỉ được quyền xóa user)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa User thành công", null));
    }

    //Thống kê số lượng user theo role
    @GetMapping("/count-by-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countUserByRole(@PathVariable String role) {
        Long count = userService.countUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success("Đếm User thành công", count));
    }

}
