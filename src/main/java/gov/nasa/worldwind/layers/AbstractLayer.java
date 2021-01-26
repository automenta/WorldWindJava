/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import java.awt.*;
import java.beans.PropertyChangeEvent;

/**
 * @author tag
 * @version $Id: AbstractLayer.java 2254 2014-08-22 17:02:46Z tgaskins $
 */
public abstract class AbstractLayer extends WWObjectImpl implements Layer {
    private boolean enabled = true;
    private boolean pickable = true;
    private double opacity = 1;
    private double minActiveAltitude = Double.NEGATIVE_INFINITY;
    private double maxActiveAltitude = Double.POSITIVE_INFINITY;
    private boolean networkDownloadEnabled = true;
    private long expiryTime;
    private ScreenCredit screenCredit;

    /**
     * Returns true if a specified DOM document is a Layer configuration document, and false otherwise.
     *
     * @param domElement the DOM document in question.
     * @return true if the document is a Layer configuration document; false otherwise.
     * @throws IllegalArgumentException if document is null.
     */
    public static boolean isLayerConfigDocument(Element domElement) {

        XPath xpath = WWXML.makeXPath();
        Element[] elements = WWXML.getElements(domElement, "//Layer", xpath);

        return elements != null && elements.length > 0;
    }

    /**
     * Appends layer configuration parameters as elements to the specified context. This appends elements for the
     * following parameters: <table> <caption style="font-weight: bold;">Append Elements</caption>
     * <tr><th>Parameter</th><th>Element Path</th><th>Type</th></tr> <tr><td>{@link
     * Keys#DISPLAY_NAME}</td><td>DisplayName</td><td>String</td></tr> <tr><td>{@link
     * Keys#OPACITY}</td><td>Opacity</td><td>Double</td></tr> <tr><td>{@link Keys#MAX_ACTIVE_ALTITUDE}</td><td>ActiveAltitudes/@max</td><td>Double</td></tr>
     * <tr><td>{@link Keys#MIN_ACTIVE_ALTITUDE}</td><td>ActiveAltitudes/@min</td><td>Double</td></tr> <tr><td>{@link
     * Keys#NETWORK_RETRIEVAL_ENABLED}</td><td>NetworkRetrievalEnabled</td><td>Boolean</td></tr> <tr><td>{@link
     * Keys#MAP_SCALE}</td><td>MapScale</td><td>Double</td></tr> <tr><td>{@link Keys#SCREEN_CREDIT}</td><td>ScreenCredit</td><td>ScreenCredit</td></tr>
     * </table>
     *
     * @param params  the key-value pairs which define the layer configuration parameters.
     * @param context the XML document root on which to append layer configuration elements.
     * @return a reference to context.
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createLayerConfigElements(KV params, Element context) {

        WWXML.checkAndAppendTextElement(params, Keys.DISPLAY_NAME, context, "DisplayName");
        WWXML.checkAndAppendDoubleElement(params, Keys.OPACITY, context, "Opacity");

        Double maxAlt = KVMap.getDoubleValue(params, Keys.MAX_ACTIVE_ALTITUDE);
        Double minAlt = KVMap.getDoubleValue(params, Keys.MIN_ACTIVE_ALTITUDE);
        if (maxAlt != null || minAlt != null) {
            Element el = WWXML.appendElementPath(context, "ActiveAltitudes");
            if (maxAlt != null)
                WWXML.setDoubleAttribute(el, "max", maxAlt);
            if (minAlt != null)
                WWXML.setDoubleAttribute(el, "min", minAlt);
        }

        WWXML.checkAndAppendBooleanElement(params, Keys.NETWORK_RETRIEVAL_ENABLED, context, "NetworkRetrievalEnabled");
        WWXML.checkAndAppendDoubleElement(params, Keys.MAP_SCALE, context, "MapScale");
        WWXML.checkAndAppendScreenCreditElement(params, Keys.SCREEN_CREDIT, context, "ScreenCredit");
        WWXML.checkAndAppendBooleanElement(params, Keys.PICK_ENABLED, context, "PickEnabled");

        return context;
    }

    /**
     * Parses layer configuration parameters from the specified DOM document. This writes output as key-value pairs to
     * params. If a parameter from the XML document already exists in params, that parameter is ignored. Supported key
     * and parameter names are: <table> <caption style="font-weight: bold;">Supported Names</caption>
     * <tr><th>Parameter</th><th>Element Path</th><th>Type</th></tr> <tr><td>{@link
     * Keys#DISPLAY_NAME}</td><td>DisplayName</td><td>String</td></tr> <tr><td>{@link
     * Keys#OPACITY}</td><td>Opacity</td><td>Double</td></tr> <tr><td>{@link Keys#MAX_ACTIVE_ALTITUDE}</td><td>ActiveAltitudes/@max</td><td>Double</td></tr>
     * <tr><td>{@link Keys#MIN_ACTIVE_ALTITUDE}</td><td>ActiveAltitudes/@min</td><td>Double</td></tr> <tr><td>{@link
     * Keys#NETWORK_RETRIEVAL_ENABLED}</td><td>NetworkRetrievalEnabled</td><td>Boolean</td></tr> <tr><td>{@link
     * Keys#MAP_SCALE}</td><td>MapScale</td><td>Double</td></tr> <tr><td>{@link Keys#SCREEN_CREDIT}</td><td>ScreenCredit</td><td>{@link
     * ScreenCredit}</td></tr> </table>
     *
     * @param domElement the XML document root to parse for layer configuration elements.
     * @param params     the output key-value pairs which recieve the layer configuration parameters. A null reference
     *                   is permitted.
     * @return a reference to params, or a new AVList if params is null.
     * @throws IllegalArgumentException if the document is null.
     */
    public static KV getLayerConfigParams(Element domElement, KV params) {

        if (params == null)
            params = new KVMap();

        XPath xpath = WWXML.makeXPath();

        WWXML.checkAndSetStringParam(domElement, params, Keys.DISPLAY_NAME, "DisplayName", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, Keys.OPACITY, "Opacity", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, Keys.MAX_ACTIVE_ALTITUDE, "ActiveAltitudes/@max", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, Keys.MIN_ACTIVE_ALTITUDE, "ActiveAltitudes/@min", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, Keys.NETWORK_RETRIEVAL_ENABLED, "NetworkRetrievalEnabled",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, Keys.MAP_SCALE, "MapScale", xpath);
        WWXML.checkAndSetScreenCreditParam(domElement, params, Keys.SCREEN_CREDIT, "ScreenCredit", xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, Keys.MAX_ABSENT_TILE_ATTEMPTS, "MaxAbsentTileAttempts",
            xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, Keys.MIN_ABSENT_TILE_CHECK_INTERVAL,
            "MinAbsentTileCheckInterval", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, Keys.PICK_ENABLED, "PickEnabled", xpath);

        return params;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public Layer setEnabled(boolean enabled) {
        boolean oldEnabled = this.enabled;
        this.enabled = enabled;
        if (oldEnabled!=enabled)
            this.propertyChange(new PropertyChangeEvent(this, "Enabled", oldEnabled, this.enabled));
        return this;
    }

    public boolean isPickEnabled() {
        return pickable;
    }

    public void setPickEnabled(boolean pickable) {
        this.pickable = pickable;
    }

    public String name() {
        Object n = this.get(Keys.DISPLAY_NAME);

        return n != null ? n.toString() : this.toString();
    }

    public void setName(String name) {
        this.set(Keys.DISPLAY_NAME, name);
    }

    public String toString() {
        Object n = this.get(Keys.DISPLAY_NAME);

        return n != null ? n.toString() : super.toString();
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    public double getMinActiveAltitude() {
        return minActiveAltitude;
    }

    public void setMinActiveAltitude(double minActiveAltitude) {
        this.minActiveAltitude = minActiveAltitude;
    }

    public double getMaxActiveAltitude() {
        return maxActiveAltitude;
    }

    public void setMaxActiveAltitude(double maxActiveAltitude) {
        this.maxActiveAltitude = maxActiveAltitude;
    }

    public Double getMinEffectiveAltitude(Double radius) {
        return null;
    }

    public Double getMaxEffectiveAltitude(Double radius) {
        return null;
    }

    public double getScale() {
        Object o = this.get(Keys.MAP_SCALE);
        return o instanceof Double ? (Double) o : 1;
    }

    public boolean isNetworkRetrievalEnabled() {
        return networkDownloadEnabled;
    }

    public void setNetworkRetrievalEnabled(boolean networkDownloadEnabled) {
        this.networkDownloadEnabled = networkDownloadEnabled;
    }

    public boolean isLayerInView(DrawContext dc) {

        return true;
    }

    public boolean isLayerActive(DrawContext dc) {
//        if (dc == null) {
//            String message = Logging.getMessage("nullValue.DrawContextIsNull");
//            Logging.logger().severe(message);
//            throw new IllegalStateException(message);
////        }
//
//        if (null == dc.getView()) {
//            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
//            Logging.logger().severe(message);
//            throw new IllegalStateException(message);
//        }

        Position eyePos = dc.view().getEyePosition();
        if (eyePos == null)
            return false;

        double altitude = eyePos.getElevation();
        return altitude >= this.minActiveAltitude && altitude <= this.maxActiveAltitude;
    }

    public final void preRender(DrawContext dc) {
        if (this.enabled && this.isLayerActive(dc) && this.isLayerInView(dc))
            this.doPreRender(dc);
    }

    /**
     * @param dc the current draw context
     * @throws IllegalArgumentException if <code>dc</code> is null, or <code>dc</code>'s <code>Globe</code> or
     *                                  <code>View</code> is null
     */
    public void render(DrawContext dc) {
        if (enabled && this.isLayerActive(dc) && this.isLayerInView(dc))
            this.doRender(dc);
    }

    public void pick(DrawContext dc, Point point) {
        if (enabled && this.isLayerActive(dc) && this.isLayerInView(dc))
            this.doPick(dc, point);
    }

    protected void doPick(DrawContext dc, Point point) {
        // any state that could change the color needs to be disabled, such as GL_TEXTURE, GL_LIGHTING or GL_FOG.
        // re-draw with unique colors
        // store the object info in the selectable objects table
        // read the color under the cursor
        // use the color code as a key to retrieve a selected object from the selectable objects table
        // create an instance of the PickedObject and add to the dc via the dc.addPickedObject() method
    }

    public void dispose() // override if disposal is a supported operation
    {
    }

    protected void doPreRender(DrawContext dc) {
    }

    protected abstract void doRender(DrawContext dc);

    public boolean isAtMaxResolution() {
        return !this.isMultiResolution();
    }

    public boolean isMultiResolution() {
        return false;
    }

    public String getRestorableState() {
        return null;
    }

    public void restoreState(String stateInXml) {
        String message = Logging.getMessage("RestorableSupport.RestoreNotSupported");
        Logging.logger().severe(message);
        throw new UnsupportedOperationException(message);
    }

    public long getExpiryTime() {
        return this.expiryTime;
    }

    //**************************************************************//
    //********************  Configuration  *************************//
    //**************************************************************//

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    protected ScreenCredit getScreenCredit() {
        return screenCredit;
    }

    protected void setScreenCredit(ScreenCredit screenCredit) {
        this.screenCredit = screenCredit;
    }
}