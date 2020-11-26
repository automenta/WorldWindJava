/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.avlist;

/**
 * @author Tom Gaskins
 * @version $Id: AVKey.java 2375 2014-10-10 23:35:05Z tgaskins $
 */
public interface AVKey // TODO: Eliminate unused constants, if any
{
    // NOTE: Keep all keys in alphabetical order except where noted

    // Direction constants
    String NORTHWEST = "gov.nasa.worldwind.layers.ViewControlsLayer.NorthWest";
    String SOUTHWEST = "gov.nasa.worldwind.layers.ViewControlsLayer.SouthWest";
    String NORTHEAST = "gov.nasa.worldwind.layers.ViewControlsLayer.NorthEast";
    String SOUTHEAST = "gov.nasa.worldwind.layers.ViewControlsLayer.SouthEast";

    // Start alphabetic order
    String ABOVE_GROUND_LEVEL = "gov.nasa.worldwind.avkey.AboveGroundLevel";
    String ABOVE_GROUND_REFERENCE = "gov.nasa.worldwind.avkey.AboveGroundReference";
    String ABOVE_MEAN_SEA_LEVEL = "gov.nasa.worldwind.avkey.AboveMeanSeaLevel";
    String ACTION = "gov.nasa.worldwind.avkey.Action";
    String AIRSPACE_GEOMETRY_CACHE_SIZE = "gov.nasa.worldwind.avkey.AirspaceGeometryCacheSize";
    String ALLOW = "gov.nasa.worldwind.avkey.Allow";
    String ANIMATION_META_DATA = "gov.nasa.worldwind.animation.Metadata";
    String ANIMATION_ANNOTATION = "gov.nasa.worldwind.animation.Annotation";
    String AUTH_TOKEN = "gov.nasa.worldwind.avkey.AuthToken";

    String AVAILABLE_IMAGE_FORMATS = "gov.nasa.worldwind.avkey.AvailableImageFormats";
    String AVERAGE_TILE_SIZE = "gov.nasa.worldwind.avkey.AverageTileSize";

    String BALLOON = "gov.nasa.worldwind.avkey.Balloon";
    String BALLOON_TEXT = "gov.nasa.worldwind.avkey.BalloonText";
    String BACK = "gov.nasa.worldwind.avkey.Back";
    String BEGIN = "gov.nasa.worldwind.avkey.Begin";
    String BIG_ENDIAN = "gov.nasa.worldwind.avkey.BigEndian";
    String BOTTOM = "gov.nasa.worldwind.avkey.Bottom";
    String BYTE_ORDER = "gov.nasa.worldwind.avkey.ByteOrder";
    String BANDS_ORDER = "gov.nasa.worldwind.avkey.BandsOrder";

    String BLACK_GAPS_DETECTION = "gov.nasa.worldwind.avkey.DetectBlackGaps";
    String BOUNDS = "gov.nasa.worldwind.avkey.Bounds";

    String CACHE_CONTENT_TYPES = "gov.nasa.worldwind.avkey.CacheContentTypes";
    String CENTER = "gov.nasa.worldwind.avkey.Center";

    String CLASS_LEVEL = "gov.nasa.worldwind.avkey.ClassLevel";
    String CLASS_LEVEL_UNCLASSIFIED = "gov.nasa.worldwind.avkey.ClassLevel.Unclassified";
    String CLASS_LEVEL_RESTRICTED = "gov.nasa.worldwind.avkey.ClassLevel.Restricted";
    String CLASS_LEVEL_CONFIDENTIAL = "gov.nasa.worldwind.avkey.ClassLevel.Confidential";
    String CLASS_LEVEL_SECRET = "gov.nasa.worldwind.avkey.ClassLevel.Secret";
    String CLASS_LEVEL_TOPSECRET = "gov.nasa.worldwind.avkey.ClassLevel.TopSecret";

    String CLOCKWISE = "gov.nasa.worldwind.avkey.ClockWise";
    String CLOSE = "gov.nasa.worldwind.avkey.Close";
    String COLOR = "gov.nasa.worldwind.avkey.Color";
    String COMPRESS_TEXTURES = "gov.nasa.worldwind.avkey.CompressTextures";
    String CONSTRUCTION_PARAMETERS = "gov.nasa.worldwind.avkey.ConstructionParameters";
    String CONTEXT = "gov.nasa.worldwind.avkey.Context";
    String COORDINATE_SYSTEM = "gov.nasa.worldwind.avkey.CoordinateSystem";
    String COORDINATE_SYSTEM_GEOGRAPHIC = "gov.nasa.worldwind.avkey.CoordinateSystem.Geographic";
    String COORDINATE_SYSTEM_NAME = "gov.nasa.worldwind.avkey.CoordinateSystem.Name";
    String COORDINATE_SYSTEM_PROJECTED = "gov.nasa.worldwind.avkey.CoordinateSystem.Projected";
    String COORDINATE_SYSTEM_SCREEN = "gov.nasa.worldwind.avkey.CoordinateSystem.Screen";
    String COORDINATE_SYSTEM_UNKNOWN = "gov.nasa.worldwind.avkey.CoordinateSystem.Unknown";

    String COUNTER_CLOCKWISE = "gov.nasa.worldwind.avkey.CounterClockWise";
    String COVERAGE_IDENTIFIERS = "gov.nasa.worldwind.avkey.CoverageIdentifiers";

    String DATA_CACHE_NAME = "gov.nasa.worldwind.avkey.DataCacheNameKey";
    String DATA_FILE_STORE_CLASS_NAME = "gov.nasa.worldwind.avkey.DataFileStoreClassName";
    String DATA_FILE_STORE_CONFIGURATION_FILE_NAME
        = "gov.nasa.worldwind.avkey.DataFileStoreConfigurationFileName";
    String DATASET_NAME = "gov.nasa.worldwind.avkey.DatasetNameKey";
    String DATA_RASTER_READER_FACTORY_CLASS_NAME = "gov.nasa.worldwind.avkey.DataRasterReaderFactoryClassName";
    String DATASET_TYPE = "gov.nasa.worldwind.avkey.DatasetTypeKey";
    String DATE_TIME = "gov.nasa.worldwind.avkey.DateTime";
    /**
     * Indicates the primitive data type of a dataset or a buffer of data. When used as a key, the corresponding value
     * may be one of the following: <code>INT8</code>, <code>INT16</code>, <code>INT32</code>, <code>INT64</code>,
     * <code>FLOAT32</code>, or <code>FLOAT64</code>.
     */
    String DATA_TYPE = "gov.nasa.worldwind.avkey.DataType";
    String DELETE_CACHE_ON_EXIT = "gov.nasa.worldwind.avkey.DeleteCacheOnExit";
    /**
     * Indicates the WorldWind scene's worst-case depth resolution, in meters. This is typically interpreted by the View
     * as the desired resolution at the scene's maximum drawing distance. In this case, the resolution closer to the
     * viewer's eye point is significantly better then the worst-case resolution. Decreasing this value enables the
     * viewer to get closer to 3D shapes positioned above the terrain at the coast of potential rendering artifacts
     * between shapes that are places closely together or close to the terrain.
     */
    String DEPTH_RESOLUTION = "gov.nasa.worldwind.avkey.DepthResolution";
    String DESCRIPTION = "gov.nasa.worldwind.avkey.Description";
    String DETAIL_HINT = "gov.nasa.worldwind.avkey.DetailHint";
    String DISPLAY_ICON = "gov.nasa.worldwind.avkey.DisplayIcon";
    String DISPLAY_NAME = "gov.nasa.worldwind.avkey.DisplayName";
    String DOCUMENT = "gov.nasa.worldwind.avkey.Document";
    /**
     * Indicates the state of dragging. Provided by the {@link gov.nasa.worldwind.drag.DragContext} object to objects
     * implementing {@link gov.nasa.worldwind.drag.Draggable}.
     */
    String DRAG_BEGIN = "gov.nasa.worldwind.avkey.DragBegin";
    String DRAG_CHANGE = "gov.nasa.worldwind.avkey.DragChange";
    String DRAG_ENDED = "gov.nasa.worldwind.avkey.DragEnded";

    String DTED_LEVEL = "gov.nasa.worldwind.avkey.DTED.Level";

    String EARTH_ELEVATION_MODEL_CAPABILITIES = "gov.nasa.worldwind.avkey.EarthElevationModelCapabilities";
    String EARTH_ELEVATION_MODEL_CLASS_NAME = "gov.nasa.worldwind.avkey.EarthElevationModelClassName";
    String EARTH_ELEVATION_MODEL_CONFIG_FILE = "gov.nasa.worldwind.avkey.EarthElevationModelConfigFile";
    String EAST = "gov.nasa.worldwind.avkey.East";

    String ELEVATION = "gov.nasa.worldwind.avkey.Elevation";
    String ELEVATION_EXTREMES_FILE = "gov.nasa.worldwind.avkey.ElevationExtremesFileKey";
    String ELEVATION_EXTREMES_LOOKUP_CACHE_SIZE = "gov.nasa.worldwind.avkey.ElevationExtremesLookupCacheSize";
    String ELEVATION_MIN = "gov.nasa.worldwind.avkey.ElevationMinKey";
    String ELEVATION_MAX = "gov.nasa.worldwind.avkey.ElevationMaxKey";
    String ELEVATION_MODEL = "gov.nasa.worldwind.avkey.ElevationModel";
    String ELEVATION_MODEL_FACTORY = "gov.nasa.worldwind.avkey.ElevationModelFactory";
    String ELEVATION_TILE_CACHE_SIZE = "gov.nasa.worldwind.avkey.ElevationTileCacheSize";
    String ELEVATION_UNIT = "gov.nasa.worldwind.avkey.ElevationUnit";

    String END = "gov.nasa.worldwind.avkey.End";

    String EXPIRY_TIME = "gov.nasa.worldwind.avkey.ExpiryTime";
    String EXTENT = "gov.nasa.worldwind.avkey.Extent";
    String EXTERNAL_LINK = "gov.nasa.worldwind.avkey.ExternalLink";

    String FEEDBACK_ENABLED = "gov.nasa.worldwind.avkey.FeedbackEnabled";
    String FEEDBACK_REFERENCE_POINT = "gov.nasa.worldwind.avkey.FeedbackReferencePoint";
    String FEEDBACK_SCREEN_BOUNDS = "gov.nasa.worldwind.avkey.FeedbackScreenBounds";
    String FILE = "gov.nasa.worldwind.avkey.File";
    String FILE_NAME = "gov.nasa.worldwind.avkey.FileName";
    String FILE_SIZE = "gov.nasa.worldwind.avkey.FileSize";
    String FILE_STORE = "gov.nasa.worldwind.avkey.FileStore";
    String FILE_STORE_LOCATION = "gov.nasa.worldwind.avkey.FileStoreLocation";
    String FLOAT32 = "gov.nasa.worldwind.avkey.Float32";
    String FLOAT64 = "gov.nasa.worldwind.avkey.Float64";
    String FORMAT_SUFFIX = "gov.nasa.worldwind.avkey.FormatSuffixKey";
    String FORWARD = "gov.nasa.worldwind.avkey.Forward";
    String FOV = "gov.nasa.worldwind.avkey.FieldOfView";
    String FORCE_LEVEL_ZERO_LOADS = "gov.nasa.worldwind.avkey.ForceLevelZeroLoads";
    String FRACTION = "gov.nasa.worldwind.avkey.Fraction";
    String FRAME_TIMESTAMP = "gov.nasa.worldwind.avkey.FrameTimestamp";

    String GDAL_AREA = "gov.nasa.worldwind.avkey.GDAL.Area";
    String GDAL_CACHEMAX = "gov.nasa.worldwind.avkey.GDAL.CacheMax";
    String GDAL_DEBUG = "gov.nasa.worldwind.avkey.GDAL.Debug";
    String GDAL_MASK_DATASET = "gov.nasa.worldwind.avkey.GDAL.MaskDataset";
    String GDAL_TIMEOUT = "gov.nasa.worldwind.avkey.GDAL.TimeOut";
    String GDAL_PATH = "gov.nasa.worldwind.avkey.GDAL.Path";

    String GET_CAPABILITIES_URL = "gov.nasa.worldwind.avkey.GetCapabilitiesURL";
    String GET_COVERAGE_URL = "gov.nasa.worldwind.avkey.GetCoverageURL";
    String GET_MAP_URL = "gov.nasa.worldwind.avkey.GetMapURL";
    String GEOGRAPHIC_PROJECTION_CLASS_NAME = "gov.nasa.worldwind.globes.GeographicProjectionClassName";
    String GLOBE = "gov.nasa.worldwind.avkey.GlobeObject";
    String GLOBE_CLASS_NAME = "gov.nasa.worldwind.avkey.GlobeClassName";
    String GRAYSCALE = "gov.nasa.worldwind.avkey.Grayscale";
    String GREAT_CIRCLE = "gov.nasa.worldwind.avkey.GreatCircle";

    String HEADING = "gov.nasa.worldwind.avkey.Heading";
    String HEIGHT = "gov.nasa.worldwind.avkey.Height";
    String HIDDEN = "gov.nasa.worldwind.avkey.Hidden";
    String HORIZONTAL = "gov.nasa.worldwind.avkey.Horizontal";
    String HOT_SPOT = "gov.nasa.worldwind.avkey.HotSpot";
    String HOVER_TEXT = "gov.nasa.worldwind.avkey.HoverText";
    String HTTP_SSL_CONTEXT = "gov.nasa.worldwind.avkey.HTTP.SSLContext";

    String ICON_NAME = "gov.nasa.worldwind.avkey.IconName";
    String IGNORE = "gov.nasa.worldwind.avkey.Ignore";
    String IMAGE = "gov.nasa.worldwind.avkey.Image";
    String IMAGE_FORMAT = "gov.nasa.worldwind.avkey.ImageFormat";
    /**
     * Indicates whether an image represents color or grayscale values. When used as a key, the corresponding value may
     * be one of the following: <code>COLOR</code> or <code>GRAYSCALE</code>.
     */
    String IMAGE_COLOR_FORMAT = "gov.nasa.worldwind.avkey.ImageColorFormat";
    String INACTIVE_LEVELS = "gov.nasa.worldwind.avkey.InactiveLevels";
    String INSTALLED = "gov.nasa.worldwind.avkey.Installed";
    String INITIAL_ALTITUDE = "gov.nasa.worldwind.avkey.InitialAltitude";
    String INITIAL_HEADING = "gov.nasa.worldwind.avkey.InitialHeading";
    String INITIAL_LATITUDE = "gov.nasa.worldwind.avkey.InitialLatitude";
    String INITIAL_LONGITUDE = "gov.nasa.worldwind.avkey.InitialLongitude";
    String INITIAL_PITCH = "gov.nasa.worldwind.avkey.InitialPitch";
    String INPUT_HANDLER_CLASS_NAME = "gov.nasa.worldwind.avkey.InputHandlerClassName";
    String INSET_PIXELS = "gov.nasa.worldwind.avkey.InsetPixels";
    String INT8 = "gov.nasa.worldwind.avkey.Int8";
    String INT16 = "gov.nasa.worldwind.avkey.Int16";
    String INT32 = "gov.nasa.worldwind.avkey.Int32";
    String INT64 = "gov.nasa.worldwind.avkey.Int64";

    String LABEL = "gov.nasa.worldwind.avkey.Label";
    String LAST_UPDATE = "gov.nasa.worldwind.avkey.LastUpdateKey";
    String LAYER = "gov.nasa.worldwind.avkey.LayerObject";
    String LAYER_ABSTRACT = "gov.nasa.worldwind.avkey.LayerAbstract";
    String LAYER_DESCRIPTOR_FILE = "gov.nasa.worldwind.avkey.LayerDescriptorFile";
    String LAYER_FACTORY = "gov.nasa.worldwind.avkey.LayerFactory";
    String LAYER_NAME = "gov.nasa.worldwind.avkey.LayerName";
    String LAYER_NAMES = "gov.nasa.worldwind.avkey.LayerNames";
    String LAYERS = "gov.nasa.worldwind.avkey.LayersObject";
    String LAYERS_CLASS_NAMES = "gov.nasa.worldwind.avkey.LayerClassNames";
    String LEFT = "gov.nasa.worldwind.avkey.Left";
    String LEFT_OF_CENTER = "gov.nasa.worldwind.avkey.LeftOfCenter";
    String LEVEL_NAME = "gov.nasa.worldwind.avkey.LevelNameKey";
    String LEVEL_NUMBER = "gov.nasa.worldwind.avkey.LevelNumberKey";
    String LEVEL_ZERO_TILE_DELTA = "gov.nasa.worldwind.avkey.LevelZeroTileDelta";
    String LINEAR = "gov.nasa.worldwind.avkey.Linear";
    String LITTLE_ENDIAN = "gov.nasa.worldwind.avkey.LittleEndian";
    String LOGGER_NAME = "gov.nasa.worldwind.avkey.LoggerName";
    String LOXODROME = "gov.nasa.worldwind.avkey.Loxodrome";

    String MAP_SCALE = "gov.nasa.worldwind.avkey.MapScale";
    String MARS_ELEVATION_MODEL_CLASS_NAME = "gov.nasa.worldwind.avkey.MarsElevationModelClassName";
    String MARS_ELEVATION_MODEL_CONFIG_FILE = "gov.nasa.worldwind.avkey.MarsElevationModelConfigFile";

    /**
     * Describes the maximum number of attempts to make when downloading a resource before attempts are suspended.
     * Attempts are restarted after the interval specified by {@link #MIN_ABSENT_TILE_CHECK_INTERVAL}.
     *
     * @see #MIN_ABSENT_TILE_CHECK_INTERVAL
     */
    String MAX_ABSENT_TILE_ATTEMPTS = "gov.nasa.worldwind.avkey.MaxAbsentTileAttempts";

    String MAX_ACTIVE_ALTITUDE = "gov.nasa.worldwind.avkey.MaxActiveAltitude";
    String MAX_MESSAGE_REPEAT = "gov.nasa.worldwind.avkey.MaxMessageRepeat";
    String MEMORY_CACHE_SET_CLASS_NAME = "gov.nasa.worldwind.avkey.MemoryCacheSetClassName";
    /**
     * Indicates the location that MIL-STD-2525 tactical symbols and tactical point graphics retrieve their icons from.
     * When used as a key, the corresponding value must be a string indicating a URL to a remote server, a URL to a
     * ZIP/JAR file, or a path to folder on the local file system.
     */
    String MIL_STD_2525_ICON_RETRIEVER_PATH = "gov.nasa.worldwind.avkey.MilStd2525IconRetrieverPath";
    String MIME_TYPE = "gov.nasa.worldwind.avkey.MimeType";

    /**
     * Describes the interval to wait before allowing further attempts to download a resource after the number of
     * attempts specified by {@link #MAX_ABSENT_TILE_ATTEMPTS} are made.
     *
     * @see #MAX_ABSENT_TILE_ATTEMPTS
     */
    String MIN_ABSENT_TILE_CHECK_INTERVAL = "gov.nasa.worldwind.avkey.MinAbsentTileCheckInterval";
    String MIN_ACTIVE_ALTITUDE = "gov.nasa.worldwind.avkey.MinActiveAltitude";

    // Implementation note: the keys MISSING_DATA_SIGNAL and MISSING_DATA_REPLACEMENT are intentionally different than
    // their actual string values. Legacy code is expecting the string values "MissingDataFlag" and "MissingDataValue",
    // respectively.
    String MISSING_DATA_SIGNAL = "gov.nasa.worldwind.avkey.MissingDataFlag";
    String MISSING_DATA_REPLACEMENT = "gov.nasa.worldwind.avkey.MissingDataValue";

    String MODEL = "gov.nasa.worldwind.avkey.ModelObject";
    String MODEL_CLASS_NAME = "gov.nasa.worldwind.avkey.ModelClassName";
    String MOON_ELEVATION_MODEL_CLASS_NAME = "gov.nasa.worldwind.avkey.MoonElevationModelClassName";
    String MOON_ELEVATION_MODEL_CONFIG_FILE = "gov.nasa.worldwind.avkey.MoonElevationModelConfigFile";

    String NAME = "gov.nasa.worldwind.avkey.Name";
    String NETWORK_STATUS_CLASS_NAME = "gov.nasa.worldwind.avkey.NetworkStatusClassName";
    String NETWORK_STATUS_TEST_SITES = "gov.nasa.worldwind.avkey.NetworkStatusTestSites";
    String NEXT = "gov.nasa.worldwind.avkey.Next";
    String NUM_BANDS = "gov.nasa.worldwind.avkey.NumBands";
    String NUM_EMPTY_LEVELS = "gov.nasa.worldwind.avkey.NumEmptyLevels";
    String NUM_LEVELS = "gov.nasa.worldwind.avkey.NumLevels";
    String NETWORK_RETRIEVAL_ENABLED = "gov.nasa.worldwind.avkey.NetworkRetrievalEnabled";
    String NORTH = "gov.nasa.worldwind.avkey.North";

    String OFFLINE_MODE = "gov.nasa.worldwind.avkey.OfflineMode";
    String OPACITY = "gov.nasa.worldwind.avkey.Opacity";
    /**
     * Indicates an object's position in a series. When used as a key, the corresponding value must be an {@link
     * Integer} object indicating the ordinal.
     */
    String ORDINAL = "gov.nasa.worldwind.avkey.Ordinal";
    /**
     * Indicates a list of one or more object's positions in a series. When used as a key, the corresponding value must
     * be a {@link java.util.List} of {@link Integer} objects indicating the ordinals.
     */
    String ORDINAL_LIST = "gov.nasa.worldwind.avkey.OrdinalList";
    String ORIGIN = "gov.nasa.worldwind.avkey.Origin";

    String PARENT_LAYER_NAME = "gov.nasa.worldwind.avkey.ParentLayerName";

    String PAUSE = "gov.nasa.worldwind.avkey.Pause";
    String PICKED_OBJECT = "gov.nasa.worldwind.avkey.PickedObject";
    String PICKED_OBJECT_ID = "gov.nasa.worldwind.avkey.PickedObject.ID";
    String PICKED_OBJECT_PARENT_LAYER = "gov.nasa.worldwind.avkey.PickedObject.ParentLayer";
    String PICKED_OBJECT_PARENT_LAYER_NAME = "gov.nasa.worldwind.avkey.PickedObject.ParentLayer.Name";
    String PICKED_OBJECT_SIZE = "gov.nasa.worldwind.avkey.PickedObject.Size";
    String PICK_ENABLED = "gov.nasa.worldwind.avkey.PickEnabled";
    String PIXELS = "gov.nasa.worldwind.avkey.Pixels";
    /**
     * Indicates whether a raster's pixel values represent imagery or elevation data. When used as a key, the
     * corresponding value may be one of the following: <code>IMAGERY</code> or <code>ELEVATION</code>.
     */
    String PIXEL_FORMAT = "gov.nasa.worldwind.avkey.PixelFormat";
    String PIXEL_HEIGHT = "gov.nasa.worldwind.avkey.PixelHeight";
    String PIXEL_WIDTH = "gov.nasa.worldwind.avkey.PixelWidth";

    /**
     * @deprecated Use <code>{@link #DATA_TYPE} instead.</code>.
     */
    @Deprecated
    String PIXEL_TYPE = AVKey.DATA_TYPE;

    String PLACENAME_LAYER_CACHE_SIZE = "gov.nasa.worldwind.avkey.PlacenameLayerCacheSize";
    String PLAY = "gov.nasa.worldwind.avkey.Play";
    String POSITION = "gov.nasa.worldwind.avkey.Position";
    String PREVIOUS = "gov.nasa.worldwind.avkey.Previous";

    String PRODUCER_ENABLE_FULL_PYRAMID = "gov.nasa.worldwind.avkey.Producer.EnableFullPyramid";

    String PROGRESS = "gov.nasa.worldwind.avkey.Progress";
    String PROGRESS_MESSAGE = "gov.nasa.worldwind.avkey.ProgressMessage";

    String PROJECTION_DATUM = "gov.nasa.worldwind.avkey.Projection.Datum";
    String PROJECTION_DESC = "gov.nasa.worldwind.avkey.Projection.Description";
    String PROJECTION_EPSG_CODE = "gov.nasa.worldwind.avkey.Projection.EPSG.Code";
    String PROJECTION_HEMISPHERE = "gov.nasa.worldwind.avkey.Projection.Hemisphere";
    String PROJECTION_NAME = "gov.nasa.worldwind.avkey.Projection.Name";
    String PROJECTION_UNITS = "gov.nasa.worldwind.avkey.Projection.Units";
    String PROJECTION_UNKNOWN = "gov.nasa.worldwind.Projection.Unknown";
    String PROJECTION_UTM = "gov.nasa.worldwind.avkey.Projection.UTM";
    String PROJECTION_ZONE = "gov.nasa.worldwind.avkey.Projection.Zone";

    String PROPERTIES = "gov.nasa.worldwind.avkey.Properties";

    String PROTOCOL = "gov.nasa.worldwind.avkey.Protocol";
    String PROTOCOL_HTTP = "gov.nasa.worldwind.avkey.Protocol.HTTP";
    String PROTOCOL_HTTPS = "gov.nasa.worldwind.avkey.Protocol.HTTPS";

    String RECTANGLES = "gov.nasa.worldwind.avkey.Rectangles";
    String REDRAW_ON_MOUSE_PRESSED = "gov.nasa.worldwind.avkey.ForceRedrawOnMousePressed";

    String RELATIVE_TO_GLOBE = "gov.nasa.worldwind.avkey.RelativeToGlobe";
    String RELATIVE_TO_SCREEN = "gov.nasa.worldwind.avkey.RelativeToScreen";

    String RANGE = "gov.nasa.worldwind.avkey.Range";
    String RASTER_BAND_ACTUAL_BITS_PER_PIXEL = "gov.nasa.worldwind.avkey.RasterBand.ActualBitsPerPixel";
    String RASTER_BAND_MIN_PIXEL_VALUE = "gov.nasa.worldwind.avkey.RasterBand.MinPixelValue";
    String RASTER_BAND_MAX_PIXEL_VALUE = "gov.nasa.worldwind.avkey.RasterBand.MaxPixelValue";

    String RASTER_HAS_ALPHA = "gov.nasa.worldwind.avkey.RasterHasAlpha";
    String RASTER_HAS_OVERVIEWS = "gov.nasa.worldwind.avkey.Raster.HasOverviews";
    String RASTER_HAS_VOIDS = "gov.nasa.worldwind.avkey.Raster.HasVoids";
    String RASTER_LAYER_CLASS_NAME = "gov.nasa.worldwind.avkey.RasterLayer.ClassName";
    String RASTER_PIXEL = "gov.nasa.worldwind.avkey.RasterPixel";
    String RASTER_PIXEL_IS_AREA = "gov.nasa.worldwind.avkey.RasterPixelIsArea";
    String RASTER_PIXEL_IS_POINT = "gov.nasa.worldwind.avkey.RasterPixelIsPoint";
    String RECTANGULAR_TESSELLATOR_MAX_LEVEL = "gov.nasa.worldwind.avkey.RectangularTessellatorMaxLevel";
    String REPAINT = "gov.nasa.worldwind.avkey.Repaint";
    String REPEAT_NONE = "gov.nasa.worldwind.avkey.RepeatNone";
    String REPEAT_X = "gov.nasa.worldwind.avkey.RepeatX";
    String REPEAT_Y = "gov.nasa.worldwind.avkey.RepeatY";
    String REPEAT_XY = "gov.nasa.worldwind.avkey.RepeatXY";

    String RESIZE = "gov.nasa.worldwind.avkey.Resize";
    /**
     * On window resize, scales the item to occupy a constant relative size of the viewport.
     */
    String RESIZE_STRETCH = "gov.nasa.worldwind.CompassLayer.ResizeStretch";
    /**
     * On window resize, scales the item to occupy a constant relative size of the viewport, but not larger than the
     * item's inherent size scaled by the layer's item scale factor.
     */
    String RESIZE_SHRINK_ONLY = "gov.nasa.worldwind.CompassLayer.ResizeShrinkOnly";
    /**
     * Does not modify the item size when the window changes size.
     */
    String RESIZE_KEEP_FIXED_SIZE = "gov.nasa.worldwind.CompassLayer.ResizeKeepFixedSize";
    String RETAIN_LEVEL_ZERO_TILES = "gov.nasa.worldwind.avkey.RetainLevelZeroTiles";
    String RETRIEVAL_POOL_SIZE = "gov.nasa.worldwind.avkey.RetrievalPoolSize";
    String RETRIEVE_PROPERTIES_FROM_SERVICE = "gov.nasa.worldwind.avkey.RetrievePropertiesFromService";
    String RETRIEVAL_QUEUE_SIZE = "gov.nasa.worldwind.avkey.RetrievalQueueSize";
    String RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT = "gov.nasa.worldwind.avkey.RetrievalStaleRequestLimit";
    String RETRIEVAL_SERVICE_CLASS_NAME = "gov.nasa.worldwind.avkey.RetrievalServiceClassName";
    String RETRIEVER_FACTORY_LOCAL = "gov.nasa.worldwind.avkey.RetrieverFactoryLocal";
    String RETRIEVER_FACTORY_REMOTE = "gov.nasa.worldwind.avkey.RetrieverFactoryRemote";
    String RETRIEVER_STATE = "gov.nasa.worldwind.avkey.RetrieverState";
    String RETRIEVAL_STATE_ERROR = "gov.nasa.worldwind.avkey.RetrievalStateError";
    String RETRIEVAL_STATE_SUCCESSFUL = "gov.nasa.worldwind.avkey.RetrievalStateSuccessful";
    String RHUMB_LINE = "gov.nasa.worldwind.avkey.RhumbLine";
    String RIGHT = "gov.nasa.worldwind.avkey.Right";
    String RIGHT_OF_CENTER = "gov.nasa.worldwind.avkey.RightOfCenter";
    String ROLL = "gov.nasa.worldwind.avkey.Roll";
    String ROLLOVER_TEXT = "gov.nasa.worldwind.avkey.RolloverText";

    String SCHEDULED_TASK_POOL_SIZE = "gov.nasa.worldwind.avkey.ScheduledTaskPoolSize";
    String SCHEDULED_TASK_SERVICE_CLASS_NAME = "gov.nasa.worldwind.avkey.ScheduledTaskServiceClassName";
    String SCENE_CONTROLLER = "gov.nasa.worldwind.avkey.SceneControllerObject";
    String SCENE_CONTROLLER_CLASS_NAME = "gov.nasa.worldwind.avkey.SceneControllerClassName";
    String SCREEN = "gov.nasa.worldwind.avkey.ScreenObject";
    String SCREEN_CREDIT = "gov.nasa.worldwind.avkey.ScreenCredit";
    String SCREEN_CREDIT_LINK = "gov.nasa.worldwind.avkey.ScreenCreditLink";
    String SECTOR = "gov.nasa.worldwind.avKey.Sector";
    String SECTOR_BOTTOM_LEFT = "gov.nasa.worldwind.avkey.Sector.BottomLeft";
    String SECTOR_BOTTOM_RIGHT = "gov.nasa.worldwind.avkey.Sector.BottomRight";
    String SECTOR_GEOMETRY_CACHE_SIZE = "gov.nasa.worldwind.avkey.SectorGeometryCacheSize";
    String SECTOR_RESOLUTION_LIMITS = "gov.nasa.worldwind.avkey.SectorResolutionLimits";
    String SECTOR_RESOLUTION_LIMIT = "gov.nasa.worldwind.avkey.SectorResolutionLimit";
    String SECTOR_UPPER_LEFT = "gov.nasa.worldwind.avkey.Sector.UpperLeft";
    String SECTOR_UPPER_RIGHT = "gov.nasa.worldwind.avkey.Sector.UpperRight";
    String SENDER = "gov.nasa.worldwind.avkey.Sender";
    String SERVER = "gov.nasa.worldwind.avkey.Server";
    String SERVICE = "gov.nasa.worldwind.avkey.ServiceURLKey";
    String SERVICE_CLASS = "gov.nasa.worldwind.avkey.ServiceClass";
    String SERVICE_NAME = "gov.nasa.worldwind.avkey.ServiceName";
    String SERVICE_NAME_LOCAL_RASTER_SERVER = "LocalRasterServer";
    String SERVICE_NAME_OFFLINE = "Offline";
    String SESSION_CACHE_CLASS_NAME = "gov.nasa.worldwind.avkey.SessionCacheClassName";
    String SHAPE_ATTRIBUTES = "gov.nasa.worldwind.avkey.ShapeAttributes";
    String SHAPE_CIRCLE = "gov.nasa.worldwind.avkey.ShapeCircle";
    String SHAPE_ELLIPSE = "gov.nasa.worldwind.avkey.ShapeEllipse";
    String SHAPE_LINE = "gov.nasa.worldwind.avkey.ShapeLine";
    String SHAPE_NONE = "gov.nasa.worldwind.avkey.ShapeNone";
    String SHAPE_PATH = "gov.nasa.worldwind.avkey.ShapePath";
    String SHAPE_POLYGON = "gov.nasa.worldwind.avkey.ShapePolygon";
    String SHAPE_QUAD = "gov.nasa.worldwind.avkey.ShapeQuad";
    String SHAPE_RECTANGLE = "gov.nasa.worldwind.avkey.ShapeRectangle";
    String SHAPE_SQUARE = "gov.nasa.worldwind.avkey.ShapeSquare";
    String SHAPE_TRIANGLE = "gov.nasa.worldwind.avkey.ShapeTriangle";
    String SHAPEFILE_GEOMETRY_CACHE_SIZE = "gov.nasa.worldwind.avkey.ShapefileGeometryCacheSize";
    String SHAPEFILE_LAYER_FACTORY = "gov.nasa.worldwind.avkey.ShapefileLayerFactory";
    String SHORT_DESCRIPTION = "gov.nasa.worldwind.avkey.Server.ShortDescription";
    String SIZE_FIT_TEXT = "gov.nasa.worldwind.avkey.SizeFitText";
    String SIZE_FIXED = "gov.nasa.worldwind.avkey.SizeFixed";
    String SPATIAL_REFERENCE_WKT = "gov.nasa.worldwind.avkey.SpatialReference.WKT";
    String SOUTH = "gov.nasa.worldwdind.avkey.South";
    String START = "gov.nasa.worldwind.avkey.Start";
    String STEREO_FOCUS_ANGLE = "gov.nasa.worldwind.StereoFocusAngle";
    String STEREO_INTEROCULAR_DISTANCE = "gov.nasa.worldwind.StereoFInterocularDistance";
    String STEREO_MODE = "gov.nasa.worldwind.stereo.mode"; // lowercase to match Java property convention
    String STEREO_MODE_DEVICE = "gov.nasa.worldwind.avkey.StereoModeDevice";
    String STEREO_MODE_NONE = "gov.nasa.worldwind.avkey.StereoModeNone";
    String STEREO_MODE_RED_BLUE = "gov.nasa.worldwind.avkey.StereoModeRedBlue";
    String STEREO_TYPE = "gov.nasa.worldwind.stereo.type";
    String STEREO_TYPE_TOED_IN = "gov.nasa.worldwind.avkey.StereoModeToedIn";
    String STEREO_TYPE_PARALLEL = "gov.nasa.worldwind.avkey.StereoModeParallel";
    String STOP = "gov.nasa.worldwind.avkey.Stop";
    String STYLE_NAMES = "gov.nasa.worldwind.avkey.StyleNames";
    String SURFACE_TILE_DRAW_CONTEXT = "gov.nasa.worldwind.avkey.SurfaceTileDrawContext";

    String TESSELLATOR_CLASS_NAME = "gov.nasa.worldwind.avkey.TessellatorClassName";
    String TEXTURE = "gov.nasa.worldwind.avkey.Texture";
    String TEXTURE_CACHE_SIZE = "gov.nasa.worldwind.avkey.TextureCacheSize";
    String TEXTURE_COORDINATES = "gov.nasa.worldwind.avkey.TextureCoordinates";
    String TEXTURE_FORMAT = "gov.nasa.worldwind.avkey.TextureFormat";
    String TEXTURE_IMAGE_CACHE_SIZE = "gov.nasa.worldwind.avkey.TextureTileCacheSize";
    String TARGET = "gov.nasa.worldwind.avkey.Target";
    String TASK_POOL_SIZE = "gov.nasa.worldwind.avkey.TaskPoolSize";
    String TASK_QUEUE_SIZE = "gov.nasa.worldwind.avkey.TaskQueueSize";
    String TASK_SERVICE_CLASS_NAME = "gov.nasa.worldwind.avkey.TaskServiceClassName";
    String TEXT = "gov.nasa.worldwind.avkey.Text";
    String TEXT_EFFECT_NONE = "gov.nasa.worldwind.avkey.TextEffectNone";
    String TEXT_EFFECT_OUTLINE = "gov.nasa.worldwind.avkey.TextEffectOutline";
    String TEXT_EFFECT_SHADOW = "gov.nasa.worldwind.avkey.TextEffectShadow";
    String TILE_DELTA = "gov.nasa.worldwind.avkey.TileDeltaKey";
    String TILE_HEIGHT = "gov.nasa.worldwind.avkey.TileHeightKey";
    String TILE_ORIGIN = "gov.nasa.worldwind.avkey.TileOrigin";
    String TILE_RETRIEVER = "gov.nasa.worldwind.avkey.TileRetriever";
    String TILE_URL_BUILDER = "gov.nasa.worldwind.avkey.TileURLBuilder";
    String TILE_WIDTH = "gov.nasa.worldwind.avkey.TileWidthKey";
    String TILED_IMAGERY = "gov.nasa.worldwind.avkey.TiledImagery";
    String TILED_ELEVATIONS = "gov.nasa.worldwind.avkey.TiledElevations";
    String TILED_RASTER_PRODUCER_CACHE_SIZE = "gov.nasa.worldwind.avkey.TiledRasterProducerCacheSize";
    String TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD =
        "gov.nasa.worldwind.avkey.TiledRasterProducerLargeDatasetThreshold";
    String TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL = "gov.nasa.worldwind.avkey.TiledRasterProducer.LimitMaxLevel";
    String TILT = "gov.nasa.worldwind.avkey.Tilt";
    String TITLE = "gov.nasa.worldwind.avkey.Title";
    String TOP = "gov.nasa.worldwind.avkey.Top";
    String TRANSPARENCY_COLORS = "gov.nasa.worldwind.avkey.TransparencyColors";
    String TREE = "gov.nasa.worldwind.avkey.Tree";
    String TREE_NODE = "gov.nasa.worldwind.avkey.TreeNode";

    String UNIT_FOOT = "gov.nasa.worldwind.avkey.Unit.Foot";
    String UNIT_METER = "gov.nasa.worldwind.avkey.Unit.Meter";

    String UNRESOLVED = "gov.nasa.worldwind.avkey.Unresolved";
    String UPDATED = "gov.nasa.worldwind.avkey.Updated";
    String URL = "gov.nasa.worldwind.avkey.URL";
    String URL_CONNECT_TIMEOUT = "gov.nasa.worldwind.avkey.URLConnectTimeout";
    String URL_PROXY_HOST = "gov.nasa.worldwind.avkey.UrlProxyHost";
    String URL_PROXY_PORT = "gov.nasa.worldwind.avkey.UrlProxyPort";
    String URL_PROXY_TYPE = "gov.nasa.worldwind.avkey.UrlProxyType";
    String URL_READ_TIMEOUT = "gov.nasa.worldwind.avkey.URLReadTimeout";
    String USE_MIP_MAPS = "gov.nasa.worldwind.avkey.UseMipMaps";
    String USE_TRANSPARENT_TEXTURES = "gov.nasa.worldwind.avkey.UseTransparentTextures";

    String VBO_THRESHOLD = "gov.nasa.worldwind.avkey.VBOThreshold";
    String VBO_USAGE = "gov.nasa.worldwind.avkey.VBOUsage";
    String VERSION = "gov.nasa.worldwind.avkey.Version";
    String VERTICAL = "gov.nasa.worldwind.avkey.Vertical";
    String VERTICAL_EXAGGERATION = "gov.nasa.worldwind.avkey.VerticalExaggeration";
    String VERTICAL_EXAGGERATION_UP = "gov.nasa.worldwind.avkey.VerticalExaggerationUp";
    String VERTICAL_EXAGGERATION_DOWN = "gov.nasa.worldwind.avkey.VerticalExaggerationDown";
    String VIEW = "gov.nasa.worldwind.avkey.ViewObject";
    String VIEW_CLASS_NAME = "gov.nasa.worldwind.avkey.ViewClassName";
    String VIEW_INPUT_HANDLER_CLASS_NAME = "gov.nasa.worldwind.avkey.ViewInputHandlerClassName";
    String VIEW_QUIET = "gov.nasa.worldwind.avkey.ViewQuiet";

    // Viewing operations
    String VIEW_OPERATION = "gov.nasa.worldwind.avkey.ViewOperation";
    String VIEW_PAN = "gov.nasa.worldwind.avkey.Pan";
    String VIEW_LOOK = "gov.nasa.worldwind.avkey.ControlLook";
    String VIEW_HEADING_LEFT = "gov.nasa.worldwind.avkey.HeadingLeft";
    String VIEW_HEADING_RIGHT = "gov.nasa.worldwind.avkey.HeadingRight";
    String VIEW_ZOOM_IN = "gov.nasa.worldwind.avkey.ZoomIn";
    String VIEW_ZOOM_OUT = "gov.nasa.worldwind.avkey.ZoomOut";
    String VIEW_PITCH_UP = "gov.nasa.worldwind.avkey.PitchUp";
    String VIEW_PITCH_DOWN = "gov.nasa.worldwind.avkey.PitchDown";
    String VIEW_FOV_NARROW = "gov.nasa.worldwind.avkey.FovNarrow";
    String VIEW_FOV_WIDE = "gov.nasa.worldwind.avkey.FovWide";

    String VISIBILITY_ACTION_RELEASE = "gov.nasa.worldwind.avkey.VisibilityActionRelease";
    String VISIBILITY_ACTION_RETAIN = "gov.nasa.worldwind.avkey.VisibilityActionRetain";

    String WAKEUP_TIMEOUT = "gov.nasa.worldwind.avkey.WakeupTimeout";
    String WEB_VIEW_FACTORY = "gov.nasa.worldwind.avkey.WebViewFactory";
    String WEST = "gov.nasa.worldwind.avkey.West";
    String WIDTH = "gov.nasa.worldwind.avkey.Width";
    String WMS_BACKGROUND_COLOR = "gov.nasa.worldwind.avkey.BackgroundColor";

    String WFS_URL = "gov.nasa.worldwind.avkey.WFS.URL";
    String WCS_VERSION = "gov.nasa.worldwind.avkey.WCSVersion";
    String WMS_VERSION = "gov.nasa.worldwind.avkey.WMSVersion";
    String WORLD_MAP_IMAGE_PATH = "gov.nasa.worldwind.avkey.WorldMapImagePath";
    String WORLD_WIND_DOT_NET_LAYER_SET = "gov.nasa.worldwind.avkey.WorldWindDotNetLayerSet";
    String WORLD_WIND_DOT_NET_PERMANENT_DIRECTORY = "gov.nasa.worldwind.avkey.WorldWindDotNetPermanentDirectory";
    String WORLD_WINDOW_CLASS_NAME = "gov.nasa.worldwind.avkey.WorldWindowClassName";
}
