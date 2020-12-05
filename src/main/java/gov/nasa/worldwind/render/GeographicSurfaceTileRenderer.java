/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.terrain.SectorGeometry;

import java.util.ArrayList;

import static java.lang.Math.toRadians;

/**
 * @author tag
 * @version $Id: GeographicSurfaceTileRenderer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GeographicSurfaceTileRenderer extends SurfaceTileRenderer {
    private double sgWidth;
    private double sgHeight;
    private double sgMinWE;
    private double sgMinSN;

    protected void preComputeTextureTransform(DrawContext dc, SectorGeometry sg, Transform t) {
        Sector st = sg.getSector();
        this.sgWidth = toRadians(st.lonDelta);
        this.sgHeight = toRadians(st.latDelta);
        this.sgMinWE = st.lonMin().radians;
        this.sgMinSN = st.latMin().radians;
    }

    protected void computeTextureTransform(DrawContext dc, SurfaceTile tile, Transform t) {
        Sector st = tile.getSector();
        double tileWidth = toRadians(st.lonDelta);
        double tileHeight = toRadians(st.latDelta);
        double minLon = st.lonMin().radians;
        double minLat = st.latMin().radians;

        t.VScale = tileHeight > 0 ? this.sgHeight / tileHeight : 1;
        t.HScale = tileWidth > 0 ? this.sgWidth / tileWidth : 1;
        t.VShift = -(minLat - this.sgMinSN) / this.sgHeight;
        t.HShift = -(minLon - this.sgMinWE) / this.sgWidth;
    }

    protected Iterable<SurfaceTile> getIntersectingTiles(DrawContext dc, SectorGeometry sg,
        Iterable<? extends SurfaceTile> tiles) {
        ArrayList<SurfaceTile> intersectingTiles = null;

        for (SurfaceTile tile : tiles) {
            if (!tile.getSector().intersectsInterior(sg.getSector()))
                continue;

            if (intersectingTiles == null) // lazy creation because most common case is no intersecting tiles
                intersectingTiles = new ArrayList<>();

            intersectingTiles.add(tile);
        }

        return intersectingTiles; // will be null if no intersecting tiles
    }
}
