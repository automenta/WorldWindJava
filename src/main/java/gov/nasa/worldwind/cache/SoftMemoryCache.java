package gov.nasa.worldwind.cache;

import com.github.benmanes.caffeine.cache.*;
import com.google.common.util.concurrent.MoreExecutors;
import org.jetbrains.annotations.*;

public class SoftMemoryCache extends AbstractMemoryCache
    implements RemovalListener<Object, AbstractMemoryCache.CacheEntry> {

    final Cache<Object, CacheEntry> cache;

    public SoftMemoryCache() {
        super(Long.MAX_VALUE);

        cache = Caffeine.newBuilder().softValues()
            .removalListener(this)
            .executor(MoreExecutors.directExecutor())
            .build();
    }

    public SoftMemoryCache(String name) {
        this();
        setName(name);
    }

    @Override
    public boolean contains(Object key) {
        return cache.asMap().containsKey(key);
    }

    @Override
    public boolean add(Object key, Object clientObject, long objectSize) {

        CacheEntry e = cache.get(key, k -> new CacheEntry(k, clientObject, objectSize));

        return true;
    }

    @Override
    public void remove(Object key) {
        cache.invalidate(key);
    }

    @Override
    public Object getObject(Object key) {
        final CacheEntry e = cache.getIfPresent(key);
        return e != null ? e.clientObject : null;
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public int getNumObjects() {
        return (int) cache.estimatedSize();
    }

    @Override
    public void onRemoval(@Nullable Object key, @Nullable CacheEntry value, RemovalCause cause) {

        if (!listeners.isEmpty()) {
            listeners.forEach(l -> l.entryRemoved(key, value));
        }
    }
}