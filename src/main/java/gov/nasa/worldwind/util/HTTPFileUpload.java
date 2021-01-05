/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

/**
 * @author Lado Garakanidze
 * @version $Id: HTTPFileUpload.java 1171 2013-02-11 21:45:02Z dcollins $
 */

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.WWRuntimeException;

import java.beans.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

/**
 * Synchronous file upload using HTTP POST as a multi-part form data
 *
 * @deprecated
 */
@Deprecated
public class HTTPFileUpload {

    protected static final String CR_LF = "\r\n";
    protected static final String TWO_HYPHENS = "--";
    protected static final String BOUNDARY = "*********NASA_World_Wind_HTTP_File_Upload_Separator**********";
    protected final URL url;
    protected final Collection<FileInfo> filesToUpload = new ArrayList<>();
    protected final AVList requestProperties = new AVListImpl();
    private final PropertyChangeSupport propertyChangeSupport;
    protected int maxBufferSize = 1024 * 1024; // default is 1M
    protected String requestMethod = "POST";
    protected long totalBytesToUpload;
    protected long totalBytesUploaded;
    protected int totalFilesUploaded;
    protected int totalFilesFailed;
    protected float lastProgress;

    public HTTPFileUpload(URL url) {
        if (url == null) {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.url = url;
        propertyChangeSupport = new PropertyChangeSupport(this);
        this.setRequestMethod("POST");
        this.setRequestProperty("Connection", "Keep-Alive");
        this.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + HTTPFileUpload.BOUNDARY);
        this.setRequestProperty("Content-Transfer-Encoding", "binary");
    }

    protected static void handleResponse(HttpURLConnection conn) throws IOException {
        if (null != conn) {
            int code = conn.getResponseCode();
            String message = conn.getResponseMessage();

            if (code != 200) {
                String reason = "(" + code + ") :" + message;
                throw new IOException(reason);
            }
        } else {
            throw new IOException(Logging.getMessage("nullValue.ConnectionIsNull"));
        }
    }

    protected static void disconnect(HttpURLConnection conn, String name) {
        if (null != conn) {
            try {
                conn.disconnect();
            }
            catch (RuntimeException e) {
                String message = Logging.getMessage("WWIO.ErrorTryingToClose", name);
                Logging.logger().log(Level.WARNING, message, e);
            }
        }
    }

    protected static void writeProperties(DataOutput dos, AVList params) throws IOException {
        if (null != dos && null != params) {
            for (Map.Entry<String, Object> param : params.getEntries()) {
                String name = param.getKey();
                String value = AVListImpl.getStringValue(params, name, "");
                HTTPFileUpload.writeContentDisposition(dos, name, value);
            }
        }
    }

    protected static void writeContentDisposition(DataOutput dos, String filename) throws IOException {
        if (null != dos) {
            dos.writeBytes(HTTPFileUpload.TWO_HYPHENS + HTTPFileUpload.BOUNDARY + HTTPFileUpload.CR_LF);
            dos.writeBytes("Content-Disposition: attachment; filename=\"" + filename + '"' + HTTPFileUpload.CR_LF);
            dos.writeBytes("Content-type: application/octet-stream" + HTTPFileUpload.CR_LF);
            dos.writeBytes(HTTPFileUpload.CR_LF);
        }
    }

    protected static void writeContentDisposition(DataOutput dos, String paramName, String paramValue)
        throws IOException {
        if (null != dos && null != paramName) {
            dos.writeBytes(HTTPFileUpload.TWO_HYPHENS + HTTPFileUpload.BOUNDARY + HTTPFileUpload.CR_LF);
            dos.writeBytes("Content-Disposition: form-data; name=\"" + paramName + '"' + HTTPFileUpload.CR_LF);
            dos.writeBytes(HTTPFileUpload.CR_LF + paramValue + HTTPFileUpload.CR_LF);
        }
    }

    protected static void writeContentSeparator(DataOutput dos) throws IOException {
        if (null != dos) {
            // send multipart form data necesssary after file data...
            dos.writeBytes(HTTPFileUpload.CR_LF + HTTPFileUpload.TWO_HYPHENS + HTTPFileUpload.BOUNDARY + HTTPFileUpload.TWO_HYPHENS + HTTPFileUpload.CR_LF);
        }
    }

    public long getTotalFilesToUpload() {
        return filesToUpload.size();
    }

    public long getTotalBytesToUpload() {
        return totalBytesToUpload;
    }

    public long getTotalBytesUploaded() {
        return totalBytesUploaded;
    }

    public int getTotalFilesUploaded() {
        return totalFilesUploaded;
    }

    public int getTotalFilesFailed() {
        return totalFilesFailed;
    }

    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    /**
     * Returns the HTTP request method - POST or GET
     *
     * @return POST or GET
     */
    public String getRequestMethod() {
        return this.requestMethod;
    }

    /**
     * Sets a HTTP request method - POST or GET
     *
     * @param method POST or GET
     */
    public void setRequestMethod(String method) {
        if ("POST".equalsIgnoreCase(method)) {
            this.requestMethod = "POST";
        } else if ("GET".equalsIgnoreCase(method)) {
            this.requestMethod = "GET";
        } else {
            String message = Logging.getMessage("generic.UnknownValueForKey", method, "method={POST|GET}");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
    }

    public void setRequestProperty(String name, String value) {
        if (WWUtil.isEmpty(name)) {
            String message = Logging.getMessage("nullValue.PropertyNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.requestProperties.set(name, value);
    }

    public void add(ByteBuffer bufferToUpload, String name, AVList params) {
        if (bufferToUpload == null) {
            String message = Logging.getMessage("nullValue.ByteBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (WWUtil.isEmpty(name)) {
            String message = Logging.getMessage("nullValue.NameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (bufferToUpload.limit() == 0) {
            String message = Logging.getMessage("generic.BufferIsEmpty");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.totalBytesToUpload += bufferToUpload.limit();
        this.filesToUpload.add(new FileInfo(name, bufferToUpload, params));
    }

    /**
     * Adds a file to the HTTP File Uploader.
     *
     * @param file   The file to upload, must exist
     * @param name   The desired name of the file
     * @param params AVList of parameters
     * @throws FileNotFoundException if the file was not found or does not exist
     */
    public void add(File file, String name, AVList params) throws FileNotFoundException {
        if (null != file && file.exists()) {
            this.totalBytesToUpload += file.length();
            this.filesToUpload.add(new FileInfo(name, file, params));
        } else {
            throw new FileNotFoundException((file != null) ? file.getName() : "");
        }
    }

    public void send() {
        for (FileInfo info : this.filesToUpload) {
            try {
                if (info.uploadItem instanceof File) {
                    send((File) info.uploadItem, info.uploadName, info.properties);
                } else if (info.uploadItem instanceof ByteBuffer) {
                    send((ByteBuffer) info.uploadItem, info.uploadName, info.properties);
                } else if (info.uploadItem instanceof String) {
                    send((String) info.uploadItem, info.uploadName, info.properties);
                }

                this.totalFilesUploaded++;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                this.totalFilesFailed++;

                String reason = WWUtil.extractExceptionReason(e);
                String message = Logging.getMessage("HTTP.FileUploadFailed", info.uploadName, reason);
                Logging.logger().log(Level.FINEST, message, e);

                throw new WWRuntimeException(message);
            }
        }
    }

    protected void send(File fileToUpload, String uploadName, AVList params)
        throws IOException, NullPointerException, FileNotFoundException, ProtocolException {
        if (null == fileToUpload || !fileToUpload.exists()) {
            throw new FileNotFoundException();
        }

        if (null == url) {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        HttpURLConnection conn = null;
        FileInputStream fis = null;
        DataOutputStream dos = null;

        int bytesRead, bytesAvailable, bufferSize;

        try {
            conn = (HttpURLConnection) this.url.openConnection();
            conn.setDoInput(true);  // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false);   // Don't use a cached copy.
            this.writeRequestProperties(conn);

            dos = new DataOutputStream(conn.getOutputStream());

            HTTPFileUpload.writeProperties(dos, params);

            HTTPFileUpload.writeContentDisposition(dos, uploadName);

            // create a buffer of maximum size
            fis = new FileInputStream(fileToUpload);
            bytesAvailable = fis.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = ByteBuffer.allocate(bufferSize).array();

            // read file and write it into form...
            bytesRead = fis.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dos.write(buffer, 0, bytesRead);

                this.totalBytesUploaded += bytesRead;
                this.notifyProgress();

                bytesAvailable = fis.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fis.read(buffer, 0, bufferSize);
            }

            HTTPFileUpload.writeContentSeparator(dos);
            dos.flush();

            HTTPFileUpload.handleResponse(conn);
        }
        finally {
            WWIO.closeStream(fis, null);
            WWIO.closeStream(dos, null);
            HTTPFileUpload.disconnect(conn, this.url.toString());
        }
    }

    protected void send(ByteBuffer bufferToUpload, String fileName, AVList params) throws IOException,
        ProtocolException {
        if (null == bufferToUpload) {
            String message = Logging.getMessage("nullValue.ByteBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (bufferToUpload.limit() == 0) {
            String message = Logging.getMessage("generic.BufferIsEmpty");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (null == url) {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (WWUtil.isEmpty(fileName)) {
            String message = Logging.getMessage("nullValue.FilenameIsNullOrEmpty");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        HttpURLConnection conn = null;
        DataOutputStream dos = null;

        try {
            conn = (HttpURLConnection) this.url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);   // Don't use a cached copy.
            this.writeRequestProperties(conn);

            dos = new DataOutputStream(conn.getOutputStream());

            HTTPFileUpload.writeProperties(dos, params);

            HTTPFileUpload.writeContentDisposition(dos, fileName);

            int bytesAvailable = bufferToUpload.rewind().remaining();

            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = ByteBuffer.allocate(bufferSize).array();

            // Send buffer to server
            bufferToUpload.rewind();
            while (bufferToUpload.hasRemaining()) {
                int bytesToRead = Math.min(bufferToUpload.remaining(), maxBufferSize);
                bufferToUpload.get(buffer, 0, bytesToRead);
                dos.write(buffer, 0, bytesToRead);

                this.totalBytesUploaded += bytesToRead;
                this.notifyProgress();
            }

            HTTPFileUpload.writeContentSeparator(dos);
            dos.flush();

            HTTPFileUpload.handleResponse(conn);
        }
        finally {
            WWIO.closeStream(dos, null);
            HTTPFileUpload.disconnect(conn, this.url.toString());
        }
    }

    protected void send(String stringToUpload, String fileName, AVList params) throws IOException, ProtocolException {
        if (WWUtil.isEmpty(stringToUpload)) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (null == url) {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (WWUtil.isEmpty(fileName)) {
            String message = Logging.getMessage("nullValue.FilenameIsNullOrEmpty");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        HttpURLConnection conn = null;
        DataOutputStream dos = null;

        try {
            conn = (HttpURLConnection) this.url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);   // Don't use a cached copy.
            this.writeRequestProperties(conn);

            dos = new DataOutputStream(conn.getOutputStream());

            HTTPFileUpload.writeProperties(dos, params);

            HTTPFileUpload.writeContentDisposition(dos, fileName);

            byte[] buffer = stringToUpload.getBytes(StandardCharsets.UTF_8);
            dos.write(buffer, 0, buffer.length);
            this.totalBytesUploaded += stringToUpload.length();
            this.notifyProgress();

            HTTPFileUpload.writeContentSeparator(dos);
            dos.flush();

            HTTPFileUpload.handleResponse(conn);
        }
        finally {
            WWIO.closeStream(dos, null);
            HTTPFileUpload.disconnect(conn, this.url.toString());
        }
    }

    /**
     * Writes HTTP request' properties (HTTP headers)
     *
     * @param conn HttpURLConnection connection
     * @throws IOException if there is any problem with a connection
     */
    protected void writeRequestProperties(HttpURLConnection conn) throws ProtocolException {
        if (null != conn) {
            conn.setRequestMethod(this.getRequestMethod());
            this.requestProperties.getEntries().forEach(
                (requestProperty) -> conn.setRequestProperty(requestProperty.getKey(),
                    (String) requestProperty.getValue()));
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void notifyProgress() {
        float progress = (float) 100 * this.totalBytesUploaded / this.totalBytesToUpload;

        if (progress != lastProgress) {
            this.propertyChangeSupport.firePropertyChange("progress", lastProgress, progress);
            lastProgress = progress;
        }
    }

    protected static class FileInfo {

        protected final String uploadName;
        protected final Object uploadItem;
        protected final AVList properties;

        public FileInfo(String name, Object item, AVList properties) {
            this.uploadName = name;
            this.uploadItem = item;
            this.properties = properties;
        }
    }
}
