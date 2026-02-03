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
     * Retrieve all users with pagination.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size)).map(this::convertToResponse);
    }

    /**
     * Retrieve user details by ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return convertToResponse(user);
    }

    /**
     * Retrieve profile of the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return convertToResponse(getCurrentUserEntity());
    }

    /**
     * Update user details. Access restricted to the user themselves or
     * administrators.
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        User currentUser = getCurrentUserEntity();
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

        // Only SUPER_ADMIN and ADMIN can enable/disable accounts
        if (request.getIsActive() != null &&
                (currentUser.getRole().equals(User.Role.SUPER_ADMIN) ||
                        currentUser.getRole().equals(User.Role.ADMIN))) {
            user.setIsActive(request.getIsActive());
        }

        return convertToResponse(userRepository.save(user));
    }

    /**
     * Change user role. Restricted to SUPER_ADMIN.
     */
    @Transactional
    public UserResponse changeUserRole(ChangeRoleRequest request) {
        User currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Only SUPER_ADMIN has permission to change roles");
        }

        User targetUser = userRepository.findById(request.getUserId())
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

    /**
     * Toggle user active status (Ban/Unban). Restricted to SUPER_ADMIN and ADMIN.
     */
    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        User currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Only SUPER_ADMIN and ADMIN have permission to change account status");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("You cannot change your own status");
        }

        if (targetUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new BadRequestException("Cannot change the status of a SUPER_ADMIN");
        }

        if (currentUser.getRole().equals(User.Role.ADMIN) &&
                targetUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("ADMIN cannot ban another ADMIN");
        }

        targetUser.setIsActive(!targetUser.getIsActive());
        return convertToResponse(userRepository.save(targetUser));
    }

    /**
     * Delete a user. Restricted to SUPER_ADMIN and ADMIN.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = getCurrentUserEntity();

        if (!currentUser.getRole().equals(User.Role.SUPER_ADMIN) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new BadRequestException("Only SUPER_ADMIN and ADMIN have permission to delete users");
        }

        User targetUser = userRepository.findById(userId)
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

    /**
     * Count users belonging to a specific role.
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
     * Create a new user with default USER role.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
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
     * Retrieve the current authenticated user entity.
     */
    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Search users by keyword with pagination and sorting.
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
     * Map User entity to UserResponse DTO.
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
