package gov.nasa.worldwind.animation;

import gov.nasa.worldwind.geom.*;

public interface Animatable {
    Position getPosition();

    void setPosition(Position position);

//    int getRedrawRequested();
//
//    void setRedrawRequested(int redrawRequested);

    Angle getHeading();

    void setHeading(Angle heading);

    void setField(String keyName, Object value);

    Object getField(String keyName);
}

