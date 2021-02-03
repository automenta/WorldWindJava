package netvr;

import gov.nasa.worldwind.geom.Extent;
import jcog.thing.Part;
import org.jetbrains.annotations.Nullable;

public abstract class NMode extends Part<NetVR> {
    public abstract String name();

    public abstract String icon();

    public abstract Object menu();

    @Nullable
    public abstract Extent extent();

    //TODO serialize/deserialize to byte[]
}