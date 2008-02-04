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

import javax.vecmath.Point3f;

/**
 * @author Charles Xie
 * 
 */
public class CuboidObstacleState extends ObstacleState {

	private float lx = Obstacle.MIN_THICKNESS;
	private float ly = Obstacle.MIN_THICKNESS;
	private float lz = Obstacle.MIN_THICKNESS;

	public CuboidObstacleState() {
	}

	public void setCorner(Point3f corner) {
		lx = corner.x;
		ly = corner.y;
		lz = corner.z;
	}

	public void setLx(float lx) {
		this.lx = lx;
	}

	public float getLx() {
		return lx;
	}

	public void setLy(float ly) {
		this.ly = ly;
	}

	public float getLy() {
		return ly;
	}

	public void setLz(float lz) {
		this.lz = lz;
	}

	public float getLz() {
		return lz;
	}

}