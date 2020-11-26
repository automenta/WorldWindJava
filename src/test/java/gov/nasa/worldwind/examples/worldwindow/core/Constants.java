/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.core;

/**
 * @author tag
 * @version $Id: Constants.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface Constants {
    // Names and titles
    String APPLICATION_DISPLAY_NAME = "gov.nasa.worldwind.examples.worldwindow.ApplicationDisplayName";

    // Services
    String IMAGE_SERVICE = "gov.nasa.worldwind.examples.worldwindow.ImageService";

    // Core object IDs
    String APP_PANEL = "gov.nasa.worldwind.examples.worldwindow.AppPanel";
    String APP_FRAME = "gov.nasa.worldwind.examples.worldwindow.AppFrame";
    String CONTROLS_PANEL = "gov.nasa.worldwind.examples.worldwindow.ControlsPanel";
    String MENU_BAR = "gov.nasa.worldwind.examples.worldwindow.MenuBar";
    String NETWORK_STATUS_SIGNAL = "gov.nasa.worldwind.examples.worldwindow.NetworkStatusSignal";
    String TOOL_BAR = "gov.nasa.worldwind.examples.worldwindow.ToolBar";
    String STATUS_PANEL = "gov.nasa.worldwind.examples.worldwindow.StatusPanel";
    String WW_PANEL = "gov.nasa.worldwind.examples.worldwindow.WWPanel";

    // Miscellaneous
    String ACCELERATOR_SUFFIX = ".Accelerator";
    String ACTION_COMMAND = "gov.nasa.worldwind.examples.worldwindow.ActionCommand";
    String CONTEXT_MENU_INFO = "gov.nasa.worldwind.examples.worldwindow.ContextMenuString";
    String FILE_MENU = "gov.nasa.worldwind.examples.worldwindow.feature.FileMenu";
    String INFO_PANEL_TEXT = "gov.nasa.worldwind.examples.worldwindow.InfoPanelText";
    String ON_STATE = "gov.nasa.worldwind.examples.worldwindow.OnState";
    String RADIO_GROUP = "gov.nasa.worldwind.examples.worldwindow.StatusBarMessage";
    String STATUS_BAR_MESSAGE = "gov.nasa.worldwind.examples.worldwindow.StatusBarMessage";

    // Layer types
    String INTERNAL_LAYER = "gov.nasa.worldwind.examples.worldwindow.InternalLayer"; // application controls, etc.
    String ACTIVE_LAYER = "gov.nasa.worldwind.examples.worldwindow.ActiveLayer"; // force display in active layers
    String USER_LAYER = "gov.nasa.worldwind.examples.worldwindow.UserLayer"; // User-generated layers
    String SCREEN_LAYER = "gov.nasa.worldwind.examples.worldwindow.ScreenLayer";
    // in-screen application controls, etc.

    // Feature IDs
    String FEATURE = "gov.nasa.worldwind.examples.worldwindow.feature";
    String FEATURE_ID = "gov.nasa.worldwind.examples.worldwindow.FeatureID";
    String FEATURE_ACTIVE_LAYERS_PANEL = "gov.nasa.worldwind.examples.worldwindow.feature.ActiveLayersPanel";
    String FEATURE_COMPASS = "gov.nasa.worldwind.examples.worldwindow.feature.Compass";
    String FEATURE_CROSSHAIR = "gov.nasa.worldwind.examples.worldwindow.feature.Crosshair";
    String FEATURE_COORDINATES_DISPLAY = "gov.nasa.worldwind.examples.worldwindow.feature.CoordinatesDisplay";
    String FEATURE_EXTERNAL_LINK_CONTROLLER
        = "gov.nasa.worldwind.examples.worldwindow.feature.ExternalLinkController";
    String FEATURE_GAZETTEER = "gov.nasa.worldwind.examples.worldwindow.feature.Gazetteer";
    String FEATURE_GAZETTEER_PANEL = "gov.nasa.worldwind.examples.worldwindow.feature.GazetteerPanel";
    String FEATURE_GRATICULE = "gov.nasa.worldwind.examples.worldwindow.feature.Graticule";
    String FEATURE_ICON_CONTROLLER = "gov.nasa.worldwind.examples.worldwindow.feature.IconController";
    String FEATURE_IMPORT_IMAGERY = "gov.nasa.worldwind.examples.worldwindow.feature.ImportImagery";
    String FEATURE_INFO_PANEL_CONTROLLER = "gov.nasa.worldwind.examples.worldwindow.feature.InfoPanelController";
    String FEATURE_LAYER_MANAGER_DIALOG = "gov.nasa.worldwind.examples.worldwindow.feature.LayerManagerDialog";
    String FEATURE_LAYER_MANAGER = "gov.nasa.worldwind.examples.worldwindow.feature.LayerManager";
    String FEATURE_LAYER_MANAGER_PANEL = "gov.nasa.worldwind.examples.worldwindow.feature.LayerManagerPanel";
    String FEATURE_LATLON_GRATICULE = "gov.nasa.worldwind.examples.worldwindow.feature.LatLonGraticule";
    String FEATURE_MEASUREMENT = "gov.nasa.worldwind.examples.worldwindow.feature.Measurement";
    String FEATURE_MEASUREMENT_DIALOG = "gov.nasa.worldwind.examples.worldwindow.feature.MeasurementDialog";
    String FEATURE_MEASUREMENT_PANEL = "gov.nasa.worldwind.examples.worldwindow.feature.MeasurementPanel";
    String FEATURE_NAVIGATION = "gov.nasa.worldwind.examples.worldwindow.feature.Navigation";
    String FEATURE_OPEN_FILE = "gov.nasa.worldwind.examples.worldwindow.feature.OpenFile";
    String FEATURE_OPEN_URL = "gov.nasa.worldwind.examples.worldwindow.feature.OpenURL";
    String FEATURE_SCALE_BAR = "gov.nasa.worldwind.examples.worldwindow.feature.ScaleBar";
    String FEATURE_TOOLTIP_CONTROLLER = "gov.nasa.worldwind.examples.worldwindow.feature.ToolTipController";
    String FEATURE_UTM_GRATICULE = "gov.nasa.worldwind.examples.worldwindow.feature.UTMGraticule";
    String FEATURE_WMS_PANEL = "gov.nasa.worldwind.examples.worldwindow.feature.WMSPanel";
    String FEATURE_WMS_DIALOG = "gov.nasa.worldwind.examples.worldwindow.feature.WMSDialog";

    // Specific properties
    String FEATURE_OWNER_PROPERTY = "gov.nasa.worldwind.examples.worldwindow.FeatureOwnerProperty";
    String TOOL_BAR_ICON_SIZE_PROPERTY = "gov.nasa.worldwind.examples.worldwindow.ToolBarIconSizeProperty";
}
