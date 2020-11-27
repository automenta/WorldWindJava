/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.worldwindow.core.layermanager;

import gov.nasa.worldwind.util.WWUtil;

import java.util.*;

/**
 * @author tag
 * @version $Id: LayerPath.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class LayerPath extends ArrayList<String> {
    public LayerPath() {
    }

    public LayerPath(Collection<String> initialPath, String... args) {
        this.addAll(initialPath);

        for (String pathElement : args) {
            if (!WWUtil.isEmpty(pathElement))
                this.add(pathElement);
        }
    }

    public LayerPath(String initialPathEntry, String... args) {
        this.add(initialPathEntry);

        for (String pathElement : args) {
            if (!WWUtil.isEmpty(pathElement))
                this.add(pathElement);
        }
    }

    public LayerPath(Collection<String> initialPathEntries) {
        this.addAll(initialPathEntries);
    }

    public static boolean isEmptyPath(List<String> path) {
        return path == null || path.isEmpty() || WWUtil.isEmpty(path.get(0));
    }

    public LayerPath lastButOne() {
        return this.subPath(0, this.size() - 1);
    }

    public LayerPath subPath(int start, int end) {
        return new LayerPath(this.subList(start, end));
    }

    @Override
    public String toString() {
        if (this.size() == 0)
            return "<empty path>";

        StringBuilder sb = new StringBuilder();

        for (String s : this) {
            if (WWUtil.isEmpty(s))
                s = "<empty>";

            if (sb.isEmpty())
                sb.append(s);
            else
                sb.append("/").append(s);
        }

        return sb.toString();
    }
}