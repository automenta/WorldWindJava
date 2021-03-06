/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * AirspaceLayer manages a list of airspaces for rendering and picking. AirspaceLayer was originally designed as a
 * special purpose layer for {@link Airspace} shapes, but is now a redundant with {@link RenderableLayer}. Usage of
 * AirspaceLayer should be replaced with RenderableLayer. Most methods on AirspaceLayer can be replaced with the
 * equivalent methods of Airspace or RenderableLayer. The methods that are no longer supported are documented as such.
 *
 * @author dcollins
 * @version $Id: AirspaceLayer.java 2231 2014-08-15 19:03:12Z dcollins $
 * @deprecated Use {@link RenderableLayer} instead.
 */
@Deprecated
public class AirspaceLayer extends AbstractLayer {
    private final Collection<Airspace> airspaces = new ConcurrentLinkedQueue<>();
    private Iterable<Airspace> airspacesOverride;

    /**
     * Creates a new <code>Airspace</code> with an empty collection of Airspaces.
     */
    public AirspaceLayer() {
    }

    /**
     * Returns false.
     *
     * @return false
     * @deprecated Use {@link ShapeAttributes#isEnableAntialiasing()} on each Airspace instance in the layer.
     */
    @Deprecated
    public static boolean isEnableAntialiasing() {
        return false; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param enable ignored.
     * @deprecated Use {@link ShapeAttributes#setEnableAntialiasing(boolean)} on each Airspace instance in the layer.
     */
    @Deprecated
    public void setEnableAntialiasing(boolean enable) {
        // deprecated method
    }

    /**
     * Returns false.
     *
     * @return false
     * @deprecated Control over airspace blending is no longer supported. Airspaces implicitly blend themselves with
     * other objects in the scene.
     */
    @Deprecated
    public static boolean isEnableBlending() {
        return false; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param enable ignored.
     * @deprecated Control over airspace blending is no longer supported. Airspaces implicitly blend themselves with
     * other objects in the scene.
     */
    @Deprecated
    public void setEnableBlending(boolean enable) {
        // deprecated method
    }

    /**
     * Returns false.
     *
     * @return false
     * @deprecated Use {@link Airspace#isEnableDepthOffset()} on each Airspace instance in the layer.
     */
    @Deprecated
    public static boolean isEnableDepthOffset() {
        return false; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param enable ignored.
     * @deprecated Use {@link Airspace#setEnableDepthOffset(boolean)} on each Airspace instance in the layer.
     */
    @Deprecated
    public void setEnableDepthOffset(boolean enable) {
        // deprecated method
    }

    /**
     * Returns false.
     *
     * @return false
     * @deprecated Use {@link ShapeAttributes#isEnableLighting()} on each Airspace instance in the layer.
     */
    @Deprecated
    public static boolean isEnableLighting() {
        return false; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param enable ignored.
     * @deprecated Use {@link ShapeAttributes#isEnableLighting()} on each Airspace instance in the layer.
     */
    @Deprecated
    public void setEnableLighting(boolean enable) {
        // deprecated method
    }

    /**
     * Returns false.
     *
     * @return false
     * @deprecated Control over drawing Airspace extents is no longer supported.
     */
    @Deprecated
    public static boolean isDrawExtents() {
        return false; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param draw ignored
     * @deprecated Control over drawing Airspace extents is no longer supported.
     */
    @Deprecated
    public void setDrawExtents(boolean draw) {
        // deprecated method
    }

    /**
     * Returns false.
     *
     * @return false
     * @deprecated Control over drawing Airspace in wireframe mode is no longer supported.
     */
    @Deprecated
    public static boolean isDrawWireframe() {
        return false; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param draw ignored
     * @deprecated Control over drawing Airspace in wireframe mode is no longer supported.
     */
    @Deprecated
    public void setDrawWireframe(boolean draw) {
        // deprecated method
    }

    /**
     * Returns 0.
     *
     * @return 0
     * @deprecated Control over Airspace depth offset is no longer supported. See {@link
     * Airspace#setEnableDepthOffset(boolean)}.
     */
    @Deprecated
    public static Double getDepthOffsetFactor() {
        return 0.0d; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param factor ignored
     * @deprecated Control over Airspace depth factor is no longer supported. See {@link
     * Airspace#setEnableDepthOffset(boolean)}.
     */
    @Deprecated
    public void setDepthOffsetFactor(Double factor) {
        // deprecated method
    }

    /**
     * Returns 0.
     *
     * @return 0
     * @deprecated Control over Airspace depth units is no longer supported. See {@link
     * Airspace#setEnableDepthOffset(boolean)}.
     */
    @Deprecated
    public static Double getDepthOffsetUnits() {
        return 0.0d; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param units ignored
     * @deprecated Control over Airspace depth units is no longer supported. See {@link
     * Airspace#setEnableDepthOffset(boolean)}.
     */
    @Deprecated
    public void setDepthOffsetUnits(Double units) {
        // deprecated method
    }

    /**
     * Returns false.
     *
     * @return false
     * @deprecated Use {@link Airspace#isEnableBatchRendering()} on each Airspace instance in the layer.
     */
    @Deprecated
    public static boolean isEnableBatchRendering() {
        return false; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param enableBatchRendering ignored
     * @deprecated Use {@link Airspace#setEnableBatchRendering(boolean)} on each Airspace instance in the layer.
     */
    @Deprecated
    public void setEnableBatchRendering(boolean enableBatchRendering) {
        // deprecated method
    }

    /**
     * Returns false.
     *
     * @return false
     * @deprecated Use {@link Airspace#isEnableBatchPicking()} on each Airspace instance in the layer.
     */
    @Deprecated
    public static boolean isEnableBatchPicking() {
        return false; // deprecated method
    }

    /**
     * Does nothing.
     *
     * @param enableBatchPicking ignored
     * @deprecated Use {@link Airspace#setEnableBatchPicking(boolean)} on each Airspace instance in the layer.
     */
    @Deprecated
    public void setEnableBatchPicking(boolean enableBatchPicking) {
        // deprecated method
    }

    /**
     * Adds the specified <code>airspace</code> to this layer's internal collection. If this layer's internal collection
     * has been overridden with a call to {@link #setAirspaces(Iterable)}, this will throw an exception.
     *
     * @param airspace the airspace to add.
     * @throws IllegalArgumentException if the airspace is null.
     * @throws IllegalStateException    if a custom Iterable has been specified by a call to setRenderables.
     * @deprecated Use {@link RenderableLayer} and {@link RenderableLayer#add(Renderable)} instead.
     */
    @Deprecated
    public void addAirspace(Airspace airspace) {
        if (airspace == null) {
            String msg = "nullValue.AirspaceIsNull";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.airspacesOverride != null) {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.airspaces.add(airspace);
    }

    /**
     * Adds the contents of the specified <code>airspaces</code> to this layer's internal collection. If this layer's
     * internal collection has been overridden with a call to {@link #setAirspaces(Iterable)}, this will throw an
     * exception.
     *
     * @param airspaces the airspaces to add.
     * @throws IllegalArgumentException if the iterable is null.
     * @throws IllegalStateException    if a custom Iterable has been specified by a call to setRenderables.
     * @deprecated Use {@link RenderableLayer} and {@link RenderableLayer#addAll(Iterable)} instead.
     */
    @Deprecated
    public void addAirspaces(Iterable<Airspace> airspaces) {
        if (airspaces == null) {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.airspacesOverride != null) {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        for (Airspace airspace : airspaces) {
            // Internal list of airspaces does not accept null values.
            if (airspace != null)
                this.airspaces.add(airspace);
        }
    }

    /**
     * Removes the specified <code>airspace</code> from this layer's internal collection, if it exists. If this layer's
     * internal collection has been overridden with a call to {@link #setAirspaces(Iterable)}, this will throw an
     * exception.
     *
     * @param airspace the airspace to remove.
     * @throws IllegalArgumentException if the airspace is null.
     * @throws IllegalStateException    if a custom Iterable has been specified by a call to setRenderables.
     * @deprecated Use {@link RenderableLayer} and {@link RenderableLayer#remove(Renderable)} instead.
     */
    @Deprecated
    public void removeAirspace(Airspace airspace) {
        if (airspace == null) {
            String msg = "nullValue.AirspaceIsNull";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.airspacesOverride != null) {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.airspaces.remove(airspace);
    }

    /**
     * Clears the contents of this layer's internal Airspace collection. If this layer's internal collection has been
     * overridden with a call to {@link #setAirspaces(Iterable)}, this will throw an exception.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setAirspaces</code>.
     * @deprecated Use {@link RenderableLayer} and {@link RenderableLayer#clear()} instead.
     */
    @Deprecated
    public void removeAllAirspaces() {
        if (this.airspacesOverride != null) {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        clearAirspaces();
    }

    private void clearAirspaces() {
        if (this.airspaces != null && !this.airspaces.isEmpty())
            this.airspaces.clear();
    }

    /**
     * Returns the Iterable of Airspaces currently in use by this layer. If the caller has specified a custom Iterable
     * via {@link #setAirspaces(Iterable)}, this will returns a reference to that Iterable. If the caller passed
     * <code>setAirspaces</code> a null parameter, or if <code>setAirspaces</code> has not been called, this returns a
     * view of this layer's internal collection of Airspaces.
     *
     * @return Iterable of currently active Airspaces.
     * @deprecated Use {@link RenderableLayer} and {@link RenderableLayer#all()} instead.
     */
    @Deprecated
    public Iterable<Airspace> getAirspaces() {
        if (this.airspacesOverride != null) {
            return this.airspacesOverride;
        } else {
            // Return an unmodifiable reference to the internal list of airspaces.
            // This prevents callers from changing this list and invalidating any invariants we have established.
            return Collections.unmodifiableCollection(this.airspaces);
        }
    }

    /**
     * Overrides the collection of currently active Airspaces with the specified <code>airspaceIterable</code>. This
     * layer will maintain a reference to <code>airspaceIterable</code> strictly for picking and rendering. This layer
     * will not modify the Iterable reference. However, this will clear the internal collection of Airspaces, and will
     * prevent any modification to its contents via <code>addAirspace, addAirspaces, or removeAirspaces</code>.
     * <p>
     * If the specified <code>airspaceIterable</code> is null, this layer will revert to maintaining its internal
     * collection.
     *
     * @param airspaceIterable Iterable to use instead of this layer's internal collection, or null to use this layer's
     * @deprecated Use {@link RenderableLayer} and {@link RenderableLayer#set(Iterable)} instead.
     */
    @Deprecated
    public void setAirspaces(Iterable<Airspace> airspaceIterable) {
        this.airspacesOverride = airspaceIterable;
        // Clear the internal collection of Airspaces.
        clearAirspaces();
    }

    /**
     * Returns the Iterable of currently active Airspaces. If the caller has specified a custom Iterable via {@link
     * #setAirspaces(Iterable)}, this will returns a reference to that Iterable. If the caller passed
     * <code>setAirspaces</code> a null parameter, or if <code>setAirspaces</code> has not been called, this returns a
     * view of this layer's internal collection of Airspaces.
     *
     * @return Iterable of currently active Airspaces.
     */
    private Iterable<Airspace> getActiveAirspaces() {
        if (this.airspacesOverride != null) {
            return this.airspacesOverride;
        } else {
            return this.airspaces;
        }
    }

    @Override
    protected void doPick(DrawContext dc, Point pickPoint) {
        for (Airspace airspace : this.getActiveAirspaces()) {
            try {
                if (airspace != null) // caller-specified Iterables can include null elements
                    airspace.render(dc);
            }
            catch (RuntimeException e) {
                String msg = Logging.getMessage("generic.ExceptionWhileRenderingAirspace");
                Logging.logger().log(Level.SEVERE, msg, e);
                // continue to next airspace
            }
        }
    }

    @Override
    protected void doRender(DrawContext dc) {
        for (Airspace airspace : this.getActiveAirspaces()) {
            try {
                if (airspace != null) // caller-specified Iterables can include null elements
                    airspace.render(dc);
            }
            catch (RuntimeException e) {
                String msg = Logging.getMessage("generic.ExceptionWhileRenderingAirspace");
                Logging.logger().log(Level.SEVERE, msg, e);
                // continue to next airspace
            }
        }
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.AirspaceLayer.Name");
    }
}
