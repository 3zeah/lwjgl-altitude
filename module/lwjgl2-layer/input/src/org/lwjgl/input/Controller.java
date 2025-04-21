/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.input;

/**
 * A game controller of some sort that will provide input. The controller
 * presents buttons and axes. Buttons are either pressed or not pressed. Axis
 * provide analogue values.
 *
 * @author Kevin Glass
 */
public interface Controller {
	/**
	 * Get the name assigned to this controller.
	 *
	 * @return The name assigned to this controller
	 */
	String getName();

	/**
	 * Retrieve the number of buttons available on this controller
	 *
	 * @return The number of butotns available on this controller
	 */
	int getButtonCount();

	/**
	 * Check if a button is currently pressed
	 *
	 * @param index The button to check
	 * @return True if the button is currently pressed
	 */
	boolean isButtonPressed(int index);

	/**
	 * Poll the controller for new data. This will also update
	 * events
	 */
	void poll();

	/**
	 * Get the X-Axis value of the POV on this controller
	 *
	 * @return The X-Axis value of the POV on this controller
	 */
	float getPovX();

	/**
	 * Get the Y-Axis value of the POV on this controller
	 *
	 * @return The Y-Axis value of the POV on this controller
	 */
	float getPovY();

	/**
	 * Retrieve the number of axes available on this controller.
	 *
	 * @return The number of axes available on this controller.
	 */
	int getAxisCount();

	/**
	 * Get the name that's given to the specified axis
	 *
	 * @param index The index of the axis whose name should be retrieved
	 * @return The name of the specified axis.
	 */
	String getAxisName(int index);

	/**
	 * Retrieve the value thats currently available on a specified axis. The
	 * value will always be between 1.0 and -1.0 and will calibrate as values
	 * are passed read. It may be useful to get the player to wiggle the joystick
	 * from side to side to get the calibration right.
	 *
	 * @param index The index of axis to be read
	 * @return The value from the specified axis.
	 */
	float getAxisValue(int index);
}
