/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util.wizard;

import javax.swing.*;
import java.util.*;

/**
 * @author dcollins
 * @version $Id: WizardModel.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WizardModel extends WizardProperties {
    public static final String CURRENT_PANEL_DESCRIPTOR = "wizard.CurrentPanelDescriptor";
    public static final String BACK_BUTTON_TEXT = "wizard.BackButtonText";
    public static final String NEXT_BUTTON_TEXT = "wizard.NextButtonText";
    public static final String CANCEL_BUTTON_TEXT = "wizard.CancelButtonText";
    public static final String BACK_BUTTON_ENABLED = "wizard.BackButtonEnabled";
    public static final String NEXT_BUTTON_ENABLED = "wizard.NextButtonEnabled";
    public static final String CANCEL_BUTTON_ENABLED = "wizard.CancelButtonEnabled";
    public static final String BACK_BUTTON_ICON = "wizard.BackButtonIcon";
    public static final String NEXT_BUTTON_ICON = "wizard.NextButtonIcon";
    public static final String CANCEL_BUTTON_ICON = "wizard.CancelButtonIcon";
    private final Map<Object, WizardPanelDescriptor> panels;

    public WizardModel() {
        this.panels = new HashMap<>();
    }

    public WizardPanelDescriptor getWizardPanel(Object id) {
        return this.panels.get(id);
    }

    public void registerWizardPanel(Object id, WizardPanelDescriptor panel) {
        if (id != null && panel != null) {
            this.panels.put(id, panel);
        }
    }

    public WizardPanelDescriptor getCurrentPanel() {
        Object value = getProperty(WizardModel.CURRENT_PANEL_DESCRIPTOR);
        return (value instanceof WizardPanelDescriptor) ? (WizardPanelDescriptor) value : null;
    }

    public boolean setCurrentPanel(Object id) {
        boolean success = false;
        WizardPanelDescriptor newPanel = this.panels.get(id);
        if (newPanel != null) {
            WizardPanelDescriptor oldPanel = getCurrentPanel();
            if (oldPanel != newPanel) {
                setProperty(WizardModel.CURRENT_PANEL_DESCRIPTOR, newPanel);
                firePropertyChange(WizardModel.CURRENT_PANEL_DESCRIPTOR, oldPanel, newPanel);
            }
            success = true;
        }
        return success;
    }

    public String getBackButtonText() {
        return getStringProperty(WizardModel.BACK_BUTTON_TEXT);
    }

    public void setBackButtonText(String newText) {
        setProperty(WizardModel.BACK_BUTTON_TEXT, newText);
    }

    public String getNextButtonText() {
        return getStringProperty(WizardModel.NEXT_BUTTON_TEXT);
    }

    public void setNextButtonText(String newText) {
        setProperty(WizardModel.NEXT_BUTTON_TEXT, newText);
    }

    public String getCancelButtonText() {
        return getStringProperty(WizardModel.CANCEL_BUTTON_TEXT);
    }

    public void setCancelButtonText(String newText) {
        setProperty(WizardModel.CANCEL_BUTTON_TEXT, newText);
    }

    public Boolean isBackButtonEnabled() {
        return getBooleanProperty(WizardModel.BACK_BUTTON_ENABLED);
    }

    public void setBackButtonEnabled(Boolean newValue) {
        setProperty(WizardModel.BACK_BUTTON_ENABLED, newValue);
    }

    public Boolean isNextButtonEnabled() {
        return getBooleanProperty(WizardModel.NEXT_BUTTON_ENABLED);
    }

    public void setNextButtonEnabled(Boolean newValue) {
        setProperty(WizardModel.NEXT_BUTTON_ENABLED, newValue);
    }

    public Boolean isCancelButtonEnabled() {
        return getBooleanProperty(WizardModel.CANCEL_BUTTON_ENABLED);
    }

    public void setCancelButtonEnabled(Boolean newValue) {
        setProperty(WizardModel.CANCEL_BUTTON_ENABLED, newValue);
    }

    public Icon getBackButtonIcon() {
        return getIconProperty(WizardModel.BACK_BUTTON_ICON);
    }

    public void setBackButtonIcon(Icon newIcon) {
        setProperty(WizardModel.BACK_BUTTON_ICON, newIcon);
    }

    public Icon getNextButtonIcon() {
        return getIconProperty(WizardModel.NEXT_BUTTON_ICON);
    }

    public void setNextButtonIcon(Icon newIcon) {
        setProperty(WizardModel.NEXT_BUTTON_ICON, newIcon);
    }

    public Icon getCancelButtonIcon() {
        return getIconProperty(WizardModel.CANCEL_BUTTON_ICON);
    }

    public void setCancelButtonIcon(Icon newIcon) {
        setProperty(WizardModel.CANCEL_BUTTON_ICON, newIcon);
    }

    public Icon getIconProperty(String propertyName) {
        Object value = getProperty(propertyName);
        return (value instanceof Icon) ? (Icon) value : null;
    }
}
