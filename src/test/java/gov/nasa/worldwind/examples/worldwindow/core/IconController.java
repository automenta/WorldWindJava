/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.core;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.examples.worldwindow.features.AbstractFeature;
import gov.nasa.worldwind.examples.worldwindow.util.Util;
import gov.nasa.worldwind.render.WWIcon;

/**
 * @author tag
 * @version $Id: IconController.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class IconController extends AbstractFeature implements SelectListener, Disposable {
    protected WWIcon lastPickedIcon = null;

    public IconController(Registry registry) {
        super("Icon Controller", Constants.FEATURE_ICON_CONTROLLER, registry);
    }

    public void initialize(Controller controller) {
        super.initialize(controller);

        this.controller.getWWd().addSelectListener(this);
    }

    public void dispose() {
        this.controller.getWWd().removeSelectListener(this);
    }

    public void selected(SelectEvent event) {
        try {
            if (event.getEventAction().equals(SelectEvent.ROLLOVER))
                highlight(event, event.getTopObject());
            else if (event.getEventAction().equals(SelectEvent.RIGHT_PRESS))
                showContextMenu(event);
        }
        catch (Exception e) {
            // Wrap the handler in a try/catch to keep exceptions from bubbling up
            Util.getLogger().warning(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void highlight(SelectEvent event, Object o) {
        // Manage highlighting of icons.

        if (this.lastPickedIcon == o)
            return; // same thing selected

        // Turn off highlight if on.
        if (this.lastPickedIcon != null) {
            this.lastPickedIcon.setHighlighted(false);
            this.lastPickedIcon = null;
        }

        // Turn on highlight if object selected.
        if (o instanceof WWIcon) {
            this.lastPickedIcon = (WWIcon) o;
            this.lastPickedIcon.setHighlighted(true);
        }
    }

    protected void showContextMenu(SelectEvent event) {
    }
}
