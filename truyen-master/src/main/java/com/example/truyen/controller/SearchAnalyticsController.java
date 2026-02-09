package com.example.truyen.controller;

import com.example.truyen.service.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for Search Analytics APIs
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchAnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    // Gợi ý từ khóa tìm kiếm (Auto-suggest)
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getAutoSuggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {

        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            // Get userId from authentication if needed
            // userId = ((User) authentication.getPrincipal()).getId();
        }

        List<String> suggestions = searchAnalyticsService.getAutoSuggest(q, userId, limit);
        return ResponseEntity.ok(suggestions);
    }

    // Lấy từ khóa tìm kiếm xu hướng
    @GetMapping("/trending")
    public ResponseEntity<Set<String>> getTrendingSearches(
            @RequestParam(defaultValue = "10") int limit) {

        Set<String> trending = searchAnalyticsService.getTrendingSearches(limit);
        return ResponseEntity.ok(trending);
    }

    // Lấy từ khóa tìm kiếm phổ biến
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularSearches(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") boolean withScores) {

        Map<String, Object> response = new HashMap<>();

        if (withScores) {
            Map<String, Double> searchesWithScores = searchAnalyticsService.getPopularSearchesWithScores(limit);
            response.put("searches", searchesWithScores);
        } else {
            Set<String> searches = searchAnalyticsService.getPopularSearches(limit);
            response.put("searches", searches);
        }

        return ResponseEntity.ok(response);
    }

    // Lấy lịch sử tìm kiếm của người dùng
    @GetMapping("/history")
    public ResponseEntity<Set<String>> getUserSearchHistory(
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Set.of());
        }

        // Get userId from authentication
        // Long userId = ((User) authentication.getPrincipal()).getId();
        Long userId = 1L; // Temporary, need to get from authentication

        Set<String> history = searchAnalyticsService.getUserSearchHistory(userId, limit);
        return ResponseEntity.ok(history);
    }
}
