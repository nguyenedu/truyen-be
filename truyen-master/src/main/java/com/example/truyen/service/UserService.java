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
import org.springframework.data.domain.Sort;
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

    /**
     * Lấy danh sách tất cả người dùng với phân trang.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    /**
     * Lấy chi tiết người dùng theo ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return convertToResponse(user);
    }

    /**
     * Lấy hồ sơ của người dùng hiện đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return convertToResponse(getCurrentUserEntity());
    }

    /**
     * Cập nhật chi tiết người dùng. Chỉ chính người dùng đó
     * hoặc quản trị viên.
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        User currentUser = getCurrentUserEntity();
        if (!currentUser.getId().equals(id) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN) &&
                !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Bạn không có quyền cập nhật thông tin của người dùng này");
        }

        if (request.getFullname() != null) {
            user.setFullname(request.getFullname());
        }

        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email đã tồn tại");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getIsActive() != null &&
                (currentUser.getRole().equals(User.Role.SUPER_ADMIN) ||
                        currentUser.getRole().equals(User.Role.ADMIN))) {
            user.setIsActive(request.getIsActive());
        }

        return convertToResponse(userRepository.save(user));
    }

    /**
     * Thay đổi quyền người dùng. Chỉ với quyền SUPER_ADMIN.
     */
    @Transactional
    public UserResponse changeUserRole(ChangeRoleRequest request) {
        User currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Chỉ SUPER_ADMIN mới có quyền thay đổi vai trò");
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không thể tự thay đổi vai trò của mình");
        }

        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Không thể thay đổi vai trò của một SUPER_ADMIN khác");
        }

        User.Role newRole;
        try {
            newRole = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Vai trò không hợp lệ: " + request.getRole());
        }

        if (newRole.equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Không thể gán vai trò SUPER_ADMIN");
        }

        targetUser.setRole(newRole);
        return convertToResponse(userRepository.save(targetUser));
    }

    /**
     * Bật/Tắt trạng thái hoạt động của người dùng (Chặn/Bỏ chặn). Chỉ
     * với quyền SUPER_ADMIN và ADMIN.
     */
    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        User currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Chỉ SUPER_ADMIN và ADMIN mới có quyền thay đổi trạng thái tài khoản");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không thể tự thay đổi trạng thái của mình");
        }

        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Không thể thay đổi trạng thái của một SUPER_ADMIN");
        }

        if (currentUser.getRole().equals(User.Role.ADMIN) &&
                targetUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("ADMIN không thể chặn một ADMIN khác");
        }

        targetUser.setIsActive(!targetUser.getIsActive());
        return convertToResponse(userRepository.save(targetUser));
    }

    /**
     * Xóa một người dùng. Chỉ với quyền SUPER_ADMIN và ADMIN.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Chỉ SUPER_ADMIN và ADMIN mới có quyền xóa người dùng");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("Bạn không thể tự xóa chính mình");
        }

        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Không thể xóa một SUPER_ADMIN");
        }

        if (currentUser.getRole().equals(User.Role.ADMIN) &&
                targetUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("ADMIN không thể xóa một ADMIN khác");
        }
        userRepository.delete(targetUser);
    }

    /**
     * Đếm số lượng người dùng thuộc một vai trò cụ thể.
     */
    @Transactional(readOnly = true)
    public long countUsersByRole(String role) {
        try {
            User.Role userRole = User.Role.valueOf(role.toUpperCase());
            return userRepository.findAll().stream()
                    .filter(u -> u.getRole().equals(userRole))
                    .count();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }
    }

    /**
     * Tạo người dùng mới với vai trò USER mặc định.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
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
        return convertToResponse(userRepository.save(user));
    }

    /**
     * Lấy người dùng hiện đang đăng nhập.
     */
    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
    }

    /**
     * Tìm kiếm người dùng theo từ khóa với phân trang và sắp xếp.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, int page, int size, String sortField, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable).map(this::convertToResponse);
        }

        return userRepository.searchUsers(keyword.trim(), pageable).map(this::convertToResponse);
    }

    /**
     * Chuyển đổi User sang UserResponse DTO.
     */
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
}
