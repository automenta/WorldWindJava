/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.glider;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.InputStream;
import java.util.List;
import java.util.*;

/**
 * @author tag
 * @version $Id: GliderTestApp.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class GliderTestApp extends ApplicationTemplate {

    protected static final LatLon nw = LatLon.fromDegrees(48.55774732, -134.459224670811);
    protected static final LatLon ne = nw.add(LatLon.fromDegrees(0, 0.036795 * 250));
    protected static final LatLon se = nw.add(LatLon.fromDegrees(-0.036795 * 200, 0.036795 * 250));
    protected static final LatLon sw = nw.add(LatLon.fromDegrees(-0.036795 * 200, 0));
    protected static final List<LatLon> corners = Arrays.asList(sw, se, ne, nw);
    protected static final String cloudImagePath = "gov/nasa/worldwind/examples/images/GLIDERTestImage-800x519.jpg";
    private static double opacityIncrement = -0.1;

    protected static float[][] makeField(Iterable<LatLon> corners, int width, int height, Angle angle) {
        Sector sector = Sector.boundingSector(corners);
        double dLat = sector.latDelta / (height - 1.0d);
        double dLon = sector.lonDelta / (width - 1.0d);

        float[] lons = new float[width * height];
        float[] lats = new float[lons.length];

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                lons[j * width + i] = (float) (sector.lonMin + i * dLon);
                lats[j * width + i] = (float) (sector.latMax - j * dLat);
            }
        }

        double cosAngle = angle.cos();
        double sinAngle = angle.sin();

        LatLon c = sector.getCentroid();
        float cx = (float) c.getLon().degrees;
        float cy = (float) c.getLat().degrees;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int index = j * width + i;

                float x = lons[index];
                float y = lats[index];

                lons[index] = (float) ((x - cx) * cosAngle - (y - cy) * sinAngle + cx);
                lats[index] = (float) ((x - cx) * sinAngle + (y - cy) * cosAngle + cy);
            }
        }

        return new float[][] {lats, lons};
    }

    protected static ArrayList<LatLon> makeBorder(float[][] field, int width, int height, ArrayList<LatLon> latLons) {
        for (int i = 0; i < width; i++) {
            latLons.add(LatLon.fromDegrees(field[0][i], field[1][i]));
        }
        for (int i = 2 * width - 1; i < height * width; i += width) {
            latLons.add(LatLon.fromDegrees(field[0][i], field[1][i]));
        }
        for (int i = width * height - 2; i > width * (height - 1); i--) {
            latLons.add(LatLon.fromDegrees(field[0][i], field[1][i]));
        }
        for (int i = width * (height - 2); i > 0; i -= width) {
            latLons.add(LatLon.fromDegrees(field[0][i], field[1][i]));
        }

        return latLons;
    }

    public static void main(String[] args) {
        final ImageUtil.AlignedImage projectedImage;
        final String imageName;
        final BufferedImage testImage;
        final ArrayList<LatLon> latLons = new ArrayList<>();

        final AppFrame frame = start("GLIDER Test Application", GliderAppFrame.class);

        InputStream stream = null;
        try {
            stream = WWIO.openFileOrResourceStream(cloudImagePath, null);
            testImage = ImageIO.read(stream);
            long start = System.currentTimeMillis();
            float[][] field = makeField(corners, testImage.getWidth(), testImage.getHeight(), new Angle(15));
            makeBorder(field, testImage.getWidth(), testImage.getHeight(), latLons);
            projectedImage = GliderImage.alignImage(testImage, field[0], field[1]);
            System.out.printf("Image projected, %d ms\n", System.currentTimeMillis() - start);
            imageName = WWIO.getFilename(cloudImagePath);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        finally {
            WWIO.closeStream(stream, cloudImagePath);
        }

        SwingUtilities.invokeLater(() -> {
            final GliderImage image = new GliderImage(imageName, projectedImage, 100);
            final GliderRegionOfInterest regionOfInterest = new GliderRegionOfInterest(latLons, Color.RED);
            image.addRegionOfInterest(regionOfInterest);

            final Timer timer = new Timer(1000, (ActionEvent evt) -> {
//                try {
                if (((GliderWorldWindow) frame.wwd()).getImages().isEmpty()) {
                    System.out.println("ADDING");
                    ((GliderWorldWindow) frame.wwd()).addImage(image);
                    image.releaseImageSource();
                }
                else {
                    double opacity = image.getOpacity() + opacityIncrement;
                    image.setOpacity(opacity);
                    if (opacity <= 0.1 || opacity >= 1.0) {
                        opacityIncrement *= -1.0;
                    }
                }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            });
            timer.setRepeats(true);
            timer.start();
        });
    }

    public static class GliderAppPanel extends AppPanel {

        public GliderAppPanel(Dimension canvasSize, boolean includeStatusBar) {
            super(canvasSize, includeStatusBar);
        }

        @Override
        protected WorldWindow createWorldWindow() {
            return new GliderWorldWindow();
        }
    }

    public static class GliderAppFrame extends AppFrame {

        public GliderAppFrame() {
            super(true, true, false);
        }

        @Override
        protected AppPanel createAppPanel(Dimension canvasSize, boolean includeStatusBar) {
            return new GliderAppPanel(canvasSize, includeStatusBar);
        }
    }
}