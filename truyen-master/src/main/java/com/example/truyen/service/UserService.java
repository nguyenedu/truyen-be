package com.example.truyen.service;

import com.example.truyen.dto.request.ChangeRoleRequest;
import com.example.truyen.dto.request.CreateUserRequest;
import com.example.truyen.dto.request.UpdateUserRequest;
import com.example.truyen.dto.response.UserResponse;
import com.example.truyen.entity.User;
import org.springframework.data.domain.Page;

// Interface UserService
public interface UserService {

    // Lấy danh sách tất cả người dùng
    Page<UserResponse> getAllUsers(int page, int size);

    // Lấy thông tin người dùng theo ID
    UserResponse getUserById(Long id);

    // Lấy thông tin người dùng hiện tại
    UserResponse getCurrentUser();

    // Cập nhật thông tin người dùng
    UserResponse updateUser(Long id, UpdateUserRequest request);

    // Thay đổi quyền người dùng (Chỉ SUPER_ADMIN)
    UserResponse changeUserRole(ChangeRoleRequest request);

    // Bật/tắt trạng thái hoạt động người dùng
    UserResponse toggleUserStatus(Long userId);

    // Xóa người dùng
    void deleteUser(Long userId);

    // Đếm người dùng theo vai trò
    long countUsersByRole(String role);

    // Tạo người dùng mới
    UserResponse createUser(CreateUserRequest request);

    // Lấy User entity hiện tại
    User getCurrentUserEntity();

    // Tìm kiếm người dùng
    Page<UserResponse> searchUsers(String keyword, int page, int size, String sortField, String sortDir);
}
