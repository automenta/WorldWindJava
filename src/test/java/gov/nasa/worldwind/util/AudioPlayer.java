/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.Keys;

import javax.sound.sampled.*;
import javax.swing.event.*;
import java.awt.*;

/**
 * Plays an audio file.
 *
 * @author Patrick Murris
 * @version $Id: AudioPlayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class AudioPlayer {
    protected final EventListenerList listenerList = new EventListenerList();
    protected long pausedMicrosecondPosition;
    private Clip clip;
    private String status = Keys.STOP;
    protected final LineListener lineListener = this::onLineEvent;

    public AudioPlayer() {
    }

    public AudioPlayer(Clip clip) {
        this.setClip(clip);
    }

    public Clip getClip() {
        return this.clip;
    }

    public void setClip(Clip clip) {
        if (this.clip == clip)
            return;

        if (this.clip != null) {
            this.clip.removeLineListener(this.lineListener);
        }

        this.clip = clip;

        if (this.clip != null) {
            this.clip.addLineListener(this.lineListener);
        }
    }

    public String getStatus() {
        return this.status;
    }

    public long getMillisecondLength() {
        if (this.clip == null)
            return 0;

        return this.clip.getMicrosecondLength() / 1000;
    }

    public long getMillisecondPosition() {
        if (this.clip == null)
            return 0;

        return this.clip.getMicrosecondPosition() / 1000;
    }

    public void setMillisecondPosition(long position) {
        if (this.clip == null)
            return;

        if (position < 0 || position > getMillisecondLength())
            return;

        this.clip.setMicrosecondPosition(position * 1000);
    }

    @SuppressWarnings("StringEquality")
    public void play() {
        if (this.clip == null)
            return;

        if (this.getStatus() == Keys.PAUSE) {
            this.doStart(this.pausedMicrosecondPosition);
        }
        else if (this.getStatus() == Keys.STOP) {
            this.doStart(0);
        }

        this.pausedMicrosecondPosition = 0;
    }

    @SuppressWarnings("StringEquality")
    public void stop() {
        if (this.clip == null)
            return;

        this.doStop(0);
    }

    @SuppressWarnings("StringEquality")
    public void pause() {
        if (this.clip == null)
            return;

        if (this.getStatus() == Keys.PLAY) {
            this.doPause();
        }
    }

    public LineListener[] getLineListeners() {
        return this.listenerList.getListeners(LineListener.class);
    }

    public void addLineListener(LineListener listener) {
        this.listenerList.add(LineListener.class, listener);
    }

    public void removeLineListener(LineListener listener) {
        this.listenerList.remove(LineListener.class, listener);
    }

    protected void doStart(long microsecondPosition) {
        this.status = Keys.PLAY;
        this.clip.setMicrosecondPosition(microsecondPosition);
        this.clip.start();
    }

    @SuppressWarnings("StringEquality")
    protected void doStop(long microsecondPosition) {
        boolean needToStop = (this.getStatus() != Keys.STOP);

        this.status = Keys.STOP;
        this.pausedMicrosecondPosition = microsecondPosition;
        this.clip.setMicrosecondPosition(microsecondPosition);

        if (needToStop) {
            this.clip.stop();
        }
    }

    protected void doPause() {
        this.status = Keys.PAUSE;
        this.pausedMicrosecondPosition = this.clip.getMicrosecondPosition();
        this.clip.stop();
    }

    protected void onLineEvent(final LineEvent e) {
        // This event comes from the Java Sound Dispatch Thread. Synchronize access to this class by processing the
        // event on the AWT Event Thread.
        EventQueue.invokeLater(() -> processLineEvent(e));
    }

    @SuppressWarnings("StringEquality")
    protected void processLineEvent(LineEvent e) {
        if (e.getType() == LineEvent.Type.STOP) {
            // If the player's statis is STATUS_PLAY, then this event is arriving because the clip has reached its end,
            // but not due to an explicit call to Clip.stop(). In this case, we must explicity stop the clip to keep
            // the player's state synchronized with the clip.
            if (this.getStatus() == Keys.PLAY) {
                long microsecondLength = this.getClip().getMicrosecondLength();
                this.doStop(microsecondLength);
            }
        }

        this.fireUpdate(e);
    }

    protected void fireUpdate(LineEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = this.listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == LineListener.class) {
                ((LineListener) listeners[i + 1]).update(e);
            }
        }
    }
}