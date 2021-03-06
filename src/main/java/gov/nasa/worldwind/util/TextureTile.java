/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class manages the conversion and timing of image data to a JOGL Texture, and provides an interface for binding
 * the texture and applying any texture transforms to align the texture and texture coordinates.
 * <p>
 *
 * @author tag
 * @version $Id: TextureTile.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class TextureTile extends Tile implements SurfaceTile {
    protected final AtomicLong updateTime = new AtomicLong(0);
    protected boolean hasMipmapData;
    private volatile TextureData textureData; // if non-null, then must be converted to a Texture
    private TextureTile fallbackTile; // holds texture to use if own texture not available

    public TextureTile(Sector sector, Level level, int row, int col) {
        super(sector, level, row, col);
    }

    public TextureTile(Sector sector, Level level, int row, int column, String cacheName) {
        super(sector, level, row, column, cacheName);
    }

    /**
     * Returns the memory cache used to cache tiles for this class and its subclasses, initializing the cache if it
     * doesn't yet exist.
     *
     * @return the memory cache associated with the tile.
     */
    public static final MemoryCache cache = WorldWind.getMemoryCacheSet().getCache(TextureTile.class.getName(), SoftMemoryCache::new);
    public static MemoryCache getMemoryCache() {
        return TextureTile.cache;
    }

    @Override
    public Sector getSector() {
        return sector;
    }

    @Override
    public long getSizeInBytes() {
        long size = super.getSizeInBytes();

        if (this.textureData != null)
            size += this.textureData.getEstimatedMemorySize();

        return size;
    }

    public List<? extends LatLon> getCorners() {
        List<LatLon> list = new ArrayList<>(4);
        for (LatLon ll : sector) {
            list.add(ll);
        }
        return list;
    }

    public TextureTile getFallbackTile() {
        return this.fallbackTile;
    }

    public void setFallbackTile(TextureTile fallbackTile) {
        this.fallbackTile = fallbackTile;
    }

    /**
     * Returns the texture data most recently specified for the tile. New texture data is typically specified when a new
     * image is read, either initially or in response to image expiration.
     * <p>
     * If texture data is non-null, a new texture is created from the texture data when the tile is next bound or
     * otherwise initialized. The texture data field is then set to null. Subsequently setting texture data to be
     * non-null causes a new texture to be created when the tile is next bound or initialized.
     *
     * @return the texture data, which may be null.
     */
    public TextureData getTextureData() {
        return this.textureData;
    }

    /**
     * Specifies new texture data for the tile. New texture data is typically specified when a new image is read, either
     * initially or in response to image expiration.
     * <p>
     * If texture data is non-null, a new texture is created from the texture data when the tile is next bound or
     * otherwise initialized. The texture data field is then set to null. Subsequently setting texture data to be
     * non-null causes a new texture to be created when the tile is next bound or initialized.
     * <p>
     * When a texture is created from the texture data, the texture data field is set to null to indicate that the data
     * has been converted to a texture and its resources may be released.
     *
     * @param textureData the texture data, which may be null.
     */
    public void setTextureData(TextureData textureData) {
        this.textureData = textureData;
        if (textureData.getMipmapData() != null)
            this.hasMipmapData = true;
    }

    public Texture getTexture(GpuResourceCache tc) {

        return tc.getTexture(this.key);
    }

    public boolean isTextureInMemory(GpuResourceCache tc) {

        return this.getTexture(tc) != null || this.getTextureData() != null;
    }

    public long getUpdateTime() {
        return this.updateTime.get();
    }

    public boolean isTextureExpired() {
        return this.isTextureExpired(level.getExpiryTime());
    }

    public boolean isTextureExpired(long expiryTime) {
        return this.updateTime.get() > 0 && this.updateTime.get() < expiryTime;
    }

    public void setTexture(GpuResourceCache tc, Texture texture) {

        tc.put(this.key, texture);
        this.updateTime.set(System.currentTimeMillis());

        // No more need for texture data; allow garbage collector and memory cache to reclaim it.
        // This also signals that new texture data has been converted.
        this.textureData = null;
        this.updateMemoryCache();
    }

    public Vec4 getCentroidPoint(Globe globe) {

        return globe.computePointFromLocation(sector.getCentroid());
    }

    public Extent getExtent(DrawContext dc) {

        return Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), sector);
    }

    /**
     * Splits this texture tile into four tiles; one for each sub quadrant of this texture tile. This attempts to
     * retrieve each sub tile from the texture tile cache. This calls {@link #createSubTile(Sector, Level, int, int)} to
     * create sub tiles not found in the cache.
     *
     * @param nextLevel the level for the sub tiles.
     * @return a four-element array containing this texture tile's sub tiles.
     * @throws IllegalArgumentException if the level is null.
     */
    public TextureTile[] subTiles(Level nextLevel) {

        Angle p0 = sector.latMin();
        Angle p2 = sector.latMax();
        Angle p1 = Angle.midAngle(p0, p2);

        Angle t0 = sector.lonMin();
        Angle t2 = sector.lonMax();
        Angle t1 = Angle.midAngle(t0, t2);

        int row = this.row;
        int col = this.col;

        TextureTile[] subTiles = new TextureTile[4];

        TileKey key = this.createSubTileKey(nextLevel, 2 * row, 2 * col);
        TextureTile subTile = this.getTileFromMemoryCache(key);
        subTiles[0] = subTile != null ? subTile
            : this.createSubTile(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col);

        key = this.createSubTileKey(nextLevel, 2 * row, 2 * col + 1);
        subTile = this.getTileFromMemoryCache(key);
        subTiles[1] = subTile != null ? subTile
            : this.createSubTile(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1);

        key = this.createSubTileKey(nextLevel, 2 * row + 1, 2 * col);
        subTile = this.getTileFromMemoryCache(key);
        subTiles[2] = subTile != null ? subTile
            : this.createSubTile(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col);

        key = this.createSubTileKey(nextLevel, 2 * row + 1, 2 * col + 1);
        subTile = this.getTileFromMemoryCache(key);
        subTiles[3] = subTile != null ? subTile
            : this.createSubTile(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1);

        return subTiles;
    }

    /**
     * Creates a sub tile of this texture tile with the specified {@link Sector}, {@link Level}, row, and column. This
     * is called by {@link #subTiles(Level)}, to construct a sub tile for each quadrant of this tile. Subclasses
     * must override this method to return an instance of the derived version.
     *
     * @param sector the sub tile's sector.
     * @param level  the sub tile's level.
     * @param row    the sub tile's row.
     * @param col    the sub tile's column.
     * @return a sub tile of this texture tile.
     */
    protected TextureTile createSubTile(Sector sector, Level level, int row, int col) {
        return new TextureTile(sector, level, row, col);
    }

    /**
     * Returns a key for a sub tile of this texture tile with the specified {@link Level}, row, and column. This is
     * called by {@link #subTiles(Level)}, to create a sub tile key for each quadrant of this tile.
     *
     * @param level the sub tile's level.
     * @param row   the sub tile's row.
     * @param col   the sub tile's column.
     * @return a sub tile of this texture tile.
     */
    protected TileKey createSubTileKey(Level level, int row, int col) {
        return new TileKey(level.getLevelNumber(), row, col, level.getCacheName());
    }

    protected TextureTile getTileFromMemoryCache(TileKey tileKey) {
        return (TextureTile) TextureTile.getMemoryCache().getObject(tileKey);
    }

    protected void updateMemoryCache() {
//        if (this.getTileFromMemoryCache(this.key) != null)
            TextureTile.getMemoryCache().add(this.key, this);
    }

    protected Texture initializeTexture(DrawContext dc) {

        Texture t = this.getTexture(dc.gpuCache());
        // Return texture if found and there is no new texture data
        if (t != null && this.getTextureData() == null)
            return t;

//        if (this.getTextureData() == null) // texture not in cache yet texture data is null, can't initialize
//        {
//            String msg = Logging.getMessage("nullValue.TextureDataIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalStateException(msg);
//        }

//        try {
            t = TextureIO.newTexture(this.getTextureData());
//        }
//        catch (Exception e) {
//            String msg = Logging.getMessage("layers.TextureLayer.ExceptionAttemptingToReadTextureFile", "");
//            Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);
//            return null;
//        }

        this.setTexture(dc.gpuCache(), t);
        t.bind(dc.getGL());

        this.setTextureParameters(dc, t);

        return t;
    }

    protected void setTextureParameters(DrawContext dc, Texture t) {

        GL gl = dc.getGL();

        // Use a mipmap minification filter when either of the following is true:
        // a. The texture has mipmap data. This is typically true for formats with embedded mipmaps, such as DDS.
        // b. The texture is setup to have GL automatically generate mipmaps. This is typically true when a texture is
        //    loaded from a standard image type, such as PNG or JPEG, and the caller instructed JOGL to generate
        //     mipmaps.
        // Additionally, the texture must be in the latitude range (-80, 80). We do this to prevent seams that appear
        // between textures near the poles.
        //
        // TODO: remove the latitude range restriction if a better tessellator fixes the problem.

        boolean useMipmapFilter = (this.hasMipmapData || t.isUsingAutoMipmapGeneration())
            && sector.latMax < 80.0d && sector.latMin > -80;

        // Set the texture minification filter. If the texture qualifies for mipmaps, apply a minification filter that
        // will access the mipmap data using the highest quality algorithm. If the anisotropic texture filter is
        // available, we will enable it. This will sharpen the appearance of the mipmap filter when the textured
        // surface is at a high slope to the eye.
        if (useMipmapFilter) {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);

            // If the maximum degree of anisotropy is 2.0 or greater, then we know this graphics context supports
            // the anisotropic texture filter.
            double maxAnisotropy = dc.getGLRuntimeCapabilities().getMaxTextureAnisotropy();
            if (dc.getGLRuntimeCapabilities().isUseAnisotropicTextureFilter() && maxAnisotropy >= 2.0) {
                gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, (float) maxAnisotropy);
            }
        }
        // If the texture does not qualify for mipmaps, then apply a linear minification filter.
        else {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        }

        // Set the texture magnification filter to a linear filter. This will blur the texture as the eye gets very
        // near, but this is still a better choice than nearest neighbor filtering.
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

        // Set the S and T wrapping modes to clamp to the texture edge. This way no border pixels will be sampled by
        // either the minification or magnification filters.
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
    }

    public boolean bind(DrawContext dc) {
//        if (dc == null) {
//            String message = Logging.getMessage("nullValue.DrawContextIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalStateException(message);
//        }

        // Reinitialize texture if new texture data
        if (this.getTextureData() != null) {
            Texture t = this.initializeTexture(dc);
            if (t != null)
                return true; // texture was bound during initialization.
        }

        Texture t = this.getTexture(dc.gpuCache());

        if (t == null && this.getFallbackTile() != null) {
            TextureTile resourceTile = this.getFallbackTile();
            t = resourceTile.getTexture(dc.gpuCache());
            if (t == null) {
                t = resourceTile.initializeTexture(dc);
                if (t != null)
                    return true; // texture was bound during initialization.
            }
        }

        if (t != null)
            t.bind(dc.getGL());

        return t != null;
    }

    public void applyInternalTransform(DrawContext dc, boolean textureIdentityActive) {

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        Texture t;
        if (this.getTextureData() != null) // Reinitialize if new texture data
            t = this.initializeTexture(dc);
        else
            t = this.getTexture(dc.gpuCache()); // Use the tile's texture if available

        if (t != null) {
            if (t.getMustFlipVertically()) {
                if (!textureIdentityActive) {
                    gl.glMatrixMode(GL2.GL_TEXTURE);
                    gl.glLoadIdentity();
                }
                gl.glScaled(1, -1, 1);
                gl.glTranslated(0, -1, 0);
            }
            return;
        }

        // Use the tile's fallback texture if its primary texture is not available.
        TextureTile resourceTile = this.getFallbackTile();
        if (resourceTile == null) // no fallback specified
            return;

        t = resourceTile.getTexture(dc.gpuCache());
        if (t == null && resourceTile.getTextureData() != null)
            t = resourceTile.initializeTexture(dc);

        if (t == null) // was not able to initialize the fallback texture
            return;

        // Apply necessary transforms to the fallback texture.
        if (!textureIdentityActive) {
            gl.glMatrixMode(GL2.GL_TEXTURE);
            gl.glLoadIdentity();
        }

        if (t.getMustFlipVertically()) {
            gl.glScaled(1, -1, 1);
            gl.glTranslated(0, -1, 0);
        }

        this.applyResourceTextureTransform(dc);
    }

    protected void applyResourceTextureTransform(DrawContext dc) {

        if (level == null)
            return;

        int levelDelta = this.getLevelNumber() - this.getFallbackTile().getLevelNumber();
        if (levelDelta <= 0)
            return;

        double twoToTheN = Math.pow(2, levelDelta);
        double oneOverTwoToTheN = 1 / twoToTheN;

        double sShift = oneOverTwoToTheN * (col % twoToTheN);
        double tShift = oneOverTwoToTheN * (row % twoToTheN);

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        gl.glTranslated(sShift, tShift, 0);
        gl.glScaled(oneOverTwoToTheN, oneOverTwoToTheN, 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        return Objects.equals(this.key, ((Tile) o).key);
    }

    @Override
    public String toString() {
        return sector.toString();
    }
}
