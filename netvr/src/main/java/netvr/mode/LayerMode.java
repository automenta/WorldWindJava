package netvr.mode;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.layers.Layer;
import netvr.*;
import org.jetbrains.annotations.Nullable;

/** simple mode which wraps 1 worldwind layer */
public class LayerMode extends NMode {
    public final Layer l;

    public LayerMode(Layer l) {
        this.l = l;
    }

    @Override
    public String name() {
        return l.name();
    }

    @Override
    public String icon() {
        return null;
    }

    @Override
    public Object menu() {
        return null;
    }

    @Nullable
    @Override
    public Extent extent() {
        return null;
    }

    @Override
    protected void start(NetVR n) {
        n.layers().add(l);
    }

    @Override
    protected void stop(NetVR n) {
        n.layers().remove(l);
    }
}