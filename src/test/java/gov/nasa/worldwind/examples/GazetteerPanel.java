/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.exception.NoItemException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.poi.*;
import gov.nasa.worldwind.view.orbit.OrbitView;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.regex.*;

import static java.lang.Math.toRadians;

/**
 * Gazetteer search panel that allows the user to enter a search term in a text field. When a search is performed the
 * view will animate to the top search result.
 *
 * @author tag
 * @version $Id: GazetteerPanel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@SuppressWarnings("unchecked")
public class GazetteerPanel extends JPanel {
    private final WorldWindow wwd;
    private final Gazetteer gazeteer;
    private final JPanel resultsPanel;
    private final JComboBox resultsBox;

    /**
     * Create a new panel.
     *
     * @param wwd                WorldWindow to animate when a search is performed.
     * @param gazetteerClassName Name of the gazetteer class to instantiate. If this parameter is {@code null} a {@link
     *                           YahooGazetteer} is instantiated.
     * @throws IllegalAccessException                      if the gazetteer class does not expose a publicly accessible
     *                                                     no-arg constructor.
     * @throws InstantiationException                      if an exception occurs while instantiating the the gazetteer
     *                                                     class.
     * @throws ClassNotFoundException                      if the gazetteer class cannot be found.
     * @throws NoSuchMethodException             if the gazetteer class doesn't have a default constructor.
     * @throws InvocationTargetException if the gazetteer class construction fails.
     */
    public GazetteerPanel(final WorldWindow wwd, String gazetteerClassName)
        throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException,
        InvocationTargetException {
        super(new BorderLayout());

        if (gazetteerClassName != null)
            this.gazeteer = GazetteerPanel.constructGazetteer(gazetteerClassName);
        else
            this.gazeteer = new YahooGazetteer();

        this.wwd = wwd;

        // The label
        URL imageURL = this.getClass().getResource("/images/32x32-icon-earth.png");
        ImageIcon icon = new ImageIcon(imageURL);
        JLabel label = new JLabel(icon);
        label.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        // The text field
        final JTextField field = new JTextField("Name or Lat,Lon?");
        field.addActionListener(actionEvent -> EventQueue.invokeLater(() -> {
            try {
                handleEntryAction(actionEvent);
            }
            catch (NoItemException e) {
                JOptionPane.showMessageDialog(GazetteerPanel.this,
                    "Location not available \"" + (field.getText() != null ? field.getText() : "") + "\"\n"
                        + "(" + e.getMessage() + ")",
                    "Location Not Available", JOptionPane.ERROR_MESSAGE);
            }
            catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(GazetteerPanel.this,
                    "Error parsing input \"" + (field.getText() != null ? field.getText() : "") + "\"\n"
                        + e.getMessage(),
                    "Lookup Failure", JOptionPane.ERROR_MESSAGE);
            }
            catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(GazetteerPanel.this,
                    "Error looking up \"" + (field.getText() != null ? field.getText() : "") + "\"\n"
                        + e.getMessage(),
                    "Lookup Failure", JOptionPane.ERROR_MESSAGE);
            }
        }));

        // Enclose entry field in an inner panel in order to control spacing/padding
        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.add(field, BorderLayout.CENTER);
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        fieldPanel.setPreferredSize(new Dimension(100, 30));

        // Put everything together
        this.add(label, BorderLayout.WEST);
        this.add(fieldPanel, BorderLayout.CENTER);

        resultsPanel = new JPanel(new FlowLayout());
        resultsPanel.add(new JLabel("Results: "));
        resultsBox = new JComboBox();
        resultsBox.setPreferredSize(new Dimension(300, 30));
        resultsBox.addActionListener(actionEvent -> EventQueue.invokeLater(() -> {
            JComboBox cb = (JComboBox) actionEvent.getSource();
            PointOfInterest selectedPoi = (PointOfInterest) cb.getSelectedItem();
            moveToLocation(selectedPoi);
        }));
        resultsPanel.add(resultsBox);
        resultsPanel.setVisible(false);
        this.add(resultsPanel, BorderLayout.EAST);
    }

    private static Gazetteer constructGazetteer(String className)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException,
        InvocationTargetException {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Gazetteer class name is null");
        }

        Class c = Class.forName(className.trim());
        Object o = c.getConstructor().newInstance();

        if (!(o instanceof Gazetteer))
            throw new IllegalArgumentException("Gazetteer class name is null");

        return (Gazetteer) o;
    }

    private void handleEntryAction(ActionEvent actionEvent) throws
        NoItemException, IllegalArgumentException {
        String lookupString = null;

        //hide any previous results
        resultsPanel.setVisible(false);
        if (actionEvent.getSource() instanceof JTextComponent)
            lookupString = ((JTextComponent) actionEvent.getSource()).getText();

        if (lookupString == null || lookupString.length() < 1)
            return;

        List<PointOfInterest> poi = parseSearchValues(lookupString);

        if (poi != null) {
            if (poi.size() == 1) {
                this.moveToLocation(poi.get(0));
            }
            else {
                resultsBox.removeAllItems();
                for (PointOfInterest p : poi) {
                    resultsBox.addItem(p);
                }
                resultsPanel.setVisible(true);
            }
        }
    }

    /*
    Sample inputs
    Coordinate formats:
    39.53, -119.816  (Reno, NV)
    21 10 14 N, 86 51 0 W (Cancun)
     */
    private List<PointOfInterest> parseSearchValues(String searchStr) {
        String sepRegex = "[,]"; //other separators??
        searchStr = searchStr.trim();
        String[] searchValues = searchStr.split(sepRegex);
        if (searchValues.length == 1) {
            return queryService(searchValues[0].trim());
        }
        else if (searchValues.length == 2) //possible coordinates
        {
            //any numbers at all?
            String regex = "[0-9]";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(
                searchValues[1]); //Street Address may have numbers in first field so use 2nd
            if (matcher.find()) {
                List<PointOfInterest> list = new ArrayList<>();
                list.add(parseCoordinates(searchValues));
                return list;
            }
            else {
                return queryService(searchValues[0].trim() + "+" + searchValues[1].trim());
            }
        }
        else {
            //build search string and send to service
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < searchValues.length; i++) {
                sb.append(searchValues[i].trim());
                if (i < searchValues.length - 1)
                    sb.append("+");
            }

            return queryService(sb.toString());
        }
    }

    private List<PointOfInterest> queryService(String queryString) {
        List<PointOfInterest> results = this.gazeteer.findPlaces(queryString);
        if (results == null || results.isEmpty())
            return null;
        else
            return results;
    }

    //throws IllegalArgumentException
    private static PointOfInterest parseCoordinates(String[] coords) {
        if (isDecimalDegrees(coords)) {
            double d1 = Double.parseDouble(coords[0].trim());
            double d2 = Double.parseDouble(coords[1].trim());

            return new BasicPointOfInterest(LatLon.fromDegrees(d1, d2));
        }
        else //may be in DMS
        {
            Angle aLat = Angle.fromDMS(coords[0].trim());
            Angle aLon = Angle.fromDMS(coords[1].trim());

            return new BasicPointOfInterest(LatLon.fromDegrees(aLat.getDegrees(), aLon.getDegrees()));
        }
    }

    private static boolean isDecimalDegrees(String[] coords) {
        try {
            Double.parseDouble(coords[0].trim());
            Double.parseDouble(coords[1].trim());
        }
        catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }

    public void moveToLocation(PointOfInterest location) {
        // Use a PanToIterator to iterate view to target position
        this.wwd.view().goTo(new Position(location.getLatlon(), 0), 25.0e3);
    }

    public void moveToLocation(Sector sector, Double altitude) {
        OrbitView view = (OrbitView) this.wwd.view();

        Globe globe = this.wwd.model().getGlobe();

        if (altitude == null || altitude == 0) {
            double t = Math.min(toRadians(sector.lonDelta), toRadians(sector.lonDelta));
            double w = 0.5 * t * 6378137.0;
            altitude = w / this.wwd.view().getFieldOfView().tanHalfAngle();
        }

        if (globe != null && view != null) {
            this.wwd.view().goTo(new Position(sector.getCentroid(), 0), altitude);
        }
    }
}
