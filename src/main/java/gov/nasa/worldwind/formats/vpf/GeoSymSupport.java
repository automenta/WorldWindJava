/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.formats.vpf;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.logging.Level;

/**
 * @author Patrick Murris
 * @version $Id: GeoSymSupport.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class GeoSymSupport {
    protected static final String UNKNOWN_POINT_SYMBOL = "5000";
    protected static final String UNKNOWN_LINE_SYMBOL = "5001";
    private final String filePath;
    private final String imageSuffix;
    private final Map<FeatureKey, List<? extends VPFSymbolKey>> featureMap;
    private GeoSymAssignment assignment;
    private Map<Integer, VPFSymbolAttributes.LabelAttributes> textJoinAttributes;
    private Map<String, Integer> productTypes;
    private Map<String, Integer> deliniations;
    private Map<VPFFeatureType, String> featureName;
    private GeoSymStyleProvider styleProvider;
    private GeoSymAbbreviationProvider abbreviationProvider;
    private GeoSymAttributeExpressionProvider attributeExpressionProvider;

    public GeoSymSupport(String filePath, String symbolMimeType) {
        if (filePath == null) {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (symbolMimeType == null) {
            String message = Logging.getMessage("nullValue.ImageFomat");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.filePath = filePath;
        this.imageSuffix = WWIO.mimeSuffix(symbolMimeType);
        this.featureMap = new HashMap<>();

        this.loadAssignment(filePath);

        if (this.assignment == null) {
            String message = Logging.getMessage("VPF.GeoSymSupportDisabled");
            Logging.logger().warning(message);
        } else {
            this.loadStyleProvider();
            this.loadAbbreviationProvider();
            this.loadAttributeExpressionProvider();
            this.loadLabelAttributes();
            this.loadProductTypes();
            this.loadFeatureTypes();
        }
    }

    protected static HashMap<Integer, VPFSymbolAttributes.LabelAttributes> getTextLabelCharacteristics(
        GeoSymTable textCharTable,
        GeoSymTable colorTable) {
        HashMap<Integer, VPFSymbolAttributes.LabelAttributes> attributes =
            new HashMap<>();

        for (KV row : textCharTable.getRecords()) {
            Integer id = (Integer) row.get("id");
            VPFSymbolAttributes.LabelAttributes attr = GeoSymSupport.getTextLabelCharacteristics(row, colorTable);

            attributes.put(id, attr);
        }

        return attributes;
    }

    protected static VPFSymbolAttributes.LabelAttributes getTextLabelCharacteristics(KV row,
        GeoSymTable colorTable) {
        VPFSymbolAttributes.LabelAttributes attr = new VPFSymbolAttributes.LabelAttributes();

        // Get text characteristics
        Integer fontType = (Integer) row.get("tfont");
        Integer fontStyle = (Integer) row.get("tstyle");
        Integer size = (Integer) row.get("tsize");
        Color color = GeoSymSupport.selectColor(colorTable, (Integer) row.get("tcolor"));
        String prepend = (String) row.get("tprepend");
        String append = (String) row.get("tappend");
        Integer abbreviationTableIndex = (Integer) row.get("abindexid");

        // Assemble label attributes
        if (fontType != null && fontStyle != null && size != null) {
            String fontString = fontType == 0 ? "Arial-" : "Serif-";
            if (fontStyle == 0 || fontStyle == 4)
                fontString += "PLAIN";
            else if (fontStyle == 1 || fontStyle == 5)
                fontString += "BOLD";
            else if (fontStyle == 2 || fontStyle == 6)
                fontString += "ITALIC";
            else if (fontStyle == 3 || fontStyle == 7)
                fontString += "BOLDITALIC";
            else
                fontString += "PLAIN";
            fontString += "-" + size;
            attr.setFont(Font.decode(fontString));
        }
        if (color != null) {
            attr.setColor(color);
            attr.setBackgroundColor(WWUtil.computeContrastingColor(color));
        }
        if (prepend != null)
            attr.setPrepend(String.valueOf(Character.toChars(Integer.parseInt(prepend, 16))[0]));
        if (append != null)
            attr.setAppend(String.valueOf(Character.toChars(Integer.parseInt(append, 16))[0]));
        if (abbreviationTableIndex != null)
            attr.setAbbreviationTableId(abbreviationTableIndex);

        return attr;
    }

    protected static HashMap<Integer, VPFSymbolAttributes.LabelAttributes> getTextLabelLocations(
        GeoSymTable textLocTable) {
        HashMap<Integer, VPFSymbolAttributes.LabelAttributes> attributes =
            new HashMap<>();

        for (KV row : textLocTable.getRecords()) {
            Integer id = (Integer) row.get("id");
            VPFSymbolAttributes.LabelAttributes attr = GeoSymSupport.getTextLabelLocation(row);

            attributes.put(id, attr);
        }

        return attributes;
    }

    protected static VPFSymbolAttributes.LabelAttributes getTextLabelLocation(KV row) {
        VPFSymbolAttributes.LabelAttributes attr = new VPFSymbolAttributes.LabelAttributes();

        // From MIL-HDBK-857A, section 6.8.3: The tjust field contain a numeric code whose possible values for the
        // specified justification of the text label is defined in the code.txt file.
        //
        // In addition to the traditional vertical and horizontal justifications, one justification (sounding text)
        // specifies that the attribute is a depth value. See section 6.8.3.1 for details on how to place the depth
        // value as a text label.

        // TODO: apply text justification

        //Integer i = (Integer) row.getValue("tjust");
        //if (i != null)
        //    attr.setJustification(i);

        // From MIL-HDBK-857A, section 6.8.3: The tdist and tdir fields describe the distance in millimeters and the
        // azimuth degrees from North for the default offset of the text label relative to the feature's position. Zero
        // values in these fields indicate that the text is to be displayed at the center of the feature.

        Integer i = (Integer) row.get("tdist");
        if (i != null)
            attr.setOffset(i);

        i = (Integer) row.get("tdir");
        if (i != null)
            attr.setOffsetAngle(new Angle((double) i));

        return attr;
    }

    protected static Collection<KV> selectSymbolAssignments(GeoSymTable table, int pid, String fcode, int delin,
        String cov) {
        Collection<KV> rows = new ArrayList<>(Arrays.asList(table.getRecords()));

        // The fcode field contains the 5-character feature code as defined in the applicable VPF product specification.
        GeoSymTable.selectMatchingStringRows("fcode", fcode, false, rows);

        // The pid field contains a coded numeric value that identifies the VPF product to which the rule applies. The
        // mapping from pidfile (see section 3.5.3.1.4).
        if (pid > 0) {
            // Some assignments do not specify a 'pid'. In these cases, we must select those rows too.
            GeoSymTable.selectMatchingRows("pid", pid, true, rows);
        }

        // The delin field contains a coded numeric value that identifies the delineation of that fcode. The mapping
        // from delin number to delineation type is defithe code.txt file.
        if (delin > 0) {
            // Some assignments do not specify a 'delin'. In these cases, we must select those rows too.
            GeoSymTable.selectMatchingRows("delin", delin, true, rows);
        }

        // The cov field is used only for those fcodes that are contained in multiple coverages within a VPF product.
        // Some products, such as DTOP, have the same feature defined in multiple coverages and with different
        // attribution within each coverage.  This is a common practice for terrain-based products (e.g., DTOP, VITD)
        // when representing common open water features, such as River/Stream features, across coverages. When this
        // situation exists, it is necessary to have a different "if" part of the logical expression based on which
        // coverage is being displayed.  If the cov field is populated, it will contain one of two types of information:
        // 1.) the abbreviation of the appropriate VPF coverage (e.g., sdr,pop) in that product as defined in that
        // product's coverage attribute table (cat); or 2.) the "not" of that same coverage (e.g., <>sdr,<>pop).

        if (cov != null) {
            // Some assignments do not specify a 'cov'. In these cases, we must ignore the cov parameter and return
            // the closest match according to the other parameters.
            GeoSymSupport.selectMatchingCoverages("cov", cov, true, rows);
        }

        return rows;
    }

    protected static void selectMatchingCoverages(String columnName, String value, boolean acceptNullValue,
        Iterable<KV> outRows) {
        Iterator<KV> iter = outRows.iterator();
        if (!iter.hasNext())
            return;

        KV record;
        while (iter.hasNext()) {
            record = iter.next();
            if (record == null)
                continue;

            Object o = record.get(columnName);
            if (o == null || o instanceof String) {
                String s = (String) o;
                if (s == null || s.isEmpty()) {
                    if (!acceptNullValue)
                        iter.remove();
                } else {
                    int pos = s.indexOf("<>");
                    if (pos >= 0)
                        s = s.substring(pos + 1);

                    boolean match = s.equalsIgnoreCase(value);
                    if ((pos < 0 && !match) || (pos >= 0 && match))
                        iter.remove();
                }
            }
        }
    }

    protected static int selectCodeValue(GeoSymTable codeTable, String fileName, String attributeName,
        String description) {
        List<KV> rows = new ArrayList<>(Arrays.asList(codeTable.getRecords()));
        GeoSymTable.selectMatchingStringRows("file", fileName, false, rows);
        GeoSymTable.selectMatchingStringRows("attribute", attributeName, false, rows);
        GeoSymTable.selectMatchingStringRows("description", description, false, rows);
        if (rows.isEmpty())
            return -1;

        Integer i = (Integer) rows.get(0).get("value");
        return (i != null) ? i : -1;
    }

    protected static String selectCodeDescription(GeoSymTable codeTable, String fileName, String attributeName,
        int value) {
        List<KV> rows = new ArrayList<>(Arrays.asList(codeTable.getRecords()));
        GeoSymTable.selectMatchingStringRows("file", fileName, false, rows);
        GeoSymTable.selectMatchingStringRows("attribute", attributeName, false, rows);
        GeoSymTable.selectMatchingRows("value", value, false, rows);
        if (rows.isEmpty())
            return null;

        return (String) rows.get(0).get("description");
    }

    //**************************************************************//
    //********************  Symbology Data Initialization  *********//
    //**************************************************************//

    protected static Color selectColor(GeoSymTable colorTable, int colorIndex) {
        ArrayList<KV> rows = new ArrayList<>(Arrays.asList(colorTable.getRecords()));
        GeoSymTable.selectMatchingRows("index", colorIndex, false, rows);
        if (rows.isEmpty())
            return null;

        KV row = rows.get(0);
        Integer r = (Integer) row.get("red");
        Integer g = (Integer) row.get("green");
        Integer b = (Integer) row.get("blue");
        return (r != null && g != null && b != null) ? new Color(r, g, b) : null;
    }

    protected static KV selectTextLabelCharacteristics(GeoSymTable textCharTable, int txtRowId) {
        ArrayList<KV> rows = new ArrayList<>(Arrays.asList(textCharTable.getRecords()));
        GeoSymTable.selectMatchingRows("id", txtRowId, false, rows);
        return (rows.isEmpty()) ? null : rows.get(0);
    }

    protected static String getAssignmentPath(String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(filePath);
        sb.append('/');
        sb.append(GeoSymConstants.SYMBOLOGY_ASSIGNMENT);
        sb.append('/');
        sb.append(GeoSymConstants.ASCII);

        return sb.toString();
    }

    protected static String getPathForAssignmentFile(String filePath, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append(GeoSymSupport.getAssignmentPath(filePath));
        sb.append('/');
        sb.append(fileName);
        return sb.toString();
    }

    /**
     * From MIL-HDBK-857A, section 6.4.1.6.1: Default Symbology  Although all efforts have been made to symbolize all
     * features according to the appropriate symbology specifications, should the application software be unable to
     * determine at least one "true" condition for a particular instance of a feature in the VPF dataset, there are
     * default CGMs provided in GeoSym for displaying that feature.  The following table identifies which symbol should
     * be used to symbolize an "unknown" point, line, or area feature.
     * <p>
     * Note that the default symbols should only be placed under two conditions: 1.  There is no row in the *sym.txt
     * file for the feature (fcode) 2.  After evaluating all rows in the *sym.txt file for that fcode, there is no row
     * that results in an evaluation of "true".
     *
     * @return a single element list containing a reference to the unknown symbol key {@link
     * VPFSymbolKey#UNKNOWN_SYMBOL_KEY}.
     */
    protected static List<? extends VPFSymbolKey> doGetUnknownSymbolKeys() {
        return Collections.singletonList(VPFSymbolKey.UNKNOWN_SYMBOL_KEY);
    }

    protected static String getProductName(VPFFeatureClass featureClass) {
        String s = featureClass.getCoverage().getLibrary().getProductType();

        // 'TADS', or Terrain Analysis Data Set is an alias for 'VITD', or 'Vector Product Interim Terrain Data'.
        if (s != null && s.equals("TADS")) {
            s = "VITD";
        }

        return s;
    }

    protected static void assembleCommonSymbolAttributes(KV symbolRow, VPFSymbolAttributes attr) {
        Integer i = KVMap.getIntegerValue(symbolRow, "dispri");
        if (i != null)
            attr.setDisplayPriority(i);

        String s = KVMap.getStringValue(symbolRow, "orient");
        if (s != null)
            attr.setOrientationAttributeName(s);

        s = KVMap.getStringValue(symbolRow, "feadesc");
        if (s != null)
            attr.setDescription(s);
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getImageSuffix() {
        return this.imageSuffix;
    }

    public GeoSymAssignment getAssignment() {
        return this.assignment;
    }

    public Iterable<? extends VPFSymbolKey> getSymbolKeys(VPFFeatureClass featureClass, String featureCode,
        KV featureAttributes) {
        if (featureClass == null) {
            String message = Logging.getMessage("nullValue.FeatureClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (featureCode == null) {
            String message = Logging.getMessage("nullValue.FeatureCodeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.getAssignment() == null) {
            return null;
        }

        // GeoSym does not provide stylization for Text features.
        if (featureClass.getType() == VPFFeatureType.TEXT) {
            return null;
        }

        // Mapped value may be null, indicating that no GeoSym attributes are available for the specified feature class
        // and FCODE.
        FeatureKey featureKey = new FeatureKey(featureClass, featureCode);
        List<? extends VPFSymbolKey> symbolKeys = this.featureMap.get(featureKey);
        if (symbolKeys == null && !this.featureMap.containsKey(featureKey)) {
            symbolKeys = this.doGetSymbolKeys(featureClass, featureCode);
            this.featureMap.put(featureKey, symbolKeys);
        }

        if (symbolKeys != null && featureAttributes != null) {
            symbolKeys = this.doEvaluateSymbolKeys(symbolKeys, featureAttributes);
        }

        List<? extends VPFSymbolKey> symbols = symbolKeys;
        if (symbolKeys != null && symbolKeys.isEmpty()) {
            symbols = GeoSymSupport.doGetUnknownSymbolKeys();
        }

        return symbols;
    }

    public Iterable<? extends VPFSymbolAttributes> getSymbolAttributes(VPFFeatureClass featureClass, VPFSymbolKey key) {
        if (key == null) {
            String message = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.getAssignment() == null) {
            return null;
        }

        // GeoSym does not provide stylization for Text features.
        if (featureClass.getType() == VPFFeatureType.TEXT) {
            return null;
        }

        if (key == VPFSymbolKey.UNKNOWN_SYMBOL_KEY) {
            return this.doGetUnknownSymbolAttributes(featureClass, key);
        }

        return this.doGetSymbolAttributes(featureClass, key);
    }

    public Object getSymbolSource(String symbol) {
        if (symbol == null) {
            String message = Logging.getMessage("nullValue.SymbolIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.getAssignment() == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(this.filePath);
        sb.append('/');
        sb.append(GeoSymConstants.GRAPHICS);
        sb.append('/');
        sb.append(GeoSymConstants.BINARY);
        sb.append('/');
        sb.append(symbol);
        if (imageSuffix != null)
            sb.append(this.imageSuffix);

        return sb.toString();
    }

    //**************************************************************//
    //********************  Symbol Selection  **********************//
    //**************************************************************//

    public String getAbbreviation(int tableId, int abbreviationId) {
        return this.abbreviationProvider.getAbbreviation(tableId, abbreviationId);
    }

    protected void loadAssignment(String filePath) {
        String geoSymPath = GeoSymSupport.getAssignmentPath(filePath);

        if (!GeoSymAssignment.isGeoSymAssignment(geoSymPath)) {
            String message = Logging.getMessage("VPF.GeoSymNotFound");
            Logging.logger().warning(message);
            return;
        }

        try {
            this.assignment = GeoSymAssignment.fromFile(geoSymPath);
        }
        catch (RuntimeException e) {
            String message = Logging.getMessage("generic.ExceptionWhileReading", filePath);
            Logging.logger().log(Level.SEVERE, message, e);
        }
    }

    protected void loadStyleProvider() {
        String path = GeoSymSupport.getPathForAssignmentFile(this.filePath, GeoSymConstants.LINE_AREA_ATTRIBUTES_FILE);
        try {
            this.styleProvider = new GeoSymStyleProvider(path);
        }
        catch (RuntimeException e) {
            String message = Logging.getMessage("generic.ExceptionWhileReading", path);
            Logging.logger().log(Level.SEVERE, message, e);
        }
    }

    protected void loadAbbreviationProvider() {
        String path = GeoSymSupport.getPathForAssignmentFile(this.filePath,
            GeoSymConstants.TEXT_ABBREVIATIONS_ASSIGNMENT_FILE);
        try {
            this.abbreviationProvider = new GeoSymAbbreviationProvider(path);
        }
        catch (RuntimeException e) {
            String message = Logging.getMessage("generic.ExceptionWhileReading", path);
            Logging.logger().log(Level.SEVERE, message, e);
        }
    }

    protected void loadAttributeExpressionProvider() {
        GeoSymTable table = this.getAssignment().getTable(GeoSymConstants.ATTRIBUTE_EXPRESSION_FILE);
        try {
            this.attributeExpressionProvider = new GeoSymAttributeExpressionProvider(table);
        }
        catch (RuntimeException e) {
            String message = Logging.getMessage("generic.ExceptionWhileReading",
                GeoSymConstants.ATTRIBUTE_EXPRESSION_FILE);
            Logging.logger().log(Level.SEVERE, message, e);
        }
    }

    protected void loadLabelAttributes() {
        // Load label attributes for text characteristics and text locations.
        GeoSymTable colorTable = this.getAssignment().getTable(GeoSymConstants.COLOR_ASSIGNMENT_FILE);
        GeoSymTable textCharTable = this.getAssignment().getTable(GeoSymConstants.TEXT_LABEL_CHARACTERISTICS_FILE);
        GeoSymTable textLocTable = this.getAssignment().getTable(GeoSymConstants.TEXT_LABEL_LOCATION_FILE);

        Map<Integer, VPFSymbolAttributes.LabelAttributes> labelCharacteristics = GeoSymSupport.getTextLabelCharacteristics(
            textCharTable, colorTable);
        Map<Integer, VPFSymbolAttributes.LabelAttributes> labelLocations = GeoSymSupport.getTextLabelLocations(textLocTable);

        // Map text join to label attributes
        this.textJoinAttributes = new HashMap<>();
        GeoSymTable joinTable = this.getAssignment().getTable(GeoSymConstants.TEXT_LABEL_JOIN_FILE);
        for (KV row : joinTable.getRecords()) {
            int id = (Integer) row.get("id");
            int textCharId = (Integer) row.get("textcharid");
            int textLocId = (Integer) row.get("textlocid");
            VPFSymbolAttributes.LabelAttributes attr = new VPFSymbolAttributes.LabelAttributes();

            VPFSymbolAttributes.LabelAttributes chars = labelCharacteristics.get(textCharId);
            if (chars != null) {
                attr.setFont(chars.getFont());
                attr.setColor(chars.getColor());
                attr.setBackgroundColor(chars.getBackgroundColor());
                attr.setPrepend(chars.getPrepend());
                attr.setAppend(chars.getAppend());
                attr.setAbbreviationTableId(chars.getAbbreviationTableId());
            }

            VPFSymbolAttributes.LabelAttributes loc = labelLocations.get(textLocId);
            if (loc != null) {
                attr.setOffset(loc.getOffset());
                attr.setOffsetAngle(loc.getOffsetAngle());
            }

            this.textJoinAttributes.put(id, attr);
        }
    }

    protected void loadProductTypes() {
        GeoSymTable codeTable = this.getAssignment().getTable(GeoSymConstants.CODE_VALUE_DESCRIPTION_FILE);
        Iterable<KV> rows = new ArrayList<>(Arrays.asList(codeTable.getRecords()));
        GeoSymTable.selectMatchingRows("file", GeoSymConstants.FULL_SYMBOL_ASSIGNMENT_FILE, false, rows);
        GeoSymTable.selectMatchingRows("attribute", "pid", false, rows);

        this.productTypes = new HashMap<>();
        for (KV row : rows) {
            Integer value = (Integer) row.get("value");
            String description = (String) row.get("description");
            if (value != null && description != null)
                this.productTypes.put(description.toUpperCase(), value);
        }
    }

    protected void loadFeatureTypes() {
        GeoSymTable codeTable = this.getAssignment().getTable(GeoSymConstants.CODE_VALUE_DESCRIPTION_FILE);
        Iterable<KV> rows = new ArrayList<>(Arrays.asList(codeTable.getRecords()));
        GeoSymTable.selectMatchingRows("file", GeoSymConstants.FULL_SYMBOL_ASSIGNMENT_FILE, false, rows);
        GeoSymTable.selectMatchingRows("attribute", "delin", false, rows);

        this.deliniations = new HashMap<>();
        for (KV row : rows) {
            Integer value = (Integer) row.get("value");
            String description = (String) row.get("description");
            if (value != null && description != null)
                this.deliniations.put(description.toUpperCase(), value);
        }

        this.featureName = new HashMap<>();
        this.featureName.put(VPFFeatureType.POINT, "POINT");
        this.featureName.put(VPFFeatureType.LINE, "LINE");
        this.featureName.put(VPFFeatureType.AREA, "AREA");
    }

    protected List<? extends VPFSymbolKey> doGetSymbolKeys(VPFFeatureClass featureClass, String featureCode) {
        // Select the symbol rows associated with this feature class and feature code combination.
        Collection<KV> symbolRows = this.selectSymbolRows(featureClass, featureCode);
        if (symbolRows == null || symbolRows.isEmpty()) {
            return Collections.emptyList();
        }

        // Create symbol keys for each symbol row.
        List<VPFSymbolKey> keyList = new ArrayList<>();

        for (KV symbolRow : symbolRows) {
            if (symbolRow != null) {
                Integer id = KVMap.getIntegerValue(symbolRow, "id");
                if (id != null) {
                    keyList.add(new VPFSymbolKey(id));
                }
            }
        }

        // Sort the list of symbol keys in ascending order according to ID.
        Collections.sort(keyList);

        return keyList;
    }

    protected List<VPFSymbolKey> doEvaluateSymbolKeys(Iterable<? extends VPFSymbolKey> iterable,
        KV featureAttributes) {
        List<VPFSymbolKey> filteredKeys = new ArrayList<>();

        for (VPFSymbolKey key : iterable) {
            // If there exists an attribute expression for the current symbol, and the expression evaluates to 'false',
            // then drop the current symbol from the list of potential symbols.
            GeoSymAttributeExpression exp = this.attributeExpressionProvider.getAttributeExpression(
                key.getSymbolCode());
            if (exp != null && !exp.evaluate(featureAttributes)) {
                continue;
            }

            // Either no attribute expression exists for the current symbol, or the expresion evaluates to 'true'.
            filteredKeys.add(key);
        }

        return filteredKeys;
    }

    protected Collection<KV> selectSymbolRows(VPFFeatureClass featureClass, String featureCode) {
        // Get the product id associated with the feature class. A value of -1 indicates that no id is available.
        int pid = -1;
        String productName = GeoSymSupport.getProductName(featureClass);
        if (productName != null) {
            Integer i = this.productTypes.get(productName.toUpperCase());
            if (i != null)
                pid = i;
        }

        // Get the feature deliniation id associated with the feature class. A value of -1 indicates that no id is
        // available.
        int delin = -1;
        String featureName = this.featureName.get(featureClass.getType());
        if (featureName != null) {
            Integer i = this.deliniations.get(featureName.toUpperCase());
            if (i != null)
                delin = i;
        }

        // Get the coverage associated with the feature class.
        String cov = featureClass.getCoverage().getName();

        // Get the symbology assignment table.
        GeoSymTable symbolTable = this.getAssignment().getTable(GeoSymConstants.FULL_SYMBOL_ASSIGNMENT_FILE);

        // Select the matching symbol rows.
        return GeoSymSupport.selectSymbolAssignments(symbolTable, pid, featureCode, delin, cov);
    }

    //**************************************************************//
    //********************  Symbol Attribute Assembly  *************//
    //**************************************************************//

    @SuppressWarnings("UnusedDeclaration")
    protected Iterable<? extends VPFSymbolAttributes> doGetSymbolAttributes(VPFFeatureClass featureClass,
        VPFSymbolKey key) {
        // Get the symbology assignment table.
        GeoSymTable symbolTable = this.getAssignment().getTable(GeoSymConstants.FULL_SYMBOL_ASSIGNMENT_FILE);
        KV symbolRow = symbolTable.getRecord(key.getSymbolCode());
        if (symbolRow == null) {
            return null;
        }

        VPFSymbolAttributes attr = null;

        String s;
        if ((s = symbolRow.getStringValue("pointsym")) != null && !s.isEmpty()) {
            attr = new VPFSymbolAttributes(VPFFeatureType.POINT, key);
            this.assemblePointSymbolAttributes(s, attr);
        } else if ((s = symbolRow.getStringValue("linesym")) != null && !s.isEmpty()) {
            attr = new VPFSymbolAttributes(VPFFeatureType.LINE, key);
            this.assembleLineSymbolAttributes(s, attr);
        } else if ((s = symbolRow.getStringValue("areasym")) != null && !s.isEmpty()) {
            attr = new VPFSymbolAttributes(VPFFeatureType.AREA, key);
            this.assembleAreaSymbolAttributes(s, attr);
        } else if ((s = symbolRow.getStringValue("labatt")) != null && !s.isEmpty()) {
            attr = new VPFSymbolAttributes(VPFFeatureType.LABEL, key);
            this.assembleTextLabelAttributes(symbolRow, attr);
        }

        if (attr != null) {
            // Assemble the common symbol attributes.
            GeoSymSupport.assembleCommonSymbolAttributes(symbolRow, attr);
        }

        return Collections.singletonList(attr);
    }

    protected Iterable<? extends VPFSymbolAttributes> doGetUnknownSymbolAttributes(VPFFeatureClass featureClass,
        VPFSymbolKey key) {
        Collection<VPFSymbolAttributes> list = new ArrayList<>();

        if (featureClass.getType() == VPFFeatureType.POINT || featureClass.getType() == VPFFeatureType.AREA) {
            VPFSymbolAttributes attr = new VPFSymbolAttributes(featureClass.getType(), key);
            this.assemblePointSymbolAttributes(GeoSymSupport.UNKNOWN_POINT_SYMBOL, attr);
            list.add(attr);
        }

        if (featureClass.getType() == VPFFeatureType.LINE || featureClass.getType() == VPFFeatureType.AREA) {
            VPFSymbolAttributes attr = new VPFSymbolAttributes(featureClass.getType(), key);
            this.assembleLineSymbolAttributes(GeoSymSupport.UNKNOWN_LINE_SYMBOL, attr);
            list.add(attr);
        }

        return list;
    }

    protected void assemblePointSymbolAttributes(String symbol, VPFSymbolAttributes attr) {
        Object source = this.getSymbolSource(symbol);
        attr.setIconImageSource(source);
    }

    protected void assembleLineSymbolAttributes(String symbol, ShapeAttributes attr) {
        VPFSymbolAttributes geoSymAttr = (this.styleProvider != null) ? this.styleProvider.getAttributes(symbol) : null;
        if (geoSymAttr != null) {
            attr.setDrawInterior(false);
            attr.setDrawOutline(true);
            attr.setOutlineMaterial(geoSymAttr.getOutlineMaterial());
            attr.setOutlineWidth(geoSymAttr.getOutlineWidth());
            attr.setOutlineStipplePattern(geoSymAttr.getOutlineStipplePattern());
            attr.setOutlineStippleFactor(geoSymAttr.getOutlineStippleFactor());
        }
    }

    protected void assembleAreaSymbolAttributes(String symbol, ShapeAttributes attr) {
        VPFSymbolAttributes geoSymAttr = (this.styleProvider != null) ? this.styleProvider.getAttributes(symbol) : null;
        if (geoSymAttr != null) {
            attr.setDrawInterior(true);
            attr.setDrawOutline(false);
            attr.setInteriorMaterial(geoSymAttr.getInteriorMaterial());

            if (geoSymAttr.getImageSource() != null && geoSymAttr.getImageSource() instanceof String) {
                Object symbolSource = this.getSymbolSource((String) geoSymAttr.getImageSource());
                attr.setImageSource(symbolSource);
            }
        }
    }

    protected void assembleTextLabelAttributes(KV labelRow, VPFSymbolAttributes attr) {
        String[] attributeNames = null;
        String[] txtRowIds = null;

        String s = (String) labelRow.get("labatt");
        if (s != null && !s.isEmpty()) {
            String[] array = s.split(",");
            if (array.length > 0) {
                attributeNames = array;
            }
        }

        s = (String) labelRow.get("txrowid");
        if (s != null && !s.isEmpty()) {
            String[] array = s.split(",");
            if (array.length > 0) {
                txtRowIds = array;
            }
        }

        if (attributeNames == null || txtRowIds == null) {
            return;
        }

        // The cardinality of the labatt and txtrowid arrays should always be identical, but check anyway. Fall back to
        // using the smallest cardinality available in both arrays.
        int numLabels = attributeNames.length;
        if (numLabels > txtRowIds.length)
            numLabels = txtRowIds.length;

        VPFSymbolAttributes.LabelAttributes[] labelAttr = new VPFSymbolAttributes.LabelAttributes[numLabels];

        for (int i = 0; i < numLabels; i++) {
            labelAttr[i] = new VPFSymbolAttributes.LabelAttributes();
            labelAttr[i].setAttributeName(attributeNames[i]);

            Integer txtRowId = WWUtil.convertStringToInteger(txtRowIds[i]);
            if (txtRowId != null) {
                VPFSymbolAttributes.LabelAttributes tmp = this.textJoinAttributes.get(txtRowId);
                if (tmp != null) {
                    labelAttr[i].setFont(tmp.getFont());
                    labelAttr[i].setColor(tmp.getColor());
                    labelAttr[i].setBackgroundColor(tmp.getBackgroundColor());
                    labelAttr[i].setOffset(tmp.getOffset());
                    labelAttr[i].setOffsetAngle(tmp.getOffsetAngle());
                    labelAttr[i].setPrepend(tmp.getPrepend());
                    labelAttr[i].setAppend(tmp.getAppend());
                    labelAttr[i].setAbbreviationTableId(tmp.getAbbreviationTableId());
                }
            }
        }

        attr.setLabelAttributes(labelAttr);
    }

    protected static class FeatureKey {
        private final VPFFeatureClass featureClass;
        private final String featureCode;

        public FeatureKey(VPFFeatureClass featureClass, String featureCode) {
            this.featureClass = featureClass;
            this.featureCode = featureCode;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || this.getClass() != o.getClass())
                return false;

            FeatureKey that = (FeatureKey) o;

            if (!Objects.equals(this.featureCode, that.featureCode))
                return false;
            //noinspection RedundantIfStatement
            if (!Objects.equals(this.featureClass, that.featureClass))
                return false;

            return true;
        }

        public int hashCode() {
            int result = this.featureClass != null ? this.featureClass.hashCode() : 0;
            result = 31 * result + (this.featureCode != null ? this.featureCode.hashCode() : 0);
            return result;
        }
    }
}