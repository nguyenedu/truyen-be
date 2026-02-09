package com.example.truyen.controller;

import com.example.truyen.service.MinIoService;
import com.example.truyen.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("api/files")
@RequiredArgsConstructor
public class UploadFileController {

    private final MinIoService minIoService;

    // API upload ảnh
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "images") String folder) {
        String url = minIoService.uploadFile(file, folder);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
