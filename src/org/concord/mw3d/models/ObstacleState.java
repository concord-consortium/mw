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
package org.concord.mw3d.models;

import java.awt.Color;
import java.io.Serializable;

import javax.vecmath.Point3f;

/**
 * @author Charles Xie
 * 
 */
public abstract class ObstacleState implements Serializable {

	private float rx, ry, rz;
	private int argb = Color.cyan.getRGB();
	private boolean translucent;

	public void setCenter(Point3f center) {
		rx = center.x;
		ry = center.y;
		rz = center.z;
	}

	public void setTranslucent(boolean b) {
		translucent = b;
	}

	public boolean isTranslucent() {
		return translucent;
	}

	public void setColor(int argb) {
		this.argb = argb;
	}

	public int getColor() {
		return argb;
	}

	public void setRx(float rx) {
		this.rx = rx;
	}

	public float getRx() {
		return rx;
	}

	public void setRy(float ry) {
		this.ry = ry;
	}

	public float getRy() {
		return ry;
	}

	public void setRz(float rz) {
		this.rz = rz;
	}

	public float getRz() {
		return rz;
	}

}