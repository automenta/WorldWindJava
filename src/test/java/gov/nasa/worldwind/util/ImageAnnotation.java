/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.render.*;
import gov.nasa.worldwind.geom.Position;

import java.awt.*;

/**
 * @author dcollins
 * @version $Id: ImageAnnotation.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ImageAnnotation extends ScreenAnnotation {
    protected int imageWidth;
    protected int imageHeight;
    protected boolean fitSizeToImage;
    protected boolean useImageAspectRatio;
    protected boolean enableSmoothing;
    protected boolean useMipmaps;
    // Tool tip state.
    protected boolean showToolTip;
    protected String toolTipText;
    protected Point toolTipPoint;

    public ImageAnnotation(Object imageSource, int imageWidth, int imageHeight) {
        super("", new Point());

        this.fitSizeToImage = true;
        this.useImageAspectRatio = true;
        this.enableSmoothing = true;
        this.useMipmaps = true;

        this.setImageSource(imageSource, imageWidth, imageHeight);
        this.setupAnnotationAttributes(this);
    }

    public ImageAnnotation(Object imageSource) {
        this(imageSource, 0, 0);
    }

    public ImageAnnotation() {
        this(null, 0, 0);
    }

    public boolean isFitSizeToImage() {
        return this.fitSizeToImage;
    }

    public void setFitSizeToImage(boolean fitSizeToImage) {
        this.fitSizeToImage = fitSizeToImage;
    }

    public boolean isUseImageAspectRatio() {
        return this.useImageAspectRatio;
    }

    public void setUseImageAspectRatio(boolean useImageAspectRatio) {
        this.useImageAspectRatio = useImageAspectRatio;
    }

    public boolean isEnableSmoothing() {
        return this.enableSmoothing;
    }

    public void setEnableSmoothing(boolean enable) {
        this.enableSmoothing = enable;
    }

    public boolean isUseMipmaps() {
        return this.useMipmaps;
    }

    public void setUseMipmaps(boolean useMipmaps) {
        this.useMipmaps = useMipmaps;
    }

    public Object getImageSource() {
        return this.getAttributes().getImageSource();
    }

    public void setImageSource(Object source) {
        this.setImageSource(source, 0, 0);
    }

    public void setImageSource(Object source, int imageWidth, int imageHeight) {
        this.getAttributes().setImageSource(source);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public Dimension getImageDimension() {
        return new Dimension(this.imageWidth, this.imageHeight);
    }

    public WWTexture getTexture(DrawContext dc) {
        return this.getAttributes().getBackgroundTexture(dc);
    }

    public boolean isShowToolTip() {
        return this.showToolTip;
    }

    public void setShowToolTip(boolean show) {
        this.showToolTip = show;
    }

    public String getToolTipText() {
        return this.toolTipText;
    }

    public void setToolTipText(String toolTipText) {
        this.toolTipText = toolTipText;
    }

    public Point getToolTipPoint() {
        return toolTipPoint;
    }

    public void setToolTipPoint(Point toolTipPoint) {
        this.toolTipPoint = toolTipPoint;
    }

    //**************************************************************//
    //********************  Preferred Size  ************************//
    //**************************************************************//

    public Dimension getPreferredSize(DrawContext dc) {
        Dimension imageSize = this.getImageSize(dc);
        Dimension insetSize = null;

        // Optionally set the annotation's inset size to the image size.
        if (this.isFitSizeToImage()) {
            insetSize = imageSize;
        }

        // Fallback to the superclass preferred size.
        if (insetSize == null) {
            insetSize = super.getPreferredSize(dc);

            // Optionally set the annotation's aspect ratio to that of the image. We'll use the superclass width, and
            // override it's height to match the image's aspect ration.
            if (this.isUseImageAspectRatio() && imageSize != null) {
                double aspect = imageSize.getHeight() / imageSize.getWidth();
                insetSize = new Dimension(insetSize.width, (int) Math.round(aspect * insetSize.width));
            }
        }

        Insets insets = this.getAttributes().getInsets();
        return new Dimension(
            insetSize.width + (insets.left + insets.right),
            insetSize.height + (insets.top + insets.bottom));
    }

    protected Dimension getImageSize(DrawContext dc) {
        WWTexture texture = this.getTexture(dc);
        if (texture != null && this.imageWidth == 0 && this.imageHeight == 0) {
            return new Dimension(texture.getWidth(dc), texture.getHeight(dc));
        }
        else if (this.imageWidth != 0 && this.imageHeight != 0) {
            return new Dimension(this.imageWidth, this.imageHeight);
        }
        else {
            return null;
        }
    }

    //**************************************************************//
    //********************  Rendering  *****************************//
    //**************************************************************//

    public void drawContent(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
        super.drawContent(dc, width, height, opacity, pickPosition);
        this.drawToolTip(dc);
    }

    protected void drawToolTip(DrawContext dc) {
        if (dc.isPickingMode())
            return;

        if (!this.isShowToolTip())
            return;

        String text = this.getToolTipText();
        if (text == null)
            return;

        Point point = this.getToolTipPoint();
        if (point == null)
            return;

        this.doDrawToolTip(dc, text, point.x, point.y);
    }

    protected void doDrawToolTip(DrawContext dc, String text, int x, int y) {
        OrderedRenderable toolTip = new ToolTip(text, x, y);
        dc.addOrderedRenderable(toolTip);
    }

    protected void applyBackgroundTextureState(DrawContext dc, int width, int height, double opacity,
        WWTexture texture) {
        super.applyBackgroundTextureState(dc, width, height, opacity, texture);

        // Setup the texture filters to correspond to the smoothing and mipmap settings.
        int minFilter = this.isEnableSmoothing() ?
            (this.isUseMipmaps() ? GL.GL_LINEAR_MIPMAP_LINEAR : GL.GL_LINEAR) : GL.GL_NEAREST;
        int magFilter = this.isEnableSmoothing() ?
            GL.GL_LINEAR : GL.GL_NEAREST;

        GL gl = dc.getGL();
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, magFilter);
    }

    protected void transformBackgroundImageCoordsToAnnotationCoords(DrawContext dc, int width, int height,
        WWTexture texture) {
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        // Scale background image coordinates to fit the Annotation's dimensions.
        Dimension size = this.getImageSize(dc);
        if (size != null) {
            gl.glScaled(size.getWidth() / width, size.getHeight() / height, 1.0d);
        }

        super.transformBackgroundImageCoordsToAnnotationCoords(dc, width, height, texture);
    }

    //**************************************************************//
    //********************  Utilities  *****************************//
    //**************************************************************//

    protected void setupAnnotationAttributes(Annotation annotation) {
        Color transparentBlack = new Color(0, 0, 0, 0);

        AnnotationAttributes defaultAttribs = new AnnotationAttributes();
        defaultAttribs.setAdjustWidthToText(AVKey.SIZE_FIXED);
        defaultAttribs.setBackgroundColor(transparentBlack);
        defaultAttribs.setBorderColor(transparentBlack);
        defaultAttribs.setBorderWidth(0);
        defaultAttribs.setCornerRadius(0);
        defaultAttribs.setDrawOffset(new Point(0, 0));
        defaultAttribs.setHighlightScale(1);
        defaultAttribs.setInsets(new Insets(0, 0, 0, 0));
        defaultAttribs.setImageScale(1);
        defaultAttribs.setImageOffset(new Point(0, 0));
        defaultAttribs.setImageOpacity(1);
        defaultAttribs.setImageRepeat(AVKey.REPEAT_NONE);
        defaultAttribs.setLeader(AVKey.SHAPE_NONE);
        defaultAttribs.setSize(new Dimension(0, 0));

        annotation.setPickEnabled(false);
        annotation.getAttributes().setDefaults(defaultAttribs);
    }
}
