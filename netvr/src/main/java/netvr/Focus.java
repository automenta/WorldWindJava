package netvr;

import gov.nasa.worldwind.geom.*;

import java.io.Serializable;

/** defines a space[time] focus */
public class Focus implements Serializable {

    public String name = "";

    //TODO When

    public Position pos;

    public Focus(Position p) {
        this.pos = p;
    }

}