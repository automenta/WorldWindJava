/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.video.LayerList;

/**
 * Aggregates a globe and a set of layers. Through the globe it also indirectly includes the elevation model and the
 * surface geometry tessellator. A default model is defined in <code>worldwind.xml</code> or its application-specified
 * alternate.
 *
 * @author Tom Gaskins
 * @version $Id: Model.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface Model extends WWObject {
    /**
     * Returns the bounding sphere in Cartesian world coordinates of the model.
     *
     * @return the model's bounding sphere in Cartesian coordinates, or null if the extent cannot be determined.
     */
    default Extent getExtent() {
        // See if the layers have it.
        //TODO union(extents...)
        LayerList layers = layers();
        if (layers != null) {
            for (Object layer1 : layers) {
                Layer layer = (Layer) layer1;
                Extent e = (Extent) layer.get(Keys.EXTENT);
                if (e != null)
                    return e;
            }
        }

        // See if the Globe has it.
        Globe globe = this.globe();
        return globe != null ? globe.getExtent() : null;
    }

    /**
     * Indicates the globe in this model.
     *
     * @return The globe associated with this model.
     */
    Globe globe();

    /**
     * Specifies the model's globe.
     *
     * @param globe the model's new globe. May be null, in which case the current globe will be detached from the
     *              model.
     */
    void setGlobe(Globe globe);

    /**
     * Indicates the layers associated with this model.
     *
     * @return List of layers in this model.
     */
    LayerList layers();

//    /**
//     * Specifies the model's layers.
//     *
//     * @param layers the model's new layers. May be null, in which case the current layers will be detached from the
//     *               model.
//     */
//    void setLayers(LayerList layers);

    /**
     * Indicates whether the globe surface's interior geometry is to be drawn.
     *
     * @return true if it is to be drawn, otherwise false.
     */
    boolean isShowWireframeInterior();

    /**
     * Specifies whether to display as wireframe the interior geometry of the tessellated globe surface.
     *
     * @param show true causes the geometry to be shown, false, the default, does not.
     */
    void setShowWireframeInterior(boolean show);

    /**
     * Indicates whether the globe surface's exterior geometry is to be drawn.
     *
     * @return true if it is to be drawn, otherwise false.
     */
    boolean isShowWireframeExterior();

    /**
     * Specifies whether to display as wireframe the exterior geometry of the tessellated globe surface.
     *
     * @param show true causes the geometry to be shown, false, the default, does not.
     */
    void setShowWireframeExterior(boolean show);

    /**
     * Indicates whether the bounding volumes of the tessellated globe's surface geometry should be displayed.
     *
     * @return true if the bounding volumes are to be drawn, otherwise false.
     */
    boolean isShowTessellationBoundingVolumes();

    /**
     * Specifies whether the bounding volumes of the globes tessellated surface geometry is to be drawn.
     *
     * @param showTileBoundingVolumes true if the bounding volumes should be drawn, false, the default, if not.
     */
    void setShowTessellationBoundingVolumes(boolean showTileBoundingVolumes);
}