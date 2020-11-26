/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar.tracks;

import gov.nasa.worldwind.formats.csv.CSVReader;
import gov.nasa.worldwind.tracks.Track;

import java.io.InputStream;

/**
 * @author dcollins
 * @version $Id: CSVTrackReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class CSVTrackReader extends AbstractTrackReader {
    public CSVTrackReader() {
    }

    public String getDescription() {
        return "Comma Separated Value (*.csv)";
    }

    protected Track[] doRead(InputStream inputStream) {
        CSVReader reader = new CSVReader();
        reader.readStream(inputStream, null); // un-named stream
        return AbstractTrackReader.asArray(reader.getTracks());
    }

    protected boolean acceptFilePath(String filePath) {
        return filePath.toLowerCase().endsWith(".csv");
    }
}
