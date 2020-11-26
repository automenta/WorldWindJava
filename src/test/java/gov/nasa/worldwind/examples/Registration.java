package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.avlist.AVList;

public class Registration {

    private String registration_id;
    private String registration_location;

    public Registration(AVList attrs) {
        attrs.getEntries().forEach((e) -> {
            switch (e.getKey()) {
                case "registration_id" -> this.registration_id = e.getValue().toString();
                case "registration_location" -> this.registration_location = e.getValue().toString();
                default -> System.out.println("Unknown attribute.");
            }
        });
    }
}
