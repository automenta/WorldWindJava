/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.Collection;

/**
 * Holds a collection of Renderables and manages local caching of them. Provides searching for Renderables by sector,
 * location or name.
 * <p>
 * NOTE: This class is experimental and not fully implemented. You should not use it now.
 *
 * @author tag
 * @version $Id: CachedRenderableLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class CachedRenderableLayer extends AbstractLayer {
    protected static final int DEFAULT_DEPTH = 4;

    protected final BasicQuadTree<Renderable> extentTree;
    // this is used until we work out the caching and retrieval scheme
    protected final PickSupport pickSupport = new PickSupport();

    /**
     * Constructs a layer instance.
     *
     * @param coverage the geographic area covered by the layer's Renderables.
     * @throws IllegalArgumentException if the coverage sector is null.
     */
    public CachedRenderableLayer(Sector coverage) {
        // Extent tree checks args
        this.extentTree = new BasicQuadTree<>(CachedRenderableLayer.DEFAULT_DEPTH, coverage, null);
    }

    public CachedRenderableLayer() {
        this((Sector) null);
    }

    /**
     * Constructs a layer instance.
     *
     * @param coverage  the geographic area covered by the layer's Renderables.
     * @param numLevels the depth of the tree used to sort the Renderables.
     * @throws IllegalArgumentException if the coverage sector is null or the number of levels is less than 1;
     */
    public CachedRenderableLayer(Sector coverage, int numLevels) {
        // Extent tree checks args
        this.extentTree = new BasicQuadTree<>(numLevels, coverage, null);
    }

    public CachedRenderableLayer(RenderableLayer l) {
//        this(Sector.union(l.renderables.stream()
//            .flatMap(z -> ((AbstractSurfaceRenderable)z).getSectors(dc))::iterator))
        this();
        for (Renderable x : l.renderables) {
            if (x instanceof GeographicExtent)
                this.add(x);
        }
    }

    protected static void doPreRender(DrawContext dc, Iterable<? extends Renderable> renderables) {
        for (Renderable renderable : renderables) {
//            try {
            // If the caller has specified their own Iterable,
            // then we cannot make any guarantees about its contents.
            if (renderable instanceof PreRenderable)
                ((PreRenderable) renderable).preRender(dc);
//            }
//            catch (Exception e) {
//                String msg = Logging.getMessage("generic.ExceptionWhilePrerenderingRenderable");
//                Logging.logger().severe(msg);
//                // continue to next renderable
//            }
        }
    }

    protected static void doRender(DrawContext dc, Iterable<? extends Renderable> renderables) {
        for (Renderable renderable : renderables) {
//            try {
            // If the caller has specified their own Iterable,
            // then we cannot make any guarantees about its contents.
//                if (renderable != null)
            renderable.render(dc);
//            }
//            catch (Exception e) {
//                String msg = Logging.getMessage("generic.ExceptionWhileRenderingRenderable");
//                Logging.logger().log(Level.SEVERE, msg, e);
//                // continue to next renderable
//            }
        }
    }

    /**
     * Indictes whether the layer contains Renderables.
     *
     * @return true if the layer contains Renderables, otherwise false.
     */
    public boolean hasItems() {
        return this.extentTree.hasItems();
    }

    /**
     * Add a Renderable to the layer.
     *
     * @param item the Renderable to add.
     * @throws IllegalArgumentException if the item is null or does not implement {@link GeographicExtent}.
     * @see #add(Renderable, String)
     */
    public void add(Renderable item) {
        this.add(item, null); // extent tree checks args
    }

    /**
     * Adds a named Renderable to the layer. The Renderable can subsequently participate in a name search of the layer.
     *
     * @param item the Renderable to add.
     * @param name a name for the Renderable. May be null, in which case the item has no name.
     * @throws IllegalArgumentException if the item is null or does not implement {@link GeographicExtent}.
     * @see #add(Renderable)
     */
    public void add(Renderable item, String name) {
        if (!(item instanceof GeographicExtent)) {
            String message = Logging.getMessage("GeographicTree.NotGeometricExtent");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // extent tree checks args
        this.extentTree.add(item, ((GeographicExtent) item).getSector().asDegreesArray(), name);
    }

    /**
     * Remove a Renderable from the layer if the Renderable is in the layer.
     *
     * @param item the Renderable to remove
     * @see #removeByName(String)
     */
    public void remove(Renderable item) {
        // extent tree checks args
        this.extentTree.remove(item);
    }

    /**
     * Remove a named Renderable from the layer if it is in the layer.
     *
     * @param name the name of the Renderable to remove. If null, no Renderable is removed.
     * @see #remove(Renderable)
     */
    public void removeByName(String name) {
        this.extentTree.removeByName(name);
    }

    /**
     * Returns all Renderables at a specfied location.
     *
     * @param location the location of interest.
     * @return the Collection of Renderables at the specified location.
     * @see #getRenderables(Sector)
     * @see #getAllRenderables()
     */
    public Collection<? extends Renderable> getRenderables(LatLon location) {
        // extent tree checks args
        return this.extentTree.getItemsAtLocation(location, null);
    }

    /**
     * Returns all Renderables within or intersecting a specified sector.
     *
     * @param extent the location of interest.
     * @return the Collection of Renderables within or intersecting the boundary of the sector.
     * @see #getRenderables(LatLon)
     * @see #getAllRenderables()
     */
    public Collection<? extends Renderable> getRenderables(Sector extent) {
        // extent tree checks args
        return this.extentTree.getItemsInRegion(extent, null);
    }

    /**
     * Returns all Renderables in the layer.
     *
     * @return an Iterable over all the Renderables in the layer.
     */
    public Iterable<? extends Renderable> getAllRenderables() {
        return this.extentTree; // the tree is an Iterable
    }

    /**
     * Searches the layer for a named Renderable.
     *
     * @param name the name to search for. If null, no search is performed and null is returned.
     * @return the Renderable of the given name, or null if no Renderable with the name is in the layer.
     */
    public Renderable getByName(String name) {
        return this.extentTree.getByName(name);
    }

    /**
     * Disposes any Renderables implementing @{link Dispose} and removes all Renderables from the layer.
     */
    public void dispose() {
        this.disposeRenderables();
    }

    protected void disposeRenderables() {
        for (Renderable renderable : this.getAllRenderables()) {
//            try {
            if (renderable instanceof Disposable)
                ((Disposable) renderable).dispose();
//            }
//            catch (Exception e) {
//                String msg = Logging.getMessage("generic.ExceptionAttemptingToDisposeRenderable");
//                Logging.logger().severe(msg);
//                // continue to next renderable
//            }
        }

        this.extentTree.clear();
    }

    protected void doPreRender(DrawContext dc) {
        CachedRenderableLayer.doPreRender(dc, this.getAllRenderables());
    }

    protected void doPick(DrawContext dc, Point pickPoint) {
        this.doPick(dc, this.getAllRenderables(), pickPoint);
    }

    protected void doRender(DrawContext dc) {
        CachedRenderableLayer.doRender(dc, this.getAllRenderables());
    }

    protected void doPick(DrawContext dc, Iterable<? extends Renderable> renderables, Point pickPoint) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        this.pickSupport.clearPickList();
        PickSupport.beginPicking(dc);

        try {
            for (Renderable renderable : renderables) {
                // If the caller has specified their own Iterable,
                // then we cannot make any guarantees about its contents.
                if (renderable != null) {
                    float[] inColor = new float[4];
                    gl.glGetFloatv(GL2.GL_CURRENT_COLOR, inColor, 0);
                    Color color = dc.getUniquePickColor();
                    gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());

//                    try {
                    renderable.render(dc);
//                    }
//                    catch (Exception e) {
//                        String msg = Logging.getMessage("generic.ExceptionWhilePickingRenderable");
//                        Logging.logger().severe(msg);
//                        continue; // go on to next renderable
//                    }

                    gl.glColor4fv(inColor, 0);

                    if (renderable instanceof Locatable) {
                        this.pickSupport.addPickableObject(color.getRGB(), renderable,
                            ((Locatable) renderable).getPosition(), false);
                    } else {
                        this.pickSupport.addPickableObject(color.getRGB(), renderable);
                    }
                }
            }

            this.pickSupport.resolvePick(dc, pickPoint, this);
        }
        finally {
            PickSupport.endPicking(dc);
        }
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.CachedRenderableLayer.Name");
    }
}
