/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.video;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.Logging;

import java.beans.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Tom Gaskins
 * @version $Id: LayerList.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class LayerList extends CopyOnWriteArrayList<Layer> implements WWObject {
    private final WWObjectImpl wwo = new WWObjectImpl(this);

    public LayerList() {
    }

    public LayerList(Layer[] layers) {
        if (layers == null) {
            String message = Logging.getMessage("nullValue.LayersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.addAll(Arrays.asList(layers));
    }

    public LayerList(Collection<Layer> layerList) {
        super(layerList);
    }

    public static List<Layer> getListDifference(Collection<Layer> oldList, Iterable<Layer> newList) {
        List<Layer> deltaList = new ArrayList<>();

        for (Layer layer : newList) {
            if (!oldList.contains(layer))
                deltaList.add(layer);
        }

        return deltaList;
    }

    /**
     * Aggregate the contents of a group of layer lists into a single one. All layers are placed in the first designated
     * list and removed from the subsequent lists.
     *
     * @param lists an array containing the lists to aggregate. All members of the second and subsequent lists in the
     *              array are added to the first list in the array.
     * @return the aggregated list.
     * @throws IllegalArgumentException if the layer-lists array is null or empty.
     */
    public static LayerList collapseLists(LayerList[] lists) {
        if (lists == null || lists.length == 0) {
            String message = Logging.getMessage("nullValue.LayersListArrayIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LayerList list = lists[0];

        for (int i = 1; i < lists.length; i++) {
            LayerList ll = lists[i];
            list.addAll(ll);

            for (Layer layer : ll) {
                ll.remove(layer);
            }
        }

        return list;
    }

    public static List<Layer> getLayersAdded(Collection<Layer> oldList, Iterable<Layer> newList) {
        return getListDifference(oldList, newList);
    }

    public static List<Layer> getLayersRemoved(Iterable<Layer> oldList, Collection<Layer> newList) {
        return getListDifference(newList, oldList);
    }

    public String getDisplayName() {
        return this.getStringValue(AVKey.DISPLAY_NAME);
    }

    public void setDisplayName(String displayName) {
        this.set(AVKey.DISPLAY_NAME, displayName);
    }

    protected static LayerList makeShallowCopy(LayerList sourceList) {
        return new LayerList(sourceList);
    }

    public boolean add(Layer layer) {
        if (layer == null) {
            String message = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LayerList copy = makeShallowCopy(this);
        super.add(layer);
        layer.addPropertyChangeListener(this);
        this.firePropertyChange(AVKey.LAYERS, copy, this);

        return true;
    }

    public void add(int index, Layer layer) {
        if (layer == null) {
            String message = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LayerList copy = makeShallowCopy(this);
        super.add(index, layer);
        layer.addPropertyChangeListener(this);
        this.firePropertyChange(AVKey.LAYERS, copy, this);
    }

    public void remove(Layer layer) {
        if (layer == null) {
            String msg = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!this.contains(layer))
            return;

        LayerList copy = makeShallowCopy(this);
        layer.removePropertyChangeListener(this);
        super.remove(layer);
        this.firePropertyChange(AVKey.LAYERS, copy, this);
    }

    public Layer remove(int index) {
        Layer layer = get(index);
        if (layer == null)
            return null;

        LayerList copy = makeShallowCopy(this);
        layer.removePropertyChangeListener(this);
        super.remove(index);
        this.firePropertyChange(AVKey.LAYERS, copy, this);

        return layer;
    }

    public boolean moveLower(Layer targetLayer) {
        int index = this.indexOf(targetLayer);
        if (index <= 0)
            return false;

        this.remove(index);
        this.add(index - 1, targetLayer);

        return true;
    }

    public boolean moveHigher(Layer targetLayer) {
        int index = this.indexOf(targetLayer);
        if (index < 0)
            return false;

        this.remove(index);
        this.add(index + 1, targetLayer);

        return true;
    }

    public Layer set(int index, Layer layer) {
        if (layer == null) {
            String message = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Layer oldLayer = this.get(index);
        if (oldLayer != null)
            oldLayer.removePropertyChangeListener(this);

        LayerList copy = makeShallowCopy(this);
        super.set(index, layer);
        layer.addPropertyChangeListener(this);
        this.firePropertyChange(AVKey.LAYERS, copy, this);

        return oldLayer;
    }

    public boolean remove(Object o) {
        for (Layer layer : this) {
            if (layer.equals(o))
                layer.removePropertyChangeListener(this);
        }

        LayerList copy = makeShallowCopy(this);
        boolean removed = super.remove(o);
        if (removed)
            this.firePropertyChange(AVKey.LAYERS, copy, this);

        return removed;
    }

    public boolean addIfAbsent(Layer layer) {
        for (Layer l : this) {
            if (l.equals(layer))
                return false;
        }

        layer.addPropertyChangeListener(this);

        LayerList copy = makeShallowCopy(this);
        boolean added = super.addIfAbsent(layer);
        if (added)
            this.firePropertyChange(AVKey.LAYERS, copy, this);

        return added;
    }

    public boolean removeAll(Collection<?> objects) {
        for (Layer layer : this) {
            layer.removePropertyChangeListener(this);
        }

        LayerList copy = makeShallowCopy(this);
        boolean removed = super.removeAll(objects);
        if (removed)
            this.firePropertyChange(AVKey.LAYERS, copy, this);

        for (Layer layer : this) {
            layer.addPropertyChangeListener(this);
        }

        return removed;
    }

    public boolean removeAll() {
        for (Layer layer : this) {
            layer.removePropertyChangeListener(this);
        }

        LayerList copy = makeShallowCopy(this);
        boolean removed = super.retainAll(new ArrayList<Layer>()); // retain no layers
        if (removed)
            this.firePropertyChange(AVKey.LAYERS, copy, this);

        return removed;
    }

    public int addAllAbsent(Collection<? extends Layer> layers) {
        for (Layer layer : layers) {
            if (!this.contains(layer))
                layer.addPropertyChangeListener(this);
        }

        LayerList copy = makeShallowCopy(this);
        int numAdded = super.addAllAbsent(layers);
        if (numAdded > 0)
            this.firePropertyChange(AVKey.LAYERS, copy, this);

        return numAdded;
    }

    public boolean addAll(Collection<? extends Layer> layers) {
        for (Layer layer : layers) {
            layer.addPropertyChangeListener(this);
        }

        LayerList copy = makeShallowCopy(this);
        boolean added = super.addAll(layers);
        if (added)
            this.firePropertyChange(AVKey.LAYERS, copy, this);

        return added;
    }

    public boolean addAll(int i, Collection<? extends Layer> layers) {
        for (Layer layer : layers) {
            layer.addPropertyChangeListener(this);
        }

        LayerList copy = makeShallowCopy(this);
        boolean added = super.addAll(i, layers);
        if (added)
            this.firePropertyChange(AVKey.LAYERS, copy, this);

        return added;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean retainAll(Collection<?> objects) {
        for (Layer layer : this) {
            if (!objects.contains(layer))
                layer.removePropertyChangeListener(this);
        }

        LayerList copy = makeShallowCopy(this);
        boolean added = super.retainAll(objects);
        if (added)
            this.firePropertyChange(AVKey.LAYERS, copy, this);

        return added;
    }

    public void replaceAll(Iterable<? extends Layer> layers) {
        Collection<Layer> toDelete = new ArrayList<>();
        Collection<Layer> toKeep = new ArrayList<>();

        for (Layer layer : layers) {
            if (!this.contains(layer))
                toDelete.add(layer);
            else
                toKeep.add(layer);
        }

        for (Layer layer : toDelete) {
            this.remove(layer);
        }

        super.clear();

        for (Layer layer : layers) {
            if (!toKeep.contains(layer))
                layer.addPropertyChangeListener(this);

            super.add(layer);
        }
    }

    public Layer getLayerByName(String name) {
        if (name == null) {
            String message = Logging.getMessage("nullValue.NameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (Layer l : this) {
            if (l.getName().equals(name))
                return l;
        }

        return null;
    }

    public List<Layer> getLayersByClass(Class classToFind) {
        if (classToFind == null) {
            String message = Logging.getMessage("nullValue.ClassIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        List<Layer> layers = new ArrayList<>();

        for (Layer l : this) {
            if (l.getClass().equals(classToFind))
                layers.add(l);
        }

        return layers;
    }

    public Object get(String key) {
        return wwo.get(key);
    }

    public Iterable<Object> getValues() {
        return wwo.getValues();
    }

    public Set<Map.Entry<String, Object>> getEntries() {
        return wwo.getEntries();
    }

    public String getStringValue(String key) {
        return wwo.getStringValue(key);
    }

    public Object set(String key, Object value) {
        return wwo.set(key, value);
    }

    public AVList setValues(AVList avList) {
        return wwo.setValues(avList);
    }

    public boolean hasKey(String key) {
        return wwo.hasKey(key);
    }

    public Object removeKey(String key) {
        return wwo.removeKey(key);
    }

    public AVList copy() {
        return wwo.copy();
    }

    public AVList clearList() {
        return this.wwo.clearList();
    }

    public LayerList sort() {
        if (this.size() <= 0)
            return this;

        Layer[] array = new Layer[this.size()];
        this.toArray(array);
        Arrays.sort(array, Comparator.comparing(Layer::getName));

        this.clear();
        super.addAll(Arrays.asList(array));

        return this;
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        wwo.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        wwo.removePropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        wwo.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        wwo.removePropertyChangeListener(listener);
    }

    public void firePropertyChange(PropertyChangeEvent propertyChangeEvent) {
        wwo.firePropertyChange(propertyChangeEvent);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        wwo.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        wwo.propertyChange(propertyChangeEvent);
    }

    public void onMessage(Message message) {
        wwo.onMessage(message);
    }

    @Override
    public String toString() {
        String r = "";
        for (Layer l : this) {
            r += l.toString() + ", ";
        }
        return r;
    }
}