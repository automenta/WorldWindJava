/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.formats.tiff.GeotiffImageReaderSpi;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;

import javax.imageio.spi.IIORegistry;
import java.awt.*;
import java.util.*;

/**
 * This example demonstrates how to use the {@link SurfaceImage} class to place images on the
 * surface of the globe.
 *
 * @author tag
 * @version $Id: SurfaceImages.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class SurfaceImages extends ApplicationTemplate {

    protected static final String GEORSS_ICON_PATH = "gov/nasa/worldwind/examples/images/georss.png";
    protected static final String TEST_PATTERN = "gov/nasa/worldwind/examples/images/antenna.png";

    static {
        IIORegistry reg = IIORegistry.getDefaultInstance();
        reg.registerServiceProvider(GeotiffImageReaderSpi.inst());
    }

    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Surface Images", SurfaceImages.AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {

        public AppFrame() {
            super(true, true, false);

            try {
                SurfaceImage si1 = new SurfaceImage(GEORSS_ICON_PATH, new ArrayList<>(Arrays.asList(
                    LatLon.fromDegrees(20.0d, -115.0d),
                    LatLon.fromDegrees(20.0d, -105.0d),
                    LatLon.fromDegrees(32.0d, -102.0d),
                    LatLon.fromDegrees(30.0d, -115.0d)
                )));
                SurfaceImage si2 = new SurfaceImage(TEST_PATTERN, new ArrayList<>(Arrays.asList(
                    LatLon.fromDegrees(37.8677, -105.1668),
                    LatLon.fromDegrees(37.8677, -104.8332),
                    LatLon.fromDegrees(38.1321, -104.8326),
                    LatLon.fromDegrees(38.1321, -105.1674)
                )));
                Path boundary = new Path(si1.getCorners(), 0);
                boundary.setSurfacePath(true);
                boundary.setPathType(AVKey.RHUMB_LINE);
                var attrs = new BasicShapeAttributes();
                attrs.setOutlineMaterial(new Material(new Color(0, 255, 0)));
                boundary.setAttributes(attrs);
                boundary.makeClosed();

                Path boundary2 = new Path(si2.getCorners(), 0);
                boundary2.setSurfacePath(true);
                boundary2.setPathType(AVKey.RHUMB_LINE);
                attrs = new BasicShapeAttributes();
                attrs.setOutlineMaterial(new Material(new Color(0, 255, 0)));
                boundary2.setAttributes(attrs);
                boundary2.makeClosed();

                RenderableLayer layer = new RenderableLayer();
                layer.setName("Surface Images");
                layer.setPickEnabled(false);
                layer.add(si1);
                layer.add(si2);
                layer.add(boundary);
                layer.add(boundary2);

                WorldWindow.insertBeforeCompass(this.wwd(), layer);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
