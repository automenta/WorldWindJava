package netvr;

import gov.nasa.worldwind.geom.Extent;
import jcog.thing.Part;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public abstract class NMode extends Part<NetVR> implements Serializable {

    public abstract String name();

    public abstract String icon();

    public abstract Object menu();

    @Nullable public abstract Extent extent();
    
}