/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Position;

import java.awt.*;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: SARTrackBuilder.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SARTrackBuilder {
    private final MouseAdapter mouseAdapter;
    private final MouseMotionAdapter mouseMotionAdapter;
    private final PositionListener positionListener;
    private WorldWindow wwd;
    private SARTrack sarTrack;
    private boolean armed = false;
    private boolean active = false;
    private boolean useTrackElevation = false;

    public SARTrackBuilder() {
        this.mouseAdapter = new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (armed && sarTrack != null && mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    if (armed && (mouseEvent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                        if (mouseEvent.isAltDown() && !mouseEvent.isControlDown()) {
                            active = true;
                            addPosition();
                        }
                    }
                    mouseEvent.consume();
                }
            }

            public void mouseReleased(MouseEvent mouseEvent) {
                if (armed && sarTrack != null && mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    active = false;
                    mouseEvent.consume();
                }
            }

            public void mouseClicked(MouseEvent mouseEvent) {
                if (armed && sarTrack != null && mouseEvent.getButton() == MouseEvent.BUTTON1) {
                    if (mouseEvent.isControlDown())
                        removeLastTrackPoint();
                    mouseEvent.consume();
                }
            }
        };

        this.mouseMotionAdapter = new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent mouseEvent) {
                if (armed && sarTrack != null && (mouseEvent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                    // Don't update the track here because the wwd current cursor position will not
                    // have been updated to reflect the current mouse position. Wait to update in the
                    // position listener, but consume the event so the view doesn't respond to it.
                    if (active)
                        mouseEvent.consume();
                }
            }
        };

        this.positionListener = event -> {
            if (!active || sarTrack == null)
                return;

            replacePosition();
        };
    }

    public void setWwd(WorldWindow wwd) {
        if (this.wwd == wwd)
            return;

        if (this.wwd != null) {
            this.wwd.input().removeMouseListener(this.mouseAdapter);
            this.wwd.input().removeMouseMotionListener(this.mouseMotionAdapter);
            this.wwd.removePositionListener(this.positionListener);
        }

        this.wwd = wwd;
        this.wwd.input().addMouseListener(this.mouseAdapter);
        this.wwd.input().addMouseMotionListener(this.mouseMotionAdapter);
        this.wwd.addPositionListener(this.positionListener);
    }

    public void setTrack(SARTrack track) {
        this.sarTrack = track;
    }

    public boolean isUseTrackElevation() {
        return this.useTrackElevation;
    }

    public void setUseTrackElevation(boolean state) {
        this.useTrackElevation = state;
    }

    public boolean isArmed() {
        return this.armed;
    }

    public void setArmed(boolean armed) {
        this.armed = armed;

        if (this.armed)
            ((Component) this.wwd).setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        else
            ((Component) this.wwd).setCursor(Cursor.getDefaultCursor());
    }

    private void addPosition() {
        Position curPos = this.wwd != null ? this.wwd.position() : null;
        if (curPos == null)
            return;

        if (this.useTrackElevation && !this.sarTrack.isEmpty())
            curPos = new Position(curPos, this.sarTrack.get(this.sarTrack.size() - 1).getElevation());

        this.sarTrack.appendPosition(new SARPosition(curPos));
    }

    private void replacePosition() {
        Position curPos = this.wwd != null ? this.wwd.position() : null;
        if (curPos == null)
            return;

        if (this.useTrackElevation && !this.sarTrack.isEmpty())
            curPos = new Position(curPos, this.sarTrack.get(this.sarTrack.size() - 1).getElevation());

        int index = this.sarTrack.size() - 1;
        if (index < 0)
            index = 0;

        this.sarTrack.set(index, new SARPosition(curPos));
    }

    public boolean canRemoveLastTrackPoint() {
        return this.sarTrack != null && !this.sarTrack.isEmpty();
    }

    public void removeLastTrackPoint() {
        if (this.sarTrack == null || this.sarTrack.isEmpty())
            return;

        this.sarTrack.removePosition(this.sarTrack.size() - 1);
    }
}
