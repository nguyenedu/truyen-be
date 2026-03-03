package com.example.truyen.config;

public class RedisKeyConstants {

    // --- VIEW TRACKING (nguồn chính, dùng chung cho cả direct và Kafka) ---

    // Tổng lượt xem tích lũy của story. Key: story:total:views:{storyId}. Type:
    // String
    public static final String STORY_TOTAL_VIEWS = "story:total:views:";

    // Lượt xem hôm nay. Key: story:views:today:{storyId}. Type: String. TTL: 1 ngày
    public static final String STORY_VIEWS_TODAY = "story:views:today:";

    // Lượt xem theo từng ngày cụ thể (trending). Key: story:views:{date}:{storyId}.
    // TTL: 35 ngày
    public static final String STORY_VIEWS_DATE = "story:views:";

    // Unique viewers hôm nay. Key: story:viewers:today:{storyId}. Type: Set. TTL: 1
    // ngày
    public static final String STORY_UNIQUE_VIEWERS_TODAY = "story:viewers:today:";

    // --- DELTA SYNC TRACKING ---

    // Số views đã được sync vào MySQL. Key: story:db:synced:{storyId}. Type: String
    // Dùng bởi scheduled job để tính delta = STORY_TOTAL_VIEWS -
    // STORY_DB_SYNCED_VIEWS
    public static final String STORY_DB_SYNCED_VIEWS = "story:db:synced:";

    // --- TRENDING ---

    // Cache top 100 trending (daily). Key: trending:daily
    public static final String TRENDING_DAILY = "trending:daily";

    // Cache top 100 trending (weekly). Key: trending:weekly
    public static final String TRENDING_WEEKLY = "trending:weekly";

    // Cache top 100 trending (monthly). Key: trending:monthly
    public static final String TRENDING_MONTHLY = "trending:monthly";

    // Trending score của từng story. Key: story:trending:score:{storyId}
    public static final String STORY_TRENDING_SCORE = "story:trending:score:";

    // Real-time trending scores từ Kafka. Key: trending:stories
    public static final String TRENDING_STORIES = "trending:stories";

    // --- SEARCH ---

    // Popular search queries. Key: search:popular
    public static final String SEARCH_POPULAR = "search:popular";

    // Trending searches theo ngày. Key: search:trending:{date}
    public static final String SEARCH_TRENDING = "search:trending:";

    // Search history của user. Key: search:user:{userId}
    public static final String SEARCH_USER_HISTORY = "search:user:";

    // Cache max views trong N ngày. Key: max:views:{days}d
    public static final String MAX_VIEWS_PREFIX = "max:views:";

    // --- AUTH ---

    // Blacklist token. Key: token:blacklist:{token}
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    private RedisKeyConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}