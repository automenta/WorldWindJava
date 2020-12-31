package gov.nasa.worldwind.layers;

import jcog.exe.Exe;

public class GeoJSONLayer extends RenderableLayer {

    private final Object src;
    private final GeoJSON.GeoJSONRenderer r;

    public GeoJSONLayer(String name, Object src) {
        this(name, src, new GeoJSON.GeoJSONRenderer());
    }

    public GeoJSONLayer(String name, Object src, GeoJSON.GeoJSONRenderer r) {
        super(name);

        this.src = src;
        this.r = r;
        super.setEnabled(false);
    }

    @Override
    public synchronized Layer setEnabled(boolean enabled) {
        if (enabled != isEnabled()) {
            super.setEnabled(enabled);
            if (enabled)
                Exe.runLater(()-> reload(src, r));
        }
        return this;
    }

    private void reload(Object src, GeoJSON.GeoJSONRenderer r) {
        clear();

        if (!isEnabled())
            return;

        new GeoJSON(r).load(src, this);
    }
}
