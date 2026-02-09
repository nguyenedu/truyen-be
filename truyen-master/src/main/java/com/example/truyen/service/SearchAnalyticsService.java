package com.example.truyen.service;

import com.example.truyen.config.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle search analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Gợi ý từ khóa: kết hợp lịch sử, xu hướng và phổ biến
    public List<String> getAutoSuggest(String prefix, Long userId, int limit) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedPrefix = prefix.toLowerCase().trim();
        Set<String> suggestions = new LinkedHashSet<>();

        // 1. Get from user history (if any) - highest priority
        if (userId != null) {
            Set<String> userHistory = getUserSearchHistory(userId, 10);
            userHistory.stream()
                    .filter(q -> q.startsWith(normalizedPrefix))
                    .limit(3)
                    .forEach(suggestions::add);
        }

        // 2. Get from trending searches (today)
        Set<String> trending = getTrendingSearches(5);
        trending.stream()
                .filter(q -> q.startsWith(normalizedPrefix))
                .limit(2)
                .forEach(suggestions::add);

        // 3. Get from popular searches (all time)
        Set<String> popular = getPopularSearches(20);
        popular.stream()
                .filter(q -> q.startsWith(normalizedPrefix))
                .limit(limit)
                .forEach(suggestions::add);

        return suggestions.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Lấy từ khóa phổ biến (toàn thời gian)
    public Set<String> getPopularSearches(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<Object>> results = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(RedisKeyConstants.SEARCH_POPULAR, 0, limit - 1);

            if (results == null) {
                return Collections.emptySet();
            }

            return results.stream()
                    .map(tuple -> (String) tuple.getValue())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        } catch (Exception e) {
            log.error("Error getting popular searches: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    // Lấy từ khóa xu hướng (trong ngày)
    public Set<String> getTrendingSearches(int limit) {
        try {
            String key = RedisKeyConstants.SEARCH_TRENDING + LocalDate.now();
            Set<ZSetOperations.TypedTuple<Object>> results = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, 0, limit - 1);

            if (results == null) {
                return Collections.emptySet();
            }

            return results.stream()
                    .map(tuple -> (String) tuple.getValue())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        } catch (Exception e) {
            log.error("Error getting trending searches: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    // Lấy lịch sử tìm kiếm của người dùng
    public Set<String> getUserSearchHistory(Long userId, int limit) {
        try {
            String key = RedisKeyConstants.SEARCH_USER_HISTORY + userId;
            Set<Object> results = redisTemplate.opsForZSet()
                    .reverseRange(key, 0, limit - 1);

            if (results == null) {
                return Collections.emptySet();
            }

            return results.stream()
                    .map(obj -> (String) obj)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        } catch (Exception e) {
            log.error("Error getting user search history: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    // Lấy từ khóa phổ biến kèm điểm số
    public Map<String, Double> getPopularSearchesWithScores(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<Object>> results = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(RedisKeyConstants.SEARCH_POPULAR, 0, limit - 1);

            if (results == null) {
                return Collections.emptyMap();
            }

            Map<String, Double> searchesWithScores = new LinkedHashMap<>();
            results.forEach(tuple -> {
                String query = (String) tuple.getValue();
                Double score = tuple.getScore();
                if (query != null && score != null) {
                    searchesWithScores.put(query, score);
                }
            });

            return searchesWithScores;

        } catch (Exception e) {
            log.error("Error getting popular searches with scores: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
