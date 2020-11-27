/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.dataimporter;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.examples.render.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.util.ExampleUtil;

import javax.swing.event.*;
import java.util.*;

/**
 * Highlights a filestore data set by showing its bounding sector.
 *
 * @author tag
 * @version $Id: FileStoreSectorHighlighter.java 1180 2013-02-15 18:40:47Z tgaskins $
 */
public class FileStoreSectorHighlighter implements ListSelectionListener, SelectListener {
    protected static final String SECTOR_LAYER = "SectorLayer";

    protected final FileStorePanel fileStorePanel;
    protected final WorldWindow wwd;
    final Collection<FileStoreDataSet> currentlyHighlightedSets = new ArrayList<>();

    public FileStoreSectorHighlighter(WorldWindow wwd, FileStorePanel panel) {
        this.wwd = wwd;
        this.fileStorePanel = panel;

        this.wwd.addSelectListener(this); // to get sector pick events
        this.fileStorePanel.addSelectionListener(this); // to get notification of table selection changes
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        // This method is called when the table selection changes.

        if (listSelectionEvent.getValueIsAdjusting())
            return;

        this.handleSelection(this.fileStorePanel.getSelectedDataSets());
    }

    protected void handleSelection(List<FileStoreDataSet> selectedDataSets) {
        this.unHighlightSelectedSets();

        if (selectedDataSets == null || selectedDataSets.isEmpty())
            return;

        Sector overallSector = this.highlightSelectedSets(selectedDataSets);
        if (overallSector != null)
            ExampleUtil.goTo(this.wwd, overallSector);
        else
            this.wwd.redraw();
    }

    protected void unHighlightSelectedSets() {
        for (FileStoreDataSet dataSet : this.currentlyHighlightedSets) {
            Layer layer = (Layer) dataSet.getValue(SECTOR_LAYER);
            if (layer != null) {
                this.wwd.model().getLayers().remove(layer);
            }
        }

        this.currentlyHighlightedSets.clear();
    }

    protected Sector highlightSelectedSets(Iterable<FileStoreDataSet> dataSets) {
        Sector overallSector = null;

        for (FileStoreDataSet dataSet : dataSets) {
            Layer layer = (Layer) dataSet.getValue(SECTOR_LAYER);
            if (layer == null) {
                layer = createSectorLayer(dataSet);
                layer.setValue("FileStoreDataSet", dataSet);
                layer.setValue(AVKey.IGNORE, true);
            }

            this.currentlyHighlightedSets.add(dataSet);
            ApplicationTemplate.insertBeforePlacenames(this.wwd, layer);

            Sector sector = dataSet.getSector();
            if (sector != null)
                overallSector = overallSector == null ? sector : overallSector.union(sector);
        }

        return overallSector;
    }

    protected Layer createSectorLayer(FileStoreDataSet dataSet) {
        RenderableLayer layer = new RenderableLayer();
        dataSet.setValue(SECTOR_LAYER, layer);

        this.populateLayer(dataSet, layer);

        return layer;
    }

    protected void populateLayer(FileStoreDataSet dataSet, RenderableLayer layer) {
        Sector sector = (Sector) dataSet.getValue(AVKey.SECTOR);
        if (sector == null)
            return;

        ShapeAttributes attrs = new BasicShapeAttributes();
        attrs.setOutlineMaterial(new Material(dataSet.getColor()));
        attrs.setOutlineWidth(2);

        List<LatLon> locations = sector.asList();
        List<Position> positions = new ArrayList<>(5);
        for (LatLon location : locations) {
            positions.add(new Position(location, 0));
        }
        positions.add(new Position(locations.get(0), 0)); // to form a closed path
        // Work around a bug in Path that does not tessellate the final segment.
        positions.add(new Position(locations.get(0), 0)); // to form a closed path

        Path path = new Path(positions);
        path.setSurfacePath(true);
        path.setAttributes(attrs);

        layer.add(path);
    }

    @Override
    public void selected(SelectEvent event) {
        // This method is called when the user picks a displayed sector. It ensures that the corresponding data set is
        // visible in the installed-data table.

        if (!event.getEventAction().equals(SelectEvent.LEFT_CLICK))
            return;

        if (!(event.getTopObject() instanceof Path))
            return;

        FileStoreDataSet dataSet = (FileStoreDataSet) event.getTopPickedObject().getParentLayer().getValue
            ("FileStoreDataSet");
        if (dataSet == null)
            return;

        this.fileStorePanel.scrollToDataSet(dataSet);
    }
}