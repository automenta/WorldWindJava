/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.util.BasicDragger;

import javax.swing.Timer;
import java.awt.*;
import java.awt.image.*;
import java.util.List;
import java.util.*;

/**
 * This example illustrates how you might show video on the globe's surface. It uses a {@link
 * SurfaceImage} to display one image after another, each of which could correspond to a frame
 * of video. The video frames are simulated by creating a new buffered image for each frame. The same
 * <code>SurfaceImage</code> is used. The image source of the <code>SurfaceImage</code> is continually set to a new
 * BufferedImage. (It would be more efficient to also re-use a single BufferedImage, but one objective of this example
 * is to show how to do this when the image can't be re-used.) The <code>SurfaceImage</code> location could also be
 * continually changed, but this example doesn't do that.
 *
 * @author tag
 * @version $Id: VideoOnTerrain.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class VideoOnTerrain extends ApplicationTemplate {
    protected static final int IMAGE_SIZE = 512;
    protected static final double IMAGE_OPACITY = 0.5;
    protected static final double IMAGE_SELECTED_OPACITY = 0.8;

    // These corners do not form a Sector, so SurfaceImage must generate a texture rather than simply using the source
    // image.
    protected static final List<LatLon> CORNERS = Arrays.asList(
        LatLon.fromDegrees(37.8313, -105.0653),
        LatLon.fromDegrees(37.8313, -105.0396),
        LatLon.fromDegrees(37.8539, -105.04),
        LatLon.fromDegrees(37.8539, -105.0653)
    );

    public static void main(String[] args) {
        Configuration.setValue(Keys.INITIAL_LATITUDE, 37.8432);
        Configuration.setValue(Keys.INITIAL_LONGITUDE, -105.0527);
        Configuration.setValue(Keys.INITIAL_ALTITUDE, 7000);
        ApplicationTemplate.start("WorldWind Video on Terrain", AppFrame.class);
    }

    protected static class AppFrame extends ApplicationTemplate.AppFrame {
        protected final long start = System.currentTimeMillis();
        protected long counter;

        public AppFrame() {
            super(true, true, true);

            RenderableLayer layer = new RenderableLayer();
            layer.setName("Video on terrain");
            WorldWindow.insertBeforePlacenames(this.wwd(), layer);

            // Set up a SelectListener to drag the SurfaceImage.
            this.wwd().addSelectListener(new SurfaceImageDragger(this.wwd()));

            final SurfaceImage surfaceImage = new SurfaceImage(makeImage(), CORNERS);
            surfaceImage.setOpacity(IMAGE_OPACITY);
            layer.add(surfaceImage);

            Timer timer = new Timer(50, actionEvent -> {
                Iterable<LatLon> corners = surfaceImage.getCorners();
                surfaceImage.setImageSource(makeImage(), corners);
                wwd().redraw();
            });
            timer.start();
        }

        protected BufferedImage makeImage() {
            BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = image.createGraphics();

            g.setPaint(Color.WHITE);
            g.fill3DRect(0, 0, IMAGE_SIZE, IMAGE_SIZE, false);

            g.setPaint(Color.RED);
            g.setFont(Font.decode("ARIAL-BOLD-50"));

            g.drawString(++this.counter + " frames", 10, IMAGE_SIZE / 4);
            g.drawString((System.currentTimeMillis() - start) / 1000 + " sec", 10, IMAGE_SIZE / 2);
            g.drawString("Heap:" + Runtime.getRuntime().totalMemory(), 10, 3 * IMAGE_SIZE / 4);

            g.dispose();

            return image;
        }
    }

    protected static class SurfaceImageDragger implements SelectListener {
        protected final WorldWindow wwd;
        protected final BasicDragger dragger;
        protected SurfaceImage lastHighlit;

        public SurfaceImageDragger(WorldWindow wwd) {
            this.wwd = wwd;
            this.dragger = new BasicDragger(wwd);
        }

        public void accept(SelectEvent event) {
            // Have rollover events highlight the rolled-over object.
            if (event.getEventAction().equals(SelectEvent.ROLLOVER) && !this.dragger.isDragging()) {
                this.highlight(event.getTopObject());
                this.wwd.redraw();
            }

            // Drag the selected object.
            if (event.getEventAction().equals(SelectEvent.DRAG) ||
                event.getEventAction().equals(SelectEvent.DRAG_END)) {
                this.dragger.accept(event);

                if (this.dragger.isDragging())
                    this.wwd.redraw();
            }

            // We missed any roll-over events while dragging, so highlight any under the cursor now.
            if (event.getEventAction().equals(SelectEvent.DRAG_END)) {
                PickedObjectList pol = this.wwd.objectsAtPosition();
                if (pol != null) {
                    this.highlight(pol.getTopObject());
                    this.wwd.redraw();
                }
            }
        }

        protected void highlight(Object o) {
            if (this.lastHighlit == o)
                return; // Same thing selected

            // Turn off highlight if on.
            if (this.lastHighlit != null) {
                this.lastHighlit.setOpacity(IMAGE_OPACITY);
                this.lastHighlit = null;
            }

            // Turn on highlight if selected object is a SurfaceImage.
            if (o instanceof SurfaceImage) {
                this.lastHighlit = (SurfaceImage) o;
                this.lastHighlit.setOpacity(IMAGE_SELECTED_OPACITY);
            }
        }
    }
}