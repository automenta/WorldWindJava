/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import com.jogamp.common.nio.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.cache.BasicGpuResourceCache;
import gov.nasa.worldwind.exception.WWRuntimeException;
import jcog.Log;
import jcog.exe.Exe;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.slf4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.zip.*;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * @author Tom Gaskins
 * @version $Id: WWIO.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WWIO {
    public static final String DELETE_ON_EXIT_PREFIX = "WWJDeleteOnExit";
    public static final String ILLEGAL_FILE_PATH_PART_CHARACTERS = '[' + "?/\\\\=+<>:;,\"|^\\[\\]" + ']';
    /**
     * The default character encoding used if none is specified.
     */
    protected static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
    protected static final Map<String, String> mimeTypeToSuffixMap = new HashMap<>();
    protected static final Map<String, String> suffixToMimeTypeMap = new HashMap<>();
    private final static Logger logger = Log.log(WWIO.class);

    static {
        WWIO.mimeTypeToSuffixMap.put("application/acad", "dwg");
        WWIO.mimeTypeToSuffixMap.put("application/bil", "bil");
        WWIO.mimeTypeToSuffixMap.put("application/bil16", "bil");
        WWIO.mimeTypeToSuffixMap.put("application/bil32", "bil");
        WWIO.mimeTypeToSuffixMap.put("application/dxf", "dxf");
        WWIO.mimeTypeToSuffixMap.put("application/octet-stream", "bin");
        WWIO.mimeTypeToSuffixMap.put("application/pdf", "pdf");
        WWIO.mimeTypeToSuffixMap.put("application/rss+xml", "xml");
        WWIO.mimeTypeToSuffixMap.put("application/rtf", "rtf");
        WWIO.mimeTypeToSuffixMap.put("application/sla", "slt");
        WWIO.mimeTypeToSuffixMap.put("application/vnd.google-earth.kml+xml", "kml");
        WWIO.mimeTypeToSuffixMap.put("application/vnd.google-earth.kmz", "kmz");
        WWIO.mimeTypeToSuffixMap.put("application/vnd.ogc.gml+xml", "gml");
        WWIO.mimeTypeToSuffixMap.put("application/x-gzip", "gz");
        WWIO.mimeTypeToSuffixMap.put("application/xml", "xml");
        WWIO.mimeTypeToSuffixMap.put("application/zip", "zip");
        WWIO.mimeTypeToSuffixMap.put("multipart/zip", "zip");
        WWIO.mimeTypeToSuffixMap.put("multipart/x-gzip", "gzip");

        WWIO.mimeTypeToSuffixMap.put("model/collada+xml", "dae");   // <--- burkey add

        WWIO.mimeTypeToSuffixMap.put("text/html", "html");
        WWIO.mimeTypeToSuffixMap.put("text/plain", "txt");
        WWIO.mimeTypeToSuffixMap.put("text/richtext", "rtx");
        WWIO.mimeTypeToSuffixMap.put("text/tab-separated-values", "tsv");
        WWIO.mimeTypeToSuffixMap.put("text/xml", "xml");

        WWIO.mimeTypeToSuffixMap.put("image/bmp", "bmp");
        WWIO.mimeTypeToSuffixMap.put("image/dds", "dds");
        WWIO.mimeTypeToSuffixMap.put("image/geotiff", "gtif");
        WWIO.mimeTypeToSuffixMap.put("image/gif", "gif");
        WWIO.mimeTypeToSuffixMap.put("image/jp2", "jp2");
        WWIO.mimeTypeToSuffixMap.put("image/jpeg", "jpg");
        WWIO.mimeTypeToSuffixMap.put("image/jpg", "jpg");
        WWIO.mimeTypeToSuffixMap.put("image/png", "png");
        WWIO.mimeTypeToSuffixMap.put("image/svg+xml", "svg");
        WWIO.mimeTypeToSuffixMap.put("image/tiff", "tif");
        WWIO.mimeTypeToSuffixMap.put("image/x-imagewebserver-ecw", "ecw");
        WWIO.mimeTypeToSuffixMap.put("image/x-mrsid", "sid");
        WWIO.mimeTypeToSuffixMap.put("image/x-rgb", "rgb");

        WWIO.mimeTypeToSuffixMap.put("video/mpeg", "mpg");
        WWIO.mimeTypeToSuffixMap.put("video/quicktime", "mov");

        WWIO.mimeTypeToSuffixMap.put("audio/x-aiff", "aif");
        WWIO.mimeTypeToSuffixMap.put("audio/x-midi", "mid");
        WWIO.mimeTypeToSuffixMap.put("audio/x-wav", "wav");

        WWIO.mimeTypeToSuffixMap.put("world/x-vrml", "wrl");

        //-----------------------------------------------

        WWIO.suffixToMimeTypeMap.put("aif", "audio/x-aiff");
        WWIO.suffixToMimeTypeMap.put("aifc", "audio/x-aiff");
        WWIO.suffixToMimeTypeMap.put("aiff", "audio/x-aiff");
        WWIO.suffixToMimeTypeMap.put("bil", "application/bil");
        WWIO.suffixToMimeTypeMap.put("bil16", "application/bil16");
        WWIO.suffixToMimeTypeMap.put("bil32", "application/bil32");
        WWIO.suffixToMimeTypeMap.put("bin", "application/octet-stream");
        WWIO.suffixToMimeTypeMap.put("bmp", "image/bmp");
        WWIO.suffixToMimeTypeMap.put("dds", "image/dds");
        WWIO.suffixToMimeTypeMap.put("dwg", "application/acad");
        WWIO.suffixToMimeTypeMap.put("dxf", "application/dxf");
        WWIO.suffixToMimeTypeMap.put("ecw", "image/x-imagewebserver-ecw");
        WWIO.suffixToMimeTypeMap.put("gif", "image/gif");
        WWIO.suffixToMimeTypeMap.put("gml", "application/vnd.ogc.gml+xml");
        WWIO.suffixToMimeTypeMap.put("gtif", "image/geotiff");
        WWIO.suffixToMimeTypeMap.put("gz", "application/x-gzip");
        WWIO.suffixToMimeTypeMap.put("gzip", "multipart/x-gzip");
        WWIO.suffixToMimeTypeMap.put("htm", "text/html");
        WWIO.suffixToMimeTypeMap.put("html", "text/html");
        WWIO.suffixToMimeTypeMap.put("jp2", "image/jp2");
        WWIO.suffixToMimeTypeMap.put("jpeg", "image/jpeg");
        WWIO.suffixToMimeTypeMap.put("jpg", "image/jpeg");
        WWIO.suffixToMimeTypeMap.put("kml", "application/vnd.google-earth.kml+xml");
        WWIO.suffixToMimeTypeMap.put("kmz", "application/vnd.google-earth.kmz");
        WWIO.suffixToMimeTypeMap.put("mid", "audio/x-midi");
        WWIO.suffixToMimeTypeMap.put("midi", "audio/x-midi");
        WWIO.suffixToMimeTypeMap.put("mov", "video/quicktime");
        WWIO.suffixToMimeTypeMap.put("mp3", "audio/x-mpeg");
        WWIO.suffixToMimeTypeMap.put("mpe", "video/mpeg");
        WWIO.suffixToMimeTypeMap.put("mpeg", "video/mpeg");
        WWIO.suffixToMimeTypeMap.put("mpg", "video/mpeg");
        WWIO.suffixToMimeTypeMap.put("pdf", "application/pdf");
        WWIO.suffixToMimeTypeMap.put("png", "image/png");
        WWIO.suffixToMimeTypeMap.put("rgb", "image/x-rgb");
        WWIO.suffixToMimeTypeMap.put("rtf", "application/rtf");
        WWIO.suffixToMimeTypeMap.put("rtx", "text/richtext");
        WWIO.suffixToMimeTypeMap.put("sid", "image/x-mrsid");
        WWIO.suffixToMimeTypeMap.put("slt", "application/sla");
        WWIO.suffixToMimeTypeMap.put("svg", "image/svg+xml");
        WWIO.suffixToMimeTypeMap.put("tif", "image/tiff");
        WWIO.suffixToMimeTypeMap.put("tiff", "image/tiff");
        WWIO.suffixToMimeTypeMap.put("tsv", "text/tab-separated-values");
        WWIO.suffixToMimeTypeMap.put("txt", "text/plain");
        WWIO.suffixToMimeTypeMap.put("wav", "audio/x-wav");
        WWIO.suffixToMimeTypeMap.put("wbmp", "image/vnd.wap.wbmp");
        WWIO.suffixToMimeTypeMap.put("wrl", "world/x-vrml");
        WWIO.suffixToMimeTypeMap.put("xml", "application/xml");
        WWIO.suffixToMimeTypeMap.put("zip", "application/zip");
    }

    public static void get(String url, ThrowingConsumer<HttpEntity> success) {
        WWIO.get(url, success, z -> true);
    }

    /**
     * fail predicate returns true to bubble up the exception
     */
    public static void get(String url, ThrowingConsumer<HttpEntity> success, Predicate<Throwable> fail) {

//        Exe.run(()->{
//        try {
            Throwable e = null;
            HttpGet httpget = new HttpGet(url);

            try (CloseableHttpResponse response = (CloseableHttpResponse) Configuration.http.execute(httpget, Configuration.httpCache)) {
                final StatusLine status = response.getStatusLine();
                final int code = status.getStatusCode();
                if (code==HTTP_OK) {
                    try {
                        success.accept(response.getEntity());
//                        String state = response.getEntity() instanceof CacheEntity ? "GOT" : "GET";
                        String state = "";
                        WWIO.logger.info("{} {} {}", code, state, url);
                    } catch (Throwable throwable) {
                        e = throwable;
                    }
                } else
                    e = new IOException(status.getReasonPhrase());

                    //r.cacheResponse() != null ? "GOT" : "GET"

//                CacheResponseStatus responseStatus = Configuration.httpCache.getCacheResponseStatus();
//                switch (responseStatus) {
//                    case CACHE_HIT:
//                        System.out.println("A response was generated from the cache with " +
//                            "no requests sent upstream");
//                        break;
//                    case CACHE_MODULE_RESPONSE:
//                        System.out.println("The response was generated directly by the " +
//                            "caching module");
//                        break;
//                    case CACHE_MISS:
//                        System.out.println("The response came from an upstream server");
//                        break;
//                    case VALIDATED:
//                        System.out.println("The response was generated from the cache " +
//                            "after validating the entry with the origin server");
//                        break;
//                }
            } catch (IOException i) {
                //assert(e==null);
                e = i;
            }

            //httpget.releaseConnection();

            if (e!=null) {
                if (fail.test(e)) {
                    WWIO.logger.warn("{} {}", e.getMessage(), url);
                    if (e instanceof RuntimeException)
                        throw (RuntimeException) e;
                    else
                        throw new RuntimeException(e);
                }
                throw new RuntimeException(e);
            }

//            Request.Builder requestBuilder = new Request.Builder()
//                .cacheControl(Configuration.cacheControl)
//                .header("User-Agent", Configuration.userAgent);
//
//            final Call call = Configuration.http.newCall(requestBuilder.url(url).build());
//
//            call.enqueue(new Callback() {
//
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    throw new RuntimeException(e);
//                }
//
//                @Override
//                public void onResponse(Call call, Response r) {
//                    try {
//                        success.accept(r.body());
//                        WWIO.logger.info("{} {}", r.cacheResponse() != null ? "GOT" : "GET", url);
//                    }
//                    catch (Throwable throwable) {
//                        throw new RuntimeException(throwable);
//                    }
//                }
//            });
//        }
//        catch (Throwable e) {
//            if (fail.test(e)) {
//                WWIO.logger.warn("{} {}", e.getMessage(), url);
//                if (e instanceof RuntimeException)
//                    throw (RuntimeException) e;
//                else
//                    throw new RuntimeException(e);
//            }
//        }
//        });
    }

    public static String formPath(String... pathParts) {
        StringBuilder sb = new StringBuilder();

        for (String pathPart : pathParts) {
            if (pathPart == null)
                continue;

            if (!sb.isEmpty())
                sb.append(File.separator);
            sb.append(pathPart.replaceAll(WWIO.ILLEGAL_FILE_PATH_PART_CHARACTERS, "_"));
        }

        return sb.toString();
    }

    public static String appendPathPart(String firstPart, String secondPart) {
        if (secondPart == null || secondPart.isEmpty())
            return firstPart;
        if (firstPart == null || firstPart.isEmpty())
            return secondPart;

        StringBuilder sb = new StringBuilder();
        sb.append(WWIO.stripTrailingSeparator(firstPart));
        sb.append(File.separator);
        sb.append(WWIO.stripLeadingSeparator(secondPart));

        return sb.toString();
    }

    /**
     * Replaces any illegal filename characters in a specified string with an underscore, "_".
     *
     * @param s the string to examine.
     * @return a new string with illegal filename characters replaced.
     * @throws IllegalArgumentException if the specified string is null.
     */
    public static String replaceIllegalFileNameCharacters(String s) {

        return s.replaceAll(WWIO.ILLEGAL_FILE_PATH_PART_CHARACTERS, "_");
    }

    public static String stripTrailingSeparator(String s) {

        if (!s.isEmpty() && s.charAt(s.length() - 1) == '/' || !s.isEmpty() && s.charAt(s.length() - 1) == '\\')
            return s.substring(0, s.length() - 1);
        else
            return s;
    }

    public static String stripLeadingSeparator(String s) {

        if (!s.isEmpty() && s.charAt(0) == '/' || !s.isEmpty() && s.charAt(0) == '\\')
            return s.substring(1);
        else
            return s;
    }

    public static String stripLeadingZeros(String s) {

        int len = s.length();
        if (len < 2) // String is empty or is a single character, so there is nothing to strip.
            return s;

        int i = 0;
        while (i < len && s.charAt(i) == '0') {
            i++;
        }
        if (i == len) // String is just '0' characters. Leave the last one.
            i = len - 1;

        if (i == 0) // String doesn't contain any '0' characters, return the original string.
            return s;

        return s.substring(i, len); // String contains at least one leading '0' character.
    }

    /**
     * Converts several types of addresses to a local file to a {@link File}. Returns null if the source cannot be
     * converted to a file. The source type may be one of the following: <ul><li>{@link URL}</li> <li>{@link URI}</li>
     * <li>{@link File}</li> <li>{@link String} containing a valid URL description, a valid URI description, or a valid
     * path to a local file.</li> </ul>
     *
     * @param src the source to convert to local file path.
     * @return a local File path, or null if the source could not be converted.
     * @throws IllegalArgumentException if the source is null or an empty string.
     */
    public static File getFileForLocalAddress(Object src) {
        if (WWUtil.isEmpty(src)) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (src instanceof File)
            return (File) src;

        else if (src instanceof URL)
            return WWIO.convertURLToFile((URL) src);

        else if (src instanceof URI)
            return WWIO.convertURIToFile((URI) src);

        else if (!(src instanceof String))
            return null;

        String sourceName = (String) src;

        File file = new File(sourceName);
        if (file.exists())
            return file;

        URL url = WWIO.makeURL(sourceName);
        if (url != null)
            return WWIO.convertURLToFile(url);

        URI uri = WWIO.makeURI(sourceName);
        if (uri != null)
            return WWIO.convertURIToFile(uri);

        return null;
    }

    /**
     * Converts a specified URI as to a path in the local file system. If the URI cannot be converted to a file path for
     * any reason, this returns null.
     *
     * @param uri the URI to convert to a local file path.
     * @return a local File path, or null if the URI could not be converted.
     * @throws IllegalArgumentException if the url is null.
     */
    public static File convertURIToFile(URI uri) {

        try {
            return new File(uri);
        }
        catch (IllegalArgumentException e) {
            // Thrown if the URI cannot be interpreted as a path on the local filesystem.
            return null;
        }
    }

    /**
     * Converts a specified URL as to a path in the local file system. If the URL cannot be converted to a file path for
     * any reason, this returns null.
     *
     * @param url the URL to convert to a local file path.
     * @return a local File path, or null if the URL could not be converted.
     * @throws IllegalArgumentException if the url is null.
     */
    public static File convertURLToFile(URL url) {

        try {
            return new File(url.toURI());
        }
        catch (IllegalArgumentException | URISyntaxException e) {
            // Thrown if the URI cannot be interpreted as a path on the local filesystem.
            return null;
        }
        // Thrown if the URL cannot be converted to a URI.
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean saveBuffer(ByteBuffer buffer, File file, boolean forceFilesystemWrite) throws IOException {

        FileOutputStream fos = null;
        FileChannel channel = null;
        FileLock lock;
        int numBytesWritten = 0;
        try {
            fos = new FileOutputStream(file);
            channel = fos.getChannel();

            lock = channel.tryLock();
            if (lock == null) {
                // The file is being written to, or some other process is keeping it to itself.
                // This is an okay condition, but worth noting.
                Logging.logger().log(Level.FINER, "WWIO.UnableToAcquireLockFor", file.getPath());
                return false;
            }

            for (buffer.rewind(); buffer.hasRemaining(); ) {
                numBytesWritten += channel.write(buffer);
            }

            // Optionally force writing to the underlying storage device. Doing so ensures that all contents are
            // written to the device (and not in the I/O cache) in the event of a system failure.
            if (forceFilesystemWrite)
                channel.force(true);
            fos.flush();
            return true;
        }
        catch (ClosedByInterruptException e) {
            Logging.logger().log(Level.FINE,
                Logging.getMessage("generic.interrupted", "WWIO.saveBuffer", file.getPath()), e);

            if (numBytesWritten > 0) // don't leave behind incomplete files
                file.delete();

            throw e;
        }
        catch (IOException e) {
            Logging.logger().log(Level.SEVERE, Logging.getMessage("WWIO.ErrorSavingBufferTo", file.getPath()), e);

            if (numBytesWritten > 0) // don't leave behind incomplete files
                file.delete();

            throw e;
        }
        finally {
            WWIO.closeStream(channel, file.getPath()); // also releases the lock
            WWIO.closeStream(fos, file.getPath());
        }
    }

    public static boolean saveBuffer(ByteBuffer buffer, File file) throws IOException {
        // By default, force changes to be written to the underlying storage device.
        return WWIO.saveBuffer(buffer, file, true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean saveBufferToStream(ByteBuffer buffer, OutputStream fos)
        throws IOException {

        WritableByteChannel channel;
        try {
            channel = Channels.newChannel(fos);

            for (buffer.rewind(); buffer.hasRemaining(); ) {
                channel.write(buffer);
            }

            fos.flush();
            return true;
        }
        finally {
            WWIO.closeStream(fos, null);
        }
    }

    /**
     * Maps the specified File's bytes directly into memory as a {@link MappedByteBuffer} according to the specified
     * mode.
     * <p>
     * If the mode is {@link FileChannel.MapMode#READ_ONLY}, the file is mapped in read-only mode, and any attempt to
     * modify the contents of the returned MappedByteBuffer causes a {@link ReadOnlyBufferException}.
     * <p>
     * If the mode is {@link FileChannel.MapMode#READ_WRITE}, the file is mapped in read-write mode. Changing the
     * contents of the returned MappedByteBuffer to be eventually propagated to the file. The specified file must be
     * avialable for both reading and writing.
     * <p>
     * If the mode is {@link FileChannel.MapMode#PRIVATE}, the file is mapped in copy-on-write mode. Changing the
     * contents of the returned MappedByteBuffer causes private copies of portions of the buffer to be created. The
     * specified file must be avialable for both reading and writing.
     *
     * @param file the file to map.
     * @param mode the mapping mode, one of {@link FileChannel.MapMode#READ_ONLY}, {@link
     *             FileChannel.MapMode#READ_WRITE}, or {@link FileChannel.MapMode#PRIVATE}.
     * @return a MappedByteBuffer representing the File's bytes.
     * @throws IOException if the file cannot be mapped for any reason.
     */
    public static MappedByteBuffer mapFile(File file, FileChannel.MapMode mode) throws IOException {

        String accessMode;
        if (mode == FileChannel.MapMode.READ_ONLY)
            accessMode = "r";
        else // (mode == FileChannel.MapMode.READ_WRITE || mode == FileChannel.MapMode.PRIVATE)
            accessMode = "rw";

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, accessMode);
            try (FileChannel fc = raf.getChannel()) {
                return fc.map(mode, 0, fc.size());
            }
        }
        finally {
            WWIO.closeStream(raf, file.getPath());
        }
    }

    /**
     * Maps the specified File's bytes directly into memory as a {@link MappedByteBuffer}. The file is mapped in
     * read-only mode; any attempt to modify the contents of the returned MappedByteBuffer causes a {@link
     * ReadOnlyBufferException}.
     *
     * @param file the file to map.
     * @return a read-only MappedByteBuffer representing the File's bytes.
     * @throws IOException if the file cannot be mapped for any reason.
     */
    public static MappedByteBuffer mapFile(File file) throws IOException {
        if (file == null) {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return WWIO.mapFile(file, FileChannel.MapMode.READ_ONLY);
    }

    /**
     * Reads all the bytes from the specified {@link URL}, returning the bytes as a non-direct {@link ByteBuffer} with
     * the current JVM byte order. Non-direct buffers are backed by JVM heap memory.
     *
     * @param url the URL to read.
     * @return the bytes from the specified URL, with the current JVM byte order.
     * @throws IllegalArgumentException if the URL is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readURLContentToBuffer(URL url) throws IOException {

        return WWIO.readURLContentToBuffer(url, false);
    }

    /**
     * Reads all the bytes from the specified {@link URL}, returning the bytes as a {@link ByteBuffer} with the current
     * JVM byte order. This returns a direct ByteBuffer if allocateDirect is true, and returns a non-direct ByteBuffer
     * otherwise. Direct buffers are backed by native memory, and may resite outside of the normal garbage-collected
     * heap. Non-direct buffers are backed by JVM heap memory.
     *
     * @param url            the URL to read.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     * @return the bytes from the specified URL, with the current JVM byte order.
     * @throws IllegalArgumentException if the URL is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readURLContentToBuffer(URL url, boolean allocateDirect) throws IOException {

        InputStream is = null;
        try {
            is = url.openStream();
            return WWIO.readStreamToBuffer(is, allocateDirect);
        }
        finally {
            WWIO.closeStream(is, url.toString());
        }
    }

    /**
     * Reads all the bytes from the specified {@link File}, returning the bytes as a non-direct {@link ByteBuffer} with
     * the current JVM byte order. Non-direct buffers are backed by JVM heap memory.
     *
     * @param file the file to read.
     * @return the bytes from the specified file, with the current JVM byte order.
     * @throws IllegalArgumentException if the file is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readFileToBuffer(File file) throws IOException {

        return WWIO.readFileToBuffer(file, false);
    }

    /**
     * Reads all the bytes from the specified {@link File}, returning the bytes as a {@link ByteBuffer} with the current
     * JVM byte order. This returns a direct ByteBuffer if allocateDirect is true, and returns a non-direct ByteBuffer
     * otherwise. Direct buffers are backed by native memory, and may reside outside of the normal garbage-collected
     * heap. Non-direct buffers are backed by JVM heap memory.
     *
     * @param file           the file to read.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     * @return the bytes from the specified file, with the current JVM byte order.
     * @throws IllegalArgumentException if the file is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readFileToBuffer(File file, boolean allocateDirect) throws IOException {

        try (FileInputStream is = new FileInputStream(file)) {
            try (FileChannel fc = is.getChannel()) {
                int size = (int) fc.size();
                ByteBuffer buffer = allocateDirect ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
                for (int count = 0; count >= 0 && buffer.hasRemaining(); ) {
                    count = fc.read(buffer);
                }
                buffer.flip();
                return buffer;
            }
        }
    }

    public static ByteBuffer readGZipFileToBuffer(File gzFile) throws IllegalArgumentException, IOException {
        if (!gzFile.exists()) {
            String message = Logging.getMessage("generic.FileNotFound", gzFile.getAbsolutePath());
            Logging.logger().severe(message);
            throw new FileNotFoundException(message);
        }
        if (!gzFile.canRead()) {
            String message = Logging.getMessage("generic.FileNoReadPermission", gzFile.getAbsolutePath());
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        int inflatedLength = WWIO.gzipGetInflatedLength(gzFile);
        if (0 == inflatedLength) {
            String message = Logging.getMessage("generic.LengthIsInvalid", gzFile.getAbsolutePath());
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        ByteBuffer buffer = null;
        GZIPInputStream is = null;

        try {
            is = new GZIPInputStream(new FileInputStream(gzFile));
            buffer = WWIO.transferStreamToByteBuffer(is, inflatedLength);
            buffer.rewind();
        }
        finally {
            WWIO.closeStream(is, gzFile.getPath());
        }

        return buffer;
    }

    private static int gzipGetInflatedLength(File gzFile) throws IOException {
        RandomAccessFile raf = null;
        int length = 0;
        try {
            raf = new RandomAccessFile(gzFile, "r");
            raf.seek(raf.length() - 4);
            int b4 = 0xFF & raf.read();
            int b3 = 0xFF & raf.read();
            int b2 = 0xFF & raf.read();
            int b1 = 0xFF & raf.read();
            length = (b1 << 24) | (b2 << 16) + (b3 << 8) + b4;
        }
        finally {
            if (null != raf)
                raf.close();
        }
        return length;
    }

    public static ByteBuffer readZipEntryToBuffer(File zipFile, String entryName) throws IOException {

        InputStream is = null;
        ZipEntry ze = null;
        try {
            try (ZipFile zf = new ZipFile(zipFile)) {
                if (zf.size() < 1) {
                    String message = Logging.getMessage("WWIO.ZipFileIsEmpty", zipFile.getPath());
                    Logging.logger().severe(message);
                    throw new IOException(message);
                }

                if (entryName != null) {   // Read the specified entry
                    ze = zf.getEntry(entryName);
                    if (ze == null) {
                        String message = Logging.getMessage("WWIO.ZipFileEntryNIF", entryName, zipFile.getPath());
                        Logging.logger().severe(message);
                        throw new IOException(message);
                    }
                } else {   // Grab first first file entry
                    Enumeration entries = zf.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = (ZipEntry) entries.nextElement();
                        if (null != entry && !entry.isDirectory()) {
                            ze = entry;
                            break;
                        }
                    }
                    if (null == ze) {
                        String message = Logging.getMessage("WWIO.ZipFileIsEmpty", zipFile.getPath());
                        Logging.logger().severe(message);
                        throw new IOException(message);
                    }
                }

                is = zf.getInputStream(ze);
            }
            ByteBuffer buffer = null;
            if (ze.getSize() > 0) {
                buffer = WWIO.transferStreamToByteBuffer(is, (int) ze.getSize());
                buffer.rewind();
            }
            return buffer;
        }
        finally {
            WWIO.closeStream(is, entryName);
        }
    }

    private static ByteBuffer transferStreamToByteBuffer(InputStream stream, int numBytes) throws IOException {

        if (numBytes < 1) {
            Logging.logger().severe("WWIO.NumberBytesTransferLessThanOne");
            throw new IllegalArgumentException(Logging.getMessage("WWIO.NumberBytesTransferLessThanOne"));
        }

        int bytesRead = 0;
        int count = 0;
        byte[] bytes = new byte[numBytes];
        while (count >= 0 && (numBytes - bytesRead) > 0) {
            count = stream.read(bytes, bytesRead, numBytes - bytesRead);
            if (count > 0) {
                bytesRead += count;
            }
        }
        ByteBuffer buffer = Buffers.newDirectByteBuffer(bytes.length); // to get a jogl-compatible buffer
        return buffer.put(bytes);
    }

    /**
     * Reads all the available bytes from the specified {@link InputStream}, returning the bytes as a non-direct {@link
     * ByteBuffer} with the current JVM byte order. Non-direct buffers are backed by JVM heap memory.
     *
     * @param inputStream the stream to read.
     * @return the bytes from the specified stream, with the current JVM byte order.
     * @throws IllegalArgumentException if the stream is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readStreamToBuffer(InputStream inputStream) throws IOException {

        return WWIO.readStreamToBuffer(inputStream, false);
    }

    /**
     * Reads all the available bytes from the specified {@link InputStream}, returning the bytes as a {@link ByteBuffer}
     * with the current JVM byte order. This returns a direct ByteBuffer if allocateDirect is true, and returns a
     * non-direct ByteBuffer otherwise. Direct buffers are backed by native memory, and may reside outside of the normal
     * garbage-collected heap. Non-direct buffers are backed by JVM heap memory.
     *
     * @param inputStream    the stream to read.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     * @return the bytes from the specified stream, with the current JVM byte order.
     * @throws IllegalArgumentException if the stream is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readStreamToBuffer(InputStream inputStream, boolean allocateDirect) throws IOException {

        return WWIO.readInputStreamToBuffer(inputStream, allocateDirect);
    }

    /**
     * Reads all the available bytes from the specified {@link InputStream}, returning the bytes as a String. The bytes
     * are interpreted according to the specified encoding, or UTF-8 if no encoding is specified.
     *
     * @param stream   the stream to read.
     * @param encoding the encoding do use. If null is specified then UTF-8 is used.
     * @return the string representation of the bytes in the stream decoded according to the specified encoding.
     * @throws IllegalArgumentException if the <code>stream</code> is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static String readStreamToString(InputStream stream, String encoding) throws IOException {

        return IOUtils.toString(stream, encoding);
    }

    /**
     * Reads all the available bytes from the specified {@link ReadableByteChannel}, returning the bytes as a {@link
     * ByteBuffer} with the current JVM byte order. This returns a direct ByteBuffer if allocateDirect is true, and
     * returns a non-direct ByteBuffer otherwise. Direct buffers are backed by native memory, and may reside outside of
     * the normal garbage-collected heap. Non-direct buffers are backed by JVM heap memory.
     *
     * @param stream         the channel to read.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     * @return the bytes from the specified channel, with the current JVM byte order.
     * @throws IllegalArgumentException if the channel is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readInputStreamToBuffer(InputStream stream, boolean allocateDirect)
        throws IOException {

        ReadableByteChannel channel = Channels.newChannel(stream);
        final int PAGE_SIZE = (int) Math.round(Math.pow(2, 16));
        final int initialSize = Math.max(stream.available(), PAGE_SIZE);
        ByteBuffer buffer = WWBufferUtil.newByteBuffer(initialSize, allocateDirect);
        int count = 0;
        while (count >= 0) {
            count = channel.read(buffer);

            if (count > 0 && !buffer.hasRemaining()) {
                int newCap = buffer.limit() + PAGE_SIZE;
                ByteBuffer biggerBuffer = allocateDirect ? ByteBuffer.allocateDirect(newCap)
                    : ByteBuffer.allocate(newCap);
                biggerBuffer.put(buffer.rewind());
                buffer = biggerBuffer;
            }
        }

        if (buffer != null)
            buffer.flip();

        return buffer;
    }

    /**
     * Reads the available bytes from the specified {@link ReadableByteChannel} up to the number of bytes remaining in
     * the buffer. Bytes read from the specified channel are copied to the specified {@link ByteBuffer}. Upon returning
     * the specified buffer's limit is set to the number of bytes read, and its position is set to zero.
     *
     * @param channel the channel to read bytes from.
     * @param buffer  the buffer to receive the bytes.
     * @return the specified buffer.
     * @throws IllegalArgumentException if the channel or the buffer is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readInputStreamToBuffer(ReadableByteChannel channel, ByteBuffer buffer)
        throws IOException {

        int count = 0;
        while (count >= 0 && buffer.hasRemaining()) {
            count = channel.read(buffer);
        }

        buffer.flip();

        return buffer;
    }

    /**
     * Reads all the available bytes from the specified {@link ReadableByteChannel}, returning the bytes as a String.
     * The bytes are interpreted according to the specified encoding, or UTF-8 if no encoding is specified.
     *
     * @param channel  the channel to read.
     * @param encoding the encoding do use. If null is specified then UTF-8 is used.
     * @return the string representation of the bytes in the channel decoded according to the specified encoding.
     * @throws IllegalArgumentException if the <code>stream</code> is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static String readChannelToString(ReadableByteChannel channel, String encoding) throws IOException {

        return WWIO.readCharacterStreamToString(
            Channels.newReader(channel, encoding != null ? encoding : WWIO.DEFAULT_CHARACTER_ENCODING));
    }

    /**
     * Reads all the character stream content from the specified {@link Reader}, returning a the accumulated content as
     * a String .
     *
     * @param reader the character stream to read.
     * @return the string representing the accumulated content from the character stream.
     * @throws IllegalArgumentException if the <code>reader</code> is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static String readCharacterStreamToString(Reader reader) throws IOException {

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(reader);
        String line;

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    public static String replaceSuffix(String in, String newSuffix) {

        String suffix = newSuffix != null ? newSuffix : "";
        int p = in.lastIndexOf('.');
        return p >= 0 ? in.substring(0, p) + suffix : in + suffix;
    }

    public static String getSuffix(String filePath) {

        int len = filePath.length();
        int p = filePath.lastIndexOf('.');
        String suffix = (p >= 0 && p + 1 < len) ? filePath.substring(p + 1, len) : null;

        // handle .bil.gz extensions
        if (null != suffix && p > 0 && "gz".equals(suffix)) {
            int idx = filePath.lastIndexOf('.', p - 1);
            suffix = (idx >= 0 && idx + 1 < len) ? filePath.substring(idx + 1, len) : suffix;
        }

        return suffix;
    }

    /**
     * Returns the name of the file or directory denoted by the specified path. This is the last file name in the path,
     * or null if the path does not contain any file names.
     *
     * @param filePath a file path String.
     * @return the last name in the specified path, or null if the path does not contain a name.
     * @throws IllegalArgumentException if the file path is null.
     */
    public static String getFilename(String filePath) {

        filePath = WWIO.stripTrailingSeparator(filePath);

        int len = filePath.length();
        int p = filePath.lastIndexOf('/');
        if (p < 0)
            p = filePath.lastIndexOf('\\');
        return (p >= 0 && p + 1 < len) ? filePath.substring(p + 1, len) : null;
    }

    /**
     * Returns the file path's parent directory path, or null if the file path does not have a parent.
     *
     * @param filePath a file path String.
     * @return the file path's parent directory, or null if the path does not have a parent.
     * @throws IllegalArgumentException if the file path is null.
     */
    public static String getParentFilePath(String filePath) {

        filePath = WWIO.stripTrailingSeparator(filePath);

        int len = filePath.length();
        int p = filePath.lastIndexOf('/');
        if (p < 0)
            p = filePath.lastIndexOf('\\');
        return (p > 0 && p < len) ? filePath.substring(0, p) : null;
    }

    /**
     * Ensure that all directories leading the element at the end of a file path exist. Create any nonexistent
     * directories in the path. A directory is not creared for the last element in the path; it's assumed to be a file
     * name and is ignored.
     *
     * @param path the path whose directories are vefified to exist or be created. The last element of the path is
     *             ignored.
     * @return true if all the directories in the path exist or were created.
     * @throws IllegalArgumentException if the path is null.
     */
    public static boolean makeParentDirs(String path) {

        String fs = File.separator;
        String[] pathParts = path.split(
            "[/" + (fs.equals("/") ? "" : (fs.equals("\\") ? "\\\\" : fs)) + "misngValue]");
        if (pathParts.length <= 1)
            return true;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pathParts.length - 1; i++) {
            if (pathParts[i].isEmpty())
                continue;

            sb.append(File.separator);
            sb.append(pathParts[i]);
        }

        return (new File(sb.toString())).mkdirs();
    }

    /**
     * Create a directory in the computer's temp directory.
     *
     * @return a file reference to the new directory, of null if a directory could not be created.
     * @throws IOException       if a directory could not be created.
     * @throws SecurityException if a security manager exists and it does not allow directory creation.
     */
    public static File makeTempDir() throws IOException {
        // To make a directory in the computer's temp directory, generate the name of a temp file then delete the file
        // and create a directory of the same name.
        File temp = File.createTempFile("wwj", null);

        if (!temp.delete())
            return null;

        if (!temp.mkdir())
            return null;

        return temp;
    }

    public static File saveBufferToTempFile(ByteBuffer buffer, String suffix) throws IOException {

        File outputFile = File.createTempFile("WorldWind", suffix != null ? suffix : "");
        outputFile.deleteOnExit();
        buffer.rewind();
        WWIO.saveBuffer(buffer, outputFile);

        return outputFile;
    }

    public static boolean isFileOutOfDate(URL url, long expiryTime) {

        try {
            // Determine whether the file can be treated like a File, e.g., a jar entry.
            URI uri = url.toURI();
            if (uri.isOpaque())
                return false; // TODO: Determine how to check the date of non-Files

            File file = new File(uri);

            return file.exists() && file.lastModified() < expiryTime;
        }
        catch (URISyntaxException e) {
            Logging.logger().log(Level.SEVERE, "WWIO.ExceptionValidatingFileExpiration", url);
            return false;
        }
    }

    public static Proxy configureProxy() {
        String proxyHost = Configuration.getStringValue(Keys.URL_PROXY_HOST);
        if (proxyHost == null)
            return null;

        Proxy proxy = null;

        try {
            int proxyPort = Configuration.getIntegerValue(Keys.URL_PROXY_PORT);
            String proxyType = Configuration.getStringValue(Keys.URL_PROXY_TYPE);

            SocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
            if (Objects.equals(proxyType, "Proxy.Type.Http"))
                proxy = new Proxy(Proxy.Type.HTTP, addr);
            else if (proxyType.equals("Proxy.Type.SOCKS"))
                proxy = new Proxy(Proxy.Type.SOCKS, addr);
        }
        catch (RuntimeException e) {
            Logging.logger().log(Level.WARNING,
                Logging.getMessage("URLRetriever.ErrorConfiguringProxy", proxyHost), e);
        }

        return proxy;
    }

    /**
     * Indicates whether a {@link File} contains content of a specified mime type.
     * <p>
     * Only the filename suffix is consulted to determine the file's content type.
     *
     * @param file      the file to test.
     * @param mimeTypes the mime types to test for.
     * @return true if the file contains a specified content type, false if the file does not contain a specified
     * content type, the specified file is null, or no content types are specified.
     */
    public static boolean isContentType(File file, String... mimeTypes) {
        if (file == null || mimeTypes == null)
            return false;

        for (String mimeType : mimeTypes) {
            if (mimeType == null)
                continue;

            String typeSuffix = WWIO.mimeSuffix(mimeType);
            String fileSuffix = WWIO.getSuffix(file.getName());

            if (fileSuffix == null)
                continue;

            if (!(!fileSuffix.isEmpty() && fileSuffix.charAt(0) == '.'))
                fileSuffix = '.' + fileSuffix;

            if (fileSuffix.equalsIgnoreCase(typeSuffix))
                return true;
        }

        return false;
    }

    /**
     * Returns the file suffix string corresponding to the specified mime type string. The returned suffix starts with
     * the period character '.' followed by the mime type's subtype, as in: ".[subtype]".
     *
     * @param mimeType the mime type who's suffix is returned.
     * @return the file suffix for the specified mime type, with a leading ".".
     * @throws IllegalArgumentException if the mime type is null or malformed.
     */
    public static String mimeSuffix(String mimeType) {

        if (!mimeType.contains("/") || mimeType.charAt(mimeType.length() - 1) == '/') {
            String message = Logging.getMessage("generic.InvalidImageFormat");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // Remove any parameters appended to this mime type before using it as a key in the mimeTypeToSuffixMap. Mime
        // parameters do not change the mapping from mime type to suffix.
        int paramIndex = mimeType.indexOf(';');
        if (paramIndex != -1)
            mimeType = mimeType.substring(0, paramIndex);

        String suffix = WWIO.mimeTypeToSuffixMap.get(mimeType);

        if (suffix == null)
            suffix = mimeType.substring(mimeType.lastIndexOf('/') + 1);

        suffix = suffix.replaceFirst("bil32", "bil"); // if bil32, replace with "bil" suffix.
        suffix = suffix.replaceFirst("bil16", "bil"); // if bil16, replace with "bil" suffix.

        return '.' + suffix;
    }

    /**
     * Returns the mime type string corresponding to the specified file suffix string.
     *
     * @param suffix the suffix who's mime type is returned.
     * @return the mime type for the specified file suffix.
     * @throws IllegalArgumentException if the file suffix is null.
     */
    public static String makeMimeTypeForSuffix(String suffix) {
//        if (suffix == null) {
//            String message = Logging.getMessage("nullValue.FormatSuffixIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalStateException(message);
//        }

        // Strip the starting period from the suffix string, if any exists.
        if (!suffix.isEmpty() && suffix.charAt(0) == '.')
            suffix = suffix.substring(1);

        return WWIO.suffixToMimeTypeMap.get(suffix.toLowerCase());
    }

    /**
     * Returns the data type constant corresponding to the specified mime type string. Supported mime types are as
     * mapped to data types as follows: <table> <caption style="font-weight: bold;">Mapping</caption>
     * <tr><th>Mime Type</th><th>Data Type</th></tr>
     * <tr><td>application/bil32</td><td>{@link Keys#FLOAT32}</td></tr>
     * <tr><td>application/bil16</td><td>{@link Keys#INT16}</td></tr>
     * <tr><td>application/bil</td><td>{@link Keys#INT16}</td></tr>
     * <tr><td>image/bil</td><td>{@link Keys#INT16}</td></tr> </table>
     *
     * @param mimeType the mime type who's data type is returned.
     * @return the data type for the specified mime type.
     * @throws IllegalArgumentException if the mime type is null or malformed.
     */
    public static String makeDataTypeForMimeType(String mimeType) {

        if (!mimeType.contains("/") || mimeType.charAt(mimeType.length() - 1) == '/') {
            String message = Logging.getMessage("generic.InvalidImageFormat");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return switch (mimeType) {
            case "application/bil32" -> Keys.FLOAT32;
            case "application/bil16", "image/bil", "application/bil" -> Keys.INT16;
            default -> null;
        };
    }

    public static Object getFileOrResourceAsStream(String path, Class c) {

        File file = new File(path);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            }
            catch (Exception e) {
                return e;
            }
        }

        if (c == null)
            c = WWIO.class;

        try {
            return c.getResourceAsStream('/' + path);
        }
        catch (RuntimeException e) {
            return e;
        }
    }

    /**
     * Creates an {@link InputStream} for the contents of a {@link String}. The method creates a copy of the string's
     * contents and passes a steam reference to that copy.
     *
     * @param string the string to create a stream for, encoded in UTF-8.
     * @return an {@link InputStream} for the string's contents.
     * @throws IllegalArgumentException if <code>string</code> is null.
     */
    public static InputStream getInputStreamFromString(String string) {
        return WWIO.getInputStreamFromString(string, WWIO.DEFAULT_CHARACTER_ENCODING);
    }

    /**
     * Creates an {@link InputStream} for the contents of a {@link String}. The method creates a copy of the string's
     * contents and passes a steam reference to that copy.
     *
     * @param string   the string to create a stream for.
     * @param encoding the character encoding of the string. UTF-8 is used if null.
     * @return an {@link InputStream} for the string's contents.
     * @throws IllegalArgumentException if <code>string</code> is null.
     */
    public static InputStream getInputStreamFromString(String string, String encoding) {

        try {
            return new ByteArrayInputStream(
                string.getBytes(encoding != null ? encoding : WWIO.DEFAULT_CHARACTER_ENCODING));
        }
        catch (UnsupportedEncodingException e) {
            throw new WWRuntimeException(e); // should never happen because encoding is always UTF-8.
        }
    }

    /**
     * Creates an {@link InputStream} for the contents of a {@link ByteBuffer}. The method creates a copy of the
     * buffer's contents and passes a steam reference to that copy.
     *
     * @param buffer the buffer to create a stream for.
     * @return an {@link InputStream} for the buffer's contents.
     * @throws IllegalArgumentException if <code>buffer</code> is null.
     */
    public static InputStream getInputStreamFromByteBuffer(ByteBuffer buffer) {

        return new ByteBufferInputStream(buffer);
    }

    /**
     * Returns a new {@link BufferedInputStream} which wraps the specified InputStream. If the specified InputStream is
     * already a BufferedInputStream, this returns the original InputStream cast to a BufferedInputStream.
     *
     * @param is the InputStream to wrap with a new BufferedInputStream.
     * @return a new BufferedInputStream which wraps the specified InputStream.
     */
    public static BufferedInputStream getBufferedInputStream(InputStream is) {

        return (is instanceof BufferedInputStream && BufferedInputStream.class.equals(is.getClass()))
            ? (BufferedInputStream) is : new BufferedInputStream(is);
    }

    public static void copyFile(File source, File destination) throws IOException {

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel fic, foc;

        try {
            fis = new FileInputStream(source);
            fic = fis.getChannel();

            fos = new FileOutputStream(destination);
            foc = fos.getChannel();

            foc.transferFrom(fic, 0, fic.size());
            fos.flush();

            fis.close();
            fos.close();
        }
        finally {
            WWIO.closeStream(fis, source.getPath());
            WWIO.closeStream(fos, destination.getPath());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteDirectory(File file) {

        File[] fileList = file.listFiles();
        if (fileList != null) {
            Collection<File> childFiles = new ArrayList<>();
            Collection<File> childDirs = new ArrayList<>();
            for (File child : fileList) {
                if (child == null)
                    continue;

                if (child.isDirectory())
                    childDirs.add(child);
                else
                    childFiles.add(child);
            }

            for (File childFile : childFiles) {
                childFile.delete();
            }

            for (File childDir : childDirs) {
                WWIO.deleteDirectory(childDir);
            }
        }
    }

    /**
     * Close a stream and catch any {@link IOException} generated in the process. This supports any object that
     * implements the {@link Closeable} interface.
     *
     * @param stream the stream to close. If null, this method does nothing.
     * @param name   the name of the stream to place in the log message if an exception is encountered.
     */
    public static void closeStream(Object stream, String name) {
        if (stream == null)
            return;

        try {
            if (stream instanceof Closeable) {
                ((Closeable) stream).close();
            } else {
                String message = Logging.getMessage("WWIO.StreamTypeNotSupported", name != null ? name : "Unknown");
                Logging.logger().warning(message);
            }
        }
        catch (IOException e) {
            String message = Logging.getMessage("generic.ExceptionClosingStream", e, name != null ? name : "Unknown");
            Logging.logger().severe(message);
        }
    }

    /**
     * Open and read a text file into {@link String}.
     *
     * @param file a {@link File} reference to the file to open and read.
     * @return a {@link String} containing the contents of the file.
     * @throws IllegalArgumentException if the file is null.
     */
    public static String readTextFile(File file) {
        try {
            return Files.readString(file.toPath());
        }
        catch (IOException e) {
            String msg = Logging.getMessage("generic.ExceptionAttemptingToReadFile", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, msg);
            return null;
        }
    }

    /**
     * Save a {@link String} to a text file.
     *
     * @param text the {@link String} to write to the file.
     * @param file a {@link File} reference to the file to create.
     * @throws IllegalArgumentException if the text string or file is null.
     */
    public static void writeTextFile(String text, File file) {

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(text);
        }
        catch (IOException e) {
            String msg = Logging.getMessage("generic.ExceptionAttemptingToWriteTo", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, msg);
        }
        finally {
            WWIO.closeStream(writer, file.getPath());
        }
    }

    /**
     * Opens a file located via an absolute path or a path relative to the classpath.
     *
     * @param fileName the path of the file to open, either absolute or relative to the classpath.
     * @param c        the class that will be used to find a path relative to the classpath.
     * @return an {@link InputStream} to the open file
     * @throws IllegalArgumentException if the file name is null.
     * @throws WWRuntimeException       if an exception occurs or the file can't be found. The causing exception is
     *                                  available via this exception's {@link Throwable#initCause(Throwable)} method.
     */
    public static InputStream openFileOrResourceStream(String fileName, Class c) {

        Object streamOrException = WWIO.getFileOrResourceAsStream(fileName, c);

        if (streamOrException instanceof Exception) {
            String msg = Logging.getMessage("generic.CannotOpenFile", fileName);
            throw new WWRuntimeException(msg, (Throwable) streamOrException);
        }

        return (InputStream) streamOrException;
    }

    /**
     * Create a {@link String} of limited size from a {@link ByteBuffer}.
     *
     * @param buffer   the byte buffer to convert.
     * @param length   the maximum number of characters to read from the buffer. Must be greater than 0.
     * @param encoding the encoding do use. If null is specified then UTF-8 is used.
     * @return the string representation of the bytes in the buffer decoded according to the specified encoding.
     * @throws IllegalArgumentException                     if the buffer is null or the length is less than 1.
     * @throws java.nio.charset.IllegalCharsetNameException if the specified encoding name is illegal.
     * @throws java.nio.charset.UnsupportedCharsetException if no support for the named encoding is available.
     */
    public static String byteBufferToString(ByteBuffer buffer, int length, String encoding) {

        if (length < 1) {
            String msg = Logging.getMessage("generic.LengthIsInvalid", length);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        CharBuffer charBuffer = Charset.forName(encoding != null ? encoding : WWIO.DEFAULT_CHARACTER_ENCODING).decode(
            buffer);
        if (charBuffer.remaining() > length) {
            charBuffer = charBuffer.slice();
            charBuffer.limit(length);
        }

        return charBuffer.toString();
    }

    /**
     * Open an {@link InputStream} from a general source. The source type may be one of the following: <ul>
     * <li>{@link InputStream}</li> <li>{@link URL}</li> <li>absolute {@link URI}</li>
     * <li>{@link File}</li> <li>{@link String} containing a valid URL description or a file or resource name
     * available on the classpath.</li> </ul>
     *
     * @param src the input source of one of the above types.
     * @return an InputStream for the input source.
     * @throws IllegalArgumentException if the source is null, an empty string, or is not one of the above types.
     * @throws Exception                if the source cannot be opened for any reason.
     */
    public static InputStream openStream(Object src) throws Exception, IllegalArgumentException, IOException,
        MalformedURLException {
        if (WWUtil.isEmpty(src)) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (src instanceof InputStream) {
            return (InputStream) src;
        } else if (src instanceof URL) {
            return ((URL) src).openStream();
        } else if (src instanceof URI) {
            return ((URI) src).toURL().openStream();
        } else if (src instanceof File) {
            Object streamOrException = WWIO.getFileOrResourceAsStream(((File) src).getPath(), null);
            if (streamOrException instanceof Exception) {
                throw (Exception) streamOrException;
            }

            return (InputStream) streamOrException;
        } else if (!(src instanceof String)) {
            String message = Logging.getMessage("generic.UnrecognizedSourceType", src.toString());
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        String sourceName = (String) src;

        URL url = WWIO.makeURL(sourceName);
        if (url != null)
            return url.openStream();

        Object streamOrException = WWIO.getFileOrResourceAsStream(sourceName, null);
        if (streamOrException instanceof Exception) {
            throw (Exception) streamOrException;
        }

        return (InputStream) streamOrException;
    }

    /**
     * Returns the specified input source's abstract path, or null if the input source has no path. The input source may
     * be one of the following: <ul> <li>{@link String}</li> <li>{@link File}</li> <li>{@link URL}</li>
     * <li>{@link URI}</li> </ul>
     *
     * @param src the input source of one of the above types.
     * @return the input source's abstract path, or null if none exists.
     * @throws IllegalArgumentException if the source is null.
     */
    public static String getSourcePath(Object src) {

        String s = null;

        if (src instanceof File)
            s = ((File) src).getAbsolutePath();
        else if (src instanceof URL)
            s = ((URL) src).toExternalForm();
        else if (src instanceof URI)
            s = src.toString();
        else if (src instanceof String)
            s = (String) src;

        return s;
    }

    /**
     * Converts a string to a URL.
     *
     * @param path the string to convert to a URL.
     * @return a URL for the specified object, or null if a URL could not be created.
     * @see #makeURL(Object)
     * @see #makeURL(Object, String)
     */
    public static URL makeURL(String path) {
        try {
            return new URL(path);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a URL from an object.
     *
     * @param path the object from which to create a URL, typically a string.
     * @return a URL for the specified object, or null if a URL could not be created.
     * @see #makeURL(String)
     * @see #makeURL(Object, String)
     */
    public static URL makeURL(Object path) {
        try {
            URI uri = WWIO.makeURI(path);

            return uri != null ? uri.toURL() : null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a URL from an object. If the object does not already convert directly to a URL, a URL with a specified
     * protocol is created.
     *
     * @param path            the object from which to create a URL, typically a string.
     * @param defaultProtocol if non-null, a protocol to use if the specified path does not yet include a protocol.
     * @return a URL for the specified object, or null if a URL could not be created.
     * @see #makeURL(String)
     * @see #makeURL(Object)
     */
    public static URL makeURL(Object path, String defaultProtocol) {
        try {
            URL url = WWIO.makeURL(path);

            final String pathString = path.toString();
            if (url == null && !WWUtil.isEmpty(pathString) && !WWUtil.isEmpty(defaultProtocol))
                url = new URL(defaultProtocol, null, pathString);

            return url;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a URI from an object.
     *
     * @param path the object from which to create a URI, typically a string.
     * @return a URI for the specified object, or null if a URI could not be created.
     * @see #makeURL(String)
     * @see #makeURL(Object)
     * @see #makeURL(Object, String)
     */
    public static URI makeURI(Object path) {
        try {
            if (path instanceof String)
                return new URI((String) path);
            else if (path instanceof File)
                return ((File) path).toURI();
            else if (path instanceof URL)
                return ((URL) path).toURI();
            else
                return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns an array of strings naming the files and directories in the directory denoted by the specified file, that
     * satisfy the specified filter. If the filter is null, then all files and directories are accepted. This returns
     * null if the specified file is not a directory.
     *
     * @param file   the directory who's contents to list.
     * @param filter a file filter.
     * @return an array of file names denoting the files and directories in the directory denoted by the specified file,
     * or null if the specified file is not a directory.
     * @throws IllegalArgumentException if the file is null.
     */
    public static String[] listChildFilenames(File file, FileFilter filter) {
        // List the file and directory names in the specified file. If the returned array is null, then the specified
        // file does not denote a directory.
        String[] names = file.list();
        if (names == null)
            return null;

        ArrayList<String> matches = new ArrayList<>();

        // Collect the non-null pathnames which match the specified filter.
        for (String filename : names) {
            // Ignore null or empty filenames.
            if (filename == null || filename.isEmpty())
                continue;

            // If the filter is null, then all pathnames are accepted.
            if (filter != null && !filter.accept(new File(file, filename)))
                continue;

            matches.add(filename);
        }

        return matches.toArray(new String[0]);
    }

    /**
     * Returns an array of relative file paths naming the files and directories in the directory tree rooted by the
     * specified file, that satisfy the specified filter. If the parameter <code>recurseAfterMatch</code> is false, then
     * this ignores any directory branches beneath a matched file. This has the effect of listing the top matches in the
     * directory tree. The returned paths are relative to the specified file. If the filter is null, then all files and
     * directories are accepted. This returns null if the specified file is not a directory.
     *
     * @param file              the directory tree who's contents to list.
     * @param filter            a file filter.
     * @param recurseAfterMatch true to list the contents of directory branches beneath a match; false to ignore
     *                          branches beneath a match.
     * @return an array of relative file paths naming the files and directories in the directory tree rooted by the
     * specified file, or null if the specified file is not a directory.
     * @throws IllegalArgumentException if the file is null.
     */
    public static String[] listDescendantFilenames(File file, FileFilter filter, boolean recurseAfterMatch) {
//        if (file == null) {
//            String msg = Logging.getMessage("nullValue.FileIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }

        // List the file and directory names in the specified file. If the returned array is null, then the specified
        // file does not denote a directory.
        if (file.list() == null)
            return null;

        ArrayList<String> matches = new ArrayList<>();
        WWIO.listDescendantFilenames(file, null, filter, recurseAfterMatch, matches);

        return matches.toArray(new String[0]);
    }

    protected static void listDescendantFilenames(File parent, String pathname, FileFilter filter,
        boolean recurseAfterMatch, Collection<String> matches) {
        // Create a file pointing to the file denoted by the parent file and child pathname string. Use the parent file
        // if the pathname string is null. 
        File file = (pathname != null) ? new File(parent, pathname) : parent;

        // List the file and directory names in the specified file. Exit if the returned filename array is null,
        // indicating that the specified file does not denote a directory.
        String[] names = file.list();
        if (names == null)
            return;

        boolean haveMatch = false;

        // Collect the non-null pathnames which match the specified filter, and collect the non-null directory names
        // in a temporary list.
        for (String filename : names) {
            // Ignore null or empty filenames.
            if (filename == null || filename.isEmpty())
                continue;

            // If the filter is null, then all pathnames are accepted.
            if (filter != null && !filter.accept(new File(file, filename)))
                continue;

            matches.add(WWIO.appendPathPart(pathname, filename));
            haveMatch = true;
        }

        // Exit if any of the file or directories in the specified file match the file filter, and the caller has
        // specified to stop recursing after a match.
        if (haveMatch && !recurseAfterMatch)
            return;

        // Recursively process the contents of each path . 
        for (String filename : names) {
            WWIO.listDescendantFilenames(parent, WWIO.appendPathPart(pathname, filename), filter, recurseAfterMatch,
                matches);
        }
    }

    /**
     * Determines whether a jar URL is a reference to a local jar file or an entry in a local jar file. See {@link
     * JarURLConnection} for a description of jar URLs.
     *
     * @param jarUrl the jar URL, in the form jar:[url]!{entry}. (Omit [, ], { and } in the actual URL
     * @return true if the URL refers to a local resource, otherwise false.
     */
    public static boolean isLocalJarAddress(URL jarUrl) {
        return jarUrl != null && jarUrl.getFile().startsWith("file:");
    }
}