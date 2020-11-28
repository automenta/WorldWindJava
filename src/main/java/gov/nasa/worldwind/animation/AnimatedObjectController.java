package gov.nasa.worldwind.animation;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;

import java.awt.*;
import java.util.ArrayList;

public class AnimatedObjectController implements RenderingListener, SelectListener {

    private final WorldWindow wwd;
    private final ArrayList<AnimatedObject> animObjects;
    private final AnnotationAttributes metaAttrs;
    private final RenderableLayer objectLayer;
    private final RenderableLayer annotationLayer;
    private Globe globe;
    private boolean started = false;
    private Animatable selectedObject;

    public AnimatedObjectController(WorldWindow wwd, RenderableLayer objectLayer, RenderableLayer annotationLayer) {
        this.wwd = wwd;
        this.animObjects = new ArrayList<>();
        this.objectLayer = objectLayer;
        this.annotationLayer = annotationLayer;
        metaAttrs = new AnnotationAttributes();
        metaAttrs.setCornerRadius(0);
        metaAttrs.setInsets(new Insets(4, 4, 4, 4));
        metaAttrs.setBackgroundColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
        metaAttrs.setTextColor(Color.WHITE);
        metaAttrs.setBorderColor(Color.yellow);
        metaAttrs.setBorderWidth(1);
        metaAttrs.setLeaderGapWidth(4);
        metaAttrs.setDrawOffset(new Point(0, 40));
    }

    @Override
    public void stageChanged(RenderingEvent event) {
        if (event.getStage().equals(RenderingEvent.BEFORE_RENDERING)) {
            if (globe == null) {
                globe = this.wwd.view().getGlobe();
            }
            if (globe != null) {
                if (started) {
                    animObjects.forEach((ao) -> ao.stepAnimation(wwd.view().getGlobe()));
                }
                else {
                    animObjects.forEach((ao) -> ao.startAnimation(wwd.view().getGlobe()));
                    started = true;
                }
            }
        }
    }

    public void addObject(AnimatedObject ao) {
        this.animObjects.add(ao);
    }

    public void startAnimations() {
        this.wwd.addRenderingListener(this);
        this.wwd.addSelectListener(this);
    }

    private void showMetadata(Object o) {
        if (this.selectedObject == o) {
            return; // same thing selected
        }

        if (o instanceof Animatable) {
            this.selectedObject = (Animatable) o;
            Object prevNote = this.selectedObject.getField(AVKey.ANIMATION_ANNOTATION);
            if (prevNote != null) {
                ((GlobeAnnotation) prevNote).getAttributes().setVisible(true);
            }
            else {
                String metadata = this.selectedObject.getField(AVKey.ANIMATION_META_DATA).toString();
                if (metadata != null) {
                    Annotation note = new GlobeAnnotation(metadata, this.selectedObject.getPosition(),
                        this.metaAttrs);
                    this.annotationLayer.add(note);
                    note.getAttributes().setVisible(true);
                    this.selectedObject.setField(AVKey.ANIMATION_ANNOTATION, note);
                }
            }
        }
    }

    @Override
    public void selected(SelectEvent event) {
        // System.out.println(event,event.);
        if (event.getEventAction().equals(SelectEvent.ROLLOVER)) {
            showMetadata(event.getTopObject());
        }
    }
}
