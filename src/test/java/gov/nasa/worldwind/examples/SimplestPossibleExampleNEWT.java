package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;
import spacegraph.video.JoglWindow;

public class SimplestPossibleExampleNEWT  { ;

    public static void main(String[] args) {
        new WorldWindowNEWT(new BasicModel()).setWindow(new JoglWindow(1000, 800));
    }

}
