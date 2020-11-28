/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import com.jogamp.opengl.GLAutoDrawable;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ui.WorldWindowGLAutoDrawable;
import gov.nasa.worldwind.ui.newt.WorldWindowNEWT;
import gov.nasa.worldwind.util.tree.*;

/**
 * This example demonstrates the use of the on-screen tree control using {@link BasicTree}.
 *
 * @author pabercrombie
 * @version $Id: TreeControl.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class TreeControl extends ApplicationTemplate {
    private static final String ICON_PATH = "images/16x16-icon-nasa.png";

    public static void main(String[] args) {
        BasicModel m = new BasicModel();
        WorldWindowNEWT ww = new WorldWindowNEWT(m, 1024, 800) {
            @Override
            public void init(GLAutoDrawable drawable) {
                super.init(drawable);
                HotSpotController controller = new HotSpotController(wwd());
            }
        };
        WorldWindowGLAutoDrawable w = ww.wwd();

        RenderableLayer layer = new RenderableLayer();

        BasicTree tree = new BasicTree();

        BasicTreeLayout layout = new BasicTreeLayout(tree, 100, 200);
        layout.getFrame().setFrameTitle("TreeControl");
        tree.setLayout(layout);

        BasicTreeModel model = new BasicTreeModel();

        TreeNode root = new BasicTreeNode("Root", ICON_PATH);
        model.setRoot(root);

        BasicTreeNode child = new BasicTreeNode("Child 1", ICON_PATH);
        child.setDescription("This is a child node");
        child.addChild(new BasicTreeNode("Subchild 1,1"));
        child.addChild(new BasicTreeNode("Subchild 1,2"));
        child.addChild(new BasicTreeNode("Subchild 1,3", ICON_PATH));
        root.addChild(child);

        child = new BasicTreeNode("Child 2", ICON_PATH);
        child.addChild(new BasicTreeNode("Subchild 2,1"));
        child.addChild(new BasicTreeNode("Subchild 2,2"));
        child.addChild(new BasicTreeNode("Subchild 2,3"));
        root.addChild(child);

        child = new BasicTreeNode("Child 3");
        child.addChild(new BasicTreeNode("Subchild 3,1"));
        child.addChild(new BasicTreeNode("Subchild 3,2"));
        child.addChild(new BasicTreeNode("Subchild 3,3"));
        root.addChild(child);

        tree.setModel(model);

        tree.expandPath(root.getPath());


        layer.add(tree);

        // Add the layer to the model.
        WorldWindow.insertBeforeCompass(w, layer);
    }
    public static void main0(String[] args) {

        ApplicationTemplate.start("Tree Control", AppFrame.class);
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        final HotSpotController controller;

        public AppFrame() {
            super(true, true, false);

            RenderableLayer layer = new RenderableLayer();

            BasicTree tree = new BasicTree();

            BasicTreeLayout layout = new BasicTreeLayout(tree, 100, 200);
            layout.getFrame().setFrameTitle("TreeControl");
            tree.setLayout(layout);

            BasicTreeModel model = new BasicTreeModel();

            TreeNode root = new BasicTreeNode("Root", ICON_PATH);
            model.setRoot(root);

            BasicTreeNode child = new BasicTreeNode("Child 1", ICON_PATH);
            child.setDescription("This is a child node");
            child.addChild(new BasicTreeNode("Subchild 1,1"));
            child.addChild(new BasicTreeNode("Subchild 1,2"));
            child.addChild(new BasicTreeNode("Subchild 1,3", ICON_PATH));
            root.addChild(child);

            child = new BasicTreeNode("Child 2", ICON_PATH);
            child.addChild(new BasicTreeNode("Subchild 2,1"));
            child.addChild(new BasicTreeNode("Subchild 2,2"));
            child.addChild(new BasicTreeNode("Subchild 2,3"));
            root.addChild(child);

            child = new BasicTreeNode("Child 3");
            child.addChild(new BasicTreeNode("Subchild 3,1"));
            child.addChild(new BasicTreeNode("Subchild 3,2"));
            child.addChild(new BasicTreeNode("Subchild 3,3"));
            root.addChild(child);

            tree.setModel(model);

            tree.expandPath(root.getPath());

            controller = new HotSpotController(this.wwd());

            layer.add(tree);

            // Add the layer to the model.
            WorldWindow.insertBeforeCompass(this.wwd(), layer);
        }
    }
}
