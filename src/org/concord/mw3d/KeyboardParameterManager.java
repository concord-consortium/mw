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
package org.concord.mw3d;

/**
 * @author Charles Xie
 * 
 */
class KeyboardParameterManager {

	private final static float ROTATION_ANGLE_STEP = (float) Math.PI / 180.0f;
	private final static float ATOM_DISPLACEMENT_STEP = 0.1f;

	final static byte SLOW = 0x00;
	final static byte FAST = 0x01;

	private float fasterTimes = 5;

	private static KeyboardParameterManager sharedInstance = new KeyboardParameterManager();

	private KeyboardParameterManager() {
	}

	static KeyboardParameterManager sharedInstance() {
		return sharedInstance;
	}

	void setFasterTimes(float x) {
		fasterTimes = x;
	}

	float getFasterTimes() {
		return fasterTimes;
	}

	float getRotationAngle(byte mode) {
		if (mode == FAST)
			return fasterTimes * ROTATION_ANGLE_STEP;
		return ROTATION_ANGLE_STEP;
	}

	float getAtomDisplacement(byte mode) {
		if (mode == FAST)
			return fasterTimes * ATOM_DISPLACEMENT_STEP;
		return ATOM_DISPLACEMENT_STEP;
	}

}