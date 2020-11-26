package gov.nasa.worldwindx.examples;

import gov.nasa.worldwind.avlist.AVList;

public class AltitudeBound {

    private double altitude_value;
    private String vertical_reference;
    private String units_of_measure;
    private String source;

    public AltitudeBound(AVList attrs) {
        attrs.getEntries().forEach((e) -> {
            switch (e.getKey()) {
                case "altitude_value" -> this.altitude_value = (double) e.getValue();
                case "vertical_reference" -> this.vertical_reference = e.getValue().toString();
                case "units_of_measure" -> this.units_of_measure = e.getValue().toString();
                case "source" -> this.source = e.getValue().toString();
                default -> System.out.println("Unknown attribute.");
            }
        });
    }

    public double getAltitudeValue() {
        return altitude_value;
    }
}
