/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwindx.examples;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import gov.nasa.worldwind.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * @author tag
 * @version $Id: WorldWindDiagnostics.java 2319 2014-09-17 19:22:58Z dcollins $
 */
public class WorldWindDiagnostics {

    static {
        if (Configuration.isMacOS()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "WorldWind Diagnostic Program");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
        }
    }

    public static void main(String[] arg) {
        final MainFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static class GLFrame extends JFrame implements GLEventListener {

        private static final Attr[] attrs = new Attr[] {
            new Attr(GL.GL_STENCIL_BITS, "stencil bits"),
            new Attr(GL.GL_DEPTH_BITS, "depth bits"),
            new Attr(GL2.GL_MAX_TEXTURE_UNITS, "max texture units"),
            new Attr(GL2.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, "max texture image units"),
            new Attr(GL2.GL_MAX_TEXTURE_COORDS_ARB, "max texture coords"),
            new Attr(GL.GL_MAX_TEXTURE_SIZE, "max texture size"),
            new Attr(GL2.GL_MAX_ELEMENTS_INDICES, "max elements indices"),
            new Attr(GL2.GL_MAX_ELEMENTS_VERTICES, "max elements vertices"),
            new Attr(GL2.GL_MAX_LIGHTS, "max lights"),
            new Attr(GL2.GL_LINE_WIDTH_RANGE, "line width range")
        };
        private final JTextArea outputArea;

        GLFrame(JTextArea outputArea) {
            this.outputArea = outputArea;
            GLCapabilities caps = new GLCapabilities(Configuration.getMaxCompatibleGLProfile());
            caps.setAlphaBits(8);
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            GLCanvas glCanvas = new GLCanvas(caps);
            glCanvas.addGLEventListener(this);
            this.add(glCanvas);
            this.setSize(200, 200);
        }

        @Override
        public void init(GLAutoDrawable glAutoDrawable) {
            StringBuilder sb = new StringBuilder();

            sb.append(Version.getVersion()).append("\n");

            sb.append("\nSystem Properties\n");
            sb.append("Processors: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
            sb.append("Free memory: ").append(Runtime.getRuntime().freeMemory()).append(" bytes\n");
            sb.append("Max memory: ").append(Runtime.getRuntime().maxMemory()).append(" bytes\n");
            sb.append("Total memory: ").append(Runtime.getRuntime().totalMemory()).append(" bytes\n");

            for (Map.Entry prop : System.getProperties().entrySet()) {
                sb.append(prop.getKey()).append(" = ").append(prop.getValue()).append("\n");
            }

            GL gl = glAutoDrawable.getGL();

            sb.append("\nOpenGL Values\n");

            String oglVersion = gl.glGetString(GL.GL_VERSION);
            sb.append("OpenGL version: ").append(oglVersion).append("\n");

            String oglVendor = gl.glGetString(GL.GL_VENDOR);
            sb.append("OpenGL vendor: ").append(oglVendor).append("\n");

            String oglRenderer = gl.glGetString(GL.GL_RENDERER);
            sb.append("OpenGL renderer: ").append(oglRenderer).append("\n");

            int[] intVals = new int[2];
            for (Attr attr : attrs) {
                sb.append(attr.name).append(": ");

                if (attr.attr instanceof Integer) {
                    gl.glGetIntegerv((Integer) attr.attr, intVals, 0);
                    sb.append(intVals[0]).append(intVals[1] > 0 ? ", " + intVals[1] : "");
                }

                sb.append("\n");
            }

            String extensionString = gl.glGetString(GL.GL_EXTENSIONS);
            String[] extensions = extensionString.split(" ");
            sb.append("Extensions\n");
            for (String ext : extensions) {
                sb.append("    ").append(ext).append("\n");
            }

            sb.append("\nJOGL Values\n");
            String pkgName = "com.jogamp.opengl";
            try {
                getClass().getClassLoader().loadClass(pkgName + ".GL");

                Package p = getClass().getClassLoader().getDefinedPackage(pkgName);
                if (p == null) {
                    sb.append("WARNING: Package.getPackage(").append(pkgName).append(") is null\n");
                }
                else {
                    sb.append(p).append("\n");
                    sb.append("Specification Title = ").append(p.getSpecificationTitle()).append("\n");
                    sb.append("Specification Vendor = ").append(p.getSpecificationVendor()).append("\n");
                    sb.append("Specification Version = ").append(p.getSpecificationVersion()).append("\n");
                    sb.append("Implementation Vendor = ").append(p.getImplementationVendor()).append("\n");
                    sb.append("Implementation Version = ").append(p.getImplementationVersion()).append("\n");
                }
            }
            catch (ClassNotFoundException e) {
                sb.append("Unable to load ").append(pkgName).append("\n");
            }

            this.outputArea.setText(sb.toString());
        }

        @Override
        public void dispose(GLAutoDrawable glAutoDrawable) {
        }

        public void display(GLAutoDrawable glAutoDrawable) {
            glAutoDrawable.getGL().glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }

        @Override
        public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        }

        private static class Attr {

            private final Object attr;
            private final String name;

            private Attr(Object attr, String name) {
                this.attr = attr;
                this.name = name;
            }
        }
    }

    private static class MainFrame extends JFrame {

        private static final JTextArea outputArea = new JTextArea(20, 80);
        private static final Action[] operations = new Action[] {
            new RunAction()
        };
        private final JPanel mainPanel = new JPanel();
        private final JPanel controlContainer = new JPanel(new BorderLayout());
        private final JPanel outputContainer = new JPanel();

        public MainFrame() {
            try {
                mainPanel.setLayout(new BorderLayout());
                mainPanel.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
                mainPanel.add(outputContainer, BorderLayout.CENTER);
                outputArea.setLineWrap(true);
                outputContainer.add(new JScrollPane(outputArea), BorderLayout.CENTER);
                outputContainer.setBorder(new TitledBorder("Results"));

                this.getContentPane().add(mainPanel, BorderLayout.CENTER);

                List<JButton> btns = new ArrayList<>();
                {
                    JPanel westPanel = new JPanel(new GridLayout(0, 1, 0, 10));
                    westPanel.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
                    JPanel opsPanel = new JPanel(new GridLayout(6, 1));
                    opsPanel.setBorder(new TitledBorder("Operations"));

                    for (Action action : operations) {
                        JPanel p = new JPanel(new BorderLayout());
                        JButton jb = new JButton(action);
                        btns.add(jb);
                        p.add(jb, BorderLayout.NORTH);
                        opsPanel.add(p);
                    }

                    westPanel.add(opsPanel);
                    controlContainer.add(westPanel, BorderLayout.CENTER);
                }

                this.getContentPane().add(controlContainer, BorderLayout.WEST);
                this.pack();

                Dimension dim = btns.get(0).getSize();
                for (JButton btn : btns) {
                    btn.setPreferredSize(dim);
                }

                Dimension prefSize = this.getPreferredSize();
                prefSize.setSize(prefSize.getWidth(), 1.1 * prefSize.getHeight());
                this.setSize(prefSize);

                Dimension parentSize;
                Point parentLocation = new Point(0, 0);
                parentSize = Toolkit.getDefaultToolkit().getScreenSize();
                int x = parentLocation.x + (parentSize.width - prefSize.width) / 2;
                int y = parentLocation.y + (parentSize.height - prefSize.height) / 2;
                this.setLocation(x, y);
                this.setResizable(true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void processWindowEvent(WindowEvent e) {
            super.processWindowEvent(e);

            if (e.getID() != WindowEvent.WINDOW_OPENED) //noinspection UnnecessaryReturnStatement
            {
                return;
            }
            GLFrame glFrame = new GLFrame(outputArea);
            glFrame.setVisible(true);
        }

        private static class RunAction extends AbstractAction {

            public RunAction() {
                super("Re-run Test");
            }

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                outputArea.setText("");
                GLFrame glFrame = new GLFrame(outputArea);
                glFrame.setVisible(true);
            }
        }
    }
}
