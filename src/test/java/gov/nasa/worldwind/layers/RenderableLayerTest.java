/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.render.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RenderableLayerTest
{
    //////////////////////////////////////////////////////////
    // Basic Operation Tests
    //////////////////////////////////////////////////////////

    @Test
    public void testConstructor()
    {
        RenderableLayer layer;

        // Test the parameterless constructor.
        layer = new RenderableLayer();
        assertNotNull("", layer);
    }

    @Test
    public void testAddRenderable()
    {
        Iterable<Renderable> renderables = createExampleIterable();

        RenderableLayer layer = new RenderableLayer();
        for (Renderable item : renderables)
        {
            layer.add(item);
        }

        // Test that the layer contains the renderables.
        assertEquals("", renderables, layer.all());
    }

    @Test
    public void testAddRenderables()
    {
        Iterable<Renderable> renderables = createExampleIterable();

        RenderableLayer layer = new RenderableLayer();
        layer.addAll(renderables);

        // Test that the layer contains the renderables.
        assertEquals("", renderables, layer.all());
    }

    @Test
    public void testInsertRenderable()
    {
        Iterable<Renderable> source = createExampleIterable();

        List<Renderable> renderables = new ArrayList<>();
        RenderableLayer layer = new RenderableLayer();

        for (Renderable renderable : source)
        {
            renderables.add(renderables.size(), renderable);
            layer.add(layer.size(), renderable);
        }

        assertEquals("", renderables, layer.all());
    }

    @Test
    public void testInsertRenderableAtBeginning()
    {
        Collection<Renderable> source = createExampleIterable();

        RenderableLayer layer = new RenderableLayer();
        List<Renderable> renderables = new ArrayList<>(source);
        layer.addAll(source);

        Path inserted = new Path();
        renderables.add(0, inserted);
        layer.add(0, inserted);

        assertEquals("", renderables, layer.all());
    }

    @Test
    public void testInsertRenderableAfterFirst()
    {
        Collection<Renderable> source = createExampleIterable();

        RenderableLayer layer = new RenderableLayer();
        List<Renderable> renderables = new ArrayList<>(source);
        layer.addAll(source);

        Path inserted = new Path();
        renderables.add(1, inserted);
        layer.add(1, inserted);

        assertEquals("", renderables, layer.all());
    }

    @Test
    public void testInsertRenderableAtEnd()
    {
        Collection<Renderable> source = createExampleIterable();

        RenderableLayer layer = new RenderableLayer();
        List<Renderable> renderables = new ArrayList<>(source);
        layer.addAll(source);

        Path inserted = new Path();
        renderables.add(renderables.size(), inserted);
        layer.add(layer.size(), inserted);

        assertEquals("", renderables, layer.all());
    }

    @Test
    public void testRemoveRenderable()
    {
        Iterable<Renderable> renderables = createExampleIterable();

        RenderableLayer layer = new RenderableLayer();
        for (Renderable item : renderables)
        {
            layer.add(item);
        }
        for (Renderable item : renderables)
        {
            layer.remove(item);
        }

        // Test that the layer contains no renderables.
        assertFalse("", layer.all().iterator().hasNext());
    }

    @Test
    public void testRemoveAllRenderables()
    {
        Iterable<Renderable> renderables = createExampleIterable();

        RenderableLayer layer = new RenderableLayer();
        layer.addAll(renderables);
        layer.clear();

        // Test that the layer contains no renderables.
        assertFalse("", layer.all().iterator().hasNext());
    }

//    @Test
//    public void testSetRenderables()
//    {
//        Iterable<Renderable> renderables = createExampleIterable();
//
//        RenderableLayer layer = new RenderableLayer();
//        layer.set(renderables);
//
//        // Test that the layer points to the Iterable.
//        assertSame("", renderables, layer.all());
//    }

    //////////////////////////////////////////////////////////
    // Edge Case Tests

    @Test @Ignore
    public void testMaliciousGetRenderables()
    {
        Iterable<Renderable> renderables = createExampleIterable();

        RenderableLayer layer = new RenderableLayer();
        layer.addAll(renderables);

        Iterable<? extends Renderable> layerRenderables = layer.all();

        // Test that the returned list cannot be modified.
        try
        {
            if (layerRenderables instanceof Collection)
            {
                Collection collection = (Collection) layerRenderables;
                collection.clear();
            }
            else
            {
                Iterator<? extends Renderable> iter = layerRenderables.iterator();
                while (iter.hasNext())
                {
                    iter.next();
                    iter.remove();
                }
            }
        }
        catch (UnsupportedOperationException e)
        {
            e.printStackTrace();
        }

        // Test that the layer contents do not change, even if the returned list can be modified.
        assertEquals("", renderables, layer.all());
    }

    @Test
    public void testDisposeDoesNotClearRenderables()
    {
        Iterable<Renderable> renderables = createExampleIterable();
        Iterable<Renderable> emptyRenderables = new ArrayList<>();

        RenderableLayer layer = new RenderableLayer();
        layer.addAll(renderables);
        layer.dispose();

        // Test that the layer contains the renderables.
        assertEquals("", emptyRenderables, layer.all());
    }

    //////////////////////////////////////////////////////////
    // Exceptional Condition Tests
    //////////////////////////////////////////////////////////

//    @Test
//    public void testAddRenderableFail()
//    {
//        Iterable<Renderable> renderables = createExampleIterable();
//
//        RenderableLayer layer = new RenderableLayer();
//        layer.set(renderables);
//
//        try
//        {
//            // Expecting an IllegalStateException here.
//            layer.add(new Path());
//            fail("Should raise an IllegalStateException");
//        }
//        catch (IllegalStateException e)
//        {
//            e.printStackTrace();
//        }
//    }

//    @Test
//    public void testAddRenderablesFail()
//    {
//        Iterable<Renderable> renderables = createExampleIterable();
//
//        RenderableLayer layer = new RenderableLayer();
//        layer.set(renderables);
//
//        try
//        {
//            // Expecting an IllegalStateException here.
//            layer.addAll(renderables);
//            fail("Should raise an IllegalStateException");
//        }
//        catch (IllegalStateException e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testInsertRenderableFail()
//    {
//        Iterable<Renderable> renderables = createExampleIterable();
//
//        RenderableLayer layer = new RenderableLayer();
//        layer.set(renderables);
//
//        try
//        {
//            // Expecting an IllegalStateException here.
//            layer.add(0, new Path());
//            fail("Should raise an IllegalStateException");
//        }
//        catch (IllegalStateException e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testRemoveRenderableFail()
//    {
//        Iterable<Renderable> renderables = createExampleIterable();
//
//        RenderableLayer layer = new RenderableLayer();
//        layer.set(renderables);
//
//        try
//        {
//            // Expecting an IllegalStateException here.
//            layer.remove(new Path());
//            fail("Should raise an IllegalStateException");
//        }
//        catch (IllegalStateException e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testRemoveAllRenderablesFail()
//    {
//        Iterable<Renderable> renderables = createExampleIterable();
//
//        RenderableLayer layer = new RenderableLayer();
//        layer.set(renderables);
//
//        try
//        {
//            // Expecting an IllegalStateException here.
//            layer.clear();
//            fail("Should raise an IllegalStateException");
//        }
//        catch (IllegalStateException e)
//        {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void testDisposeFail()
//    {
//        Iterable<Renderable> renderables = createExampleIterable();
//
//        RenderableLayer layer = new RenderableLayer();
//        layer.set(renderables);
//
//        try
//        {
//            // Expecting an IllegalStateException here.
//            layer.dispose();
//            fail("Should raise an IllegalStateException");
//        }
//        catch (IllegalStateException e)
//        {
//            e.printStackTrace();
//        }
//    }

    //////////////////////////////////////////////////////////
    // Helper Methods
    //////////////////////////////////////////////////////////

    private static void assertEquals(String message, Iterable<Renderable> expected, Iterable<Renderable> actual)
    {
        if (expected == null)
        {
            assertNull(message, actual);
        }
        else
        {
            Iterator<Renderable> expectedIter = expected.iterator(), actualIter = actual.iterator();
            // Compare the elements in each iterator, as long as they both have elements.
            while (expectedIter.hasNext() && actualIter.hasNext())
            {
                Assert.assertEquals(message, expectedIter.next(), actualIter.next());
            }
            // If either iterator has more elements, then their lengths are different.
            assertFalse(message, expectedIter.hasNext() || actualIter.hasNext());
        }
    }

    private static Collection<Renderable> createExampleIterable()
    {
        //noinspection RedundantArrayCreation
        return Arrays.asList(new Renderable[] {
            new Path(),
            new Path(),
            new Path()});
    }
}