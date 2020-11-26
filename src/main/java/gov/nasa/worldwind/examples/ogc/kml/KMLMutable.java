package gov.nasa.worldwind.examples.ogc.kml;

import gov.nasa.worldwind.geom.*;

public interface KMLMutable {
    Position getPosition();

    void setPosition(Position position);

    void setScale(Vec4 modelScale);
}
