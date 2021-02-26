package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContextImpl;

public class LowResTerrain implements Terrain {

    private final DrawContextImpl draw;

    public LowResTerrain(DrawContextImpl draw) {
        this.draw = draw;
    }

    @Override public Globe globe() {
        return draw.getGlobe();
    }

    public double verticalExaggeration() {
        return draw.getVerticalExaggeration();
    }

    public Vec4 surfacePoint(Position position) {

        SectorGeometryList sectorGeometry = draw.getSurfaceGeometry();
        if (sectorGeometry == null)
            return null;

        Vec4 pt = sectorGeometry.getSurfacePoint(position);
        if (pt == null) {
            final Globe g = this.globe();
            double elevation = g.elevation(position.getLat(), position.getLon());
            pt = g.computePointFromPosition(position,
                position.getAltitude() + elevation * this.verticalExaggeration());
        }

        return pt;
    }

    public Vec4 surfacePoint(Angle lat, Angle lon, double metersOffset) {

        SectorGeometryList s = draw.getSurfaceGeometry();
        if (s == null)
            return null;

        Vec4 pt = s.getSurfacePoint(lat, lon, metersOffset);

        if (pt == null) {
            Globe g = this.globe();
            return g.computePointFromPosition(lat, lon,
    metersOffset + g.elevation(lat, lon) * this.verticalExaggeration()
            );
        } else
            return pt;
    }

    public Intersection[] intersect(Position pA, Position pB) {

        Vec4 ptA = this.surfacePoint(pA);
        if (ptA == null)
            return null;
        Vec4 ptB = this.surfacePoint(pB);
        if (ptB == null)
            return null;

        return draw.getSurfaceGeometry().intersect(new Line(ptA, ptB.subtract3(ptA)));
    }

    public Intersection[] intersect(Position pA, Position pB, int altitudeMode) {
//            if (pA == null || pB == null) {
//                String msg = Logging.getMessage("nullValue.PositionIsNull");
//                Logging.logger().severe(msg);
//                throw new IllegalArgumentException(msg);
//            }

        // The intersect method expects altitudes to be relative to ground, so make them so if they aren't already.
        double altitudeA = pA.elevation;
        double altitudeB = pB.elevation;
        if (altitudeMode == WorldWind.ABSOLUTE) {
            altitudeA -= this.elevation(pA);
            altitudeB -= this.elevation(pB);
        } else if (altitudeMode == WorldWind.CLAMP_TO_GROUND) {
            altitudeA = 0;
            altitudeB = 0;
        }

        return this.intersect(new Position(pA, altitudeA), new Position(pB, altitudeB));
    }

    /** TODO NaN instead of null */
    public Double elevation(LatLon location) {
        Vec4 pt = this.surfacePoint(location.getLat(), location.getLon(), 0);
        return pt == null ?
            null
            :
            this.globe().computePointFromPosition(location.lat, location.lon, 0).distanceTo3(pt);
    }
}