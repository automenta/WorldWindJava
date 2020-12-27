package gov.nasa.worldwind.cache;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

abstract public class AbstractMemoryCache implements MemoryCache {
    protected final CopyOnWriteArrayList<CacheListener> listeners;
    protected final AtomicLong capacity = new AtomicLong();
    protected final AtomicLong currentUsedCapacity = new AtomicLong();
    protected final Object lock = new Object();
    protected String name = "";

    public AbstractMemoryCache(long capacity) {
        this.listeners = new CopyOnWriteArrayList<>();
        this.capacity.set(capacity);
        this.currentUsedCapacity.set(0);
    }

    /**
     * @return the capacity of the cache.
     */
    public long getCapacity() {
        return this.capacity.get();
    }

    /**
     * Sets the new capacity for the cache. When decreasing cache size, it is recommended to check that the lowWater
     * variable is suitable. If the capacity infringes on items stored in the cache, these items are removed. Setting a
     * new low water is up to the user, that is, it remains unchanged and may be higher than the maximum capacity. When
     * the low water level is higher than or equal to the maximum capacity, it is ignored, which can lead to poor
     * performance when adding entries.
     *
     * @param newCapacity the new capacity of the cache.
     */
    public void setCapacity(long newCapacity) {
//        this.makeSpace(this.capacity - newCapacity);
        this.capacity.set(newCapacity);
    }

    /**
     * @return the number of cache units that the cache currently holds.
     */
    public long getUsedCapacity() {
        return this.currentUsedCapacity.get();
    }

    /**
     * @return the amount of free space left in the cache (in cache units).
     */
    public long getFreeCapacity() {
        return Math.max(this.capacity.get() - this.currentUsedCapacity.get(), 0);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    /**
     * Adds a  cache listener, MemoryCache listeners are used to notify classes when an item is removed from the cache.
     *
     * @param listener The new <code>CacheListener</code>.
     * @throws IllegalArgumentException is <code>listener</code> is null.
     */
    public void addCacheListener(MemoryCache.CacheListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes a cache listener, objects using this listener will no longer receive notification of cache events.
     *
     * @param listener The <code>CacheListener</code> to remove.
     * @throws IllegalArgumentException if <code>listener</code> is null.
     */
    public void removeCacheListener(MemoryCache.CacheListener listener) {
        this.listeners.remove(listener);
    }

    public final boolean add(Object key, Cacheable clientObject) {
        return this.add(key, clientObject, clientObject.getSizeInBytes());
    }

    protected static class CacheEntry implements Comparable<CacheEntry> {
        protected final long clientObjectSize;
        final Object key;
        final Object clientObject;
        protected long lastUsed;

        CacheEntry(Object key, Object clientObject, long clientObjectSize) {
            this.key = key;
            this.clientObject = clientObject;
            this.lastUsed = System.nanoTime();
            this.clientObjectSize = clientObjectSize;
        }

        public int compareTo(CacheEntry that) {
            if (this == that)
                return 0;

            int when = Long.compare(this.lastUsed, that.lastUsed);
            if (when != 0)
                return when;

            return Integer.compare(System.identityHashCode(clientObject), System.identityHashCode(that.clientObject));
        }

        public String toString() {
            return key.toString() + ' ' + clientObject.toString() + ' ' + lastUsed + ' ' + clientObjectSize;
        }
    }
}
