/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.tool.ScalebarLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.markers.*;

import java.awt.*;
import java.util.ArrayList;

/**
 * Handles marker indicator for the scalebar.
 *
 * @author Patrick Murris
 * @version $Id: ScalebarHint.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ScalebarHint {
    final MarkerAttributes markerAttributes;
    private final RenderableLayer layer = new RenderableLayer();
    private final MarkerRenderer markerRenderer = new MarkerRenderer();
    private final RenderableMarker marker;
    private WorldWindow wwd;
    private boolean enabled = true;

    public ScalebarHint() {
        this.layer.setName("Scalebar reference");
        this.layer.setEnabled(false);
        this.markerAttributes = new BasicMarkerAttributes(new Material(Color.YELLOW),
            BasicMarkerShape.CONE, 1, 10, 5);
        this.marker = new RenderableMarker(Position.ZERO, this.markerAttributes);
        this.layer.add(this.marker);
    }

    public void setWwd(WorldWindow worldWindow) {
        this.wwd = worldWindow;
        // Enable picking on the scalebar layer
        for (Layer l : this.wwd.model().layers()) {
            if (l instanceof ScalebarLayer)
                l.setPickEnabled(true);
        }
        // Add our layer
        this.wwd.model().layers().add(this.layer);

        // Add scalebar select listener to handle rollover
        this.wwd.addSelectListener(event -> {
            if (!enabled || event.getTopObject() == null || !(event.getTopObject() instanceof ScalebarLayer)) {
                layer.setEnabled(false);
                return;
            }

            if (!event.getEventAction().equals(SelectEvent.ROLLOVER))
                return;

            marker.setPosition(event.getTopPickedObject().position());
            layer.setEnabled(true);
            wwd.redraw();
        });
    }

    public MarkerAttributes getMarkerAttributes() {
        return this.markerAttributes;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean state) {
        this.enabled = state;
    }

    private class RenderableMarker extends BasicMarker implements Renderable {
        private ArrayList<Marker> markerList;

        public RenderableMarker(Position position, MarkerAttributes attrs) {
            super(position, attrs);
        }

        public void render(DrawContext dc) {
            if (this.markerList == null) {
                this.markerList = new ArrayList<>();
                this.markerList.add(this);
            }
            markerRenderer.render(dc, this.markerList);
        }
    }
}