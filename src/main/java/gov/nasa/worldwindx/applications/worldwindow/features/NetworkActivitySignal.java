/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwindx.applications.worldwindow.features;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwindx.applications.worldwindow.core.*;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author tag
 * @version $Id: NetworkActivitySignal.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class NetworkActivitySignal extends AbstractFeature {
    private final List<NetworkUser> networkUsers = new ArrayList<>();
    private final AtomicBoolean isNetworkAvailable = new AtomicBoolean(true);
    private JLabel networkLabel = new JLabel();
    private ImageIcon busySignal;

    public NetworkActivitySignal(Registry registry) {
        super("Network Activity Signal", Constants.NETWORK_STATUS_SIGNAL, registry);
    }

    public void initialize(Controller controller) {
        super.initialize(controller);

        // Must construct the busy signal from a URL rather than getResource used in the image library.
        URL iconURL = ImageLibrary.getImageURL("images/indicator-16.gif");
        this.busySignal = iconURL != null ? new ImageIcon(iconURL) : null;

        this.networkLabel = new JLabel();
        this.networkLabel.setOpaque(false);

        NetworkUser downloadUser = () -> WorldWind.getRetrievalService().hasActiveTasks();
        this.networkUsers.add(downloadUser);

        Timer activityTimer = new Timer(500, actionEvent -> {
            if (!isNetworkAvailable.get()) {
                if (networkLabel.getText() == null) {
                    networkLabel.setIcon(null);
                    networkLabel.setText("No network");
                    networkLabel.setForeground(Color.RED);
                    networkLabel.setVisible(true);
                }
            }
            else {
                for (NetworkUser user : networkUsers) {
                    if (user.hasNetworkActivity()) {
                        runBusySignal(true);
                        return;
                    }
                }
                runBusySignal(false);
            }
        });
        activityTimer.start();

        Timer netCheckTimer = new Timer(1000, actionEvent -> {
            Thread t = new Thread(() -> isNetworkAvailable.set(!WorldWind.getNetworkStatus().isNetworkUnavailable()));
            t.start();
        });
        netCheckTimer.start();
    }

    private void runBusySignal(boolean tf) {
        if (tf) {
            if (this.networkLabel.getIcon() == null) {
                this.networkLabel.setIcon(this.busySignal);
                this.networkLabel.setText(null);
                this.networkLabel.setVisible(true);
            }
        }
        else {
            if (this.networkLabel.isVisible()) {
                this.networkLabel.setText(null);
                this.networkLabel.setIcon(null);
                this.networkLabel.setVisible(false);
            }
        }
    }

    public JLabel getLabel() {
        return this.networkLabel;
    }

    public void addNetworkUser(NetworkUser user) {
        this.networkUsers.add(user);
    }

    public void removeNetworkUser(NetworkUser user) {
        this.networkUsers.remove(user);
    }

    public interface NetworkUser {
        boolean hasNetworkActivity();
    }
}
