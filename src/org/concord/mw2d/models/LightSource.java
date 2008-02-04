/*
 *   Copyright (C) 2006  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * END LICENSE */

package org.concord.mw2d.models;

import java.io.Serializable;

/** This class models a light source. */

public class LightSource implements Serializable {

	public final static byte WEST = 101;
	public final static byte EAST = 102;
	public final static byte NORTH = 103;
	public final static byte SOUTH = 104;
	public final static byte OTHER = 105;

	private static float infrared = 0.1f;
	private static float ultraviolet = 15.0f;

	private boolean monochromatic = true;
	private boolean singleBeam;
	private float frequency = 1.0f;
	private int radiationPeriod = 1000;
	private boolean turnOn;
	private float angle;

	/** Create a monochromatic light source that shines from the west. */
	public LightSource() {
	}

	public void setOn(boolean b) {
		turnOn = b;
	}

	public boolean isOn() {
		return turnOn;
	}

	public void setRadiationPeriod(int i) {
		radiationPeriod = i;
	}

	public int getRadiationPeriod() {
		return radiationPeriod;
	}

	public void setDirection(byte i) {
		switch (i) {
		case WEST:
			angle = 0;
			break;
		case EAST:
			angle = (float) Math.PI;
			break;
		case NORTH:
			angle = 0.5f * (float) Math.PI;
			break;
		case SOUTH:
			angle = -0.5f * (float) Math.PI;
			break;
		}
	}

	public byte getDirection() {
		if (Math.abs(angle) < 0.001f)
			return WEST;
		if (Math.abs(angle - Math.PI) < 0.001f)
			return EAST;
		if (Math.abs(angle - 0.5 * Math.PI) < 0.001f)
			return NORTH;
		if (Math.abs(angle + 0.5 * Math.PI) < 0.001f)
			return SOUTH;
		return OTHER;
	}

	public float getAngleOfIncidence() {
		return angle;
	}

	public void setAngleOfIncidence(float angle) {
		this.angle = angle;
	}

	public void setMonochromatic(boolean b) {
		monochromatic = b;
	}

	public boolean isMonochromatic() {
		return monochromatic;
	}

	public void setSingleBeam(boolean b) {
		singleBeam = b;
	}

	public boolean isSingleBeam() {
		return singleBeam;
	}

	public static float getInfraredFrequency() {
		return infrared;
	}

	public static float getUltravioletFrequency() {
		return ultraviolet;
	}

	public void setFrequency(float freq) {
		this.frequency = freq;
	}

	public float getFrequency() {
		return frequency;
	}

	public static float getRandomFrequency() {
		return infrared + (float) ((ultraviolet - infrared) * Math.random());
	}

	public String toString() {
		return "[" + (turnOn ? "On" : "Off") + ", angle=" + getAngleOfIncidence()
				+ (monochromatic ? ", frequency=" + frequency : "") + "]";
	}

}