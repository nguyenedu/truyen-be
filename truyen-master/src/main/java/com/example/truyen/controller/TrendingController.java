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
@CrossOrigin(origins = { "http://localhost:5174", "http://localhost:5175", "http://localhost:5176" })
public class TrendingController {

    private final TrendingService trendingService;
    private final StoryViewService viewService;
    private final StoryRepository storyRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;

    // Lấy danh sách truyện thịnh hành theo ngày, tuần, tháng
    @GetMapping
    public ResponseEntity<List<StoryTrendingDTO>> getTrending(
            @RequestParam(value = "t", required = false) String tParam,
            @RequestParam(value = "type", required = false) String typeParam,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            var typeString = tParam != null ? tParam : typeParam;

            if (typeString == null || typeString.isEmpty()) {
                typeString = "DAILY";
            }

            // Standardize type string
            typeString = typeString.replace("e-", "").toUpperCase();

            Ranking.RankingType type;
            try {
                type = Ranking.RankingType.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid ranking type: {}, defaulting to DAILY", typeString);
                type = Ranking.RankingType.DAILY;
            }

            log.info("Fetching trending: count={}, type={}", limit, type);
            return ResponseEntity.ok(trendingService.getTrending(type, limit));

        } catch (Exception e) {
            log.error("Failed to fetch trending stories", e);
            return ResponseEntity.ok(List.of());
        }
    }

    // Ghi nhận lượt xem
    @PostMapping("/stories/{id}/view")
    public ResponseEntity<String> trackViewSimple(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {

        try {
            viewService.trackView(id, userId, request.getRemoteAddr());
            return ResponseEntity.ok("View tracked");
        } catch (Exception e) {
            log.error("Failed to track view for story: {}", id, e);
            return ResponseEntity.ok("Tracking failed");
        }
    }

    // Ghi nhận lượt xem với DTO
    @PostMapping("/track")
    public ResponseEntity<String> trackView(
            @Valid @RequestBody TrackViewRequest requestDto,
            HttpServletRequest request) {

        try {
            viewService.trackView(requestDto.getStoryId(), requestDto.getUserId(), request.getRemoteAddr());
            return ResponseEntity.ok("View tracked");
        } catch (Exception e) {
            log.error("Failed to track view", e);
            return ResponseEntity.ok("Tracking failed");
        }
    }

    // Kích hoạt làm mới bảng xếp hạng thủ công
    @PostMapping("/refresh")
    public ResponseEntity<String> manualRefresh(
            @RequestParam(defaultValue = "DAILY") Ranking.RankingType type) {

        try {
            trendingService.manualRefresh(type);
            return ResponseEntity.ok("Trending refreshed: " + type);
        } catch (Exception e) {
            log.error("Failed to refresh trending: {}", type, e);
            return ResponseEntity.status(500).body("Refresh failed: " + e.getMessage());
        }
    }

    @GetMapping("/stats/{storyId}")
    public ResponseEntity<?> getStoryStats(@PathVariable Long storyId) {

        try {
            var story = storyRepository.findById(storyId)
                    .orElseThrow(() -> new RuntimeException("Story not found"));

            var stats = StoryStatsResponse.builder()
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
            log.error("Failed to fetch stats for story: {}", storyId, e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
