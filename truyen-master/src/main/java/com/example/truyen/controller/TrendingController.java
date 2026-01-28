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
public class TrendingController {

    private final TrendingService trendingService;
    private final StoryViewService viewService;
    private final StoryRepository storyRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;

    @GetMapping
    public ResponseEntity<List<StoryTrendingDTO>> getTrending(
            @RequestParam(defaultValue = "DAILY") Ranking.RankingType type,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("Getting trending: type={}, limit={}", type, limit);

        List<StoryTrendingDTO> trending = trendingService.getTrending(type, limit);

        return ResponseEntity.ok(trending);
    }

    /**
     * POST /api/trending/stories/{id}/view?userId=123
     */
    @PostMapping("/stories/{id}/view")
    public ResponseEntity<String> trackViewSimple(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();
        viewService.trackView(id, userId, ipAddress);

        return ResponseEntity.ok("View tracked successfully");
    }


    @PostMapping("/track")
    public ResponseEntity<String> trackView(
            @Valid @RequestBody TrackViewRequest requestDto,
            HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();
        viewService.trackView(requestDto.getStoryId(), requestDto.getUserId(), ipAddress);

        log.info("Tracked view for story {} by user {}",
                requestDto.getStoryId(), requestDto.getUserId());

        return ResponseEntity.ok("View tracked successfully");
    }

    /**
     * POST /api/trending/refresh?type=DAILY
     * Manual refresh (for admin/testing)
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> manualRefresh(
            @RequestParam(defaultValue = "DAILY") Ranking.RankingType type) {

        log.info("Manual refresh triggered for {}", type);
        trendingService.manualRefresh(type);

        return ResponseEntity.ok("Trending refreshed for " + type);
    }

    /**
     * GET /api/trending/stats/{storyId}
     * Xem stats chi tiết của 1 truyện
     */
    @GetMapping("/stats/{storyId}")
    public ResponseEntity<StoryStatsResponse> getStoryStats(@PathVariable Long storyId) {

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
    }
}