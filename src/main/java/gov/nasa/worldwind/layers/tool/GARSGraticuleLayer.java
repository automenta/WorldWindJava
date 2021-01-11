/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.tool;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import static java.lang.Math.toRadians;

/**
 * Displays the geographic Global Area Reference System (GARS) graticule. The graticule has four levels. The first level
 * displays lines of latitude and longitude. The second level displays 30 minute square grid cells. The third level
 * displays 15 minute grid cells. The fourth and final level displays 5 minute grid cells.
 * <p>
 * This graticule is intended to be used on 2D globes because it is so dense.
 *
 * @version $Id: GARSGraticuleLayer.java 2384 2014-10-14 21:55:10Z tgaskins $
 */
public class GARSGraticuleLayer extends GraticuleLayer {
    public static final String GRATICULE_GARS_LEVEL_0 = "Graticule.GARSLevel0";
    public static final String GRATICULE_GARS_LEVEL_1 = "Graticule.GARSLevel1";
    public static final String GRATICULE_GARS_LEVEL_2 = "Graticule.GARSLevel2";
    public static final String GRATICULE_GARS_LEVEL_3 = "Graticule.GARSLevel3";

    protected static final int MIN_CELL_SIZE_PIXELS = 40; // TODO: make settable
    protected static final ArrayList<String> latLabels = new ArrayList<>(360);
    protected static final ArrayList<String> lonLabels = new ArrayList<>(720);
    protected static final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    protected static final String[][] level2Labels = {{"3", "4"}, {"1", "2"}};

    static {
        for (int i = 1; i <= 720; i++) {
            GARSGraticuleLayer.lonLabels.add(String.format("%03d", i));
        }

        for (int i = 0; i < 360; i++) {
            int length = GARSGraticuleLayer.chars.length();
            int i1 = i / length;
            int i2 = i % length;
            GARSGraticuleLayer.latLabels.add(String.format("%c%c", GARSGraticuleLayer.chars.charAt(i1), GARSGraticuleLayer.chars.charAt(i2)));
        }
    }

    protected final GraticuleTile[][] gridTiles = new GraticuleTile[18][36]; // 10 degrees row/col
    protected final Collection<Double> latitudeLabels = new ArrayList<>();
    protected final Collection<Double> longitudeLabels = new ArrayList<>();
    /**
     * Indicates the eye altitudes in meters below which each level should be displayed.
     */
    protected final double[] thresholds = {1200.0e3, 600.0e3, 180.0e3}; // 30 min, 15 min, 5 min
    protected String angleFormat = Angle.ANGLE_FORMAT_DMS;

    public GARSGraticuleLayer() {
        initRenderingParams();
        this.setPickEnabled(false);
        this.setName(Logging.getMessage("layers.LatLonGraticule.Name"));
    }

    protected static String[] getOrderedTypes() {
        return new String[] {
            GARSGraticuleLayer.GRATICULE_GARS_LEVEL_0,
            GARSGraticuleLayer.GRATICULE_GARS_LEVEL_1,
            GARSGraticuleLayer.GRATICULE_GARS_LEVEL_2,
            GARSGraticuleLayer.GRATICULE_GARS_LEVEL_3,
        };
    }

    protected static String getTypeFor(double resolution) {
        if (resolution >= 10)
            return GARSGraticuleLayer.GRATICULE_GARS_LEVEL_0;
        else if (resolution >= 0.5)
            return GARSGraticuleLayer.GRATICULE_GARS_LEVEL_1;
        else if (resolution >= 0.25)
            return GARSGraticuleLayer.GRATICULE_GARS_LEVEL_2;
        else if (resolution >= 5.0 / 60.0)
            return GARSGraticuleLayer.GRATICULE_GARS_LEVEL_3;

        return null;
    }

    private static Rectangle2D getGridRectangleForSector(Sector sector) {
        int x1 = GARSGraticuleLayer.getGridColumn(sector.lonMin);
        int x2 = GARSGraticuleLayer.getGridColumn(sector.lonMax);
        int y1 = GARSGraticuleLayer.getGridRow(sector.latMin);
        int y2 = GARSGraticuleLayer.getGridRow(sector.latMax);
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    // --- Graticule Rendering --------------------------------------------------------------

    private static Sector getGridSector(int row, int col) {
        int minLat = -90 + row * 10;
        int maxLat = minLat + 10;
        int minLon = -180 + col * 10;
        int maxLon = minLon + 10;
        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
    }

    private static int getGridColumn(Double longitude) {
        int col = (int) Math.floor((longitude + 180) / 10.0d);
        return Math.min(col, 35);
    }

    private static int getGridRow(Double latitude) {
        int row = (int) Math.floor((latitude + 90) / 10.0d);
        return Math.min(row, 17);
    }

    protected static String makeLabel(Sector sector, String graticuleType) {
        if (graticuleType.equals(GARSGraticuleLayer.GRATICULE_GARS_LEVEL_1)) {
            int iLat = (int) ((90 + sector.getCentroid().getLatitude().degrees) * 60 / 30);
            int iLon = (int) ((180 + sector.getCentroid().getLongitude().degrees) * 60 / 30);

            return GARSGraticuleLayer.lonLabels.get(iLon) + GARSGraticuleLayer.latLabels.get(iLat);
        } else if (graticuleType.equals(GARSGraticuleLayer.GRATICULE_GARS_LEVEL_2)) {
            int minutesLat = (int) ((90 + sector.latMin) * 60);
            int j = (minutesLat % 30) / 15;
            int minutesLon = (int) ((180 + sector.lonMin) * 60);
            int i = (minutesLon % 30) / 15;

            return GARSGraticuleLayer.level2Labels[j][i];
        } else {
            return "";
        }
    }

    /**
     * Get the graticule division and angular display format. Can be one of {@link Angle#ANGLE_FORMAT_DD} or {@link
     * Angle#ANGLE_FORMAT_DMS}.
     *
     * @return the graticule division and angular display format.
     */
    public String getAngleFormat() {
        return this.angleFormat;
    }

    /**
     * Sets the graticule division and angular display format. Can be one of {@link Angle#ANGLE_FORMAT_DD}, {@link
     * Angle#ANGLE_FORMAT_DMS} of {@link Angle#ANGLE_FORMAT_DM}.
     *
     * @param format the graticule division and angular display format.
     * @throws IllegalArgumentException is <code>format</code> is null.
     */
    public void setAngleFormat(String format) {
        if (format == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.angleFormat.equals(format))
            return;

        this.angleFormat = format;
        this.clearTiles();
        this.lastEyePoint = null; // force graticule to update
    }

    /**
     * Indicates the eye altitude below which the 30 minute grid is displayed.
     *
     * @return the eye altitude in meters below which the 30 minute grid is displayed.
     */
    public double get30MinuteThreshold() {
        return this.thresholds[0];
    }

    /**
     * Specifies the eye altitude below which the 30 minute grid is displayed.
     *
     * @param altitude the eye altitude in meters below which the 30 minute grid is displayed.
     */
    public void set30MinuteThreshold(double altitude) {
        this.thresholds[0] = altitude;
    }

    /**
     * Indicates the eye altitude below which the 15 minute grid is displayed.
     *
     * @return the eye altitude in meters below which the 15 minute grid is displayed.
     */
    public double get15MinuteThreshold() {
        return this.thresholds[1];
    }

    /**
     * Specifies the eye altitude below which the 15 minute grid is displayed.
     *
     * @param altitude the eye altitude in meters below which the 15 minute grid is displayed.
     */
    public void set15MinuteThreshold(double altitude) {
        this.thresholds[1] = altitude;
    }

    /**
     * Indicates the eye altitude below which the 5 minute grid is displayed.
     *
     * @return the eye altitude in meters below which the 5 minute grid is displayed.
     */
    public double get5MinuteThreshold() {
        return this.thresholds[2];
    }

    /**
     * Specifies the eye altitude below which the 5 minute grid is displayed.
     *
     * @param altitude the eye altitude in meters below which the 5 minute grid is displayed.
     */
    public void set5MinuteThreshold(double altitude) {
        this.thresholds[2] = altitude;
    }

    protected void initRenderingParams() {
        GraticuleRenderingParams params;
        // Ten degrees grid
        params = new GraticuleRenderingParams();
        params.set(GraticuleRenderingParams.KEY_LINE_COLOR, Color.WHITE);
        params.set(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.WHITE);
        params.set(GraticuleRenderingParams.KEY_LABEL_FONT, Font.decode("Arial-Bold-16"));
        setRenderingParams(GARSGraticuleLayer.GRATICULE_GARS_LEVEL_0, params);
        // One degree
        params = new GraticuleRenderingParams();
        params.set(GraticuleRenderingParams.KEY_LINE_COLOR, Color.YELLOW);
        params.set(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.YELLOW);
        params.set(GraticuleRenderingParams.KEY_LABEL_FONT, Font.decode("Arial-Bold-14"));
        setRenderingParams(GARSGraticuleLayer.GRATICULE_GARS_LEVEL_1, params);
        // 1/10th degree - 1/6th (10 minutes)
        params = new GraticuleRenderingParams();
        params.set(GraticuleRenderingParams.KEY_LINE_COLOR, Color.GREEN);
        params.set(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.GREEN);
        setRenderingParams(GARSGraticuleLayer.GRATICULE_GARS_LEVEL_2, params);
        // 1/100th degree - 1/60th (one minutes)
        params = new GraticuleRenderingParams();
        params.set(GraticuleRenderingParams.KEY_LINE_COLOR, Color.CYAN);
        params.set(GraticuleRenderingParams.KEY_LABEL_COLOR, Color.CYAN);
        setRenderingParams(GARSGraticuleLayer.GRATICULE_GARS_LEVEL_3, params);
    }

    protected void clear(DrawContext dc) {
        super.clear(dc);
        this.latitudeLabels.clear();
        this.longitudeLabels.clear();
        this.applyTerrainConformance();
    }

    private void applyTerrainConformance() {
        String[] graticuleType = GARSGraticuleLayer.getOrderedTypes();
        for (String type : graticuleType) {
            getRenderingParams(type).set(
                GraticuleRenderingParams.KEY_LINE_CONFORMANCE, this.terrainConformance);
        }
    }

    /**
     * Select the visible grid elements
     *
     * @param dc the current <code>DrawContext</code>.
     */
    protected void selectRenderables(DrawContext dc) {
        ArrayList<GraticuleTile> tileList = getVisibleTiles(dc);
        if (!tileList.isEmpty()) {
            for (GraticuleTile gz : tileList) {
                // Select tile visible elements
                gz.selectRenderables(dc);
            }
        }
    }

    protected ArrayList<GraticuleTile> getVisibleTiles(DrawContext dc) {
        ArrayList<GraticuleTile> tileList = new ArrayList<>();
        Sector vs = dc.getVisibleSector();
        if (vs != null) {
            Rectangle2D gridRectangle = GARSGraticuleLayer.getGridRectangleForSector(vs);
            for (int row = (int) gridRectangle.getY(); row <= gridRectangle.getY() + gridRectangle.getHeight();
                row++) {
                for (int col = (int) gridRectangle.getX(); col <= gridRectangle.getX() + gridRectangle.getWidth();
                    col++) {
                    if (gridTiles[row][col] == null)
                        gridTiles[row][col] = new GraticuleTile(GARSGraticuleLayer.getGridSector(row, col), 20, 0);
                    if (gridTiles[row][col].isInView(dc))
                        tileList.add(gridTiles[row][col]);
                    else
                        gridTiles[row][col].clearRenderables();
                }
            }
        }
        return tileList;
    }

    protected void clearTiles() {
        for (int row = 0; row < 18; row++) {
            for (int col = 0; col < 36; col++) {
                if (this.gridTiles[row][col] != null) {
                    this.gridTiles[row][col].clearRenderables();
                    this.gridTiles[row][col] = null;
                }
            }
        }
    }

    protected String makeAngleLabel(Angle angle, double resolution) {
        double epsilon = 0.000000001;
        String label;
        if (this.getAngleFormat().equals(Angle.ANGLE_FORMAT_DMS)) {
            if (resolution >= 1)
                label = angle.toDecimalDegreesString(0);
            else {
                double[] dms = angle.toDMS();
                if (dms[1] < epsilon && dms[2] < epsilon)
                    label = String.format("%4d\u00B0", (int) dms[0]);
                else if (dms[2] < epsilon)
                    label = String.format("%4d\u00B0 %2d\u2019", (int) dms[0], (int) dms[1]);
                else
                    label = angle.toDMSString();
            }
        } else if (this.getAngleFormat().equals(Angle.ANGLE_FORMAT_DM)) {
            if (resolution >= 1)
                label = angle.toDecimalDegreesString(0);
            else {
                double[] dms = angle.toDMS();
                if (dms[1] < epsilon && dms[2] < epsilon)
                    label = String.format("%4d\u00B0", (int) dms[0]);
                else if (dms[2] < epsilon)
                    label = String.format("%4d\u00B0 %2d\u2019", (int) dms[0], (int) dms[1]);
                else
                    label = angle.toDMString();
            }
        } else // default to decimal degrees
        {
            if (resolution >= 1)
                label = angle.toDecimalDegreesString(0);
            else if (resolution >= 0.1)
                label = angle.toDecimalDegreesString(1);
            else if (resolution >= 0.01)
                label = angle.toDecimalDegreesString(2);
            else if (resolution >= 0.001)
                label = angle.toDecimalDegreesString(3);
            else
                label = angle.toDecimalDegreesString(4);
        }

        return label;
    }

    protected void addLevel0Label(double value, String labelType, String graticuleType, double resolution,
        LatLon labelOffset) {
        if (labelType.equals(GraticuleLayer.GridElement.TYPE_LATITUDE_LABEL)) {
            if (!graticuleType.equals(GARSGraticuleLayer.GRATICULE_GARS_LEVEL_0) || !this.latitudeLabels.contains(value)) {

                this.latitudeLabels.add(value);
                String label = makeAngleLabel(new Angle(value), resolution);
                GeographicText text = new UserFacingText(label,
                    Position.fromDegrees(value, labelOffset.getLongitude().degrees, 0));
                text.setPriority(resolution * 1.0e6);
                this.addRenderable(text, graticuleType);
            }
        } else if (labelType.equals(GraticuleLayer.GridElement.TYPE_LONGITUDE_LABEL)) {
            if (!graticuleType.equals(GARSGraticuleLayer.GRATICULE_GARS_LEVEL_0) || !this.longitudeLabels.contains(value)) {
                this.longitudeLabels.add(value);
                String label = makeAngleLabel(new Angle(value), resolution);
                GeographicText text = new UserFacingText(label,
                    Position.fromDegrees(labelOffset.getLatitude().degrees, value, 0));
                text.setPriority(resolution * 1.0e6);
                this.addRenderable(text, graticuleType);
            }
        }
    }

    // --- Graticule tile ----------------------------------------------------------------------

    protected class GraticuleTile {
        private final Sector sector;
        private final int divisions;
        private final int level;

        private ArrayList<GraticuleLayer.GridElement> gridElements;
        private ArrayList<GraticuleTile> subTiles;

        public GraticuleTile(Sector sector, int divisions, int level) {
            this.sector = sector;
            this.divisions = divisions;
            this.level = level;
        }

        public Extent getExtent(Globe globe, double ve) {
            return Sector.computeBoundingCylinder(globe, ve, this.sector);
        }

        @SuppressWarnings("RedundantIfStatement")
        public boolean isInView(DrawContext dc) {
            if (!dc.getView().getFrustumInModelCoordinates().intersects(
                this.getExtent(dc.getGlobe(), dc.getVerticalExaggeration())))
                return false;

            if (this.level != 0) {
                if (dc.getView().getEyePosition().getAltitude() > thresholds[this.level - 1])
                    return false;
            }

            return true;
        }

        public double getSizeInPixels(DrawContext dc) {
            View view = dc.getView();
            Vec4 centerPoint = GraticuleLayer.getSurfacePoint(dc, this.sector.getCentroid().getLatitude(),
                this.sector.getCentroid().getLongitude());
            double distance = view.getEyePoint().distanceTo3(centerPoint);
            double tileSizeMeter = toRadians(this.sector.latDelta) * dc.getGlobe().getRadius();
            return tileSizeMeter / view.computePixelSizeAtDistance(distance);
        }

        public void selectRenderables(DrawContext dc) {
            if (this.gridElements == null)
                this.createRenderables();

            String graticuleType = GARSGraticuleLayer.getTypeFor(this.sector.latDelta);
            if (this.level == 0 && dc.getView().getEyePosition().getAltitude() > thresholds[0]) {
                LatLon labelOffset = GraticuleLayer.computeLabelOffset(dc);

                for (GraticuleLayer.GridElement ge : this.gridElements) {
                    if (ge.isInView(dc)) {
                        // Add level zero bounding lines and labels
                        if (ge.type.equals(GraticuleLayer.GridElement.TYPE_LINE_SOUTH) || ge.type.equals(GraticuleLayer.GridElement.TYPE_LINE_NORTH)
                            || ge.type.equals(GraticuleLayer.GridElement.TYPE_LINE_WEST)) {
                            addRenderable(ge.renderable, graticuleType);
                            String labelType = ge.type.equals(GraticuleLayer.GridElement.TYPE_LINE_SOUTH)
                                || ge.type.equals(GraticuleLayer.GridElement.TYPE_LINE_NORTH) ?
                                GraticuleLayer.GridElement.TYPE_LATITUDE_LABEL : GraticuleLayer.GridElement.TYPE_LONGITUDE_LABEL;
                            GARSGraticuleLayer.this.addLevel0Label(ge.value, labelType, graticuleType,
                                this.sector.latDelta, labelOffset);
                        }
                    }
                }

                if (dc.getView().getEyePosition().getAltitude() > thresholds[0])
                    return;
            }

            // Select tile grid elements
            double eyeDistance = dc.getView().getEyePosition().getAltitude();

            if (this.level == 0 && eyeDistance <= thresholds[0]
                || this.level == 1 && eyeDistance <= thresholds[1]
                || this.level == 2) {
                double resolution = this.sector.latDelta / this.divisions;
                graticuleType = GARSGraticuleLayer.getTypeFor(resolution);
                for (GraticuleLayer.GridElement ge : this.gridElements) {
                    if (ge.isInView(dc)) {
                        addRenderable(ge.renderable, graticuleType);
                    }
                }
            }

            if (this.level == 0 && eyeDistance > thresholds[1])
                return;
            else if (this.level == 1 && eyeDistance > thresholds[2])
                return;
            else if (this.level == 2)
                return;

            // Select child elements
            if (this.subTiles == null)
                createSubTiles();
            for (GraticuleTile gt : this.subTiles) {
                if (gt.isInView(dc)) {
                    gt.selectRenderables(dc);
                } else
                    gt.clearRenderables();
            }
        }

        public void clearRenderables() {
            if (this.gridElements != null) {
                this.gridElements.clear();
                this.gridElements = null;
            }
            if (this.subTiles != null) {
                for (GraticuleTile gt : this.subTiles) {
                    gt.clearRenderables();
                }
                this.subTiles.clear();
                this.subTiles = null;
            }
        }

        private void createSubTiles() {
            this.subTiles = new ArrayList<>();
            Sector[] sectors = this.sector.subdivide(this.divisions);
            int nextLevel = this.level + 1;
            int subDivisions = 10;
            if (nextLevel == 1)
                subDivisions = 2;
            else if (nextLevel == 2)
                subDivisions = 3;
            for (Sector s : sectors) {
                this.subTiles.add(new GraticuleTile(s, subDivisions, nextLevel));
            }
        }

        /**
         * Create the grid elements
         */
        private void createRenderables() {
            this.gridElements = new ArrayList<>();

            double step = sector.latDelta / this.divisions;

            // Generate meridians with labels
            double lon = sector.lonMin + (this.level == 0 ? 0 : step);
            while (lon < sector.lonMax - step / 2) {
                Angle longitude = new Angle(lon);
                // Meridian
                Collection<Position> positions = new ArrayList<>(2);
                positions.add(new Position(this.sector.latMin(), longitude, 0));
                positions.add(new Position(this.sector.latMax(), longitude, 0));

                Object line = GraticuleLayer.createLineRenderable(positions, AVKey.LINEAR);
                Sector sector = Sector.fromDegrees(
                    this.sector.latMin, this.sector.latMax, lon, lon);
                String lineType = lon == this.sector.lonMin ?
                    GraticuleLayer.GridElement.TYPE_LINE_WEST : GraticuleLayer.GridElement.TYPE_LINE;
                GraticuleLayer.GridElement ge = new GraticuleLayer.GridElement(sector, line, lineType);
                ge.value = lon;
                this.gridElements.add(ge);

                // Increase longitude
                lon += step;
            }

            // Generate parallels
            double lat = this.sector.latMin + (this.level == 0 ? 0 : step);
            while (lat < this.sector.latMax - step / 2) {
                Angle latitude = new Angle(lat);
                Collection<Position> positions = new ArrayList<>(2);
                positions.add(new Position(latitude, this.sector.lonMin(), 0));
                positions.add(new Position(latitude, this.sector.lonMax(), 0));

                Object line = GraticuleLayer.createLineRenderable(positions, AVKey.LINEAR);
                Sector sector = Sector.fromDegrees(
                    lat, lat, this.sector.lonMin, this.sector.lonMax);
                String lineType = lat == this.sector.latMin ?
                    GraticuleLayer.GridElement.TYPE_LINE_SOUTH : GraticuleLayer.GridElement.TYPE_LINE;
                GraticuleLayer.GridElement ge = new GraticuleLayer.GridElement(sector, line, lineType);
                ge.value = lat;
                this.gridElements.add(ge);

                // Increase latitude
                lat += step;
            }

            // Draw and label a parallel at the top of the graticule. The line is apparent only on 2D globes.
            if (this.sector.latMax().equals(Angle.POS90)) {
                Collection<Position> positions = new ArrayList<>(2);
                positions.add(new Position(Angle.POS90, this.sector.lonMin(), 0));
                positions.add(new Position(Angle.POS90, this.sector.lonMax(), 0));

                Object line = GraticuleLayer.createLineRenderable(positions, AVKey.LINEAR);
                Sector sector = Sector.fromDegrees(
                    90, 90, this.sector.lonMin, this.sector.lonMax);
                GraticuleLayer.GridElement ge = new GraticuleLayer.GridElement(sector, line, GraticuleLayer.GridElement.TYPE_LINE_NORTH);
                ge.value = 90;
                this.gridElements.add(ge);
            }

            double resolution = this.sector.latDelta / this.divisions;
            if (this.level == 0) {
                Sector[] sectors = this.sector.subdivide(20);
                for (int j = 0; j < 20; j++) {
                    for (int i = 0; i < 20; i++) {
                        Sector sector = sectors[j * 20 + i];
                        String label = GARSGraticuleLayer.makeLabel(sector, GARSGraticuleLayer.GRATICULE_GARS_LEVEL_1);
                        addLabel(label, sectors[j * 20 + i], resolution);
                    }
                }
            } else if (this.level == 1) {
                String label = GARSGraticuleLayer.makeLabel(this.sector, GARSGraticuleLayer.GRATICULE_GARS_LEVEL_1);

                Sector[] sectors = this.sector.subdivide();
                addLabel(label + '3', sectors[0], resolution);
                addLabel(label + '4', sectors[1], resolution);
                addLabel(label + '1', sectors[2], resolution);
                addLabel(label + '2', sectors[3], resolution);
            } else if (this.level == 2) {
                String label = GARSGraticuleLayer.makeLabel(this.sector, GARSGraticuleLayer.GRATICULE_GARS_LEVEL_1);
                label += GARSGraticuleLayer.makeLabel(this.sector, GARSGraticuleLayer.GRATICULE_GARS_LEVEL_2);

                resolution = 0.26; // make label priority a little higher than level 2's
                Sector[] sectors = this.sector.subdivide(3);
                addLabel(label + '7', sectors[0], resolution);
                addLabel(label + '8', sectors[1], resolution);
                addLabel(label + '9', sectors[2], resolution);
                addLabel(label + '4', sectors[3], resolution);
                addLabel(label + '5', sectors[4], resolution);
                addLabel(label + '6', sectors[5], resolution);
                addLabel(label + '1', sectors[6], resolution);
                addLabel(label + '2', sectors[7], resolution);
                addLabel(label + '3', sectors[8], resolution);
            }
        }

        protected void addLabel(String label, Sector sector, double resolution) {
            GeographicText text = new UserFacingText(label, new Position(sector.getCentroid(), 0));
            text.setPriority(resolution * 1.0e6);
            GraticuleLayer.GridElement ge = new GraticuleLayer.GridElement(sector, text, GraticuleLayer.GridElement.TYPE_GRIDZONE_LABEL);
            this.gridElements.add(ge);
        }
    }
}
