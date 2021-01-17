/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.util.logging.Level;

/**
 * Abstract base class for retrieval post-processors. Verifies the retrieval operation and dispatches the content to the
 * a subclasses content handlers.
 * <p>
 * Subclasses are expected to override the methods necessary to handle their particular post-processing operations.
 *
 * @author Tom Gaskins
 * @version $Id: AbstractRetrievalPostProcessor.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class AbstractRetrievalPostProcessor implements RetrievalPostProcessor {
    /**
     * Holds miscellaneous parameters examined by this and subclasses.
     */
    protected AVList avList;
    /**
     * The retriever associated with the post-processor. Only non-null after {@link #apply(Retriever)} is called.
     */
    protected Retriever retriever;

    /**
     * Create a default post-processor.
     */
    public AbstractRetrievalPostProcessor() {
    }

    /**
     * Create a post-processor and pass it attributes that can be examined during content handling.
     *
     * @param avList an attribute-value list with values that might be used during post-processing.
     */
    public AbstractRetrievalPostProcessor(AVList avList) {
        this.avList = avList;
    }

    protected static boolean isPrimaryContentType(String typeOfContent, String contentType) {
        if (WWUtil.isEmpty(contentType) || WWUtil.isEmpty(typeOfContent))
            return false;

        return contentType.trim().toLowerCase().startsWith(typeOfContent);
    }

    /**
     * Log the content of a buffer as a String. If the buffer is null or empty, nothing is logged. Only the first 2,048
     * characters of the buffer are included in the log message.
     *
     * @param buffer the content to log. The content is assumed to be of type "text".
     */
    protected static void logTextBuffer(ByteBuffer buffer) {
        if (buffer == null || !buffer.hasRemaining())
            return;

        Logging.logger().warning(WWIO.byteBufferToString(buffer, 2048, null));
    }

    /**
     * Runs the post-processor.
     *
     * @param retriever the retriever to associate with the post-processor.
     * @return a buffer containing the downloaded data, perhaps converted during content handling. null is returned if a
     * fatal problem occurred during post-processing.
     * @throws IllegalArgumentException if the retriever is null.
     */
    public ByteBuffer apply(Retriever retriever) {

        this.retriever = retriever;

        if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL)) {
            this.handleUnsuccessfulRetrieval();
            return null;
        } else
            return this.handleSuccessfulRetrieval();
    }

    /**
     * Returns the retriever associarted with this post-processor.
     *
     * @return the retriever associated with the post-processor, or null if no retriever is associated.
     */
    public Retriever getRetriever() {
        return this.retriever;
    }

    /**
     * Called when the retrieval state is other than {@link Retriever#RETRIEVER_STATE_SUCCESSFUL}. Can be overridden by
     * subclasses to handle special error cases. The default implementation calls {@link #markResourceAbsent()} if the
     * retrieval state is {@link Retriever#RETRIEVER_STATE_ERROR}.
     */
    protected void handleUnsuccessfulRetrieval() {
        if (this.getRetriever().getState().equals(Retriever.RETRIEVER_STATE_ERROR))
            this.markResourceAbsent();
    }

    /**
     * Process the retrieved data if it has been retrieved successfully.
     *
     * @return a buffer containing the downloaded data, perhaps converted during content handling.
     */
    protected ByteBuffer handleSuccessfulRetrieval() {
        try {
            return this.handleContent();
        } catch (Exception e) {
            this.handleContentException(e);
            return null;
        }
    }

    /**
     * Checks the retrieval response code.
     *
     * @return true if the response code is the OK value for the protocol, e.g., ({@link HttpURLConnection#HTTP_OK} for
     * HTTP protocol), otherwise false.
     */
    protected boolean validateResponseCode() {
        //noinspection SimplifiableIfStatement
        final Retriever r = this.getRetriever();
        if (r instanceof HTTPRetriever)
            return this.validateHTTPResponseCode();
        else if (r instanceof JarRetriever)
            return this.validateJarResponseCode();
        else
            return false;
    }

    /**
     * Checks the retrieval's HTTP response code. Must only be called when the retriever is a subclass of {@link
     * HTTPRetriever}.
     *
     * @return true if the response code is {@link HttpURLConnection#HTTP_OK}, otherwise false.
     */
    protected boolean validateHTTPResponseCode() {
        return ((HTTPRetriever) this.getRetriever()).getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    /**
     * Checks the retrieval's HTTP response code. Must only be called when the retriever is a subclass of {@link
     * HTTPRetriever}.
     *
     * @return true if the response code is {@link HttpURLConnection#HTTP_OK}, otherwise false.
     */
    protected boolean validateJarResponseCode() {
        return ((JarRetriever) this.getRetriever()).getResponseCode() == HttpURLConnection.HTTP_OK; // Re-using the HTTP response code for OK
    }

    /**
     * Marks the retrieval target absent. Subclasses should override this method if they keep track of absent-resources.
     * The default implementation does nothing.
     */
    protected void markResourceAbsent() {
    }

    protected boolean isWMSException() {
        String contentType = this.getRetriever().getContentType();

        if (WWUtil.isEmpty(contentType))
            return false;

        return contentType.trim().equalsIgnoreCase("application/vnd.ogc.se_xml");
    }

    /**
     * Process the retrieved data. Dispatches content handling to content-type specific handlers: {@link
     * #handleZipContent()} for content types containing "zip", {@link #handleTextContent()} for content types starting
     * with "text", and {@link #handleImageContent()} for contents types starting with "image".
     *
     * @return a buffer containing the retrieved data, which may have been transformed during content handling.
     * @throws IOException if an IO error occurs while processing the data.
     */
    public ByteBuffer handleContent() throws IOException {
        String contentType = this.getRetriever().getContentType();
        if (WWUtil.isEmpty(contentType)) {
            // Try to determine the content type from the URL's suffix, if any.
            String suffix = WWIO.getSuffix(this.getRetriever().getName().split(";")[0]);
            if (!WWUtil.isEmpty(suffix))
                contentType = WWIO.makeMimeTypeForSuffix(suffix);

            if (WWUtil.isEmpty(contentType)) {
                Logging.logger().severe(Logging.getMessage("nullValue.ContentTypeIsNullOrEmpty"));
                return null;
            }
        }
        contentType = contentType.trim().toLowerCase();

        if (this.isWMSException())
            return this.handleWMSExceptionContent();

        if (contentType.contains("zip"))
            return this.handleZipContent();

        if (AbstractRetrievalPostProcessor.isPrimaryContentType("text", contentType))
            return this.handleTextContent();

        if (AbstractRetrievalPostProcessor.isPrimaryContentType("image", contentType))
            return this.handleImageContent();

        if (AbstractRetrievalPostProcessor.isPrimaryContentType("application", contentType))
            return this.handleApplicationContent();

        return this.handleUnknownContentType();
    }

    /**
     * Reacts to exceptions occurring during content handling. Subclasses may override this method to perform special
     * exception handling. The default implementation logs a message specific to the exception.
     *
     * @param e the exception to handle.
     */
    protected void handleContentException(Exception e) {
        if (e instanceof ClosedByInterruptException) {
            Logging.logger().log(Level.FINE,
                Logging.getMessage("generic.OperationCancelled",
                    "retrieval post-processing for " + this.getRetriever().getName()), e);
        } else if (e instanceof IOException) {
            this.markResourceAbsent();
            Logging.logger().log(Level.SEVERE,
                Logging.getMessage("generic.ExceptionWhileSavingRetreivedData", this.getRetriever().getName()), e);
        }
    }

    /**
     * Handles content types that are not recognized by the content handler. Subclasses may override this method to
     * handle such cases. The default implementation logs an error message and returns null.
     *
     * @return null if no further processing should occur, otherwise the retrieved data, perhaps transformed.
     */
    protected ByteBuffer handleUnknownContentType() {
        Logging.logger().log(Level.WARNING,
            Logging.getMessage("generic.UnknownContentType", this.getRetriever().getContentType()));

        return null;
    }

    /**
     * Handles Text content. If the content type is text/xml, {@link #handleXMLContent()} is called. If the content type
     * is text/html, {@link #handleHTMLContent()} is called. For all other sub-types the content is logged as a message
     * with level {@link Level#SEVERE}.
     *
     * @return a buffer containing the retrieved text.
     * @throws IOException if an IO error occurs while processing the data.
     */
    protected ByteBuffer handleTextContent() throws IOException {
        String contentType = this.getRetriever().getContentType().trim().toLowerCase();

        if (contentType.contains("xml"))
            return this.handleXMLContent();

        if (contentType.contains("html"))
            return this.handleHTMLContent();

        AbstractRetrievalPostProcessor.logTextBuffer(this.getRetriever().getBuffer());

        return null;
    }

    /**
     * Handles XML content. The default implementation only calls {@link #logTextBuffer(ByteBuffer)} and returns.
     *
     * @return a buffer containing the retrieved XML.
     */
    protected ByteBuffer handleXMLContent() {
        AbstractRetrievalPostProcessor.logTextBuffer(this.getRetriever().getBuffer());

        return null;
    }

    /**
     * Handles HTML content. The default implementation only calls {@link #logTextBuffer(ByteBuffer)} and returns.
     *
     * @return a buffer containing the retrieved HTML.
     */
    protected ByteBuffer handleHTMLContent() {
        AbstractRetrievalPostProcessor.logTextBuffer(this.getRetriever().getBuffer());

        return null;
    }

    /**
     * Handles zipped content. The default implementation saves the data to the retriever's output file without
     * unzipping it.
     *
     * @return a buffer containing the retrieved data.
     */
    protected ByteBuffer handleZipContent() {

        return this.getRetriever().getBuffer();
    }

    /**
     * Handles application content. The default implementation saves the retrieved data without modification via {@link
     * #saveBuffer()} without.
     *
     * @return a buffer containing the retrieved data.
     */
    protected ByteBuffer handleApplicationContent() {
        return this.getRetriever().getBuffer();
    }

    /**
     * Handles WMS exceptions.
     *
     * @return a buffer containing the retrieved XML.
     */
    protected ByteBuffer handleWMSExceptionContent() {
        // TODO: Parse the xml and include only the message text in the log message.

        StringBuilder sb = new StringBuilder(this.getRetriever().getName());
        sb.append('\n');
        sb.append(WWIO.byteBufferToString(this.getRetriever().getBuffer(), 2048, null));
        Logging.logger().warning(sb.toString());

        return null;
    }

    /**
     * Handles image content. The default implementation simply saves the retrieved data via {@link #saveBuffer()},
     * first converting it to DDS if the suffix of the output file is .dds.
     * <p>
     * The default implementation of this method returns immediately if the output file cannot be determined or it
     * exists and {@link #overwriteExistingFile()} returns false.
     *
     * @return a buffer containing the retrieved data.
     */
    protected final ByteBuffer handleImageContent() {

        return this.getRetriever().getBuffer();
    }
}
