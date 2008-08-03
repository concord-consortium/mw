/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

package org.concord.mw3d.models;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

/**
 * @author Charles Xie
 * 
 */
public class GField implements VectorField {

	private boolean alwaysDown = true;
	private float g;
	private Vector3f direction;
	private final Vector3f down;
	private Matrix3f inverseRotation;

	public GField() {
		direction = new Vector3f(0, 0, 1);
		down = new Vector3f(0, -1, 0);
	}

	public void setAlwaysDown(boolean b) {
		alwaysDown = b;
	}

	public boolean isAlwaysDown() {
		return alwaysDown;
	}

	public void setIntensity(float intensity) {
		this.g = intensity;
	}

	public float getIntensity() {
		return g;
	}

	public void setDirection(float x, float y, float z) {
		direction.set(x, y, z);
	}

	public float[] getDirection() {
		return new float[] { direction.x, direction.y, direction.z };
	}

	void setRotation(Matrix3f rotation) {
		if (inverseRotation == null)
			inverseRotation = new Matrix3f();
		inverseRotation.invert(rotation);
		inverseRotation.transform(down, direction);
	}

	public float compute(Atom a) {
		if (g < MolecularModel.ZERO)
			return 0;
		float temp = g * a.mass * ForceCalculator.GF_CONVERSION_CONSTANT;
		a.fx += temp * direction.x;
		a.fy += temp * direction.y;
		a.fz += temp * direction.z;
		// assume that the center has zero gravitational potential
		return -a.mass * g * (direction.x * a.rx + direction.y * a.ry + direction.z * a.rz);
	}

}