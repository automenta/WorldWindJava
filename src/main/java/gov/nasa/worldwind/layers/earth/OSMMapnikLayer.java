/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.earth;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.mercator.*;
import gov.nasa.worldwind.util.*;

import java.net.*;

/**
 * @version $Id: OSMMapnikLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class OSMMapnikLayer extends BasicMercatorTiledImageLayer {
    public OSMMapnikLayer() {
        super(makeLevels());
    }

    private static LevelSet makeLevels() {
        AVList params = new AVListImpl();

        params.set(AVKey.TILE_WIDTH, 256);
        params.set(AVKey.TILE_HEIGHT, 256);
        params.set(AVKey.DATA_CACHE_NAME, "Earth/OSM-Mercator/OpenStreetMap Mapnik");
        params.set(AVKey.SERVICE, "http://a.tile.openstreetmap.org/");
        params.set(AVKey.DATASET_NAME, "h");
        params.set(AVKey.FORMAT_SUFFIX, ".png");
        params.set(AVKey.NUM_LEVELS, 16);
        params.set(AVKey.NUM_EMPTY_LEVELS, 0);
        params.set(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle
            .fromDegrees(22.5d), Angle.fromDegrees(45.0d)));
        params.set(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
        params.set(AVKey.TILE_URL_BUILDER, new URLBuilder());

        return new LevelSet(params);
    }

    @Override
    public String toString() {
        return "OpenStreetMap";
    }

    private static class URLBuilder implements TileUrlBuilder {
        public URL getURL(Tile tile, String imageFormat)
            throws MalformedURLException {
            return new URL(tile.level.getService()
                + (tile.getLevelNumber() + 3) + "/" + tile.column + "/"
                + ((1 << (tile.getLevelNumber()) + 3) - 1 - tile.row) + ".png");
        }
    }
}
