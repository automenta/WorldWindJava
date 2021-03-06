/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.ogc.wcs.wcs100.*;
import gov.nasa.worldwind.terrain.CompoundElevationModel;

import javax.swing.*;
import javax.swing.border.*;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.net.*;
import java.util.List;
import java.util.*;

/**
 * @author tag
 * @version $Id$
 */
public class WCSCoveragePanel extends JPanel {
    protected final Set<CoverageInfo> coverageInfos = new TreeSet<>((infoA, infoB) -> {
        String nameA = infoA.getTitle();
        String nameB = infoB.getTitle();
        return nameA.compareTo(nameB);
    });
    protected WorldWindow wwd;
    protected URI serverURI;
    protected Dimension size;
    protected Thread loadingThread;

    public WCSCoveragePanel(WorldWindow wwd, String server, Dimension size) throws URISyntaxException {
        super(new BorderLayout());

        // See if the server name is a valid URI. Throw an exception if not.
        this.serverURI = new URI(server.trim()); // throws an exception if server name is not a valid uri.

        this.wwd = wwd;
        this.size = size;
        this.setPreferredSize(this.size);

        this.makeProgressPanel();

        // Thread off a retrieval of the server's capabilities document and update of this panel.
        this.loadingThread = new Thread(this::load);
        this.loadingThread.setPriority(Thread.MIN_PRIORITY);
        this.loadingThread.start();
    }

    protected static Object createComponent(WCS100Capabilities caps, CoverageInfo coverageInfo) {
        KV configParams = coverageInfo.params.copy(); // Copy to insulate changes from the caller.

        // Some wcs servers are slow, so increase the timeouts and limits used by WorldWind's retrievers.
        configParams.set(Keys.URL_CONNECT_TIMEOUT, 30000);
        configParams.set(Keys.URL_READ_TIMEOUT, 30000);
        configParams.set(Keys.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 60000);

        try {
            String describeCoverageUrlString = caps.getCapability().getGetOperationAddress("DescribeCoverage");
            URI uri = new URI(describeCoverageUrlString);
            WCS100DescribeCoverage coverageDescription = WCS100DescribeCoverage.retrieve(uri, coverageInfo.getName());
            coverageDescription.parse();
            configParams.set(Keys.DOCUMENT, coverageDescription);
        }
        catch (URISyntaxException | XMLStreamException e) {
            e.printStackTrace();
            return null;
        }

        try {
            Factory factory = (Factory) WorldWind.createConfigurationComponent(Keys.ELEVATION_MODEL_FACTORY);
            return factory.createFromConfigSource(caps, configParams);
        }
        catch (Exception e) {
            // Ignore the exception, and just return null.
        }

        return null;
    }

    public static void main(String[] args) {
        try {
            final JFrame controlFrame = new JFrame();
            controlFrame.getContentPane().add(new WCSCoveragePanel(null, "https://worldwind26.arc.nasa.gov/wcs?",
                new Dimension(400, 600)));
            controlFrame.pack();
            controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            controlFrame.setVisible(true);
            EventQueue.invokeLater(() -> controlFrame.setVisible(true));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getServerDisplayString() {
        return this.serverURI.getHost();
    }

    protected void load() {
        WCS100Capabilities caps;

        try {
            caps = WCS100Capabilities.retrieve(this.serverURI);
            caps.parse();
        }
        catch (Exception e) {
            e.printStackTrace();
            Container c = WCSCoveragePanel.this.getParent();
            c.remove(WCSCoveragePanel.this);
            JOptionPane.showMessageDialog((Component) wwd, "Unable to connect to server " + serverURI.toString(),
                "Server Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final List<WCS100CoverageOfferingBrief> coverages = caps.getContentMetadata().getCoverageOfferings();
        if (coverages == null)
            return;

        try {
            for (WCS100CoverageOfferingBrief coverage : coverages) {
                CoverageInfo coverageInfo = WCSCoveragePanel.createCoverageInfo(caps, coverage);
                WCSCoveragePanel.this.coverageInfos.add(coverageInfo);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Fill the panel with the coverage titles.
        EventQueue.invokeLater(() -> {
            WCSCoveragePanel.this.removeAll();
            makeCoverageInfosPanel(coverageInfos);
        });
    }

    protected void makeCoverageInfosPanel(Iterable<CoverageInfo> coverageInfos) {
        // Create the panel holding the coverage names.
        JPanel layersPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        layersPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Add the server's coverages to the panel.
        for (CoverageInfo coverageInfo : coverageInfos) {
            addCoverageInfoPanel(layersPanel, WCSCoveragePanel.this.wwd, coverageInfo);
        }

        // Put the name panel in a scroll bar.
        JScrollPane scrollPane = new JScrollPane(layersPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scrollPane.setPreferredSize(size);

        // Add the scroll bar and name panel to a titled panel that will resize with the main window.
        JPanel westPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        westPanel.setBorder(
            new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Coverages")));
        westPanel.add(scrollPane);
        this.add(westPanel, BorderLayout.CENTER);

        this.revalidate();
    }

    protected void addCoverageInfoPanel(JPanel coveragesPanel, WorldWindow wwd, CoverageInfo coverageInfo) {
        // Give a coverage a button and label and add it to the names panel.

        CoverageInfoAction action = new CoverageInfoAction(coverageInfo, wwd);
        JCheckBox jcb = new JCheckBox(action);
        jcb.setSelected(false);
        coveragesPanel.add(jcb);
    }

    protected static CoverageInfo createCoverageInfo(WCS100Capabilities caps, WCS100CoverageOfferingBrief coverage) {
        // Create the layer info specified by the coverage capabilities.

        CoverageInfo info = new CoverageInfo();
        info.caps = caps;
        info.params = new KVMap();
        info.params.set(Keys.COVERAGE_IDENTIFIERS, coverage.getName());
        info.params.set(Keys.DISPLAY_NAME, coverage.getLabel());

        return info;
    }

    protected void updateComponent(Object component, boolean enable) {
        ElevationModel model = (ElevationModel) component;
        CompoundElevationModel compoundModel =
            (CompoundElevationModel) this.wwd.model().globe().getElevationModel();

        if (enable) {
            if (!compoundModel.getElevationModels().contains(model))
                compoundModel.addElevationModel(model);
        }
        else {
            compoundModel.removeElevationModel(model);
        }

        wwd.emit(new PropertyChangeEvent(wwd, Keys.ELEVATION_MODEL, null, compoundModel));
    }

    protected void makeProgressPanel() {
        // Create the panel holding the progress bar during loading.

        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        outerPanel.setPreferredSize(this.size);

        JPanel innerPanel = new JPanel(new BorderLayout());
        innerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        innerPanel.add(progressBar, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        innerPanel.add(cancelButton, BorderLayout.EAST);
        cancelButton.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (loadingThread.isAlive())
                    loadingThread.interrupt();

                Container c = WCSCoveragePanel.this.getParent();
                c.remove(WCSCoveragePanel.this);
            }
        });

        outerPanel.add(innerPanel, BorderLayout.NORTH);
        this.add(outerPanel, BorderLayout.CENTER);
        this.revalidate();
    }

    protected static class CoverageInfo {
        protected WCS100Capabilities caps;
        protected KV params = new KVMap();

        protected String getTitle() {
            return params.getStringValue(Keys.DISPLAY_NAME);
        }

        protected String getName() {
            return params.getStringValue(Keys.COVERAGE_IDENTIFIERS);
        }
    }

    protected class CoverageInfoAction extends AbstractAction {
        protected final WorldWindow wwd;
        protected final CoverageInfo coverageInfo;
        protected Object component;

        public CoverageInfoAction(CoverageInfo info, WorldWindow wwd) {
            super(info.getTitle());

            // Capture info we'll need later to control the coverage.
            this.wwd = wwd;
            this.coverageInfo = info;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            // If the coverage is selected, add it to the WorldWindow's current model, else remove it from the model.
            if (((AbstractButton) actionEvent.getSource()).isSelected()) {
                if (this.component == null)
                    this.component = createComponent(coverageInfo.caps, coverageInfo);

                updateComponent(this.component, true);
            }
            else {
                if (this.component != null)
                    updateComponent(this.component, false);
            }

            // Tell the WorldWindow to update.
            wwd.redraw();
        }
    }
}