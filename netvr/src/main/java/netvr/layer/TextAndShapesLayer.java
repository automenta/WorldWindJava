package netvr.layer;

import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;

import java.util.*;

public class TextAndShapesLayer extends RenderableLayer {
    protected final Collection<GeographicText> labels = new ArrayList();
    protected final GeographicTextRenderer textRenderer = new GeographicTextRenderer();

    public TextAndShapesLayer() {
        this.textRenderer.setCullTextEnabled(true);
        this.textRenderer.setCullTextMargin(2);
        this.textRenderer.setDistanceMaxScale(2.0D);
        this.textRenderer.setDistanceMinScale(0.5D);
        this.textRenderer.setDistanceMinOpacity(0.5D);
        this.textRenderer.setEffect("gov.nasa.worldwind.avkey.TextEffectOutline");
    }

    public void addLabel(GeographicText label) {
        this.labels.add(label);
    }

    public void doRender(DrawContext dc) {
        super.doRender(dc);
        this.setActiveLabels(dc);
        this.textRenderer.render(dc, this.labels);
    }

    protected void setActiveLabels(DrawContext dc) {

        for (GeographicText text : this.labels) {
            if (text instanceof ShapefileLayer.Label)
                text.setVisible(((ShapefileLayer.Label) text).isActive(dc));
        }
    }
}