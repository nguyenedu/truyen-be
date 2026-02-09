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
 * Controller cho Search Analytics APIs
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchAnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    /**
     * Auto-suggest API
     * GET /api/search/suggest?q=truyen
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getAutoSuggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {

        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            // Lấy userId từ authentication nếu cần
            // userId = ((User) authentication.getPrincipal()).getId();
        }

        List<String> suggestions = searchAnalyticsService.getAutoSuggest(q, userId, limit);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Trending searches API
     * GET /api/search/trending?limit=10
     */
    @GetMapping("/trending")
    public ResponseEntity<Set<String>> getTrendingSearches(
            @RequestParam(defaultValue = "10") int limit) {

        Set<String> trending = searchAnalyticsService.getTrendingSearches(limit);
        return ResponseEntity.ok(trending);
    }

    /**
     * Popular searches API
     * GET /api/search/popular?limit=10
     */
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

    /**
     * User search history API
     * GET /api/search/history
     */
    @GetMapping("/history")
    public ResponseEntity<Set<String>> getUserSearchHistory(
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Set.of());
        }

        // Lấy userId từ authentication
        // Long userId = ((User) authentication.getPrincipal()).getId();
        Long userId = 1L; // Temporary, cần lấy từ authentication

        Set<String> history = searchAnalyticsService.getUserSearchHistory(userId, limit);
        return ResponseEntity.ok(history);
    }
}
