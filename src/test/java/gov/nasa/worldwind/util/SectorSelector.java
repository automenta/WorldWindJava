/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.video.LayerList;

import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;

/**
 * Provides an interactive region selector. To use, construct and call enable/disable. Register a property listener to
 * receive changes to the sector as they occur, or just wait until the user is done and then query the result via {@link
 * #getSector()}.
 *
 * @author tag
 * @version $Id: SectorSelector.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class SectorSelector extends WWObjectImpl
    implements SelectListener, MouseListener, MouseMotionListener, RenderingListener {
    public final static String SECTOR_PROPERTY = "gov.nasa.worldwind.SectorSelector";

    protected static final int NONE = 0;

    protected static final int MOVING = 1;
    protected static final int SIZING = 2;

    protected static final int NORTH = 1;
    protected static final int SOUTH = 2;
    protected static final int EAST = 4;
    protected static final int WEST = 8;
    protected static final int NORTHWEST = NORTH + WEST;
    protected static final int NORTHEAST = NORTH + EAST;
    protected static final int SOUTHWEST = SOUTH + WEST;
    protected static final int SOUTHEAST = SOUTH + EAST;

    private final WorldWindow wwd;
    private final Layer layer;
    private final RegionShape shape;

    private double edgeFactor = 0.10;

    // state tracking fields
    private boolean armed = false;
    private int operation = NONE;
    private int side = NONE;
    private Position previousPosition = null;
    private Sector previousSector = null;

    public SectorSelector(WorldWindow worldWindow) {
        if (worldWindow == null) {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().log(Level.SEVERE, msg);
            throw new IllegalArgumentException(msg);
        }

        this.wwd = worldWindow;

        this.layer = new RenderableLayer();
        this.shape = new RegionShape(Sector.EMPTY_SECTOR);
        ((RenderableLayer) this.layer).add(this.shape);
    }

    protected SectorSelector(WorldWindow worldWindow, RegionShape shape, RenderableLayer rLayer) {
        if (worldWindow == null) {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().log(Level.SEVERE, msg);
            throw new IllegalArgumentException(msg);
        }

        if (shape == null) {
            String msg = Logging.getMessage("nullValue.Shape");
            Logging.logger().log(Level.SEVERE, msg);
            throw new IllegalArgumentException(msg);
        }

        if (rLayer == null) {
            String msg = Logging.getMessage("nullValue.Layer");
            Logging.logger().log(Level.SEVERE, msg);
            throw new IllegalArgumentException(msg);
        }

        this.wwd = worldWindow;
        this.shape = shape;
        this.layer = rLayer;
        rLayer.add(this.shape);
    }

    private static double abs(double a) {
        return a >= 0 ? a : -a;
    }

    public WorldWindow getWwd() {
        return wwd;
    }

    public Layer getLayer() {
        return layer;
    }

    public void enable() {
        this.getShape().setStartPosition(null);

        LayerList layers = this.getWwd().model().layers();

        if (!layers.contains(this.getLayer()))
            layers.add(this.getLayer());

        if (!this.getLayer().isEnabled())
            this.getLayer().setEnabled(true);

        this.setArmed(true);

        this.getWwd().addRenderingListener(this);
        this.getWwd().addSelectListener(this);
        this.getWwd().input().addMouseListener(this);
        this.getWwd().input().addMouseMotionListener(this);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    public void disable() {
        this.getWwd().removeRenderingListener(this);
        this.getWwd().removeSelectListener(this);
        this.getWwd().input().removeMouseListener(this);
        this.getWwd().input().removeMouseMotionListener(this);

        this.getWwd().model().layers().remove(this.getLayer());

        this.getShape().clear();
    }

    public Sector getSector() {
        return this.getShape().hasSelection() ? this.getShape().getSector() : null;
        // TODO: Determine how to handle date-line spanning sectors.
    }

    public Color getInteriorColor() {
        return this.getShape().getInteriorColor();
    }

    public void setInteriorColor(Color color) {
        this.getShape().setInteriorColor(color);
    }

    public Color getBorderColor() {
        return this.getShape().getBorderColor();
    }

    public void setBorderColor(Color color) {
        this.getShape().setBorderColor(color);
    }

    public double getInteriorOpacity() {
        return this.getShape().getInteriorOpacity();
    }

    public void setInteriorOpacity(double opacity) {
        this.getShape().setInteriorOpacity(opacity);
    }

    public double getBorderOpacity() {
        return this.getShape().getBorderOpacity();
    }

    public void setBorderOpacity(double opacity) {
        this.getShape().setBorderOpacity(opacity);
    }

    public double getBorderWidth() {
        return this.getShape().getBorderWidth();
    }

    public void setBorderWidth(double width) {
        this.getShape().setBorderWidth(width);
    }

    protected RegionShape getShape() {
        return shape;
    }

    protected boolean isArmed() {
        return armed;
    }

    protected void setArmed(boolean armed) {
        this.armed = armed;
    }

    protected int getOperation() {
        return operation;
    }

    protected void setOperation(int operation) {
        this.operation = operation;
    }

    protected int getSide() {
        return side;
    }

    protected void setSide(int side) {
        this.side = side;
    }

    protected Position getPreviousPosition() {
        return previousPosition;
    }

    protected void setPreviousPosition(Position previousPosition) {
        this.previousPosition = previousPosition;
    }

    protected double getEdgeFactor() {
        return edgeFactor;
    }

    protected void setEdgeFactor(double edgeFactor) {
        this.edgeFactor = edgeFactor;
    }

    public void stageChanged(RenderingEvent event) {
        if (!event.getStage().equals(RenderingEvent.AFTER_BUFFER_SWAP))
            return;

        // We notify of changes during this rendering stage because the sector is updated within the region shape's
        // render method.

        this.notifySectorChanged();
    }

//
    // Mouse events are used to initiate and track initial drawing of the region. When the selector is enabled it is
    // "armed", meaning that the next mouse press on the globe will initiate the region selection and display. The
    // selector is then disarmed so that subsequent mouse presses either size or move the region when they occur on
    // the region, or move the globe if they occur outside the region.
    //

    protected void notifySectorChanged() {
        if (this.getShape().hasSelection() && this.getSector() != null && !this.getSector().equals(
            this.previousSector)) {
            this.emit(SECTOR_PROPERTY, this.previousSector, this.getShape().getSector());
            this.previousSector = this.getSector();
        }
    }

    public void mousePressed(MouseEvent mouseEvent) {
        if (MouseEvent.BUTTON1_DOWN_MASK != mouseEvent.getModifiersEx())
            return;

        if (!this.isArmed())
            return;

        this.getShape().setResizeable(true);
        this.getShape().setStartPosition(null);
        this.setArmed(false);

        mouseEvent.consume();
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        if (MouseEvent.BUTTON1 != mouseEvent.getButton())
            return;

        if (this.getShape().isResizeable())
            this.setCursor(null);

        this.getShape().setResizeable(false);

        mouseEvent.consume(); // prevent view operations

        this.emit(SECTOR_PROPERTY, this.previousSector, null);
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        if (MouseEvent.BUTTON1_DOWN_MASK != mouseEvent.getModifiersEx())
            return;

        if (this.getShape().isResizeable())
            mouseEvent.consume(); // prevent view operations
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    //
    // Selection events are used to resize and move the region
    //

    public void mouseMoved(MouseEvent e) {
    }

    public void accept(SelectEvent event) {
        if (event == null) {
            String msg = Logging.getMessage("nullValue.EventIsNull");
            Logging.logger().log(Level.FINE, msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.getOperation() == NONE
            && event.getTopObject() != null && !(event.getTopPickedObject().getParentLayer() == this.layer)) {
            this.setCursor(null);
            return;
        }

        if (event.getEventAction().equals(SelectEvent.LEFT_PRESS)) {
            this.setPreviousPosition(this.getWwd().position());
        }
        else if (event.getEventAction().equals(SelectEvent.DRAG)) {
            DragSelectEvent dragEvent = (DragSelectEvent) event;
            Object topObject = dragEvent.getTopObject();
            if (topObject == null)
                return;

            RegionShape dragObject = this.getShape();

            if (this.getOperation() == SIZING) {
                Sector newSector = this.resizeShape(dragObject, this.getSide());
                if (newSector != null)
                    dragObject.setSector(newSector);
            }
            else {
                this.setSide(this.determineAdjustmentSide(dragObject, this.getEdgeFactor()));

                if (this.getSide() == NONE || this.getOperation() == MOVING) {
                    this.setOperation(MOVING);
                    this.dragWholeShape(dragEvent, dragObject);
                }
                else {
                    Sector newSector = this.resizeShape(dragObject, this.getSide());
                    if (newSector != null)
                        dragObject.setSector(newSector);
                    this.setOperation(SIZING);
                }
            }
            event.consume();

            this.setPreviousPosition(this.getWwd().position());
            this.notifySectorChanged();
        }
        else if (event.getEventAction().equals(SelectEvent.DRAG_END)) {
            this.setOperation(NONE);
            this.setPreviousPosition(null);
        }
        else if (event.getEventAction().equals(SelectEvent.ROLLOVER) && this.getOperation() == NONE) {
            if (!(this.getWwd() instanceof Component))
                return;

            if (event.getTopObject() == null || event.getTopPickedObject().isTerrain()) {
                this.setCursor(null);
                return;
            }

            if (!(event.getTopObject() instanceof Movable))
                return;

            this.setCursor(this.determineAdjustmentSide((Movable) event.getTopObject(), this.getEdgeFactor()));
        }
    }

    protected int determineAdjustmentSide(Movable dragObject, double factor) {
        if (dragObject instanceof SurfaceSector) {
            SurfaceSector quad = (SurfaceSector) dragObject;
            Sector s = quad.getSector(); // TODO: go over all sectors
            Position p = this.getWwd().position();

            if (p == null) {
                return NONE;
            }

            double dN = abs(s.latMax().sub(p.getLat()).degrees);
            double dS = abs(s.latMin().sub(p.getLat()).degrees);
            double dW = abs(s.lonMin().sub(p.getLon()).degrees);
            double dE = abs(s.lonMax().sub(p.getLon()).degrees);

            double sLat = factor * s.latDelta;
            double sLon = factor * s.lonDelta;

            if (dN < sLat && dW < sLon)
                return NORTHWEST;
            if (dN < sLat && dE < sLon)
                return NORTHEAST;
            if (dS < sLat && dW < sLon)
                return SOUTHWEST;
            if (dS < sLat && dE < sLon)
                return SOUTHEAST;
            if (dN < sLat)
                return NORTH;
            if (dS < sLat)
                return SOUTH;
            if (dW < sLon)
                return WEST;
            if (dE < sLon)
                return EAST;
        }

        return NONE;
    }

    protected Sector resizeShape(Movable dragObject, int side) {
        if (dragObject instanceof SurfaceSector) {
            SurfaceSector quad = (SurfaceSector) dragObject;
            Sector s = quad.getSector(); // TODO: go over all sectors
            Position p = this.getWwd().position();

            if (p == null || this.getPreviousPosition() == null) {
                return null;
            }

            Angle dLat = p.getLat().sub(this.getPreviousPosition().getLat());
            Angle dLon = p.getLon().sub(this.getPreviousPosition().getLon());

            Angle newMinLat = s.latMin();
            Angle newMinLon = s.lonMin();
            Angle newMaxLat = s.latMax();
            Angle newMaxLon = s.lonMax();

            if (side == NORTH) {
                newMaxLat = s.latMax().add(dLat);
            }
            else if (side == SOUTH) {
                newMinLat = s.latMin().add(dLat);
            }
            else if (side == EAST) {
                newMaxLon = s.lonMax().add(dLon);
            }
            else if (side == WEST) {
                newMinLon = s.lonMin().add(dLon);
            }
            else if (side == NORTHWEST) {
                newMaxLat = s.latMax().add(dLat);
                newMinLon = s.lonMin().add(dLon);
            }
            else if (side == NORTHEAST) {
                newMaxLat = s.latMax().add(dLat);
                newMaxLon = s.lonMax().add(dLon);
            }
            else if (side == SOUTHWEST) {
                newMinLat = s.latMin().add(dLat);
                newMinLon = s.lonMin().add(dLon);
            }
            else if (side == SOUTHEAST) {
                newMinLat = s.latMin().add(dLat);
                newMaxLon = s.lonMax().add(dLon);
            }

            return new Sector(newMinLat, newMaxLat, newMinLon, newMaxLon);
        }

        return null;
    }

    protected void dragWholeShape(DragSelectEvent dragEvent, Movable dragObject) {
        View view = getWwd().view();
        Globe globe = getWwd().model().globe();

        // Compute ref-point position in screen coordinates.
        Position refPos = dragObject.getReferencePosition();
        if (refPos == null)
            return;

        Vec4 refPoint = globe.computePointFromPosition(refPos);
        Vec4 screenRefPoint = view.project(refPoint);

        // Compute screen-coord delta since last event.
        int dx = dragEvent.pickPoint.x - dragEvent.getPreviousPickPoint().x;
        int dy = dragEvent.pickPoint.y - dragEvent.getPreviousPickPoint().y;

        // Find intersection of screen coord ref-point with globe.
        double x = screenRefPoint.x + dx;
        double y = dragEvent.mouseEvent.getComponent().getSize().height - screenRefPoint.y + dy - 1;
        Line ray = view.computeRayFromScreenPoint(x, y);
        Intersection[] inters = globe.intersect(ray, refPos.getElevation());

        if (inters != null) {
            // Intersection with globe. Move reference point to the intersection point.
            Position p = globe.computePositionFromPoint(inters[0].getIntersectionPoint());
            dragObject.moveTo(p);
        }
    }

    protected void setCursor(int sideName) {
        Cursor cursor = switch (sideName) {
            case NONE -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            case NORTH -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case SOUTH -> Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
            case EAST -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case WEST -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            case NORTHWEST -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case NORTHEAST -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case SOUTHWEST -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            case SOUTHEAST -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            default -> null;
        };

        this.setCursor(cursor);
    }

    protected void setCursor(Cursor cursor) {
        ((Component) this.getWwd()).setCursor(cursor != null ? cursor : Cursor.getDefaultCursor());
    }

    protected static class RegionShape extends SurfaceSector {
        private boolean resizeable = false;
        private Position startPosition;
        private Position endPosition;
        private SurfaceSector borderShape;

        protected RegionShape(Sector sector) {
            super(sector);

            // Create the default border shape.
            this.setBorder(new SurfaceSector(sector));

            // The edges of the region shape should be constant lines of latitude and longitude.
            this.setPathType(Keys.LINEAR);
            this.getBorder().setPathType(Keys.LINEAR);

            // Setup default interior rendering attributes. Note that the interior rendering attributes are
            // configured so only the SurfaceSector's interior is rendered.
            ShapeAttributes interiorAttrs = new BasicShapeAttributes();
            interiorAttrs.setDrawOutline(false);
            interiorAttrs.setInteriorMaterial(new Material(Color.WHITE));
            interiorAttrs.setInteriorOpacity(0.1);
            this.setAttributes(interiorAttrs);
            this.setHighlightAttributes(interiorAttrs);

            // Setup default border rendering attributes. Note that the border rendering attributes are configured
            // so that only the SurfaceSector's outline is rendered.
            ShapeAttributes borderAttrs = new BasicShapeAttributes();
            borderAttrs.setDrawInterior(false);
            borderAttrs.setOutlineMaterial(new Material(Color.RED));
            borderAttrs.setOutlineOpacity(0.7);
            borderAttrs.setOutlineWidth(3);
            this.getBorder().setAttributes(borderAttrs);
            this.getBorder().setHighlightAttributes(borderAttrs);
        }

        public Color getInteriorColor() {
            return this.getAttributes().getInteriorMaterial().getDiffuse();
        }

        public void setInteriorColor(Color color) {
            ShapeAttributes attr = this.getAttributes();
            attr.setInteriorMaterial(new Material(color));
            this.setAttributes(attr);
        }

        public Color getBorderColor() {
            return this.getBorder().getAttributes().getOutlineMaterial().getDiffuse();
        }

        public void setBorderColor(Color color) {
            ShapeAttributes attr = this.getBorder().getAttributes();
            attr.setOutlineMaterial(new Material(color));
            this.getBorder().setAttributes(attr);
        }

        public double getInteriorOpacity() {
            return this.getAttributes().getInteriorOpacity();
        }

        public void setInteriorOpacity(double opacity) {
            ShapeAttributes attr = this.getAttributes();
            attr.setInteriorOpacity(opacity);
            this.setAttributes(attr);
        }

        public double getBorderOpacity() {
            return this.getBorder().getAttributes().getOutlineOpacity();
        }

        public void setBorderOpacity(double opacity) {
            ShapeAttributes attr = this.getBorder().getAttributes();
            attr.setOutlineOpacity(opacity);
            this.getBorder().setAttributes(attr);
        }

        public double getBorderWidth() {
            return this.getBorder().getAttributes().getOutlineWidth();
        }

        public void setBorderWidth(double width) {
            ShapeAttributes attr = this.getBorder().getAttributes();
            attr.setOutlineWidth(width);
            this.getBorder().setAttributes(attr);
        }

        public void setSector(Sector sector) {
            super.setSector(sector);
            this.getBorder().setSector(sector);
        }

        protected boolean isResizeable() {
            return resizeable;
        }

        protected void setResizeable(boolean resizeable) {
            this.resizeable = resizeable;
        }

        protected Position getStartPosition() {
            return startPosition;
        }

        protected void setStartPosition(Position startPosition) {
            this.startPosition = startPosition;
        }

        protected Position getEndPosition() {
            return endPosition;
        }

        protected void setEndPosition(Position endPosition) {
            this.endPosition = endPosition;
        }

        protected SurfaceSector getBorder() {
            return borderShape;
        }

        protected void setBorder(SurfaceSector shape) {
            if (shape == null) {
                String message = Logging.getMessage("nullValue.Shape");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.borderShape = shape;
        }

        protected boolean hasSelection() {
            return getStartPosition() != null && getEndPosition() != null;
        }

        protected void clear() {
            this.setStartPosition(null);
            this.setEndPosition(null);
            this.setSector(Sector.EMPTY_SECTOR);
        }

        public void preRender(DrawContext dc) {
            // This is called twice: once during normal rendering, then again during ordered surface rendering. During
            // normal renering we pre-render both the interior and border shapes. During ordered surface rendering, both
            // shapes are already added to the DrawContext and both will be individually processed. Therefore we just 
            // call our superclass behavior
            if (dc.isOrderedRenderingMode()) {
                super.preRender(dc);
                return;
            }

            this.doPreRender(dc);
        }

        @Override
        public void render(DrawContext dc) {
            if (dc.isPickingMode() && this.isResizeable())
                return;

            // This is called twice: once during normal rendering, then again during ordered surface rendering. During
            // normal renering we render both the interior and border shapes. During ordered surface rendering, both
            // shapes are already added to the DrawContext and both will be individually processed. Therefore we just
            // call our superclass behavior
            if (dc.isOrderedRenderingMode()) {
                super.render(dc);
                return;
            }

            if (!this.isResizeable()) {
                if (this.hasSelection()) {
                    this.doRender(dc);
                }
                return;
            }

            PickedObjectList pos = dc.getPickedObjects();
            PickedObject terrainObject = pos != null ? pos.getTerrainObject() : null;

            if (terrainObject == null)
                return;

            if (this.getStartPosition() != null) {
                Position end = terrainObject.position();
                if (!this.getStartPosition().equals(end)) {
                    this.setEndPosition(end);
                    this.setSector(Sector.boundingSector(this.getStartPosition(), this.getEndPosition()));
                    this.doRender(dc);
                }
            }
            else {
                this.setStartPosition(pos.getTerrainObject().position());
            }
        }

        protected void doPreRender(DrawContext dc) {
            this.doPreRenderInterior(dc);
            this.doPreRenderBorder(dc);
        }

        protected void doPreRenderInterior(DrawContext dc) {
            super.preRender(dc);
        }

        protected void doPreRenderBorder(DrawContext dc) {
            this.getBorder().preRender(dc);
        }

        protected void doRender(DrawContext dc) {
            this.doRenderInterior(dc);
            this.doRenderBorder(dc);
        }

        protected void doRenderInterior(DrawContext dc) {
            super.render(dc);
        }

        protected void doRenderBorder(DrawContext dc) {
            this.getBorder().render(dc);
        }
    }
}