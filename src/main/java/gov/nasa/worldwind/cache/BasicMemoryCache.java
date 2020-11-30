/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.cache;

import gov.nasa.worldwind.util.Logging;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Eric Dalgliesh
 * @version $Id: BasicMemoryCache.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicMemoryCache implements MemoryCache {
    protected final ConcurrentHashMap<Object, CacheEntry> entries;
    protected final CopyOnWriteArrayList<MemoryCache.CacheListener> listeners;
    protected final AtomicLong capacity = new AtomicLong();
    protected final AtomicLong currentUsedCapacity = new AtomicLong();
    protected final Object lock = new Object();
    protected Long lowWater;
    protected String name = "";

    /**
     * Constructs a new cache using <code>capacity</code> for maximum size, and <code>loWater</code> for the low water.
     *
     * @param loWater  the low water level.
     * @param capacity the maximum capacity.
     */
    public BasicMemoryCache(long loWater, long capacity) {
        this.entries = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.capacity.set(capacity);
        this.lowWater = loWater;
        this.currentUsedCapacity.set(0);
    }

    /**
     * @return the number of objects currently stored in this cache.
     */
    public int getNumObjects() {
        return this.entries.size();
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
        if (listener == null) {
            String message = Logging.getMessage("BasicMemoryCache.nullListenerAdded");
            Logging.logger().warning(message);
            throw new IllegalArgumentException(message);
        }
        this.listeners.add(listener);
    }

    /**
     * Removes a cache listener, objects using this listener will no longer receive notification of cache events.
     *
     * @param listener The <code>CacheListener</code> to remove.
     * @throws IllegalArgumentException if <code>listener</code> is null.
     */
    public void removeCacheListener(MemoryCache.CacheListener listener) {
        if (listener == null) {
            String message = Logging.getMessage("BasicMemoryCache.nullListenerRemoved");
            Logging.logger().warning(message);
            throw new IllegalArgumentException(message);
        }
        this.listeners.remove(listener);
    }

    /**
     * Returns the low water level in cache units. When the cache fills, it removes items until it reaches the low water
     * level.
     *
     * @return the low water level.
     */
    public long getLowWater() {
        return this.lowWater;
    }

    /**
     * Sets the new low water level in cache units, which controls how aggresively the cache discards items.
     * <p>
     * When the cache fills, it removes items until it reaches the low water level.
     * <p>
     * Setting a high loWater level will increase cache misses, but decrease average add time, but setting a low loWater
     * will do the opposite.
     *
     * @param loWater the new low water level.
     */
    public void setLowWater(long loWater) {
        if (loWater < this.capacity.get() && loWater >= 0) {
            this.lowWater = loWater;
        }
    }

    /**
     * Returns true if the cache contains the item referenced by key. No guarantee is made as to whether or not the item
     * will remain in the cache for any period of time.
     * <p>
     * This function does not cause the object referenced by the key to be marked as accessed. <code>getObject()</code>
     * should be used for that purpose.
     *
     * @param key The key of a specific object.
     * @return true if the cache holds the item referenced by key.
     * @throws IllegalArgumentException if <code>key</code> is null.
     */
    public boolean contains(Object key) {
        if (key == null) {
            String msg = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        synchronized (this.lock) {
            return this.entries.containsKey(key);
        }
    }

    /**
     * Adds an object to the cache. The add fails if the object or key is null, or if the size is zero, negative or
     * greater than the maximmum capacity.
     *
     * @param key              The unique reference key that identifies this object.
     * @param clientObject     The actual object to be cached.
     * @param clientObjectSize The size of the object in cache units.
     * @return returns true if clientObject was added, false otherwise.
     */
    public boolean add(Object key, Object clientObject, long clientObjectSize) {
        long cap = this.capacity.get();

        if (key == null || clientObject == null || clientObjectSize <= 0 || clientObjectSize > cap) {
            String message = Logging.getMessage("BasicMemoryCache.CacheItemNotAdded");

            if (clientObjectSize > cap) {
                message += " - " + Logging.getMessage("BasicMemoryCache.ItemTooLargeForCache");
            }

            Logging.logger().warning(message);

            return false;
            // the logic behind not throwing an exception is that whether we throw an exception or not,
            // the object won't be added. This doesn't matter because that object could be removed before
            // it is accessed again anyway.
        }

        BasicMemoryCache.CacheEntry entry = new BasicMemoryCache.CacheEntry(key, clientObject, clientObjectSize);

        //synchronized (this.lock) {
        CacheEntry existing = this.entries.put(key, entry);
        if (existing != null && existing!=entry) { // replacing
            this.removeEntry(existing);
            this.entries.put(entry.key, entry);
        }
        if (existing == null || existing!=entry) {
            this.currentUsedCapacity.addAndGet(clientObjectSize);

            if (this.currentUsedCapacity.get() + clientObjectSize > cap) {
                synchronized (this.lock) {
                    this.makeSpace(clientObjectSize);
                }
            }
        }

        //}

        return true;
    }

    public boolean add(Object key, Cacheable clientObject) {
        return this.add(key, clientObject, clientObject.getSizeInBytes());
    }

    /**
     * Remove the object reference by key from the cache. If no object with the corresponding key is found, this method
     * returns immediately.
     *
     * @param key the key of the object to be removed.
     * @throws IllegalArgumentException if <code>key</code> is null.
     */
    public void remove(Object key) {
        if (key == null) {
            Logging.logger().finer("nullValue.KeyIsNull");

            return;
        }

        synchronized (this.lock) {
            CacheEntry entry = this.entries.get(key);
            if (entry != null)
                this.removeEntry(entry);
        }
    }

    /**
     * Obtain the object referenced by key without removing it. Apart from adding an object, this is the only way to
     * mark an object as recently used.
     *
     * @param key The key for the object to be found.
     * @return the object referenced by key if it is present, null otherwise.
     * @throws IllegalArgumentException if <code>key</code> is null.
     */
    public Object getObject(Object key) {
        if (key == null) {
            Logging.logger().finer("nullValue.KeyIsNull");

            return null;
        }

        CacheEntry entry; // don't need to lock because call is atomic
        synchronized (this.lock) {
            entry = this.entries.get(key);

            if (entry == null)
                return null;

            entry.lastUsed = System.nanoTime(); // nanoTime overflows once every 292 years
            // which will result in a slowing of the cache
            // until ww is restarted or the cache is cleared.
        }

        return entry.clientObject;
    }

    /**
     * Empties the cache.
     */
    public void clear() {
        synchronized (this.lock) {
            for (CacheEntry entry : this.entries.values()) {
                this.removeEntry(entry);
            }
        }
    }

    /**
     * Removes <code>entry</code> from the cache. To remove an entry using its key, use <code>remove()</code>.
     *
     * @param entry The entry (as opposed to key) of the item to be removed.
     */
    protected void removeEntry(CacheEntry entry) // MUST BE CALLED WITHIN SYNCHRONIZED
    {
        // all removal passes through this function,
        // so the reduction in "currentUsedCapacity" and listener notification is done here

        if (this.entries.remove(entry.key) != null) { // returns null if entry does not exist

            this.currentUsedCapacity.addAndGet(-entry.clientObjectSize);

            for (MemoryCache.CacheListener listener : this.listeners) {
                try {
                    listener.entryRemoved(entry.key, entry.clientObject);
                } catch (Exception e) {
                    listener.removalException(e, entry.key, entry.clientObject);
                }
            }
        }
    }

    /**
     * Makes at least <code>spaceRequired</code> space in the cache. If spaceRequired is less than (capacity-lowWater),
     * makes more space. Does nothing if capacity is less than spaceRequired.
     *
     * @param spaceRequired the amount of space required.
     */
    private void makeSpace(long spaceRequired) // MUST BE CALLED WITHIN SYNCHRONIZED
    {
        if (spaceRequired > this.capacity.get() || spaceRequired < 0)
            return;

        CacheEntry[] timeOrderedEntries = new CacheEntry[this.entries.size()];
        Arrays.sort(this.entries.values().toArray(timeOrderedEntries)); // TODO

        int i = 0;
        while (this.getFreeCapacity() < spaceRequired || this.getUsedCapacity() > this.lowWater) {
            if (i < timeOrderedEntries.length) {
                this.removeEntry(timeOrderedEntries[i++]);
            }
        }
    }

    /**
     * a <code>String</code> representation of this object is returned.&nbsp; This representation consists of maximum
     * size, current used capacity and number of currently cached items.
     *
     * @return a <code>String</code> representation of this object.
     */
    @Override
    public String toString() {
        return "MemoryCache " + this.name + " max size = " + this.getCapacity() + " current size = "
            + this.currentUsedCapacity.get() + " number of items: " + this.getNumObjects();
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
            if (that == null) {
                String msg = Logging.getMessage("nullValue.CacheEntryIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            return Long.compare(this.lastUsed, that.lastUsed);
        }

        public String toString() {
            return key.toString() + " " + clientObject.toString() + " " + lastUsed + " " + clientObjectSize;
        }
    }
}
