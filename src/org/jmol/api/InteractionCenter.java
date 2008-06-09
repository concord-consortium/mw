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
package org.jmol.api;

import javax.vecmath.Point3f;

import org.myjmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
public class InteractionCenter extends Attachment {

	private Point3f center;
	private int charge = 1;
	private float radius = 0.5f;
	private int hostIndex = -1;

	public InteractionCenter() {
		super();
		setKeyRgb(Graphics3D.getArgb(Graphics3D.OLIVE));
		center = new Point3f();
	}

	public void setHost(int i) {
		hostIndex = i;
	}

	public int getHost() {
		return hostIndex;
	}

	public void setCharge(int i) {
		charge = i;
	}

	public int getCharge() {
		return charge;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

	public float getRadius() {
		return radius;
	}

	public void setCoordinates(float x, float y, float z) {
		center.set(x, y, z);
	}

	public Point3f getCoorindates() {
		return center;
	}

	public float getX() {
		return center.x;
	}

	public float getY() {
		return center.y;
	}

	public float getZ() {
		return center.z;
	}

}
