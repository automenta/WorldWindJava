/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe2D;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.terrain.SectorGeometryList;

/**
 * @author Tom Gaskins
 * @version $Id: BasicSceneController.java 2249 2014-08-21 20:13:30Z dcollins $
 */
public class BasicSceneController extends AbstractSceneController {
    SectorGeometryList sglC, sglL, sglR;
    Sector visibleSectorC, visibleSectorL, visibleSectorR;

    public void doRepaint(DrawContext dc) {
        AbstractSceneController.initializeFrame(dc);
        try {
            if (dc.getGlobe() instanceof Globe2D && ((Globe2D) dc.getGlobe()).isContinuous())
                this.do2DContiguousRepaint(dc);
            else
                this.doNormalRepaint(dc);
        }
        finally {
            AbstractSceneController.finalizeFrame(dc);
        }
    }

    protected void doNormalRepaint(DrawContext dc) {
        AbstractSceneController.applyView(dc);
        AbstractSceneController.createPickFrustum(dc);
        AbstractSceneController.createTerrain(dc);
        this.preRender(dc);
        AbstractSceneController.clearFrame(dc);
        this.pick(dc);
        AbstractSceneController.clearFrame(dc);
        this.draw(dc);
    }

    protected void do2DContiguousRepaint(DrawContext dc) {
        ((Globe2D) dc.getGlobe()).setOffset(0);

        AbstractSceneController.applyView(dc);
        AbstractSceneController.createPickFrustum(dc);
        this.createTerrain2DContinuous(dc);
        this.preRender2DContiguous(dc);
        AbstractSceneController.clearFrame(dc);
        this.pick2DContiguous(dc);
        AbstractSceneController.clearFrame(dc);
        this.draw2DContiguous(dc);
    }

    protected void makeCurrent(DrawContext dc, int offset) {
        ((Globe2D) dc.getGlobe()).setOffset(offset);

        switch (offset) {
            case -1 -> {
                dc.setSurfaceGeometry(this.sglL);
                dc.setVisibleSector(this.visibleSectorL);
            }
            case 0 -> {
                dc.setSurfaceGeometry(this.sglC);
                dc.setVisibleSector(this.visibleSectorC);
            }
            case 1 -> {
                dc.setSurfaceGeometry(this.sglR);
                dc.setVisibleSector(this.visibleSectorR);
            }
        }
    }

    protected void createTerrain2DContinuous(DrawContext dc) {
        this.sglC = null;
        this.visibleSectorC = null;
        ((Globe2D) dc.getGlobe()).setOffset(0);
        if (dc.getGlobe().intersects(dc.view().getFrustumInModelCoordinates())) {
            this.sglC = dc.getModel().globe().tessellate(dc);
            this.visibleSectorC = this.sglC.getSector();
        }

        this.sglR = null;
        this.visibleSectorR = null;
        ((Globe2D) dc.getGlobe()).setOffset(1);
        if (dc.getGlobe().intersects(dc.view().getFrustumInModelCoordinates())) {
            this.sglR = dc.getModel().globe().tessellate(dc);
            this.visibleSectorR = this.sglR.getSector();
        }

        this.sglL = null;
        this.visibleSectorL = null;
        ((Globe2D) dc.getGlobe()).setOffset(-1);
        if (dc.getGlobe().intersects(dc.view().getFrustumInModelCoordinates())) {
            this.sglL = dc.getModel().globe().tessellate(dc);
            this.visibleSectorL = this.sglL.getSector();
        }
    }

    protected void draw2DContiguous(DrawContext dc) {
        String drawing = "";
        if (this.sglC != null) {
            drawing += " 0 ";
            this.makeCurrent(dc, 0);
            this.setDeferOrderedRendering(this.sglL != null || this.sglR != null);
            this.draw(dc);
        }

        if (this.sglR != null) {
            drawing += " 1 ";
            this.makeCurrent(dc, 1);
            this.setDeferOrderedRendering(this.sglL != null);
            this.draw(dc);
        }

        this.setDeferOrderedRendering(false);

        if (this.sglL != null) {
            drawing += " -1 ";
            this.makeCurrent(dc, -1);
            this.draw(dc);
        }
    }

    protected void preRender2DContiguous(DrawContext dc) {
        if (this.sglC != null) {
            this.makeCurrent(dc, 0);
            this.preRender(dc);
        }

        if (this.sglR != null) {
            this.makeCurrent(dc, 1);
            this.preRender(dc);
        }

        if (this.sglL != null) {
            this.makeCurrent(dc, -1);
            this.preRender(dc);
        }
    }

    protected void pick2DContiguous(DrawContext dc) {
        if (this.sglC != null) {
            this.makeCurrent(dc, 0);
            this.setDeferOrderedRendering(this.sglL != null || this.sglR != null);
            this.pick(dc);
        }

        if (this.sglR != null) {
            this.makeCurrent(dc, 1);
            this.setDeferOrderedRendering(this.sglL != null);
            this.pick(dc);
        }

        this.setDeferOrderedRendering(false);

        if (this.sglL != null) {
            this.makeCurrent(dc, -1);
            this.pick(dc);
        }
    }
}