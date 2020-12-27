/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.symbology.milstd2525;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.symbology.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

/**
 * Retriever to retrieve icons for symbols in the MIL-STD-2525 symbol set. The retriever can retrieve icons from either
 * a local or remote symbol store. See the <a href="https://worldwind.arc.nasa.gov/java/tutorials/tactical-symbols/#offline-use">Symbology
 * Usage Guide</a> for details on how to configure a local symbol repository. For more information on how to use this
 * class see the IconRetriever Usage Guide and the {@link gov.nasa.worldwind.examples.symbology.IconRetrieverUsage}
 * example.
 * <h2>Retrieval parameters</h2>
 * <p>
 * Table IX (pg. 35) of MIL-STD-2525C defines a hierarchy for simplifying tactical symbols. This hierarchy is
 * implemented using retrieval parameters SHOW_FILL, SHOW_FRAME, and SHOW_ICON. By default, all three elements are
 * displayed, and they can be turned off by setting the appropriate parameter. If frame and icon are turned off the
 * retriever will return an image that contains a circle, either black or filled with the icon fill color (depending on
 * the state of SHOW_FILL).
 * <p>
 * {@link #createIcon(String, AVList) createIcon} accepts the following parameters:
 * <table><caption style="font-weight: bold;">createIcon Parameters</caption> <tr><th>Key</th><th>Type</th><td><th>Description</th></tr> <tr><td>SymbologyConstants.SHOW_ICON</td><td>Boolean</td><td>Determines
 * if the symbol will be created with an icon.</td></tr> <tr><td>SymbologyConstants.SHOW_FRAME</td><td>Boolean</td><td>Determines
 * if the symbol will be created with a frame.</td></tr> <tr><td>SymbologyConstants.SHOW_FILL</td><td>Boolean</td><td>Determines
 * if the symbol will be created with a fill color.</td></tr><tr><td valign="top">AVKey.COLOR</td><td
 * valign="top">java.awt.Color</td><td valign="top">Fill color applied to the symbol. If the symbol is drawn with a
 * frame, then this color will be used to fill the frame. If the symbol is not drawn with a frame, then the fill will be
 * applied to the icon itself. The fill color has no effect if Show Fill is False.</td></tr> </table>
 *
 * @author ccrick
 * @version $Id: MilStd2525IconRetriever.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class MilStd2525IconRetriever extends AbstractIconRetriever {
    protected static final String FILLS_PATH = "fills";
    protected static final String FRAMES_PATH = "frames";
    protected static final String ICONS_PATH = "icons";
    protected static final String TACTICAL_SYMBOLS_PATH = "tacsym";
    protected static final String UNKNOWN_PATH = "unk";

    protected static final Color FILL_COLOR_LIGHT_RED = new Color(255, 128, 128);
    protected static final Color FILL_COLOR_LIGHT_BLUE = new Color(128, 224, 255);
    protected static final Color FILL_COLOR_LIGHT_GREEN = new Color(170, 255, 170);
    protected static final Color FILL_COLOR_LIGHT_YELLOW = new Color(255, 255, 128);
    protected static final Color FILL_COLOR_LIGHT_PURPLE = new Color(255, 161, 255);

    protected static final Color FRAME_COLOR_RED = new Color(255, 0, 0);
    protected static final Color FRAME_COLOR_BLUE = new Color(0, 255, 255);
    protected static final Color FRAME_COLOR_GREEN = new Color(0, 255, 0);
    protected static final Color FRAME_COLOR_YELLOW = new Color(255, 255, 0);
    protected static final Color FRAME_COLOR_PURPLE = new Color(255, 0, 255);

    protected static final Color ICON_COLOR_RED = new Color(255, 0, 0);
    protected static final Color ICON_COLOR_ORANGE = new Color(255, 140, 0);
    protected static final Color ICON_COLOR_GREEN = new Color(0, 255, 0);
    protected static final Color ICON_COLOR_DARK_GREEN = new Color(0, 128, 0);
    protected static final Color ICON_COLOR_YELLOW = new Color(255, 255, 0);

    protected static final Color DEFAULT_FRAME_COLOR = Color.BLACK;
    protected static final Color DEFAULT_ICON_COLOR = Color.BLACK;
    protected static final String DEFAULT_IMAGE_FORMAT = "image/png";

    /**
     * Radius (in pixels) of circle that is drawn to the represent the symbol when both frame and icon are off.
     */
    protected static final int CIRCLE_RADIUS = 16;
    /**
     * Line width used to stroke circle when fill is turned off.
     */
    protected static final int CIRCLE_LINE_WIDTH = 2;

    // Static maps and sets providing fast access to attributes about a symbol ID. These data structures are populated
    // in a static block at the bottom of this class.
    protected static final Map<String, String> schemePathMap = new HashMap<>();
    protected static final Map<String, Color> fillColorMap = new HashMap<>();
    protected static final Map<String, Color> frameColorMap = new HashMap<>();
    protected static final Map<String, Color> iconColorMap = new HashMap<>();
    protected static final Collection<String> unfilledIconMap = new HashSet<>();
    protected static final Collection<String> unframedIconMap = new HashSet<>();
    protected static final Collection<String> emsEquipment = new HashSet<>();

    static {
        MilStd2525IconRetriever.schemePathMap.put("s", "war"); // Scheme Warfighting
        MilStd2525IconRetriever.schemePathMap.put("i", "sigint"); // Scheme Signals Intelligence
        MilStd2525IconRetriever.schemePathMap.put("o", "stbops"); // Scheme Stability Operations
        MilStd2525IconRetriever.schemePathMap.put("e", "ems"); // Scheme Emergency Management

        // The MIL-STD-2525 symbol fill colors for each Standard Identity.
        MilStd2525IconRetriever.fillColorMap.put("p", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW); // Standard Identity Pending
        MilStd2525IconRetriever.fillColorMap.put("u", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW); // Standard Identity Unknown
        MilStd2525IconRetriever.fillColorMap.put("f", MilStd2525IconRetriever.FILL_COLOR_LIGHT_BLUE); // Standard Identity Friend
        MilStd2525IconRetriever.fillColorMap.put("n", MilStd2525IconRetriever.FILL_COLOR_LIGHT_GREEN); // Standard Identity Neutral
        MilStd2525IconRetriever.fillColorMap.put("h", MilStd2525IconRetriever.FILL_COLOR_LIGHT_RED); // Standard Identity Hostile
        MilStd2525IconRetriever.fillColorMap.put("a", MilStd2525IconRetriever.FILL_COLOR_LIGHT_BLUE); // Standard Identity Assumed Friend
        MilStd2525IconRetriever.fillColorMap.put("s", MilStd2525IconRetriever.FILL_COLOR_LIGHT_RED); // Standard Identity Suspect
        MilStd2525IconRetriever.fillColorMap.put("g", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW); // Standard Identity Exercise Pending
        MilStd2525IconRetriever.fillColorMap.put("w", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW); // Standard Identity Exercise Unknown
        MilStd2525IconRetriever.fillColorMap.put("d", MilStd2525IconRetriever.FILL_COLOR_LIGHT_BLUE); // Standard Identity Exercise Friend
        MilStd2525IconRetriever.fillColorMap.put("l", MilStd2525IconRetriever.FILL_COLOR_LIGHT_GREEN); // Standard Identity Exercise Neutral
        MilStd2525IconRetriever.fillColorMap.put("m", MilStd2525IconRetriever.FILL_COLOR_LIGHT_BLUE); // Standard Identity Exercise Assumed Friend
        MilStd2525IconRetriever.fillColorMap.put("j", MilStd2525IconRetriever.FILL_COLOR_LIGHT_RED); // Standard Identity Joker
        MilStd2525IconRetriever.fillColorMap.put("k", MilStd2525IconRetriever.FILL_COLOR_LIGHT_RED); // Standard Identity Faker

        // The MIL-STD-2525 symbol frame colors for each Standard Identity.
        MilStd2525IconRetriever.frameColorMap.put("p", MilStd2525IconRetriever.FRAME_COLOR_YELLOW); // Standard Identity Pending
        MilStd2525IconRetriever.frameColorMap.put("u", MilStd2525IconRetriever.FRAME_COLOR_YELLOW); // Standard Identity Unknown
        MilStd2525IconRetriever.frameColorMap.put("f", MilStd2525IconRetriever.FRAME_COLOR_BLUE); // Standard Identity Friend
        MilStd2525IconRetriever.frameColorMap.put("n", MilStd2525IconRetriever.FRAME_COLOR_GREEN); // Standard Identity Neutral
        MilStd2525IconRetriever.frameColorMap.put("h", MilStd2525IconRetriever.FRAME_COLOR_RED); // Standard Identity Hostile
        MilStd2525IconRetriever.frameColorMap.put("a", MilStd2525IconRetriever.FRAME_COLOR_BLUE); // Standard Identity Assumed Friend
        MilStd2525IconRetriever.frameColorMap.put("s", MilStd2525IconRetriever.FRAME_COLOR_RED); // Standard Identity Suspect
        MilStd2525IconRetriever.frameColorMap.put("g", MilStd2525IconRetriever.FRAME_COLOR_YELLOW); // Standard Identity Exercise Pending
        MilStd2525IconRetriever.frameColorMap.put("w", MilStd2525IconRetriever.FRAME_COLOR_YELLOW); // Standard Identity Exercise Unknown
        MilStd2525IconRetriever.frameColorMap.put("d", MilStd2525IconRetriever.FRAME_COLOR_BLUE); // Standard Identity Exercise Friend
        MilStd2525IconRetriever.frameColorMap.put("l", MilStd2525IconRetriever.FRAME_COLOR_GREEN); // Standard Identity Exercise Neutral
        MilStd2525IconRetriever.frameColorMap.put("m", MilStd2525IconRetriever.FRAME_COLOR_BLUE); // Standard Identity Exercise Assumed Friend
        MilStd2525IconRetriever.frameColorMap.put("j", MilStd2525IconRetriever.FRAME_COLOR_RED); // Standard Identity Joker
        MilStd2525IconRetriever.frameColorMap.put("k", MilStd2525IconRetriever.FRAME_COLOR_RED); // Standard Identity Faker

        // The MIL-STD-2525 symbol icon colors for each icon that has either a white or colored fill. White is denoted
        // as a null value.
        MilStd2525IconRetriever.iconColorMap.put("e-f-a----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-aa---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ab---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ad---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ag---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ba---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-bb---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-bc---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-bd---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-c----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ca---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-cb---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-cc---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-cd---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ce---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-cf---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-cg---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ch---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ci---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-cj---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ee---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-f----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-g----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-h----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ha---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-hb---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ia---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-id---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-jb---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-ld---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-le---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-lf---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-lm---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-lo---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-lp---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-me---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-mf---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-mg---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-mh---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-f-mi---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-b----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-ca---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-cc---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-d----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-da---------", new Color(255, 254, 111));
        MilStd2525IconRetriever.iconColorMap.put("e-i-dc---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-dd---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-de---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-df---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-dg---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-dh---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-di---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-dj---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-dm---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-e----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-ea---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-f----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-i-fa---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-ae---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-af---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-aj---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-ak---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-am---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-b----------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-ba---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-bb---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-bc---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-bd---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-be---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-bf---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-bg---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-bh---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-bi---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-bj---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-cc---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-cd---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-de---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dea--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-deb--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dec--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-df---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dfa--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dfb--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dfc--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dk---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dn---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dna--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dnc--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-do---------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-doa--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-dob--------", null);
        MilStd2525IconRetriever.iconColorMap.put("e-o-doc--------", null);
        MilStd2525IconRetriever.iconColorMap.put("o-o-ha---------", null);
        MilStd2525IconRetriever.iconColorMap.put("o-o-hv---------", null);
        MilStd2525IconRetriever.iconColorMap.put("o-o-y----------", null);
        MilStd2525IconRetriever.iconColorMap.put("o-o-yh---------", null);
        MilStd2525IconRetriever.iconColorMap.put("o-o-yt---------", null);
        MilStd2525IconRetriever.iconColorMap.put("o-o-yw---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-a-cf---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-a-ch---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-a-cl---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-a-w----------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wm---------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wma--------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmaa-------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmap-------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmas-------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmb--------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmcm-------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wms--------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmsa-------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmsb-------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmss-------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmsu-------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-a-wmu--------", MilStd2525IconRetriever.FILL_COLOR_LIGHT_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-f-gp---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-f-gpa--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-f-nb---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evca-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcah------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcal------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcam------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcf-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcfh------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcfl------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcfm------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcj-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcjh------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcjl------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcjm------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcm-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcmh------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcml------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcmm------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evco-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcoh------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcol------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcom------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evct-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcth------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evctl------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evctm------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcu-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcuh------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcul------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-evcum------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-ucfs-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-ucfsa------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-ucfsl------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-ucfso------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-ucfss------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-ucfts------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-uumrs------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-uumrss-----", null);
        MilStd2525IconRetriever.iconColorMap.put("s-g-uusx-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-p-t----------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-c----------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-nh---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xa---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xar--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xas--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xf---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xfdf-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xfdr-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xftr-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xh---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xl---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xm---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xmc--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xmf--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xmh--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xmo--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xmp--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xmr--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xmto-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xmtu-------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xp---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-s-xr---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-e----------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-nd---------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-sca--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-scb--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-scg--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-scm--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-sna--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-snb--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-sng--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-snm--------", null);
        MilStd2525IconRetriever.iconColorMap.put("s-u-v----------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wdm--------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wdmg-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wdmm-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wm---------", MilStd2525IconRetriever.ICON_COLOR_RED);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wma--------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmb--------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmbd-------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmc--------", MilStd2525IconRetriever.ICON_COLOR_ORANGE);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmd--------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wme--------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmf--------", MilStd2525IconRetriever.ICON_COLOR_RED);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmfc-------", MilStd2525IconRetriever.ICON_COLOR_ORANGE);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmfd-------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmfe-------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmfo-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmfr-------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmfx-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmg--------", MilStd2525IconRetriever.ICON_COLOR_RED);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmgc-------", MilStd2525IconRetriever.ICON_COLOR_ORANGE);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmgd-------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmge-------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmgo-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmgr-------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmgx-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmm--------", MilStd2525IconRetriever.ICON_COLOR_RED);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmmc-------", MilStd2525IconRetriever.ICON_COLOR_ORANGE);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmmd-------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmme-------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmmo-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmmr-------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmmx-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmn--------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmo--------", MilStd2525IconRetriever.ICON_COLOR_RED);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmod-------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmr--------", MilStd2525IconRetriever.ICON_COLOR_YELLOW);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wms--------", MilStd2525IconRetriever.ICON_COLOR_RED);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmsd-------", MilStd2525IconRetriever.ICON_COLOR_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmsx-------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-wmx--------", MilStd2525IconRetriever.ICON_COLOR_DARK_GREEN);
        MilStd2525IconRetriever.iconColorMap.put("s-u-x----------", MilStd2525IconRetriever.ICON_COLOR_RED);

        // The MIL-STD-2525 symbol icons that are implicitly unfilled.
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wm---------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmd--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmg--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmgd-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmgx-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmge-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmgc-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmgr-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmgo-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmm--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmmd-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmmx-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmme-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmmc-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmmr-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmmo-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmf--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmfd-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmfx-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmfe-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmfc-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmfr-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmfo-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmo--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmod-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmx--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wme--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wma--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmc--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmr--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmb--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmbd-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmn--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wms--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmsx-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wmsd-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wdm--------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wdmg-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-wdmm-------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-e----------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-v----------");
        MilStd2525IconRetriever.unfilledIconMap.add("s-u-x----------");

        // The MIL-STD-2525 symbol icons that are implicitly unframed.
        MilStd2525IconRetriever.unframedIconMap.add("s-s-o----------");
        MilStd2525IconRetriever.unframedIconMap.add("s-u-nd---------");

        // The MIL-STD-2525 Emergency Management symbols representing units.
        MilStd2525IconRetriever.emsEquipment.add("e-o-ab---------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-ae---------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-af---------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-bb---------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-cb---------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-cc---------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-db---------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-ddb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-deb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-dfb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-dgb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-dhb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-dib--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-djb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-dlb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-dmb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-dob--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-pea--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-peb--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-pec--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-ped--------");
        MilStd2525IconRetriever.emsEquipment.add("e-o-pee--------");
        MilStd2525IconRetriever.emsEquipment.add("e-f-ba---------");
        MilStd2525IconRetriever.emsEquipment.add("e-f-ma---------");
        MilStd2525IconRetriever.emsEquipment.add("e-f-mc---------");
    }

    /**
     * Create a new retriever that will retrieve icons from the specified location. The retrieval path may be a file URL
     * to a directory on the local file system (for example, file:///symbols/mil-std-2525). A URL to a network resource
     * (http://myserver.com/milstd2525/), or a URL to a JAR or ZIP file (jar:file:milstd2525-symbols.zip!).
     *
     * @param retrieverPath File path or URL to the symbol directory, for example "http://myserver.com/milstd2525/".
     */
    public MilStd2525IconRetriever(String retrieverPath) {
        super(retrieverPath);
    }

    protected static boolean mustDrawFill(SymbolCode symbolCode, AVList params) {
        String maskedCode = symbolCode.toMaskedString().toLowerCase();
        if (MilStd2525IconRetriever.unfilledIconMap.contains(maskedCode))
            return false;

        Object o = params != null ? params.get(SymbologyConstants.SHOW_FILL) : null;
        return o == null || o.equals(Boolean.TRUE);
    }

    protected static boolean mustDrawFrame(SymbolCode symbolCode, AVList params) {
        String maskedCode = symbolCode.toMaskedString().toLowerCase();
        if (MilStd2525IconRetriever.unframedIconMap.contains(maskedCode))
            return false;

        Object o = params != null ? params.get(SymbologyConstants.SHOW_FRAME) : null;
        return o == null || o.equals(Boolean.TRUE);
    }

    @SuppressWarnings("UnusedParameters")
    protected static boolean mustDrawIcon(SymbolCode symbolCode, AVList params) {
        Object o = params != null ? params.get(SymbologyConstants.SHOW_ICON) : null;
        return o == null || o.equals(Boolean.TRUE);
    }

    protected static String composeFillPath(SymbolCode symbolCode) {
        String maskedCode = MilStd2525IconRetriever.getMaskedFillCode(symbolCode);

        StringBuilder sb = new StringBuilder();
        sb.append(MilStd2525IconRetriever.FILLS_PATH).append('/');
        sb.append(MilStd2525IconRetriever.TACTICAL_SYMBOLS_PATH).append('/');
        sb.append(maskedCode.toLowerCase());
        sb.append(WWIO.mimeSuffix(MilStd2525IconRetriever.DEFAULT_IMAGE_FORMAT));

        return sb.toString();
    }

    protected static String composeFramePath(SymbolCode symbolCode) {
        String maskedCode = MilStd2525IconRetriever.getMaskedFrameCode(symbolCode);

        StringBuilder sb = new StringBuilder();
        sb.append(MilStd2525IconRetriever.FRAMES_PATH).append('/');
        sb.append(MilStd2525IconRetriever.TACTICAL_SYMBOLS_PATH).append('/');
        sb.append(maskedCode.toLowerCase());
        sb.append(WWIO.mimeSuffix(MilStd2525IconRetriever.DEFAULT_IMAGE_FORMAT));

        return sb.toString();
    }

    protected static String composeIconPath(SymbolCode symbolCode, AVList params) {
        String scheme = symbolCode.getScheme();
        String bd = symbolCode.getBattleDimension();

        if (bd != null && bd.equalsIgnoreCase(SymbologyConstants.BATTLE_DIMENSION_UNKNOWN)) {
            String maskedCode = MilStd2525IconRetriever.getMaskedUnknownIconCode(symbolCode, params);
            StringBuilder sb = new StringBuilder();
            sb.append(MilStd2525IconRetriever.ICONS_PATH).append('/');
            sb.append(MilStd2525IconRetriever.UNKNOWN_PATH).append('/');
            sb.append(maskedCode.toLowerCase());
            sb.append(WWIO.mimeSuffix(MilStd2525IconRetriever.DEFAULT_IMAGE_FORMAT));
            return sb.toString();
        } else {
            if (SymbolCode.isFieldEmpty(symbolCode.getFunctionId()))
                return null; // Don't draw an icon if the function ID is empty.

            String maskedCode = MilStd2525IconRetriever.getMaskedIconCode(symbolCode, params);
            StringBuilder sb = new StringBuilder();
            sb.append(MilStd2525IconRetriever.ICONS_PATH).append('/');
            sb.append(MilStd2525IconRetriever.schemePathMap.get(scheme.toLowerCase())).append('/');
            sb.append(maskedCode.toLowerCase());
            sb.append(WWIO.mimeSuffix(MilStd2525IconRetriever.DEFAULT_IMAGE_FORMAT));
            return sb.toString();
        }
    }

    protected static String getMaskedFillCode(SymbolCode symbolCode) {
        // Transform the symbol code to its equivalent code in the Warfighting scheme. This ensures that we can use
        // the generic fill shape lookup logic used by Warfighting symbols.
        symbolCode = MilStd2525IconRetriever.transformToWarfightingScheme(symbolCode);

        String si = MilStd2525IconRetriever.getSimpleStandardIdentity(
            symbolCode); // Either Unknown, Friend, Neutral, or Hostile
        String bd = symbolCode.getBattleDimension();
        String fid = MilStd2525IconRetriever.getGroundFunctionId(symbolCode);

        StringBuilder sb = new StringBuilder();
        SymbolCode.appendFieldValue(sb, null, 1); // Scheme
        SymbolCode.appendFieldValue(sb, si, 1); // Standard Identity
        SymbolCode.appendFieldValue(sb, bd, 1); // Battle Dimension
        SymbolCode.appendFieldValue(sb, null, 1); // Status
        SymbolCode.appendFieldValue(sb, fid, 6); // Function ID
        SymbolCode.appendFieldValue(sb, null, 2); // Symbol Modifier
        SymbolCode.appendFieldValue(sb, null, 2); // Country Code
        SymbolCode.appendFieldValue(sb, null, 1); // Order of Battle

        return sb.toString();
    }

    protected static String getMaskedFrameCode(SymbolCode symbolCode) {
        // Transform the symbol code to its equivalent code in the Warfighting scheme. This ensures that we can use
        // the generic fill shape lookup logic used by Warfighting symbols.
        symbolCode = MilStd2525IconRetriever.transformToWarfightingScheme(symbolCode);

        String si = symbolCode.getStandardIdentity();
        String bd = symbolCode.getBattleDimension();
        String status = MilStd2525IconRetriever.getSimpleStatus(symbolCode); // Either Present or Anticipated
        String fid = MilStd2525IconRetriever.getGroundFunctionId(
            symbolCode); // Either "U-----", "E-----", "I-----", or null

        StringBuilder sb = new StringBuilder();
        SymbolCode.appendFieldValue(sb, null, 1); // Scheme
        SymbolCode.appendFieldValue(sb, si, 1); // Standard Identity
        SymbolCode.appendFieldValue(sb, bd, 1); // Battle Dimension
        SymbolCode.appendFieldValue(sb, status, 1); // Status
        SymbolCode.appendFieldValue(sb, fid, 6); // Function ID
        SymbolCode.appendFieldValue(sb, null, 2); // Symbol Modifier
        SymbolCode.appendFieldValue(sb, null, 2); // Country Code
        SymbolCode.appendFieldValue(sb, null, 1); // Order of Battle

        return sb.toString();
    }

    protected static SymbolCode transformToWarfightingScheme(SymbolCode symbolCode) {
        String maskedCode = symbolCode.toMaskedString().toLowerCase();
        String scheme = symbolCode.getScheme();
        String bd = symbolCode.getBattleDimension();

        SymbolCode newCode = new SymbolCode();
        newCode.setScheme(SymbologyConstants.SCHEME_WARFIGHTING);
        newCode.setStandardIdentity(symbolCode.getStandardIdentity());
        newCode.setStatus(symbolCode.getStatus());

        if (scheme != null && scheme.equalsIgnoreCase(SymbologyConstants.SCHEME_INTELLIGENCE)) {
            newCode.setBattleDimension(bd);

            // Signals Intelligence ground symbols are equivalent to Warfighting ground equipment.
            if (bd != null && bd.equalsIgnoreCase(SymbologyConstants.BATTLE_DIMENSION_GROUND))
                newCode.setFunctionId("E-----");

            return newCode;
        } else if (scheme != null && scheme.equalsIgnoreCase(SymbologyConstants.SCHEME_STABILITY_OPERATIONS)) {
            // Stability Operations symbols frames are equivalent to Warfighting ground units.
            newCode.setBattleDimension(SymbologyConstants.BATTLE_DIMENSION_GROUND);
            newCode.setFunctionId("U-----");

            return newCode;
        } else if (scheme != null && scheme.equalsIgnoreCase(SymbologyConstants.SCHEME_EMERGENCY_MANAGEMENT)) {
            // Emergency Management symbol frames are equivalent to either Warfighting ground units or ground equipment.
            newCode.setBattleDimension(SymbologyConstants.BATTLE_DIMENSION_GROUND);
            newCode.setFunctionId(MilStd2525IconRetriever.emsEquipment.contains(maskedCode) ? "E-----" : "U-----");

            return newCode;
        } else {
            return symbolCode;
        }
    }

    protected static String getMaskedIconCode(SymbolCode symbolCode, AVList params) {
        String si = MilStd2525IconRetriever.getSimpleStandardIdentity(
            symbolCode); // Either Unknown, Friend, Neutral, or Hostile.
        String status = MilStd2525IconRetriever.getSimpleStatus(symbolCode); // Either Present or Anticipated.

        if (MilStd2525IconRetriever.mustDrawFrame(symbolCode, params))
            status = SymbologyConstants.STATUS_PRESENT;

        SymbolCode maskedCode = new SymbolCode(symbolCode.toString());
        maskedCode.setStandardIdentity(si);
        maskedCode.setStatus(status);
        maskedCode.setSymbolModifier(null); // Ignore the Symbol Modifier field.
        maskedCode.setCountryCode(null); // Ignore the Country Code field.
        maskedCode.setOrderOfBattle(null); // Ignore the Order of Battle field.

        return maskedCode.toString();
    }

    protected static String getMaskedUnknownIconCode(SymbolCode symbolCode, AVList params) {
        String si = MilStd2525IconRetriever.getSimpleStandardIdentity(
            symbolCode); // Either Unknown, Friend, Neutral, or Hostile.
        String bd = symbolCode.getBattleDimension();
        String status = MilStd2525IconRetriever.getSimpleStatus(symbolCode); // Either Present or Anticipated.

        if (MilStd2525IconRetriever.mustDrawFrame(symbolCode, params))
            status = SymbologyConstants.STATUS_PRESENT;

        StringBuilder sb = new StringBuilder();
        SymbolCode.appendFieldValue(sb, null, 1); // Scheme
        SymbolCode.appendFieldValue(sb, si, 1); // Standard Identity
        SymbolCode.appendFieldValue(sb, bd, 1); // Battle Dimension
        SymbolCode.appendFieldValue(sb, status, 1); // Status
        SymbolCode.appendFieldValue(sb, null, 6); // Function ID
        SymbolCode.appendFieldValue(sb, null, 2); // Symbol Modifier
        SymbolCode.appendFieldValue(sb, null, 2); // Country Code
        SymbolCode.appendFieldValue(sb, null, 1); // Order of Battle

        return sb.toString();
    }

    protected static boolean isDashedFrame(SymbolCode symbolCode) {
        String si = symbolCode.getStandardIdentity();
        return si != null && (si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_PENDING)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_ASSUMED_FRIEND)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_SUSPECT)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_EXERCISE_PENDING)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_EXERCISE_ASSUMED_FRIEND));
    }

    protected static String getSimpleStandardIdentity(SymbolCode symbolCode) {
        String si = symbolCode.getStandardIdentity();
        if (si != null && (si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_PENDING)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_UNKNOWN)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_EXERCISE_PENDING)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_EXERCISE_UNKNOWN))) {
            return SymbologyConstants.STANDARD_IDENTITY_UNKNOWN;
        } else if (si != null && (si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_FRIEND)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_ASSUMED_FRIEND)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_EXERCISE_FRIEND)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_EXERCISE_ASSUMED_FRIEND)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_JOKER)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_FAKER))) {
            return SymbologyConstants.STANDARD_IDENTITY_FRIEND;
        } else if (si != null && (si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_NEUTRAL)
            || si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_EXERCISE_NEUTRAL))) {
            return SymbologyConstants.STANDARD_IDENTITY_NEUTRAL;
        } else if (si != null && (si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_HOSTILE) ||
            si.equalsIgnoreCase(SymbologyConstants.STANDARD_IDENTITY_SUSPECT))) {
            return SymbologyConstants.STANDARD_IDENTITY_HOSTILE;
        }

        return si;
    }

    protected static String getSimpleStatus(SymbolCode symbolCode) {
        String status = symbolCode.getStatus();

        if (status != null && status.equalsIgnoreCase(SymbologyConstants.STATUS_ANTICIPATED))
            return SymbologyConstants.STATUS_ANTICIPATED;
        else
            return SymbologyConstants.STATUS_PRESENT;
    }

    protected static String getGroundFunctionId(SymbolCode symbolCode) {
        String scheme = symbolCode.getScheme();
        String bd = symbolCode.getBattleDimension();
        String fid = symbolCode.getFunctionId();

        if (scheme != null && scheme.equalsIgnoreCase(SymbologyConstants.SCHEME_WARFIGHTING)
            && bd != null && bd.equalsIgnoreCase(SymbologyConstants.BATTLE_DIMENSION_GROUND)) {
            if (fid != null && !fid.isEmpty() && fid.toLowerCase().charAt(0) == 'u')
                return "u-----";
            else if (fid != null && !fid.isEmpty() && fid.toLowerCase().charAt(0) == 'e')
                return "e-----";
            else if (fid != null && !fid.isEmpty() && fid.toLowerCase().charAt(0) == 'i')
                return "i-----";
        }

        return null;
    }

    /**
     * Create an icon for a MIL-STD-2525C symbol. By default the symbol will include a filled frame and an icon. The
     * fill, frame, and icon can be turned off by setting retrieval parameters. If both frame and icon are turned off
     * then this method will return an image containing a circle.
     *
     * @param sidc   SIDC identifier for the symbol.
     * @param params Parameters that affect icon retrieval. See <a href="#parameters">Parameters</a> in class
     *               documentation.
     * @return An BufferedImage containing the icon for the requested symbol, or null if the icon cannot be retrieved.
     */
    public BufferedImage createIcon(String sidc, AVList params) {
        if (sidc == null) {
            String msg = Logging.getMessage("nullValue.SymbolCodeIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        SymbolCode symbolCode = new SymbolCode(sidc);
        BufferedImage image = null;

        boolean mustDrawFill = MilStd2525IconRetriever.mustDrawFill(symbolCode, params);
        boolean mustDrawIcon = MilStd2525IconRetriever.mustDrawIcon(symbolCode, params);
        boolean mustDrawFrame = MilStd2525IconRetriever.mustDrawFrame(symbolCode, params);

        if (mustDrawFrame || mustDrawIcon) {
            if (mustDrawFill && mustDrawFrame)
                image = this.drawFill(symbolCode, params, null);

            if (mustDrawFrame)
                image = this.drawFrame(symbolCode, params, image);

            if (mustDrawIcon)
                image = this.drawIcon(symbolCode, params, image);
        }

        // Draw a dot if both frame and icon are turned off
        if (image == null)
            image = this.drawCircle(symbolCode, params, image);

        return image;
    }

    protected BufferedImage drawFill(SymbolCode symbolCode, AVList params, BufferedImage dest) {
        String path = MilStd2525IconRetriever.composeFillPath(symbolCode);
        Color color = this.getFillColor(symbolCode, params);

        return path != null ? this.drawIconComponent(path, color, dest) : dest;
    }

    protected BufferedImage drawFrame(SymbolCode symbolCode, AVList params, BufferedImage dest) {
        String path = MilStd2525IconRetriever.composeFramePath(symbolCode);
        Color color = this.getFrameColor(symbolCode, params);

        return path != null ? this.drawIconComponent(path, color, dest) : dest;
    }

    protected BufferedImage drawIcon(SymbolCode symbolCode, AVList params, BufferedImage dest) {
        String path = MilStd2525IconRetriever.composeIconPath(symbolCode, params);
        Color color = this.getIconColor(symbolCode, params);

        return path != null ? this.drawIconComponent(path, color, dest) : dest;
    }

    protected BufferedImage drawCircle(SymbolCode symbolCode, AVList params, BufferedImage dest) {
        Color fillColor = MilStd2525IconRetriever.mustDrawFill(symbolCode, params) ? this.getFillColor(symbolCode,
            params)
            : MilStd2525IconRetriever.DEFAULT_ICON_COLOR;

        if (dest == null) {
            int diameter = MilStd2525IconRetriever.CIRCLE_RADIUS * 2;
            dest = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g = null;
        try {
            g = dest.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int upperLeft = (int) (dest.getWidth() / 2.0 - MilStd2525IconRetriever.CIRCLE_RADIUS + MilStd2525IconRetriever.CIRCLE_LINE_WIDTH);
            int width = MilStd2525IconRetriever.CIRCLE_RADIUS * 2 - MilStd2525IconRetriever.CIRCLE_LINE_WIDTH * 2;
            @SuppressWarnings("SuspiciousNameCombination")
            Shape circle = new Ellipse2D.Double(upperLeft, upperLeft, width, width);

            // Draw filled circle
            g.setColor(fillColor);
            g.fill(circle);

            // Draw the circle's border. Always draw the circle with a solid border, even if the status is not Present.
            // MIL-STD-2525C section 5.3.1.4 (pg. 18) states: "Planned status cannot be shown if the symbol is [...]
            // displayed as a dot."
            g.setColor(MilStd2525IconRetriever.DEFAULT_FRAME_COLOR);
            g.setStroke(new BasicStroke(MilStd2525IconRetriever.CIRCLE_LINE_WIDTH));
            g.draw(circle);
        }
        finally {
            if (g != null)
                g.dispose();
        }

        return dest;
    }

    protected BufferedImage drawIconComponent(String path, Color color, BufferedImage dest) {
        BufferedImage image = this.readImage(path);
        if (image == null) {
            String msg = Logging.getMessage("Symbology.MissingIconComponent", path);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (color != null)
            AbstractIconRetriever.multiply(image, color);

        if (dest != null)
            image = AbstractIconRetriever.drawImage(image, dest);

        return image;
    }

    protected Color getFillColor(SymbolCode symbolCode, AVList params) {
        Color color = this.getColorFromParams(params);
        return color != null ? color : MilStd2525IconRetriever.fillColorMap.get(symbolCode.getStandardIdentity().toLowerCase());
    }

    protected Color getFrameColor(SymbolCode symbolCode, AVList params) {
        if (MilStd2525IconRetriever.isDashedFrame(symbolCode))
            return null; // Dashed pending or exercise frames are not colored.

        if (MilStd2525IconRetriever.mustDrawFill(symbolCode, params))
            return MilStd2525IconRetriever.DEFAULT_FRAME_COLOR; // Use the default color if the fill is on.

        Color color = this.getColorFromParams(params);
        return color != null ? color : MilStd2525IconRetriever.frameColorMap.get(symbolCode.getStandardIdentity().toLowerCase());
    }

    protected Color getIconColor(SymbolCode symbolCode, AVList params) {
        String maskedCode = symbolCode.toMaskedString().toLowerCase();

        if (MilStd2525IconRetriever.mustDrawFrame(symbolCode, params)) {
            // When the frame is enabled, we draw the icon in either its specified custom color or the default color. In
            // this case the app-specified color override (if any) is applied to the frame, and does apply to the icon.
            return MilStd2525IconRetriever.iconColorMap.getOrDefault(maskedCode, MilStd2525IconRetriever.DEFAULT_ICON_COLOR);
        } else if (MilStd2525IconRetriever.mustDrawFill(symbolCode, params)) {
            // When the frame is disabled and the fill is enabled, we draw the icon in its corresponding standard
            // identity color (or app-specified color override).
            Color color = this.getColorFromParams(params);
            return color != null ? color : MilStd2525IconRetriever.fillColorMap.get(symbolCode.getStandardIdentity().toLowerCase());
        } else {
            // When the frame is disabled and the fill is disabled, we draw the icon in either its specified custom
            // color or the default color. In this case the app-specified color override (if any) is ignored.
            return MilStd2525IconRetriever.iconColorMap.getOrDefault(maskedCode, MilStd2525IconRetriever.DEFAULT_ICON_COLOR);
        }
    }

    /**
     * Retrieves the value of the AVKey.COLOR parameter.
     *
     * @param params Parameter list.
     * @return The value of the AVKey.COLOR parameter, if such a parameter exists and is of type java.awt.Color. Returns
     * null if the parameter list is null, if there is no value for key AVKey.COLOR, or if the value is not a Color.
     */
    protected Color getColorFromParams(AVList params) {
        if (params == null)
            return null;

        Object o = params.get(AVKey.COLOR);
        return (o instanceof Color) ? (Color) o : null;
    }
}

