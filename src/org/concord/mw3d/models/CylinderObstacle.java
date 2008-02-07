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
public class CylinderObstacle extends Obstacle {

	public final static float MIN_RADIUS = 2.5f;

	final static byte OUT_LATERAL = 64;

	private char axis = 'z';
	private float height, radius;
	private float xmin, xmax, ymin, ymax, zmin, zmax;
	private float dx, dy, vdotd, vdotv, discriminant, t1, t2, xc, yc;

	public CylinderObstacle(float rx, float ry, float rz, char axis, float h, float r) {
		super(rx, ry, rz);
		this.height = h;
		this.radius = r;
		this.axis = axis;
	}

	public CylinderObstacle(CylinderObstacleState state) {
		this(state.getRx(), state.getRy(), state.getRz(), state.getAxis(), Math.max(MIN_THICKNESS, state.getHeight()),
				Math.max(MIN_THICKNESS, state.getRadius()));
		setColor(new Color(state.getColor()));
		setTranslucent(state.isTranslucent());
	}

	public char getAxis() {
		return axis;
	}

	public void setHeight(float h) {
		this.height = h;
	}

	public float getHeight() {
		return height;
	}

	public void setRadius(float r) {
		this.radius = r;
	}

	public float getRadius() {
		return radius;
	}

	private float getMinX() {
		if (axis == 'x')
			return center.x - 0.5f * height;
		return center.x - radius;
	}

	private float getMaxX() {
		if (axis == 'x')
			return center.x + 0.5f * height;
		return center.x + radius;
	}

	private float getMinY() {
		if (axis == 'y')
			return center.y - 0.5f * height;
		return center.y - radius;
	}

	private float getMaxY() {
		if (axis == 'y')
			return center.y + 0.5f * height;
		return center.y + radius;
	}

	private float getMinZ() {
		if (axis == 'z')
			return center.z - 0.5f * height;
		return center.z - radius;
	}

	private float getMaxZ() {
		if (axis == 'z')
			return center.z + 0.5f * height;
		return center.z + radius;
	}

	public boolean isContained(char axis) {
		switch (axis) {
		case 'x':
			if (getMinX() < -0.5f * model.getLength() || getMaxX() > 0.5f * model.getLength())
				return false;
			break;
		case 'y':
			if (getMinY() < -0.5f * model.getWidth() || getMaxY() > 0.5f * model.getWidth())
				return false;
			break;
		case 'z':
			if (getMinZ() < -0.5f * model.getHeight() || getMaxZ() > 0.5f * model.getHeight())
				return false;
			break;
		}
		return true;
	}

	public boolean contains(Point3f p) {
		float h2 = height * 0.5f;
		float a12, b12;
		switch (axis) {
		case 'x':
			if (p.x > center.x - h2 && p.x < center.x + h2) {
				a12 = p.y - center.y;
				b12 = p.z - center.z;
				if (a12 * a12 + b12 * b12 < radius * radius)
					return true;
			}
			break;
		case 'y':
			if (p.y > center.y - h2 && p.y < center.y + h2) {
				a12 = p.x - center.x;
				b12 = p.z - center.z;
				if (a12 * a12 + b12 * b12 < radius * radius)
					return true;
			}
			break;
		case 'z':
			if (p.z > center.z - h2 && p.z < center.z + h2) {
				a12 = p.x - center.x;
				b12 = p.y - center.y;
				if (a12 * a12 + b12 * b12 < radius * radius)
					return true;
			}
			break;
		}
		return false;
	}

	public boolean contains(Atom at) {
		return at.overlapCylinder(center.x, center.y, center.z, axis, height, radius);
	}

	private byte outcode(Atom at, float r) {
		switch (axis) {
		case 'x':
			if (at.rx - at.dx - r > xmax && at.rx - r < xmax)
				return OUT_RIGHT;
			if (at.rx - at.dx + r < xmin && at.rx + r > xmin)
				return OUT_LEFT;
			break;
		case 'y':
			if (at.ry - at.dy - r > ymax && at.ry - r < ymax)
				return OUT_TOP;
			if (at.ry - at.dy + r < ymin && at.ry + r > ymin)
				return OUT_BOTTOM;
			break;
		case 'z':
			if (at.rz - at.dz - r > zmax && at.rz - r < zmax)
				return OUT_FRONT;
			if (at.rz - at.dz + r < zmin && at.rz + r > zmin)
				return OUT_REAR;
			break;
		}
		return OUT_LATERAL;
	}

	void collide() {

		if (model == null)
			return;
		int n = model.getAtomCount();
		if (n <= 0)
			return;

		xmin = getMinX();
		xmax = getMaxX();
		ymin = getMinY();
		ymax = getMaxY();
		zmin = getMinZ();
		zmax = getMaxZ();

		Atom at = null;
		float radius;
		for (int i = 0; i < n; i++) {
			at = model.getAtom(i);
			radius = at.getSigma() * 0.5f;
			if (contains(at)) {
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
				case OUT_LATERAL:
					switch (axis) {
					case 'x':
						boolean b = collideLaterally(at.ry, at.rz, at.dy, at.dz, center.y, center.z, at.vy, at.vz);
						if (b) {
							at.vy = dx;
							at.vz = dy;
						}
						break;
					case 'y':
						b = collideLaterally(at.rx, at.rz, at.dx, at.dz, center.x, center.z, at.vx, at.vz);
						if (b) {
							at.vx = dx;
							at.vz = dy;
						}
						break;
					case 'z':
						b = collideLaterally(at.rx, at.ry, at.dx, at.dy, center.x, center.y, at.vx, at.vy);
						if (b) {
							at.vx = dx;
							at.vy = dy;
						}
						break;
					}
					break;
				}
			}
		}

	}

	// calculation of reflection vectors uses R = 2(N dot L)N - L. See p. 125 of Math for 3D Game by Eric Lengyel
	private boolean collideLaterally(float atomRx, float atomRy, float atomDx, float atomDy, float centerX,
			float centerY, float atomVx, float atomVy) {
		dx = atomRx - atomDx - centerX;
		dy = atomRy - atomDy - centerY;
		vdotd = atomVx * dx + atomVy * dy;
		vdotv = atomVx * atomVx + atomVy * atomVy;
		// the discriminant has a prefactor 4, but it is omitted here because it gets cancelled out later
		discriminant = vdotd * vdotd - vdotv * (dx * dx + dy * dy - radius * radius);
		if (discriminant <= 0)
			return false; // no collision (tangential contact is not considered collision)
		discriminant = (float) Math.sqrt(discriminant);
		vdotv = 1.0f / vdotv;
		t1 = (vdotd + discriminant) * vdotv;
		t2 = (vdotd - discriminant) * vdotv;
		// now check which one results in a closer position (xc, yc) to the atom
		dx = atomVx * t1;
		dy = atomVy * t1;
		vdotd = dx * dx + dy * dy; // reuse vdotd for storage
		dx = atomVx * t2;
		dy = atomVy * t2;
		vdotv = dx * dx + dy * dy; // reuse vdotv for storage
		if (vdotd > vdotv) {
			xc = atomRx - atomDx + dx;
			yc = atomRy - atomDy + dy;
		}
		else {
			xc = atomRx - atomDx + atomVx * t1;
			yc = atomRy - atomDy + atomVy * t1;
		}
		// calculate the normal vector at (xc, yc)
		dx = xc - centerX;
		dy = yc - centerY;
		vdotd = (float) (1.0 / Math.sqrt(dx * dx + dy * dy));
		dx *= vdotd;
		dy *= vdotd;
		// normalize the velocity vector and store the results in xc, yc
		vdotv = (float) Math.sqrt(atomVx * atomVx + atomVy * atomVy);
		vdotd = 1.0f / vdotv;
		xc = atomVx * vdotd;
		yc = atomVy * vdotd;
		// vdotd now stores 2*(N dot L)
		vdotd = 2 * (dx * xc + dy * yc);
		// (t1, t2) now stores the normalized vector of the reflected direction
		t1 = xc - vdotd * dx;
		t2 = yc - vdotd * dy;
		// System.out.println(vdotv + "," + t1 + "," + t2 + "=" + (t1 * t1 + t2 * t2)); // must be approximately 1
		// calculate the reflected velocity direction
		dx = vdotv * t1;
		dy = vdotv * t2;
		return true;
	}

}