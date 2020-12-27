/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.cache;

////.*;

import gov.nasa.worldwind.util.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author tag
 * @version $Id: BasicMemoryCacheSet.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicMemoryCacheSet implements MemoryCacheSet {

    private final Map<String, MemoryCache> caches = new ConcurrentHashMap<>();

    public boolean containsCache(String key) {
        return this.caches.containsKey(key);
    }

    public MemoryCache getCache(String cacheKey) {
        return this.caches.get(cacheKey);
    }

    @Override
    public MemoryCache getCache(String cacheKey, Function<String, MemoryCache> s) {
        return caches.computeIfAbsent(cacheKey, s);
    }

    public Map<String, MemoryCache> getAllCaches() {
        return this.caches;
    }

    public MemoryCache addCache(String key, MemoryCache cache) {
        MemoryCache existing = this.caches.put(key, cache);
        if (existing != null) {
            String message = Logging.getMessage("MemoryCacheSet.CacheAlreadyExists");
            Logging.logger().fine(message);
            throw new IllegalStateException(message);
        }
        return cache;
    }

    public void clear() {
        caches.values().forEach(MemoryCache::clear);
    }

    public Collection<PerformanceStatistic> getPerformanceStatistics() {
        Collection<PerformanceStatistic> stats = new ArrayList<>();

        for (MemoryCache cache : this.caches.values()) {
            stats.add(new PerformanceStatistic(PerformanceStatistic.MEMORY_CACHE, "Cache Size (Kb): " + cache.getName(),
                cache.getUsedCapacity() / 1000));
        }

        return stats;
    }
}
