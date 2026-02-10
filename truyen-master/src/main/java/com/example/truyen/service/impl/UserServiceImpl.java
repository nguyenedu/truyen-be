package com.example.truyen.service.impl;

import com.example.truyen.dto.request.ChangeRoleRequest;
import com.example.truyen.service.MinIoService;
import org.springframework.web.multipart.MultipartFile;
import com.example.truyen.dto.request.CreateUserRequest;
import com.example.truyen.dto.request.UpdateUserRequest;
import com.example.truyen.dto.response.UserResponse;
import com.example.truyen.entity.User;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.UserRepository;
import com.example.truyen.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinIoService minIoService;

    // Lấy danh sách tất cả người dùng
    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    // Lấy thông tin người dùng theo ID
    @Transactional(readOnly = true)
    @Override
    public UserResponse getUserById(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return convertToResponse(user);
    }

    // Lấy thông tin người dùng hiện tại
    @Transactional(readOnly = true)
    @Override
    public UserResponse getCurrentUser() {
        return convertToResponse(getCurrentUserEntity());
    }

    // Cập nhật thông tin người dùng
    @Transactional
    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        var currentUser = getCurrentUserEntity();
        if (!currentUser.getId().equals(id) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN) &&
                !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("You do not have permission to update this user's information");
        }

        if (request.getFullname() != null) {
            user.setFullname(request.getFullname());
        }

        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists");
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

    // Thay đổi quyền người dùng (Chỉ SUPER_ADMIN)
    @Transactional
    @Override
    public UserResponse changeUserRole(ChangeRoleRequest request) {
        var currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Only SUPER_ADMIN can change roles");
        }

        var targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot change your own role");
        }

        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Cannot change the role of another SUPER_ADMIN");
        }

        User.Role newRole;
        try {
            newRole = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + request.getRole());
        }

        if (newRole.equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Cannot assign SUPER_ADMIN role");
        }

        targetUser.setRole(newRole);
        return convertToResponse(userRepository.save(targetUser));
    }

    @Transactional
    @Override
    public UserResponse toggleUserStatus(Long userId) {
        var currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Only SUPER_ADMIN and ADMIN can change account status");
        }

        var targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot change your own status");
        }

        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Cannot change the status of a SUPER_ADMIN");
        }

        if (currentUser.getRole().equals(User.Role.ADMIN) &&
                targetUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("ADMIN cannot block another ADMIN");
        }

        targetUser.setIsActive(!targetUser.getIsActive());
        return convertToResponse(userRepository.save(targetUser));
    }

    @Transactional
    @Override
    public void deleteUser(Long userId) {
        var currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Only SUPER_ADMIN and ADMIN can delete users");
        }

        var targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot delete yourself");
        }

        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Cannot delete a SUPER_ADMIN");
        }

        if (currentUser.getRole().equals(User.Role.ADMIN) &&
                targetUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("ADMIN cannot delete another ADMIN");
        }
        userRepository.delete(targetUser);
    }

    @Transactional(readOnly = true)
    @Override
    public long countUsersByRole(String role) {
        try {
            var userRole = User.Role.valueOf(role.toUpperCase());
            return userRepository.findAll().stream()
                    .filter(u -> u.getRole().equals(userRole))
                    .count();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }
    }

    @Transactional
    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        var user = User.builder()
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

    @Override
    public User getCurrentUserEntity() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> searchUsers(String keyword, int page, int size, String sortField, String sortDir) {
        var direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable).map(this::convertToResponse);
        }

        return userRepository.searchUsers(keyword.trim(), pageable).map(this::convertToResponse);
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

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, MultipartFile avatar) {
        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = minIoService.uploadFile(avatar, "avatars");
            request.setAvatar(avatarUrl);
        }
        return updateUser(id, request);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, MultipartFile avatar) {
        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = minIoService.uploadFile(avatar, "avatars");
            request.setAvatar(avatarUrl);
        }
        return createUser(request);
    }
}
