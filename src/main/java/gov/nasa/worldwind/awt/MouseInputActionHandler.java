/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.awt;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * @author jym
 * @version $Id: MouseInputActionHandler.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface MouseInputActionHandler {
    boolean inputActionPerformed(KeyEventState keys, String target,
        ViewInputAttributes.ActionAttributes viewAction);

    boolean inputActionPerformed(AbstractViewInputHandler inputHandler,
        MouseEvent mouseEvent, ViewInputAttributes.ActionAttributes viewAction);

    boolean inputActionPerformed(AbstractViewInputHandler inputHandler,
        MouseWheelEvent mouseWheelEvent, ViewInputAttributes.ActionAttributes viewAction);
}
