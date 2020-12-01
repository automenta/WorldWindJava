/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;

import java.awt.*;

/**
 * @author dcollins
 * @version $Id: AudioPlayerAnnotation.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class AudioPlayerAnnotation extends DialogAnnotation {
    protected static final String PLAY_IMAGE_PATH = "gov/nasa/worldwind/examples/images/16x16-button-play.png";
    protected static final String PAUSE_IMAGE_PATH = "gov/nasa/worldwind/examples/images/16x16-button-pause.png";
    protected static final String BACK_IMAGE_PATH = "gov/nasa/worldwind/examples/images/16x16-button-start.png";

    protected static final String PLAY_TOOLTIP_TEXT = "Play audio clip";
    protected static final String PAUSE_TOOLTIP_TEXT = "Pause audio clip";
    protected static final String BACK_TOOLTIP_TEXT = "Stop and reset audio clip";

    protected long position;
    protected long length;
    // Nested annotation components.
    protected Annotation titleLabel;
    protected ButtonAnnotation playButton;
    protected ButtonAnnotation backButton;
    protected Annotation positionLabel;
    protected Annotation lengthLabel;
    protected ProgressAnnotation progress;

    public AudioPlayerAnnotation(Position position) {
        super(position);

        this.setClipPosition(0);
        this.setClipLength(0);
    }

    public long getClipPosition() {
        return this.position;
    }

    public void setClipPosition(long position) {
        this.position = position;

        String text = this.formatTimeString(position);
        this.getClipPositionLabel().setText(text);

        this.getClipProgressBar().setValue(position);
    }

    public long getClipLength() {
        return this.length;
    }

    public void setClipLength(long length) {
        this.length = length;

        String text = this.formatTimeString(length);
        this.getClipLengthLabel().setText(text);

        this.getClipProgressBar().setMax(0);
        this.getClipProgressBar().setMax(length);
    }

    public Annotation getTitleLabel() {
        return this.titleLabel;
    }

    public ButtonAnnotation getPlayButton() {
        return this.playButton;
    }

    public ButtonAnnotation getBackButton() {
        return this.backButton;
    }

    public Annotation getClipPositionLabel() {
        return this.positionLabel;
    }

    public Annotation getClipLengthLabel() {
        return this.lengthLabel;
    }

    public ProgressAnnotation getClipProgressBar() {
        return this.progress;
    }

    @SuppressWarnings("StringEquality")
    public void setPlayButtonState(String state) {
        if (state == AVKey.PLAY) {
            this.playButton.setImageSource(PLAY_IMAGE_PATH);
            this.playButton.setToolTipText(PLAY_TOOLTIP_TEXT);
        }
        else if (state == AVKey.PAUSE) {
            this.playButton.setImageSource(PAUSE_IMAGE_PATH);
            this.playButton.setToolTipText(PAUSE_TOOLTIP_TEXT);
        }
    }

    //**************************************************************//
    //********************  Annotation Components  *****************//
    //**************************************************************//

    protected void initComponents() {
        super.initComponents();

        this.titleLabel = new ScreenAnnotation("", new Point());
        this.playButton = new ButtonAnnotation(PLAY_IMAGE_PATH, DEPRESSED_MASK_PATH);
        this.backButton = new ButtonAnnotation(BACK_IMAGE_PATH, DEPRESSED_MASK_PATH);
        this.positionLabel = new ScreenAnnotation("", new Point());
        this.lengthLabel = new ScreenAnnotation("", new Point());
        this.progress = new ProgressAnnotation();

        this.setupTitle(this.titleLabel);
        this.setupTimeLabel(this.positionLabel);
        this.setupTimeLabel(this.lengthLabel);
        this.setupProgressBar(this.progress);

        this.playButton.setActionCommand(AVKey.PLAY);
        this.backButton.setActionCommand(AVKey.STOP);

        this.playButton.addActionListener(this);
        this.backButton.addActionListener(this);

        this.playButton.setToolTipText(PLAY_TOOLTIP_TEXT);
        this.backButton.setToolTipText(BACK_TOOLTIP_TEXT);
    }

    protected void layoutComponents() {
        super.layoutComponents();

        Annotation controlsContainer = new ScreenAnnotation("", new Point());
        {
            this.setupContainer(controlsContainer);
            controlsContainer.setLayout(new AnnotationFlowLayout(AVKey.HORIZONTAL, AVKey.CENTER, 4, 0)); // hgap, vgap
            controlsContainer.addChild(this.playButton);
            controlsContainer.addChild(this.backButton);
            controlsContainer.addChild(this.positionLabel);
            controlsContainer.addChild(this.progress);
            controlsContainer.addChild(this.lengthLabel);

            Insets insets = this.positionLabel.getAttributes().getInsets();
            this.positionLabel.getAttributes().setInsets(
                new Insets(insets.top, insets.left + 4, insets.bottom, insets.right));
        }

        Annotation contentContainer = new ScreenAnnotation("", new Point());
        {
            this.setupContainer(contentContainer);
            contentContainer.setLayout(new AnnotationFlowLayout(AVKey.VERTICAL, AVKey.CENTER, 0, 16)); // hgap, vgap
            contentContainer.addChild(this.titleLabel);
            contentContainer.addChild(controlsContainer);
        }

        this.addChild(contentContainer);
    }

    protected void setupTitle(Annotation annotation) {
        this.setupLabel(annotation);

        AnnotationAttributes attribs = annotation.getAttributes();
        attribs.setFont(Font.decode("Arial-BOLD-14"));
        attribs.setSize(new Dimension(260, 0));
        attribs.setTextAlign(AVKey.CENTER);
    }

    protected void setupTimeLabel(Annotation annotation) {
        this.setupLabel(annotation);

        AnnotationAttributes attribs = annotation.getAttributes();
        attribs.setFont(Font.decode("CourierNew-PLAIN-12"));
        attribs.setSize(new Dimension(80, 0));
    }

    protected void setupProgressBar(Annotation annotation) {
        AnnotationAttributes defaultAttribs = new AnnotationAttributes();
        this.setupDefaultAttributes(defaultAttribs);
        defaultAttribs.setSize(new Dimension(160, 10));
        annotation.getAttributes().setDefaults(defaultAttribs);
    }

    //**************************************************************//
    //********************  Utilities  *****************************//
    //**************************************************************//

    protected String formatTimeString(long millis) {
        return this.formatAsMinutesSeconds(millis);
    }

    protected String formatAsMinutesSeconds(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000);
        long remainderSeconds = seconds - minutes * 60;

        return String.format("%02d:%02d", minutes, remainderSeconds);
    }
}
