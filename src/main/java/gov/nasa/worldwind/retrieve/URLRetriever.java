/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.util.*;
import jcog.WTF;
import org.apache.http.HttpEntity;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.*;
import java.util.zip.*;

/**
 * @author Tom Gaskins
 * @version $Id: URLRetriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class URLRetriever extends WWObjectImpl implements Retriever {
    /**
     * Applications never need to use this constant. It provides compatibility with very old WorldWind tile servers that
     * deliver zipped content without identifying the content type as other than application/zip. In these cases, the
     * object requesting the content must know the content type to expect, and also requires that the zip file be opened
     * and only its first entry returned.
     */
    public static final String EXTRACT_ZIP_ENTRY = "URLRetriever.ExtractZipEntry";
    private static final Pattern maxAge = Pattern.compile("max-age=(\\d+)");
    protected final AtomicLong expiration = new AtomicLong(0);
    protected final URL url;
    protected final Function<Retriever, ByteBuffer> postProcessor;
    protected volatile String state = Retriever.RETRIEVER_STATE_NOT_STARTED;
    protected volatile String contentType;
    protected volatile ByteBuffer byteBuffer;
    @Deprecated protected volatile URLConnection connection;
    protected int connectTimeout = Configuration.getIntegerValue(Keys.URL_CONNECT_TIMEOUT, 15000);
    protected int readTimeout = Configuration.getIntegerValue(Keys.URL_READ_TIMEOUT, 5000);
    protected int staleRequestLimit = -1;
    protected long submitEpoch;

    /**
     * @param url           the URL of the resource to retrieve.
     * @param postProcessor the retrieval post-processor to invoke when the resource is retrieved. May be null.
     * @throws IllegalArgumentException if <code>url</code>.
     */
    public URLRetriever(URL url, Function<Retriever, ByteBuffer> postProcessor) {

        this.url = url;
        this.postProcessor = postProcessor;
    }

    /**
     * Create the appropriate retriever for a URL's protocol.
     *
     * @param url           the url that will be the source of the retrieval.
     * @param postProcessor the retriever's post-processor.
     * @return a retriever for the protocol specified in the url, or null if no retriever exists for the protocol.
     * @throws IllegalArgumentException if the url is null.
     */
    public static URLRetriever createRetriever(URL url, Function<Retriever, ByteBuffer> postProcessor) {
        return switch (url.getProtocol().toLowerCase()) {
            case "http", "https" -> new HTTPRetriever(url, postProcessor);
            case "jar" -> new JarRetriever(url, postProcessor);
            default -> null;
        };
    }

    protected static void configureSSLContext(HttpsURLConnection connection) {
        SSLContext sslContext = (SSLContext) WorldWind.getValue(Keys.HTTP_SSL_CONTEXT);

        if (sslContext != null)
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
    }

    protected static ByteBuffer readStream(InputStream inputStream) throws IOException {
        return ByteBuffer.wrap(inputStream.readAllBytes());
    }

    /**
     * @param inputStream a stream to the zip connection.
     * @param url         the URL of the zip resource.
     * @return a buffer containing the content read from the zip stream.
     * @throws IOException              if the stream does not refer to a zip resource or an exception occurs during
     *                                  reading.
     * @throws IllegalArgumentException if <code>inputStream</code> is null
     */
    protected static ByteBuffer readZipStream(InputStream inputStream, URL url) throws IOException {
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry ze = zis.getNextEntry();
//        if (ze == null) {
//            Logging.logger().severe(Logging.getMessage("URLRetriever.NoZipEntryFor") + url);
//            return null;
//        }

//        if (ze.getSize() > 0) {
        return ByteBuffer.wrap(zis.readNBytes((int) ze.getSize()));
//
//
//            byte[] inputBuffer = new byte[8192];
//            while (buffer.hasRemaining()) {
//                int count = zis.read(inputBuffer);
//                if (count > 0) {
//                    buffer.put(inputBuffer, 0, count);
////                    this.contentLengthRead.getAndAdd(buffer.position() + 1);
//                }
//            }
//        }

    }

    /**
     * Indicates the expiration time specified by either the Expires header or the max-age directive of the
     * Cache-Control header. If both are present, then Cache-Control is given priority (See section 14.9.3 of the HTTP
     * Specification: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html).
     * <p>
     * If both the Expires and Date headers are present then the expiration time is calculated as current time +
     * (expires - date). This helps guard against clock skew between the client and server.
     *
     * @param connection Connection for which to get expiration time.
     * @return The expiration time, in milliseconds since the Epoch, specified by the HTTP headers, or zero if there is
     * no expiration time.
     */
    protected static long getExpiration(URLConnection connection) {
        // Read the expiration time from either the Cache-Control header or the Expires header. Cache-Control has
        // priority if both headers are specified.
        String cacheControl = connection.getHeaderField("cache-control");
        long nowMS = System.currentTimeMillis();
        if (cacheControl != null) {

            Matcher matcher = URLRetriever.maxAge.matcher(cacheControl);
            if (matcher.find()) {
                Long maxAgeSec = WWUtil.makeLong(matcher.group(1));
                if (maxAgeSec != null)
                    return maxAgeSec * 1000 + nowMS;
            }
        }

        // If the Cache-Control header is not present, or does not contain max-age, then look for the Expires header.
        // If the Date header is also present then compute the expiration time based on the server reported response
        // time. This helps guard against clock skew between client and server.
        long expiration = connection.getExpiration();
        long date = connection.getDate();

        if (date > 0 && expiration > date)
            return nowMS + (expiration - date);

        return expiration;
    }

    public final URL getUrl() {
        return url;
    }

    public final String getContentType() {
        return this.contentType;
    }

    /**
     * {@inheritDoc} Expiration time is determined by either the Expires header, or the max-age directive of the
     * Cache-Control header. Cache-Control has priority if both headers are specified (see section 14.9.3 of the <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html" target="_blank">HTTP Specification</a>).
     */
    public long getExpirationTime() {
        return this.expiration.get();
    }

    public final ByteBuffer getBuffer() {
        return this.byteBuffer;
    }

    public final String getName() {
        return this.url.toString();
    }

    public final URL getURL() {
        return this.url;
    }

    public final String getState() {
        return this.state;
    }

    protected void setState(String state) {
        String oldState = this.state;
        emit(Keys.RETRIEVER_STATE, oldState, this.state = state);
    }

    public final Function<Retriever, ByteBuffer> getPostProcessor() {
        return postProcessor;
    }

    public final int getConnectTimeout() {
        return connectTimeout;
    }

    public final void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getStaleRequestLimit() {
        return staleRequestLimit;
    }

    public void setStaleRequestLimit(int staleRequestLimit) {
        this.staleRequestLimit = staleRequestLimit;
    }

    public long getSubmitEpoch() {
        return submitEpoch;
    }

    public void setSubmitEpoch(long e) {
        this.submitEpoch = e;
    }

    private static final Set<String> REMOTE_PROTOCOLS = Set.of("http", "https", "ftp" /*?*/);

    @Override public final URLRetriever call() {

        if (WorldWind.getNetworkStatus().isHostUnavailable(url)) {
            setState(Retriever.RETRIEVER_STATE_NOT_STARTED);
            return this;
        }
        if (!REMOTE_PROTOCOLS.contains(url.getProtocol()))
            throw new WTF();
        try {

            WWIO.get(url.toString(), (response)->{
                if ((this.byteBuffer = this.read(url, response)) == null)
                    throw new IOException("empty");

                setState(Retriever.RETRIEVER_STATE_SUCCESSFUL); //before postProcessor
                WorldWind.getNetworkStatus().logAvailableHost(this.url);

                if (this.postProcessor != null)
                    this.byteBuffer = this.postProcessor.apply(this);

            });
        } catch (Exception e) {
            setState(Retriever.RETRIEVER_STATE_ERROR);
            WorldWind.getNetworkStatus().logUnavailableHost(this.url);
        }

        return this;
    }

//    private static final Logger logger = Log.log(URLRetriever.class);

    protected boolean interrupted() {
        if (Thread.currentThread().isInterrupted()) {
            setState(Retriever.RETRIEVER_STATE_INTERRUPTED);
            Logging.logger().fine(Logging.getMessage("URLRetriever.RetrievalInterruptedFor", this.url.toString()));
            return true;
        }
        return false;
    }

    protected final URLConnection openConnection() throws IOException {
        this.connection = connection();

        if (this.connection == null) // java.net.URL docs imply that this won't happen. We check anyway.
        {
            String message = Logging.getMessage("URLRetriever.NullReturnedFromOpenConnection", this.url);
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (this.connection instanceof HttpsURLConnection)
            URLRetriever.configureSSLContext((HttpsURLConnection) this.connection);

        final int cTimeOut = this.connectTimeout;
        final int rTimeOut = this.readTimeout;
        if (cTimeOut == 0 || rTimeOut == 0)
            throw new IllegalStateException("unspecified timeout");

        this.connection.setConnectTimeout(cTimeOut);
        this.connection.setReadTimeout(rTimeOut);

        return connection;
    }

    protected URLConnection connection() throws IOException {
        Proxy proxy = WWIO.configureProxy();
        return proxy != null ? this.url.openConnection(proxy) : this.url.openConnection();
    }

    /**
     *
     * @param url
     * @param connection the connection to read from.
     * @return a buffer containing the content read from the connection
     * @throws Exception                if <code>connection</code> is null or an exception occurs during reading.
     * @throws IllegalArgumentException if <code>connection</code> is null
     */
    public ByteBuffer read(URL url, HttpEntity connection) throws Exception {

        assert(this.connection == null);

        try (InputStream inputStream = connection.getContent()) {
//        if (inputStream == null) {
//            Logging.logger().log(Level.SEVERE, "URLRetriever.InputStreamFromConnectionNull", connection.getURL());
//            return null;
//        }

            //            this.contentLength = this.connection.getContentLength();
            //this.expiration.set(URLRetriever.getExpiration(connection));

            // The legacy WW servers send data with application/zip as the content type, and the retrieval initiator is
            // expected to know what type the unzipped content is. This is a kludge, but we have to deal with it. So
            // automatically unzip the content if the content type is application/zip.
            this.contentType = connection.getContentType().getValue();
            if (this.contentType != null && this.contentType.equalsIgnoreCase("application/zip")
                && !WWUtil.isEmpty(this.get(URLRetriever.EXTRACT_ZIP_ENTRY)))
                // Assume single file in zip and decompress it
                return URLRetriever.readZipStream(inputStream, url);
            else
                return URLRetriever.readStream(inputStream);
        }
    }

    /**
     * @param connection the connection to read from.
     * @return a buffer containing the content read from the connection
     * @throws Exception                if <code>connection</code> is null or an exception occurs during reading.
     * @throws IllegalArgumentException if <code>connection</code> is null
     */
    @Deprecated protected ByteBuffer doRead(URLConnection connection) throws Exception {

        try (InputStream inputStream = this.connection.getInputStream()) {

            this.expiration.set(URLRetriever.getExpiration(connection));

            // The legacy WW servers send data with application/zip as the content type, and the retrieval initiator is
            // expected to know what type the unzipped content is. This is a kludge, but we have to deal with it. So
            // automatically unzip the content if the content type is application/zip.
            this.contentType = connection.getContentType();
            if (this.contentType != null && this.contentType.equalsIgnoreCase("application/zip")
                && !WWUtil.isEmpty(this.get(URLRetriever.EXTRACT_ZIP_ENTRY)))
                // Assume single file in zip and decompress it
                return URLRetriever.readZipStream(inputStream, connection.getURL());
            else
                return URLRetriever.readStream(inputStream);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final URLRetriever that = (URLRetriever) o;
        //if (hash != that.hash) return false;

        // Retrievers are considered identical if they are for the same URL. This convention is used by the
        // retrieval service to filter out duplicate retrieval requests.
        return !(url != null ? !url.toString().equals(that.url.toString()) : that.url != null);
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return this.getName() != null ? this.getName() : super.toString();
    }
}