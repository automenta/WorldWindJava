/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;

import gov.nasa.worldwind.geom.Angle;

/**
 * An interface for scene controllers that provide stereo.
 * <p>
 * Note: The {@link WorldWindow} instance must support stereo display in order to use device-supported stereo. See
 * {@link gov.nasa.worldwind.awt.WorldWindowGLCanvas} to learn how to select a stereo device.
 *
 * @author Tom Gaskins
 * @version $Id: StereoSceneController.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface StereoSceneController extends SceneController {
    /**
     * Indicates the current stereo mode of this controller.
     *
     * @return the current stereo mode. See this class' description for the possible modes. This method does not return
     * null. If a null mode was passed to {@link #setStereoMode(String)}, this instance's mode was set to {@link
     * gov.nasa.worldwind.avlist.AVKey#STEREO_MODE_NONE}.
     */
    String getStereoMode();

    /**
     * Specifies the technique used to provide a stereo effect. Defined options are {@link
     * gov.nasa.worldwind.avlist.AVKey#STEREO_MODE_DEVICE} to request device supported stereo, {@link
     * gov.nasa.worldwind.avlist.AVKey#STEREO_MODE_RED_BLUE} to request red-blue anaglyph stereo implemented in
     * software, or {@link gov.nasa.worldwind.avlist.AVKey#STEREO_MODE_NONE} (the default) to request no stereo effect.
     * <p>
     * If <code>STEREO_MODE_DEVICE</code> is requested but the display device does not support stereo, stereo is not
     * applied.
     * <p>
     * See the implementing class to determine how it detects the initial stereo mode.
     *
     * @param mode the technique used to provide the stereo effect. If null, the mode is set to {@link
     *             gov.nasa.worldwind.avlist.AVKey#STEREO_MODE_NONE}.
     */
    void setStereoMode(String mode);

    /**
     * Returns this controller's focus angle, the angle difference between the left and right eye direction.
     *
     * @return this controller's focus angle.
     */
    Angle getFocusAngle();

    /**
     * Specifies the angle difference between the left and right eye direction. Larger angles increase the stereo
     * effect.
     *
     * @param a the left-right eye direction difference. If null, the angle is set to 0.
     */
    void setFocusAngle(Angle a);

    /**
     * Indicates whether to switch the left/right stereo images.
     *
     * @return true to switch the images, otherwise false
     */
    boolean isSwapEyes();

    /**
     * Specifies whether to draw the right eye image in the left eye's position and the left eye's image in the right
     * eye's position.
     *
     * @param swapEyes true to switch the left/right stereo images, otherwise false.
     */
    void setSwapEyes(boolean swapEyes);

    /**
     * Indicates whether stereo is being implemented directly by the display device rather than software.
     *
     * @return true if if stereo is being implemented by the display device, otherwise false.
     */
    boolean isHardwareStereo();

    /**
     * Indicates whether stereo is being applied, either directly by the display device or simulated via software.
     *
     * @return true if stereo is being applied.
     */
    boolean isInStereo();
}
