/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.earth;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.mercator.*;
import gov.nasa.worldwind.util.*;

import java.net.*;

/**
 * @version $Id: OSMCycleMapLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class OSMCycleMapLayer extends BasicMercatorTiledImageLayer {
    public OSMCycleMapLayer() {
        super(OSMCycleMapLayer.makeLevels());
    }

    private static LevelSet makeLevels() {
        KV params = new KVMap();

        params.set(Keys.TILE_WIDTH, 256);
        params.set(Keys.TILE_HEIGHT, 256);
        params.set(Keys.DATA_CACHE_NAME, "Earth/OSM-Mercator/OpenStreetMap Cycle");
        params.set(Keys.SERVICE, "http://b.andy.sandbox.cloudmade.com/tiles/cycle/");
        params.set(Keys.DATASET_NAME, "*");
        params.set(Keys.FORMAT_SUFFIX, ".png");
        params.set(Keys.NUM_LEVELS, 16);
        params.set(Keys.NUM_EMPTY_LEVELS, 0);
        params.set(Keys.LEVEL_ZERO_TILE_DELTA, new LatLon(new Angle(22.5d), new Angle(45.0d)));
        params.set(Keys.SECTOR, new MercatorSector(-1.0, 1.0,
            Angle.NEG180, Angle.POS180));
        params.set(Keys.TILE_URL_BUILDER, new URLBuilder());

        return new LevelSet(params);
    }

    @Override
    public String toString() {
        return "OpenStreetMap Cycle";
    }

    private static class URLBuilder implements TileUrlBuilder {
        public URL getURL(Tile tile, String imageFormat)
            throws MalformedURLException {
            return new URL(tile.level.getService()
                + (tile.getLevelNumber() + 3) + '/' + tile.col + '/' + ((1 << (tile.getLevelNumber()) + 3) - 1
                - tile.row) + ".png");
        }
    }
}