/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.net.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: ScreenCreditController.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class ScreenCreditController implements Renderable, SelectListener, Disposable {
    private final int creditWidth = 32;
    private final int creditHeight = 32;
    private final int leftMargin = 240;
    private final int bottomMargin = 10;
    private final int separation = 10;
    private final double baseOpacity = 0.5;
    private final double highlightOpacity = 1;
    private final WorldWindow wwd;
    private final Collection<String> badURLsReported = new HashSet<>();
    private boolean enabled = true;

    public ScreenCreditController(WorldWindow wwd) {
        if (wwd == null) {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.wwd = wwd;

        if (wwd.sceneControl().getScreenCreditController() != null)
            wwd.sceneControl().getScreenCreditController().dispose();

        wwd.sceneControl().setScreenCreditController(this);
        wwd.addSelectListener(this);
    }

    public void dispose() {
        wwd.removeSelectListener(this);
        if (wwd.sceneControl() == this)
            wwd.sceneControl().setScreenCreditController(null);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void pick(DrawContext dc, Point pickPoint) {
        if (dc == null) {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!this.isEnabled())
            return;

        if (dc.getScreenCredits() == null || dc.getScreenCredits().size() < 1)
            return;

        Set<Map.Entry<ScreenCredit, Long>> credits = dc.getScreenCredits().entrySet();

        int y = dc.view().getViewport().height - (bottomMargin + creditHeight / 2);
        int x = leftMargin + creditWidth / 2;

        for (Map.Entry<ScreenCredit, Long> entry : credits) {
            ScreenCredit credit = entry.getKey();
            Rectangle viewport = new Rectangle(x, y, creditWidth, creditHeight);

            credit.setViewport(viewport);
            credit.pick(dc, pickPoint);

            x += (separation + creditWidth);
        }
    }

    public void render(DrawContext dc) {
        if (dc == null) {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getScreenCredits() == null || dc.getScreenCredits().size() < 1)
            return;

        if (!this.isEnabled())
            return;

        Set<Map.Entry<ScreenCredit, Long>> credits = dc.getScreenCredits().entrySet();

        int y = dc.view().getViewport().height - (bottomMargin + creditHeight / 2);
        int x = leftMargin + creditWidth / 2;

        for (Map.Entry<ScreenCredit, Long> entry : credits) {
            ScreenCredit credit = entry.getKey();
            Rectangle viewport = new Rectangle(x, y, creditWidth, creditHeight);

            credit.setViewport(viewport);
            if (entry.getValue() == dc.getFrameTimeStamp()) {
                Object po = dc.getPickedObjects().getTopObject();
                credit.setOpacity(po instanceof ScreenCredit ? this.highlightOpacity : this.baseOpacity);
                credit.render(dc);
            }

            x += (separation + creditWidth);
        }
    }

    public void accept(SelectEvent event) {
        if (event.mouseEvent != null && event.mouseEvent.isConsumed())
            return;

        Object po = event.getTopObject();

        if (po instanceof ScreenCredit) {
            if (event.getEventAction().equals(SelectEvent.LEFT_DOUBLE_CLICK)) {
                openBrowser((ScreenCredit) po);
            }
        }
    }

    protected void openBrowser(ScreenCredit credit) {
        if (credit.getLink() != null && !credit.getLink().isEmpty()) {
            try {
                BrowserOpener.browse(new URL(credit.getLink()));
            }
            catch (MalformedURLException e) {
                if (!badURLsReported.contains(credit.getLink())) // report it only once
                {
                    String msg = Logging.getMessage("generic.URIInvalid",
                        credit.getLink() != null ? credit.getLink() : "null");
                    Logging.logger().warning(msg);
                    badURLsReported.add(credit.getLink());
                }
            }
            catch (Exception e) {
                String msg = Logging.getMessage("generic.ExceptionAttemptingToInvokeWebBrower for URL",
                    credit.getLink());
                Logging.logger().warning(msg);
            }
        }
    }
}