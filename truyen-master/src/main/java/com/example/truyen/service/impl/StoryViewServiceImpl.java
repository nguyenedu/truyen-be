package com.example.truyen.service.impl;

import com.example.truyen.config.RedisKeyConstants;
import com.example.truyen.entity.StoryView;
import com.example.truyen.repository.StoryRepository;
import com.example.truyen.repository.StoryViewRepository;
import com.example.truyen.service.StoryViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryViewServiceImpl implements StoryViewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryViewRepository storyViewRepository;
    private final StoryRepository storyRepository;

    @Override
    public void trackView(Long storyId, String visitorId) {
        String todayKey = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
        String dateKey = RedisKeyConstants.STORY_VIEWS_DATE + LocalDate.now() + ":" + storyId;
        String uniqueKey = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
        String totalKey = RedisKeyConstants.STORY_TOTAL_VIEWS + storyId;

        try {
            // 1. Tăng tổng lượt xem tích lũy trong Redis
            redisTemplate.opsForValue().increment(totalKey);

            // 2. Tăng lượt xem hôm nay (TTL: 1 ngày)
            redisTemplate.opsForValue().increment(todayKey);
            redisTemplate.expire(todayKey, Duration.ofDays(1));

            // 3. Tăng lượt xem theo ngày cụ thể (cho trending, TTL: 35 ngày)
            redisTemplate.opsForValue().increment(dateKey);
            redisTemplate.expire(dateKey, Duration.ofDays(35));

            // 4. Track unique viewer hôm nay (TTL: 1 ngày)
            String visitorKey = visitorId != null ? visitorId : "anonymous_" + System.currentTimeMillis();
            redisTemplate.opsForSet().add(uniqueKey, visitorKey);
            redisTemplate.expire(uniqueKey, Duration.ofDays(1));

            // 5. Lưu bản ghi chi tiết vào DB
            StoryView view = StoryView.builder()
                    .storyId(storyId)
                    .visitorId(visitorId)
                    .viewedAt(LocalDateTime.now())
                    .build();
            storyViewRepository.save(view);

        } catch (Exception e) {
            log.error("Error tracking view for story {}: {}", storyId, e.getMessage());
        }
    }

    @Override
    public void trackView(Long storyId, Long userId, String visitorId) {
        String visitor = userId != null ? "user_" + userId : visitorId;
        trackView(storyId, visitor);
    }

    @Override
    public long getRecentViews(Long storyId, int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            Long count = storyViewRepository.countByStoryIdAndViewedAtAfter(storyId, since);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Error getting recent views for story {}: {}", storyId, e.getMessage());
            return 0L;
        }
    }

    @Override
    public long getUniqueViewersToday(Long storyId) {
        try {
            String key = RedisKeyConstants.STORY_UNIQUE_VIEWERS_TODAY + storyId;
            Long count = redisTemplate.opsForSet().size(key);
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public long getViewsToday(Long storyId) {
        try {
            String key = RedisKeyConstants.STORY_VIEWS_TODAY + storyId;
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Delta sync: chạy mỗi 5 phút.
     * Dùng Redis SCAN để tìm tất cả key story:total:views:* còn tồn tại,
     * tính delta giữa tổng views và số đã sync trước đó, rồi chỉ
     * incrementTotalViews(delta).
     * Không gọi findAll() và không overwrite toàn bộ totalViews.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    @Override
    public void syncAllViewsToDatabase() {
        try {
            String pattern = RedisKeyConstants.STORY_TOTAL_VIEWS + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty())
                return;

            int synced = 0;
            for (String totalKey : keys) {
                try {
                    // Lấy storyId từ key "story:total:views:{storyId}"
                    String storyIdStr = totalKey.substring(RedisKeyConstants.STORY_TOTAL_VIEWS.length());
                    Long storyId = Long.parseLong(storyIdStr);

                    Object totalObj = redisTemplate.opsForValue().get(totalKey);
                    if (totalObj == null)
                        continue;
                    long totalViews = Long.parseLong(totalObj.toString());

                    // Lấy số views đã sync lần trước
                    String syncedKey = RedisKeyConstants.STORY_DB_SYNCED_VIEWS + storyId;
                    Object syncedObj = redisTemplate.opsForValue().get(syncedKey);
                    long alreadySynced = syncedObj != null ? Long.parseLong(syncedObj.toString()) : 0L;

                    long delta = totalViews - alreadySynced;
                    if (delta <= 0)
                        continue;

                    // Chỉ cộng phần chênh lệch vào MySQL
                    storyRepository.incrementTotalViews(storyId, (int) delta);

                    // Cập nhật mốc đã sync
                    redisTemplate.opsForValue().set(syncedKey, totalViews);
                    synced++;

                } catch (Exception e) {
                    log.error("Error syncing views for key {}: {}", totalKey, e.getMessage());
                }
            }

            if (synced > 0) {
                log.info("Scheduled sync: updated totalViews for {} stories", synced);
            }

        } catch (Exception e) {
            log.error("Error in scheduled sync: {}", e.getMessage());
        }
    }
}
