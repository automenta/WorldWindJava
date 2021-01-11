/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: VPFBasicSymbolFactory.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class VPFBasicSymbolFactory implements VPFSymbolFactory {
    private static final double DEFAULT_ICON_MAX_SIZE = 10000; // Max 10km

    protected final VPFPrimitiveData primitiveData;
    protected final VPFFeatureFactory featureFactory;
    protected VPFSymbolSupport symbolSupport;

    public VPFBasicSymbolFactory(VPFTile tile, VPFPrimitiveData primitiveData) {
        this.primitiveData = primitiveData;
        this.featureFactory = new VPFBasicFeatureFactory(tile, primitiveData);
    }

    protected static VPFBoundingBox combineBounds(Iterable<? extends VPFFeature> features) {
        VPFBoundingBox bounds = null;

        for (VPFFeature f : features) {
            bounds = (bounds != null) ? f.getBounds().union(bounds) : f.getBounds();
        }

        return bounds;
    }

    protected static int[] combinePrimitiveIds(Iterable<? extends VPFFeature> features) {
        int length = 0;
        for (VPFFeature f : features) {
            if (f.getPrimitiveIds() != null) {
                length += f.getPrimitiveIds().length;
            }
        }

        int[] array = new int[length];
        int position = 0;
        for (VPFFeature f : features) {
            if (f.getPrimitiveIds() != null) {
                int[] src = f.getPrimitiveIds();
                System.arraycopy(src, 0, array, position, src.length);
                position += src.length;
            }
        }

        return array;
    }

    protected static void addPointLabel(CombinedFeature feature, VPFSymbolAttributes attr,
        Collection<VPFSymbol> outCollection) {
        // Build the list of point symbol locations associated with each sub-feature.
        ArrayList<LatLon> locations = new ArrayList<>();

        for (VPFFeature subFeature : feature) {
            if (subFeature.getBounds() != null) {
                locations.add(subFeature.getBounds().toSector().getCentroid());
            }
        }

        SurfaceIcons o = new SurfaceIcons("", locations);
        VPFBasicSymbolFactory.applyPointSymbolAttributes(attr, o);
        outCollection.add(new VPFSymbol(feature, attr, o));
    }

    protected static void applyPointSymbolAttributes(VPFSymbolAttributes attr, SurfaceIcon icon) {
        if (attr.getIconImageSource() != null)
            icon.setImageSource(attr.getIconImageSource());
        icon.setUseMipMaps(attr.isMipMapIconImage());
        icon.setScale(attr.getIconImageScale());
        icon.setMaxSize(VPFBasicSymbolFactory.DEFAULT_ICON_MAX_SIZE);
    }

    protected static Angle getPointSymbolHeading(VPFSymbolAttributes attr, AVList featureAttributes) {
        if (attr.getOrientationAttributeName() == null) {
            return null;
        }

        Object o = featureAttributes.get(attr.getOrientationAttributeName());
        if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            return new Angle(d);
        } else if (o instanceof String) {
            Double d = WWUtil.convertStringToDouble((String) o);
            if (d != null)
                return new Angle(d);
        }

        return null;
    }

    protected static void applySymbolAttributes(ShapeAttributes attr, Attributable surfaceShape) {
        surfaceShape.setAttributes(attr);
    }

    public static void applyTextAttributes(VPFSymbolAttributes attr, GeographicText text) {
        VPFSymbolAttributes.LabelAttributes[] labelAttr = attr.getLabelAttributes();
        if (labelAttr != null && labelAttr.length > 0) {
            text.setFont(labelAttr[0].getFont());
            text.setColor(labelAttr[0].getColor());
            text.setBackgroundColor(labelAttr[0].getBackgroundColor());
        } else {
            text.setFont(Font.decode("Arial-PLAIN-12"));
            text.setColor(attr.getInteriorMaterial().getDiffuse());
            text.setBackgroundColor(WWUtil.computeContrastingColor(attr.getInteriorMaterial().getDiffuse()));
        }
    }

    protected static LatLon computeLabelLocation(VPFSymbolAttributes.LabelAttributes attr, VPFFeature feature) {
        LatLon location = feature.getBounds().toSector().getCentroid();

        // If we are given label offset parameters, compute the label location using the offset azimuth and offset arc
        // length.
        if (attr.getOffset() != 0) {
            VPFLibrary library = feature.getFeatureClass().getCoverage().getLibrary();

            Angle offsetAzimuth = attr.getOffsetAngle();
            Angle offsetLength = library.computeArcLengthFromMapDistance(attr.getOffset());
            if (offsetAzimuth != null && offsetLength != null) {
                location = LatLon.greatCircleEndPosition(location, offsetAzimuth, offsetLength);
            }
        }

        return location;
    }

    //**************************************************************//
    //********************  Symbol Assembly  ***********************//
    //**************************************************************//

    public VPFSymbolSupport getStyleSupport() {
        return this.symbolSupport;
    }

    public void setStyleSupport(VPFSymbolSupport symbolSupport) {
        this.symbolSupport = symbolSupport;
    }

    /**
     * @param featureClass The feature class.
     * @return the symbols.
     */
    @Override
    public Collection<? extends VPFSymbol> createPointSymbols(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FeatureMap map = this.createFeatureMap(featureClass);
        if (map == null)
            return null;

        Collection<VPFSymbol> symbols = new ArrayList<>();
        this.doCreatePointSymbols(map, symbols);
        return symbols;
    }

    /**
     * @param featureClass The feature class.
     * @return The symbols.
     */
    public Collection<? extends VPFSymbol> createLineSymbols(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FeatureMap map = this.createFeatureMap(featureClass);
        if (map == null)
            return null;

        Collection<VPFSymbol> symbols = new ArrayList<>();
        this.doCreateLineSymbols(map, symbols);
        return symbols;
    }

    /**
     * @param featureClass The feature class.
     * @return The symbols.
     */
    public Collection<? extends VPFSymbol> createAreaSymbols(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FeatureMap map = this.createFeatureMap(featureClass);
        if (map == null)
            return null;

        Collection<VPFSymbol> symbols = new ArrayList<>();
        this.doCreateAreaSymbols(map, symbols);
        return symbols;
    }

    /**
     * @param featureClass The feature class.
     * @return The symbols.
     */
    public Collection<? extends VPFSymbol> createTextSymbols(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FeatureMap map = this.createFeatureMap(featureClass);
        if (map == null)
            return null;

        Collection<VPFSymbol> symbols = new ArrayList<>();
        this.doCreateTextSymbols(map, symbols);
        return symbols;
    }

    /**
     * @param featureClass The feature class.
     * @return The symbols.
     */
    public Collection<? extends VPFSymbol> createComplexSymbols(VPFFeatureClass featureClass) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        throw new UnsupportedOperationException();
    }

    protected FeatureMap createFeatureMap(VPFFeatureClass featureClass) {
        // Get the features associated with the feature class.
        Collection<? extends VPFFeature> features = featureClass.createFeatures(this.featureFactory);
        if (features == null)
            return null;

        FeatureMap map = new FeatureMap();

        for (VPFFeature feature : features) {
            // Get the symbol keys associated with the current feature.
            Iterable<? extends VPFSymbolKey> symbolKeys = this.getSymbolKeys(feature);
            if (symbolKeys == null)
                continue;

            // Map the feature according to its associated symbol key.
            for (VPFSymbolKey key : symbolKeys) {
                map.addFeature(key, feature);
            }
        }

        return map;
    }

    /**
     * From MIL-DTL-89045A, section 3.5.3.1.1: A point feature may be symbolized as either a point symbol or as a text
     * label or both.
     * <p>
     * From MIL-HDBK-857A, section 6.5.3.1: For point features (e.g., buoys, beacons, lights) that are composed of
     * several symbol components, displaying the components according to the row ids in the *sym.txt file will result in
     * the properly constructed composite symbol.
     *
     * @param featureMap    The feature map.
     * @param outCollection The symbols.
     */
    protected void doCreatePointSymbols(Map<VPFSymbolKey, CombinedFeature> featureMap,
        Collection<VPFSymbol> outCollection) {
        for (Map.Entry<VPFSymbolKey, CombinedFeature> entry : featureMap.entrySet()) {
            CombinedFeature feature = entry.getValue();

            for (VPFSymbolAttributes attr : this.getSymbolAttributes(feature, entry.getKey())) {
                // Construct a renderable object for each point symbol.
                // Construct a renderable object for each label symbol.
                switch (attr.getFeatureType()) {
                    case POINT -> this.addPointSymbol(feature, attr, outCollection);
                    case LABEL -> this.addTextLabel((VPFFeature) feature, attr, outCollection);
                }
            }
        }
    }

    /**
     * From MIL-DTL-89045A, section 3.5.3.1.1: A linear feature will be symbolized exclusively by a line symbol that may
     * or may not be labeled.
     *
     * @param featureMap    The feature map.
     * @param outCollection The symbols.
     */
    protected void doCreateLineSymbols(Map<VPFSymbolKey, CombinedFeature> featureMap,
        Collection<VPFSymbol> outCollection) {
        for (Map.Entry<VPFSymbolKey, CombinedFeature> entry : featureMap.entrySet()) {
            CombinedFeature feature = entry.getValue();

            for (VPFSymbolAttributes attr : this.getSymbolAttributes(feature, entry.getKey())) {
                // Construct a renderable object for each line symbol.
                // Construct a renderable object for each label symbol.
                switch (attr.getFeatureType()) {
                    case LINE -> this.addLineSymbol(feature, attr, outCollection);
                    case LABEL -> this.addTextLabel((VPFFeature) feature, attr, outCollection);
                }
            }
        }
    }

    /**
     * From MIL-DTL-89045A, section 3.5.3.1.1: An area feature may be symbolized by any combination of point, line, or
     * area symbol as well as with a text label.
     * <p>
     * From MIL-HDBK-857A, section 6.4.1.4: It is also possible for there to exist multiple symbology components for
     * area features.  The most common situation is the need for the addition of the low accuracy symbol (see
     * 6.4.1.3.4).  This situation is implemented in the same way as for points, with an additional row in the *sym.txt
     * table to control the placement of the low accuracy symbol.
     * <p>
     * It is also possible for the symbolization of an area feature to require multiple rows to specify the components
     * of the full area symbol.  This situation will exist for those area features requiring both a solid fill and a
     * pattern fill.  In this case, the two area symbols will be specified using two rows in the *sym.txt file with the
     * row specifying the solid fill always preceding the row specifying the pattern fill.  If the same area feature
     * also requires a boundary and/or a centered point symbol, those symbols will be specified in the second row for
     * the area feature (along with the area pattern).  Section 6.5 explains the ramifications of this approach in more
     * detail.
     * <p>
     * For these reasons, as well as for the placement of text labels (see section 6.4.1.5), it is crucial that
     * application software access all rows from the sym.txt file for a given product/delineation/feature code in order
     * to ensure full symbolization for any feature.
     * <p>
     * From MIL-HDBK-857A, section 6.5.3.2: There are some area features (e.g., Maritime Areas) that require both a
     * solid fill and one or more pattern fills.  Since the areasym column can only contain a single CGM reference,
     * there is a separate row in the *sym.txt file for each of the area symbols, as well as for the line symbol and/or
     * point symbol that apply to the specific area feature.  These multiple rows will have sequential row ids in the
     * *sym.txt file according to the order in which the symbols are to be displayed on the screen:  solid fill, pattern
     * fill (may be more than one), linear boundary, centered point symbol (may be more than one).
     *
     * @param featureMap    The feature map.
     * @param outCollection The symbols.
     */
    protected void doCreateAreaSymbols(Map<VPFSymbolKey, CombinedFeature> featureMap,
        Collection<VPFSymbol> outCollection) {
        for (Map.Entry<VPFSymbolKey, CombinedFeature> entry : featureMap.entrySet()) {
            CombinedFeature feature = entry.getValue();

            for (VPFSymbolAttributes attr : this.getSymbolAttributes(feature, entry.getKey())) {
                // Construct a renderable object for each area symbol.
                // Construct a renderable object for each line symbol.
                // This renderable area has been configured to draw its outline, and not its fill. Because the display
                // order is data driven; we want to control the display order of the fill and the outline independently.
                // Construct a renderable object for each point symbol.
                // Construct a renderable object for each label symbol.
                switch (attr.getFeatureType()) {
                    case AREA, LINE -> this.addAreaSymbol(feature, attr, outCollection);
                    case POINT -> VPFBasicSymbolFactory.addPointLabel(feature, attr, outCollection);
                    case LABEL -> this.addTextLabel((VPFFeature) feature, attr, outCollection);
                }
            }
        }
    }

    //**************************************************************//
    //********************  Symbol Attribute Assembly  *************//
    //**************************************************************//

    /**
     * From MIL-DTL-89045A, section 3.5.3.1.1: VPF products can contain a fourth type of feature known as a text
     * feature.GeoSym does not include rules to display text features.The application software should refer to the
     * MIL-STD-2407 for information on how to display VPF text features.
     *
     * @param featureMap    The feature map.
     * @param outCollection The symbols.
     */
    protected void doCreateTextSymbols(Map<VPFSymbolKey, CombinedFeature> featureMap,
        Collection<VPFSymbol> outCollection) {
        for (Map.Entry<VPFSymbolKey, CombinedFeature> entry : featureMap.entrySet()) {
            CombinedFeature feature = entry.getValue();

            for (VPFSymbolAttributes attr : this.getSymbolAttributes(feature, entry.getKey())) {
                // Construct a renderable object for each area symbol.
                switch (attr.getFeatureType()) {
                    case TEXT -> this.addTextSymbol(feature, attr, outCollection);
                }
            }
        }
    }

    protected void addPointSymbol(CombinedFeature feature, VPFSymbolAttributes attr,
        Collection<VPFSymbol> outCollection) {
        // Build the list of locations and headings associated with each point symbol.
        int numSymbols = 0;
        boolean haveUniqueHeadings = false;
        ArrayList<LatLon> locations = new ArrayList<>();
        ArrayList<Angle> headings = new ArrayList<>();

        for (VPFFeature subFeature : feature) {
            String primitiveName = feature.getFeatureClass().getPrimitiveTableName();
            Angle heading = VPFBasicSymbolFactory.getPointSymbolHeading(attr, subFeature);

            for (int id : subFeature.getPrimitiveIds()) {
                CompoundVecBuffer combinedCoords = this.primitiveData.getPrimitiveCoords(primitiveName);
                if (combinedCoords != null) {
                    VecBuffer coords = combinedCoords.subBuffer(id);
                    if (coords != null) {
                        if (!haveUniqueHeadings)
                            haveUniqueHeadings = !headings.isEmpty() && !headings.contains(heading);

                        locations.add(coords.getPosition(0));
                        headings.add(heading);
                        numSymbols++;
                    }
                }
            }
        }

        if (haveUniqueHeadings) {
            for (int i = 0; i < numSymbols; i++) {
                SurfaceIcon o = new SurfaceIcon("", locations.get(i));
                o.setHeading(headings.get(i));
                VPFBasicSymbolFactory.applyPointSymbolAttributes(attr, o);
                outCollection.add(new VPFSymbol(feature, attr, o));
            }
        } else {
            SurfaceIcons o = new SurfaceIcons("", locations);
            if (headings.get(0) != null)
                o.setHeading(headings.get(0));
            VPFBasicSymbolFactory.applyPointSymbolAttributes(attr, o);
            outCollection.add(new VPFSymbol(feature, attr, o));
        }
    }

    protected void addLineSymbol(CombinedFeature feature, VPFSymbolAttributes attr,
        Collection<VPFSymbol> outCollection) {
        SurfaceShape o = new VPFSurfaceLine(feature, this.primitiveData);
        VPFBasicSymbolFactory.applySymbolAttributes(attr, o);
        outCollection.add(new VPFSymbol(feature, attr, o));
    }

    protected void addAreaSymbol(CombinedFeature feature, VPFSymbolAttributes attr,
        Collection<VPFSymbol> outCollection) {
        SurfaceShape o = new VPFSurfaceArea(feature, this.primitiveData);
        VPFBasicSymbolFactory.applySymbolAttributes(attr, o);
        outCollection.add(new VPFSymbol(feature, attr, o));
    }

    protected void addTextSymbol(CombinedFeature feature, VPFSymbolAttributes attr,
        Collection<VPFSymbol> outCollection) {
        String primitiveName = feature.getFeatureClass().getPrimitiveTableName();
        CompoundVecBuffer combinedCoords = this.primitiveData.getPrimitiveCoords(primitiveName);
        CompoundStringBuilder combinedStrings = this.primitiveData.getPrimitiveStrings(primitiveName);

        // Construct a renderable object for the first text symbol.
        for (int id : feature.getPrimitiveIds()) {
            VecBuffer coords = combinedCoords.subBuffer(id);
            CharSequence text = combinedStrings.subSequence(id);
            if (text != null)
                text = WWUtil.trimCharSequence(text);

            GeographicText o = new UserFacingText(text, coords.getPosition(0));
            VPFBasicSymbolFactory.applyTextAttributes(attr, o);
            outCollection.add(new VPFSymbol(feature, attr, o));
        }
    }

    protected void addTextLabel(Iterable<VPFFeature> feature, VPFSymbolAttributes attr,
        Collection<VPFSymbol> outCollection) {
        for (VPFFeature subFeature : feature) {
            this.addTextLabel(subFeature, attr, outCollection);
        }
    }

    protected void addTextLabel(VPFFeature feature, VPFSymbolAttributes attr, Collection<VPFSymbol> outCollection) {
        VPFSymbolAttributes.LabelAttributes[] labelAttr = attr.getLabelAttributes();
        if (labelAttr == null || labelAttr.length == 0)
            return;

        for (VPFSymbolAttributes.LabelAttributes la : labelAttr) {
            GeographicText o = new UserFacingText("", null);
            this.applyLabelAttributes(la, feature, o);

            // Do not specify any symbol attributes for the label.
            outCollection.add(new VPFSymbol(feature, null, o));
        }
    }

    protected Iterable<? extends VPFSymbolKey> getSymbolKeys(VPFFeature feature) {
        String fcode = feature.getStringValue("f_code");
        return this.symbolSupport.getSymbolKeys(feature.getFeatureClass(), fcode, feature);
    }

    //**************************************************************//
    //********************  Feature Support  ***********************//
    //**************************************************************//

    protected Iterable<? extends VPFSymbolAttributes> getSymbolAttributes(VPFFeature feature, VPFSymbolKey symbolKey) {
        return this.symbolSupport.getSymbolAttributes(feature.getFeatureClass(), symbolKey);
    }

    protected void applyLabelAttributes(VPFSymbolAttributes.LabelAttributes attr, VPFFeature feature,
        GeographicText text) {
        text.setFont(attr.getFont());
        text.setColor(attr.getColor());
        text.setBackgroundColor(attr.getBackgroundColor());

        LatLon location = VPFBasicSymbolFactory.computeLabelLocation(attr, feature);
        if (location != null)
            text.setPosition(new Position(location, 0));

        String labelText = this.symbolSupport.getSymbolLabelText(attr, feature);
        if (labelText != null)
            text.setText(labelText);
    }

    protected static class FeatureMap extends HashMap<VPFSymbolKey, CombinedFeature> {
        public void addFeature(VPFSymbolKey key, VPFFeature feature) {
            CombinedFeature combined = this.get(key);
            if (combined == null) {
                combined = new CombinedFeature(feature.getFeatureClass());
                this.put(key, combined);
            }

            combined.add(feature);
        }
    }

    protected static class CombinedFeature extends VPFFeature implements Iterable<VPFFeature> {
        private final ArrayList<VPFFeature> featureList;

        public CombinedFeature(VPFFeatureClass featureClass) {
            super(featureClass, -1, new VPFBoundingBox(0, 0, 0, 0), null);
            this.featureList = new ArrayList<>();
        }

        public VPFBoundingBox getBounds() {
            return VPFBasicSymbolFactory.combineBounds(this.featureList);
        }

        public int[] getPrimitiveIds() {
            return VPFBasicSymbolFactory.combinePrimitiveIds(this.featureList);
        }

        public void add(VPFFeature feature) {
            this.featureList.add(feature);
        }

        public Iterator<VPFFeature> iterator() {
            return this.featureList.iterator();
        }
    }
}
