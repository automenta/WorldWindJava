/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Disposable;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * http://java.sun.com/products/java-media/2D/reference/faqs/index.html#Q_What_are_fractional_metrics_Wh
 *
 * @author tag
 * @version $Id: TextRendererCache.java 2053 2014-06-10 20:16:57Z tgaskins $
 */
public class TextRendererCache implements Disposable {
    protected final ConcurrentHashMap<Object, TextRenderer> textRendererMap;

    public TextRendererCache() {
        this.textRendererMap = new ConcurrentHashMap<>();
    }

    protected static void dispose(TextRenderer textRenderer) {
        if (textRenderer != null) {
            textRenderer.dispose();
        }
    }

    public void dispose() {
        this.disposeAll();
        this.textRendererMap.clear();
    }

    public TextRenderer get(Object key) {

        return this.textRendererMap.get(key);
    }

    public void put(Object key, TextRenderer textRenderer) {

        TextRenderer oldTextRenderer = this.textRendererMap.put(key, textRenderer);

        if (oldTextRenderer != null) {
            TextRendererCache.dispose(oldTextRenderer);
        }
    }

    public void remove(Object key) {

        TextRenderer textRenderer = this.textRendererMap.remove(key);

        if (textRenderer != null) {
            TextRendererCache.dispose(textRenderer);
        }
    }

    public boolean contains(Object key) {

        return this.textRendererMap.containsKey(key);
    }

    public void clear() {
        this.disposeAll();
        this.textRendererMap.clear();
    }

    protected void disposeAll() {
        for (Map.Entry<Object, TextRenderer> e : this.textRendererMap.entrySet()) {
            if (e.getValue() != null) {
                TextRendererCache.dispose(e.getValue());
            }
        }
    }

    public static class CacheKey {
        private final Font font;
        private final boolean antialiased;
        private final boolean useFractionalMetrics;
        private final boolean mipmap;

        public CacheKey(Font font, boolean antialiased, boolean useFractionalMetrics, boolean mipmap) {

            this.font = font;
            this.antialiased = antialiased;
            this.useFractionalMetrics = useFractionalMetrics;
            this.mipmap = mipmap;
        }

        public final Font getFont() {
            return this.font;
        }

        public final boolean isAntialiased() {
            return this.antialiased;
        }

        public final boolean isUseFractionalMetrics() {
            return this.useFractionalMetrics;
        }

        public final boolean isMipmap() {
            return this.mipmap;
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || this.getClass() != o.getClass())
                return false;

            CacheKey that = (CacheKey) o;

            return (this.antialiased == that.antialiased)
                && (this.useFractionalMetrics == that.useFractionalMetrics)
                && (this.mipmap == that.mipmap)
                && (this.font.equals(that.font));
        }

        public int hashCode() {
            int result = this.font.hashCode();
            result = 31 * result + (this.antialiased ? 1 : 0);
            result = 31 * result + (this.useFractionalMetrics ? 1 : 0);
            result = 31 * result + (this.mipmap ? 1 : 0);
            return result;
        }
    }
}
