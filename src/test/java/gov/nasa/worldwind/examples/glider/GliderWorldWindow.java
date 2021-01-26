/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.glider;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.video.awt.WorldWindowGLCanvas;

import java.beans.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * View class to support GLIDER program. This class is internal to the Eclipse RCP implementation of EarthView in the
 * GLIDER source.
 *
 * @author tag
 * @version $Id: GliderWorldWindow.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GliderWorldWindow extends WorldWindowGLCanvas {
    protected final Set<GliderImage> imageTable = new CopyOnWriteArraySet<>();
    protected final PropertyChangeListener layerListener = new LayerListener();

    /**
     * Adds an image to display on the globe.
     *
     * @param image the image to display
     * @throws IllegalArgumentException if <code>image</code> is null.
     */
    public void addImage(GliderImage image) {
        if (image == null) {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.imageTable.contains(image))
            this.removeImage(image);

        GliderImageLayer layer = new GliderImageLayer();

        image.set(Keys.LAYER, layer);
        layer.setImage(image);
        WorldWindow.insertBeforeCompass(this, layer);

        layer.addPropertyChangeListener(this.layerListener);

        this.imageTable.add(image);

        this.emit(GliderImage.GLIDER_IMAGE_SOURCE, null, image);

        this.repaint();
    }

    /**
     * Removes a specified image from the globe.
     *
     * @param image the image to remove.
     * @throws IllegalArgumentException if <code>image</code> is null.
     */
    public void removeImage(GliderImage image) {
        if (image == null) {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.imageTable.remove(image);

        Layer layer = (Layer) image.get(Keys.LAYER);
        if (layer != null) {
            image.removeKey(Keys.LAYER);
            layer.removePropertyChangeListener(this.layerListener);
            this.model().layers().remove(layer);
            layer.dispose();
        }

        this.emit(GliderImage.GLIDER_IMAGE_SOURCE, null, image);

        this.repaint();
    }

    /**
     * Returns the set of currently associated images.
     *
     * @return the set of currently associated images. The returned set is not modifiable and cannot be used to
     * associate or disassociate an image.
     */
    public Set<GliderImage> getImages() {
        return Collections.unmodifiableSet(this.imageTable);
    }

    /**
     * Indicates whether a specified image is associated.
     *
     * @param image the image in question.
     * @return <code>true</code> if the image is associated, <code>false</code> if not or if <code>image</code> is
     * null.
     */
    public boolean containsImage(GliderImage image) {
        return image != null && this.imageTable.contains(image);
    }

    protected class LayerListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            GliderWorldWindow.this.emit(GliderImage.GLIDER_IMAGE_SOURCE, null, this);
            GliderWorldWindow.this.repaint();
        }
    }
}