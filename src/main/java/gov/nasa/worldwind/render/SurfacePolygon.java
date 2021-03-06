/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.ogc.kml.KMLConstants;
import gov.nasa.worldwind.layers.ogc.kml.impl.KMLExportUtil;
import gov.nasa.worldwind.util.*;

import javax.xml.stream.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.logging.Level;

import static gov.nasa.worldwind.util.WWUtil.sizeEstimate;

/**
 * @author dcollins
 * @version $Id: SurfacePolygon.java 3436 2015-10-28 17:43:24Z tgaskins $
 */
@SuppressWarnings("unchecked")
public class SurfacePolygon extends AbstractSurfaceShape implements GeographicExtent, Exportable {
    protected static GLUtessellator tess;
    protected static GLUTessellatorSupport.CollectPrimitivesCallback tessCallback;
    /* The polygon's boundaries. */
    protected final List<Iterable<? extends LatLon>> boundaries = new ArrayList<>();
    protected final Map<Object, ShapeData> shapeDataCache = new HashMap<>();
    /**
     * If an image source was specified, this is the WWTexture form.
     */
    protected WWTexture explicitTexture;
    /**
     * This shape's texture coordinates.
     */
    protected float[] explicitTextureCoords;

    /**
     * Constructs a new surface polygon with the default attributes and no locations.
     */
    public SurfacePolygon() {
    }

    /**
     * Creates a shallow copy of the specified source shape.
     *
     * @param source the shape to copy.
     */
    public SurfacePolygon(SurfacePolygon source) {
        super(source);

        this.boundaries.addAll(source.boundaries);
    }

    /**
     * Constructs a new surface polygon with the specified normal (as opposed to highlight) attributes and no locations.
     * Modifying the attribute reference after calling this constructor causes this shape's appearance to change
     * accordingly.
     *
     * @param normalAttrs the normal attributes. May be null, in which case default attributes are used.
     */
    public SurfacePolygon(ShapeAttributes normalAttrs) {
        super(normalAttrs);
    }

    /**
     * Constructs a new surface polygon with the default attributes and the specified iterable of locations.
     * <p>
     * Note: If fewer than three locations is specified, no polygon is drawn.
     *
     * @param iterable the polygon locations.
     * @throws IllegalArgumentException if the locations iterable is null.
     */
    public SurfacePolygon(Iterable<? extends LatLon> iterable) {

        this.setOuterBoundary(iterable);
    }

    /**
     * Constructs a new surface polygon with the specified normal (as opposed to highlight) attributes and the specified
     * iterable of locations. Modifying the attribute reference after calling this constructor causes this shape's
     * appearance to change accordingly.
     * <p>
     * Note: If fewer than three locations is specified, no polygon is drawn.
     *
     * @param normalAttrs the normal attributes. May be null, in which case default attributes are used.
     * @param iterable    the polygon locations.
     * @throws IllegalArgumentException if the locations iterable is null.
     */
    public SurfacePolygon(ShapeAttributes normalAttrs, Iterable<? extends LatLon> iterable) {
        super(normalAttrs);

        this.setOuterBoundary(iterable);
    }

    protected static void closeContour(List<Vertex> contour) {
        final Vertex x = contour.get(0);
        if (!x.equals(contour.get(contour.size() - 1))) {
            contour.add(x);
        }
    }

    protected static double[] uvWeightedAverage(List<Vertex> contour, Vertex vertex) {
        double[] weight = new double[contour.size()];
        double sumOfWeights = 0;
        for (int i = 0; i < contour.size(); i++) {
            double distance = LatLon.greatCircleDistance(contour.get(i), vertex).degrees;
            weight[i] = 1 / distance;
            sumOfWeights += weight[i];
        }

        double u = 0;
        double v = 0;
        for (int i = 0; i < contour.size(); i++) {
            double factor = weight[i] / sumOfWeights;
            u += contour.get(i).u * factor;
            v += contour.get(i).v * factor;
        }

        return new double[] {u, v};
    }

    protected static List<List<Vertex>> clipWithDateline(Iterable<Vertex> contour) {
        List<Vertex> result = new ArrayList<>();
        Vertex prev = null;
        Angle offset = null;
        boolean applyOffset = false;

        for (Vertex cur : contour) {
            if (prev != null && LatLon.locationsCrossDateline(prev, cur)) {
                if (offset == null)
                    offset = (prev.lon < 0 ? Angle.NEG360 : Angle.POS360);
                applyOffset = !applyOffset;
            }

            result.add(applyOffset ? new Vertex(cur.getLat(), cur.getLon().add(offset), cur.u, cur.v) : cur);

            prev = cur;
        }

        List<Vertex> mirror = new ArrayList<>(result.size());
        for (Vertex cur : result) {
            mirror.add(new Vertex(cur.getLat(), cur.getLon().sub(offset), cur.u, cur.v));
        }

        return Arrays.asList(result, mirror);
    }

    public Iterable<? extends LatLon> getLocations(Globe globe) {
        return this.getOuterBoundary();
    }

    public Iterable<? extends LatLon> getLocations() {
        return this.getOuterBoundary();
    }

    public void setLocations(Iterable<? extends LatLon> iterable) {

        this.setOuterBoundary(iterable);
    }

    @Override
    public Sector getSector() {
        return Sector.boundingSector(this.getOuterBoundary());
    }

    public List<Iterable<? extends LatLon>> getBoundaries() {
        return this.boundaries;
    }

    public Iterable<? extends LatLon> getOuterBoundary() {
        return this.boundaries.isEmpty() ? null : this.boundaries.get(0);
    }

    public void setOuterBoundary(Iterable<? extends LatLon> iterable) {

        if (!this.boundaries.isEmpty())
            this.boundaries.set(0, iterable);
        else
            this.boundaries.add(iterable);

        this.onShapeChanged();
    }

    public void addInnerBoundary(Iterable<? extends LatLon> iterable) {

        this.boundaries.add(iterable);
        this.onShapeChanged();
    }

    /**
     * Returns the texture coordinates for this polygon.
     *
     * @return the texture coordinates, or null if no texture coordinates have been specified.
     */
    public float[] getTextureCoords() {
        return this.explicitTextureCoords;
    }

    /**
     * Specifies the texture to apply to this polygon.
     *
     * @param imageSource   the texture image source. May be a {@link String} identifying a file path or URL, a {@link
     *                      File}, or a {@link java.net.URL}.
     * @param texCoords     the (s, t) texture coordinates aligning the image to the polygon. There must be one texture
     *                      coordinate pair, (s, t), for each polygon location in the polygon's outer boundary.
     * @param texCoordCount the number of texture coordinates, (s, v) pairs, specified.
     * @throws IllegalArgumentException if the image source is not null and either the texture coordinates are null or
     *                                  inconsistent with the specified texture-coordinate count, or there are fewer
     *                                  than three texture coordinate pairs.
     */
    public void setTextureImageSource(Object imageSource, float[] texCoords, int texCoordCount) {
        if (imageSource == null) {
            this.explicitTexture = null;
            this.explicitTextureCoords = null;
            this.onShapeChanged();
            return;
        }

        if (texCoordCount < 3 || texCoords.length < 2 * texCoordCount) {
            String message = Logging.getMessage("generic.InsufficientPositions");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.explicitTexture = new BasicWWTexture(imageSource, true);
        this.explicitTextureCoords = texCoords;
        this.onShapeChanged();
    }

    public Position getReferencePosition() {
        final Iterable<? extends LatLon> outer = this.getOuterBoundary();
        if (outer == null)
            return null;

        Iterator<? extends LatLon> iterator = outer.iterator();
        if (!iterator.hasNext())
            return null;

        return new Position(iterator.next(), 0);
    }

    protected void clearCaches() {
        super.clearCaches();
        this.shapeDataCache.clear();
    }

    protected void doDrawGeographic(DrawContext dc, SurfaceTileDrawContext sdc) {
        if (this.boundaries.isEmpty())
            return;

        Object key = this.createGeometryKey(dc, sdc);
        ShapeData shapeData = this.shapeDataCache.get(key);

        if (shapeData == null) {
            shapeData = this.tessellateContours(this.assembleContours(
                new Angle(1.0 / this.computeEdgeIntervalsPerDegree(sdc))));

            this.shapeDataCache.put(key, shapeData);
        }

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        gl.glVertexPointer(2, GL.GL_FLOAT, shapeData.vertexStride, shapeData.vertices.position(0));

        if (shapeData.hasTexCoords) {
            gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
            gl.glTexCoordPointer(2, GL.GL_FLOAT, shapeData.vertexStride, shapeData.vertices.position(2));
        }

        ShapeAttributes attrs = this.getActiveAttributes();
        if (attrs.isDrawInterior()) {
            this.applyInteriorState(dc, sdc, attrs, this.getInteriorTexture(), this.getReferencePosition());
            IntBuffer indices = shapeData.interiorIndices;
            gl.glDrawElements(GL.GL_TRIANGLES, indices.remaining(), GL.GL_UNSIGNED_INT, indices);
        }

        if (attrs.isDrawOutline()) {
            AbstractSurfaceShape.applyOutlineState(dc, attrs);
            IntBuffer indices = shapeData.outlineIndices;
            gl.glDrawElements(GL.GL_LINES, indices.remaining(), GL.GL_UNSIGNED_INT, indices);
        }

        if (shapeData.hasTexCoords) {
            gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        }
    }

    protected void applyInteriorState(DrawContext dc, SurfaceTileDrawContext sdc, ShapeAttributes attributes,
        WWTexture texture, LatLon refLocation) {
        if (this.explicitTexture != null && !dc.isPickingMode()) {
            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            OGLUtil.applyBlending(gl, true);
            OGLUtil.applyColor(gl, attributes.getInteriorMaterial().getDiffuse(), attributes.getInteriorOpacity(),
                true);

            if (this.explicitTexture.bind(dc)) {
                this.explicitTexture.applyInternalTransform(dc);
                gl.glEnable(GL.GL_TEXTURE_2D);
                gl.glDisable(GL2.GL_TEXTURE_GEN_S);
                gl.glDisable(GL2.GL_TEXTURE_GEN_T);
            }
        } else {
            super.applyInteriorState(dc, sdc, attributes, this.getInteriorTexture(), this.getReferencePosition());
        }
    }

    protected List<List<Vertex>> assembleContours(Angle maxEdgeLength) {
        final int n = this.boundaries.size();
        List<List<Vertex>> result = new ArrayList<>(n);

        for (int b = 0; b < n; b++) {
            Iterable<? extends LatLon> locations = this.boundaries.get(b);
            float[] texCoords = (b == 0) ? this.explicitTextureCoords : null;
            int c = 0;

            // Merge the boundary locations with their respective texture coordinates, if any.
            List<Vertex> contour = new ArrayList<>();
            for (LatLon location : locations) {
                Vertex vertex = new Vertex(location);
                contour.add(vertex);

                if (texCoords != null && texCoords.length > c) {
                    vertex.u = texCoords[c++];
                    vertex.v = texCoords[c++];
                }
            }

            // Interpolate the contour vertices according to this polygon's path type and number of edge intervals.
            SurfacePolygon.closeContour(contour);
            this.subdivideContour(contour, maxEdgeLength);

            // Modify the contour vertices to compensate for the spherical nature of geographic coordinates.
            String pole = LatLon.locationsContainPole(contour);
            if (pole != null) {
                result.add(clipWithPole(contour, pole, maxEdgeLength));
            } else if (LatLon.locationsCrossDateLine(contour)) {
                result.addAll(SurfacePolygon.clipWithDateline(contour));
            } else {
                result.add(contour);
            }
        }

        return result;
    }

    protected void subdivideContour(List<Vertex> contour, Angle maxEdgeLength) {
        List<Vertex> original = new ArrayList<>(contour.size());
        original.addAll(contour);
        contour.clear();

        final int n = original.size() - 1;
        for (int i = 0; i < n; i++) {
            Vertex begin = original.get(i);
            Vertex end = original.get(i + 1);
            contour.add(begin);
            this.subdivideEdge(begin, end, maxEdgeLength, contour);
        }

        contour.add(original.get(n));
    }

    protected void subdivideEdge(Vertex begin, Vertex end, Angle maxEdgeLength, List<Vertex> result) {
        Vertex center = new Vertex(LatLon.interpolate(this.pathType, 0.5, begin, end));
        center.u = 0.5 * (begin.u + end.u);
        center.v = 0.5 * (begin.v + end.v);
        center.edgeFlag = begin.edgeFlag || end.edgeFlag;

        if (LatLon.linearDistance(begin, end).compareTo(maxEdgeLength) > 0) {
            this.subdivideEdge(begin, center, maxEdgeLength, result);
            result.add(center);
            this.subdivideEdge(center, end, maxEdgeLength, result);
        } else {
            result.add(center);
        }
    }

    protected List<Vertex> clipWithPole(List<Vertex> contour, String pole, Angle maxEdgeLength) {
        List<Vertex> newVertices = new ArrayList<>();

        Angle poleLat = Keys.NORTH.equals(pole) ? Angle.POS90 : Angle.NEG90;

        Vertex vertex = null;
        for (Vertex nextVertex : contour) {
            if (vertex != null) {
                newVertices.add(vertex);
                if (LatLon.locationsCrossDateline(vertex, nextVertex)) {
                    // Determine where the segment crosses the dateline.
                    LatLon separation = LatLon.intersectionWithMeridian(vertex, nextVertex, Angle.POS180);
                    double sign = Math.signum(vertex.lon);

                    Angle lat = separation.getLat();
                    Angle thisSideLon = Angle.POS180.multiply(sign);
                    Angle otherSideLon = thisSideLon.multiply(-1);

                    // Add locations that run from the intersection to the pole, then back to the intersection. Note
                    // that the longitude changes sign when the path returns from the pole.
                    //         . Pole
                    //      2 ^ | 3
                    //        | |
                    //      1 | v 4
                    // --->---- ------>
                    Vertex in = new Vertex(lat, thisSideLon, 0, 0);
                    Vertex inPole = new Vertex(poleLat, thisSideLon, 0, 0);
                    Vertex centerPole = new Vertex(poleLat, Angle.ZERO, 0, 0);
                    Vertex outPole = new Vertex(poleLat, otherSideLon, 0, 0);
                    Vertex out = new Vertex(lat, otherSideLon, 0, 0);
                    in.edgeFlag = inPole.edgeFlag = centerPole.edgeFlag = outPole.edgeFlag = out.edgeFlag = false;

                    double vertexDistance = LatLon.linearDistance(vertex, in).degrees;
                    double nextVertexDistance = LatLon.linearDistance(nextVertex, out).degrees;
                    double a = vertexDistance / (vertexDistance + nextVertexDistance);
                    in.u = out.u = WWMath.mix(a, vertex.u, nextVertex.u);
                    in.v = out.v = WWMath.mix(a, vertex.v, nextVertex.v);

                    double[] uv = SurfacePolygon.uvWeightedAverage(contour, centerPole);
                    inPole.u = outPole.u = centerPole.u = uv[0];
                    inPole.v = outPole.v = centerPole.v = uv[1];

                    newVertices.add(in);
                    newVertices.add(inPole);
                    this.subdivideEdge(inPole, centerPole, maxEdgeLength, newVertices);
                    newVertices.add(centerPole);
                    this.subdivideEdge(centerPole, outPole, maxEdgeLength, newVertices);
                    newVertices.add(outPole);
                    newVertices.add(out);
                }
            }
            vertex = nextVertex;
        }
        newVertices.add(vertex);

        return newVertices;
    }

    protected ShapeData tessellateContours(Iterable<List<Vertex>> contours) {
        Collection<Vertex> polygonData = new ArrayList<>();
        double[] coords = {0, 0, 0};

        if (SurfacePolygon.tess == null) {
            SurfacePolygon.tess = GLU.gluNewTess();
            SurfacePolygon.tessCallback = new GLUTessellatorSupport.CollectPrimitivesCallback();
            SurfacePolygon.tessCallback.attach(SurfacePolygon.tess);
            GLU.gluTessCallback(SurfacePolygon.tess, GLU.GLU_TESS_COMBINE_DATA, new GLUtessellatorCallbackAdapter() {
                @Override
                public void combineData(double[] coords, Object[] vertexData, float[] weight, Object[] outData,
                    Object polygonData) {
                    List<Vertex> vertexList = (List<Vertex>) polygonData;
                    Vertex vertex = new Vertex(LatLon.fromDegrees(coords[1], coords[0]));
                    vertex.edgeFlag = false; // set to true if any of the combined vertices have the edge flag

                    for (int w = 0; w < 4; w++) {
                        if (weight[w] > 0) {
                            int index = ((GLUTessellatorSupport.VertexData) vertexData[w]).index;
                            Vertex tmp = vertexList.get(index);
                            vertex.u += weight[w] * tmp.u;
                            vertex.v += weight[w] * tmp.v;
                            vertex.edgeFlag |= tmp.edgeFlag;
                        }
                    }

                    int index = ((Collection) polygonData).size();
                    vertexList.add(vertex);

                    outData[0] = new GLUTessellatorSupport.VertexData(index, vertex.edgeFlag);
                }
            });
        }

        try {
            SurfacePolygon.tessCallback.reset();
            GLU.gluTessNormal(SurfacePolygon.tess, 0, 0, 1);
            GLU.gluTessBeginPolygon(SurfacePolygon.tess, polygonData);

            for (List<Vertex> contour : contours) {
                GLU.gluTessBeginContour(SurfacePolygon.tess);

                for (Vertex vertex : contour) {
                    coords[0] = vertex.lon;
                    coords[1] = vertex.lat;
                    int index = polygonData.size();
                    polygonData.add(vertex);
                    Object vertexData = new GLUTessellatorSupport.VertexData(index, vertex.edgeFlag);
                    GLU.gluTessVertex(SurfacePolygon.tess, coords, 0, vertexData);
                }

                GLU.gluTessEndContour(SurfacePolygon.tess);
            }

            GLU.gluTessEndPolygon(SurfacePolygon.tess);
        }
        catch (RuntimeException e) {
            String msg = Logging.getMessage("generic.ExceptionWhileTessellating", e.getMessage());
            Logging.logger().log(Level.SEVERE, msg, e);
            return null;
        }

        if (SurfacePolygon.tessCallback.getError() != 0) {
            String msg = Logging.getMessage("generic.ExceptionWhileTessellating",
                GLUTessellatorSupport.convertGLUTessErrorToString(SurfacePolygon.tessCallback.getError()));
            Logging.logger().log(Level.SEVERE, msg);
            return null;
        }

        ShapeData shapeData = new ShapeData();
        shapeData.hasTexCoords = this.explicitTextureCoords != null;
        shapeData.vertexStride = shapeData.hasTexCoords ? 16 : 0;
        shapeData.vertices = Buffers.newDirectFloatBuffer(polygonData.size() * (shapeData.hasTexCoords ? 4 : 2));
        final Position refPos = this.getReferencePosition();
        double lonOffset = refPos.lon;
        double latOffset = refPos.lat;
        for (Vertex vertex : polygonData) {
            shapeData.vertices.put((float) (vertex.lon - lonOffset));
            shapeData.vertices.put((float) (vertex.lat - latOffset));

            if (shapeData.hasTexCoords) {
                shapeData.vertices.put((float) vertex.u);
                shapeData.vertices.put((float) vertex.v);
            }
        }
        shapeData.vertices.rewind();

        IntBuffer tmp = SurfacePolygon.tessCallback.getTriangleIndices();
        shapeData.interiorIndices = Buffers.newDirectIntBuffer(tmp.remaining());
        shapeData.interiorIndices.put(tmp);
        shapeData.interiorIndices.rewind();

        tmp = SurfacePolygon.tessCallback.getLineIndices();
        shapeData.outlineIndices = Buffers.newDirectIntBuffer(tmp.remaining());
        shapeData.outlineIndices.put(tmp);
        shapeData.outlineIndices.rewind();

        return shapeData;
    }

    protected List<List<LatLon>> createGeometry(Globe globe, double edgeIntervalsPerDegree) {
        if (this.boundaries.isEmpty())
            return null;

        List<List<LatLon>> geom = new ArrayList<>();

        for (Iterable<? extends LatLon> boundary : this.boundaries) {
            List<LatLon> drawLocations = new ArrayList<>();

            this.generateIntermediateLocations(boundary, edgeIntervalsPerDegree, true, drawLocations);

            // Ensure all contours have counter-clockwise winding order. The GLU tessellator we'll use to tessellate
            // these contours is configured to recognize interior holes when all contours have counter clockwise winding
            // order.
            //noinspection StringEquality
            if (WWMath.computeWindingOrderOfLocations(drawLocations) != Keys.COUNTER_CLOCKWISE)
                Collections.reverse(drawLocations);

            geom.add(drawLocations);
        }

        return geom.isEmpty() || geom.get(0).size() < 3 ? null : geom;
    }

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition) {
        if (this.boundaries.isEmpty())
            return;

        final int b = this.boundaries.size();
        for (int i = 0; i < b; i++) {

            final Iterable<? extends LatLon> bi = this.boundaries.get(i);
            Collection<LatLon> newLocations = new ArrayList<>(sizeEstimate(bi));
            for (LatLon ll : bi) {
                Angle heading = LatLon.greatCircleAzimuth(oldReferencePosition, ll);
                Angle pathLength = LatLon.greatCircleDistance(oldReferencePosition, ll);
                newLocations.add(LatLon.greatCircleEndPosition(newReferencePosition, heading, pathLength));
            }

            this.boundaries.set(i, newLocations);
        }

        // We've changed the polygon's list of boundaries; flag the shape as changed.
        this.onShapeChanged();
    }

    protected void doMoveTo(Globe globe, Position oldReferencePosition, Position newReferencePosition) {
        if (this.boundaries.isEmpty())
            return;

        final int b = this.boundaries.size();
        for (int i = 0; i < b; i++) {
            this.boundaries.set(i, LatLon.computeShiftedLocations(globe, oldReferencePosition,
                newReferencePosition, this.boundaries.get(i)));
        }

        // We've changed the polygon's list of boundaries; flag the shape as changed.
        this.onShapeChanged();
    }

    /**
     * Overridden to clear the polygon's locations iterable upon an unsuccessful tessellation attempt. This ensures the
     * polygon won't attempt to re-tessellate itself each frame.
     *
     * @param dc the current DrawContext.
     */
    @Override
    protected void handleUnsuccessfulInteriorTessellation(DrawContext dc) {
        super.handleUnsuccessfulInteriorTessellation(dc);

        // If tessellating the polygon's interior was unsuccessful, we modify the polygon's to avoid any additional
        // tessellation attempts, and free any resources that the polygon won't use. This is accomplished by clearing
        // the polygon's boundary list.
        this.boundaries.clear();
        this.onShapeChanged();
    }

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.doGetRestorableState(rs, context);

        if (!this.boundaries.isEmpty()) {
            RestorableSupport.StateObject so = rs.addStateObject(context, "boundaries");
            for (Iterable<? extends LatLon> boundary : this.boundaries) {
                rs.addStateValueAsLatLonList(so, "boundary", boundary);
            }
        }
    }

    //**************************************************************//
    //********************  Interior Tessellation  *****************//
    //**************************************************************//

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.doRestoreState(rs, context);

        RestorableSupport.StateObject so = rs.getStateObject(context, "boundaries");
        if (so != null) {
            this.boundaries.clear();

            RestorableSupport.StateObject[] sos = rs.getAllStateObjects(so, "boundary");
            if (sos != null) {
                for (RestorableSupport.StateObject boundary : sos) {
                    if (boundary == null)
                        continue;

                    Iterable<LatLon> locations = rs.getStateObjectAsLatLonList(boundary);
                    if (locations != null)
                        this.boundaries.add(locations);
                }
            }

            // We've changed the polygon's list of boundaries; flag the shape as changed.
            this.onShapeChanged();
        }
    }

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context) {
        super.legacyRestoreState(rs, context);

        Iterable<LatLon> locations = rs.getStateValueAsLatLonList(context, "locationList");

        if (locations == null)
            locations = rs.getStateValueAsLatLonList(context, "locations");

        if (locations != null)
            this.setOuterBoundary(locations);
    }

    /**
     * Export the polygon to KML as a {@code <Placemark>} element. The {@code output} object will receive the data. This
     * object must be one of: java.io.Writer java.io.OutputStream javax.xml.stream.XMLStreamWriter
     *
     * @param output Object to receive the generated KML.
     * @throws XMLStreamException If an exception occurs while writing the KML
     * @throws IOException        if an exception occurs while exporting the data.
     * @see #export(String, Object)
     */
    protected void exportAsKML(Object output) throws IOException, XMLStreamException {
        XMLStreamWriter xmlWriter = null;
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        boolean closeWriterWhenFinished = true;

        if (output instanceof XMLStreamWriter) {
            xmlWriter = (XMLStreamWriter) output;
            closeWriterWhenFinished = false;
        } else if (output instanceof Writer) {
            xmlWriter = factory.createXMLStreamWriter((Writer) output);
        } else if (output instanceof OutputStream) {
            xmlWriter = factory.createXMLStreamWriter((OutputStream) output);
        }

        if (xmlWriter == null) {
            String message = Logging.getMessage("Export.UnsupportedOutputObject");
            Logging.logger().warning(message);
            throw new IllegalArgumentException(message);
        }

        xmlWriter.writeStartElement("Placemark");

        String property = getStringValue(Keys.DISPLAY_NAME);
        if (property != null) {
            xmlWriter.writeStartElement("name");
            xmlWriter.writeCharacters(property);
            xmlWriter.writeEndElement();
        }

        xmlWriter.writeStartElement("visibility");
        xmlWriter.writeCharacters(KMLExportUtil.kmlBoolean(this.isVisible()));
        xmlWriter.writeEndElement();

        String shortDescription = (String) get(Keys.SHORT_DESCRIPTION);
        if (shortDescription != null) {
            xmlWriter.writeStartElement("Snippet");
            xmlWriter.writeCharacters(shortDescription);
            xmlWriter.writeEndElement();
        }

        String description = (String) get(Keys.BALLOON_TEXT);
        if (description != null) {
            xmlWriter.writeStartElement("description");
            xmlWriter.writeCharacters(description);
            xmlWriter.writeEndElement();
        }

        // KML does not allow separate attributes for cap and side, so just use the side attributes.
        final ShapeAttributes normalAttributes = getAttributes();
        final ShapeAttributes highlightAttributes = getHighlightAttributes();

        // Write style map
        if (normalAttributes != null || highlightAttributes != null) {
            xmlWriter.writeStartElement("StyleMap");
            KMLExportUtil.exportAttributesAsKML(xmlWriter, KMLConstants.NORMAL, normalAttributes);
            KMLExportUtil.exportAttributesAsKML(xmlWriter, KMLConstants.HIGHLIGHT, highlightAttributes);
            xmlWriter.writeEndElement(); // StyleMap
        }

        // Write geometry
        xmlWriter.writeStartElement("Polygon");

        xmlWriter.writeStartElement("extrude");
        xmlWriter.writeCharacters("0");
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("altitudeMode");
        xmlWriter.writeCharacters("clampToGround");
        xmlWriter.writeEndElement();

        // Outer boundary
        Iterable<? extends LatLon> outerBoundary = this.getOuterBoundary();
        if (outerBoundary != null) {
            xmlWriter.writeStartElement("outerBoundaryIs");
            KMLExportUtil.exportBoundaryAsLinearRing(xmlWriter, outerBoundary, null);
            xmlWriter.writeEndElement(); // outerBoundaryIs
        }

        // Inner boundaries
        Iterator<Iterable<? extends LatLon>> boundaryIterator = boundaries.iterator();
        if (boundaryIterator.hasNext())
            boundaryIterator.next(); // Skip outer boundary, we already dealt with it above

        while (boundaryIterator.hasNext()) {
            Iterable<? extends LatLon> boundary = boundaryIterator.next();

            xmlWriter.writeStartElement("innerBoundaryIs");
            KMLExportUtil.exportBoundaryAsLinearRing(xmlWriter, boundary, null);
            xmlWriter.writeEndElement(); // innerBoundaryIs
        }

        xmlWriter.writeEndElement(); // Polygon
        xmlWriter.writeEndElement(); // Placemark

        xmlWriter.flush();
        if (closeWriterWhenFinished)
            xmlWriter.close();
    }

    protected static class ShapeData {
        public int vertexStride;
        public boolean hasTexCoords;
        public FloatBuffer vertices;
        public IntBuffer interiorIndices;
        public IntBuffer outlineIndices;
    }

    protected static class Vertex extends LatLon {
        public double u;
        public double v;
        public boolean edgeFlag = true;

        public Vertex(LatLon location) {
            super(location);
        }

        public Vertex(Angle latitude, Angle longitude, double u, double v) {
            super(latitude, longitude);
            this.u = u;
            this.v = v;
        }
    }
}