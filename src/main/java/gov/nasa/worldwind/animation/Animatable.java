package gov.nasa.worldwind.animation;

import gov.nasa.worldwind.geom.*;

public interface Animatable {
    void setPosition(Position position);
    Position getPosition();
    int getRedrawRequested();
    void setRedrawRequested(int redrawRequested);
    Angle getHeading();
    void setHeading(Angle heading);
    void setField(String keyName, Object value);
    Object getField(String keyName);
}

