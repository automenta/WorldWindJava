/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * The <code>RenderableLayer</code> class manages a collection of {@link Renderable} objects
 * for rendering, picking, and disposal.
 *
 * @author tag
 * @version $Id: RenderableLayer.java 3435 2015-10-13 10:32:43Z dcollins $
 * @see Renderable
 */
public class RenderableLayer extends AbstractLayer {
    protected final List<Renderable> renderables = new ArrayList();

    protected final PickSupport pickSupport = new PickSupport();
//    protected Iterable<Renderable> renderablesOverride;

    /**
     * Creates a new <code>RenderableLayer</code> with a null <code>delegateOwner</code>
     */
    public RenderableLayer() {
    }

    /**
     * Adds the specified <code>renderable</code> to the end of this layer's internal collection. If this layer's
     * internal collection has been overridden with a call to {@link #set(Iterable)}, this will throw an
     * exception.
     * <p>
     * If the <code>renderable</code> implements {@link AVList}, the layer forwards its
     * property change events to the layer's property change listeners. Any property change listeners the layer attaches
     * to the <code>renderable</code> are removed in {@link #remove(Renderable)},
     * {@link #clear()}, or {@link #dispose()}.
     *
     * @param renderable Renderable to add.
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     * @throws IllegalStateException    If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void add(Renderable renderable) {

    

        this.renderables.add(renderable);

        // Attach the layer as a property change listener of the renderable. This forwards property change events from
        // the renderable to the SceneController.
        if (renderable instanceof AVList)
            ((AVList) renderable).addPropertyChangeListener(this);
    }

    /**
     * Inserts the specified <code>renderable</code> at the specified <code>index</code> in this layer's internal
     * collection. If this layer's internal collection has been overridden with a call to {@link
     * #set(Iterable)}, this will throw an exception.
     * <p>
     * If the <code>renderable</code> implements {@link AVList}, the layer forwards its
     * property change events to the layer's property change listeners. Any property change listeners the layer attaches
     * to the <code>renderable</code> are removed in {@link #remove(Renderable)},
     * {@link #clear()}, or {@link #dispose()}.
     *
     * @param index      the index at which to insert the specified renderable.
     * @param renderable Renderable to insert.
     * @throws IllegalArgumentException If <code>renderable</code> is null, if the <code>index</code> is less than zero,
     *                                  or if the <code>index</code> is greater than the number of renderables in this
     *                                  layer.
     * @throws IllegalStateException    If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void add(int index, Renderable renderable) {


        if (index < 0 || index > this.renderables.size()) {
            String msg = Logging.getMessage("generic.indexOutOfRange", index);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // The renderables are contained in a ConcurrentLinkedQueue, which does not support element insertion. Make a
        // shallow copy of the queue, insert into the copy, then replace the queue contents with the copy. This process
        // maintains the element order, with the new renderabable inserted in the specified index.
        List<Renderable> copy = new ArrayList<>(this.renderables);
        copy.add(index, renderable);
        this.renderables.clear();
        this.renderables.addAll(copy);

        // Attach the layer as a property change listener of the renderable. This forwards property change events from
        // the renderable to the SceneController.
        if (renderable instanceof AVList)
            ((AVList) renderable).addPropertyChangeListener(this);
    }

    /**
     * Adds the contents of the specified <code>renderables</code> to this layer's internal collection. If this layer's
     * internal collection has been overriden with a call to {@link #set(Iterable)}, this will throw an
     * exception.
     * <p>
     * If any of the <code>renderables</code> implement {@link AVList}, the layer forwards
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attaches to the <code>renderable</code> are removed in {@link #remove(Renderable)},
     * {@link #clear()}, or {@link #dispose()}.
     *
     * @param renderables Renderables to add.
     * @throws IllegalArgumentException If <code>renderables</code> is null.
     * @throws IllegalStateException    If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void addAll(Iterable<? extends Renderable> renderables) {


        for (Renderable renderable : renderables) {
            // Internal list of renderables does not accept null values.
            if (renderable != null)
                this.renderables.add(renderable);

            // Attach the layer as a property change listener of the renderable. This forwards property change events
            // from the renderable to the SceneController.
            if (renderable instanceof AVList)
                ((AVList) renderable).addPropertyChangeListener(this);
        }
    }

    /**
     * Removes the specified <code>renderable</code> from this layer's internal collection, if it exists. If this
     * layer's internal collection has been overridden with a call to {@link #set(Iterable)}, this will throw
     * an exception.
     * <p>
     * If the <code>renderable</code> implements {@link AVList}, this stops forwarding the its
     * property change events to the layer's property change listeners. Any property change listeners the layer attached
     * to the <code>renderable</code> in {@link #add(Renderable)} or {@link
     * #addAll(Iterable)} are removed.
     *
     * @param renderable Renderable to remove.
     * @throws IllegalArgumentException If <code>renderable</code> is null.
     * @throws IllegalStateException    If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void remove(Renderable renderable) {


        if (this.renderables.remove(renderable)) {

            // Remove the layer as a property change listener of the renderable. This prevents the renderable from keeping a
            // dangling reference to the layer.
            if (renderable instanceof AVList)
                ((AVList) renderable).removePropertyChangeListener(this);
        }
    }

    /**
     * Clears the contents of this layer's internal Renderable collection. If this layer's internal collection has been
     * overriden with a call to {@link #set(Iterable)}, this will throw an exception.
     * <p>
     * If any of the <code>renderables</code> implement {@link AVList}, this stops forwarding
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attached to the <code>renderables</code> in {@link #add(Renderable)} or
     * {@link #addAll(Iterable)} are removed.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    public void clear() {


        this._clear();
    }

    protected void _clear() {
        if (this.renderables != null && !this.renderables.isEmpty()) {
            // Remove the layer as property change listener of any renderables. This prevents the renderables from
            // keeping a dangling references to the layer.
            for (Renderable renderable : this.renderables) {
                if (renderable instanceof AVList)
                    ((AVList) renderable).removePropertyChangeListener(this);
            }

            this.renderables.clear();
        }
    }

    public int size() {
//        if (this.renderablesOverride != null) {
//            int size = 0;
//            //noinspection UnusedDeclaration
//            for (Renderable r : this.renderablesOverride) {
//                ++size;
//            }
//
//            return size;
//        }
//        else {
            return this.renderables.size();
//        }
    }

    /**
     * Returns the Iterable of Renderables currently in use by this layer. If the caller has specified a custom Iterable
     * via {@link #set(Iterable)}, this will returns a reference to that Iterable. If the caller passed
     * <code>setRenderables</code> a null parameter, or if <code>setRenderables</code> has not been called, this
     * returns a view of this layer's internal collection of Renderables.
     *
     * @return Iterable of currently active Renderables.
     */
    public Iterable<Renderable> all() {
        return this.active();
    }

    /**
     * Returns the Iterable of currently active Renderables. If the caller has specified a custom Iterable via {@link
     * #set(Iterable)}, this will returns a reference to that Iterable. If the caller passed
     * <code>setRenderables</code> a null parameter, or if <code>setRenderables</code> has not been called, this
     * returns a view of this layer's internal collection of Renderables.
     *
     * @return Iterable of currently active Renderables.
     */
    protected Iterable<Renderable> active() {
//        if (this.renderablesOverride != null) {
//            return this.renderablesOverride;
//        } else {
            // Return an unmodifiable reference to the internal list of renderables.
            // This prevents callers from changing this list and invalidating any invariants we have established.
            //return Collections.unmodifiableCollection(this.renderables);
            return renderables;
//        }
    }


    /**
     * Disposes the contents of this layer's internal Renderable collection, but does not remove any elements from that
     * collection.
     * <p>
     * If any of layer's internal Renderables implement {@link AVList}, this stops forwarding
     * their property change events to the layer's property change listeners. Any property change listeners the layer
     * attached to the <code>renderables</code> in {@link #add(Renderable)} or
     * {@link #addAll(Iterable)} are removed.
     *
     * @throws IllegalStateException If a custom Iterable has been specified by a call to <code>setRenderables</code>.
     */
    @Override public void dispose() {

        if (this.renderables != null && !this.renderables.isEmpty()) {
            for (Renderable renderable : this.renderables) {
                try {
                    // Remove the layer as a property change listener of the renderable. This prevents the renderable
                    // from keeping a dangling reference to the layer.
                    if (renderable instanceof AVList)
                        ((AVList) renderable).removePropertyChangeListener(this);

                    if (renderable instanceof Disposable)
                        ((Disposable) renderable).dispose();
                }
                catch (Exception e) {
                    String msg = Logging.getMessage("generic.ExceptionAttemptingToDisposeRenderable");
                    Logging.logger().severe(msg);
                    // continue to next renderable
                }
            }
        }

        this.renderables.clear();
    }

    protected void doPreRender(DrawContext dc) {
        RenderableLayer.doPreRender(dc, this.active());
    }

    protected void doPick(DrawContext dc, Point pickPoint) {
        this.doPick(dc, this.active(), pickPoint);
    }

    protected void doRender(DrawContext dc) {
        RenderableLayer.doRender(dc, this.active());
    }

    protected static void doPreRender(DrawContext dc, Iterable<? extends Renderable> renderables) {
        for (Renderable renderable : renderables) {
//            try {
                // If the caller has specified their own Iterable,
                // then we cannot make any guarantees about its contents.
                if (renderable instanceof PreRenderable)
                    ((PreRenderable) renderable).preRender(dc);
//            } catch (Exception e) {
//                String msg = Logging.getMessage("generic.ExceptionWhilePrerenderingRenderable");
//                Logging.logger().severe(msg);
//                // continue to next renderable
//            }
        }
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
                    Color color = dc.getUniquePickColor();
                    gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());

//                    try {
                        renderable.render(dc);
//                    }
//                    catch (Exception e) {
//                        String msg = Logging.getMessage("generic.ExceptionWhilePickingRenderable");
//                        Logging.logger().severe(msg);
//                        Logging.logger().log(Level.FINER, msg, e); // show exception for this level
//                        continue; // go on to next renderable
//                    }
//
//                    gl.glColor4fv(inColor, 0);

                    if (renderable instanceof Locatable) {
                        this.pickSupport.addPickableObject(color.getRGB(), renderable,
                            ((Locatable) renderable).getPosition(), false);
                    }else {
                        this.pickSupport.addPickableObject(color.getRGB(), renderable);
                    }
                }
            }

            this.pickSupport.resolvePick(dc, pickPoint, this);

        } finally {
            PickSupport.endPicking(dc);
        }
    }

    protected static void doRender(DrawContext dc, Iterable<? extends Renderable> renderables) {
        for (Renderable renderable : renderables) {
//            try {
                // If the caller has specified their own Iterable,
                // then we cannot make any guarantees about its contents.
                //if (renderable != null)
                    renderable.render(dc);
//            }
//            catch (Exception e) {
//                String msg = Logging.getMessage("generic.ExceptionWhileRenderingRenderable");
//                Logging.logger().log(Level.SEVERE, msg, e);
//                // continue to next renderable
//            }
        }
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.RenderableLayer.Name");
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation forwards the message to each Renderable that implements {@link MessageListener}.
     *
     * @param message The message that was received.
     */
    @Override
    public void onMessage(Message message) {
        for (Renderable renderable : this.renderables) {
//            try {
                if (renderable instanceof MessageListener)
                    ((MessageListener) renderable).onMessage(message);
//            }
//            catch (Exception e) {
//                String msg = Logging.getMessage("generic.ExceptionInvokingMessageListener");
//                Logging.logger().log(Level.SEVERE, msg, e);
//                // continue to next renderable
//            }
        }
    }
}
