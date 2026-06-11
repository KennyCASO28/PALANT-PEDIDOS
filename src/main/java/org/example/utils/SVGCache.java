package org.example.utils;

import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cache for SVG content to avoid repeated disk I/O.
 * Thread-safe implementation using Google Guava Cache for advanced memory management.
 */
public class SVGCache {

    // Integración de Google Guava: Limita la memoria a 500 elementos y expira si no se usan
    private static final Map<String, String> cache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .<String, String>build()
            .asMap();

    private static final Map<String, Map<String, String>> categorizedCache = CacheBuilder.newBuilder()
            .maximumSize(200)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .<String, Map<String, String>>build()
            .asMap();

    /**
     * Loads SVG path content from cache if available, otherwise loads from disk and
     * caches it.
     * 
     * @param resourcePath The resource path to the SVG file
     * @return The SVG path content as a string
     */
    public static String loadPath(String resourcePath) {
        return cache.computeIfAbsent(resourcePath, SVGUtils::loadPath);
    }

    public static String loadOptionalPath(String resourcePath) {
        return cache.computeIfAbsent(resourcePath, path -> SVGUtils.loadPath(path, true));
    }

    /**
     * Clears the entire cache. Useful for memory management if needed.
     */
    public static void clear() {
        cache.clear();
        categorizedCache.clear();
    }

    /**
     * Loads categorized SVG paths (BODY, SLEEVES) from cache or parses them.
     */
    public static Map<String, String> loadCategorizedPaths(String resourcePath) {
        return categorizedCache.computeIfAbsent(resourcePath, SVGUtils::loadCategorizedPaths);
    }

    /**
     * Gets the current cache size (number of cached files).
     * 
     * @return Number of SVG files in cache
     */
    public static int getCacheSize() {
        return cache.size();
    }
}

