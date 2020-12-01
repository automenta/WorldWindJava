/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.dataimporter;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.formats.rpf.RPFDataSeries;

/**
 * A file set specific to RPF data.
 *
 * @author tag
 * @version $Id: FileSetRPF.java 1180 2013-02-15 18:40:47Z tgaskins $
 */
public class FileSetRPF extends FileSet {
    FileSetRPF(String rpfSuffixCode) {
        this.assignRPFMetadata(rpfSuffixCode);
    }

    public void assignRPFMetadata(String rpfSuffixCode) {
        RPFDataSeries series = RPFDataSeries.dataSeriesFor(rpfSuffixCode);
        this.set(FileSet.FILE_SET_CODE, series.seriesCode);
        this.set(FileSet.FILE_SET_ABBREVIATION, series.seriesAbbreviation);
        this.set(FileSet.FILE_SET_SCALE, series.scaleOrResolution);
        this.set(FileSet.FILE_SET_GSD, series.scaleOrGSD);
        this.set(AVKey.DATASET_NAME, series.dataSeries);
        this.set(AVKey.DATASET_TYPE, series.rpfDataType);
        this.set(AVKey.DISPLAY_NAME, series.dataSeries);
    }
}
