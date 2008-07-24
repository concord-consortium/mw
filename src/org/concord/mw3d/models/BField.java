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

import javax.vecmath.Vector3f;

public class BField implements VectorField {

	private float intensity = 1;
	private Vector3f direction;
	private boolean local;

	public BField() {
		direction = new Vector3f(0, 0, 1);
	}

	public void setLocal(boolean b) {
		local = b;
	}

	public boolean isLocal() {
		return local;
	}

	public void setIntensity(float intensity) {
		this.intensity = intensity;
	}

	public float getIntensity() {
		return intensity;
	}

	public void setDirection(Vector3f direction) {
		this.direction.set(direction);
	}

	public Vector3f getDirection() {
		return direction;
	}

	public float compute(Atom a) {
		float temp = intensity * a.charge * ForceCalculator.GF_CONVERSION_CONSTANT / a.mass;
		a.fx += temp * (a.vy * direction.z - a.vz * direction.y);
		a.fy += temp * (a.vz * direction.x - a.vx * direction.z);
		a.fz += temp * (a.vx * direction.y - a.vy * direction.x);
		return 0;
	}

}