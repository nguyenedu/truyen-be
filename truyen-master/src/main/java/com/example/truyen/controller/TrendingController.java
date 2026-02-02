package com.example.truyen.controller;

import com.example.truyen.dto.request.TrackViewRequest;
import com.example.truyen.dto.response.StoryStatsResponse;
import com.example.truyen.dto.response.StoryTrendingDTO;
import com.example.truyen.entity.Ranking;
import com.example.truyen.entity.Story;
import com.example.truyen.repository.RatingRepository;
import com.example.truyen.repository.FavoriteRepository;
import com.example.truyen.repository.CommentRepository;
import com.example.truyen.repository.StoryRepository;
import com.example.truyen.service.StoryViewService;
import com.example.truyen.service.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/trending")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5174", "http://localhost:5175", "http://localhost:5176"})
public class TrendingController {

    private final TrendingService trendingService;
    private final StoryViewService viewService;
    private final StoryRepository storyRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;

    /**
     * GET /api/trending?t=DAILY&limit=12
     * GET /api/trending?type=DAILY&limit=12
     *
     * Support cả 2 format: 't' và 'type'
     */
    @GetMapping
    public ResponseEntity<List<StoryTrendingDTO>> getTrending(
            @RequestParam(value = "t", required = false) String tParam,
            @RequestParam(value = "type", required = false) String typeParam,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            // Xử lý parameter 't' hoặc 'type'
            String typeString = tParam != null ? tParam : typeParam;

            // Default to DAILY if no param provided
            if (typeString == null || typeString.isEmpty()) {
                typeString = "DAILY";
            }

            // Remove prefix "e-" if exists (from frontend: t=e-WEEKLY)
            typeString = typeString.replace("e-", "").toUpperCase();

            // Parse to enum
            Ranking.RankingType type;
            try {
                type = Ranking.RankingType.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid ranking type: {}, defaulting to DAILY", typeString);
                type = Ranking.RankingType.DAILY;
            }

            log.info("Getting trending: type={}, limit={}", type, limit);

            List<StoryTrendingDTO> trending = trendingService.getTrending(type, limit);

            log.info("Successfully retrieved {} trending stories", trending.size());

            return ResponseEntity.ok(trending);

        } catch (Exception e) {
            log.error("Error getting trending stories", e);
            // Return empty list instead of error to prevent frontend crash
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * POST /api/trending/stories/{id}/view?userId=123
     */
    @PostMapping("/stories/{id}/view")
    public ResponseEntity<String> trackViewSimple(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {

        try {
            String ipAddress = request.getRemoteAddr();
            viewService.trackView(id, userId, ipAddress);
            return ResponseEntity.ok("View tracked successfully");
        } catch (Exception e) {
            log.error("Error tracking view for story {}", id, e);
            return ResponseEntity.ok("View tracking failed");
        }
    }

    @PostMapping("/track")
    public ResponseEntity<String> trackView(
            @Valid @RequestBody TrackViewRequest requestDto,
            HttpServletRequest request) {

        try {
            String ipAddress = request.getRemoteAddr();
            viewService.trackView(requestDto.getStoryId(), requestDto.getUserId(), ipAddress);

            log.info("Tracked view for story {} by user {}",
                    requestDto.getStoryId(), requestDto.getUserId());

            return ResponseEntity.ok("View tracked successfully");
        } catch (Exception e) {
            log.error("Error tracking view", e);
            return ResponseEntity.ok("View tracking failed");
        }
    }

    /**
     * POST /api/trending/refresh?type=DAILY
     * Manual refresh (for admin/testing)
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> manualRefresh(
            @RequestParam(defaultValue = "DAILY") Ranking.RankingType type) {

        try {
            log.info("Manual refresh triggered for {}", type);
            trendingService.manualRefresh(type);
            return ResponseEntity.ok("Trending refreshed for " + type);
        } catch (Exception e) {
            log.error("Error refreshing trending for {}", type, e);
            return ResponseEntity.status(500).body("Refresh failed: " + e.getMessage());
        }
    }

    /**
     * GET /api/trending/stats/{storyId}
     * Xem stats chi tiết của 1 truyện
     */
    @GetMapping("/stats/{storyId}")
    public ResponseEntity<?> getStoryStats(@PathVariable Long storyId) {

        try {
            Story story = storyRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("Story not found"));

            StoryStatsResponse stats = StoryStatsResponse.builder()
                    .storyId(storyId)
                    .title(story.getTitle())
                    .image(story.getImage())
                    .viewsToday(viewService.getViewsToday(storyId))
                    .uniqueViewersToday(viewService.getUniqueViewersToday(storyId))
                    .views7Days(viewService.getRecentViews(storyId, 7))
                    .views30Days(viewService.getRecentViews(storyId, 30))
                    .totalViews(story.getTotalViews())
                    .averageRating(ratingRepository.getAverageRating(storyId))
                    .favoriteCount(favoriteRepository.countByStoryId(storyId))
                    .commentCount(commentRepository.countByStoryId(storyId))
                    .build();

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting stats for story {}", storyId, e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}