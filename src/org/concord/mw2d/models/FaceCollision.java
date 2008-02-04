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

/**
 * This is a face collision event object. It records which side of an obstacle or boundary was hit by which particle
 * with what velocity at what location of the face.
 * 
 * @author Qian Xie
 */

class FaceCollision {

	private byte face;
	private short index;
	private double vx, vy;
	private double rx, ry;

	public FaceCollision() {
	}

	public FaceCollision(byte face, short index, double rx, double ry, double vx, double vy) {
		this.face = face;
		this.index = index;
		this.rx = rx;
		this.ry = ry;
		this.vx = vx;
		this.vy = vy;
	}

	public byte getFace() {
		return face;
	}

	public short getParticleIndex() {
		return index;
	}

	public double getRx() {
		return rx;
	}

	public double getRy() {
		return ry;
	}

	public double getVx() {
		return vx;
	}

	public double getVy() {
		return vy;
	}

	public String toString() {
		return "[" + face + ":" + index + ", vx=" + vx + ", vy=" + vy + ", rx=" + rx + ", ry=" + ry;
	}

}