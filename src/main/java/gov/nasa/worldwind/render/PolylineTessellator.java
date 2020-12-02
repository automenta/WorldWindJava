/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import java.nio.IntBuffer;

/**
 * @author dcollins
 * @version $Id: PolylineTessellator.java 2290 2014-08-30 21:27:27Z dcollins $
 */
public class PolylineTessellator {
    protected IntBuffer indices;
    protected int lastIndex = -1;

    public PolylineTessellator() {
        this.indices = IntBuffer.allocate(10);
    }

    public IntBuffer getIndices() {
        return this.indices;
    }

    public void reset() {
        this.indices.clear();
    }

    public void beginPolyline() {
        this.lastIndex = -1;
    }

    public void endPolyline() {
        this.lastIndex = -1;
    }

    public void addVertex(double x, double y, double z, int index) {
        if (this.lastIndex >= 0) {
            IntBuffer i = indices;
            i =            PolylineTessellator.addIndex(i, this.lastIndex);
            this.indices = PolylineTessellator.addIndex(i, index);
        }

        this.lastIndex = index;
    }

    protected static IntBuffer addIndex(IntBuffer buffer, int index) {
        if (!buffer.hasRemaining()) {
            int newCapacity = buffer.capacity() + buffer.capacity() / 2; // increase capacity by 50%
            IntBuffer newBuffer = IntBuffer.allocate(newCapacity);
            newBuffer.put(buffer.flip());
            buffer = newBuffer;
        }

        return buffer.put(index);
    }
}
