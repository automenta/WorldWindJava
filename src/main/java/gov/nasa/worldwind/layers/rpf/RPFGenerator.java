/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.rpf;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.rpf.*;
import gov.nasa.worldwind.formats.wvt.WaveletCodec;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;

/**
 * @author brownrigg
 * @version $Id: RPFGenerator.java 1171 2013-02-11 21:45:02Z dcollins $
 */
class RPFGenerator {
    // Configuration property keys.
    public static final String RPF_FILE_INDEX = "RPFGenerator.RPFFileIndex";
    public static final String WAVELET_IMAGE_THRESHOLD = "RPFGenerator.WaveletImageThreshold";
    public static final String WAVELET_PRELOAD_SIZE = "RPFGenerator.WaveletPreloadSize";
    @SuppressWarnings("FieldCanBeLocal")
    private final RPFFileIndex fileIndex;
    private final FrameFile[] frameFiles;
    private final Sector globalBounds;
    private final AbsentResourceList absentFrames;
    // Wavelet parameters.
    private final int smallImageSize;
    private final int preloadRes;

    public RPFGenerator(KV params) {
        if (params == null) {
            String message = Logging.getMessage("nullValue.AVListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        params = RPFGenerator.initParams(params.copy());

        this.fileIndex = (RPFFileIndex) params.get(RPFGenerator.RPF_FILE_INDEX);
        this.frameFiles = RPFGenerator.loadFrameFiles(this.fileIndex);
        this.globalBounds = RPFGenerator.computeGlobalBounds(this.fileIndex);
        this.absentFrames = new AbsentResourceList(1, 0); // Mark frame files absent after the first failed attempt.

        this.smallImageSize = (Integer) params.get(RPFGenerator.WAVELET_IMAGE_THRESHOLD);
        this.preloadRes = (Integer) params.get(RPFGenerator.WAVELET_PRELOAD_SIZE);
    }

    private static KV initParams(KV params) {
        if (params == null) {
            String message = Logging.getMessage("nullValue.AVListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params.get(RPFGenerator.RPF_FILE_INDEX) == null) {
            String message = "RPFFileIndex is null";
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.get(RPFGenerator.WAVELET_IMAGE_THRESHOLD);
        if (!(o instanceof Integer))
            params.set(RPFGenerator.WAVELET_IMAGE_THRESHOLD, 256);

        o = params.get(RPFGenerator.WAVELET_PRELOAD_SIZE);
        if (!(o instanceof Integer) || !WWMath.isPowerOfTwo((Integer) o))
            params.set(RPFGenerator.WAVELET_PRELOAD_SIZE, 32);

        return params;
    }

    private static FrameFile[] loadFrameFiles(RPFFileIndex fileIndex) {
        Collection<FrameFile> list = new ArrayList<>();

        long frameId = -1;
        RPFFileIndex.Table fileTable = fileIndex.getRPFFileTable();
        if (fileTable != null) {
            for (RPFFileIndex.Record record : fileTable.getRecords()) {
                ++frameId;
                RPFFileIndex.RPFFileRecord rpfRecord = (RPFFileIndex.RPFFileRecord) record;
                long rpfKey = record.getKey();
                long waveletKey = rpfRecord.getWaveletSecondaryKey();
                Sector sector = rpfRecord.getSector();
                if (rpfKey != -1 && waveletKey != -1 && sector != null) {
                    File rpfFile = fileIndex.getRPFFile(rpfKey);
                    File waveletFile = fileIndex.getWaveletFile(waveletKey);
                    list.add(new FrameFile(frameId, rpfFile, waveletFile, sector));
                } else {
                    String message = "Ignoring frame file: " + (rpfKey == -1 ? "?"
                        : fileIndex.getRPFFile(rpfKey).getPath());
                    Logging.logger().fine(message);
                }
            }
        }

        FrameFile[] array = new FrameFile[list.size()];
        list.toArray(array);
        return array;
    }

    //
    // Find the global bounds for this collection of frame files (i.e., the union of their Sectors).
    //
    private static Sector computeGlobalBounds(RPFFileIndex fileIndex) {
        Sector gb = null;
        if (fileIndex != null && fileIndex.getIndexProperties() != null)
            gb = fileIndex.getIndexProperties().getBoundingSector();
        if (gb == null)
            gb = Sector.EMPTY_SECTOR;
        return gb;
    }

    public Sector getGlobalBounds() {
        return this.globalBounds;
    }

    public RPFServiceInstance getServiceInstance() {
        return new RPFServiceInstance();
    }

    private void markFrameFileAbsent(FrameFile frame) {
        this.absentFrames.markResourceAbsent(frame.id);
    }

    private boolean isFrameFileAbsent(FrameFile frame) {
        return this.absentFrames.isResourceAbsent(frame.id);
    }

    // -----------------------------------------------
    // class FrameFile
    //
    // A small private class to bundle info about framefiles.
    // Public access to fields is intentional.
    //
    private static class FrameFile {
        public final long id;
        public final File rpfFile;
        public final File waveletFile;
        public final Sector sector;
        public final RPFFrameFilename frameFile;
        public WaveletCodec codec;
        public RPFFrameTransform transform;

        public FrameFile(long id, File rpfFile, File waveletFile, Sector sector) {
            this.id = id;
            this.rpfFile = rpfFile;
            this.waveletFile = waveletFile;
            this.sector = sector;
            this.frameFile = RPFFrameFilename.parseFilename(rpfFile.getName().toUpperCase());
        }

        public RPFFrameTransform getFrameTransform() {
            if (this.transform == null) {
                RPFDataSeries dataSeries = RPFDataSeries.dataSeriesFor(this.frameFile.getDataSeriesCode());
                this.transform = RPFFrameTransform.createFrameTransform(this.frameFile.getZoneCode(),
                    dataSeries.rpfDataType, dataSeries.scaleOrGSD);
            }
            return this.transform;
        }

        public int getFrameNumber() {
            return frameFile.getFrameNumber();
        }
    }

    // --------------------------------------------
    // class ServiceInstance
    //
    // Used to manage per-request state.
    //
    public class RPFServiceInstance {
        public static final String BBOX = "bbox";
        public static final String WIDTH = "width";
        public static final String HEIGHT = "height";

        public RPFServiceInstance() {
        }

        public BufferedImage serviceRequest(KV params) throws IOException {
            if (params == null) {
                String message = Logging.getMessage("nullValue.AVListIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
            String message = this.validate(params);
            if (message != null) {
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            try {
                Sector reqSector = (Sector) params.get(RPFServiceInstance.BBOX);
                int reqWidth = (Integer) params.get(RPFServiceInstance.WIDTH);
                int reqHeight = (Integer) params.get(RPFServiceInstance.HEIGHT);

                BufferedImage reqImage = new BufferedImage(reqWidth, reqHeight, BufferedImage.TYPE_4BYTE_ABGR);
                int numFramesInRequest = 0;

                for (FrameFile frame : RPFGenerator.this.frameFiles) {
                    try {
                        // The call to getSector() can throw an exception if the file is
                        // named with an inappropriate frameNumber for the dataseries/zone.
                        // We don't want these to short circuit the entire request, so
                        // trap any such occurances and ignore 'em.
                        if (!reqSector.intersects(frame.sector))
                            continue;
                    }
                    catch (RuntimeException e) {
                        /* ignore this framefile */
                        String msg = String.format("Exception while computing frame bounds: %s", frame.rpfFile);
                        Logging.logger().log(Level.SEVERE, msg, e);
                        markFrameFileAbsent(frame);
                        continue;
                    }

                    if (RPFGenerator.this.isFrameFileAbsent(frame))
                        continue;

                    Sector frameSector = frame.sector;

                    // find size of the frame's footprint at the requested image resolution...
                    int footprintX = (int) (frameSector.lonDelta * reqImage.getWidth()
                        / reqSector.lonDelta);
                    int footprintY = (int) (frameSector.latDelta * reqImage.getHeight()
                        / reqSector.latDelta);

                    // Depending upon footprint, either get image from it RPF framefile, or reconstruct
                    // it from a wavelet encoding.
                    BufferedImage sourceImage;
                    if (footprintX > smallImageSize || footprintY > smallImageSize) {
                        RPFFrameTransform.RPFImage[] images = getImageFromRPFSource(frame);
                        if (images == null)
                            continue;
                        for (RPFFrameTransform.RPFImage image : images) {
                            if (image.getSector() == null || image.getImage() == null)
                                continue;
                            drawImageIntoRequest(reqImage, reqSector, image.getImage(), image.getSector());
                        }
                    } else {
                        int maxRes = footprintX;
                        maxRes = Math.max(footprintY, maxRes);
                        int power = (int) Math.ceil(Math.log(maxRes) / Math.log(2.0));
                        int res = (int) Math.pow(2.0, power);
                        res = Math.max(1, res);

                        sourceImage = getImageFromWaveletEncoding(frame, res);
                        if (sourceImage == null)
                            continue;
                        drawImageIntoRequest(reqImage, reqSector, sourceImage, frameSector);
                    }

                    ++numFramesInRequest;
                }

                if (numFramesInRequest <= 0)
                    return null;

                return reqImage;
            }
            catch (RuntimeException e) {
                String msg = "Exception while processing request";
                Logging.logger().log(Level.SEVERE, msg, e);
                throw new IOException(msg);
            }
        }

        private void drawImageIntoRequest(BufferedImage reqImage, Sector reqSector, RenderedImage srcImage,
            Sector srcSector) {

            double tx = (srcSector.lonMin - reqSector.lonMin) * (
                reqImage.getWidth() / reqSector.lonDelta);
            double ty = (reqSector.latMax - srcSector.latMax) * (
                reqImage.getHeight() / reqSector.latDelta);
            double sx = (reqImage.getWidth() / reqSector.lonDelta) * (
                srcSector.lonDelta / srcImage.getWidth());
            double sy = (reqImage.getHeight() / reqSector.latDelta) * (
                srcSector.latDelta / srcImage.getHeight());

            Graphics2D g2d = (Graphics2D) reqImage.getGraphics();
            AffineTransform xform = g2d.getTransform();
            g2d.translate(tx, ty);
            g2d.scale(sx, sy);
            g2d.drawRenderedImage(srcImage, null);
            g2d.setTransform(xform);
        }

        public BufferedImage serviceRequest(URL url) throws IOException {
            if (url == null) {
                String message = Logging.getMessage("nullValue.URLIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            KV params = new KVMap();
            // Extract query parameters from the URL, placing them in the AVList.
            String query = url.getQuery();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String s : pairs) {
                    String[] keyvalue = s.split("=", 2);
                    if (keyvalue.length == 2)
                        params.set(keyvalue[0], keyvalue[1]);
                }
            }
            // Convert parameter values to the appropriate type.
            initParams(params);

            return serviceRequest(params);
        }

        /**
         * Determines whether the constructor arguments are valid.
         *
         * @param params the list of parameters to validate.
         * @return null if valid, otherwise a <code>String</code> containing a description of why it's invalid.
         */
        private String validate(KV params) {
            StringBuilder sb = new StringBuilder();

            Object o = params.get(RPFServiceInstance.BBOX);
            if (!(o instanceof Sector))
                sb.append("bounding box");

            o = params.get(RPFServiceInstance.WIDTH);
            if (!(o instanceof Integer) || ((Integer) o) < 1)
                sb.append("width");

            o = params.get(RPFServiceInstance.HEIGHT);
            if (!(o instanceof Integer) || ((Integer) o) < 1)
                sb.append("height");

            if (sb.isEmpty())
                return null;

            return "Inavlid RPFGenerator service request fields: " + sb;
        }

        private KV initParams(KV params) {
            String s = params.getStringValue(RPFServiceInstance.BBOX);
            if (s != null) {
                String[] values = s.split(",");
                if (values.length == 4) {
                    try {
                        // Bounding box string is expected in WMS format: "minlon,minlat,maxlon,maxlat"
                        double minLon = Double.parseDouble(values[0]);
                        double minLat = Double.parseDouble(values[1]);
                        double maxLon = Double.parseDouble(values[2]);
                        double maxLat = Double.parseDouble(values[3]);
                        params.set(RPFServiceInstance.BBOX, Sector.fromDegrees(minLat, maxLat, minLon, maxLon));
                    }
                    catch (NumberFormatException e) {
                        Logging.logger().log(Level.WARNING, "Parameter conversion error", e);
                        params.set(RPFServiceInstance.BBOX, null);
                    }
                }
            }

            s = params.getStringValue(RPFServiceInstance.WIDTH);
            if (s != null) {
                try {
                    int value = Integer.parseInt(s);
                    params.set(RPFServiceInstance.WIDTH, value);
                }
                catch (NumberFormatException e) {
                    Logging.logger().log(Level.WARNING, "Parameter conversion error", e);
                    params.set(RPFServiceInstance.WIDTH, null);
                }
            }

            s = params.getStringValue(RPFServiceInstance.HEIGHT);
            if (s != null) {
                try {
                    int value = Integer.parseInt(s);
                    params.set(RPFServiceInstance.HEIGHT, value);
                }
                catch (NumberFormatException e) {
                    Logging.logger().log(Level.WARNING, "Parameter conversion error", e);
                    params.set(RPFServiceInstance.HEIGHT, null);
                }
            }

            return params;
        }

        //
        // Attempts to return the specified FrameFile as a BufferedImage. Returns null on failure.
        //
        private RPFFrameTransform.RPFImage[] getImageFromRPFSource(FrameFile frame) {
            try {
                File file = frame.rpfFile;
                RPFImageFile sourceFile = RPFImageFile.load(file);
                BufferedImage image = sourceFile.getBufferedImage();
                return frame.getFrameTransform().deproject(frame.getFrameNumber(), image);
            }
            catch (Exception e) {
                String message = "Exception while reading frame file: " + frame.rpfFile;
                Logging.logger().log(Level.SEVERE, message, e);
                RPFGenerator.this.markFrameFileAbsent(frame);
                return null;
            }
        }

        //
        // Attempts to reconstruct the given FrameFile as a BufferedImage from a WaveletEncoding.
        // Returns null if encoding does not exist or on any other failure.
        //
        private BufferedImage getImageFromWaveletEncoding(FrameFile frame, int resolution) {
            if (resolution <= 0)
                return null;

            try {
                WaveletCodec codec;
                if (resolution <= RPFGenerator.this.preloadRes) {
                    // Lazily load the wavelet up to "preload resolution".
                    if (frame.codec == null) {
                        ByteBuffer buffer = WWIO.readFileToBuffer(frame.waveletFile);
                        frame.codec = WaveletCodec.loadPartial(buffer, RPFGenerator.this.preloadRes);
                    }
                    codec = frame.codec;
                } else {
                    // Read wavelet file.
                    ByteBuffer buffer = WWIO.readFileToBuffer(frame.waveletFile);
                    codec = WaveletCodec.loadPartial(buffer, resolution);
                }

                BufferedImage sourceImage = null;
                sourceImage = codec.reconstruct(resolution);

                return sourceImage;
            }
            catch (Exception e) {
                String message = "Exception while reading wavelet file: " + frame.waveletFile;
                Logging.logger().log(Level.SEVERE, message, e);
                RPFGenerator.this.markFrameFileAbsent(frame);
                return null;
            }
        }
    }
}