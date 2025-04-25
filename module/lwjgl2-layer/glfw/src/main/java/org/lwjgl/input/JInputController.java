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

import net.java.games.input.Component;
import net.java.games.input.Component.Identifier.Axis;
import net.java.games.input.Component.Identifier.Button;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import java.util.ArrayList;

/**
 * A wrapper round a JInput controller that attempts to make the interface
 * more useable.
 *
 * @author Kevin Glass
 */
class JInputController implements Controller {
	/** The JInput controller this class is wrapping */
	private net.java.games.input.Controller target;
	/** The Buttons that have been detected on the JInput controller */
	private ArrayList<Component> buttons = new ArrayList<Component>();
	/** The Axes that have been detected on the JInput controller */
	private ArrayList<Component> axes = new ArrayList<Component>();
	/** The POVs that have been detected on the JInput controller */
	private ArrayList<Component> pov = new ArrayList<Component>();
	/** The state of the buttons last check */
	private boolean[] buttonState;
	/** The values that were read from the pov last check */
	private float[] povValues;
	/** The values that were read from the axes last check */
	private float[] axesValue;
	/** The maximum values read for each axis */
	private float[] axesMax;
	/** The dead zones for each axis */
	private float[] deadZones;
	/** The index of the X axis or -1 if no X axis is defined */
	private int xaxis = -1;
	/** The index of the Y axis or -1 if no Y axis is defined */
	private int yaxis = -1;

	/**
	 * Create a new controller that wraps round a JInput controller and hopefully
	 * makes it easier to use.
	 *
	 * @param index The index this controller has been assigned to
	 * @param target The target JInput controller this class is wrapping
	 */
	JInputController(int index, net.java.games.input.Controller target) {
		this.target = target;

		Component[] sourceAxes = target.getComponents();

		for ( Component sourceAxis : sourceAxes ) {
			if ( sourceAxis.getIdentifier() instanceof Button ) {
				buttons.add(sourceAxis);
			} else if ( sourceAxis.getIdentifier().equals(Axis.POV) ) {
				pov.add(sourceAxis);
			} else {
				axes.add(sourceAxis);
			}
		}

		buttonState = new boolean[buttons.size()];
		povValues = new float[pov.size()];
		axesValue = new float[axes.size()];
		int buttonsCount = 0;
		int axesCount = 0;

		// initialise the state
		for ( Component sourceAxis : sourceAxes ) {
			if ( sourceAxis.getIdentifier() instanceof Button ) {
				buttonState[buttonsCount] = sourceAxis.getPollData() != 0;
				buttonsCount++;
			} else if ( sourceAxis.getIdentifier().equals(Axis.POV) ) {
				// no account for POV yet
				// pov.add(sourceAxes[i]);
			} else {
				axesValue[axesCount] = sourceAxis.getPollData();
				if ( sourceAxis.getIdentifier().equals(Axis.X) ) {
					xaxis = axesCount;
				}
				if ( sourceAxis.getIdentifier().equals(Axis.Y) ) {
					yaxis = axesCount;
				}

				axesCount++;
			}
		}

		axesMax = new float[axes.size()];
		deadZones = new float[axes.size()];

		for (int i=0;i<axesMax.length;i++) {
			axesMax[i] = 1.0f;
			deadZones[i] = 0.05f;
		}
	}

	/*
	 * @see org.lwjgl.input.Controller#getName()
	 */
	public String getName() {
		String name = target.getName();
		return name;
	}

	/*
	 * @see org.lwjgl.input.Controller#getButtonCount()
	 */
	public int getButtonCount() {
		return buttons.size();
	}

	/*
	 * @see org.lwjgl.input.Controller#isButtonPressed(int)
	 */
	public boolean isButtonPressed(int index) {
		return buttonState[index];
	}

	/*
	 * @see org.lwjgl.input.Controller#poll()
	 */
	public void poll() {
		target.poll();

		Event event = new Event();
		EventQueue queue = target.getEventQueue();

		while (queue.getNextEvent(event)) {
			// handle button event
			if (buttons.contains(event.getComponent())) {
				Component button = event.getComponent();
				int buttonIndex = buttons.indexOf(button);
				buttonState[buttonIndex] = event.getValue() != 0;
			}

			// handle pov events
			if (pov.contains(event.getComponent())) {
				Component povComponent = event.getComponent();
				int povIndex = pov.indexOf(povComponent);
				povValues[povIndex] = event.getValue();
			}

			// handle axis updates
			if (axes.contains(event.getComponent())) {
				Component axis = event.getComponent();
				int axisIndex = axes.indexOf(axis);
				float value = axis.getPollData();

				// fixed dead zone since most axis don't report it :(
				if (Math.abs(value) < deadZones[axisIndex]) {
					value = 0;
				}
				if (Math.abs(value) < axis.getDeadZone()) {
					value = 0;
				}
				if (Math.abs(value) > axesMax[axisIndex]) {
					axesMax[axisIndex] = Math.abs(value);
				}

				// normalize the value based on maximum value read in the past
				value /= axesMax[axisIndex];

				axesValue[axisIndex] = value;
			}
		}
	}

	/*
	 * @see org.lwjgl.input.Controller#getAxisCount()
	 */
	public int getAxisCount() {
		return axes.size();
	}

	/*
	 * @see org.lwjgl.input.Controller#getAxisName(int)
	 */
	public String getAxisName(int index) {
		return axes.get(index).getName();
	}

	/*
	 * @see org.lwjgl.input.Controller#getAxisValue(int)
	 */
	public float getAxisValue(int index) {
		return axesValue[index];
	}

	/*
	 * @see org.lwjgl.input.Controller#getPovX()
	 */
	public float getPovX() {
		if (pov.size() == 0) {
			return 0;
		}

		float value = povValues[0];

		if ((value == Component.POV.DOWN_LEFT) ||
		    (value == Component.POV.UP_LEFT) ||
		    (value == Component.POV.LEFT)) {
			return -1;
		}
		if ((value == Component.POV.DOWN_RIGHT) ||
		    (value == Component.POV.UP_RIGHT) ||
		    (value == Component.POV.RIGHT)) {
			return 1;
		}

		return 0;
	}

	/*
	 * @see org.lwjgl.input.Controller#getPovY()
	 */
	public float getPovY() {
		if (pov.size() == 0) {
			return 0;
		}

		float value = povValues[0];

		if ((value == Component.POV.DOWN_LEFT) ||
		    (value == Component.POV.DOWN_RIGHT) ||
		    (value == Component.POV.DOWN)) {
			return 1;
		}
		if ((value == Component.POV.UP_LEFT) ||
		    (value == Component.POV.UP_RIGHT) ||
		    (value == Component.POV.UP)) {
			return -1;
		}

		return 0;
	}
}
