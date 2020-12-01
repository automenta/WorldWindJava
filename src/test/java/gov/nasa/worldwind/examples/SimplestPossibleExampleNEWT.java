package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.video.newt.WorldWindowNEWT;

public class SimplestPossibleExampleNEWT  { ;

    public static void main(String[] args) {
        new WorldWindowNEWT(new BasicModel(), 1000, 800);
    }

}
