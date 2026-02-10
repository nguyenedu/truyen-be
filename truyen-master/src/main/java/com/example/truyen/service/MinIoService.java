package com.example.truyen.service;

import org.springframework.web.multipart.MultipartFile;

// Interface MinIoService
public interface MinIoService {

    // Tải file lên MinIO và trả về URL
    String uploadFile(MultipartFile file);

    // Tải file lên MinIO vào thư mục cụ thể và trả về URL
    String uploadFile(MultipartFile file, String folder);
}
