/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.sky;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Renders a star background based on a subset of ESA Hipparcos catalog.
 *
 * @author Patrick Murris
 * @version $Id: StarsLayer.java 2176 2014-07-25 16:35:25Z dcollins $
 */
public class StarsLayer extends RenderableLayer {
    /**
     * The default name of the stars file.s
     */
    protected static final String DEFAULT_STARS_FILE = "config/Hipparcos_Stars_Mag6x5044.dat";
    protected static final double DEFAULT_MIN_ACTIVE_ALTITUDE = 100.0e3;
    protected final Object vboCacheKey = new Object();
    /**
     * The stars file name.
     */
    protected String starsFileName =
        Configuration.getStringValue("gov.nasa.worldwind.StarsLayer.StarsFileName", StarsLayer.DEFAULT_STARS_FILE);
    /**
     * The float buffer holding the Cartesian star coordinates.
     */
    protected FloatBuffer starsBuffer;
    protected int numStars;
    protected boolean rebuild;            // True if need to rebuild GL list
    /**
     * The radius of the spherical shell containing the stars.
     */
    protected Double radius; // radius is either set explicitly or taken from the star file
    /**
     * The star sphere longitudinal rotation.
     */
    protected Angle longitudeOffset = Angle.ZERO;
    /**
     * The star sphere latitudinal rotation.
     */
    protected Angle latitudeOffset = Angle.ZERO;

    /**
     * Constructs a stars layer using the default stars file, which may be specified in {@link Configuration}.
     */
    public StarsLayer() {
        this.initialize(null, null);
    }

    /**
     * Constructs a stars layer using a specified stars file.
     *
     * @param starsFileName the full path the star file.
     */
    public StarsLayer(String starsFileName) {
        this.initialize(starsFileName, null);
    }

    /**
     * Constructs a stars layer using a specified stars file and star-field radius.
     *
     * @param starsFileName the full path the star file.
     * @param radius        the radius of the stars sphere. May be null, in which case the radius in the stars file is
     *                      used.
     */
    public StarsLayer(String starsFileName, Double radius) {
        if (WWUtil.isEmpty(starsFileName)) {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.initialize(starsFileName, radius);
    }

    /**
     * Called by constructors to save the stars file name, the stars field radius and the layer's minimum active
     * altitude.
     *
     * @param starsFileName the full path the star file.
     * @param radius        the radius of the stars sphere. May be null, in which case the radius in the stars file is
     *                      used.
     */
    protected void initialize(String starsFileName, Double radius) {
        if (starsFileName != null)
            this.setStarsFileName(starsFileName);

        if (radius != null)
            this.radius = radius;

        this.setPickEnabled(false);

        // Turn the layer off to eliminate its overhead when the user zooms in.
        this.setMinActiveAltitude(StarsLayer.DEFAULT_MIN_ACTIVE_ALTITUDE);
    }

    /**
     * Indicates the path and filename of the stars file.
     *
     * @return name of stars catalog file.
     */
    public String getStarsFileName() {
        return this.starsFileName;
    }

    /**
     * Specifies the path and filename of the stars file.
     *
     * @param fileName the path and filename.
     * @throws IllegalArgumentException if the file name is null or empty.
     */
    public void setStarsFileName(String fileName) {
        if (WWUtil.isEmpty(fileName)) {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.starsFileName = fileName;
        this.rebuild = true;
    }

    /**
     * Returns the latitude offset (tilt) for the star sphere.
     *
     * @return the latitude offset.
     */
    public Angle getLatitudeOffset() {
        return this.latitudeOffset;
    }

    /**
     * Sets the latitude offset (tilt) of the star sphere.
     *
     * @param offset the latitude offset.
     */
    public void setLatitudeOffset(Angle offset) {
        if (offset == null) {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.latitudeOffset = offset;
    }

    /**
     * Returns the longitude offset of the star sphere.
     *
     * @return the longitude offset.
     */
    public Angle getLongitudeOffset() {
        return this.longitudeOffset;
    }

    /**
     * Sets the longitude offset of the star sphere.
     *
     * @param offset the longitude offset.
     * @throws IllegalArgumentException if the angle is null.s
     */
    public void setLongitudeOffset(Angle offset) {
        if (offset == null) {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.longitudeOffset = offset;
    }

    @Override
    public void doRender(DrawContext dc) {
        if (dc.is2DGlobe())
            return; // Layer doesn't make sense in 2D

        // Load or reload stars if not previously loaded
        if (this.starsBuffer == null || this.rebuild) {
            this.loadStars();
            this.rebuild = false;
        }

        // Still no stars to render ?
        if (this.starsBuffer == null)
            return;

        // Exit if the viewport is not visible, in which case rendering results in exceptions.
        View view = dc.getView();
        if (view.getViewport().getWidth() == 0 || view.getViewport().getHeight() == 0)
            return;

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
        OGLStackHandler ogsh = new OGLStackHandler();
        double[] matrixArray = new double[16];

        try {
            gl.glDisable(GL.GL_DEPTH_TEST);

            // Override the default projection matrix in order to extend the far clip plane to include the stars.
            Matrix projection = Matrix.fromPerspective(view.getFieldOfView(), view.getViewport().width,
                view.getViewport().height, 1, this.radius + 1);
            ogsh.pushProjectionIdentity(gl);
            gl.glLoadMatrixd(projection.toArray(matrixArray, 0, false), 0);

            // Override the default modelview matrix in order to force the eye point to the origin, and apply the
            // latitude and longitude rotations for the stars dataset. Forcing the eye point to the origin causes the
            // stars to appear at an infinite distance, regardless of the view's eye point.
            Matrix modelview = view.getModelviewMatrix();
            modelview = modelview.multiply(Matrix.fromTranslation(view.getEyePoint()));
            modelview = modelview.multiply(Matrix.fromAxisAngle(this.longitudeOffset, 0, 1, 0));
            double degrees = -this.latitudeOffset.degrees;
            modelview = modelview.multiply(
                Matrix.fromAxisAngle(new Angle(degrees), 1, 0, 0));
            ogsh.pushModelviewIdentity(gl);
            gl.glLoadMatrixd(modelview.toArray(matrixArray, 0, false), 0);

            // Draw
            ogsh.pushClientAttrib(gl, GL2.GL_CLIENT_VERTEX_ARRAY_BIT);

            if (dc.getGLRuntimeCapabilities().isUseVertexBufferObject()) {
                if (!this.drawWithVBO(dc))
                    this.drawWithVertexArray(dc);
            } else {
                this.drawWithVertexArray(dc);
            }
        }
        finally {
            dc.restoreDefaultDepthTesting();
            ogsh.pop(gl);
        }
    }

    protected void drawWithVertexArray(DrawContext dc) {
        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        gl.glInterleavedArrays(GL2.GL_C3F_V3F, 0, this.starsBuffer);
        gl.glDrawArrays(GL.GL_POINTS, 0, this.numStars);
    }

    protected boolean drawWithVBO(DrawContext dc) {
        int[] vboId = (int[]) dc.gpuCache().get(this.vboCacheKey);
        if (vboId == null) {
            this.fillVbo(dc);
            vboId = (int[]) dc.gpuCache().get(this.vboCacheKey);
            if (vboId == null)
                return false;
        }

        GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboId[0]);
        gl.glInterleavedArrays(GL2.GL_C3F_V3F, 0, 0);
        gl.glDrawArrays(GL.GL_POINTS, 0, this.numStars);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        return true;
    }

    /**
     * Creates and fills this layer's vertex buffer.
     *
     * @param dc the current draw context.
     */
    protected void fillVbo(DrawContext dc) {
        GL gl = dc.getGL();

        //Create a new bufferId
        int[] glBuf = new int[1];
        gl.glGenBuffers(1, glBuf, 0);

        // Load the buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, glBuf[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, this.starsBuffer.limit() * 4, this.starsBuffer, GL.GL_STATIC_DRAW);

        // Add it to the gpu resource cache
        dc.gpuCache().put(this.vboCacheKey, glBuf, GpuResourceCache.VBO_BUFFERS,
            this.starsBuffer.limit() * 4);
    }

    /**
     * Read stars file and load it into a float buffer.
     */
    protected void loadStars() {
        ByteBuffer byteBuffer = null;

        if (WWIO.getSuffix(this.starsFileName).equals("dat")) {
            try {
                //Try loading from a resource
                InputStream starsStream = WWIO.openFileOrResourceStream(this.starsFileName, this.getClass());
                if (starsStream == null) {
                    String message = Logging.getMessage("layers.StarLayer.CannotReadStarFile");
                    Logging.logger().severe(message);
                    return;
                }

                //Read in the binary buffer
                try {
                    byteBuffer = WWIO.readStreamToBuffer(starsStream, true); // Read stars to a direct ByteBuffer.
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                }
                finally {
                    WWIO.closeStream(starsStream, starsFileName);
                }
            }
            catch (IOException e) {
                String message = "IOException while loading stars data from " + this.starsFileName;
                Logging.logger().severe(message);
            }
        } else {
            //Assume it is a tsv text file
            byteBuffer = StarsConvertor.convertTsvToByteBuffer(this.starsFileName);
        }

        if (byteBuffer == null) {
            String message = "IOException while loading stars data from " + this.starsFileName;
            Logging.logger().severe(message);
            return;
        }

        //Grab the radius from the first value in the buffer
        if (this.radius == null)
            this.radius = (double) byteBuffer.getFloat();
        else
            byteBuffer.getFloat(); // skip over it

        //View the rest of the ByteBuffer as a FloatBuffer
        this.starsBuffer = byteBuffer.asFloatBuffer();

        //byteBuffer is Little-Endian. If native order is not Little-Endian, switch to Big-Endian.
        if (byteBuffer.order() != ByteOrder.nativeOrder()) {
            //tmpByteBuffer is allocated as Big-Endian on all systems
            ByteBuffer tmpByteBuffer = ByteBuffer.allocateDirect(byteBuffer.limit());

            //View it as a Float Buffer
            FloatBuffer fbuffer = tmpByteBuffer.asFloatBuffer();

            //Fill it with the floats in starsBuffer
            for (int i = 0; i < fbuffer.limit(); i++) {
                fbuffer.put(this.starsBuffer.get(i));
            }

            fbuffer.flip();

            //Make the starsBuffer the Big-Endian buffer
            this.starsBuffer = fbuffer;
        }

        //Number of stars = limit / 6 floats per star -> (R,G,B,X,Y,Z)
        this.numStars = this.starsBuffer.limit() / 6;
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.Earth.StarsLayer.Name");
    }

    /**
     * Converts a star background based on a subset of ESA Hipparcos catalog to ByteBuffer.
     *
     * @author Patrick Murris
     * @version $Id: StarsConvertor.java 1171 2013-02-11 21:45:02Z dcollins $
     */
    public static class StarsConvertor {
        private static final float DEFAULT_RADIUS = 6356752 * 10;        // Earth radius x 10

        /**
         * Convert star tsv text file to binary dat file
         *
         * @param tsvFileName name of tsv text star file
         */
        public static void convertTsvToDat(String tsvFileName) {
            String datFileName = WWIO.replaceSuffix(tsvFileName, ".dat");

            StarsConvertor.convertTsvToDat(tsvFileName, datFileName, StarsConvertor.DEFAULT_RADIUS);
        }

        /**
         * Convert star tsv text file to binary dat file
         *
         * @param tsvFileName name of tsv text star file
         * @param radius      radius of star sphere
         */
        public static void convertTsvToDat(String tsvFileName, float radius) {
            String datFileName = WWIO.replaceSuffix(tsvFileName, ".dat");

            StarsConvertor.convertTsvToDat(tsvFileName, datFileName, radius);
        }

        /**
         * Convert star tsv text file to binary dat file
         *
         * @param tsvFileName name of tsv text star file
         * @param datFileName name of dat binary star file
         */
        public static void convertTsvToDat(String tsvFileName, String datFileName) {
            StarsConvertor.convertTsvToDat(tsvFileName, datFileName, StarsConvertor.DEFAULT_RADIUS);
        }

        /**
         * Convert star tsv text file to binary dat file
         *
         * @param tsvFileName name of tsv text star file
         * @param datFileName name of dat binary star file
         * @param radius      radius of star sphere
         */
        public static void convertTsvToDat(String tsvFileName, String datFileName, float radius) {
            //Convert the Tsv Star file to a ByteBuffer in little-endian order
            ByteBuffer bbuf = StarsConvertor.convertTsvToByteBuffer(tsvFileName, radius);

            try {
                WWIO.saveBuffer(bbuf, new File(datFileName));
            }
            catch (IOException e) {
                Logging.logger().log(Level.SEVERE,
                    Logging.getMessage("generic.ExceptionAttemptingToWriteTo", datFileName), e);
            }
        }

        /**
         * Converts a Stars tsv file to a ByteBuffer with radius DEFAULT_RADIUS
         *
         * @param starsFileName filename of tsv file
         * @return ByteBuffer with interleaved color and vertex positions as floats in little-endian order
         */
        public static ByteBuffer convertTsvToByteBuffer(String starsFileName) {
            return StarsConvertor.convertTsvToByteBuffer(starsFileName, StarsConvertor.DEFAULT_RADIUS);
        }

        /**
         * Converts a Stars tsv file to a ByteBuffer
         *
         * @param starsFileName filename of tsv file
         * @param radius        radius of the sphere on which to paint stars
         * @return ByteBuffer with interleaved color and vertex positions as floats in little-endian order
         */
        public static ByteBuffer convertTsvToByteBuffer(String starsFileName, float radius) {
            try {
                Collection<Float> tmpBuffer = new ArrayList<>();

                InputStream starsStream = StarsConvertor.class.getResourceAsStream('/' + starsFileName);

                if (starsStream == null) {
                    File starsFile = new File(starsFileName);
                    if (starsFile.exists()) {
                        starsStream = new FileInputStream(starsFile);
                    }
                }

                if (starsStream == null)
                    // TODO: logger error
                    return null;

                BufferedReader starsReader = new BufferedReader(new InputStreamReader(starsStream));

                String line;
                int idxRAhms = 2;        // Catalog field indices
                int idxDEdms = 3;
                int idxVmag = 4;
                int idxBV = 5;
                double longitude;
                double latitude;
                boolean isData = false;

                //Add the radius as the first value
                tmpBuffer.add(radius);

                while ((line = starsReader.readLine()) != null) {
                    if (line.length() < 3)
                        continue;
                    if (line.charAt(0) == '#')
                        continue;
                    if (isData) // Star data here
                    {
                        // Split data in ';' separated values
                        String[] starData = line.trim().split(";");
                        String RAhms, DEdms, Vmag, BV;
                        RAhms = starData[idxRAhms];    // Right Asc in H, min, sec 	"00 01 35.85"
                        DEdms = starData[idxDEdms];    // Declinaison Degre min sec	"-77 03 55.1"
                        Vmag = starData[idxVmag];    // Apparent magnitude	" 4.78"
                        // B-V spectral color " 1.254" (may be missing)
                        BV = idxBV < starData.length ? starData[idxBV] : "";

                        // compute RAhms into longitude
                        double RAh = Double.parseDouble(RAhms.substring(0, 2));
                        double RAm = Double.parseDouble(RAhms.substring(3, 5));
                        double RAs = Double.parseDouble(RAhms.substring(6));
                        longitude = (RAh * 15) + (RAm * 0.25) + (RAs * 0.0041666) - 180;
                        // compute DEdms into latitude
                        String DEsign = DEdms.substring(0, 1);
                        double DEd = Double.parseDouble(DEdms.substring(1, 3));
                        double DEm = Double.parseDouble(DEdms.substring(4, 6));
                        double DEs = Double.parseDouble(DEdms.substring(7));
                        latitude = DEd + (DEm / 60) + (DEs / 3600);
                        if (DEsign.equals("-"))
                            latitude *= -1;
                        // compute aparent magnitude -1.5 - 10 to grayscale 0 - 255
                        double VM = Double.parseDouble(Vmag);
                        double Vdec = 255 - ((VM + 1.5) * 255 / 10);
                        if (Vdec > 255)
                            Vdec = 255;
                        Vdec /= 255;    // scale back to 0.0 - 1.0
                        // convert B-V  -0.5 - 4 for rgb color select
                        double BVdec;
                        try {
                            BVdec = Double.parseDouble(BV);
                        }
                        catch (Exception e) {
                            BVdec = 0;
                        }

                        // Star color
                        Color color = StarsConvertor.BVColor(BVdec);
                        tmpBuffer.add(color.getRed() / 255.0f * (float) Vdec);
                        tmpBuffer.add(color.getGreen() / 255.0f * (float) Vdec);
                        tmpBuffer.add(color.getBlue() / 255.0f * (float) Vdec);

                        // Place vertex for point star
                        Vec4 pos = StarsConvertor.SphericalToCartesian(latitude, longitude, radius);
                        tmpBuffer.add((float) pos.x);
                        tmpBuffer.add((float) pos.y);
                        tmpBuffer.add((float) pos.z);
                    }

                    // Data starting next line
                    if (line.startsWith("---"))
                        isData = true;
                }

                starsReader.close();

                ByteBuffer buf = Buffers.newDirectByteBuffer(tmpBuffer.size() * 4);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                FloatBuffer fBuf = buf.asFloatBuffer();

                for (Float fVal : tmpBuffer) {
                    fBuf.put(fVal);
                }

                buf.rewind();

                return buf;
            }
            catch (IOException e) {
                // TODO: Log proper message
                //String message = WorldWind.retrieveErrMsg("generic.IOExceptionWhileLoadingData");
                String message = "IOException while loading stars data from " + starsFileName;
                Logging.logger().severe(message);
            }
            catch (Exception e) {
                String message = "Error while loading stars data from " + starsFileName;
                Logging.logger().severe(message);
            }

            return null;
        }

        /**
         * Converts position in spherical coordinates (lat/lon/radius) to cartesian (XYZ) coordinates.
         *
         * @param latitude  Latitude in decimal degrees
         * @param longitude Longitude in decimal degrees
         * @param radius    Radius
         * @return the corresponding Point
         */
        private static Vec4 SphericalToCartesian(double latitude, double longitude, float radius) {
            latitude *= Math.PI / 180.0f;
            longitude *= Math.PI / 180.0f;

            double radCosLat = radius * Math.cos(latitude);

            return new Vec4(
                radCosLat * Math.sin(longitude),
                radius * Math.sin(latitude),
                radCosLat * Math.cos(longitude));
        }

        /**
         * Returns the corresponding B-V color
         *
         * @param BV the star B-V decimal value (-.5 .. 4)
         * @return the corresponding Color
         */
        private static Color BVColor(double BV) {
            // TODO: interpolate between values
            if (BV < 0)
                return new Color(0.635f, 0.764f, 0.929f);            // Light blue
            else if (BV < 0.5)
                return new Color(1.0f, 1.0f, 1.0f);                // White
            else if (BV < 1)
                return new Color(1.0f, 0.984f, 0.266f);            // Yellow
            else if (BV < 1.5)
                return new Color(0.964f, 0.725f, 0.0784f);    // Orange
            else
                return new Color(0.921f, 0.376f, 0.0392f);                // Redish
        }

        public static void main(String[] args) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setMultiSelectionEnabled(true);

            int status = fileChooser.showOpenDialog(null);
            if (status != JFileChooser.APPROVE_OPTION)
                return;

            File[] files = fileChooser.getSelectedFiles();
            if (files == null) {
                System.out.println("No files selected");
                return;
            }

            String ans;
            ans = JOptionPane.showInputDialog("Enter star sphere radius?", StarsConvertor.DEFAULT_RADIUS);

            float radius;

            while (true) {
                try {
                    radius = Float.parseFloat(ans);
                    break;
                }
                catch (NumberFormatException e) {
                    String message = Logging.getMessage("generic.NumberFormatException");
                    Logging.logger().warning(message);

                    ans = JOptionPane.showInputDialog(
                        "<html><font color=#ff0000>INVALID VALUE: Please enter a floating point number."
                            + "</font><br>Enter star sphere radius?</html>", StarsConvertor.DEFAULT_RADIUS);
                }
            }

            for (File file : files) {
                StarsConvertor.convertTsvToDat(file.getAbsolutePath(), radius);
            }
        }
    }
}
