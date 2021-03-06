/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import java.util.*;

/**
 * BoundedHashMap is a map with a fixed capacity. When the map's size exceeds its capacity, it automatically removes
 * elements until its size is equal to its capacity. <p> BoundedHashMap can operate in two ordering modes: insertion
 * order and access order. The mode specified which entries are automatically removed when the map is over capacity. In
 * insertion order mode the map removes the eldest entry (the first entry added). In access order mode, the map
 * automatically removes the least recently used entry.
 *
 * @param <K> The map key type.
 * @param <V> The map value type.
 * @author dcollins
 * @version $Id: BoundedHashMap.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BoundedHashMap<K, V> extends LinkedHashMap<K, V> {
    protected static final int DEFAULT_CAPACITY = 16;
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private int capacity;

    /**
     * Creates a BoundedHashMap with a specified maximum capacity and ordering mode.
     *
     * @param capacity    the maximum number of entries in the map.
     * @param accessOrder the ordering mode: true specifies access order, false specifies insertion order.
     */
    public BoundedHashMap(int capacity, boolean accessOrder) {
        super(BoundedHashMap.getInitialCapacity(capacity, BoundedHashMap.DEFAULT_LOAD_FACTOR), BoundedHashMap.DEFAULT_LOAD_FACTOR, accessOrder);
        this.capacity = capacity;
    }

    /**
     * Creates a BoundedHashMap with a specified maximum capacity, in insertion order mode.
     *
     * @param capacity the maximum number of entries in the map.
     */
    public BoundedHashMap(int capacity) {
        this(capacity, false);
    }

    /**
     * Creates a BoundedHashMap with a capacity of 16, in insertion order mode.
     */
    public BoundedHashMap() {
        this(BoundedHashMap.DEFAULT_CAPACITY);
    }

    protected static int getInitialCapacity(int capacity, float loadFactor) {
        return WWMath.powerOfTwoCeiling((int) Math.ceil(capacity / loadFactor));
    }

    /**
     * Returns the maximum number of entries in the map.
     *
     * @return maximum number of entries in the map.
     */
    public int getCapacity() {
        return this.capacity;
    }

    /**
     * Sets the maximum number of entries in the map. If the new capacity is less than the map's current size, this
     * automatically removes entries until the map's size is equal to its capacity.
     *
     * @param capacity maximum number of entries in the map.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.removeOverCapacityEntries();
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return this.size() > this.getCapacity();
    }

    /**
     * Removes the first n entries in the map, where n is the number of entries in the map beyond its capacity. Note
     * that the entry set and the corresponding iterator are backed by the map itself, so changes to an entry iterator
     * correspond to changes in the map. We use the iterator's remove() method because we're removing elements from the
     * entry set as we iterate over them.
     */
    protected void removeOverCapacityEntries() {
        int count = this.size() - this.getCapacity();
        if (count <= 0)
            return;

        Iterator<Map.Entry<K, V>> iter = this.entrySet().iterator();
        for (int i = 0; i < count && iter.hasNext(); i++) {
            iter.next();
            iter.remove();
        }
    }
}
