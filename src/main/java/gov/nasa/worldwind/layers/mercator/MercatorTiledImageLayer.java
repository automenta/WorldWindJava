/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.mercator;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.Cylinder;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
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
    private final LevelSet levels;
    @SuppressWarnings("FieldCanBeLocal")
    private final double splitScale = 0.9; // TODO: Make configurable
    private final ArrayList<String> supportedImageFormats = new ArrayList<>();
    // Stuff computed each frame
    private final ArrayList<MercatorTextureTile> currentTiles = new ArrayList<>();
    private final PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<>(
        200);
    private ArrayList<MercatorTextureTile> topLevels;
    private boolean forceLevelZeroLoads;
    private boolean levelZeroLoaded;
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
    private Vec4 referencePoint;
    private boolean atMaxResolution;

    public MercatorTiledImageLayer(LevelSet levelSet) {
        if (levelSet == null) {
            String message = Logging.getMessage("nullValue.LevelSetIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.levels = new LevelSet(levelSet); // the caller's levelSet may change internally, so we copy it.

        this.createTopLevelTiles();

        this.setPickEnabled(false); // textures are assumed to be terrain unless specifically indicated otherwise.
        this.tileCountName = this.name() + " Tiles";
    }

    private static boolean isTileVisible(DrawContext dc, MercatorTextureTile tile) {
        return tile.getExtent(dc).intersects(
            dc.getView().getFrustumInModelCoordinates())
            && (dc.getVisibleSector() == null || dc.getVisibleSector()
            .intersects(tile.sector));
    }

    private static Vec4 computeReferencePoint(DrawContext dc) {
        if (dc.getViewportCenterPosition() != null)
            return dc.getGlobe().computePointFromPosition(
                dc.getViewportCenterPosition());

        final View view = dc.getView();
        Rectangle2D viewport = view.getViewport();
        int x = (int) viewport.getWidth() / 2;
        for (int y = (int) (0.5 * viewport.getHeight()); y >= 0; y--) {
            Position pos = view.computePositionFromScreenPoint(x, y);
            if (pos != null)
                return dc.getGlobe().computePointFromPosition(pos.getLatitude(),
                    pos.getLongitude(), 0);
        }

        return null;
    }

    private static void drawTileIDs(DrawContext dc,
        Iterable<MercatorTextureTile> tiles) {
        Rectangle viewport = dc.getView().getViewport();
        TextRenderer textRenderer = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(),
            Font.decode("Arial-Plain-13"));

        dc.getGL().glDisable(GL.GL_DEPTH_TEST);
        dc.getGL().glDisable(GL.GL_BLEND);
        dc.getGL().glDisable(GL.GL_TEXTURE_2D);

        textRenderer.setColor(Color.YELLOW);
        textRenderer.beginRendering(viewport.width, viewport.height);
        for (MercatorTextureTile tile : tiles) {
            String tileLabel = tile.getLabel();

            if (tile.getFallbackTile() != null)
                tileLabel += '/' + tile.getFallbackTile().getLabel();

            LatLon ll = tile.sector.getCentroid();
            Vec4 pt = dc.getGlobe().computePointFromPosition(
                ll.getLatitude(),
                ll.getLongitude(),
                dc.getGlobe().getElevation(ll.getLatitude(),
                    ll.getLongitude()));
            pt = dc.getView().project(pt);
            textRenderer.draw(tileLabel, (int) pt.x, (int) pt.y);
        }
        textRenderer.endRendering();
    }

    abstract protected void requestTexture(DrawContext dc, MercatorTextureTile tile);

    abstract protected void forceTextureLoad(MercatorTextureTile tile);

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

    public boolean isForceLevelZeroLoads() {
        return this.forceLevelZeroLoads;
    }

    public void setForceLevelZeroLoads(boolean forceLevelZeroLoads) {
        this.forceLevelZeroLoads = forceLevelZeroLoads;
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

    protected PriorityBlockingQueue<Runnable> getRequestQ() {
        return requestQ;
    }

    public boolean isMultiResolution() {
        return this.getLevels() != null && this.getLevels().getNumLevels() > 1;
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
        MercatorSector sector = (MercatorSector) this.levels.getSector();

        Level level = levels.getFirstLevel();
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();

        Angle latOrigin = this.levels.getTileOrigin().getLatitude();
        Angle lonOrigin = this.levels.getTileOrigin().getLongitude();

        // Determine the row and column offset from the common WorldWind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.latMin(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.lonMin(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.latMax(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.lonMax(), lonOrigin);

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        this.topLevels = new ArrayList<>(nLatTiles * nLonTiles);

        //Angle p1 = Tile.computeRowLatitude(firstRow, dLat);
        double deltaLat = dLat.degrees / 90;
        double d1 = -1.0 + deltaLat * firstRow;
        for (int row = firstRow; row <= lastRow; row++) {
            double d2 = d1 + deltaLat;

            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);
            for (int col = firstCol; col <= lastCol; col++) {
                Angle t2 = t1.add(dLon);

                this.topLevels.add(new MercatorTextureTile(new MercatorSector(
                    d1, d2, t1, t2), level, row, col));
                t1 = t2;
            }
            d1 = d2;
        }
    }

    private void loadAllTopLevelTextures(DrawContext dc) {
        for (MercatorTextureTile tile : this.topLevels) {
            if (!tile.isTextureInMemory(dc.getTextureCache()))
                this.forceTextureLoad(tile);
        }

        this.levelZeroLoaded = true;
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
            if (tile.isTextureInMemory(dc.getTextureCache())
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

            MercatorTextureTile[] subTiles = tile.createSubTiles(this.levels
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

        if (tile.isTextureInMemory(dc.getTextureCache())) {
            this.addTileToCurrent(tile);
            return;
        }

        // Level 0 loads may be forced
        if (tile.getLevelNumber() == 0 && this.forceLevelZeroLoads
            && !tile.isTextureInMemory(dc.getTextureCache())) {
            this.forceTextureLoad(tile);
            if (tile.isTextureInMemory(dc.getTextureCache())) {
                this.addTileToCurrent(tile);
                return;
            }
        }

        // Tile's texture isn't available, so request it
        if (tile.getLevelNumber() < this.levels.getNumLevels()) {
            // Request only tiles with data associated at this level
            if (!this.levels.missing(tile))
                this.requestTexture(dc, tile);
        }

        // Set up to use the currentResource tile's texture
        if (this.currentResourceTile != null) {
            if (this.currentResourceTile.getLevelNumber() == 0
                && this.forceLevelZeroLoads
                && !this.currentResourceTile.isTextureInMemory(dc
                .getTextureCache())
                && !this.currentResourceTile.isTextureInMemory(dc
                .getTextureCache()))
                this.forceTextureLoad(this.currentResourceTile);

            if (this.currentResourceTile
                .isTextureInMemory(dc.getTextureCache())) {
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

        View view = dc.getView();
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
        final LevelSet levels = this.getLevels();
        if (levels == null || dc.getView() == null)
            return false;

        Position vpc = dc.getViewportCenterPosition();
        if (vpc == null)
            return false;

        if (!levels.getSector().contains(vpc.getLatitude(), vpc.getLongitude()))
            return true;

        Level nextToLast = levels.getNextToLastLevel();
        if (nextToLast == null)
            return true;

        Sector centerSector = nextToLast.computeSectorForPosition(vpc.getLatitude(), vpc.getLongitude(),
            levels.getTileOrigin());
        return this.needToSplit(dc, centerSector);
    }

    @Override
    public void render(DrawContext dc) {
        this.atMaxResolution = this.atMaxLevel(dc);
        super.render(dc);
    }

    @Override
    protected final void doRender(DrawContext dc) {
        if (this.forceLevelZeroLoads && !this.levelZeroLoaded)
            this.loadAllTopLevelTextures(dc);

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

        if (dc.getView() == null) {
            String message = Logging
                .getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return !(dc.getVisibleSector() != null && !this.levels.getSector()
            .intersects(dc.getVisibleSector()));
    }

    protected Vec4 getReferencePoint() {
        return this.referencePoint;
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
            this.levels.getSector());
        gl.glColor3d(1, 1, 0);
        c.render(dc);

        gl.glColor4fv(previousColor, 0);
    }

    public List<String> getAvailableImageFormats() {
        return new ArrayList<>(this.supportedImageFormats);
    }

    // ============== Image Composition ======================= //
    // ============== Image Composition ======================= //
    // ============== Image Composition ======================= //

    protected void setAvailableImageFormats(String[] formats) {
        this.supportedImageFormats.clear();

        if (formats != null) {
            this.supportedImageFormats.addAll(Arrays.asList(formats));
        }
    }

    public boolean isImageFormatAvailable(String imageFormat) {
        return imageFormat != null
            && this.supportedImageFormats.contains(imageFormat);
    }

    public String getDefaultImageFormat() {
        return this.supportedImageFormats.isEmpty() ? null : this.supportedImageFormats
            .get(0);
    }

    private BufferedImage requestImage(MercatorTextureTile tile, String mimeType)
        throws URISyntaxException {
        String pathBase = tile.getPath().substring(0,
            tile.getPath().lastIndexOf('.'));
        String suffix = WWIO.makeSuffixForMimeType(mimeType);
        String path = pathBase + suffix;
        URL url = this.getDataFileStore().findFile(path, false);

        if (url == null) // image is not local
            return null;

        if (WWIO.isFileOutOfDate(url, tile.level.getExpiryTime())) {
            // The file has expired. Delete it.
            this.getDataFileStore().removeFile(url);
            String message = Logging.getMessage("generic.DataFileExpired", url);
            Logging.logger().fine(message);
        } else {
            try {
                File imageFile = new File(url.toURI());
                BufferedImage image = ImageIO.read(imageFile);
                if (image == null) {
                    String message = Logging.getMessage(
                        "generic.ImageReadFailed", imageFile);
                    throw new RuntimeException(message);
                }

                this.levels.has(tile);
                return image;
            }
            catch (IOException e) {
                // Assume that something's wrong with the file and delete it.
                this.getDataFileStore().removeFile(url);
                this.levels.miss(tile);
                String message = Logging.getMessage(
                    "generic.DeletedCorruptDataFile", url);
                Logging.logger().info(message);
            }
        }

        return null;
    }

    private void downloadImage(final MercatorTextureTile tile, String mimeType)
        throws Exception, MalformedURLException, RuntimeException {
        //        System.out.println(tile.getPath());
        final URL resourceURL = tile.getResourceURL(mimeType);
        Retriever retriever;

        String protocol = resourceURL.getProtocol();

        if ("http".equalsIgnoreCase(protocol)) {
            retriever = new HTTPRetriever(resourceURL, new HttpRetrievalPostProcessor(tile));
            retriever.set(URLRetriever.EXTRACT_ZIP_ENTRY, "true"); // supports legacy layers
        } else {
            String message = Logging
                .getMessage("layers.TextureLayer.UnknownRetrievalProtocol",
                    resourceURL);
            throw new RuntimeException(message);
        }

        retriever.setConnectTimeout(10000);
        retriever.setReadTimeout(20000);
        retriever.call();
    }

    public int computeLevelForResolution(Sector sector, Globe globe,
        double resolution) {

        double texelSize = 0;
        Level targetLevel = this.levels.getLastLevel();
        for (int i = 0; i < this.getLevels().getLastLevel().getLevelNumber(); i++) {
            if (this.levels.isLevelEmpty(i))
                continue;

            texelSize = this.levels.getLevel(i).getTexelSize();
            if (texelSize > resolution)
                continue;

            targetLevel = this.levels.getLevel(i);
            break;
        }

        Logging.logger().info(
            Logging.getMessage("layers.TiledImageLayer.LevelSelection",
                targetLevel.getLevelNumber(), texelSize));
        return targetLevel.getLevelNumber();
    }

    public BufferedImage composeImageForSector(Sector sector, int imageWidth,
        int imageHeight, int levelNumber, String mimeType,
        boolean abortOnError, BufferedImage image) {

        if (levelNumber < 0) {
            levelNumber = this.levels.getLastLevel().getLevelNumber();
        } else if (levelNumber > this.levels.getLastLevel().getLevelNumber()) {
            Logging.logger().warning(
                Logging.getMessage(
                    "generic.LevelRequestedGreaterThanMaxLevel",
                    levelNumber, this.levels.getLastLevel()
                        .getLevelNumber()));
            levelNumber = this.levels.getLastLevel().getLevelNumber();
        }

        MercatorTextureTile[][] tiles = this.getTilesInSector(sector,
            levelNumber);

        if (tiles.length == 0 || tiles[0].length == 0) {
            Logging
                .logger()
                .severe(
                    Logging
                        .getMessage("layers.TiledImageLayer.NoImagesAvailable"));
            return null;
        }

        if (image == null)
            image = new BufferedImage(imageWidth, imageHeight,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();

        for (MercatorTextureTile[] row : tiles) {
            for (MercatorTextureTile tile : row) {
                if (tile == null)
                    continue;

                BufferedImage tileImage;
                try {
                    tileImage = this.getImage(tile, mimeType);

                    double sh = ((double) imageHeight / tileImage
                        .getHeight())
                        * (tile.sector.latDelta().divide(sector.latDelta()));
                    double sw = ((double) imageWidth / tileImage
                        .getWidth())
                        * (tile.sector.lonDelta().divide(sector.lonDelta()));

                    double dh = imageHeight
                        * (-tile.sector.latMax().sub(
                        sector.latMax()).degrees / sector.latDelta().degrees);
                    double dw = imageWidth
                        * (tile.sector.lonMin().sub(
                        sector.lonMin()).degrees / sector.lonDelta().degrees);

                    AffineTransform txf = g.getTransform();
                    g.translate(dw, dh);
                    g.scale(sw, sh);
                    g.drawImage(tileImage, 0, 0, null);
                    g.setTransform(txf);
                }
                catch (Exception e) {
                    if (abortOnError)
                        throw new RuntimeException(e);

                    String message = Logging.getMessage(
                        "generic.ExceptionWhileRequestingImage", tile
                            .getPath());
                    Logging.logger().log(java.util.logging.Level.WARNING,
                        message, e);
                }
            }
        }

        return image;
    }

    public int countImagesInSector(Sector sector, int levelNumber) {
        if (sector == null) {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Level targetLevel = this.levels.getLastLevel();
        if (levelNumber >= 0) {
            for (int i = levelNumber; i < this.getLevels().getLastLevel()
                .getLevelNumber(); i++) {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        Angle latOrigin = this.levels.getTileOrigin().getLatitude();
        Angle lonOrigin = this.levels.getTileOrigin().getLongitude();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector
            .latMax(), latOrigin);
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector
            .lonMin(), lonOrigin);
        final int seRow = Tile.computeRow(delta.getLatitude(), sector
            .latMin(), latOrigin);
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector
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
            for (int i = levelNumber; i < this.getLevels().getLastLevel()
                .getLevelNumber(); i++) {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        Angle latOrigin = this.levels.getTileOrigin().getLatitude();
        Angle lonOrigin = this.levels.getTileOrigin().getLongitude();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector
            .latMax(), latOrigin);
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector
            .lonMin(), lonOrigin);
        final int seRow = Tile.computeRow(delta.getLatitude(), sector
            .latMin(), latOrigin);
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector
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

    private BufferedImage getImage(MercatorTextureTile tile, String mimeType)
        throws Exception, URISyntaxException, RuntimeException {
        // Read the image from disk.
        BufferedImage image = this.requestImage(tile, mimeType);
        if (image != null)
            return image;

        // Retrieve it from the net since it's not on disk.
        this.downloadImage(tile, mimeType);

        // Try to read from disk again after retrieving it from the net.
        image = this.requestImage(tile, mimeType);
        if (image == null) {
            String message = Logging.getMessage(
                "layers.TiledImageLayer.ImageUnavailable", tile.getPath());
            throw new RuntimeException(message);
        }

        return image;
    }

    private static class LevelComparer implements
        Comparator<MercatorTextureTile> {
        public int compare(MercatorTextureTile ta, MercatorTextureTile tb) {
            int la = ta.getFallbackTile() == null ? ta.getLevelNumber() : ta
                .getFallbackTile().getLevelNumber();
            int lb = tb.getFallbackTile() == null ? tb.getLevelNumber() : tb
                .getFallbackTile().getLevelNumber();

            return Integer.compare(la, lb);
        }
    }

    private class HttpRetrievalPostProcessor implements RetrievalPostProcessor {
        private final MercatorTextureTile tile;

        public HttpRetrievalPostProcessor(MercatorTextureTile tile) {
            this.tile = tile;
        }

        public ByteBuffer run(Retriever retriever) {
            if (!retriever.getState().equals(
                Retriever.RETRIEVER_STATE_SUCCESSFUL))
                return null;

            HTTPRetriever htr = (HTTPRetriever) retriever;
            if (htr.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
                // Mark tile as missing to avoid excessive attempts
                MercatorTiledImageLayer.this.levels.miss(tile);
                return null;
            }

            if (htr.getResponseCode() != HttpURLConnection.HTTP_OK)
                return null;

            ByteBuffer buffer = retriever.getBuffer();

            String suffix = WWIO.makeSuffixForMimeType(htr.getContentType());

            String path = tile.getPath().substring(0,
                tile.getPath().lastIndexOf('.'));
            path += suffix;

            final File outFile = getDataFileStore().newFile(path);
            if (outFile == null)
                return null;

            try {
                WWIO.saveBuffer(buffer, outFile);
                return buffer;
            }
            catch (IOException e) {
                e.printStackTrace(); // TODO: log error
                return null;
            }
        }
    }
}
