package com.example.truyen.service;

import com.example.truyen.dto.request.ChangeRoleRequest;
import com.example.truyen.dto.request.CreateUserRequest;
import com.example.truyen.dto.request.UpdateUserRequest;
import com.example.truyen.dto.response.UserResponse;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Lấy tất cả users
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToResponse);
    }

    // Lấy chi tiết user theo ID
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return convertToResponse(user);
    }

    // Lấy thông tin cuar user hiện tại
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User currentUser = getCurrentUserEntity();
        return convertToResponse(currentUser);
    }


    // Cập nhật thông tin user - Method này thay thế cho method updateUser hiện tại
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Kiểm tra quyền
        User currentUser = getCurrentUserEntity();
        if (!currentUser.getId().equals(id) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN) &&
                !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Bạn không có quyền cập nhật thông tin user này");
        }

        // Cập nhật fullname
        if (request.getFullname() != null) {
            user.setFullname(request.getFullname());
        }

        // Cập nhật email
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email đã tồn tại");
            }
            user.setEmail(request.getEmail());
        }

        // Cập nhật avatar
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        // Cập nhật phone
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        // Cập nhật password nếu có
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Chỉ SUPER_ADMIN và ADMIN mới có thể kích hoạt/vô hiệu hóa tài khoản
        if (request.getIsActive() != null &&
                (currentUser.getRole().equals(User.Role.SUPER_ADMIN) ||
                        currentUser.getRole().equals(User.Role.ADMIN))) {
            user.setIsActive(request.getIsActive());
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }

    // Thay đổi role của user (CHỈ SUPER_ADMIN)
    @Transactional
    public UserResponse changeUserRole(ChangeRoleRequest request) {
        User currentUser = getCurrentUserEntity();

        // Kiểm tra quyền SUPER_ADMIN
        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Chỉ SUPER_ADMIN mới có quyền thay đổi role");
        }

        // Tìm user cần thay đổi role
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        // Không được thay đổi role của chính mình
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Không thể thay đổi role của chính mình");
        }

        // Không được thay đổi role của SUPER_ADMIN khác
        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Không thể thay đổi role của SUPER_ADMIN");
        }

        // Parse và validate role
        User.Role newRole;
        try {
            newRole = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Role không hợp lệ: " + request.getRole());
        }

        // Không cho phép tạo SUPER_ADMIN mới
        if (newRole.equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Không thể gán role SUPER_ADMIN");
        }

        // Thay đổi role
        targetUser.setRole(newRole);
        User updatedUser = userRepository.save(targetUser);

        return convertToResponse(updatedUser);
    }

    // Ban, Unban tài khoản (CHỈ SUPER_ADMIN và ADMIN) (2 trạng thái riêng biệt)
    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        User currentUser = getCurrentUserEntity();

        // Kiểm tra quyền SUPER_ADMIN và ADMIN
        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Chỉ SUPER_ADMIN và ADMIN mới có quyền thay đổi trạng thái tài khoản");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Không được ban chính mình
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Không thể thay đổi trạng thái của chính mình");
        }

        // Không được ban SUPER_ADMIN khác
        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Không thể thay đổi trạng thái của SUPER_ADMIN");
        }
        //Không được ban ADMIN khác
        if (currentUser.getRole().equals(User.Role.ADMIN) &&
                targetUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("ADMIN không thể ban ADMIN khác");
        }

        targetUser.setIsActive(!targetUser.getIsActive());
        User updatedUser = userRepository.save(targetUser);

        return convertToResponse(updatedUser);
    }

    // Xóa user (CHỈ SUPER_ADMIN và ADMIN)
    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = getCurrentUserEntity();

        // Kiểm tra quyền SUPER_ADMIN và ADMIN
        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Chỉ SUPER_ADMIN và ADMIN mới có quyền xóa user");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Không được xóa chính mình
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Không thể xóa chính mình");
        }

        // Không được xóa SUPER_ADMIN khác
        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Không thể xóa SUPER_ADMIN");
        }
        // Không được xóa ADMIN khác
        if (currentUser.getRole().equals(User.Role.ADMIN) &&
                targetUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("ADMIN không thể xóa ADMIN khác");
        }
        userRepository.delete(targetUser);
    }

    // Đếm số lượng users theo role
    @Transactional(readOnly = true)
    public long countUsersByRole(String role) {
        try {
            User.Role userRole = User.Role.valueOf(role.toUpperCase());
            return userRepository.findAll().stream()
                    .filter(u -> u.getRole().equals(userRole))
                    .count();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Role không hợp lệ: " + role);
        }
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã tồn tại");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullname(request.getFullname())
                .phone(request.getPhone())
                .role(User.Role.USER)
                .isActive(true)
                .build();
        return mapToResponse(userRepository.save(user));
    }

    //getCurrentUserEntity
    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullname(user.getFullname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullname(user.getFullname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}