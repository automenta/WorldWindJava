/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.Terrain;
import gov.nasa.worldwind.util.*;

import java.nio.FloatBuffer;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: ShapefileMultiPatch.java 2324 2014-09-17 20:25:35Z dcollins $
 */
public class ShapefileMultiPatch { //extends ShapefileRenderable implements OrderedRenderable {

    protected ShapeAttributes initNormalAttrs;
    protected ShapeAttributes initHighlightAttrs;
    protected ArrayList<Record> records;
    protected ArrayList<Renderable> polygons;

    /**
     * Creates a new ShapefileMultiPatch with the specified shapefile. The normal attributes and the highlight
     * attributes for each ShapefileRenderable.Record are assigned default values. In order to modify
     * ShapefileRenderable.Record shape attributes or key-value attributes during construction, use {@link
     * #ShapefileMultiPatch(Shapefile, ShapeAttributes, ShapeAttributes, ShapefileRenderable.AttributeDelegate)}.
     *
     * @param shapefile The shapefile to display.
     * @throws IllegalArgumentException if the shapefile is null.
     */
    public ShapefileMultiPatch(Shapefile shapefile) {
        if (shapefile == null) {
            String msg = Logging.getMessage("nullValue.ShapefileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.init(shapefile, null, null, null);
    }

    /**
     * Creates a new ShapefileMultiPatch with the specified shapefile. The normal attributes, the highlight attributes
     * and the attribute delegate are optional. Specifying a non-null value for normalAttrs or highlightAttrs causes
     * each ShapefileRenderable.Record to adopt those attributes. Specifying a non-null value for the attribute delegate
     * enables callbacks during creation of each ShapefileRenderable.Record. See {@link AttributeDelegate} for more
     * information.
     *
     * @param shapefile         The shapefile to display.
     * @param normalAttrs       The normal attributes for each ShapefileRenderable.Record. May be null to use the
     *                          default attributes.
     * @param highlightAttrs    The highlight attributes for each ShapefileRenderable.Record. May be null to use the
     *                          default highlight attributes.
     * @param attributeDelegate Optional callback for configuring each ShapefileRenderable.Record's shape attributes and
     *                          key-value attributes. May be null.
     * @throws IllegalArgumentException if the shapefile is null.
     */
    public ShapefileMultiPatch(Shapefile shapefile, ShapeAttributes normalAttrs, ShapeAttributes highlightAttrs,
        ShapefileRenderable.AttributeDelegate attributeDelegate) {
        if (shapefile == null) {
            String msg = Logging.getMessage("nullValue.ShapefileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.init(shapefile, normalAttrs, highlightAttrs, attributeDelegate);
    }

    protected static boolean mustAssembleRecord(ShapefileRecord shapefileRecord) {
        double[] bounds = shapefileRecord.getBoundingRectangle();
        Sector aoi = new Sector(Angle.fromDegreesLatitude(42.36), Angle.fromDegreesLatitude(42.37),
            Angle.fromDegreesLongitude(-71.075), Angle.fromDegreesLongitude(-71.055));
        if (!aoi.contains(LatLon.fromDegrees(bounds[0], bounds[2]))) {
            return false;
        }
        return shapefileRecord.getNumberOfParts() > 0
            && shapefileRecord.getNumberOfPoints() > 0
            && !shapefileRecord.isNullRecord();
    }

    protected static ShapefileMultiPatch.Record createRecord(ShapefileRecord shapefileRecord) {
        return new Record(shapefileRecord);
    }

    protected void generatePolygon(Position[] locations) {
        ArrayList<Position> positions = new ArrayList<>(Arrays.asList(locations));
        polygons.add(new Polygon(positions));
    }

    protected void generateTriangleStrip(Position[] locations) {
        for (int i = 0; i < locations.length - 2; i++) {
            generatePolygon(new Position[] {locations[i], locations[i + 1], locations[i + 2]});
        }
    }

    protected void generateTriangleFan(Position[] locations) {
        for (int i = 1; i < locations.length - 1; i++) {
            generatePolygon(new Position[] {locations[0], locations[i], locations[i + 1]});
        }
    }

    public ArrayList<Renderable> getRenderables() {
        if (this.polygons == null) {
            polygons = new ArrayList<>();
            double[] location = new double[2];
            for (Record record : this.records) {
                for (int i = 0; i < record.getBoundaryCount(); i++) {
                    VecBuffer points = record.getBoundaryPoints(i);
                    double[] zValues = record.getZValues();
                    Position[] locations = new Position[points.getSize()];
                    for (int j = 0; j < points.getSize(); j++) {
                        points.get(j, location);
                        locations[j] = new Position(Angle.fromDegrees(location[1]), Angle.fromDegrees(location[0]),
                            zValues[j]);
                    }
                    switch (record.getBoundaryType(i)) {
                        case TriangleStrip -> generateTriangleStrip(locations);
                        case TriangleFan -> generateTriangleFan(locations);
                        case OuterRing, InnerRing, FirstRing, Ring -> generatePolygon(locations);
                        default -> {
                            String message = Logging.getMessage("generic.UnrecognizedDataType",
                                record.getBoundaryType(i));
                            Logging.logger().severe(message);
                            throw new IllegalArgumentException(message);
                        }
                    }
                }
            }
        }

        return polygons;
    }

    private void init(Shapefile shapefile, ShapeAttributes normalAttrs, ShapeAttributes highlightAttrs,
        ShapefileRenderable.AttributeDelegate attributeDelegate) {
        double[] boundingRect = shapefile.getBoundingRectangle();
        if (boundingRect == null) // suppress record assembly for empty shapefiles
        {
            return;
        }

//        this.sector = Sector.fromDegrees(boundingRect);
        this.initNormalAttrs = normalAttrs;
        this.initHighlightAttrs = highlightAttrs;
//        this.initAttributeDelegate = attributeDelegate;
        this.assembleRecords(shapefile);
    }

    protected void assembleRecords(Shapefile shapefile) {
        this.records = new ArrayList<>();

        int shapeNo = 0;
        while (shapefile.hasNext()) {
            ShapefileRecord shapefileRecord = shapefile.nextRecord();

//            if (shapeNo>4000 && shapeNo<6000)
            {
//            if (shapeNo>8000 && shapeNo<10000) {
//            if (shapeNo>12000 && shapeNo<14000) {
//            if (shapeNo>82000 && shapeNo<92000) {
                //if (shapeNo>92000 && shapeNo<102000) {
                //if (shapeNo>92000 && shapeNo<102000) {
                if (ShapefileMultiPatch.mustAssembleRecord(shapefileRecord)) {
                    this.assembleRecord(shapefileRecord);
                    shapeNo++;
                }
            }
        }

        System.out.println(shapeNo);
        this.records.trimToSize(); // Reduce memory overhead from unused ArrayList capacity.
    }

    protected void addRecord(ShapefileRecord shapefileRecord, Record renderableRecord) {
        this.records.add(renderableRecord);
    }

    protected void assembleRecord(ShapefileRecord shapefileRecord) {
        Record record = ShapefileMultiPatch.createRecord(shapefileRecord);
        this.addRecord(shapefileRecord, record);
    }

    protected void tessellateTriangleStrip(Terrain terrain, FloatBuffer vertices, Position[] locations, Vec4 refPt) {
        for (int i = 0; i < locations.length - 2; i++) {
            tessellateContour(terrain, vertices, new Position[] {locations[i], locations[i + 1], locations[i + 2]},
                refPt);
        }
    }

    protected void tessellateTriangleFan(Terrain terrain, FloatBuffer vertices, Position[] locations, Vec4 refPt) {
        for (int i = 1; i < locations.length - 1; i++) {
            tessellateContour(terrain, vertices, new Position[] {locations[0], locations[i], locations[i + 1]}, refPt);
        }
    }

    protected void tessellateContour(Terrain terrain, FloatBuffer vertices, Position[] locations, Vec4 refPt) {
    }

    protected void tessellateTile(Terrain terrain, Tile tile) { //, ShapeData shapeData) {
//        // Allocate the model coordinate vertices to hold the upper and lower points for all records in the tile. The
//        // records in the tile never changes, so the number of vertices in the tile never changes.
//        //ArrayList<Float> vertices = new ArrayList<>();
//        FloatBuffer vertices = shapeData.vertices;
//        if (vertices == null) {
//            int numPoints = 0;
//            for (Record record : tile.records) {
//                for (int i = 0; i < record.getBoundaryCount(); i++) {
//                    VecBuffer points = record.getBoundaryPoints(i);
//                    switch (record.getBoundaryType(i)) {
//                        case TriangleStrip:
//                        case TriangleFan:
//                            numPoints += (points.getSize() - 2) * 3;
//                            break;
//                        case OuterRing:
//                        case InnerRing:
//                        case FirstRing:
//                        case Ring:
//                            numPoints += points.getSize();
//                            break;
//                        default:
//                            String message = Logging.getMessage("generic.UnrecognizedDataType", record.getBoundaryType(i));
//                            Logging.logger().severe(message);
//                            throw new IllegalArgumentException(message);
//                    }
//                }
//            }
//            vertices = Buffers.newDirectFloatBuffer(2 * VERTEX_STRIDE * numPoints);
//        }
//
//        double[] location = new double[2];
//        Vec4 refPt = null;
//        // Generate the model coordinate vertices and indices for all records in the tile. This may include records that
//        // are marked as not visible, as recomputing the vertices and indices for record visibility changes would be
//        // expensive. The tessellated interior and outline indices are generated only once, since each record's indices
//        // never change.
//        for (Record record : tile.records) {
//            this.tess.setEnabled(record.interiorIndices == null); // generate polygon interior and outline indices once
//            this.tess.reset();
//            this.tess.setPolygonNormal(0, 0, 1); // tessellate in geographic coordinates
//            this.tess.beginPolygon();
//
//            for (int i = 0; i < record.getBoundaryCount(); i++) {
//                VecBuffer points = record.getBoundaryPoints(i);
//                double[] zValues = record.getZValues();
//                Position[] locations = new Position[points.getSize()];
//                for (int j = 0; j < points.getSize(); j++) {
//                    points.get(j, location);
//                    locations[j] = new Position(Angle.fromDegrees(location[1]), Angle.fromDegrees(location[0]), zValues[j]);
//                }
//                if (refPt == null) { // first vertex in the tile
//                    refPt = terrain.getSurfacePoint(locations[0].latitude, locations[0].longitude, 0);
//                }
//                switch (record.getBoundaryType(i)) {
//                    case TriangleStrip:
//                        tessellateTriangleStrip(terrain, vertices, locations, refPt);
//                        break;
//                    case TriangleFan:
//                        tessellateTriangleFan(terrain, vertices, locations, refPt);
//                        break;
//                    case OuterRing:
//                    case InnerRing:
//                    case FirstRing:
//                    case Ring:
//                        // TODO: Test
//                        Logging.logger().severe("Patch type not tested.");
//                        tessellateContour(terrain, vertices, locations, refPt);
//                        break;
//                    default:
//                        String message = Logging.getMessage("generic.UnrecognizedDataType", record.getBoundaryType(i));
//                        Logging.logger().severe(message);
//                        throw new IllegalArgumentException(message);
//                }
//            }
//
//            this.tess.endPolygon();
//            this.assembleRecordIndices(this.tess, record);
//        }
//        shapeData.vertices = (FloatBuffer) vertices.rewind();
//        shapeData.referencePoint = refPt;
//        shapeData.transformMatrix = Matrix.fromTranslation(refPt.x, refPt.y, refPt.z);
//        shapeData.vboExpired = true;
    }

    public static class Record { // extends ShapefileRenderable.Record {

        // Record properties.
        // protected Double height; // may be null
        // Data structures supporting drawing.
        protected final int firstPartNumber;
        protected final int numberOfParts;
        protected final int numberOfPoints;
        protected final double[] zValues;
        protected final CompoundVecBuffer pointBuffer;
        final ShapefileRecordMultiPatch.PartType[] partTypes;

        public Record(ShapefileRecord shapefileRecord) {
            //super(shapefileRenderable, shapefileRecord);
            this.firstPartNumber = shapefileRecord.getFirstPartNumber();
            this.numberOfParts = shapefileRecord.getNumberOfParts();
            this.numberOfPoints = shapefileRecord.getNumberOfPoints();
            this.pointBuffer = shapefileRecord.getShapeFile().getPointBuffer();
            this.zValues = ((ShapefileRecordMultiPatch) shapefileRecord).getZValues();
            this.partTypes = ((ShapefileRecordMultiPatch) shapefileRecord).getPartTypes();
//            this.height = ShapefileUtils.extractHeightAttribute(shapefileRecord); // may be null
        }

        public int getBoundaryCount() {
            return this.numberOfParts;
        }

        public VecBuffer getBoundaryPoints(int index) {
            if (index < 0 || index >= this.numberOfParts) {
                String msg = Logging.getMessage("generic.indexOutOfRange", index);
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            synchronized (this.pointBuffer) // synchronize access to the Shapefile's shared pointBuffer
            {
                return this.pointBuffer.subBuffer(this.firstPartNumber + index);
            }
        }

        public double[] getZValues() {
            return this.zValues;
        }

        public ShapefileRecordMultiPatch.PartType getBoundaryType(int partNo) {
            return this.partTypes[partNo];
        }
    }
}
