/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.util.*;

/**
 * @author Patrick Murris
 * @version $Id: VPFSymbolAttributes.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFSymbolAttributes extends BasicShapeAttributes {
    private static final Font defaultFont = Font.decode("Arial-PLAIN-12");
    private static final Color defaultColor = Color.WHITE;
    private static final Color defaultBackgroundColor = Color.BLACK;
    private VPFFeatureType featureType;
    private VPFSymbolKey symbolKey;
    private Object iconImageSource;
    private double iconImageScale;
    private boolean mipMapIconImage;
    private LabelAttributes[] labelAttributes;
    private double displayPriority;
    private String orientationAttributeName;
    private String description;

    public VPFSymbolAttributes() {
    }

    public VPFSymbolAttributes(VPFFeatureType featureType, VPFSymbolKey symbolKey) {
        this.featureType = featureType;
        this.symbolKey = symbolKey;
        this.iconImageSource = null;
        this.iconImageScale = 1.0d;
        this.mipMapIconImage = true;
        this.labelAttributes = null;
        this.displayPriority = 0;
        this.orientationAttributeName = null;
        this.description = null;
    }

    public VPFSymbolAttributes(VPFSymbolAttributes attributes) {
        super(attributes);
        this.featureType = attributes.getFeatureType();
        this.symbolKey = attributes.getSymbolKey();
        this.iconImageSource = attributes.getIconImageSource();
        this.iconImageScale = attributes.getIconImageScale();
        this.mipMapIconImage = attributes.isMipMapIconImage();
        this.displayPriority = attributes.getDisplayPriority();
        this.orientationAttributeName = attributes.getOrientationAttributeName();
        this.description = attributes.getDescription();

        if (attributes.getLabelAttributes() != null) {
            LabelAttributes[] array = attributes.getLabelAttributes();
            int numLabelAttributes = array.length;
            this.labelAttributes = new LabelAttributes[numLabelAttributes];

            for (int i = 0; i < numLabelAttributes; i++) {
                this.labelAttributes[i] = (array[i] != null) ? array[i].copy() : null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ShapeAttributes copy() {
        return new VPFSymbolAttributes(this);
    }

    /**
     * {@inheritDoc}
     */
    public void copy(ShapeAttributes attributes) {
        super.copy(attributes);

        if (attributes instanceof VPFSymbolAttributes) {
            VPFSymbolAttributes vpfAttrs = (VPFSymbolAttributes) attributes;
            this.featureType = vpfAttrs.getFeatureType();
            this.symbolKey = vpfAttrs.getSymbolKey();
            this.iconImageSource = vpfAttrs.getIconImageSource();
            this.iconImageScale = vpfAttrs.getIconImageScale();
            this.mipMapIconImage = vpfAttrs.isMipMapIconImage();
            this.displayPriority = vpfAttrs.getDisplayPriority();
            this.orientationAttributeName = vpfAttrs.getOrientationAttributeName();
            this.description = vpfAttrs.getDescription();

            if (vpfAttrs.getLabelAttributes() != null) {
                LabelAttributes[] array = vpfAttrs.getLabelAttributes();
                int numLabelAttributes = array.length;
                this.labelAttributes = new LabelAttributes[numLabelAttributes];

                for (int i = 0; i < numLabelAttributes; i++) {
                    this.labelAttributes[i] = (array[i] != null) ? array[i].copy() : null;
                }
            }
        }
    }

    public VPFFeatureType getFeatureType() {
        return this.featureType;
    }

    public VPFSymbolKey getSymbolKey() {
        return this.symbolKey;
    }

    public Object getIconImageSource() {
        return this.iconImageSource;
    }

    public void setIconImageSource(Object imageSource) {
        this.iconImageSource = imageSource;
    }

    public double getIconImageScale() {
        return this.iconImageScale;
    }

    public void setIconImageScale(double scale) {
        this.iconImageScale = scale;
    }

    public boolean isMipMapIconImage() {
        return this.mipMapIconImage;
    }

    public void setMipMapIconImage(boolean mipMap) {
        this.mipMapIconImage = mipMap;
    }

    public LabelAttributes[] getLabelAttributes() {
        return this.labelAttributes;
    }

    public void setLabelAttributes(LabelAttributes[] attributes) {
        this.labelAttributes = attributes;
    }

    public double getDisplayPriority() {
        return this.displayPriority;
    }

    public void setDisplayPriority(double displayPriority) {
        this.displayPriority = displayPriority;
    }

    public String getOrientationAttributeName() {
        return this.orientationAttributeName;
    }

    public void setOrientationAttributeName(String name) {
        this.orientationAttributeName = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        VPFSymbolAttributes that = (VPFSymbolAttributes) o;

        if (Double.compare(this.displayPriority, that.displayPriority) != 0)
            return false;
        if (Double.compare(this.iconImageScale, that.iconImageScale) != 0)
            return false;
        if (this.mipMapIconImage != that.mipMapIconImage)
            return false;
        if (!Objects.equals(this.description, that.description))
            return false;
        if (this.featureType != that.featureType)
            return false;
        if (!Objects.equals(this.iconImageSource, that.iconImageSource))
            return false;
        if (!Arrays.equals(this.labelAttributes, that.labelAttributes))
            return false;
        if (!Objects.equals(this.orientationAttributeName, that.orientationAttributeName))
            return false;
        if (!Objects.equals(this.symbolKey, that.symbolKey))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        result = 31 * result + (this.featureType != null ? this.featureType.hashCode() : 0);
        result = 31 * result + (this.symbolKey != null ? this.symbolKey.hashCode() : 0);
        result = 31 * result + (this.iconImageSource != null ? this.iconImageSource.hashCode() : 0);
        temp = this.iconImageScale == +0.0d ? 0L : Double.doubleToLongBits(this.iconImageScale);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (this.mipMapIconImage ? 1 : 0);
        result = 31 * result + (this.labelAttributes != null ? Arrays.hashCode(this.labelAttributes) : 0);
        temp = this.displayPriority == +0.0d ? 0L : Double.doubleToLongBits(this.displayPriority);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (this.orientationAttributeName != null ? this.orientationAttributeName.hashCode() : 0);
        result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
        return result;
    }

    public static class LabelAttributes {
        private Font font;
        private Color color;
        private Color backgroundColor;
        private double offset;
        private Angle offsetAngle;
        private String prepend;
        private String append;
        private String attributeName;
        private int abbreviationTableId;

        public LabelAttributes() {
            this.font = VPFSymbolAttributes.defaultFont;
            this.color = VPFSymbolAttributes.defaultColor;
            this.backgroundColor = VPFSymbolAttributes.defaultBackgroundColor;
        }

        public LabelAttributes(LabelAttributes attributes) {
            if (attributes == null) {
                String message = Logging.getMessage("nullValue.AttributesIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.font = attributes.getFont();
            this.color = attributes.getColor();
            this.backgroundColor = attributes.getBackgroundColor();
            this.offset = attributes.getOffset();
            this.offsetAngle = attributes.getOffsetAngle();
            this.prepend = attributes.getPrepend();
            this.append = attributes.getAppend();
            this.attributeName = attributes.getAttributeName();
            this.abbreviationTableId = attributes.getAbbreviationTableId();
        }

        public LabelAttributes copy() {
            return new LabelAttributes(this);
        }

        public Font getFont() {
            return this.font;
        }

        public void setFont(Font font) {
            this.font = font;
        }

        public Color getColor() {
            return this.color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public Color getBackgroundColor() {
            return this.backgroundColor;
        }

        public void setBackgroundColor(Color color) {
            this.backgroundColor = color;
        }

        public double getOffset() {
            return offset;
        }

        public void setOffset(double offset) {
            this.offset = offset;
        }

        public Angle getOffsetAngle() {
            return this.offsetAngle;
        }

        public void setOffsetAngle(Angle angle) {
            this.offsetAngle = angle;
        }

        public String getPrepend() {
            return this.prepend;
        }

        public void setPrepend(String text) {
            this.prepend = text;
        }

        public String getAppend() {
            return this.append;
        }

        public void setAppend(String text) {
            this.append = text;
        }

        public String getAttributeName() {
            return this.attributeName;
        }

        public void setAttributeName(String name) {
            this.attributeName = name;
        }

        public int getAbbreviationTableId() {
            return this.abbreviationTableId;
        }

        public void setAbbreviationTableId(int tableId) {
            this.abbreviationTableId = tableId;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            LabelAttributes that = (LabelAttributes) o;

            if (this.abbreviationTableId != that.abbreviationTableId)
                return false;
            if (Double.compare(this.offset, that.offset) != 0)
                return false;
            if (!Objects.equals(this.append, that.append))
                return false;
            if (!Objects.equals(this.attributeName, that.attributeName))
                return false;
            if (!Objects.equals(this.backgroundColor, that.backgroundColor))
                return false;
            if (!Objects.equals(this.color, that.color))
                return false;
            if (!Objects.equals(this.font, that.font))
                return false;
            if (!Objects.equals(this.offsetAngle, that.offsetAngle))
                return false;
            if (!Objects.equals(this.prepend, that.prepend))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = this.font != null ? this.font.hashCode() : 0;
            result = 31 * result + (this.color != null ? this.color.hashCode() : 0);
            result = 31 * result + (this.backgroundColor != null ? this.backgroundColor.hashCode() : 0);
            temp = this.offset == +0.0d ? 0L : Double.doubleToLongBits(this.offset);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + (this.offsetAngle != null ? this.offsetAngle.hashCode() : 0);
            result = 31 * result + (this.prepend != null ? this.prepend.hashCode() : 0);
            result = 31 * result + (this.append != null ? this.append.hashCode() : 0);
            result = 31 * result + (this.attributeName != null ? this.attributeName.hashCode() : 0);
            result = 31 * result + this.abbreviationTableId;
            return result;
        }
    }
}
