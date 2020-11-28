/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * Example of how to animate the view from one position to another.
 * <p>
 * Use the buttons on the left side of the frame to animate the view: <ul> <li> Zero: Move the view to look at 0 degrees
 * latitude, 0 degrees longitude.</li> <li> Heading: Animate the view from 0 degrees heading to 90 degrees
 * heading.</li>
 * <li> Follow: Animate the view to along a path of geographic positions.</li> <li> Forward: Animate the view to look
 * at the next position in a list.</li> <li> Backward: Animate the view to look at the previous position in a
 * list.</li>
 * </ul>
 *
 * @author tag
 * @version $Id: ViewIteration.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ViewIteration extends ApplicationTemplate {
    public static void main(String[] args) {
        try {
            AppFrame frame = new AppFrame();
            frame.setTitle("WorldWind View Paths");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class AppFrame extends JFrame {
        static final ArrayList<Position> path;

        static {
            path = new ArrayList<>();
            path.add(Position.fromDegrees(0, 0, 1.0e5));
            path.add(Position.fromDegrees(0, 10, 1.0e5));
            path.add(Position.fromDegrees(0, 20, 1.0e5));
            path.add(Position.fromDegrees(0, 30, 1.0e5));
            path.add(Position.fromDegrees(0, 40, 1.0e5));
            path.add(Position.fromDegrees(0, 50, 1.0e5));
            path.add(Position.fromDegrees(0, 60, 1.0e5));
            path.add(Position.fromDegrees(0, 70, 1.0e5));
        }

        protected final PathAction[] pathActions =
            new PathAction[] {
                new GoToLatLonFromCurrent("Zero", LatLon.ZERO),
                new FollowPath("Follow"),
                new Heading("Heading"),
                new Forward("Forward"),
                new Backwards("Backwards"),
            };
        protected final Dimension canvasSize = new Dimension(800, 600);
        protected int pathPosition = 0;
        protected ApplicationTemplate.AppPanel wwjPanel;

        public AppFrame() {
            // Create the WorldWindow.
            this.wwjPanel = new ApplicationTemplate.AppPanel(this.canvasSize, true);
            this.wwjPanel.setPreferredSize(canvasSize);

            JPanel controlPanel = makeControlPanel();
            controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
            this.getContentPane().add(controlPanel, BorderLayout.WEST);
            this.pack();

            // Center the application on the screen.
            Dimension prefSize = this.getPreferredSize();
            Dimension parentSize;
            Point parentLocation = new Point(0, 0);
            parentSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = parentLocation.x + (parentSize.width - prefSize.width) / 2;
            int y = parentLocation.y + (parentSize.height - prefSize.height) / 2;
            this.setLocation(x, y);
            this.setResizable(true);
        }

        protected JPanel makeControlPanel() {
            JPanel innerPanel = new JPanel(new GridLayout(8, 1));
            innerPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Go To"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

            for (PathAction pa : pathActions) {
                JButton btn = new JButton(pa);
                innerPanel.add(btn);
            }

            JPanel cp = new JPanel(new BorderLayout());
            cp.add(innerPanel, BorderLayout.CENTER);

            return cp;
        }

        protected Angle computeHeading(Position pa, Position pb) {
            return LatLon.greatCircleAzimuth(pa, pb);
        }

        //
        // Specific Actions
        //

        protected abstract static class PathAction extends AbstractAction {
            PathAction(String name) {
                super(name);
            }
        }

        protected class Forward extends PathAction {
            public Forward(String name) {
                super(name);
            }

            public void actionPerformed(ActionEvent actionEvent) {
                if (pathPosition < path.size() - 1) {
                    BasicOrbitView view = (BasicOrbitView) wwjPanel.wwd().view();
                    view.setHeading(Angle.fromDegrees(90));
                    view.addEyePositionAnimator(4000, view.getEyePosition(), path.get(++pathPosition));
                }
            }
        }

        protected class Backwards extends PathAction {
            public Backwards(String name) {
                super(name);
            }

            public void actionPerformed(ActionEvent actionEvent) {
                if (pathPosition > 0) {
                    BasicOrbitView view = (BasicOrbitView) wwjPanel.wwd().view();

                    view.addEyePositionAnimator(4000, view.getEyePosition(), path.get(--pathPosition));
                }
            }
        }

        protected class Heading extends PathAction {
            public Heading(String name) {
                super(name);
            }

            public void actionPerformed(ActionEvent actionEvent) {
                Angle heading;
                if (pathPosition == 0)
                    heading = computeHeading(path.get(0), path.get(1));
                else
                    heading = computeHeading(path.get(pathPosition - 1), path.get(pathPosition));

                BasicOrbitView view = (BasicOrbitView) wwjPanel.wwd().view();
                view.addHeadingAnimator(view.getHeading(), heading);
            }
        }

        protected class GoToLatLonFromCurrent extends PathAction {
            protected final LatLon latlon;

            GoToLatLonFromCurrent(String name, LatLon latlon) {
                super(name);
                this.latlon = latlon;
            }

            public void actionPerformed(ActionEvent actionEvent) {
                BasicOrbitView view = (BasicOrbitView) wwjPanel.wwd().view();
                view.addEyePositionAnimator(
                    4000, view.getEyePosition(), new Position(this.latlon, view.getEyePosition().getElevation()));
            }
        }

        protected class FollowPath extends PathAction {
            final List<Position> path = new ArrayList<>();

            FollowPath(String name) {
                super(name);
                path.add(Position.fromDegrees(0, 0, 1.0e5));
                path.add(Position.fromDegrees(1, 3, 1.0e5));
                path.add(Position.fromDegrees(2, 4, 1.0e5));
                path.add(Position.fromDegrees(3, 5, 1.0e5));
            }

            public void actionPerformed(ActionEvent actionEvent) {
                for (Position p : path) {
                    final BasicOrbitView view = (BasicOrbitView) wwjPanel.wwd().view();
                    view.addEyePositionAnimator(4000,
                        view.getEyePosition(), new Position(p, view.getEyePosition().getElevation()));
                }
            }
        }
    }
}
