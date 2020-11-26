/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.gcps;

////.*;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.util.List;
import java.util.regex.*;

/**
 * @author dcollins
 * @version $Id: GCPSReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GCPSReader {
    private String delimiter = "[\\s]";

    public GCPSReader() {
    }

    public static File getGCPSFileFor(File file) {
        if (file == null) {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        File parent = file.getParentFile();
        if (parent == null)
            return null;

        String tabFilename = WWIO.replaceSuffix(file.getName(), ".gcps");

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

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty())
            return null;

        try {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public String getDelimiter() {
        return this.delimiter;
    }

    public void setDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) {
            String message = Logging.getMessage("nullValue.DelimiterIsNullOrEmpty");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.delimiter = delimiter;
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
            try {
                stream.close();
            }
            catch (IOException e) {
                String message = Logging.getMessage("generic.ExceptionClosingStream", stream);
                Logging.logger().severe(message);
            }
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
            this.doRead(fileReader, controlPoints);
            return controlPoints;
        }
        finally {
            try {
                if (fileReader != null)
                    fileReader.close();
            }
            catch (IOException e) {
                String message = Logging.getMessage("generic.ExceptionClosingStream", file);
                Logging.logger().severe(message);
            }
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
            RasterControlPointList controlPoints = new RasterControlPointList();
            this.doRead(streamReader, controlPoints);
            return controlPoints;
        }
        finally {
            try {
                stream.close();
            }
            catch (IOException e) {
                String message = Logging.getMessage("generic.ExceptionClosingStream", stream);
                Logging.logger().severe(message);
            }
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
            String line = this.nextLine(br);
            Pattern pattern = this.createPattern();

            return pattern.matcher(line).matches();
        }
        catch (Exception e) {
            return false;
        }
    }

    protected void doRead(Reader reader, RasterControlPointList controlPoints) throws IOException {
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
        this.readControlPoints(br, controlPoints);
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

        Pattern pattern = this.createPattern();

        String line;
        Matcher matcher;
        while ((line = this.nextLine(reader)) != null && (matcher = pattern.matcher(line)).matches()) {
            String swx = matcher.group(1);
            String swy = matcher.group(2);
            String srx = matcher.group(3);
            String sry = matcher.group(4);

            Double wx = parseDouble(swx);
            Double wy = parseDouble(swy);
            Double rx = parseDouble(srx);
            Double ry = parseDouble(sry);

            if (wx != null && wy != null && rx != null && ry != null) {
                RasterControlPointList.ControlPoint controlPoint =
                    new RasterControlPointList.ControlPoint(wx, wy, rx, ry);
                controlPoints.add(controlPoint);
            }
        }
    }

    protected Pattern createPattern() {
        String delim = this.getDelimiter();

        StringBuilder sb = new StringBuilder();
        sb.append("(.+)");
        sb.append(delim).append("+");
        sb.append("(.+)");
        sb.append(delim).append("+");
        sb.append("(.+)");
        sb.append(delim).append("+");
        sb.append("(.+)");

        return Pattern.compile(sb.toString());
    }

    protected String nextLine(BufferedReader reader) throws IOException {
        // Read until the next non-whitespace line.

        String line;
        while ((line = reader.readLine()) != null && line.trim().isEmpty()) {
        }

        return (line != null) ? line.trim() : null;
    }
}
