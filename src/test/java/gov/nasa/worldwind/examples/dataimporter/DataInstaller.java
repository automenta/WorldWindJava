/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.dataimporter;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles all the work necessary to install tiled image layers and elevation models.
 *
 * @author tag
 * @version $Id: DataInstaller.java 2982 2015-04-06 19:52:46Z tgaskins $
 */
public class DataInstaller extends AVListImpl {
    public static final String IMAGERY = "Imagery";
    public static final String ELEVATION = "Elevation";
    public static final String INSTALL_COMPLETE = "gov.nasa.worldwind.dataimport.DataInstaller.InstallComplete";
    public static final String PREVIEW_LAYER = "gov.nasa.worldwind.dataimport.DataInstaller.PreviewLayer";

    public static void addToWorldWindow(WorldWindow wwd, Element domElement, AVList dataSet, boolean goTo) {
        String type = DataConfigurationUtils.getDataConfigType(domElement);
        if (type == null)
            return;

        if (type.equalsIgnoreCase("Layer")) {
            addLayerToWorldWindow(wwd, domElement, dataSet, goTo);
        }
        else if (type.equalsIgnoreCase("ElevationModel")) {
            addElevationModelToWorldWindow(wwd, domElement, dataSet, true);
        }
    }

    public static void addLayerToWorldWindow(final WorldWindow wwd, Element domElement, final AVList dataSet,
        final boolean goTo) {
        Layer layer = null;
        try {
            Factory factory = (Factory) WorldWind.createConfigurationComponent(AVKey.LAYER_FACTORY);
            layer = (Layer) factory.createFromConfigSource(domElement, null);

            Sector sector = WWXML.getSector(domElement, "Sector", null);
            layer.set(AVKey.SECTOR, sector);
            dataSet.set(AVKey.DISPLAY_NAME, layer.name());
        }
        catch (Exception e) {
            String message = Logging.getMessage("generic.CreationFromConfigurationFailed",
                DataConfigurationUtils.getDataConfigDisplayName(domElement));
            Logging.logger().log(Level.SEVERE, message, e);
        }

        if (layer == null)
            return;

        final Layer finalLayer = layer;
        SwingUtilities.invokeLater(() -> {
            finalLayer.setEnabled(true); // BasicLayerFactory creates layer which is initially disabled

            Layer existingLayer = findLayer(wwd, dataSet.getStringValue(AVKey.DISPLAY_NAME));
            if (existingLayer != null)
                wwd.model().getLayers().remove(existingLayer);

            removeLayerPreview(wwd, dataSet);

            WorldWindow.insertBeforePlacenames(wwd, finalLayer);

            final Sector sector = (Sector) finalLayer.get(AVKey.SECTOR);
            if (goTo && sector != null && !sector.equals(Sector.FULL_SPHERE)) {
                ExampleUtil.goTo(wwd, sector);
            }
        });
    }

    protected static void removeLayerPreview(WorldWindow wwd, AVList dataSet) {
        AVList layer = (AVList) dataSet.get(AVKey.LAYER);
        if (layer == null || layer.get(PREVIEW_LAYER) == null)
            return;

        if (!(layer instanceof RenderableLayer))
            return;

        SurfaceImage surfaceImage = null;
        RenderableLayer renderableLayer = (RenderableLayer) layer;
        for (Renderable renderable : renderableLayer.all()) {
            if (renderable instanceof SurfaceImage) {
                surfaceImage = (SurfaceImage) renderable;
                break;
            }
        }

        if (surfaceImage != null)
            renderableLayer.remove(surfaceImage);
//
//        wwd.getModel().getLayers().remove(layer);
    }

    public static void addElevationModelToWorldWindow(final WorldWindow wwd, Element domElement, final AVList dataSet,
        final boolean goTo) {
        ElevationModel elevationModel = null;
        try {
            Factory factory = (Factory) WorldWind.createConfigurationComponent(AVKey.ELEVATION_MODEL_FACTORY);
            elevationModel = (ElevationModel) factory.createFromConfigSource(domElement, null);
//            elevationModel.setValue(AVKey.DATASET_NAME, dataSet.getStringValue(AVKey.DATASET_NAME));
            dataSet.set(AVKey.DISPLAY_NAME, elevationModel.name());

            // TODO: set Sector as in addLayerToWorldWindow?
        }
        catch (Exception e) {
            String message = Logging.getMessage("generic.CreationFromConfigurationFailed",
                DataConfigurationUtils.getDataConfigDisplayName(domElement));
            Logging.logger().log(Level.SEVERE, message, e);
        }

        if (elevationModel == null)
            return;

        final ElevationModel em = elevationModel;
        SwingUtilities.invokeLater(() -> {
            ElevationModel existingElevationModel = findElevationModel(wwd, dataSet.getStringValue(AVKey.DISPLAY_NAME));
            if (existingElevationModel != null)
                removeElevationModel(wwd, existingElevationModel);

            ElevationModel defaultElevationModel = wwd.model().getGlobe().getElevationModel();
            if (defaultElevationModel instanceof CompoundElevationModel) {
                if (!((CompoundElevationModel) defaultElevationModel).containsElevationModel(em)) {
                    ((CompoundElevationModel) defaultElevationModel).addElevationModel(em);
                }
            }
            else {
                CompoundElevationModel cm = new CompoundElevationModel();
                cm.addElevationModel(defaultElevationModel);
                cm.addElevationModel(em);
                wwd.model().getGlobe().setElevationModel(cm);
            }

            Sector sector = (Sector) em.get(AVKey.SECTOR);
            if (goTo && sector != null && !sector.equals(Sector.FULL_SPHERE)) {
                ExampleUtil.goTo(wwd, sector);
            }

            wwd.firePropertyChange(new PropertyChangeEvent(wwd, AVKey.ELEVATION_MODEL, null, em));
        });
    }

    public static DataRasterReaderFactory getReaderFactory() {
        try {
            return (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
                AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
        }
        catch (Exception e) {
            return new BasicDataRasterReaderFactory();
        }
    }

    public static Layer findLayer(WorldWindow wwd, String layerName) {
        for (Layer layer : wwd.model().getLayers()) {
            String dataSetName = layer.getStringValue(AVKey.DISPLAY_NAME);
            if (dataSetName != null && dataSetName.equals(layerName))
                return layer;
        }

        return null;
    }

    public static ElevationModel findElevationModel(WorldWindow wwd, String elevationModelName) {
        ElevationModel defaultElevationModel = wwd.model().getGlobe().getElevationModel();
        if (defaultElevationModel instanceof CompoundElevationModel) {
            CompoundElevationModel cm = (CompoundElevationModel) defaultElevationModel;
            for (ElevationModel em : cm.getElevationModels()) {
                String name = em.getStringValue(AVKey.DISPLAY_NAME);
                if (name != null && name.equals(elevationModelName))
                    return em;
            }
        }
        else {
            String name = defaultElevationModel.getStringValue(AVKey.DISPLAY_NAME);
            if (name != null && name.equals(elevationModelName))
                return defaultElevationModel;
        }

        return null;
    }

    public static void removeElevationModel(WorldWindow wwd, ElevationModel elevationModel) {
        ElevationModel defaultElevationModel = wwd.model().getGlobe().getElevationModel();
        if (defaultElevationModel instanceof CompoundElevationModel) {
            CompoundElevationModel cm = (CompoundElevationModel) defaultElevationModel;
            for (ElevationModel em : cm.getElevationModels()) {
                String name = em.getStringValue(AVKey.DISPLAY_NAME);
                if (name != null && name.equals(elevationModel.name())) {
                    cm.removeElevationModel(elevationModel);
                    wwd.firePropertyChange(new PropertyChangeEvent(wwd, AVKey.ELEVATION_MODEL, null, elevationModel));
                }
            }
        }
    }

    public Document installDataFromFiles(Component parentComponent, FileSet fileSet) throws Exception {
        // Create a DataStoreProducer that is capable of processing the file.
        final DataStoreProducer producer = createDataStoreProducerFromFiles(fileSet);

        File installLocation = DataInstaller.getDefaultInstallLocation(WorldWind.store());
        if (installLocation == null) {
            String message = Logging.getMessage("generic.NoDefaultImportLocation");
            Logging.logger().severe(message);
            return null;
        }

        String datasetName = askForDatasetName(suggestDatasetName(fileSet));

        DataInstallerProgressMonitor progressMonitor = new DataInstallerProgressMonitor(parentComponent, producer);
        Document doc = null;
        try {
            // Install the file into the specified FileStore.
            progressMonitor.start();
            doc = createDataStore(fileSet, installLocation, datasetName, producer);

            // The user clicked the ProgressMonitor's "Cancel" button. Revert any change made during production,
            // and
            // discard the returned DataConfiguration reference.
            if (progressMonitor.isCanceled()) {
                doc = null;
                producer.removeProductionState();
            }
        }
        finally {
            progressMonitor.stop();
        }

        return doc;
    }

    protected DataStoreProducer createDataStoreProducerFromFiles(FileSet fileSet) throws IllegalArgumentException {
        if (fileSet == null || fileSet.getLength() == 0) {
            String message = Logging.getMessage("nullValue.ArrayIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String commonPixelFormat = this.determineCommonPixelFormat(fileSet);

        if (AVKey.IMAGE.equals(commonPixelFormat)) {
            return new TiledImageProducer();
        }
        else if (AVKey.ELEVATION.equals(commonPixelFormat)) {
            return new TiledElevationProducer();
        }

        String message = Logging.getMessage("generic.UnexpectedRasterType", commonPixelFormat);
        Logging.logger().severe(message);
        throw new IllegalArgumentException(message);
    }

    protected String determineCommonPixelFormat(FileSet fileSet) throws IllegalArgumentException {
        if (fileSet == null || fileSet.getLength() == 0) {
            String message = Logging.getMessage("nullValue.ArrayIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String commonPixelFormat = null;

        for (File file : fileSet.getFiles()) {
            AVList params = new AVListImpl();
            if (DataInstaller.isDataRaster(file, params)) {
                String pixelFormat = params.getStringValue(AVKey.PIXEL_FORMAT);
                if (WWUtil.isEmpty(commonPixelFormat)) {
                    if (WWUtil.isEmpty(pixelFormat)) {
                        String message = Logging.getMessage("generic.UnrecognizedSourceType",
                            file.getAbsolutePath());
                        Logging.logger().severe(message);
                        throw new IllegalArgumentException(message);
                    }
                    else {
                        commonPixelFormat = pixelFormat;
                    }
                }
                else if (!commonPixelFormat.equals(pixelFormat)) {
                    if (WWUtil.isEmpty(pixelFormat)) {
                        String message = Logging.getMessage("generic.UnrecognizedSourceType",
                            file.getAbsolutePath());
                        Logging.logger().severe(message);
                        throw new IllegalArgumentException(message);
                    }
                    else {
                        String reason = Logging.getMessage("generic.UnexpectedRasterType", pixelFormat);
                        String details = file.getAbsolutePath() + ": " + reason;
                        String message = Logging.getMessage("DataRaster.IncompatibleRaster", details);
                        Logging.logger().severe(message);
                        throw new IllegalArgumentException(message);
                    }
                }
            }
        }

        return commonPixelFormat;
    }

    protected static Document createDataStore(FileSet fileSet, File installLocation, String datasetName,
        DataStoreProducer producer) throws Exception {
        // Create the production parameters. These parameters instruct the DataStoreProducer where to install the
        // cached data, and what name to put in the data configuration document.
        AVList params = new AVListImpl();

        params.set(AVKey.DATASET_NAME, datasetName);
        params.set(AVKey.DATA_CACHE_NAME, datasetName);
        params.set(AVKey.FILE_STORE_LOCATION, installLocation.getAbsolutePath());

        // These parameters define producer's behavior:
        // create a full tile cache OR generate only first two low resolution levels
        boolean enableFullPyramid = Configuration.getBooleanValue(AVKey.PRODUCER_ENABLE_FULL_PYRAMID, false);
        if (!enableFullPyramid) {
            params.set(AVKey.SERVICE_NAME, AVKey.SERVICE_NAME_LOCAL_RASTER_SERVER);
            // retrieve the value of the AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL, default to 1 level if missing
            String maxLevel = Configuration.getStringValue(AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL, "0");
            params.set(AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL, maxLevel);
        }
        else {
            params.set(AVKey.PRODUCER_ENABLE_FULL_PYRAMID, true);
        }

        producer.setStoreParameters(params);

        try {
            for (File file : fileSet.getFiles()) {
                producer.offerDataSource(file, null);
                Thread.yield();
            }

            // Convert the file to a form usable by WorldWind components,
            // according to the specified DataStoreProducer.
            // This throws an exception if production fails for any reason.
            producer.startProduction();
        }
        catch (InterruptedException ie) {
            producer.removeProductionState();
            Thread.interrupted();
            throw ie;
        }
        catch (Exception e) {
            // Exception attempting to convert the file. Revert any change made during production.
            producer.removeProductionState();
            throw e;
        }

        // Return the DataConfiguration from the production results. Since production successfully completed, the
        // DataStoreProducer should contain a DataConfiguration in the production results. We test the production
        // results anyway.
        Iterable results = producer.getProductionResults();
        if (results != null) {
            results.iterator();
            if (results.iterator().hasNext()) {
                Object o = results.iterator().next();
                if (o instanceof Document) {
                    return (Document) o;
                }
            }
        }

        return null;
    }

    protected static String askForDatasetName(String suggestedName) {

        for (; ; ) {
            Object o = JOptionPane.showInputDialog(null, "Name:", "Enter dataset name",
                JOptionPane.QUESTION_MESSAGE, null, null, suggestedName);

            if (!(o instanceof String)) // user canceled the input
            {
                Thread.interrupted();

                String msg = Logging.getMessage("generic.OperationCancelled", "Import");
                Logging.logger().info(msg);
                throw new WWRuntimeException(msg);
            }

            return WWIO.replaceIllegalFileNameCharacters((String) o);
        }
    }

    protected static String suggestDatasetName(FileSet fileSet) {
        if (null == fileSet || fileSet.getLength() == 0) {
            return null;
        }

        if (fileSet.getName() != null)
            return fileSet.getScale() != null ? fileSet.getName() + " " + fileSet.getScale() : fileSet.getName();

        // extract file and folder names that all files have in common
        StringBuilder sb = new StringBuilder();
        for (File file : fileSet.getFiles()) {
            String name = file.getAbsolutePath();
            if (WWUtil.isEmpty(name)) {
                continue;
            }

            name = WWIO.replaceIllegalFileNameCharacters(WWIO.replaceSuffix(name, ""));

            if (sb.isEmpty()) {
                sb.append(name);
            }
            else {
                int size = Math.min(name.length(), sb.length());
                for (int i = 0; i < size; i++) {
                    if (name.charAt(i) != sb.charAt(i)) {
                        sb.setLength(i);
                        break;
                    }
                }
            }
        }

        String name = sb.toString();
        sb.setLength(0);

        List<String> words = new ArrayList<>();

        StringTokenizer tokens = new StringTokenizer(name, " _:/\\-=!@#$%^&()[]{}|\".,<>;`+");
        String lastWord = null;
        while (tokens.hasMoreTokens()) {
            String word = tokens.nextToken();
            // discard empty, one-char long, and duplicated keys
            if (WWUtil.isEmpty(word) || word.length() < 2 || word.equalsIgnoreCase(lastWord)) {
                continue;
            }

            lastWord = word;

            words.add(word);
            if (words.size() > 4)  // let's keep only last four words
            {
                words.remove(0);
            }
        }

        if (!words.isEmpty()) {
            sb.setLength(0);
            for (String word : words) {
                sb.append(word).append(' ');
            }
            sb.append(fileSet.isImagery() ? " Imagery" : fileSet.isElevation() ? " Elevations" : "");
            return sb.toString().trim();
        }
        else {
            return (WWUtil.isEmpty(name)) ? "change me" : name;
        }
    }

    public static boolean isDataRaster(Object source, AVList params) {
        if (source == null) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DataRasterReaderFactory readerFactory;
        try {
            readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(
                AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
        }
        catch (Exception e) {
            readerFactory = new BasicDataRasterReaderFactory();
        }

        params = (null == params) ? new AVListImpl() : params;
        DataRasterReader reader = readerFactory.findReaderFor(source, params);
        if (reader == null) {
            return false;
        }

        if (!params.hasKey(AVKey.PIXEL_FORMAT)) {
            try {
                reader.readMetadata(source, params);
            }
            catch (Exception e) {
                String message = Logging.getMessage("generic.ExceptionWhileReading", e.getMessage());
                Logging.logger().finest(message);
            }
        }

        return AVKey.IMAGE.equals(params.getStringValue(AVKey.PIXEL_FORMAT))
            || AVKey.ELEVATION.equals(params.getStringValue(AVKey.PIXEL_FORMAT));
    }

    public static File getDefaultInstallLocation(FileStore fileStore) {
        if (fileStore == null) {
            String message = Logging.getMessage("nullValue.FileStoreIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (File location : fileStore.getLocations()) {
            if (fileStore.isInstallLocation(location.getPath())) {
                return location;
            }
        }

        return fileStore.getWriteLocation();
    }
}
