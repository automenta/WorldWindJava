package gov.nasa.worldwind.layers;

import jcog.exe.Exe;

public class GeoJSONLayer extends RenderableLayer {

    public GeoJSONLayer(String name, Object src) {
        this(name, src, new GeoJSON.GeoJSONRenderer());
    }

    public GeoJSONLayer(String name, Object src, GeoJSON.GeoJSONRenderer r) {
        super(name);

        Exe.runLater(()->{
            reload(src, r);
        });
    }

    private void reload(Object src, GeoJSON.GeoJSONRenderer r) {
        clear();
        new GeoJSON(r).load(src, this);
    }
}
