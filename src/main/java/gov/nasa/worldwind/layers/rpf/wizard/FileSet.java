/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.rpf.wizard;

import gov.nasa.worldwind.util.wizard.WizardProperties;

import java.io.File;
import java.util.Collection;

/**
 * @author dcollins
 * @version $Id: FileSet.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class FileSet extends WizardProperties {
    public static final String IDENTIFIER = "fileSet.Identifier";
    public static final String FILES = "fileSet.Files";
    public static final String TITLE = "fileSet.Title";
    public static final String SELECTED = "fileSet.Selected";

    public FileSet() {
    }

    public String getIdentifier() {
        return getStringProperty(FileSet.IDENTIFIER);
    }

    public void setIdentifier(String identifier) {
        setProperty(FileSet.IDENTIFIER, identifier);
    }

    @SuppressWarnings("unchecked")
    public Collection<File> getFiles() {
        Object value = getProperty(FileSet.FILES);
        return (value instanceof Collection) ? (Collection<File>) value : null;
    }

    public void setFiles(Collection<File> files) {
        setProperty(FileSet.FILES, files);
    }

    public int getFileCount() {
        Collection<File> files = getFiles();
        return files != null ? files.size() : 0;
    }

    public String getTitle() {
        return getStringProperty(FileSet.TITLE);
    }

    public void setTitle(String title) {
        setProperty(FileSet.TITLE, title);
    }

    public boolean isSelected() {
        Boolean b = getBooleanProperty(FileSet.SELECTED);
        return b != null ? b : false;
    }

    public void setSelected(boolean b) {
        setProperty(FileSet.SELECTED, b);
    }
}
