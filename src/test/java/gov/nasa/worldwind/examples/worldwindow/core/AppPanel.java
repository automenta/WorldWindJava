/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.core;

import javax.swing.*;

/**
 * @author tag
 * @version $Id: AppPanel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface AppPanel extends WWOPanel, Initializable {
    JPanel getJPanel();
}
