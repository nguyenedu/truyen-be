package com.example.truyen.config;

// Hằng số Redis
public class RedisKeyConstants {

    // Mục đích: Đếm số views hôm nay của story. Key: story:views:today:{storyId},
    // Type: String
    public static final String STORY_VIEWS_TODAY = "story:views:today:";

    // Mục đích: Đếm views theo từng ngày cụ thể (để tính trending). Key:
    // story:views:{date}:{storyId}
    public static final String STORY_VIEWS_DATE = "story:views:";

    // Mục đích: Track unique viewers hôm nay. Key: story:viewers:today:{storyId},
    // Type: Set
    public static final String STORY_UNIQUE_VIEWERS_TODAY = "story:viewers:today:";

    // Mục đích: Cache top 100 trending stories (daily). Key: trending:daily
    public static final String TRENDING_DAILY = "trending:daily";

    // Mục đích: Cache top 100 trending stories (weekly). Key: trending:weekly
    public static final String TRENDING_WEEKLY = "trending:weekly";

    // Mục đích: Cache top 100 trending stories (monthly). Key: trending:monthly
    public static final String TRENDING_MONTHLY = "trending:monthly";

    // Mục đích: Cache trending score của từng story. Key:
    // story:trending:score:{storyId}
    public static final String STORY_TRENDING_SCORE = "story:trending:score:";

    // Mục đích: Real-time trending scores từ Kafka analytics. Key: trending:stories
    public static final String TRENDING_STORIES = "trending:stories";

    // Mục đích: Popular search queries (all time). Key: search:popular
    public static final String SEARCH_POPULAR = "search:popular";

    // Mục đích: Trending searches theo ngày. Key: search:trending:{date}
    public static final String SEARCH_TRENDING = "search:trending:";

    // Mục đích: Search history của user. Key: search:user:{userId}
    public static final String SEARCH_USER_HISTORY = "search:user:";

    // Mục đích: Cache max views trong N ngày (để normalize score). Key:
    // max:views:{days}d
    public static final String MAX_VIEWS_PREFIX = "max:views:";

    // Private constructor để ngăn instantiate class
    private RedisKeyConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}