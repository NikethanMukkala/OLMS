package utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class CacheManager {
    
    // A generic string-based object cache with a short lifespan
    public static final Cache<String, Object> globalStatsCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    // Clears the entire cache. Use this when manual invalidation is required.
    public static void invalidateAll() {
        globalStatsCache.invalidateAll();
    }
}
