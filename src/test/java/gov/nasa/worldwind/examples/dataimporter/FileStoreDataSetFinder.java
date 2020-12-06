/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.dataimporter;

import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.util.*;

import java.io.File;
import java.util.*;

/**
 * Finds all the data sets within a filestore.
 *
 * @author tag
 * @version $Id: FileStoreDataSetFinder.java 1180 2013-02-15 18:40:47Z tgaskins $
 */
public class FileStoreDataSetFinder {
    public List<FileStoreDataSet> findDataSets(FileStore fileStore) {
        final List<FileStoreDataSet> dataSets = new ArrayList<>();

        for (File file : fileStore.getLocations()) {
            if (!file.exists())
                continue;

            if (!fileStore.isInstallLocation(file.getPath()))
                continue;

            dataSets.addAll(FileStoreDataSetFinder.findDataSets(file));
        }

        return dataSets;
    }

    protected static List<FileStoreDataSet> findDataSets(File cacheRoot) {
        if (cacheRoot == null) {
            String message = Logging.getMessage("nullValue.FileStorePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] configFilePaths = WWIO.listDescendantFilenames(cacheRoot, new DataConfigurationFilter(), false);
        if (configFilePaths == null || configFilePaths.length == 0)
            return Collections.emptyList();

        List<FileStoreDataSet> dataSets = new ArrayList<>();

        for (String configFilePath : configFilePaths) {
            File configFile = new File(configFilePath);
            dataSets.add(new FileStoreDataSet(cacheRoot.getPath(),
                cacheRoot.getPath() + File.separator + configFile.getParent(),
                cacheRoot.getPath() + File.separator + configFilePath));
        }

        return dataSets;
    }
}
