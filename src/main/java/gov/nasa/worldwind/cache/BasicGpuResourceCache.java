/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.cache;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.Texture;
import gov.nasa.worldwind.util.Logging;

import java.util.logging.Level;

/**
 * Provides the interface for caching of OpenGL resources that are stored on or registered with a GL context. This cache
 * maintains a map of resources that fit within a specifiable memory capacity. If adding a resource would exceed this
 * cache's capacity, existing but least recently used resources are removed from the cache to make room. The cache is
 * reduced to the "low water" size in this case (see {@link #setLowWater(long)}.
 * <p>
 * When a resource is removed from the cache, and if it is a recognized OpenGL resource -- a texture, a list of vertex
 * buffer IDs, a list of display list IDs, etc. -- and there is a current Open GL context, the appropriate glDelete
 * function is called to de-register the resource with the GPU. If there is no current OpenGL context the resource is
 * not deleted and will likely remain allocated on the GPU until the GL context is destroyed.
 *
 * @author tag
 * @version $Id: BasicGpuResourceCache.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicGpuResourceCache implements GpuResourceCache {
    protected final MemoryCache cache;

    public BasicGpuResourceCache() {

        this.cache =
            new SoftMemoryCache();

        this.cache.setName("GPU Resource Cache");
        this.cache.addCacheListener(new MemoryCache.CacheListener() {
            public void entryRemoved(Object key, Object clientObject) {
                BasicGpuResourceCache.onEntryRemoved(key, clientObject);
            }

            public void removalException(Throwable e, Object key, Object clientObject) {
                Logging.logger().log(Level.INFO,
                    Logging.getMessage("BasicMemoryCache.ExceptionFromRemovalListener", e.getMessage()));
            }
        });
    }

    @SuppressWarnings("UnusedParameters")
    protected static void onEntryRemoved(Object key, Object clientObject) {
        GLContext context = GLContext.getCurrent();
        if (context == null || context.getGL() == null)
            return;

        if (!(clientObject instanceof CacheEntry)) // shouldn't be null or wrong type, but check anyway
            return;

        CacheEntry entry = (CacheEntry) clientObject;
        GL2 gl = context.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        if (entry.resourceType == GpuResourceCache.TEXTURE) {
            // Unbind a tile's texture when the tile leaves the cache.
            ((Texture) entry.resource).destroy(gl);
        } else if (entry.resourceType == GpuResourceCache.VBO_BUFFERS) {
            int[] ids = (int[]) entry.resource;
            gl.glDeleteBuffers(ids.length, ids, 0);
        } else if (entry.resourceType == GpuResourceCache.DISPLAY_LISTS) {
            // Delete display list ids. They're in a two-element int array, with the id at 0 and the count at 1
            int[] ids = (int[]) entry.resource;
            gl.glDeleteLists(ids[0], ids[1]);
        }
    }

    protected static CacheEntry createCacheEntry(Object resource, String resourceType) {
        CacheEntry entry = new CacheEntry(resource, resourceType);
        entry.resourceSize = BasicGpuResourceCache.computeEntrySize(entry);

        return entry;
    }

    protected static CacheEntry createCacheEntry(Object resource, String resourceType, long size) {
        CacheEntry entry = new CacheEntry(resource, resourceType, size);
        entry.resourceSize = size;

        return entry;
    }

    protected static long computeEntrySize(CacheEntry entry) {
        if (entry.resourceType == GpuResourceCache.TEXTURE)
            return BasicGpuResourceCache.computeTextureSize(entry);

        return 0;
    }

    protected static long computeTextureSize(CacheEntry entry) {
        Texture texture = (Texture) entry.resource;

        long size = texture.getEstimatedMemorySize();

        // JOGL returns a zero estimated memory size for some textures, so calculate a size ourselves.
        if (size < 1)
            size = texture.getHeight() * texture.getWidth() * 4;

        return size;
    }

    public void put(Object key, Texture texture) {
        CacheEntry te = BasicGpuResourceCache.createCacheEntry(texture, GpuResourceCache.TEXTURE);
        this.cache.add(key, te);
    }

    public void put(Object key, Object resource, String resourceType, long size) {
        CacheEntry te = BasicGpuResourceCache.createCacheEntry(resource, resourceType, size);
        this.cache.add(key, te);
    }

    public Object get(Object key) {
        CacheEntry entry = (CacheEntry) this.cache.getObject(key);
        return entry != null ? entry.resource : null;
    }

    public Texture getTexture(Object key) {
        CacheEntry entry = (CacheEntry) this.cache.getObject(key);
        return entry != null && entry.resourceType == GpuResourceCache.TEXTURE ? (Texture) entry.resource : null;
    }

    public void remove(Object key) {
        this.cache.remove(key);
    }

    public int getNumObjects() {
        return this.cache.getNumObjects();
    }

    public long getCapacity() {
        return this.cache.getCapacity();
    }

    /**
     * Sets the new capacity (in bytes) for the cache. When decreasing cache size, it is recommended to check that the
     * lowWater variable is suitable. If the capacity infringes on items stored in the cache, these items are removed.
     * Setting a new low water is up to the user, that is, it remains unchanged and may be higher than the maximum
     * capacity. When the low water level is higher than or equal to the maximum capacity, it is ignored, which can lead
     * to poor performance when adding entries.
     *
     * @param newCapacity the new capacity of the cache.
     */
    public synchronized void setCapacity(long newCapacity) {
        this.cache.setCapacity(newCapacity);
    }

    public long getUsedCapacity() {
        return this.cache.getUsedCapacity();
    }

    public long getFreeCapacity() {
        return this.cache.getFreeCapacity();
    }

    public boolean contains(Object key) {
        return this.cache.contains(key);
    }

    public void clear() {
        this.cache.clear();
    }

    public static class CacheEntry implements Cacheable {
        protected final String resourceType;
        protected final Object resource;
        protected long resourceSize;

        public CacheEntry(Object resource, String resourceType) {
            this.resource = resource;
            this.resourceType = resourceType;
        }

        public CacheEntry(Object resource, String resourceType, long size) {
            this.resource = resource;
            this.resourceType = resourceType;
            this.resourceSize = size;
        }

        public long getSizeInBytes() {
            return this.resourceSize;
        }
    }
}
