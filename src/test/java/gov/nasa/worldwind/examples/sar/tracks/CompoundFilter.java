/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar.tracks;

import gov.nasa.worldwind.util.Logging;

import java.io.*;

/**
 * @author dcollins
 * @version $Id: CompoundFilter.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class CompoundFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
    private final FileFilter[] filters;
    private final String description;

    public CompoundFilter(FileFilter[] filters, String description) {
        this.filters = new FileFilter[filters.length];
        System.arraycopy(filters, 0, this.filters, 0, filters.length);
        this.description = description;
    }

    public FileFilter[] getFilters() {
        FileFilter[] copy = new FileFilter[this.filters.length];
        System.arraycopy(this.filters, 0, copy, 0, this.filters.length);
        return copy;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean accept(File file) {
        if (file == null) {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (file.isDirectory())
            return true;

        for (FileFilter filter : this.filters) {
            if (filter.accept(file))
                return true;
        }

        return false;
    }
}
