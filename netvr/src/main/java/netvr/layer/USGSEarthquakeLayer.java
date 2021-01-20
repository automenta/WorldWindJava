package netvr.layer;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.formats.geojson.GeoJSONPoint;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;

import java.awt.*;
import java.nio.DoubleBuffer;

public class USGSEarthquakeLayer extends GeoJSONLayer {

    public USGSEarthquakeLayer(String name, Object src) {
        super(name, src, new USGSEarthquakeRenderer());
    }

    private static class USGSEarthquakeRenderer extends GeoJSON.GeoJSONRenderer {

        protected static final long MILLISECONDS_PER_MINUTE = 60000;
        protected static final long MILLISECONDS_PER_HOUR = 60 * MILLISECONDS_PER_MINUTE;
        protected static final long MILLISECONDS_PER_DAY = 24 * MILLISECONDS_PER_HOUR;
        protected static final String USGS_EARTHQUAKE_MAGNITUDE = "mag";
//        protected static final String USGS_EARTHQUAKE_PLACE = "place";
        protected static final String USGS_EARTHQUAKE_TIME = "time";

//        private final Color[] eqColors =
//            {
//                Color.RED,
//                Color.ORANGE,
//                Color.YELLOW,
//                Color.GREEN,
//                Color.BLUE,
//                Color.GRAY,
//                Color.BLACK,
//            };

        private AnnotationAttributes eqAttributes;

        final long updateTime = System.currentTimeMillis();

        @Override
        protected void addRenderableForPoint(GeoJSONPoint geom, RenderableLayer l, KV properties) {
            addEarthquake(geom, l, properties);
        }

        private void addEarthquake(GeoJSONPoint geom, RenderableLayer layer, KV p) {
            if (eqAttributes == null) {
                // Init default attributes for all eq
                eqAttributes = new AnnotationAttributes();
                eqAttributes.setLeader(Keys.SHAPE_NONE);
                eqAttributes.setDrawOffset(new Point(0, -16));
                eqAttributes.setSize(new Dimension(32, 32));
                eqAttributes.setBorderWidth(0);
                eqAttributes.setCornerRadius(0);
                eqAttributes.setBackgroundColor(new Color(0, 0, 0, 0));
            }
            Number eqTime = (Number) p.get(USGS_EARTHQUAKE_TIME);
            int elapsedDays;
            if (eqTime != null) {
                // Compute days elapsed since earthquake event
                elapsedDays = (int) ((this.updateTime - eqTime.longValue()) / MILLISECONDS_PER_DAY);

                // Update latest earthquake event
//                        if (this.latestEq != null) {
//                            Number latestEqTime = (Number) this.latestEq.get(USGS_EARTHQUAKE_TIME);
//                            if (latestEqTime.longValue() < eqTime.longValue())
//                                this.latestEq = eq;
//                        } else {
//                            this.latestEq = eq;
//                        }
            } else
                elapsedDays = 6;

            Number eqMagnitude = (Number) p.get(USGS_EARTHQUAKE_MAGNITUDE);

            final double radMeters = eqMagnitude.doubleValue() * 32_000;
            final Cylinder eq = new Cylinder(geom.getPosition(),
                radMeters / 2,
                radMeters
            );

//                    final SurfaceCircle eq = new SurfaceCircle(
//                        geom.getPosition(),
//                        radMeters);
            final BasicShapeAttributes a = new BasicShapeAttributes();
            eq.setAttributes(a);
            a.setDrawOutline(false);
            float alpha = 1f / (1 + elapsedDays);
            a.setInteriorMaterial(new Material(
                new Color(0.5f + 0.5f * alpha, 0, 0)
            ));
            a.setInteriorOpacity(0.5f * alpha);
            eq.setHighlightAttributes(a);

            layer.add(eq);

//                    EqAnnotation eq = new EqAnnotation(geom.getPosition(), eqAttributes);
//                    eq.setAltitudeMode(WorldWind.CLAMP_TO_GROUND); // GeoJON point's 3rd coordinate indicates depth
//                    eq.setValues(properties);
//
//                    eq.getAttributes().setTextColor(eqColors[Math.min(elapsedDays, eqColors.length - 1)]);
//
//                    eq.getAttributes().setScale(eqMagnitude.doubleValue() / 4);
//
//                    layer.add(eq);
        }

        class EqAnnotation extends GlobeAnnotation {
            // Override annotation drawing for a simple circle
            private DoubleBuffer shapeBuffer;

            public EqAnnotation(Position position, AnnotationAttributes defaults) {
                super("", position, defaults);
            }

            protected void applyScreenTransform(DrawContext dc, int x, int y, int width, int height,
                double scale) {
                double finalScale = scale * this.computeScale(dc);

                GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
                gl.glTranslated(x, y, 0);
                gl.glScaled(finalScale, finalScale, 1);
            }

            protected void doDraw(DrawContext dc, int width, int height, double opacity,
                Position pickPosition) {
                // Draw colored circle around screen point - use annotation's text color
                if (dc.isPickingMode()) {
                    this.bindPickableObject(dc, pickPosition);
                }

                AbstractAnnotation.applyColor(dc, this.getAttributes().getTextColor(), 0.6 * opacity, true);

                // Draw 32x32 shape from its bottom left corner
                int size = 32;
                if (this.shapeBuffer == null)
                    this.shapeBuffer = FrameFactory.createShapeBuffer(Keys.SHAPE_ELLIPSE, size, size, 0, null);
                GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
                gl.glTranslatef(-size / 2.0f, -size / 2.0f, 0);
                FrameFactory.drawBuffer(dc, GL.GL_TRIANGLE_FAN, this.shapeBuffer);
            }
        }
    }
}