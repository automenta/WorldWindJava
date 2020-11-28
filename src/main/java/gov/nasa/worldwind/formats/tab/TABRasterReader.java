/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tab;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.util.List;
import java.util.regex.*;

/**
 * Reader for the MapInfo TAB file format. Documentation on the MapInfo TAB format can be found here:
 * https://en.wikipedia.org/wiki/MapInfo_TAB_format
 *
 * @author dcollins
 * @version $Id: TABRasterReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class TABRasterReader {
    public static final String VERSION = "TABRaster.Version";
    public static final String CHARSET = "TABRaster.Charset";
    public static final String TYPE = "TABRaster.Type";
    public static final String IMAGE_PATH = "TABRaster.ImagePath";
    public static final String LABEL = "TABRaster.Label";

    public static final String RASTER_STYLE_BRIGHTNESS_VALUE = "TABRaster.RasterStyleBrightnessValue";
    public static final String RASTER_STYLE_CONTRAST_VALUE = "TABRaster.RasterStyleContrastValue";
    public static final String RASTER_STYLE_GRAYSCALE_VALUE = "TABRaster.RasterStyleGrayscaleValue";
    public static final String RASTER_STYLE_USE_TRANSPARENT_VALUE = "TABRaster.RasterStyleUseTransparentValue";
    public static final String RASTER_STYLE_TRANSPARENT_INDEX_VALUE = "TABRaster.RasterStyleTransparentIndexValue";
    public static final String RASTER_STYLE_GRID_VALUE = "TABRaster.RasterStyleGridValue";
    public static final String RASTER_STYLE_TRANSPARENT_COLOR_VALUE = "TABRaster.TransparentColorValue";
    public static final String RASTER_STYLE_TRANSLUCENT_ALPHA_VALUE = "TABRaster.TranslucentAlphaValue";

    protected static final String TAG_DEFINITION = "Definition";
    protected static final String TAG_FILE = "File";
    protected static final String TAG_HEADER_TABLE = "!table";
    protected static final String TAG_HEADER_VERSION = "!version";
    protected static final String TAG_HEADER_CHARSET = "!charset";
    protected static final String TAG_TABLE = "Table";
    protected static final String TAG_TYPE = "Type";

    protected static final int RASTER_STYLE_ID_BRIGHTNESS_VALUE = 1;
    protected static final int RASTER_STYLE_ID_CONTRAST_VALUE = 2;
    protected static final int RASTER_STYLE_ID_GRAYSCALE_VALUE = 3;
    protected static final int RASTER_STYLE_ID_USE_TRANSPARENT_VALUE = 4;
    protected static final int RASTER_STYLE_ID_TRANSPARENT_INDEX_VALUE = 5;
    protected static final int RASTER_STYLE_ID_GRID_VALUE = 6;
    protected static final int RASTER_STYLE_ID_TRANSPARENT_COLOR_VALUE = 7;
    protected static final int RASTER_STYLE_ID_TRANSLUCENT_ALPHA_VALUE = 8;

    public TABRasterReader() {
    }

    public static File getTABFileFor(File file) {
        if (file == null) {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        File parent = file.getParentFile();
        if (parent == null)
            return null;

        String tabFilename = WWIO.replaceSuffix(file.getName(), ".tab");

        // The file already has a TAB extension. Rather than returning a self reference, we return null to deonte that
        // a TAB file does not associate with itself.
        if (file.getName().equalsIgnoreCase(tabFilename)) {
            return null;
        }

        // Find the first sibling with the matching filename, and TAB extension.
        for (File child : parent.listFiles()) {
            if (!child.equals(file) && child.getName().equalsIgnoreCase(tabFilename)) {
                return child;
            }
        }

        return null;
    }

    private static String stripQuotes(String s) {
        if (!s.isEmpty() && s.charAt(0) == '\"' || !s.isEmpty() && s.charAt(0) == '\'')
            s = s.substring(1);
        if (!s.isEmpty() && s.charAt(s.length() - 1) == '\"' || !s.isEmpty() && s.charAt(s.length() - 1) == '\'')
            s = s.substring(0, s.length() - 1);
        return s;
    }

    private static void setProperty(String line, String key, AVList values) {
        String[] tokens = line.split(" ", 2);
        if (tokens == null || tokens.length < 2)
            return;

        String value = tokens[1];
        if (value == null || value.trim().isEmpty())
            return;

        values.set(key, value.trim());
    }

    public boolean canRead(File file) {
        if (file == null || !file.exists() || !file.canRead())
            return false;

        try (FileReader fileReader = new FileReader(file)) {
            RasterControlPointList controlPoints = new RasterControlPointList();
            return this.doCanRead(fileReader, controlPoints);
        }
        catch (Exception ignored) {
            return false;
        }
        //noinspection EmptyCatchBlock
    }

    public boolean canRead(String path) {
        if (path == null)
            return false;

        Object streamOrException = WWIO.getFileOrResourceAsStream(path, this.getClass());
        if (streamOrException == null || streamOrException instanceof Exception)
            return false;

        InputStream stream = (InputStream) streamOrException;
        try {
            InputStreamReader streamReader = new InputStreamReader(stream);
            RasterControlPointList controlPoints = new RasterControlPointList();
            return this.doCanRead(streamReader, controlPoints);
        }
        catch (Exception ignored) {
            return false;
        }
        finally {
            WWIO.closeStream(stream, path);
        }
    }

    public RasterControlPointList read(File file) throws IOException {
        if (file == null) {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (!file.exists()) {
            String message = Logging.getMessage("generic.FileNotFound", file);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (!file.canRead()) {
            String message = Logging.getMessage("generic.FileNoReadPermission", file);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
            RasterControlPointList controlPoints = new RasterControlPointList();
            this.doRead(fileReader, file.getParent(), controlPoints);
            return controlPoints;
        }
        finally {
            WWIO.closeStream(fileReader, file.getPath());
        }
    }

    public RasterControlPointList read(String path) throws IOException {
        if (path == null) {
            String message = Logging.getMessage("nullValue.PathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object streamOrException = WWIO.getFileOrResourceAsStream(path, this.getClass());
        if (streamOrException == null || streamOrException instanceof Exception) {
            String message = Logging.getMessage("generic.ExceptionAttemptingToReadFile",
                (streamOrException != null) ? streamOrException : path);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        InputStream stream = (InputStream) streamOrException;
        try {
            InputStreamReader streamReader = new InputStreamReader(stream);

            String workingDirectory = WWIO.getParentFilePath(path);

            RasterControlPointList controlPoints = new RasterControlPointList();
            this.doRead(streamReader, workingDirectory, controlPoints);
            return controlPoints;
        }
        finally {
            WWIO.closeStream(stream, path);
        }
    }

    protected boolean doCanRead(Reader reader, RasterControlPointList controlPoints) {
        if (reader == null) {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (controlPoints == null) {
            String message = Logging.getMessage("nullValue.RasterControlPointListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try {
            BufferedReader br = new BufferedReader(reader);
            this.readHeader(br, controlPoints);

            String s = this.validateHeaderValues(controlPoints);
            return (s == null);
        }
        catch (Exception e) {
            return false;
        }
    }

    protected void doRead(Reader reader, String workingDirectory, RasterControlPointList controlPoints)
        throws IOException {
        if (reader == null) {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (controlPoints == null) {
            String message = Logging.getMessage("nullValue.RasterControlPointListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        BufferedReader br = new BufferedReader(reader);
        this.readHeader(br, controlPoints);
        this.readDefinitionTable(br, workingDirectory, controlPoints);

        String s = this.validateHeaderValues(controlPoints);
        if (s != null) {
            String message = Logging.getMessage("TABReader.MissingHeaderValues", s);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        s = this.validateRasterControlPoints(controlPoints);
        if (s != null) {
            String message = Logging.getMessage("TABReader.MissingRasterData", s);
            Logging.logger().severe(message);
            throw new IOException(message);
        }
    }

    protected void readHeader(BufferedReader reader, AVList controlPoints)
        throws IOException {
        if (reader == null) {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (controlPoints == null) {
            String message = Logging.getMessage("nullValue.RasterControlPointListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String line = this.skipToHeader(reader);
        if (line == null || !line.equalsIgnoreCase(TAG_HEADER_TABLE)) {
            String message = Logging.getMessage("TABReader.InvalidMagicString", line);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        line = this.nextLine(reader);
        if (line != null && line.startsWith(TAG_HEADER_VERSION)) {
            if (controlPoints.get(VERSION) == null)
                setProperty(line, VERSION, controlPoints);
        }

        line = this.nextLine(reader);
        if (line != null && line.startsWith(TAG_HEADER_CHARSET)) {
            if (controlPoints.get(CHARSET) == null)
                setProperty(line, CHARSET, controlPoints);
        }
    }

    protected void readDefinitionTable(BufferedReader reader, String workingDirectory,
        RasterControlPointList controlPoints) throws IOException {
        if (reader == null) {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (controlPoints == null) {
            String message = Logging.getMessage("nullValue.RasterControlPointListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String line = this.skipToDefinition(reader);
        if (line == null || !line.equalsIgnoreCase(TAG_TABLE))
            return;

        line = this.nextLine(reader);
        if (line != null && line.startsWith(TAG_FILE)) {
            if (controlPoints.getStringValue(IMAGE_PATH) == null
                || controlPoints.getStringValue(IMAGE_PATH).isEmpty()) {
                String[] tokens = line.split(" ", 2);
                if (tokens.length >= 2 && tokens[1] != null) {
                    String pathname = stripQuotes(tokens[1].trim());
                    controlPoints.set(IMAGE_PATH, WWIO.appendPathPart(workingDirectory, pathname));
                }
            }
        }

        line = this.nextLine(reader);
        if (line != null && line.startsWith(TAG_TYPE)) {
            if (controlPoints.get(TYPE) == null)
                setProperty(line, TYPE, controlPoints);
        }

        this.readControlPoints(reader, controlPoints);
        this.readCoordSys(reader, controlPoints);
        this.readRasterStyle(reader, controlPoints);
    }

    protected void readControlPoints(BufferedReader reader,
        List<RasterControlPointList.ControlPoint> controlPoints)
        throws IOException {
        if (reader == null) {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (controlPoints == null) {
            String message = Logging.getMessage("nullValue.RasterControlPointListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Pattern pattern = Pattern.compile(
            "[(](.+)[,](.+)[)].+[(](.+)[,](.+)[)][\\s]+.+[\\s]+[\"']?(.+)[\"']?[,]?");

        String line;
        Matcher matcher;
        while ((line = this.nextLine(reader)) != null && (matcher = pattern.matcher(line)).matches()) {
            String swx = matcher.group(1);
            String swy = matcher.group(2);
            String srx = matcher.group(3);
            String sry = matcher.group(4);
            String label = matcher.group(5);

            Double wx = WWUtil.convertStringToDouble(swx);
            Double wy = WWUtil.convertStringToDouble(swy);
            Double rx = WWUtil.convertStringToDouble(srx);
            Double ry = WWUtil.convertStringToDouble(sry);

            if (wx != null && wy != null && rx != null && ry != null) {
                RasterControlPointList.ControlPoint controlPoint =
                    new RasterControlPointList.ControlPoint(wx, wy, rx, ry);
                controlPoint.set(LABEL, label);
                controlPoints.add(controlPoint);
            }
        }
    }

    protected void readCoordSys(BufferedReader reader, RasterControlPointList controlPoints) {
        if (reader == null) {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (controlPoints == null) {
            String message = Logging.getMessage("nullValue.RasterControlPointListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // TODO
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void readRasterStyle(BufferedReader reader, RasterControlPointList controlPoints) {
        if (controlPoints == null) {
            String message = Logging.getMessage("nullValue.RasterControlPointListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // TODO
    }

    protected String skipToHeader(BufferedReader reader) throws IOException {
        return this.nextLine(reader);
    }

    protected String skipToDefinition(BufferedReader reader) throws IOException {
        String line = this.nextLine(reader);

        if (line == null || line.isEmpty())
            return null;

        String[] tokens = line.split(" ", 2);
        if (tokens.length < 2)
            return null;

        return (tokens[1] != null) ? tokens[1].trim() : null;
    }

    protected String nextLine(BufferedReader reader) throws IOException {
        // Read until the next non-whitespace line.

        String line;
        while ((line = reader.readLine()) != null && line.trim().isEmpty()) {
        }

        return (line != null) ? line.trim() : null;
    }

    protected String validateHeaderValues(AVList values) {
        StringBuilder sb = new StringBuilder();

        String s = values.getStringValue(VERSION);
        if (s == null || s.isEmpty()) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(Logging.getMessage("term.version"));
        }

        s = values.getStringValue(CHARSET);
        if (s == null || s.isEmpty()) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(Logging.getMessage("term.charset"));
        }

        if (!sb.isEmpty())
            return sb.toString();

        return null;
    }

    protected String validateRasterControlPoints(RasterControlPointList controlPoints) {
        StringBuilder sb = new StringBuilder();

        if (controlPoints.getStringValue(IMAGE_PATH) == null && controlPoints.getStringValue(IMAGE_PATH).isEmpty()) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(Logging.getMessage("TABReader.MissingOrInvalidFileName",
                controlPoints.getStringValue(IMAGE_PATH)));
        }

        if (controlPoints.size() < 3) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(Logging.getMessage("TABReader.NotEnoughControlPoints", controlPoints.size()));
        }

        if (!sb.isEmpty())
            return sb.toString();

        return null;
    }
}
