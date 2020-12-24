/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.cache;

import gov.nasa.worldwind.util.Logging;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eric Dalgliesh
 * @version $Id: BasicMemoryCache.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@Deprecated public class BasicMemoryCache extends AbstractMemoryCache {

    protected final Map<Object, CacheEntry> entries;
    protected Long lowWater;

    /**
     * Constructs a new cache using <code>capacity</code> for maximum size, and <code>loWater</code> for the low water.
     *
     * @param loWater  the low water level.
     * @param capacity the maximum capacity.
     */
    public BasicMemoryCache(long loWater, long capacity) {
        super(capacity);
        this.lowWater = loWater;
        this.entries = new ConcurrentHashMap<>();
    }

    /**
     * @return the number of objects currently stored in this cache.
     */
    @Override public int getNumObjects() {
        return this.entries.size();
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
    @Override public boolean contains(Object key) {
        return this.entries.containsKey(key);
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


    /**
     * Remove the object reference by key from the cache. If no object with the corresponding key is found, this method
     * returns immediately.
     *
     * @param key the key of the object to be removed.
     * @throws IllegalArgumentException if <code>key</code> is null.
     */
    public void remove(Object key) {

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

        CacheEntry entry; // don't need to lock because call is atomic

            entry = this.entries.get(key);

            if (entry == null)
                return null;

            entry.lastUsed = System.nanoTime(); // nanoTime overflows once every 292 years
            // which will result in a slowing of the cache
            // until ww is restarted or the cache is cleared.


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

}
