/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.mercator;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Cylinder;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import static java.lang.Math.toRadians;

/**
 * TiledImageLayer modified 2009-02-03 to add support for Mercator projections.
 *
 * @author tag
 * @version $Id: MercatorTiledImageLayer.java 2053 2014-06-10 20:16:57Z tgaskins $
 */
public abstract class MercatorTiledImageLayer extends AbstractLayer {
    // Infrastructure
    private static final Comparator<MercatorTextureTile> levelComparer = new LevelComparer();
    public final LevelSet levels;
    @SuppressWarnings("FieldCanBeLocal")
    private final double splitScale = 0.9; // TODO: Make configurable
    private final ArrayList<String> supportedImageFormats = new ArrayList<>();
    // Stuff computed each frame
    private final ArrayList<MercatorTextureTile> currentTiles = new ArrayList<>();

    final PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<>(200);

    private ArrayList<MercatorTextureTile> topLevels;
    //private boolean levelZeroLoaded;
    private boolean retainLevelZeroTiles;
    private String tileCountName;
    private boolean useMipMaps;
    // Diagnostic flags
    private boolean showImageTileOutlines;
    private boolean drawTileBoundaries;
    private boolean useTransparentTextures;
    private boolean drawTileIDs;
    private boolean drawBoundingVolumes;
    private MercatorTextureTile currentResourceTile;
    Vec4 referencePoint;
    private boolean atMaxResolution;

    public MercatorTiledImageLayer(LevelSet levelSet) {

        this.levels = new LevelSet(levelSet); // the caller's levelSet may change internally, so we copy it.

        this.createTopLevelTiles();

        this.setPickEnabled(false); // textures are assumed to be terrain unless specifically indicated otherwise.
        this.tileCountName = this.name() + " Tiles";
    }

    private static boolean isTileVisible(DrawContext dc, MercatorTextureTile tile) {
        return tile.getExtent(dc).intersects(
            dc.view().getFrustumInModelCoordinates())
            && (dc.getVisibleSector() == null || dc.getVisibleSector()
            .intersects(tile.sector));
    }

    private static Vec4 computeReferencePoint(DrawContext dc) {
        final Globe g = dc.getGlobe();
        if (dc.getViewportCenterPosition() != null)
            return g.computePointFromPosition(
                dc.getViewportCenterPosition());

        final View view = dc.view();
        Rectangle2D viewport = view.getViewport();
        int x = (int) viewport.getWidth() / 2;
        for (int y = (int) (0.5 * viewport.getHeight()); y >= 0; y--) {
            Position pos = view.computePositionFromScreenPoint(x, y);
            if (pos != null)
                return g.computePointFromPosition(pos.getLat(),
                    pos.getLon(), 0);
        }

        return null;
    }

    private static void drawTileIDs(DrawContext dc,
        Iterable<MercatorTextureTile> tiles) {
        Rectangle viewport = dc.view().getViewport();
        TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(),
            Font.decode("Arial-Plain-13"));

        dc.getGL().glDisable(GL.GL_DEPTH_TEST);
        dc.getGL().glDisable(GL.GL_BLEND);
        dc.getGL().glDisable(GL.GL_TEXTURE_2D);

        textRenderer.setColor(Color.YELLOW);
        textRenderer.beginRendering(viewport.width, viewport.height);
        final Globe g = dc.getGlobe();
        for (MercatorTextureTile tile : tiles) {
            String tileLabel = tile.getLabel();

            if (tile.getFallbackTile() != null)
                tileLabel += '/' + tile.getFallbackTile().getLabel();

            LatLon ll = tile.sector.getCentroid();
            Vec4 pt = g.computePointFromPosition(ll.lat, ll.lon,
                g.elevation(ll.getLat(), ll.getLon()));
            pt = dc.view().project(pt);
            textRenderer.draw(tileLabel, (int) pt.x, (int) pt.y);
        }
        textRenderer.endRendering();
    }

    abstract protected void requestTexture(DrawContext dc, MercatorTextureTile tile);

    @Override
    public void setName(String name) {
        super.setName(name);
        this.tileCountName = this.name() + " Tiles";
    }

    public boolean isUseTransparentTextures() {
        return this.useTransparentTextures;
    }

    public void setUseTransparentTextures(boolean useTransparentTextures) {
        this.useTransparentTextures = useTransparentTextures;
    }

    public boolean isRetainLevelZeroTiles() {
        return retainLevelZeroTiles;
    }

    public void setRetainLevelZeroTiles(boolean retainLevelZeroTiles) {
        this.retainLevelZeroTiles = retainLevelZeroTiles;
    }

    public boolean isDrawTileIDs() {
        return drawTileIDs;
    }

    public void setDrawTileIDs(boolean drawTileIDs) {
        this.drawTileIDs = drawTileIDs;
    }

    public boolean isDrawTileBoundaries() {
        return drawTileBoundaries;
    }

    public void setDrawTileBoundaries(boolean drawTileBoundaries) {
        this.drawTileBoundaries = drawTileBoundaries;
    }

    public boolean isShowImageTileOutlines() {
        return showImageTileOutlines;
    }

    public void setShowImageTileOutlines(boolean showImageTileOutlines) {
        this.showImageTileOutlines = showImageTileOutlines;
    }

    public boolean isDrawBoundingVolumes() {
        return drawBoundingVolumes;
    }

    public void setDrawBoundingVolumes(boolean drawBoundingVolumes) {
        this.drawBoundingVolumes = drawBoundingVolumes;
    }

    protected LevelSet getLevels() {
        return levels;
    }

    public boolean isMultiResolution() {
        return levels != null && levels.getNumLevels() > 1;
    }

    public boolean isAtMaxResolution() {
        return this.atMaxResolution;
    }

    public boolean isUseMipMaps() {
        return useMipMaps;
    }

    public void setUseMipMaps(boolean useMipMaps) {
        this.useMipMaps = useMipMaps;
    }

    private void createTopLevelTiles() {
        MercatorSector sector = (MercatorSector) this.levels.sector;

        Level level = levels.getFirstLevel();
        Angle dLat = level.getTileDelta().getLat();
        Angle dLon = level.getTileDelta().getLon();

        Angle latOrigin = this.levels.tileOrigin.getLat();
        Angle lonOrigin = this.levels.tileOrigin.getLon();

        // Determine the row and column offset from the common WorldWind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.latMin(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.lonMin(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.latMax(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.lonMax(), lonOrigin);

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        this.topLevels = new ArrayList<>(nLatTiles * nLonTiles);

        double deltaLat = dLat.degrees / 90;
        double d1 = -1.0 + deltaLat * firstRow;
        for (int row = firstRow; row <= lastRow; row++) {
            double d2 = d1 + deltaLat;

            Angle t1 = Tile.columnLon(firstCol, dLon, lonOrigin);
            for (int col = firstCol; col <= lastCol; col++) {
                Angle t2 = t1.add(dLon);

                this.topLevels.add(new MercatorTextureTile(new MercatorSector(
                    d1, d2, t1, t2), level, row, col));
                t1 = t2;
            }
            d1 = d2;
        }
    }

    private void assembleTiles(DrawContext dc) {
        this.currentTiles.clear();

        for (MercatorTextureTile tile : this.topLevels) {
            if (MercatorTiledImageLayer.isTileVisible(dc, tile)) {
                this.currentResourceTile = null;
                this.addTileOrDescendants(dc, tile);
            }
        }
    }

    private void addTileOrDescendants(DrawContext dc, MercatorTextureTile tile) {
        if (this.meetsRenderCriteria(dc, tile)) {
            this.addTile(dc, tile);
            return;
        }

        // The incoming tile does not meet the rendering criteria, so it must be subdivided and those
        // subdivisions tested against the criteria.

        // All tiles that meet the selection criteria are drawn, but some of those tiles will not have
        // textures associated with them either because their texture isn't loaded yet or because they
        // are finer grain than the layer has textures for. In these cases the tiles use the texture of
        // the closest ancestor that has a texture loaded. This ancestor is called the currentResourceTile.
        // A texture transform is applied during rendering to align the sector's texture coordinates with the
        // appropriate region of the ancestor's texture.

        MercatorTextureTile ancestorResource = null;

        try {
            // TODO: Revise this to reflect that the parent layer is only requested while the algorithm continues
            // to search for the layer matching the criteria.
            // At this point the tile does not meet the render criteria but it may have its texture in memory.
            // If so, register this tile as the resource tile. If not, then this tile will be the next level
            // below a tile with texture in memory. So to provide progressive resolution increase, add this tile
            // to the draw list. That will cause the tile to be drawn using its parent tile's texture, and it will
            // cause it's texture to be requested. At some future call to this method the tile's texture will be in
            // memory, it will not meet the render criteria, but will serve as the parent to a tile that goes
            // through this same process as this method recurses. The result of all this is that a tile isn't rendered
            // with its own texture unless all its parents have their textures loaded. In addition to causing
            // progressive resolution increase, this ensures that the parents are available as the user zooms out, and
            // therefore the layer remains visible until the user is zoomed out to the point the layer is no longer
            // active.
            if (tile.isTextureInMemory(dc.gpuCache())
                || tile.getLevelNumber() == 0) {
                ancestorResource = this.currentResourceTile;
                this.currentResourceTile = tile;
            } else if (!tile.level.isEmpty()) {
                //                this.addTile(dc, tile);
                //                return;

                // Issue a request for the parent before descending to the children.
                if (tile.getLevelNumber() < this.levels.getNumLevels()) {
                    // Request only tiles with data associated at this level
                    if (!this.levels.missing(tile))
                        this.requestTexture(dc, tile);
                }
            }

            MercatorTextureTile[] subTiles = tile.subTiles(this.levels
                .getLevel(tile.getLevelNumber() + 1));
            for (MercatorTextureTile child : subTiles) {
                if (MercatorTiledImageLayer.isTileVisible(dc, child))
                    this.addTileOrDescendants(dc, child);
            }
        }
        finally {
            if (ancestorResource != null) // Pop this tile as the currentResource ancestor
                this.currentResourceTile = ancestorResource;
        }
    }

    private void addTile(DrawContext dc, MercatorTextureTile tile) {
        tile.setFallbackTile(null);

        final GpuResourceCache texCache = dc.gpuCache();

        if (tile.isTextureInMemory(texCache)) {
            this.addTileToCurrent(tile);
            return;
        }

//        // Level 0 loads may be forced
//        if (tile.getLevelNumber() == 0 && this.forceLevelZeroLoads
//            && !tile.isTextureInMemory(texCache)) {
//            this.forceTextureLoad(tile);
//            if (tile.isTextureInMemory(texCache)) {
//                this.addTileToCurrent(tile);
//                return;
//            }
//        }

//        // Level 0 loads may be forced
//        if (tile.getLevelNumber() == 0 && this.forceLevelZeroLoads
//            && !tile.isTextureInMemory(texCache)) {
//            this.forceTextureLoad(tile);
//            if (tile.isTextureInMemory(texCache)) {
//                this.addTileToCurrent(tile);
//                return;
//            }
//        }

        // Tile's texture isn't available, so request it
        if (tile.getLevelNumber() < this.levels.getNumLevels()) {
            // Request only tiles with data associated at this level
            if (!this.levels.missing(tile))
                this.requestTexture(dc, tile);
        }

        // Set up to use the currentResource tile's texture
        if (this.currentResourceTile != null) {

            if (this.currentResourceTile.isTextureInMemory(texCache)) {
                tile.setFallbackTile(currentResourceTile);
                this.addTileToCurrent(tile);
            }
        }
    }

    private void addTileToCurrent(MercatorTextureTile tile) {
        this.currentTiles.add(tile);
    }

    private boolean meetsRenderCriteria(DrawContext dc, MercatorTextureTile tile) {
        return this.levels.isFinalLevel(tile.getLevelNumber())
            || !needToSplit(dc, tile.sector);
    }

    // ============== Rendering ======================= //
    // ============== Rendering ======================= //
    // ============== Rendering ======================= //

    private boolean needToSplit(DrawContext dc, Sector sector) {
        Vec4[] corners = sector.computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());
        Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe(), dc.getVerticalExaggeration());

        View view = dc.view();
        final Vec4 eye = view.getEyePoint();
        double d1 = eye.distanceTo3(corners[0]);
        double d2 = eye.distanceTo3(corners[1]);
        double d3 = eye.distanceTo3(corners[2]);
        double d4 = eye.distanceTo3(corners[3]);
        double d5 = eye.distanceTo3(centerPoint);

        double minDistance = d1;
        if (d2 < minDistance)
            minDistance = d2;
        if (d3 < minDistance)
            minDistance = d3;
        if (d4 < minDistance)
            minDistance = d4;
        if (d5 < minDistance)
            minDistance = d5;

        double cellSize = (Math.PI * toRadians(sector.latDelta) * dc
            .getGlobe().getRadius()) / 20; // TODO

        return Math.log10(cellSize) > (Math.log10(minDistance) - this.splitScale);
    }

    private boolean atMaxLevel(DrawContext dc) {
        final LevelSet levels = this.levels;
        if (levels == null || dc.view() == null)
            return false;

        Position vpc = dc.getViewportCenterPosition();
        if (vpc == null)
            return false;

        if (!levels.sector.contains(vpc.getLat(), vpc.getLon()))
            return true;

        Level nextToLast = levels.getNextToLastLevel();
        if (nextToLast == null)
            return true;

        Sector centerSector = nextToLast.computeSectorForPosition(vpc.getLat(), vpc.getLon(),
            levels.tileOrigin);
        return this.needToSplit(dc, centerSector);
    }

    @Override
    public void render(DrawContext dc) {
        this.atMaxResolution = this.atMaxLevel(dc);
        super.render(dc);
    }

    @Override
    protected final void doRender(DrawContext dc) {

        final SectorGeometryList surfaceGeometry = dc.getSurfaceGeometry();
        if (surfaceGeometry == null || surfaceGeometry.size() < 1)
            return;

        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(this.showImageTileOutlines);

        draw(dc);
    }

    private void draw(DrawContext dc) {
        this.referencePoint = MercatorTiledImageLayer.computeReferencePoint(dc);

        this.assembleTiles(dc); // Determine the tiles to draw.

        if (this.currentTiles.size() >= 1) {
            MercatorTextureTile[] sortedTiles = new MercatorTextureTile[this.currentTiles
                .size()];
            sortedTiles = this.currentTiles.toArray(sortedTiles);
            Arrays.sort(sortedTiles, MercatorTiledImageLayer.levelComparer);

            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

            if (this.isUseTransparentTextures() || this.getOpacity() < 1) {
                gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT
                    | GL2.GL_CURRENT_BIT);
                gl.glColor4d(1.0d, 1.0d, 1.0d, this.getOpacity());
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT);
            }

            gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT,
                this.tileCountName, this.currentTiles.size());
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc,
                this.currentTiles);

            gl.glPopAttrib();

            if (this.drawTileIDs)
                MercatorTiledImageLayer.drawTileIDs(dc, this.currentTiles);

            if (this.drawBoundingVolumes)
                this.drawBoundingVolumes(dc, this.currentTiles);

            this.currentTiles.clear();
        }

        WorldWind.tasks().drain(requestQ);
    }

    public boolean isLayerInView(DrawContext dc) {

        if (dc.view() == null) {
            String message = Logging
                .getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return !(dc.getVisibleSector() != null && !this.levels.sector
            .intersects(dc.getVisibleSector()));
    }

    private void drawBoundingVolumes(DrawContext dc,
        Iterable<MercatorTextureTile> tiles) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        float[] previousColor = new float[4];
        gl.glGetFloatv(GL2.GL_CURRENT_COLOR, previousColor, 0);
        gl.glColor3d(0, 1, 0);

        for (MercatorTextureTile tile : tiles) {
            ((Renderable) tile.getExtent(dc)).render(dc);
        }

        Cylinder c = Sector.computeBoundingCylinder(dc.getGlobe(), dc.getVerticalExaggeration(),
            this.levels.sector);
        gl.glColor3d(1, 1, 0);
        c.render(dc);

        gl.glColor4fv(previousColor, 0);
    }

    public List<String> getAvailableImageFormats() {
        return new ArrayList<>(this.supportedImageFormats);
    }

    public int countImagesInSector(Sector sector, int levelNumber) {
        if (sector == null) {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level targetLevel = this.levels.getLastLevel();
        if (levelNumber >= 0) {
            for (int i = levelNumber; i < levels.getLastLevel()
                .getLevelNumber(); i++) {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        Angle latOrigin = this.levels.tileOrigin.getLat();
        Angle lonOrigin = this.levels.tileOrigin.getLon();
        final int nwRow = Tile.computeRow(delta.getLat(), sector
            .latMax(), latOrigin);
        final int nwCol = Tile.computeColumn(delta.getLon(), sector
            .lonMin(), lonOrigin);
        final int seRow = Tile.computeRow(delta.getLat(), sector
            .latMin(), latOrigin);
        final int seCol = Tile.computeColumn(delta.getLon(), sector
            .lonMax(), lonOrigin);

        int numRows = nwRow - seRow + 1;
        int numCols = seCol - nwCol + 1;

        return numRows * numCols;
    }

    private MercatorTextureTile[][] getTilesInSector(Sector sector,
        int levelNumber) {
        if (sector == null) {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level targetLevel = this.levels.getLastLevel();
        if (levelNumber >= 0) {
            for (int i = levelNumber; i < levels.getLastLevel()
                .getLevelNumber(); i++) {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        Angle latOrigin = this.levels.tileOrigin.getLat();
        Angle lonOrigin = this.levels.tileOrigin.getLon();
        final int nwRow = Tile.computeRow(delta.getLat(), sector
            .latMax(), latOrigin);
        final int nwCol = Tile.computeColumn(delta.getLon(), sector
            .lonMin(), lonOrigin);
        final int seRow = Tile.computeRow(delta.getLat(), sector
            .latMin(), latOrigin);
        final int seCol = Tile.computeColumn(delta.getLon(), sector
            .lonMax(), lonOrigin);

        int numRows = nwRow - seRow + 1;
        int numCols = seCol - nwCol + 1;
        MercatorTextureTile[][] sectorTiles = new MercatorTextureTile[numRows][numCols];

        for (int row = nwRow; row >= seRow; row--) {
            for (int col = nwCol; col <= seCol; col++) {
                TileKey key = new TileKey(targetLevel.getLevelNumber(), row,
                    col, targetLevel.getCacheName());
                Sector tileSector = this.levels.computeSectorForKey(key);
                MercatorSector mSector = MercatorSector.fromSector(tileSector); //TODO: check
                sectorTiles[nwRow - row][col - nwCol] = new MercatorTextureTile(
                    mSector, targetLevel, row, col);
            }
        }

        return sectorTiles;
    }

    private static class LevelComparer implements
        Comparator<MercatorTextureTile> {
        public int compare(MercatorTextureTile ta, MercatorTextureTile tb) {
            if (ta==tb) return 0;

            int la = ta.getFallbackTile() == null ? ta.getLevelNumber() : ta
                .getFallbackTile().getLevelNumber();
            int lb = tb.getFallbackTile() == null ? tb.getLevelNumber() : tb
                .getFallbackTile().getLevelNumber();

            return Integer.compare(la, lb);
        }
    }
}