package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.formats.geojson.GeoJSONDoc;

import java.io.*;
import java.util.*;

public class TestParser {

    private final ArrayList<AOLFlightPlan> plans = new ArrayList<>();
    private final ArrayList<AOLPosition> positions = new ArrayList<>();

    public static void main(String[] args) {
        try {
            TestParser tp = new TestParser();
            tp.parseMessages("/home/mpeterson/d/temp/aol-data");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void parseMessages(String path) throws Exception {
        File messageDir = new File(path);
        FilenameFilter filter = (File directory, String fileName) -> fileName.startsWith("message")
            && fileName.endsWith(".json");
        File[] messageList = messageDir.listFiles(filter);
        for (File f : messageList) {
            GeoJSONDoc messageJson = new GeoJSONDoc(f);
            Object root = messageJson.getRoot();
            if (root instanceof Object[]) {
                Object[] rootArray = (Object[]) root;
                for (Object o : rootArray) {
                    if (o instanceof AVList) {
                        AVList avl = (AVList) o;
                        Set<Map.Entry<String, Object>> entries = avl.getEntries();
                        entries.forEach((e) -> {
                            switch (e.getKey()) {
                                case "MessageAolFlightPlan" -> plans.add(new AOLFlightPlan((AVList) e.getValue()));
                                case "MessageAolPosition" -> positions.add(new AOLPosition((AVList) e.getValue()));
                                default -> System.out.println("Unknown key:" + e.getKey());
                            }
                        });
                    }
                }
            }
        }
    }

    public ArrayList<AOLFlightPlan> getPlans() {
        return this.plans;
    }

    public ArrayList<AOLPosition> getPositions() {
        return this.positions;
    }
}
