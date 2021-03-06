/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.examples.sar;

import gov.nasa.worldwind.util.Logging;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: AboutDialog.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class AboutDialog {
    private Object content;
    private String contentType;
    private Dimension preferredSize;

    public AboutDialog() {
    }

    private static void showContentDialog(Component parentComponent, Component component) {
        try {
            final JDialog dialog;
            if (parentComponent instanceof Dialog)
                dialog = new JDialog((Dialog) parentComponent);
            else if (parentComponent instanceof Frame)
                dialog = new JDialog((Frame) parentComponent);
            else
                dialog = new JDialog();

            component.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });

            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.getContentPane().add(component, BorderLayout.CENTER);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setModal(true);
            dialog.setResizable(false);
            dialog.pack();
            SAR2.centerWindowInDesktop(dialog);
            dialog.setVisible(true);
        }
        catch (Exception e) {
            String message = "Exception while displaying content dialog";
            Logging.logger().log(Level.SEVERE, message, e);
        }
    }

    public Object getContent() {
        return this.content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Dimension getPreferredSize() {
        return this.preferredSize;
    }

    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
    }

    public void showDialog(Component parentComponent) {
        Component component = makeContentComponent();
        showContentDialog(parentComponent, component);
    }

    private Component makeContentComponent() {
        JEditorPane editor = null;
        try {
            if (this.content != null) {
                URL url = getClass().getResource(this.content.toString());
                editor = new JEditorPane();
                if (this.contentType != null)
                    editor.setContentType(this.contentType);
                editor.setPage(url);
            }

            if (editor != null) {
                editor.setEditable(false);
                if (this.preferredSize != null)
                    editor.setPreferredSize(this.preferredSize);
            }
        }
        catch (Exception e) {
            String message = "Exception while fetching content";
            Logging.logger().log(Level.SEVERE, message, e);
            editor = null;
        }
        return editor;
    }
}
