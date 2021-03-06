/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Keys;
import gov.nasa.worldwind.util.RestorableSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.awt.*;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class SizeTest
{
    @Test
    public void testSize()
    {
        // Test with native width and fractional height
        Size size = new Size(Size.NATIVE_DIMENSION, 0, Keys.PIXELS, Size.EXPLICIT_DIMENSION, 0.5, Keys.FRACTION);
        Dimension dim = size.compute(70, 10, 100, 100);
        assertEquals("Dimension should be 70 x 50", dim, new Dimension(70, 50));

        // Test with maintain aspect ratio
        size = new Size(Size.MAINTAIN_ASPECT_RATIO, 0, Keys.PIXELS, Size.EXPLICIT_DIMENSION, 50, Keys.PIXELS);
        dim = size.compute(20, 10, 100, 100);
        assertEquals("Dimension should be 100 x 50", dim, new Dimension(100, 50));
    }

    @Test
    public void testZeroSizeContainer()
    {
        Size size = new Size(Size.EXPLICIT_DIMENSION, 0.5, Keys.FRACTION,
            Size.EXPLICIT_DIMENSION, 0.5, Keys.FRACTION);

        Dimension dim = size.compute(100, 100, 0, 0);

        assertNotNull("Dimension != null", dim);
        assertEquals("Dimension should be zero", dim, new Dimension(0, 0));
    }

    @Test
    public void testZeroSizeRect()
    {
        // Test with fractional dimensions
        Size size = new Size(Size.EXPLICIT_DIMENSION, 0.5, Keys.FRACTION, Size.EXPLICIT_DIMENSION, 0.5,
            Keys.FRACTION);
        Dimension dim = size.compute(0, 0, 100, 100);
        assertEquals("Dimension should be 50 x 50", dim, new Dimension(50, 50));

        // Test with pixel dimensions
        size = new Size(Size.EXPLICIT_DIMENSION, 50, Keys.PIXELS, Size.EXPLICIT_DIMENSION, 50, Keys.PIXELS);
        dim = size.compute(0, 0, 100, 100);
        assertEquals("Dimension should be 50 x 50", dim, new Dimension(50, 50));

        // Test with maintain aspect radio 
        size = new Size(Size.MAINTAIN_ASPECT_RATIO, 0, Keys.PIXELS, Size.MAINTAIN_ASPECT_RATIO, 0, Keys.PIXELS);
        dim = size.compute(0, 0, 100, 100);
        assertEquals("Dimension should be 0 x 0", dim, new Dimension(0, 0));

        // Test with native dimension
        size = new Size(Size.NATIVE_DIMENSION, 0, Keys.PIXELS, Size.NATIVE_DIMENSION, 0, Keys.PIXELS);
        dim = size.compute(0, 0, 100, 100);
        assertEquals("Dimension should be 0 x 0", dim, new Dimension(0, 0));
    }

    @Test
    public void testRestorableStateExplicit()
    {
        // Test with fractional dimensions
        Size expected = new Size(Size.EXPLICIT_DIMENSION, 0.5, Keys.FRACTION, Size.EXPLICIT_DIMENSION, 0.5,
            Keys.FRACTION);

        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        expected.getRestorableState(rs, null);

        Size actual = new Size();
        actual.restoreState(rs, null);

        assertEquals(expected, actual);
    }

    @Test
    public void testRestorableStateNative()
    {
        // Test with fractional dimensions
        Size expected = new Size(Size.NATIVE_DIMENSION, 0, null, Size.NATIVE_DIMENSION, 0, null);

        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        expected.getRestorableState(rs, null);

        Size actual = new Size();
        actual.restoreState(rs, null);

        assertEquals(expected, actual);
    }

    @Test
    public void testRestorableStateAspectRatio()
    {
        // Test with fractional dimensions
        Size expected = new Size(Size.MAINTAIN_ASPECT_RATIO, 0, null, Size.MAINTAIN_ASPECT_RATIO, 0, null);

        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        expected.getRestorableState(rs, null);

        Size actual = new Size();
        actual.restoreState(rs, null);

        assertEquals(expected, actual);
    }

    @Test
    public void testRestorableStateLegacy()
    {
        // Test with fractional dimensions
        Size input = new Size("MaintainAspectRatio", 0, null, "ExplicitDimension", 100, Keys.PIXELS);
        Size expected = new Size(Size.MAINTAIN_ASPECT_RATIO, 0, null, Size.EXPLICIT_DIMENSION, 100, Keys.PIXELS);

        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        input.getRestorableState(rs, null);

        Size actual = new Size();
        actual.restoreState(rs, null);

        assertEquals(expected, actual);
    }
}