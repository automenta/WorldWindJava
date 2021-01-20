/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.features;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.KV;
import gov.nasa.worldwind.examples.worldwindow.core.*;
import gov.nasa.worldwind.examples.worldwindow.core.layermanager.*;
import gov.nasa.worldwind.examples.worldwindow.features.swinglayermanager.LayerTree;
import gov.nasa.worldwind.examples.worldwindow.features.swinglayermanager.*;
import gov.nasa.worldwind.examples.worldwindow.util.*;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.ogc.wms.*;
import gov.nasa.worldwind.layers.wms.CapabilitiesRequest;
import gov.nasa.worldwind.util.WWUtil;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.net.*;
import java.util.List;
import java.util.*;
import java.util.logging.Level;

/**
 * @author tag
 * @version $Id: WMSPanel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
@SuppressWarnings("unchecked")
public class WMSPanel extends AbstractFeaturePanel implements TreeModelListener, NetworkActivitySignal.NetworkUser {
    protected static final String FEATURE_TITLE = "WMS Server Panel";
    protected static final String ICON_PATH = "gov/nasa/worldwind/examples/worldwindow/images/wms-64x64.png";

    protected LayerTree layerTree;
    protected JTextField nameField;
    protected JTextField urlField;
    protected JButton infoButton;
    protected Thread loadingThread;
    protected URI serverURI;

    public WMSPanel(Registry registry) {
        super(FEATURE_TITLE, Constants.FEATURE_WMS_PANEL, new ShadedPanel(new BorderLayout()), registry);
    }

    public String getURLString() {
        return this.urlField.getText();
    }

    public boolean hasNetworkActivity() {
        return this.loadingThread != null && this.loadingThread.isAlive();
    }

    public void initialize(final Controller controller) {
        super.initialize(controller);

        LayerTreeModel model = new LayerTreeModel();
        this.layerTree = new LayerTree(model);
        this.layerTree.getModel().addTreeModelListener(this); // listen for changes to the model

        JPanel np = new JPanel(new BorderLayout(5, 5));
        np.setOpaque(false);
        createComponents(np);

        JPanel np2 = new JPanel(new BorderLayout());
        np2.setOpaque(false);
        np2.setBorder(new EmptyBorder(10, 10, 10, 10));
        np2.add(np, BorderLayout.CENTER);

        this.panel.setOpaque(false);
        this.panel.add(np2, BorderLayout.CENTER);
        this.panel.setToolTipText("");

        // listen for triggers to cause WMS server contact
        this.urlField.addActionListener(actionEvent -> {
            try {
                String serverURLString = urlField.getText();
                if (!WWUtil.isEmpty(serverURLString)) {
                    if (serverURI == null || !serverURI.toString().contains(serverURLString)) {
                        if (getTopGroup() != null)
                            firePropertyChange("NewServer", null, serverURLString);
                        else
                            contactWMSServer(serverURLString);
                    }
                }
            }
            catch (URISyntaxException e) {
                String msg = "Invalid URL";
                Util.getLogger().log(Level.SEVERE, msg, e);
                controller.showErrorDialog(e, "Invalid URL", msg);
            }
        });

        this.infoButton.addActionListener(actionEvent -> {
            String urlString = (String) infoButton.getClientProperty("CapsURL");
            if (!WWUtil.isEmpty(urlString)) {
                controller.openLink(urlString);
            }
        });
    }

    /**
     * Called when the tree cell check box changes. Adds and removes the selected layer in the layer manager.
     *
     * @param event the description of the change.
     */
    public void treeNodesChanged(TreeModelEvent event) {
        Object[] changedNodes = event.getChildren();
        if (changedNodes == null || changedNodes.length <= 0)
            return;

        for (Object o : changedNodes) {
            if (o == null)
                continue;

            if (o instanceof LayerTreeGroupNode)
                this.handleGroupSelection((LayerTreeGroupNode) o);
            else if (o instanceof LayerNode)
                this.handleLayerSelection((LayerNode) o);
        }
    }

    protected void handleLayerSelection(LayerNode layerNode) {
        if (layerNode.getWmsLayerInfo() == null)
            return;

        LayerManager layerManager = controller.getLayerManager();

        if (layerNode.isSelected()) {
            if (layerNode.getLayer() == null)
                try {
                    WMSPanel.createLayer(layerNode);
                }
                catch (Exception e) {
                    String msg = "Error creating WMS layer " + layerNode.toString();
                    Util.getLogger().log(Level.SEVERE, msg, e);
                    this.controller.showErrorDialog(e, "WMS Error", msg);
                }

            if (layerNode.getLayer() != null) {
                layerManager.addLayer(layerNode.getLayer(), new LayerPath(this.nameField.getText()));
                layerManager.selectLayer(layerNode.getLayer(), true);
            }
        }
        else {
            layerManager.removeLayer(layerNode.getLayer());
        }
    }

    protected void handleGroupSelection(LayerTreeGroupNode groupNode) {
        Enumeration iter = groupNode.breadthFirstEnumeration();
        while (iter.hasMoreElements()) {
            Object o = iter.nextElement();
            if (!(o instanceof LayerNode) || (o instanceof LayerTreeGroupNode))
                continue;

            LayerNode layerNode = (LayerNode) o;
            layerNode.setSelected(groupNode.isSelected());
            this.handleLayerSelection(layerNode);
        }

        LayerNode topNode = this.getLayerManagerGroupNode();
        if (topNode != null) {
            topNode.setSelected(groupNode.isSelected());
            this.controller.getLayerManager().expandGroup(topNode.getTitle());
        }
        this.layerTree.repaint();
    }

    protected static void createLayer(LayerNode layerNode) {
        if (layerNode == null) {
            String msg = "LayerNode is null";
            Util.getLogger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (layerNode.getWmsLayerInfo() != null) {
            WMSLayerInfo wmsInfo = layerNode.getWmsLayerInfo();
            KV configParams = wmsInfo.getParams().copy(); // Copy to insulate changes from the caller.

            // Some wms servers are slow, so increase the timeouts and limits used by WorldWind's retrievers.
            configParams.set(Keys.URL_CONNECT_TIMEOUT, 30000);
            configParams.set(Keys.URL_READ_TIMEOUT, 30000);
            configParams.set(Keys.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 60000);

            Factory factory = (Factory) WorldWind.createConfigurationComponent(Keys.LAYER_FACTORY);
            Layer layer = (Layer) factory.createFromConfigSource(wmsInfo.getCaps(), configParams);
            layerNode.setLayer(layer);
        }
    }

    public void treeNodesInserted(TreeModelEvent treeModelEvent) // unused
    {
    }

    public void treeNodesRemoved(TreeModelEvent treeModelEvent) // unused
    {
    }

    public void treeStructureChanged(TreeModelEvent treeModelEvent) // unused
    {
    }

    public void cancel() {
        if (this.loadingThread != null && this.loadingThread.isAlive())
            this.loadingThread.interrupt();
    }

    /**
     * Clears the panel. Any layers currently in the layer manager are left there.
     */
    public void clearPanel() {
        if (this.loadingThread != null && this.loadingThread.isAlive()) {
            this.loadingThread.interrupt();
            return; // just cancel any retrieval the first time called
        }

        if (this.nameField != null)
            this.nameField.setText("");

        if (this.urlField != null)
            this.urlField.setText("");

        if (this.layerTree != null) {
            this.layerTree.clearTree();
        }
    }

    /**
     * Contact the specified WMS server to get its capabilities document. Then load the layer tree with its contents.
     *
     * @param URLString a text string containing the server's URL.
     * @throws URISyntaxException if the URL is invalid.
     */
    public void contactWMSServer(String URLString) throws URISyntaxException {
        this.serverURI = new URI(URLString.trim()); // throws an exception if server name is not a valid uri.

        // Thread off a retrieval of the server's capabilities document and update of this panel.
        this.loadingThread = new Thread(() -> {
            controller.getNetworkActivitySignal().addNetworkUser(WMSPanel.this);
            try {
                CapabilitiesRequest request = new CapabilitiesRequest(serverURI);
                WMSCapabilities caps = new WMSCapabilities(request);
                caps.parse();
                if (!Thread.currentThread().isInterrupted())
                    createLayerList(caps);
            }
            catch (XMLStreamException e) {
                String msg = "Error retrieving servers capabilities " + serverURI;
                Util.getLogger().log(Level.SEVERE, msg, e);
                controller.showErrorDialog(e, "Get Capabilities Error", msg);
            }
            catch (Exception e) {
                String msg;
                if (e.getClass().getName().toLowerCase().contains("timeout")) {
                    msg = "Connection to server timed out\n" + serverURI;
                    controller.showErrorDialog(e, "Connection Timeout", msg);
                }
                else {
                    msg = "Attempt to contact server failed\n" + serverURI;
                    controller.showErrorDialog(e, "Server Not Responding", msg);
                }
                Util.getLogger().log(Level.SEVERE, msg + serverURI, e);
            }
            finally // ensure that the cursor is restored to default whether succes or failure
            {
                EventQueue.invokeLater(() -> {
                    controller.getNetworkActivitySignal().removeNetworkUser(WMSPanel.this);
                    panel.setCursor(Cursor.getDefaultCursor());
                });
            }
        });

        this.loadingThread.setPriority(Thread.MIN_PRIORITY);
        this.loadingThread.start();
        this.panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    protected void createLayerList(final WMSCapabilities caps) {
        java.util.List<WMSLayerCapabilities> layers = caps.getCapabilityInformation().getLayerCapabilities();
        if (layers.isEmpty())
            return;

        // TODO: Make the list for all top-level layers if more than one.
        WMSLayerCapabilities layer = layers.get(0);
        addLayer(caps, layer, null, (LayerTreeModel) this.layerTree.getModel());

        LayerTreeGroupNode topGroupNode = this.getTopGroup();
        if (topGroupNode != null)
            topGroupNode.setEnableSelectionBox(false); // Prevents user from selecting all the server's layers at once.

        String docAbstract = caps.getServiceInformation().getServiceAbstract();
        if (docAbstract != null)
            this.infoButton.setToolTipText(Util.makeMultiLineToolTip(docAbstract));
        String infoUrl = caps.getServiceInformation().getOnlineResource().getHref();
        this.infoButton.putClientProperty("CapsURL", infoUrl != null ? infoUrl
            : caps.getRequestURL("GetCapabilities", "HTTP", "Get"));

        // UI changes should be finalized on the EDT
        EventQueue.invokeLater(() -> {
            if (nameField.getText() == null || nameField.getText().isEmpty())
                nameField.setText(getServerDisplayString(caps));

            urlField.setText(serverURI.toString());//SelectedItem(serverURI.toString());

            layerTree.expandRow(0); // ensure that the top grouping layer is expanded
        });
    }

    protected LayerTreeGroupNode getTopGroup() {
        Object root = this.layerTree.getModel().getRoot();
        return root instanceof LayerTreeGroupNode
            && ((TreeNode) root).getChildCount() > 0 ?
            (LayerTreeGroupNode) ((DefaultMutableTreeNode) root).getFirstChild() : null;
    }

    protected LayerNode getLayerManagerGroupNode() {
        LayerTreeGroupNode localTopNode = this.getTopGroup();
        if (localTopNode == null)
            return null;

        LayerPath path = new LayerPath(this.getTabTitle());

        return this.controller.getLayerManager().getNode(path);
    }

    public static void addItemToComboBox(JComboBox cmb, Object item) {
        if (cmb == null || item == null)
            return;

        for (int i = 0; i < cmb.getItemCount(); i++) {
            Object oi = cmb.getItemAt(i);
            if (oi != null && oi.toString().trim().equals(item.toString().trim()))
                return;
        }

        cmb.insertItemAt(item, 1);
    }

    protected String getServerDisplayString(WMSCapabilities caps) {
        String title = caps.getServiceInformation().getServiceTitle();
        return title != null ? title : this.serverURI.getHost();
    }

    /**
     * Recursively adds layers to the layer tree.
     *
     * @param caps      the server's capabilities document.
     * @param layerCaps the DOM description of the layer to retrieve.
     * @param groupNode the display group
     * @param model     the layer tree model
     */
    protected static void addLayer(WMSCapabilities caps, WMSLayerCapabilities layerCaps, LayerTreeGroupNode groupNode,
        LayerTreeModel model) {
        java.util.List<WMSLayerCapabilities> subLayers = layerCaps.getLayers();
        if (subLayers != null && !subLayers.isEmpty()) // it's a grouping layer
        {
            LayerTreeGroupNode subGroupNode = new LayerTreeGroupNode(new WMSLayerInfo(caps, layerCaps, null));

            if (groupNode == null)
                model.insertNodeInto(subGroupNode, (MutableTreeNode) model.getRoot(),
                    ((TreeNode) model.getRoot()).getChildCount());
            else
                model.insertNodeInto(subGroupNode, groupNode, groupNode.getChildCount());

            for (WMSLayerCapabilities subLayerCaps : subLayers) {
                addLayer(caps, subLayerCaps, subGroupNode, model);
            }

            String toolTipText = layerCaps.getLayerAbstract();
            if (!WWUtil.isEmpty(toolTipText))
                subGroupNode.setToolTipText(Util.makeMultiLineToolTip(toolTipText));
        }
        else // it's a leaf layer
        {
            List<WMSLayerInfo> layerInfos = WMSLayerInfo.createLayerInfos(caps, layerCaps);
            for (WMSLayerInfo layerInfo : layerInfos) {
                LayerTreeNode layerNode = new LayerTreeNode(layerInfo);
                layerNode.setSelected(false);
                layerNode.setAllowsChildren(false); // indicates that the node is a leaf
                if (groupNode == null)
                    model.insertNodeInto(layerNode, (MutableTreeNode) model.getRoot(),
                        ((TreeNode) model.getRoot()).getChildCount());
                else
                    model.insertNodeInto(layerNode, groupNode, groupNode.getChildCount());

                String toolTipText = layerCaps.getLayerAbstract();
                if (!WWUtil.isEmpty(toolTipText))
                    layerNode.setToolTipText(Util.makeMultiLineToolTip(toolTipText));
            }
        }
    }

    protected void createComponents(JPanel panel) {
        panel.add(makeTopPanel(), BorderLayout.NORTH);
        panel.add(makeTreePanel(), BorderLayout.CENTER);
    }

    protected JPanel makeTopPanel() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setOpaque(false);

        final JLabel nameLabel = new JLabel("Name");
        nameLabel.setOpaque(false);

        this.nameField = new JTextField(20);
        this.nameField.setOpaque(false);
        this.nameField.setToolTipText("Enter a display name for the server");

        this.infoButton = new JButton(
            ImageLibrary.getIcon("gov/nasa/worldwind/examples/worldwindow/images/info-20x20.png"));
        this.infoButton.setOpaque(false);
        this.infoButton.setBackground(new Color(0, 0, 0, 0));
        this.infoButton.setBorderPainted(false);

        JLabel urlLabel = new JLabel("URL");
        urlLabel.setOpaque(false);

        this.urlField = new JTextField();//JComboBox(INITIAL_SERVER_LIST);
        this.urlField.setOpaque(false);
        this.urlField.setEditable(true);
        this.urlField.setToolTipText("Enter a WMS server URL");

        int t = 5, l = 5, b = 5, r = 5;
        topPanel.add(nameLabel, new GB(0, 0).setWeight(0, 0).setAnchor(GB.WEST).setInsets(t, l, b, r));
        topPanel.add(this.nameField, new GB(1, 0).setWeight(100, 100).setAnchor(GB.WEST).setInsets(t, l, b, r).setFill(
            GridBagConstraints.HORIZONTAL));
        topPanel.add(this.infoButton, new GB(2, 0).setWeight(0, 0).setAnchor(GB.WEST).setInsets(t, l, b, r));
        topPanel.add(urlLabel, new GB(0, 1).setWeight(0, 0).setAnchor(GB.WEST).setInsets(t, l, b, r));
        topPanel.add(this.urlField,
            new GB(1, 1, 2, 1).setWeight(100, 100).setAnchor(GB.WEST).setInsets(t, l, b, r).setFill(
                GridBagConstraints.HORIZONTAL));

        // Inform the parent tabbed pane as the user enters the server name
        this.nameField.getDocument().addUndoableEditListener(event -> {
            if (nameField.getText().trim().length() <= 0)
                return;

            // Change the layer name in the application's layer manager
            LayerNode lmGroupNode = getLayerManagerGroupNode();
            if (lmGroupNode != null) {
                lmGroupNode.setTitle(nameField.getText());
                controller.getLayerManager().redraw();
            }

            // Change the tabbed-pane title
            setTabTitle(nameField.getText());
        });

        return topPanel;
    }

    protected String getTabTitle() {
        Container parent = panel.getParent();
        if (parent instanceof JTabbedPane) {
            int index = ((JTabbedPane) panel.getParent()).indexOfComponent(panel);
            return ((JTabbedPane) panel.getParent()).getTitleAt(index);
        }

        return null;
    }

    protected void setTabTitle(String title) {
        Container parent = panel.getParent();
        if (parent instanceof JTabbedPane) {
            int index = ((JTabbedPane) panel.getParent()).indexOfComponent(panel);
            ((JTabbedPane) panel.getParent()).setTitleAt(index, title != null ? title : "");
        }
    }

    protected JPanel makeTreePanel() {
        this.layerTree.setBorder(new EmptyBorder(10, 10, 10, 10));
        this.layerTree.setVisibleRowCount(15);

        JScrollPane scrollPane = new JScrollPane(layerTree);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        JPanel treePanel = new JPanel(new BorderLayout(5, 5));
        treePanel.setOpaque(false);
        treePanel.add(scrollPane, BorderLayout.CENTER);

        return treePanel;
    }
}