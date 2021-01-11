/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import com.jogamp.opengl.*;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * This example illustrates how to change the size of the pick frustum, and how a smaller pick frustum can give better
 * performance.
 *
 * @author Jeff Addison
 * @version $Id: PickFrustum.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class PickFrustum extends ApplicationTemplate {
    public static void main(String[] args) {
        ApplicationTemplate.start("WorldWind Picking Frustum", AppFrame.class);
    }

    public static class PickFrustumLayer extends RenderableLayer {
        protected final OrderedRenderable orderedImage = new OrderedIcon();

        /**
         * A RenderableLayer that displays a box around the PickPointFrustum
         */
        public PickFrustumLayer() {
        }

        @Override
        public void doRender(DrawContext dc) {
            dc.addOrderedRenderable(this.orderedImage);
        }

        @Override
        public void doPick(DrawContext dc, Point pickPoint) {
            // Delegate drawing to the ordered renderable list
            dc.addOrderedRenderable(this.orderedImage);
        }

        public static void draw(DrawContext dc) {
            GL2 gl = dc.getGL2(); // GL initialization checks for GL2 compatibility.
            boolean attribsPushed = false;
            boolean modelviewPushed = false;
            boolean projectionPushed = false;

            ArrayList<PickPointFrustum> frustums = dc.getPickFrustums();

            if (frustums == null)
                return;

            try {
                gl.glPushAttrib(GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_COLOR_BUFFER_BIT | GL2.GL_ENABLE_BIT
                    | GL2.GL_TRANSFORM_BIT | GL2.GL_VIEWPORT_BIT | GL2.GL_CURRENT_BIT);
                attribsPushed = true;

                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                gl.glDisable(GL.GL_DEPTH_TEST);

                // Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
                // into the GL projection matrix.
                Rectangle viewport = dc.getView().getViewport();
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glPushMatrix();
                projectionPushed = true;
                gl.glLoadIdentity();
                gl.glOrtho(0.0d, viewport.width, 0.0d, viewport.height, -1, 1);

                gl.glMatrixMode(GL2.GL_MODELVIEW);
                gl.glPushMatrix();
                modelviewPushed = true;
                gl.glLoadIdentity();

                gl.glLineWidth(2.0f);

                gl.glColor3f(1.0f, 0.0f, 0.0f);

                for (PickPointFrustum frustum : frustums) {
                    Rectangle rect = frustum.getScreenRect();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(rect.getX(), rect.getY(), 0);
                    gl.glVertex3d(rect.getX() + rect.getWidth(), rect.getY(), 0);
                    gl.glVertex3d(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), 0);
                    gl.glVertex3d(rect.getX(), rect.getY() + rect.getHeight(), 0);
                    gl.glVertex3d(rect.getX(), rect.getY(), 0);
                    gl.glEnd();
                }
            }
            finally {
                if (projectionPushed) {
                    gl.glMatrixMode(GL2.GL_PROJECTION);
                    gl.glPopMatrix();
                }
                if (modelviewPushed) {
                    gl.glMatrixMode(GL2.GL_MODELVIEW);
                    gl.glPopMatrix();
                }
                if (attribsPushed)
                    gl.glPopAttrib();
            }
        }

        protected class OrderedIcon implements OrderedRenderable {
            public double getDistanceFromEye() {
                return 0;
            }

            public void pick(DrawContext dc, Point pickPoint) {
                PickFrustumLayer.draw(dc);
            }

            public void render(DrawContext dc) {
                PickFrustumLayer.draw(dc);
            }
        }
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame {
        protected final Layer frustumLayer = new PickFrustumLayer();
        protected JLabel lblDimension;
        protected JLabel frustumDimensionLabel;
        protected JPanel panel;
        protected JSlider slider;
        protected JToggleButton butShowPickingFrustum;
        protected JToggleButton butTogglePickingClipping;
        protected JToggleButton butToggleViewClipping;
        protected WWIcon lastPickedIcon;
        protected IconLayer iconLayer = null;

        public AppFrame() {
            super(true, false, false);

            wwd().addSelectListener(event -> {
                if (isEnabled()) {
                    // Have rollover events highlight the rolled-over object.
                    if (event.getEventAction().equals(SelectEvent.ROLLOVER)) {
                        highlight(event.getTopObject());
                    }
                }
            });

            iconLayer = new IconLayer();
            iconLayer.setViewClippingEnabled(true);
            iconLayer.setPickFrustumClippingEnabled(true);

            for (int x = -180; x < 180; x += 5) {
                for (int y = -90; y < 90; y += 5) {
                    WWIcon icon = new UserFacingIcon("gov/nasa/worldwind/examples/images/georss.png",
                        new Position(new Angle(y), new Angle(x), 0));
                    icon.setSize(new Dimension(32, 32));
                    icon.setHighlightScale(1.3);
                    iconLayer.addIcon(icon);
                }
            }

            WorldWindow.insertAfterPlacenames(this.wwd(), iconLayer);

            frustumLayer.setEnabled(false);
            WorldWindow.insertBeforeCompass(this.wwd(), frustumLayer);

            this.getContentPane().add(this.makeControlPanel(), BorderLayout.WEST);
        }

        protected void highlight(Object o) {
            // Manage highlighting of icons.
            if (this.lastPickedIcon == o)
                return; // same thing selected

            // Turn off highlight if on.
            if (this.lastPickedIcon != null) {
                this.lastPickedIcon.setHighlighted(false);
                this.lastPickedIcon = null;
            }

            // Turn on highlight if object selected.
            if (o instanceof WWIcon) {
                this.lastPickedIcon = (WWIcon) o;
                this.lastPickedIcon.setHighlighted(true);
            }
        }

        protected JPanel makeControlPanel() {
            //Assumes square Frustum
            Dimension dim = wwd().sceneControl().getDrawContext().getPickPointFrustumDimension();
            int frustumWidth = dim.width;

            JPanel p = new JPanel();
            p.setLayout(new GridLayout(0, 1));
            p.setBorder(new CompoundBorder(new TitledBorder("Frustum Culling"), new EmptyBorder(20, 10, 20, 10)));
            JPanel p2 = new JPanel(new BorderLayout(10, 10));
            p2.add(p, BorderLayout.NORTH);

            butToggleViewClipping = new JToggleButton();
            butToggleViewClipping.addActionListener(e -> {
                if (butToggleViewClipping.isSelected()) {
//                        System.out.println("layer.setViewClippingEnabled(true);");
                    iconLayer.setViewClippingEnabled(true);
                    butToggleViewClipping.setText("Disable View Clipping");
                }
                else {
//                        System.out.println("layer.setViewClippingEnabled(false);");
                    iconLayer.setViewClippingEnabled(false);
                    butToggleViewClipping.setText("Enable View Clipping");
                }

                wwd().redraw();
            });
            butToggleViewClipping.setSelected(true);
            butToggleViewClipping.setText("Disable View Clipping");
            p.add(butToggleViewClipping);

            butTogglePickingClipping = new JToggleButton();
            butTogglePickingClipping.addActionListener(e -> {
                if (butTogglePickingClipping.isSelected()) {
                    iconLayer.setPickFrustumClippingEnabled(true);
                    butTogglePickingClipping.setText("Disable Picking Clipping");
                }
                else {
                    iconLayer.setPickFrustumClippingEnabled(false);
                    butTogglePickingClipping.setText("Enable Picking Clipping");
                }

                wwd().redraw();
            });
            butTogglePickingClipping.setSelected(true);
            butTogglePickingClipping.setText("Disable Picking Clipping");
            p.add(butTogglePickingClipping);

            butShowPickingFrustum = new JToggleButton();
            butShowPickingFrustum.addActionListener(e -> {
                if (butShowPickingFrustum.isSelected()) {
                    frustumLayer.setEnabled(true);
                    butShowPickingFrustum.setText("Hide Picking Frustum");
                }
                else {
                    frustumLayer.setEnabled(false);
                    butShowPickingFrustum.setText("Show Picking Frustum");
                }

                wwd().redraw();
            });
            butShowPickingFrustum.setText("Show Picking Frustum");
            p.add(butShowPickingFrustum);

            slider = new JSlider();
            slider.addChangeListener(e -> {
                int val = slider.getValue();

                if (lblDimension != null) {
                    lblDimension.setText("(" + val + "," + val + ")");
                    wwd().sceneControl().getDrawContext().setPickPointFrustumDimension(
                        new Dimension(val, val));
                    wwd().redraw();
                }
            });

            slider.setMaximum(200);
            slider.setMinimum(3);
            slider.setValue(frustumWidth);
            slider.setPaintLabels(true);
            slider.setPaintTicks(true);
            p.add(slider);

            panel = new JPanel();
            panel.setLayout(new GridLayout(1, 0));
            p.add(panel);

            frustumDimensionLabel = new JLabel();
            frustumDimensionLabel.setText("Frustum Dimension:");
            panel.add(frustumDimensionLabel);

            lblDimension = new JLabel();
            lblDimension.setText("(" + frustumWidth + "," + frustumWidth + ")");

            panel.add(lblDimension);
            p2.setBorder(new EmptyBorder(10, 10, 10, 10));
            return p2;
        }
    }
}
