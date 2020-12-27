/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.google.common.collect.Iterables;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.terrain.SectorGeometry;

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
        this.sgWidth = st.lonDelta;
        this.sgHeight = st.latDelta;
        this.sgMinWE = st.lonMin;
        this.sgMinSN = st.latMin;
    }

    protected void computeTextureTransform(DrawContext dc, SurfaceTile tile, Transform t) {
        Sector s = tile.getSector();

        double tileWidth = s.lonDelta;
        double tileHeight = s.latDelta;
        t.VScale = tileHeight > 0 ? this.sgHeight / tileHeight : 1;
        t.HScale = tileWidth > 0 ? this.sgWidth / tileWidth : 1;

        double minLon = s.lonMin;
        double minLat = s.latMin;
        t.VShift = (this.sgMinSN - minLat) / this.sgHeight;
        t.HShift = (this.sgMinWE - minLon) / this.sgWidth;
    }

    protected Iterable<? extends SurfaceTile> getIntersectingTiles(DrawContext dc, SectorGeometry sg,
        Iterable<? extends SurfaceTile> tiles) {
        final Sector s = sg.getSector();
        return Iterables.filter(tiles, t -> s.intersectsInterior(t.getSector()));
    }
}
