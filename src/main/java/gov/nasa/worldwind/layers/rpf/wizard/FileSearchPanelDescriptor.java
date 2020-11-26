/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers.rpf.wizard;

import gov.nasa.worldwind.formats.rpf.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.util.wizard.*;

import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: FileSearchPanelDescriptor.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class FileSearchPanelDescriptor extends DefaultPanelDescriptor {
    public static final String IDENTIFIER = "gov.nasa.worldwind.rpf.wizard.FileSearchPanel";
    private final ProgressPanel panelComponent;
    private final PropertyEvents propertyEvents;
    private Thread workerThread;

    public FileSearchPanelDescriptor() {
        this.panelComponent = new ProgressPanel();
        this.propertyEvents = new PropertyEvents();
        this.panelComponent.addPropertyChangeListener(this.propertyEvents);
        setPanelIdentifier(IDENTIFIER);
        setPanelComponent(this.panelComponent);
    }

    public Object getBackPanelDescriptor() {
        return FileChooserPanelDescriptor.IDENTIFIER;
    }

    public Object getNextPanelDescriptor() {
        return DataChooserPanelDescriptor.IDENTIFIER;
    }

    public void registerPanel(Wizard wizard) {
        WizardModel oldWizardModel = getWizardModel();
        if (oldWizardModel != null)
            oldWizardModel.removePropertyChangeListener(this.propertyEvents);

        super.registerPanel(wizard);

        WizardModel newWizardModel = getWizardModel();
        if (newWizardModel != null)
            newWizardModel.addPropertyChangeListener(this.propertyEvents);
    }

    public void aboutToDisplayPanel() {
        this.panelComponent.setTitle(RPFWizardUtil.makeLarger("Searching for Imagery"));
        this.panelComponent.setProgressDescription1(" ");
        this.panelComponent.setProgressDescription2(" ");
        this.panelComponent.getProgressBar().setIndeterminate(false);
        WizardModel model = getWizardModel();
        if (model != null) {
            File selectedFile = RPFWizardUtil.getSelectedFile(model);
            if (selectedFile != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("<br>");
                sb.append("Searching ");
                sb.append("'").append(selectedFile.getAbsolutePath()).append(File.separator).append("...'");
                sb.append("<br>");
                this.panelComponent.setDescription(RPFWizardUtil.makeBold(sb.toString()));
            }
        }
    }

    public void displayingPanel() {
        WizardModel model = getWizardModel();
        if (model != null && !RPFWizardUtil.isFileListCurrent(model)) {
            this.panelComponent.getProgressBar().setIndeterminate(true);
            startWorkerThread(() -> {
                refreshFileList();

                WizardModel model1 = getWizardModel();
                if (model1 != null)
                    RPFWizardUtil.setFileListCurrent(model1, true);

                moveToNextPanel();
            });
        }
    }

    public void aboutToHidePanel() {
        killWorkerThread();
    }

    private void moveToNextPanel() {
        Wizard wizard = getWizard();
        Object nextPanel = getNextPanelDescriptor();
        if (wizard != null && nextPanel != null) {
            wizard.setCurrentPanelDescriptor(nextPanel);
        }
    }

    private void selectedFileChanged() {
        WizardModel model = getWizardModel();
        if (model != null) {
            RPFWizardUtil.setFileListCurrent(model, false);
        }
    }

    private void refreshFileList() {
        WizardModel model = getWizardModel();
        if (model != null) {
            // Disable the "<Back" and "Next>" buttons.
            boolean backEnabled = model.isBackButtonEnabled();
            boolean nextEnabled = model.isNextButtonEnabled();
            model.setBackButtonEnabled(false);
            model.setNextButtonEnabled(false);

            // Search for any RPF files under the selected file.
            File selectedFile = RPFWizardUtil.getSelectedFile(model);
            FileFilter rpfFilter = new AcceptRPFFilter();
            FileFilter updateUIFilter = new UpdateDescriptionFilter(rpfFilter, this.panelComponent);
            List<File> fileList = searchSelectedFile(selectedFile, updateUIFilter);
            RPFWizardUtil.setFileList(model, fileList);

            // Create FileSets from the search results (if any).
            List<FileSet> fileSetList = makeFileSetList(fileList);
            if (fileSetList != null) {
                makeDefaultSelections(fileSetList);
                makeTitles(fileSetList);
                sortFileSetList(fileSetList);
            }
            RPFWizardUtil.setFileSetList(model, fileSetList);
            // Forces notification that the FileSetList has changed.
            model.firePropertyChange(RPFWizardUtil.FILE_SET_LIST, null, fileSetList);

            // Restore the previous state of the "<Back" buttons.
            model.setBackButtonEnabled(backEnabled);
            model.setNextButtonEnabled(nextEnabled);
        }
    }

    private List<File> searchSelectedFile(File fileToSearch, FileFilter fileFilter) {
        if (Thread.interrupted())
            return null;

        List<File> fileList;
        try {
            FileTree fileTree = new FileTree(fileToSearch);
            fileTree.setMode(FileTree.FILES_ONLY);
            fileList = fileTree.asList(fileFilter);
        }
        catch (Throwable t) {
            String message = String.format("Exception while searching file: %s", fileToSearch);
            Logging.logger().log(Level.SEVERE, message, t);
            fileList = null;
        }
        return fileList;
    }

    private List<FileSet> makeFileSetList(Iterable<File> fileList) {
        List<FileSet> result = null;
        if (fileList != null) {
            Map<String, FileSet> map = new HashMap<>();
            for (File file : fileList) {
                try {
                    String filename = file.getName().toUpperCase();
                    RPFFrameFilename rpfFilename = RPFFrameFilename.parseFilename(filename);
                    String id = rpfFilename.getDataSeriesCode();
                    FileSet set = map.get(id);
                    if (set == null) {
                        set = new FileSet();
                        set.setIdentifier(id);
                        set.setFiles(new LinkedList<>());
                        map.put(id, set);
                    }
                    set.getFiles().add(file);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            result = new ArrayList<>(map.values());
        }
        return result;
    }

    private void makeDefaultSelections(List<FileSet> fileSetList) {
        // If only one FileSet is available, select it.
        if (fileSetList != null
            && fileSetList.size() == 1
            && fileSetList.get(0) != null) {
            fileSetList.get(0).setSelected(true);
        }
    }

    private void makeTitles(Iterable<FileSet> fileSetList) {
        if (fileSetList != null) {
            for (FileSet set : fileSetList) {
                makeTitle(set);
            }
        }
    }

    private void makeTitle(FileSet set) {
        if (set != null && set.getIdentifier() != null) {
            String id = set.getIdentifier();
            RPFDataSeries ds;
            try {
                ds = RPFDataSeries.dataSeriesFor(id);
            }
            catch (Exception e) {
                e.printStackTrace();
                ds = null;
            }

            if (ds != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(ds.dataSeries);
                sb.append(" (");
                sb.append(ds.seriesCode);
                sb.append(")");
                set.setTitle(sb.toString());
            }
        }
    }

    private void sortFileSetList(List<FileSet> fileSetList) {
        Comparator<FileSet> comparator = (o1, o2) -> {
            // Don't care about ordering in this case.
            if (o1 == null || o2 == null)
                return 0;
            String id1 = o1.getIdentifier();
            String id2 = o2.getIdentifier();
            // Don't care about ordering in this case.
            if (id1 == null || id2 == null)
                return 0;

            RPFDataSeries ds1, ds2;
            try {
                ds1 = RPFDataSeries.dataSeriesFor(id1);
                ds2 = RPFDataSeries.dataSeriesFor(id2);
            }
            catch (Exception e) {
                e.printStackTrace();
                ds1 = ds2 = null;
            }

            // Sort on the order the data series appears in the NITFS specification table,
            // or if the data series cannot be identified, sort alphabetically.
            return (ds1 != null && ds2 != null) ? ds1.compareTo(ds2) : id1.compareTo(id2);
        };
        fileSetList.sort(comparator);
    }

    private void startWorkerThread(Runnable runnable) {
        killWorkerThread();
        this.workerThread = new Thread(runnable);
        this.workerThread.start();
    }

    private void killWorkerThread() {
        if (this.workerThread != null && this.workerThread.isAlive())
            this.workerThread.interrupt();
        this.workerThread = null;
    }

    private static class UpdateDescriptionFilter implements FileFilter {
        private final FileFilter delegate;
        private final ProgressPanel panel;

        private UpdateDescriptionFilter(FileFilter delegate, ProgressPanel panel) {
            this.delegate = delegate;
            this.panel = panel;
        }

        public boolean accept(File pathname) {
            if (!Thread.interrupted()) {
                if (this.panel != null && pathname != null) {
                    String oldDescription = this.panel.getProgressDescription2();
                    String newDescription = pathname.getParent();
                    if (!Objects.equals(newDescription, oldDescription)) {
                        this.panel.setProgressDescription1(newDescription);
                    }
                }
                return this.delegate != null && this.delegate.accept(pathname);
            }
            return false;
        }
    }

    private static class AcceptRPFFilter implements FileFilter {
        public boolean accept(File pathname) {
            if (pathname != null && pathname.getName() != null) {
                String filename = pathname.getName().toUpperCase();
                return RPFFrameFilename.isFilename(filename);
            }
            return false;
        }
    }

    private class PropertyEvents implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt != null && evt.getPropertyName() != null) {
                String propertyName = evt.getPropertyName();
                if (propertyName.equals(RPFWizardUtil.SELECTED_FILE)) {
                    selectedFileChanged();
                }
            }
        }
    }
}
