/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.core;

/**
 * @author tag
 * @version $Id: WWMenu.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface WWMenu {
    void addMenu(String featureID);

    void addMenus(String[] featureIDs);
}
