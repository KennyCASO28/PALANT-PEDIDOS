package org.example.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for SVG content to avoid repeated disk I/O.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class SVGCache {

    private static final Map<String, String> cache = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> categorizedCache = new ConcurrentHashMap<>();

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

