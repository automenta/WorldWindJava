/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.orbit.OrbitView;

import java.awt.*;
import java.util.List;
import java.util.*;

import static java.lang.Math.toRadians;

/**
 * Surface renderable.
 *
 * @author Patrick Murris
 * @version $Id: AbstractSurfaceRenderable.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class AbstractSurfaceRenderable extends AbstractSurfaceObject {
    private double opacity = 1.0d;

    protected static Angle getViewHeading(DrawContext dc) {
        Angle heading = Angle.ZERO;
        if (dc.view() instanceof OrbitView)
            heading = dc.view().getHeading();
        return heading;
    }

    protected static double computePixelSizeAtLocation(DrawContext dc, LatLon location) {
        Globe globe = dc.getGlobe();
        Vec4 locationPoint = globe.computePointFromPosition(location.getLat(), location.getLon(),
            globe.elevation(location.getLat(), location.getLon()));
        double distance = dc.view().getEyePoint().distanceTo3(locationPoint);
        return dc.view().computePixelSizeAtDistance(distance);
    }

    // *** Utility methods

    protected static double computeDrawPixelSize(DrawContext dc, SurfaceTileDrawContext sdc) {
        return dc.getGlobe().getRadius() * toRadians(sdc.getSector().latDelta) / sdc.getViewport().height;
    }

    protected static Vec4 computeDrawPoint(LatLon location, SurfaceTileDrawContext sdc) {
        Vec4 point = new Vec4(location.getLon().degrees, location.getLat().degrees, 1);
        return point.transformBy4(sdc.getModelviewMatrix());
    }

    protected static Sector computeRotatedSectorBounds(Sector sector, LatLon location, Angle heading) {
        if (Math.abs(heading.degrees) < 0.0001)
            return sector;

        LatLon[] corners = {
            new LatLon(sector.latMax, sector.lonMin),  // nw
            new LatLon(sector.latMax, sector.lonMax),  // ne
            new LatLon(sector.latMin, sector.lonMin),  // sw
            new LatLon(sector.latMin, sector.lonMax),  // se
        };
        // Rotate corners around location
        for (int i = 0; i < corners.length; i++) {
            Angle azimuth = LatLon.greatCircleAzimuth(location, corners[i]);
            Angle distance = LatLon.greatCircleDistance(location, corners[i]);
            corners[i] = LatLon.greatCircleEndPosition(location, azimuth.add(heading), distance);
        }

        return Sector.boundingSector(Arrays.asList(corners));
    }

    protected static List<Sector> computeNormalizedSectors(Sector sector) {
        Angle minLat = sector.latMin();
        Angle maxLat = sector.latMax();
        Angle minLon = sector.lonMin();
        Angle maxLon = sector.lonMax();
        minLat = minLat.degrees >= -90 ? minLat : Angle.NEG90;
        maxLat = maxLat.degrees <= 90 ? maxLat : Angle.POS90;

        java.util.List<Sector> sectors = new ArrayList<>();
        if (minLon.degrees >= -180 && maxLon.degrees <= 180) {
            // No date line crossing on both sides
            sectors.add(new Sector(minLat, maxLat, minLon, maxLon));
        } else {
            if (minLon.degrees < -180 && maxLon.degrees > 180) {
                // min and max lon overlap at the date line - span the whole ongitude range
                sectors.add(new Sector(minLat, maxLat, Angle.NEG180, Angle.POS180));
            } else {
                // Date line crossing, produce two sectors, one on each side of the date line
                while (minLon.degrees < -180) {
                    minLon = minLon.addDegrees(360);
                }
                while (maxLon.degrees > 180) {
                    maxLon = maxLon.subtractDegrees(360);
                }
                if (minLat.degrees > maxLat.degrees) {
                    sector = new Sector(minLat, maxLat, minLon, maxLon);
                    Collections.addAll(sectors, Sector.splitBoundingSectors(sector));
                } else {
                    // min and max lon overlap - span the whole ongitude range
                    sectors.add(new Sector(minLat, maxLat, Angle.NEG180, Angle.POS180));
                }
            }
        }

        return sectors;
    }

    protected static int computeHemisphereOffset(Sector sector, LatLon location) {
        Angle sectorLon = sector.getCentroid().getLon();
        Angle locationLon = location.getLon();
        if (Math.abs(locationLon.degrees - sectorLon.degrees) > 180
            && Math.signum(locationLon.degrees) != Math.signum(sectorLon.degrees)) {
            return (int) (360 * Math.signum(sectorLon.degrees));
        }

        return 0;
    }

    protected static void applyPremultipliedAlphaColor(GL2 gl, Color color, double opacity) {
        float[] compArray = new float[4];
        color.getRGBComponents(compArray);
        compArray[3] = (float) WWMath.clamp(opacity, 0, 1);
        compArray[0] *= compArray[3];
        compArray[1] *= compArray[3];
        compArray[2] *= compArray[3];
        gl.glColor4fv(compArray, 0);
    }

    protected static void applyNonPremultipliedAlphaColor(GL2 gl, Color color, double opacity) {
        float[] compArray = new float[4];
        color.getRGBComponents(compArray);
        compArray[3] = (float) WWMath.clamp(opacity, 0, 1);
        gl.glColor4fv(compArray, 0);
    }

    public double getOpacity() {
        return this.opacity;
    }

    public void setOpacity(double opacity) {
        var o = this.opacity;
        if (o!=opacity) {
            this.opacity = opacity < 0 ? 0 : opacity > 1 ? 1 : opacity;  // clamp to 0..1
            this.updateModifiedTime();
        }
    }
}