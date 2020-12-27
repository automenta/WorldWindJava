package netvr;

import gov.nasa.worldwind.layers.*;

public class GeoJSONLayer extends RenderableLayer {

    public GeoJSONLayer(String name, Object src) {
        this(name, src, new GeoJSON.GeoJSONRenderer());
    }

    public GeoJSONLayer(String name, Object src, GeoJSON.GeoJSONRenderer r) {
        super(name);
        new GeoJSON(r).load(src, this);
    }
    
}
