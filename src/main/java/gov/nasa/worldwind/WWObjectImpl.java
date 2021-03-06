/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.KVMap;

import java.beans.PropertyChangeEvent;

/**
 * Implements <code>WWObject</code> functionality. Meant to be either subclassed or aggregated by classes implementing
 * <code>WWObject</code>.
 *
 * @author Tom Gaskins
 * @version $Id: WWObjectImpl.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class WWObjectImpl extends KVMap implements WWObject {
    /**
     * Constructs a new <code>WWObjectImpl</code>.
     */
    public WWObjectImpl() {
    }

    public WWObjectImpl(Object source) {
        super(source);
    }

    /**
     * The property change listener for <em>this</em> instance. Receives property change notifications that this
     * instance has registered with other property change notifiers.
     *
     * @param propertyChangeEvent the event
     * @throws IllegalArgumentException if <code>propertyChangeEvent</code> is null
     */
    @Override public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
//        if (propertyChangeEvent == null) {
//            String msg = Logging.getMessage("nullValue.PropertyChangeEventIsNull");
//            Logging.logger().severe(msg);
//            throw new IllegalArgumentException(msg);
//        }

        // Notify all *my* listeners of the change that I caught
        super.emit(propertyChangeEvent);
    }


}