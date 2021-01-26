/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.coords.UTMCoord;

/**
 * @author Patrick Murris
 * @version $Id: StatusBarUTM.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class StatusBarUTM extends StatusBar {

    protected void handleCursorPositionChange(PositionEvent event) {
        Position newPos = event.getPosition();
        if (newPos != null) {
            String las = String.format("%7.4f\u00B0 %7.4f\u00B0", newPos.getLat().degrees,
                newPos.getLon().degrees);
            String els = makeCursorElevationDescription(
                getEventSource().model().globe().elevation(newPos.getLat(), newPos.getLon()));
            String los = "";
            try {
                UTMCoord UTM = UTMCoord.fromLatLon(newPos.getLat(), newPos.getLon(),
                    getEventSource().model().globe());
                los = UTM.toString();
            }
            catch (RuntimeException e) {
                los = "";
            }
            latDisplay.setText(las);
            lonDisplay.setText(los);
            eleDisplay.setText(els);
        } else {
            latDisplay.setText("");
            lonDisplay.setText("Off globe");
            eleDisplay.setText("");
        }
    }
}