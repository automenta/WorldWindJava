/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.dataimporter;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.util.Logging;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.logging.Level;

/**
 * Displays data sets available to install.
 *
 * @author tag
 * @version $Id: FileSetPanel.java 1180 2013-02-15 18:40:47Z tgaskins $
 */
public class FileSetPanel extends JPanel {
    protected final WorldWindow wwd;
    protected final FileSetHighlighter sectorHighlighter;
    protected final JFileChooser fileChooser;
    protected FileSetFinder fileSetFinder;
    protected FileSetTable fileSetTable;
    protected Thread scanningThread;

    /**
     * Constructs a new panel.
     *
     * @param wwd the WorldWindow associated with the panel.
     */
    public FileSetPanel(WorldWindow wwd) {
        super(new BorderLayout(30, 30));

        this.wwd = wwd;

        this.setPreferredSize(new Dimension(1200, 400));

        this.fileChooser = new JFileChooser();
        this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.fileChooser.setMultiSelectionEnabled(true);
        this.fileChooser.addActionListener(event -> {
            if (event.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
                // Cancel the scanning action by interrupting the thread. Interrupts are checked by the
                // FileSetFinder.
                if (scanningThread != null && scanningThread.isAlive())
                    scanningThread.interrupt();

                return;
            }

            // Re-populate the file set table if the file-chooser selection changed.
            File[] roots = fileChooser.getSelectedFiles();
            if (roots != null && roots.length > 0)
                resetTable(roots);
        });

        // Disable the cancel button until a directory scan begins.
        this.enableCancelAction(false);

        this.add(this.fileChooser, BorderLayout.WEST);

        JPanel tablePanel = createTablePanel();
        this.add(tablePanel, BorderLayout.CENTER);

        // Link a sector highlighter that shows the data set coverage areas.
        this.sectorHighlighter = new FileSetHighlighter(this.wwd, this);
    }

    protected JButton findCancelButton(Container container) {
        // Searches the file chooser to find the Cancel button.

        int length = container.getComponentCount();

        for (int i = 0; i < length; i++) {
            Component c = container.getComponent(i);
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                if (UIManager.getString("FileChooser.cancelButtonText").equals(b.getText()))
                    return b;
            }
            else if (c instanceof Container) {
                JButton b = this.findCancelButton((Container) c);
                if (b != null)
                    return b;
            }
        }

        return null;
    }

    protected void enableCancelAction(boolean tf) {
        JButton button = this.findCancelButton(this.fileChooser);
        if (button != null)
            button.setEnabled(tf);
    }

    protected void resetTable(final File[] roots) {
        // Causes the file set table to be cleared and repopulated for the specified root directories.

        fileSetTable.setFileSetMap(null);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        this.enableCancelAction(true);

        this.scanningThread = new Thread(() -> {
            try {
                fileSetFinder.findFileSets(roots);
            }
            catch (Exception e) {
                Logging.logger().log(Level.SEVERE, "Exception while finding available data", e);
            }
            finally {
                SwingUtilities.invokeLater(() -> {
                    fileSetTable.setFileSetMap(fileSetFinder.getFileSetMap());
                    setCursor(Cursor.getDefaultCursor());
                    enableCancelAction(false);
                    scanningThread = null;
                });
            }
        });
        this.scanningThread.start();
    }

    protected JPanel createTablePanel() {
        this.fileSetFinder = new FileSetFinder();
        this.fileSetTable = new FileSetTable(null);
        JScrollPane scrollPane = new JScrollPane(this.fileSetTable);

        final JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(20, 5, 5, 20));
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton importButton = new JButton(new AbstractAction("Install Selected Data") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final List<FileSet> fileSetList = new ArrayList<>();

                int[] rows = fileSetTable.getSelectedRows();
                for (int row : rows) {
                    int modelRow = fileSetTable.convertRowIndexToModel(row);
                    FileSet fileSet = ((FileSetTableModel) fileSetTable.getModel()).getRow(modelRow);

                    if (fileSet != null)
                        fileSetList.add(fileSet);
                }

                final List<FileSet> consolidatedFileSetList = FileSetFinder.consolidateFileSets(fileSetList);

                Thread t = new Thread(() -> performInstallation(consolidatedFileSetList));
                t.start();
            }
        });
        panel.add(importButton, BorderLayout.SOUTH);

        return panel;
    }

    public void addSelectionListener(ListSelectionListener listener) {
        this.fileSetTable.getSelectionModel().addListSelectionListener(listener);
    }

    public List<FileSet> getSelectedFileSets() {
        return this.fileSetTable.getSelectedFileSets();
    }

    public void scrollToFileSet(FileSet fileSet) {
        // This method makes the selected file set visible in the table.
        this.fileSetTable.scrollToFileSet(fileSet);
    }

    protected void performInstallation(Iterable<FileSet> fileSets) {
        // Install the selected data sets.

        final DataInstaller dataImporter = new DataInstaller();
        for (final FileSet fileSet : fileSets) {
            try {
                final Document
                    dataConfig = dataImporter.installDataFromFiles(FileSetPanel.this, fileSet);

                if (dataConfig != null && this.wwd != null) {
                    SwingUtilities.invokeLater(() -> {
                        DataInstaller.addToWorldWindow(wwd, dataConfig.getDocumentElement(), fileSet, true);
                        FileSetPanel.this.firePropertyChange(DataInstaller.INSTALL_COMPLETE, dataConfig, null);
                    });
                }
            }
            catch (Exception e) {
                Logging.logger().log(Level.SEVERE, "Exception performing installation", e);
            }
        }
    }
}
//
//    protected void disableCancelButton(Container container)
//    {
//        int length = container.getComponentCount();
//
//        for (int i = 0; i < length; i++)
//        {
//            Component c = container.getComponent(i);
//            if (c instanceof JButton)
//            {
//                JButton b = (JButton) c;
//                if (UIManager.getString("FileChooser.cancelButtonText").equals(b.getText()))
//                {
//                    b.setEnabled(false);
//                    c.getParent().remove(b);
//                    return;
//                }
//            }
//            else if (c instanceof Container)
//            {
//                this.disableCancelButton((Container) c);
//            }
//        }
//    }
