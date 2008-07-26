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

import javax.vecmath.Point3f;

/**
 * @author Charles Xie
 * 
 */
public class CuboidObstacle extends Obstacle {

	private Point3f corner;
	private float xmin, xmax, ymin, ymax, zmin, zmax;

	public CuboidObstacle(float rx, float ry, float rz, float lx, float ly, float lz) {
		super(rx, ry, rz);
		corner = new Point3f(Math.max(MIN_THICKNESS, lx), Math.max(MIN_THICKNESS, ly), Math.max(MIN_THICKNESS, lz));
	}

	public CuboidObstacle(CuboidObstacleState state) {
		this(state.getRx(), state.getRy(), state.getRz(), Math.max(MIN_THICKNESS, state.getLx()), Math.max(
				MIN_THICKNESS, state.getLy()), Math.max(MIN_THICKNESS, state.getLz()));
		setColor(new Color(state.getColor()));
		setTranslucent(state.isTranslucent());
	}

	/** corner is a vertex with its coordinates relative to the center all positive. */
	public void setCorner(float lx, float ly, float lz) {
		corner.set(lx, ly, lz);
	}

	public Point3f getCorner() {
		return corner;
	}

	public boolean isContained(char axis) {
		switch (axis) {
		case 'x':
			if (center.x - corner.x < -0.5f * model.getLength() || center.x + corner.x > 0.5f * model.getLength())
				return false;
			break;
		case 'y':
			if (center.y - corner.y < -0.5f * model.getWidth() || center.y + corner.y > 0.5f * model.getWidth())
				return false;
			break;
		case 'z':
			if (center.z - corner.z < -0.5f * model.getHeight() || center.z + corner.z > 0.5f * model.getHeight())
				return false;
			break;
		}
		return true;
	}

	public boolean contains(Point3f p) {
		if (p.x > center.x + corner.x)
			return false;
		if (p.x < center.x - corner.x)
			return false;
		if (p.y > center.y + corner.y)
			return false;
		if (p.y < center.y - corner.y)
			return false;
		if (p.z > center.z + corner.z)
			return false;
		if (p.z < center.z - corner.z)
			return false;
		return true;
	}

	public boolean contains(Atom at) {
		float radius = 0.5f * at.getSigma();
		if (at.rx - radius > center.x + corner.x)
			return false;
		if (at.rx + radius < center.x - corner.x)
			return false;
		if (at.ry - radius > center.y + corner.y)
			return false;
		if (at.ry + radius < center.y - corner.y)
			return false;
		if (at.rz - radius > center.z + corner.z)
			return false;
		if (at.rz + radius < center.z - corner.z)
			return false;
		return true;
	}

	private boolean contains(Atom at, float radius) {
		if (at.rx - radius > xmax)
			return false;
		if (at.rx + radius < xmin)
			return false;
		if (at.ry - radius > ymax)
			return false;
		if (at.ry + radius < ymin)
			return false;
		if (at.rz - radius > zmax)
			return false;
		if (at.rz + radius < zmin)
			return false;
		return true;
	}

	private byte outcode(Atom at, float radius) {
		if (at.rx - at.dx - radius > xmax && at.rx - radius < xmax)
			return OUT_RIGHT;
		if (at.rx - at.dx + radius < xmin && at.rx + radius > xmin)
			return OUT_LEFT;
		if (at.ry - at.dy - radius > ymax && at.ry - radius < ymax)
			return OUT_TOP;
		if (at.ry - at.dy + radius < ymin && at.ry + radius > ymin)
			return OUT_BOTTOM;
		if (at.rz - at.dz - radius > zmax && at.rz - radius < zmax)
			return OUT_FRONT;
		if (at.rz - at.dz + radius < zmin && at.rz + radius > zmin)
			return OUT_REAR;
		// the above algorithm cannot guarantee that the atom will exit the body of the obstacle
		// after reversing the velocity. so we need to double-check, but how?
		return -1;
	}

	void collide() {

		if (model == null)
			return;
		int n = model.getAtomCount();
		if (n <= 0)
			return;

		xmin = center.x - corner.x;
		xmax = center.x + corner.x;
		ymin = center.y - corner.y;
		ymax = center.y + corner.y;
		zmin = center.z - corner.z;
		zmax = center.z + corner.z;

		Atom at = null;
		float radius;
		for (int i = 0; i < n; i++) {
			at = model.getAtom(i);
			radius = at.getSigma() * 0.5f;
			if (contains(at, radius)) {
				switch (outcode(at, radius)) {
				case OUT_RIGHT:
					at.vx = Math.abs(at.vx);
					break;
				case OUT_LEFT:
					at.vx = -Math.abs(at.vx);
					break;
				case OUT_TOP:
					at.vy = Math.abs(at.vy);
					break;
				case OUT_BOTTOM:
					at.vy = -Math.abs(at.vy);
					break;
				case OUT_FRONT:
					at.vz = Math.abs(at.vz);
					break;
				case OUT_REAR:
					at.vz = -Math.abs(at.vz);
					break;
				}

			}
		}

	}

}