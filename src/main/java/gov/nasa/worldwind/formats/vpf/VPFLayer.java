/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Renders elements from a VPF database.
 *
 * @author Patrick Murris
 * @version $Id: VPFLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFLayer extends AbstractLayer {
    public static final String LIBRARY_CHANGED = "VPFLayer.LibraryChanged";
    public static final String COVERAGE_CHANGED = "VPFLayer.CoverageChanged";
    protected static final VPFTile NULL_TILE = new VPFTile(-1, "NullTile", new VPFBoundingBox(0, 0, 0, 0));
    // Renderables
    protected final double drawDistance = 1.0e6;
    protected final int maxTilesToDraw = 4;
    protected final boolean drawTileExtents = false;
    protected final List<VPFSymbol> symbols = new ArrayList<>();
    protected final Collection<GeographicText> textObjects = new ArrayList<>();
    protected final Collection<Renderable> renderableObjects = new ArrayList<>();
    // Renderers
    protected final GeographicTextRenderer textRenderer = new GeographicTextRenderer();
    protected final VPFSymbolSupport symbolSupport = new VPFSymbolSupport(GeoSymConstants.GEOSYM, "image/png");
    // Threaded requests
    protected final Queue<Runnable> requestQ = new PriorityBlockingQueue<>(4);
    protected final Queue<Disposable> disposalQ = new ConcurrentLinkedQueue<>();
    // Reference
    protected VPFDatabase db;

    // --- Inner classes ----------------------------------------------------------------------
    protected ArrayList<VPFLibraryRenderable> libraries;

    public VPFLayer() {
        this(null);
    }

    public VPFLayer(VPFDatabase db) {
        this.setName("VPF Layer");
        this.setPickEnabled(false);
        if (db != null)
            this.setVPFDatabase(db);

        this.textRenderer.setCullTextEnabled(true);
        this.textRenderer.setEffect(Keys.TEXT_EFFECT_OUTLINE);
    }

    protected static void sortSymbols(List<VPFSymbol> list) {
        list.sort(new VPFSymbolComparator());
    }

    protected VPFSymbolCollection loadTileSymbols(VPFCoverage coverage, VPFTile tile) {
        VPFPrimitiveDataFactory primitiveDataFactory = new VPFBasicPrimitiveDataFactory(tile);
        VPFPrimitiveData primitiveData = primitiveDataFactory.createPrimitiveData(coverage);

        // The PrimitiveDataFactory returns null when there are no primitive data tables for this coverage tile. We
        // return the constant EMPTY_SYMBOL_COLLECTION to indicate that we have successfully loaded nothing the empty
        // contents of this coverage tile.
        if (primitiveData == null) {
            return VPFSymbolCollection.EMPTY_SYMBOL_COLLECTION;
        }

        VPFBasicSymbolFactory symbolFactory = new VPFBasicSymbolFactory(tile, primitiveData);
        symbolFactory.setStyleSupport(this.symbolSupport);

        ArrayList<VPFSymbol> list = new ArrayList<>();

        // Create coverage renderables for one tile - if tile is null gets all coverage
        VPFFeatureClass[] array = VPFUtils.readFeatureClasses(coverage, new VPFFeatureTableFilter());
        for (VPFFeatureClass cls : array) {
            Collection<? extends VPFSymbol> symbols = cls.createFeatureSymbols(symbolFactory);
            if (symbols != null)
                list.addAll(symbols);
        }

        return new VPFSymbolCollection(list);
    }

    public VPFDatabase getVPFDatabase() {
        return this.db;
    }

    // --- VPF Layer ----------------------------------------------------------------------

    public void setVPFDatabase(VPFDatabase db) {
        this.db = db;
        this.initialize();

        this.db.addPropertyChangeListener(event -> {
            if (event.getPropertyName().equals(VPFLayer.LIBRARY_CHANGED)) {
                VPFLibrary library = (VPFLibrary) event.getSource();
                boolean enabled = (Boolean) event.getNewValue();
                setLibraryEnabled(library, enabled);
            } else if (event.getPropertyName().equals(VPFLayer.COVERAGE_CHANGED)) {
                VPFCoverage coverage = (VPFCoverage) event.getSource();
                boolean enabled = (Boolean) event.getNewValue();
                setCoverageEnabled(coverage, enabled);
            }
        });
    }

    protected void initialize() {
        this.libraries = new ArrayList<>();

        for (VPFLibrary lib : db.getLibraries()) {
            this.libraries.add(new VPFLibraryRenderable(this, lib));
        }
    }

    public void setCoverageEnabled(VPFCoverage coverage, boolean enabled) {
        for (VPFLibraryRenderable lr : this.libraries) {
            lr.setCoverageEnabled(coverage, enabled);
        }
    }

    public void doPreRender(DrawContext dc) {
        // Assemble renderables lists
        this.assembleRenderables(dc);
        // Handle object disposal.
        this.handleDisposal();

        // Pre render renderable objects.
        for (Renderable r : this.renderableObjects) {
            if (r instanceof PreRenderable)
                ((PreRenderable) r).preRender(dc);
        }
    }

    public void doRender(DrawContext dc) {
        for (Renderable r : this.renderableObjects)       // Other renderables
        {
            r.render(dc);
        }

        this.textRenderer.render(dc, this.textObjects);   // Geo text

        if (this.drawTileExtents) {
            for (VPFLibraryRenderable lr : this.libraries) {
                lr.drawTileExtents(dc);
            }
        }
    }

    public void setLibraryEnabled(VPFLibrary library, boolean enabled) {
        VPFLibraryRenderable lr = this.getLibraryRenderable(library);
        if (lr != null)
            lr.enabled = enabled;

        this.emit(Keys.LAYER, null, this);
    }

    public VPFLibraryRenderable getLibraryRenderable(VPFLibrary library) {
        for (VPFLibraryRenderable lr : this.libraries) {
            if (lr.library.getFilePath().equals(library.getFilePath()))
                return lr;
        }
        return null;
    }

    public Iterable<VPFSymbol> getActiveSymbols() {
        return this.symbols;
    }

    protected void assembleRenderables(DrawContext dc) {
        this.symbols.clear();
        this.textObjects.clear();
        this.renderableObjects.clear();

        for (VPFLibraryRenderable lr : this.libraries) {
            lr.assembleSymbols(dc, this.drawDistance, this.maxTilesToDraw);
        }

        VPFLayer.sortSymbols(this.symbols);

        // Dispatch renderable according to its class
        for (VPFSymbol symbol : this.symbols) {
            if (symbol.getMapObject() instanceof GeographicText)
                this.textObjects.add((GeographicText) symbol.getMapObject());
            else if (symbol.getMapObject() instanceof Renderable)
                this.renderableObjects.add((Renderable) symbol.getMapObject());
        }

        this.sendRequests();
        this.requestQ.clear();
    }

    protected void handleDisposal() {
        Disposable disposable;
        while ((disposable = this.disposalQ.poll()) != null) {
            disposable.dispose();
        }
    }

    protected void sendRequests() {
        Runnable task;
        while ((task = this.requestQ.poll()) != null) {
            if (!WorldWind.tasks().isFull()) {
                WorldWind.tasks().addTask(task);
            }
        }
    }

    protected static class VPFLibraryRenderable {
        protected final VPFLayer layer;
        protected final VPFLibrary library;
        protected final Collection<VPFCoverageRenderable> coverages = new ArrayList<>();
        protected final List<VPFTile> currentTiles = new ArrayList<>();
        protected boolean enabled;
        protected VPFCoverageRenderable referenceCoverage;

        public VPFLibraryRenderable(VPFLayer layer, VPFLibrary library) {
            this.layer = layer;
            this.library = library;

            for (VPFCoverage cov : this.library.getCoverages()) {
                if (cov.getName().equalsIgnoreCase(VPFConstants.LIBRARY_REFERENCE_COVERAGE))
                    this.referenceCoverage = new VPFCoverageRenderable(this.layer, cov);
                else
                    this.coverages.add(new VPFCoverageRenderable(this.layer, cov));
            }

            if (this.referenceCoverage != null) {
                this.referenceCoverage.enabled = true;
            }
        }

        public void assembleSymbols(DrawContext dc, double drawDistance, int maxTilesToDraw) {
            if (!this.enabled)
                return;

            this.assembleVisibleTiles(dc, drawDistance, maxTilesToDraw);

            if (this.referenceCoverage != null) {
                this.referenceCoverage.assembleSymbols(null);
            }

            for (VPFCoverageRenderable cr : this.coverages) {
                cr.assembleSymbols((cr.coverage.isTiled() ? this.currentTiles : null));
            }
        }

        public void drawTileExtents(DrawContext dc) {
            for (VPFTile tile : this.currentTiles) {
                Extent extent = tile.getExtent(dc.getGlobe(), dc.getVerticalExaggeration());
                if (extent instanceof Renderable)
                    ((Renderable) extent).render(dc);
            }
        }

        public void setCoverageEnabled(VPFCoverage coverage, boolean enabled) {
            VPFCoverageRenderable cr = this.getCoverageRenderable(coverage);
            if (cr != null)
                cr.enabled = enabled;

            this.layer.emit(Keys.LAYER, null, this.layer);
        }

        public VPFCoverageRenderable getCoverageRenderable(VPFCoverage coverage) {
            for (VPFCoverageRenderable cr : this.coverages) {
                if (cr.coverage.getFilePath().equals(coverage.getFilePath()))
                    return cr;
            }
            return null;
        }

        protected void assembleVisibleTiles(DrawContext dc, double drawDistance, int maxTilesToDraw) {
            this.currentTiles.clear();

            if (!this.library.hasTiledCoverages())
                return;

            Frustum frustum = dc.view().getFrustumInModelCoordinates();
            Vec4 eyePoint = dc.view().getEyePoint();

            for (VPFTile tile : this.library.getTiles()) {
                Extent extent = tile.getExtent(dc.getGlobe(), dc.getVerticalExaggeration());
                double d = extent.getCenter().distanceTo3(eyePoint) - extent.getRadius();

                if (d < drawDistance && frustum.intersects(extent))
                    this.currentTiles.add(tile);
            }

            // Trim down list to four closest tiles
            while (this.currentTiles.size() > maxTilesToDraw) {
                int idx = -1;
                double maxDistance = 0;
                for (int i = 0; i < this.currentTiles.size(); i++) {
                    Extent extent = this.currentTiles.get(i).getExtent(dc.getGlobe(), dc.getVerticalExaggeration());
                    double distance = dc.view().getEyePoint().distanceTo3(extent.getCenter());
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        idx = i;
                    }
                }
                this.currentTiles.remove(idx);
            }
        }
    }

    protected static class VPFCoverageRenderable {
        protected final VPFLayer layer;
        protected final VPFCoverage coverage;
        protected final Map<VPFTile, VPFSymbolCollection> tileCache;
        protected boolean enabled;

        public VPFCoverageRenderable(VPFLayer layer, VPFCoverage coverage) {
            this.layer = layer;
            this.coverage = coverage;
            this.tileCache = Collections.synchronizedMap(new BoundedHashMap<>(6, true) {
                protected boolean removeEldestEntry(Map.Entry<VPFTile, VPFSymbolCollection> eldest) {
                    if (!super.removeEldestEntry(eldest))
                        return false;

                    dispose(eldest.getValue());
                    return true;
                }
            });
        }

        public void assembleSymbols(Iterable<? extends VPFTile> tiles) {
            if (!this.enabled)
                return;

            if (tiles == null) {
                this.doAssembleSymbols(VPFLayer.NULL_TILE);
                return;
            }

            for (VPFTile tile : tiles) {
                this.doAssembleSymbols(tile);
            }
        }

        protected void doAssembleSymbols(VPFTile tile) {
            VPFSymbolCollection symbolCollection = this.tileCache.get(tile);
            if (symbolCollection != null) {
                this.layer.symbols.addAll(symbolCollection.getSymbols());
            } else {
                this.layer.requestQ.add(new RequestTask(this, tile));
            }
        }

        protected void dispose(Disposable renderInfo) {
            this.layer.disposalQ.add(renderInfo);
        }
    }

    protected static class VPFSymbolCollection implements Disposable {
        public static final VPFSymbolCollection EMPTY_SYMBOL_COLLECTION = new VPFSymbolCollection(null);

        protected final Collection<VPFSymbol> symbols = new ArrayList<>();

        public VPFSymbolCollection(Collection<? extends VPFSymbol> symbols) {
            if (symbols != null)
                this.symbols.addAll(symbols);
        }

        public Collection<VPFSymbol> getSymbols() {
            return Collections.unmodifiableCollection(this.symbols);
        }

        public void dispose() {
            for (VPFSymbol s : this.symbols) {
                if (s == null)
                    continue;

                if (s.getMapObject() instanceof Disposable) {
                    ((Disposable) s.getMapObject()).dispose();
                }
            }

            this.symbols.clear();
        }
    }

    protected static class RequestTask implements Runnable, Comparable<RequestTask> {
        protected final VPFCoverageRenderable coverageRenderable;
        protected final VPFTile tile;

        protected RequestTask(VPFCoverageRenderable coverageRenderable, VPFTile tile) {
            this.coverageRenderable = coverageRenderable;
            this.tile = tile;
        }

        public void run() {
            VPFSymbolCollection symbols = this.coverageRenderable.layer.loadTileSymbols(
                this.coverageRenderable.coverage, (this.tile == VPFLayer.NULL_TILE) ? null : this.tile);

            this.coverageRenderable.tileCache.put(this.tile, symbols);
            this.coverageRenderable.layer.emit(Keys.LAYER, null, this.coverageRenderable.layer);
        }

        /**
         * @param that the task to compare
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(RequestTask that) {
            if (that == null) {
                String msg = Logging.getMessage("nullValue.RequestTaskIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            return 0;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            RequestTask that = (RequestTask) o;

            if (!Objects.equals(coverageRenderable, that.coverageRenderable))
                return false;
            //noinspection RedundantIfStatement
            if (!Objects.equals(tile, that.tile))
                return false;

            return true;
        }

        public int hashCode() {
            int result = coverageRenderable != null ? coverageRenderable.hashCode() : 0;
            result = 31 * result + (tile != null ? tile.hashCode() : 0);
            return result;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("coverageRenderable=").append(this.coverageRenderable.coverage.getName());
            sb.append(", tile=").append(this.tile);
            return sb.toString();
        }
    }
}