package com.example.truyen.config;

/**
 * Redis Key Constants
 * Chứa tất cả các key patterns dùng trong Redis
 */
public class RedisKeyConstants {

    // ==================== VIEWS TRACKING ====================

    /**
     * Key: story:views:today:{storyId}
     * Type: String (counter)
     * TTL: 1 day
     * Mục đích: Đếm số views hôm nay của story
     *
     * Ví dụ: story:views:today:1 = "25"
     */
    public static final String STORY_VIEWS_TODAY = "story:views:today:";

    /**
     * Key: story:views:{date}:{storyId}
     * Type: String (counter)
     * TTL: 35 days
     * Mục đích: Đếm views theo từng ngày cụ thể (để tính trending)
     *
     * Ví dụ: story:views:2026-01-28:1 = "15"
     */
    public static final String STORY_VIEWS_DATE = "story:views:";

    /**
     * Key: story:viewers:today:{storyId}
     * Type: Set (unique user IDs)
     * TTL: 1 day
     * Mục đích: Track unique viewers hôm nay
     *
     * Ví dụ: story:viewers:today:1 = {3, 5, 7, 12}
     */
    public static final String STORY_UNIQUE_VIEWERS_TODAY = "story:viewers:today:";


    // ==================== TRENDING CACHE ====================

    /**
     * Key: trending:daily
     * Type: List (StoryTrendingDTO objects)
     * TTL: 30 minutes
     * Mục đích: Cache top 100 trending stories (daily)
     */
    public static final String TRENDING_DAILY = "trending:daily";

    /**
     * Key: trending:weekly
     * Type: List (StoryTrendingDTO objects)
     * TTL: 2 hours
     * Mục đích: Cache top 100 trending stories (weekly)
     */
    public static final String TRENDING_WEEKLY = "trending:weekly";

    /**
     * Key: trending:monthly
     * Type: List (StoryTrendingDTO objects)
     * TTL: 6 hours
     * Mục đích: Cache top 100 trending stories (monthly)
     */
    public static final String TRENDING_MONTHLY = "trending:monthly";

    /**
     * Key: story:trending:score:{storyId}
     * Type: String (double score)
     * TTL: Varies by ranking type
     * Mục đích: Cache trending score của từng story
     *
     * Ví dụ: story:trending:score:1 = "85.5"
     */
    public static final String STORY_TRENDING_SCORE = "story:trending:score:";


    // ==================== HELPER KEYS ====================

    /**
     * Key: max:views:{days}d
     * Type: String (long number)
     * TTL: 30 minutes
     * Mục đích: Cache max views trong N ngày (để normalize score)
     *
     * Ví dụ: max:views:7d = "1500000"
     */
    public static final String MAX_VIEWS_PREFIX = "max:views:";


    // ==================== PRIVATE CONSTRUCTOR ====================

    /**
     * Private constructor để ngăn instantiate class
     */
    private RedisKeyConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}