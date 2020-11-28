/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import java.awt.*;

/**
 * @author tag
 * @version $Id: ScreenCredit.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface ScreenCredit extends Renderable {
    Rectangle getViewport();

    void setViewport(Rectangle viewport);

    double getOpacity();

    void setOpacity(double opacity);

    String getLink();

    void setLink(String link);

    void pick(DrawContext dc, Point pickPoint);
}
