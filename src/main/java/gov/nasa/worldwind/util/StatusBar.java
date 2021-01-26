/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author tag
 * @version $Id: StatusBar.java 1945 2014-04-18 17:08:43Z tgaskins $
 */
public class StatusBar extends JPanel implements PositionListener, RenderingListener {
    // Units constants TODO: Replace with UnitsFormat
    public final static String UNIT_METRIC = "gov.nasa.worldwind.StatusBar.Metric";
    public final static String UNIT_IMPERIAL = "gov.nasa.worldwind.StatusBar.Imperial";

    protected static final int MAX_ALPHA = 254;
    protected final JLabel latDisplay = new JLabel("");
    protected final JLabel lonDisplay = new JLabel(Logging.getMessage("term.OffGlobe"));
    protected final JLabel altDisplay = new JLabel("");
    protected final JLabel eleDisplay = new JLabel("");
    protected final AtomicBoolean showNetworkStatus = new AtomicBoolean(true);
    protected final AtomicBoolean isNetworkAvailable = new AtomicBoolean(true);
    protected Thread netCheckThread;
    private WorldWindow eventSource;
    private String elevationUnit = StatusBar.UNIT_METRIC;
    private String angleFormat = Angle.ANGLE_FORMAT_DD;

    public StatusBar() {
        super(new GridLayout(1, 0));

        final JLabel heartBeat = new JLabel(Logging.getMessage("term.Downloading"));

        altDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        latDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        lonDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        eleDisplay.setHorizontalAlignment(SwingConstants.CENTER);

        this.add(altDisplay);
        this.add(latDisplay);
        this.add(lonDisplay);
        this.add(eleDisplay);
        this.add(heartBeat);

        heartBeat.setHorizontalAlignment(SwingConstants.CENTER);
        heartBeat.setForeground(new Color(255, 0, 0, 0));

        Timer downloadTimer = new Timer(100, actionEvent -> {
            if (!showNetworkStatus.get()) {
                if (!heartBeat.getText().isEmpty())
                    heartBeat.setText("");
                return;
            }

            if (!isNetworkAvailable.get()) {
                heartBeat.setText(Logging.getMessage("term.NoNetwork"));
                heartBeat.setForeground(new Color(255, 0, 0, StatusBar.MAX_ALPHA));
                return;
            }

            Color color = heartBeat.getForeground();
            int alpha = color.getAlpha();
            if (isNetworkAvailable.get() && WorldWind.retrieveRemote().hasActiveTasks()) {
                heartBeat.setText(Logging.getMessage("term.Downloading"));
                if (alpha >= StatusBar.MAX_ALPHA)
                    alpha = StatusBar.MAX_ALPHA;
                else
                    alpha = alpha < 16 ? 16 : Math.min(StatusBar.MAX_ALPHA, alpha + 20);
            } else {
                alpha = Math.max(0, alpha - 20);
            }
            heartBeat.setForeground(new Color(255, 0, 0, alpha));
        });
        downloadTimer.start();

        this.netCheckThread = this.startNetCheckThread();
    }

    protected NetworkCheckThread startNetCheckThread() {
        NetworkCheckThread nct = new NetworkCheckThread(this.showNetworkStatus, this.isNetworkAvailable, null);
        nct.setDaemon(true);
        nct.start();

        return nct;
    }

    public boolean isShowNetworkStatus() {
        return showNetworkStatus.get();
    }

    public void setShowNetworkStatus(boolean showNetworkStatus) {
        this.showNetworkStatus.set(showNetworkStatus);

        if (showNetworkStatus) {
            if (this.netCheckThread != null)
                this.netCheckThread.interrupt();

            this.netCheckThread = this.startNetCheckThread();
        } else {
            if (this.netCheckThread != null)
                this.netCheckThread.interrupt();

            this.netCheckThread = null;
        }
    }

    public void moved(PositionEvent event) {
        this.handleCursorPositionChange(event);
    }

    public WorldWindow getEventSource() {
        return this.eventSource;
    }

    public void setEventSource(WorldWindow newEventSource) {
        if (this.eventSource != null) {
            this.eventSource.removePositionListener(this);
            this.eventSource.removeRenderingListener(this);
        }

        if (newEventSource != null) {
            newEventSource.addPositionListener(this);
            newEventSource.addRenderingListener(this);
        }

        this.eventSource = newEventSource;
    }

    public String getElevationUnit() {
        return this.elevationUnit;
    }

    public void setElevationUnit(String unit) {
        if (unit == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.elevationUnit = unit;
    }

    public String getAngleFormat() {
        return this.angleFormat;
    }

    public void setAngleFormat(String format) {
        if (format == null) {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.angleFormat = format;
    }

    protected String makeCursorElevationDescription(double metersElevation) {
        String s;
        String elev = Logging.getMessage("term.Elev");
        if (StatusBar.UNIT_IMPERIAL.equals(elevationUnit))
            s = String.format(elev + " %,7d feet", (int) (WWMath.convertMetersToFeet(metersElevation)));
        else // Default to metric units.
            s = String.format(elev + " %,7d meters", (int) metersElevation);
        return s;
    }

    protected String makeEyeAltitudeDescription(double metersAltitude) {
        String s;
        String altitude = Logging.getMessage("term.Altitude");
        if (StatusBar.UNIT_IMPERIAL.equals(elevationUnit)) {
            double miles = WWMath.convertMetersToMiles(metersAltitude);
            if (Math.abs(miles) >= 1)
                s = String.format(altitude + " %,7d mi", (int) Math.round(miles));
            else
                s = String.format(altitude + " %,7d ft", (int) Math.round(WWMath.convertMetersToFeet(metersAltitude)));
        } else if (Math.abs(metersAltitude) >= 1000) // Default to metric units.
            s = String.format(altitude + " %,7d km", (int) Math.round(metersAltitude / 1.0e3));
        else
            s = String.format(altitude + " %,7d m", (int) Math.round(metersAltitude));
        return s;
    }

    protected String makeAngleDescription(String label, Angle angle) {
        String s;
        if (Angle.ANGLE_FORMAT_DMS.equals(angleFormat))
            s = String.format("%s %s", label, angle.toDMSString());
        else
            s = String.format("%s %7.4f\u00B0", label, angle.degrees);
        return s;
    }

    protected void handleCursorPositionChange(PositionEvent event) {
        Position newPos = event.getPosition();
        if (newPos != null) {
            String las = makeAngleDescription("Lat", newPos.getLat());
            String los = makeAngleDescription("Lon", newPos.getLon());
            String els = makeCursorElevationDescription(
                eventSource.model().globe().elevation(newPos.getLat(), newPos.getLon()));
            latDisplay.setText(las);
            lonDisplay.setText(los);
            eleDisplay.setText(els);
        } else {
            latDisplay.setText("");
            lonDisplay.setText(Logging.getMessage("term.OffGlobe"));
            eleDisplay.setText("");
        }
    }

    public void stageChanged(RenderingEvent event) {
        if (!event.getStage().equals(RenderingEvent.BEFORE_BUFFER_SWAP))
            return;

        EventQueue.invokeLater(() -> {
            if (eventSource.view() != null && eventSource.view().getEyePosition() != null)
                altDisplay.setText(makeEyeAltitudeDescription(
                    eventSource.view().getEyePosition().getElevation()));
            else
                altDisplay.setText(Logging.getMessage("term.Altitude"));
        });
    }
}