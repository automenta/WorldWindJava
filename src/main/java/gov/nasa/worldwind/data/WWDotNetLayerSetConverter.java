/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.formats.dds.DDSCompressor;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: WWDotNetLayerSetConverter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WWDotNetLayerSetConverter extends AbstractDataStoreProducer {
    protected static final String DEFAULT_IMAGE_FORMAT = "image/png";
    protected static final String DEFAULT_TEXTURE_FORMAT = "image/dds";

    public WWDotNetLayerSetConverter() {
    }

    private static File makeWWJavaDirectory(File dir, String dirname) {
        return new File(dir, WWIO.stripLeadingZeros(dirname));
    }

    private static File makeWWJavaFile(File dir, String filename, String installMimeType) {
        // If the filename does not match the standard pattern, then return a file with that name.
        String[] tokens = filename.split("[._]");
        if (tokens.length < 3 || tokens[0].length() < 1 || tokens[1].length() < 1)
            return new File(dir, filename);

        // If an installation type is specified, override the file extension with the new type.
        if (installMimeType != null)
            tokens[2] = WWIO.makeSuffixForMimeType(installMimeType);
            // Otherwise keep the existing extension. Add a leading '.' so that both cases can be handled transparently.
        else if (tokens[2].length() > 1)
            tokens[2] = "." + tokens[2];

        // If the filename is "000n_000m.foo", then the contents of tokens[] are:
        // tokens[0] = "000n"
        // tokens[1] = "000m"
        // tokens[2] = "foo"
        StringBuilder sb = new StringBuilder();
        sb.append(WWIO.stripLeadingZeros(tokens[0])).append("_").append(WWIO.stripLeadingZeros(tokens[1]));
        sb.append(tokens[2]);
        return new File(dir, sb.toString());
    }

    private static boolean isWWDotNetDirectory(File file) {
        String pattern = "\\d+";
        return file.getName().matches(pattern);
    }

    private static boolean isWWDotNetFile(File file) {
        String pattern = "\\d+[_]\\d+[.]\\w+";
        return file.getName().matches(pattern);
    }

    public String getDataSourceDescription() {
        return Logging.getMessage("WWDotNetLayerSetConverter.Description");
    }

    public void removeProductionState() {
        Iterable<SourceInfo> dataSources = this.getDataSourceList();
        AVList params = this.getStoreParameters();

        for (SourceInfo info : dataSources) {
            this.removeLayerSet(info.source, params);
        }
    }

    protected void doStartProduction(AVList parameters) {
        this.getProductionResultsList().clear();
        Iterable<SourceInfo> dataSources = this.getDataSourceList();
        ProductionState productionState = new ProductionState();

        // Initialize any missing production parameters with suitable defaults.
        WWDotNetLayerSetConverter.initProductionParameters(parameters, productionState);
        // Set the progress parameters for the current data sources.
        WWDotNetLayerSetConverter.setProgressParameters(dataSources, productionState);

        if (this.isStopped())
            return;

        for (SourceInfo info : dataSources) {
            if (this.isStopped())
                return;

            productionState.curSource++;
            this.convertLayerSet(info.source, productionState);
        }
    }

    protected static void initProductionParameters(AVList params, ProductionState productionState) {
        // Preserve backward compatibility with previous verisons of WWDotNetLayerSetConverter. If the caller specified
        // a format suffix parameter, use it to compute the image format properties. This gives priority to the format
        // suffix property to ensure applications which use format suffix continue to work.
        if (params.get(AVKey.FORMAT_SUFFIX) != null) {
            String s = WWIO.makeMimeTypeForSuffix(params.get(AVKey.FORMAT_SUFFIX).toString());
            if (s != null) {
                params.set(AVKey.IMAGE_FORMAT, s);
                params.set(AVKey.AVAILABLE_IMAGE_FORMATS, new String[] {s});
            }
        }

        // Use the default image format if none exists.
        if (params.get(AVKey.IMAGE_FORMAT) == null)
            params.set(AVKey.IMAGE_FORMAT, DEFAULT_IMAGE_FORMAT);

        // Compute the available image formats if none exists.
        if (params.get(AVKey.AVAILABLE_IMAGE_FORMATS) == null) {
            params.set(AVKey.AVAILABLE_IMAGE_FORMATS,
                new String[] {params.get(AVKey.IMAGE_FORMAT).toString()});
        }

        // Compute the format suffix if none exists.
        if (params.get(AVKey.FORMAT_SUFFIX) == null) {
            params.set(AVKey.FORMAT_SUFFIX,
                WWIO.makeSuffixForMimeType(params.get(AVKey.IMAGE_FORMAT).toString()));
        }

        productionState.productionParams = params;
    }

    //**************************************************************//
    //********************  LayerSet Installation  *****************//
    //**************************************************************//

    protected String validateProductionParameters(AVList parameters) {
        StringBuilder sb = new StringBuilder();

        Object o = parameters.get(AVKey.FILE_STORE_LOCATION);
        if (!(o instanceof String) || ((CharSequence) o).length() < 1)
            sb.append((!sb.isEmpty() ? ", " : "")).append(Logging.getMessage("term.fileStoreLocation"));

        o = parameters.get(AVKey.DATA_CACHE_NAME);
        // It's okay if the cache path is empty, but if specified it must be a String.
        if (o != null && !(o instanceof String))
            sb.append((!sb.isEmpty() ? ", " : "")).append(Logging.getMessage("term.fileStoreFolder"));

        if (sb.isEmpty())
            return null;

        return Logging.getMessage("DataStoreProducer.InvalidDataStoreParamters", sb.toString());
    }

    protected String validateDataSource(Object source, AVList params) {
        File file = WWDotNetLayerSetConverter.getSourceConfigFile(source);
        if (file == null)
            return Logging.getMessage("WWDotNetLayerSetConverter.NoSourceLocation");

        // Open the document in question as an XML event stream. Since we're only interested in testing the document
        // element, we avoiding any unecessary overhead incurred from parsing the entire document as a DOM.
        XMLEventReader eventReader = null;
        try {
            eventReader = WWXML.openEventReader(file);
            if (eventReader == null)
                return Logging.getMessage("WWDotNetLayerSetConverter.CannotReadLayerSetConfigFile", file);

            // Get the first start element event, if any exists, then determine if it represents a LayerSet
            // configuration document.
            XMLEvent event = WWXML.nextStartElementEvent(eventReader);
            if (event == null || !DataConfigurationUtils.isWWDotNetLayerSetConfigEvent(event))
                return Logging.getMessage("WWDotNetLayerSetConverter.FileNotLayerSet", file);
        }
        catch (Exception e) {
            Logging.logger().fine(Logging.getMessage("generic.ExceptionAttemptingToParseXml", file));

            return Logging.getMessage("WWDotNetLayerSetConverter.CannotReadLayerSetConfigFile", file);
        }
        finally {
            WWXML.closeEventReader(eventReader, file.getPath());
        }

        // Return null, indicating the DataSource is a valid LayerSet configuration document.
        return null;
    }

    protected static Document readLayerSetDocument(Object source) {
        Document doc = null;
        try {
            doc = WWXML.openDocument(source);
        }
        catch (Exception e) {
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseXml", source);
            Logging.logger().fine(message);
        }

        if (doc == null) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.CannotReadLayerSetConfigFile", source);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        if (!DataConfigurationUtils.isWWDotNetLayerSetConfigDocument(doc.getDocumentElement())) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.FileNotLayerSet", source);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        return doc;
    }

    protected void convertLayerSet(Object source, ProductionState productionState) {
        File sourceConfigFile = WWDotNetLayerSetConverter.getSourceConfigFile(source);
        if (sourceConfigFile == null) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.NoSourceLocation");
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        File sourceDataFile = sourceConfigFile.getParentFile();
        if (sourceDataFile == null) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.FileWithoutParent", sourceConfigFile);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        File destConfigFile = WWDotNetLayerSetConverter.getDestConfigFile(productionState.productionParams);
        if (destConfigFile == null) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.NoInstallLocation", sourceConfigFile);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        File destDataFile = destConfigFile.getParentFile();
        if (destDataFile == null) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.FileWithoutParent", destConfigFile);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        if (WWIO.isAncestorOf(sourceDataFile, destDataFile) || WWIO.isAncestorOf(destDataFile, sourceDataFile)) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.CannotInstallToSelf", sourceDataFile,
                destDataFile);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        if (this.isStopped())
            return;

        Document sourceConfigDoc = WWDotNetLayerSetConverter.readLayerSetDocument(sourceConfigFile);

        try {
            String imageFormat = productionState.productionParams.getStringValue(AVKey.IMAGE_FORMAT);
            productionState.numSourceFiles[productionState.curSource] = WWDotNetLayerSetConverter.countWWDotNetFiles(sourceDataFile);

            this.copyWWDotNetDiretory(sourceDataFile, destDataFile, imageFormat, productionState);
        }
        catch (Exception e) {
            // Back out all file system changes made so far.
            WWIO.deleteDirectory(destDataFile);

            String message = Logging.getMessage("WWDotNetLayerSetConverter.CannotInstallLayerSet", sourceConfigFile);
            Logging.logger().log(Level.SEVERE, message, e);
            throw new WWRuntimeException(message);
        }

        if (this.isStopped())
            return;

        Document destConfigDoc;
        try {
            destConfigDoc = WWDotNetLayerSetConverter.createDestConfigDoc(sourceConfigDoc, productionState.productionParams);
            WWXML.saveDocumentToFile(destConfigDoc, destConfigFile.getAbsolutePath());
        }
        catch (Exception e) {
            // Back out all file system changes made so far.
            WWIO.deleteDirectory(destDataFile);
            //noinspection ResultOfMethodCallIgnored
            destConfigFile.delete();

            String message = Logging.getMessage("WWDotNetLayerSetConverter.CannotWriteLayerConfigFile", destConfigFile);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        if (this.isStopped())
            return;

        this.getProductionResultsList().add(destConfigDoc);
    }

    //**************************************************************//
    //********************  Imagery Installation  ******************//
    //**************************************************************//

    protected static File getSourceConfigFile(Object source) {
        if (source instanceof File) {
            return (File) source;
        }
        else if (source instanceof String && !WWUtil.isEmpty(source)) {
            return new File((String) source);
        }

        return null;
    }

    protected static File getDestConfigFile(AVList installParams) {
        String fileStoreLocation = installParams.getStringValue(AVKey.FILE_STORE_LOCATION);
        if (fileStoreLocation != null)
            fileStoreLocation = WWIO.stripTrailingSeparator(fileStoreLocation);

        if (WWUtil.isEmpty(fileStoreLocation))
            return null;

        String cacheName = DataConfigurationUtils.getDataConfigFilename(installParams, ".xml");
        if (cacheName != null)
            cacheName = WWIO.stripLeadingSeparator(cacheName);

        if (WWUtil.isEmpty(cacheName))
            return null;

        return new File(fileStoreLocation + File.separator + cacheName);
    }

    protected static Document createDestConfigDoc(Document layerSetDoc, AVList installParams) {
        AVList params = new AVListImpl();

        // Extract configuration parameters from the LayerSet document.
        DataConfigurationUtils.getWWDotNetLayerSetConfigParams(layerSetDoc.getDocumentElement(), params);

        // Override the LayerSet's display name with the name used by the converter.
        if (installParams.get(AVKey.DISPLAY_NAME) != null)
            params.set(AVKey.DISPLAY_NAME, installParams.get(AVKey.DISPLAY_NAME));

        // Override the LayerSet's cache name with the cache name used by the converter.
        if (installParams.get(AVKey.DATA_CACHE_NAME) != null)
            params.set(AVKey.DATA_CACHE_NAME, installParams.get(AVKey.DATA_CACHE_NAME));

        // Override the LayerSet's image format and available image format parameters with values used by the converter.
        if (installParams.get(AVKey.IMAGE_FORMAT) != null)
            params.set(AVKey.IMAGE_FORMAT, installParams.get(AVKey.IMAGE_FORMAT));
        if (installParams.get(AVKey.AVAILABLE_IMAGE_FORMATS) != null)
            params.set(AVKey.AVAILABLE_IMAGE_FORMATS, installParams.get(AVKey.AVAILABLE_IMAGE_FORMATS));

        // Override the LayerSet's format suffix with the suffix used by the converter.
        if (installParams.get(AVKey.FORMAT_SUFFIX) != null)
            params.set(AVKey.FORMAT_SUFFIX, installParams.get(AVKey.FORMAT_SUFFIX));

        // Set the texture format to DDS. If the texture data is already in DDS format, this parameter is benign.
        params.set(AVKey.TEXTURE_FORMAT, DEFAULT_TEXTURE_FORMAT);

        return BasicTiledImageLayer.createTiledImageLayerConfigDocument(params);
    }

    private void copyWWDotNetDiretory(File source, File destination, String installMimeType,
        ProductionState productionState) throws IOException {
        if (this.isStopped())
            return;

        if (!destination.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destination.mkdirs();
        }

        if (!destination.exists()) {
            String message = Logging.getMessage("generic.CannotCreateFile", destination);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        File[] fileList = source.listFiles();
        if (fileList == null)
            return;

        Collection<File> childFiles = new ArrayList<>();
        Collection<File> childDirs = new ArrayList<>();
        for (File child : fileList) {
            if (child == null) // Don't allow null subfiles.
                continue;

            if (child.isHidden()) // Ignore hidden files.
                continue;

            if (child.isDirectory())
                childDirs.add(child);
            else
                childFiles.add(child);
        }

        for (File childFile : childFiles) {
            if (this.isStopped())
                break;

            if (!isWWDotNetFile(childFile))
                continue;

            File destFile = makeWWJavaFile(destination, childFile.getName(), installMimeType);
            this.installWWDotNetFile(childFile, destFile, productionState);

            if (!destFile.exists()) {
                String message = Logging.getMessage("generic.CannotCreateFile", destFile);
                Logging.logger().severe(message);
                throw new IOException(message);
            }
        }

        for (File childDir : childDirs) {
            if (this.isStopped())
                break;

            if (!isWWDotNetDirectory(childDir))
                continue;

            File destDir = makeWWJavaDirectory(destination, childDir.getName());
            this.copyWWDotNetDiretory(childDir, destDir, installMimeType, productionState);
        }
    }

    private void installWWDotNetFile(File source, File destination, ProductionState productionState)
        throws IOException {
        // Bypass file installation if:
        // (a) destination is newer than source, and
        // (b) source and destination have identical size.
        if (destination.exists() && source.lastModified() >= destination.lastModified()
            && source.length() == destination.length()) {
            return;
        }

        String sourceSuffix = WWIO.getSuffix(source.getName());
        String destinationSuffix = WWIO.getSuffix(destination.getName());

        // Source and destination types match. Copy the source file directly.
        if (sourceSuffix.equalsIgnoreCase(destinationSuffix)) {
            WWIO.copyFile(source, destination);
        }
        // Destination type is different. Convert the source file and write the converstion to the destionation.
        else {
            if (destinationSuffix.equalsIgnoreCase("dds")) {
                ByteBuffer sourceBuffer = DDSCompressor.compressImageFile(source);
                WWIO.saveBuffer(sourceBuffer, destination);
            }
            else {
                BufferedImage sourceImage = ImageIO.read(source);
                ImageIO.write(sourceImage, destinationSuffix, destination);
            }
        }

        this.updateProgress(productionState);
    }

    private static int countWWDotNetFiles(File source) {
        int count = 0;

        File[] fileList = source.listFiles();
        if (fileList == null)
            return count;

        Collection<File> childFiles = new ArrayList<>();
        Collection<File> childDirs = new ArrayList<>();
        for (File child : fileList) {
            if (child == null) // Don't allow null subfiles.
                continue;

            if (child.isHidden()) // Ignore hidden files.
                continue;

            if (child.isDirectory())
                childDirs.add(child);
            else
                childFiles.add(child);
        }

        for (File childFile : childFiles) {
            if (!isWWDotNetFile(childFile))
                continue;

            count++;
        }

        for (File childDir : childDirs) {
            if (!isWWDotNetDirectory(childDir))
                continue;

            count += countWWDotNetFiles(childDir);
        }

        return count;
    }

    //**************************************************************//
    //********************  Progress and Verification  *************//
    //**************************************************************//

    protected static void setProgressParameters(Iterable<?> dataSources, ProductionState productionState) {
        int numSources = 0;
        //noinspection UnusedDeclaration
        for (Object o : dataSources) {
            numSources++;
        }

        productionState.numSources = numSources;
        productionState.curSource = -1;
        productionState.numSourceFiles = new int[numSources];
        productionState.numInstalledFiles = new int[numSources];
    }

    //**************************************************************//
    //********************  Progress Parameters  *******************//
    //**************************************************************//

    private void updateProgress(ProductionState productionState) {
        double oldProgress = WWDotNetLayerSetConverter.computeProgress(productionState);
        productionState.numInstalledFiles[productionState.curSource]++;
        double newProgress = WWDotNetLayerSetConverter.computeProgress(productionState);

        this.firePropertyChange(AVKey.PROGRESS, oldProgress, newProgress);
    }

    private static double computeProgress(ProductionState productionState) {
        double progress = 0.0;
        for (int i = 0; i <= productionState.curSource; i++) {
            progress += (productionState.numInstalledFiles[i] /
                (double) productionState.numSourceFiles[i]) * (1.0 / productionState.numSources);
        }
        return progress;
    }

    protected void removeLayerSet(Object source, AVList params) {
        File sourceConfigFile = WWDotNetLayerSetConverter.getSourceConfigFile(source);
        if (sourceConfigFile == null) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.NoSourceLocation");
            Logging.logger().warning(message);
            return;
        }

        File destConfigFile = WWDotNetLayerSetConverter.getDestConfigFile(params);
        if (destConfigFile == null) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.NoInstallLocation", sourceConfigFile);
            Logging.logger().warning(message);
            return;
        }

        File destDataFile = destConfigFile.getParentFile();
        if (destDataFile == null) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.FileWithoutParent", destConfigFile);
            Logging.logger().warning(message);
            return;
        }

        try {
            WWIO.deleteDirectory(destDataFile);
        }
        catch (Exception e) {
            String message = Logging.getMessage("WWDotNetLayerSetConverter.ExceptionRemovingProductionState",
                sourceConfigFile);
            Logging.logger().log(Level.SEVERE, message, e);
        }
    }

    //**************************************************************//
    //********************  LayerSet Removal  **********************//
    //**************************************************************//

    protected static class ProductionState {
        // Production parameters.
        AVList productionParams;
        // Progress counters.
        int numSources;
        int curSource;
        int[] numSourceFiles;
        int[] numInstalledFiles;
    }
}
