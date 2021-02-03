package netvr;

import gov.nasa.worldwind.geom.*;
import jcog.thing.Part;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/** defines a space[time] focus */
public class Focus extends NMode {

    public String name = "";

    //TODO When

    public Position pos;

    public Focus(Position p) {
        this.pos = p;
    }

    @Override
    public String name() {
        return name;
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
    protected void start(NetVR netVR) {

    }

    @Override
    protected void stop(NetVR netVR) {

    }
}