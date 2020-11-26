/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar.tracks;

import gov.nasa.worldwind.formats.nmea.NmeaReader;
import gov.nasa.worldwind.tracks.Track;

import java.io.InputStream;

/**
 * @author dcollins
 * @version $Id: NMEATrackReader.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class NMEATrackReader extends AbstractTrackReader {
    public NMEATrackReader() {
    }

    public String getDescription() {
        return "National Marine Electronics Association (*.nmea)";
    }

    protected Track[] doRead(InputStream inputStream) {
        NmeaReader reader = new NmeaReader();
        reader.readStream(inputStream, null); // un-named stream
        return AbstractTrackReader.asArray(reader.getTracks());
    }

    protected boolean acceptFilePath(String filePath) {
        return filePath.toLowerCase().endsWith(".nmea");
    }
}
