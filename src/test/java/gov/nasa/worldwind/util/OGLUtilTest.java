/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.TextureData;
import gov.nasa.worldwind.Configuration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class OGLUtilTest
{
    private static final String PNG_FILE = "images/earth-map-512x256.png";
    private static final String DDS_FILE = "images/BMNG_world.topo.bathy.200405.3.2048x1024.dds";
    private static final String JPG_FILE = "images/BMNG_world.topo.bathy.200405.3.2048x1024.jpg";

    private GLProfile glProfile;

    @Before
    public void setUp()
    {
        this.glProfile = Configuration.getMaxCompatibleGLProfile();
    }

    @After
    public void tearDown()
    {
        this.glProfile = null;
    }

    @Test
    public void testPngFile() throws IOException
    {
        InputStream s = resource(PNG_FILE);

        TextureData td = OGLUtil.newTextureData(this.glProfile, s, false);

        assertEquals(td.getWidth(), 512);
        assertEquals(td.getHeight(), 256);
    }


    private InputStream resource(String s)
    {
        return getClass().getResourceAsStream("/" + s);
    }



    @Test
    public void testDdsStream() throws IOException
    {
        InputStream s = resource(DDS_FILE);

        TextureData td = OGLUtil.newTextureData(this.glProfile, s, false);

        assertEquals(td.getWidth(), 2048);
        assertEquals(td.getHeight(), 1024);
    }


    @Test
    public void testJpgFile() throws IOException
    {
        InputStream f = resource(JPG_FILE);

        TextureData td = OGLUtil.newTextureData(this.glProfile, f, false);

        assertEquals(td.getWidth(), 2048);
        assertEquals(td.getHeight(), 1024);
    }


    /**
     * Test that the newTextureData supports indexed color PNG images. See http://issues.worldwind.arc.nasa.gov/jira/browse/WWJ-369.
     */
    @Test
    public void testIndexedColorPng() throws IOException
    {

        InputStream f = resource("32x32-icon-nasa-indexed-color.png");


        TextureData td = OGLUtil.newTextureData(this.glProfile, f, false);
        assertEquals(td.getPixelFormat(), GL.GL_RGBA);
    }

    /**
     * Test that the newTextureData supports interlaced PNG images. See http://issues.worldwind.arc.nasa.gov/jira/browse/WWJ-365.
     */
    @Test
    public void testInterlacedPng() throws IOException
    {
        InputStream f = resource("32x32-icon-nasa-interlaced.png");

        TextureData td = OGLUtil.newTextureData(this.glProfile, f, false);

        assertEquals(td.getWidth(), 32);
        assertEquals(td.getHeight(), 32);
    }
}
