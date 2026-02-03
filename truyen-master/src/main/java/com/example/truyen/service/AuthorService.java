package com.example.truyen.service;

import com.example.truyen.dto.request.AuthorRequest;
import com.example.truyen.dto.response.AuthorResponse;
import com.example.truyen.entity.Author;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    /**
     * Lấy danh sách tất cả tác giả.
     */
    @Transactional(readOnly = true)
    public List<AuthorResponse> getAllAuthors() {
        List<Author> authors = authorRepository.findAll();
        return authors.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết tác giả theo ID.
     */
    @Transactional(readOnly = true)
    public AuthorResponse getAuthorById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "id", id));
        return convertToResponse(author);
    }

    /**
     * Tạo tác giả mới.
     */
    @Transactional
    public AuthorResponse createAuthor(AuthorRequest request) {
        if (authorRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tác giả '" + request.getName() + "' đã tồn tại");
        }

        Author author = Author.builder()
                .name(request.getName())
                .bio(request.getBio())
                .avatar(request.getAvatar())
                .build();

        Author savedAuthor = authorRepository.save(author);
        return convertToResponse(savedAuthor);
    }

    /**
     * Cập nhật thông tin tác giả hiện có.
     */
    @Transactional
    public AuthorResponse updateAuthor(Long id, AuthorRequest request) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "id", id));

        if (request.getName() != null && !author.getName().equals(request.getName())) {
            if (authorRepository.existsByName(request.getName())) {
                throw new BadRequestException("Tác giả '" + request.getName() + "' đã tồn tại");
            }
            author.setName(request.getName());
        }

        if (request.getBio() != null) {
            author.setBio(request.getBio());
        }

        if (request.getAvatar() != null) {
            author.setAvatar(request.getAvatar());
        }

        Author updatedAuthor = authorRepository.save(author);
        return convertToResponse(updatedAuthor);
    }

    /**
     * Xóa tác giả theo ID.
     */
    @Transactional
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author", "id", id));
        authorRepository.delete(author);
    }

    /**
     * Tìm kiếm tác giả theo tên có chứa từ khóa.
     */
    @Transactional(readOnly = true)
    public List<AuthorResponse> searchAuthors(String name) {
        List<Author> authors;

        if (name == null || name.trim().isEmpty()) {
            authors = authorRepository.findAll();
        } else {
            authors = authorRepository.findByNameContainingIgnoreCase(name.trim());
        }

        return authors.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi Author sang AuthorResponse DTO.
     */
    private AuthorResponse convertToResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .bio(author.getBio())
                .avatar(author.getAvatar())
                .createdAt(author.getCreatedAt())
                .build();
    }
}