package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Position;

public class AOLPosition {

    private String callsign;
    private String gufi;
    private String hpr;
    private Position lla;
    private String groundSpeed;
    private String misc;
    private String time;
    private String time_measured;
    private String time_sent;
    private String uss_name;

    public AOLPosition(AVList attrs) {
        attrs.getEntries().forEach((e) -> {
            switch (e.getKey()) {
                case "callsign" -> this.callsign = e.getValue().toString();
                case "gufi" -> this.gufi = e.getValue().toString();
                case "hpr" -> this.hpr = e.getValue().toString();
                case "lla" -> {
                    Object[] coords = (Object[]) e.getValue();
                    this.lla = Position.fromDegrees((Double) coords[0], (Double) coords[1], (Double) coords[2]);
                }
                case "groundSpeed" -> this.groundSpeed = e.getValue().toString();
                case "misc" -> this.misc = e.getValue().toString();
                case "time" -> this.time = e.getValue().toString();
                case "time_measured" -> this.time_measured = e.getValue().toString();
                case "time_sent" -> this.time_sent = e.getValue().toString();
                case "uss_name" -> this.uss_name = e.getValue().toString();
                default -> System.out.println("Unknown attribute");
            }
        });
    }

    public String getGufi() {
        return this.gufi;
    }

    public Position getLLA() {
        return this.lla;
    }
}
