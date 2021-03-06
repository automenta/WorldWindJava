package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.formats.geojson.GeoJSONPolygon;

public class OperationVolume {

    private String volumn_type;
    private AltitudeBound min_altitude;
    private AltitudeBound max_altitude;
    private String effective_time_begin;
    private String effective_time_end;
    private boolean beyond_visual_line_of_sight;
    private GeoJSONPolygon flight_geography;
    private String ordinal;
    private String near_structure;

    public OperationVolume(KV attrs) {
        attrs.getEntries().forEach((e) -> {
            switch (e.getKey()) {
                case "volume_type" -> this.volumn_type = e.getValue().toString();
                case "min_altitude" -> this.min_altitude = new AltitudeBound((KV) e.getValue());
                case "max_altitude" -> this.max_altitude = new AltitudeBound((KV) e.getValue());
                case "effective_time_begin" -> this.effective_time_begin = e.getValue().toString();
                case "effective_time_end" -> this.effective_time_end = e.getValue().toString();
                case "beyond_visual_line_of_sight" -> this.beyond_visual_line_of_sight = (boolean) e.getValue();
                case "flight_geography" -> this.flight_geography = (GeoJSONPolygon) e.getValue();
                case "ordinal" -> this.ordinal = e.getValue().toString();
                case "near_structure" -> this.near_structure = e.getValue().toString();
                default -> System.out.println(e.getKey() + "," + e.getValue());
            }
        });
    }

    /**
     * @return the min_altitude
     */
    public AltitudeBound getMin_altitude() {
        return min_altitude;
    }

    /**
     * @return the max_altitude
     */
    public AltitudeBound getMax_altitude() {
        return max_altitude;
    }

    /**
     * @return the flight_geography
     */
    public GeoJSONPolygon getFlight_geography() {
        return flight_geography;
    }
}