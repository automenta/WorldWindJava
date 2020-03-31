package gov.nasa.worldwind.ogc.kml;

import gov.nasa.worldwind.geom.*;

public interface KMLMutable {
    void setPosition(Position position);
    Position getPosition();
    void setScale(Vec4 modelScale);
}
