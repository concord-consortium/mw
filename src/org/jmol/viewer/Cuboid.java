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
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class Cuboid implements Object3D {

	final static byte FRONT = 0x00;
	final static byte REAR = 0x01;
	final static byte TOP = 0x02;
	final static byte BOTTOM = 0x03;
	final static byte RIGHT = 0x04;
	final static byte LEFT = 0x05;

	final static byte LINE_MODE = 0x10;
	final static byte SOLID_MODE = 0x11;

	final static byte EDGES[] = { 0, 1, 0, 2, 0, 4, 1, 3, 1, 5, 2, 3, 2, 6, 3, 7, 4, 5, 4, 6, 5, 7, 6, 7 };

	Point3f corner;
	Point3f center;
	byte mode = SOLID_MODE;
	short colix = Graphics3D.LIME;
	int zDepth;

	private Point3f faceCenter;
	private Point3f[] faceVertices;
	private Point3f tempPoint;
	private static byte[] iOn = new byte[2];
	private static Point3f reusedP3f1, reusedP3f2;
	private static Point3i reusedP3i1, reusedP3i2;

	Cuboid(float xlen, float ylen, float zlen) {
		this(0, 0, 0, xlen, ylen, zlen);
	}

	Cuboid(float x0, float y0, float z0, float xlen, float ylen, float zlen) {
		setCenter(x0, y0, z0);
		setCorner(xlen * 0.5f, ylen * 0.5f, zlen * 0.5f);
	}

	void setMode(byte mode) {
		this.mode = mode;
	}

	byte getMode() {
		return mode;
	}

	void setColix(short colix) {
		this.colix = colix;
	}

	short getColix() {
		return colix;
	}

	public float getRotationRadius(Point3f p) {
		if (tempPoint == null)
			tempPoint = new Point3f();
		float max = 0;
		float dis;
		float x, y, z;
		for (int i = 0; i < 8; i++) {
			x = corner.x * Frame.unitBboxPoints[i].x + center.x;
			y = corner.y * Frame.unitBboxPoints[i].y + center.y;
			z = corner.z * Frame.unitBboxPoints[i].z + center.z;
			tempPoint.set(x, y, z);
			dis = tempPoint.distance(p);
			if (max < dis)
				max = dis;
		}
		return max;
	}

	byte getFace(ExtendedViewer viewer, int x, int y) {
		byte i = 0;
		for (byte j = FRONT; j <= LEFT; j++) {
			if (intersectFace(viewer, j, x, y)) {
				iOn[i++] = j;
			}
		}
		if (i == 0) {
			return -1;
		}
		byte face = -1;
		if (i == 2) {
			if (reusedP3f1 == null) {
				reusedP3f1 = new Point3f();
				reusedP3f2 = new Point3f();
			}
			reusedP3f1.set(getFaceCenter(iOn[0]));
			reusedP3f2.set(getFaceCenter(iOn[1]));
			if (reusedP3i1 == null) {
				reusedP3i1 = new Point3i();
				reusedP3i2 = new Point3i();
			}
			viewer.transformPoint(reusedP3f1, reusedP3i1);
			viewer.transformPoint(reusedP3f2, reusedP3i2);
			if (reusedP3i1.z < reusedP3i2.z) {
				face = iOn[0];
				zDepth = reusedP3i1.z;
			}
			else {
				face = iOn[1];
				zDepth = reusedP3i2.z;
			}
		}
		else {
			face = iOn[0];
			System.err.println("only one face index is found: " + face);
		}
		return face;
	}

	private boolean intersectFace(ExtendedViewer viewer, byte face, int x, int y) {
		boolean b = false;
		switch (face) {
		case FRONT:
			Point3f p = viewer.findPointOnPlane('z', x, y, getMaxZ());
			b = p.x < getMaxX() && p.x > getMinX() && p.y < getMaxY() && p.y > getMinY();
			break;
		case REAR:
			p = viewer.findPointOnPlane('z', x, y, getMinZ());
			b = p.x < getMaxX() && p.x > getMinX() && p.y < getMaxY() && p.y > getMinY();
			break;
		case TOP:
			p = viewer.findPointOnPlane('y', x, y, getMaxY());
			b = p.x < getMaxX() && p.x > getMinX() && p.z < getMaxZ() && p.z > getMinZ();
			break;
		case BOTTOM:
			p = viewer.findPointOnPlane('y', x, y, getMinY());
			b = p.x < getMaxX() && p.x > getMinX() && p.z < getMaxZ() && p.z > getMinZ();
			break;
		case RIGHT:
			p = viewer.findPointOnPlane('x', x, y, getMaxX());
			b = p.y < getMaxY() && p.y > getMinY() && p.z < getMaxZ() && p.z > getMinZ();
			break;
		case LEFT:
			p = viewer.findPointOnPlane('x', x, y, getMinX());
			b = p.y < getMaxY() && p.y > getMinY() && p.z < getMaxZ() && p.z > getMinZ();
			break;
		}
		return b;
	}

	Point3f[] getFaceVertices(byte face) {
		if (faceVertices == null) {
			faceVertices = new Point3f[4];
			for (int i = 0; i < 4; i++)
				faceVertices[i] = new Point3f();
		}
		float x = Math.abs(corner.x);
		float y = Math.abs(corner.y);
		float z = Math.abs(corner.z);
		switch (face) {
		case FRONT:
			faceVertices[0].set(center.x + x, center.y + y, center.z + z);
			faceVertices[1].set(center.x - x, center.y + y, center.z + z);
			faceVertices[2].set(center.x - x, center.y - y, center.z + z);
			faceVertices[3].set(center.x + x, center.y - y, center.z + z);
			return faceVertices;
		case REAR:
			faceVertices[0].set(center.x + x, center.y + y, center.z - z);
			faceVertices[1].set(center.x - x, center.y + y, center.z - z);
			faceVertices[2].set(center.x - x, center.y - y, center.z - z);
			faceVertices[3].set(center.x + x, center.y - y, center.z - z);
			return faceVertices;
		case TOP:
			faceVertices[0].set(center.x + x, center.y + y, center.z + z);
			faceVertices[1].set(center.x - x, center.y + y, center.z + z);
			faceVertices[2].set(center.x - x, center.y + y, center.z - z);
			faceVertices[3].set(center.x + x, center.y + y, center.z - z);
			return faceVertices;
		case BOTTOM:
			faceVertices[0].set(center.x + x, center.y - y, center.z + z);
			faceVertices[1].set(center.x - x, center.y - y, center.z + z);
			faceVertices[2].set(center.x - x, center.y - y, center.z - z);
			faceVertices[3].set(center.x + x, center.y - y, center.z - z);
			return faceVertices;
		case RIGHT:
			faceVertices[0].set(center.x + x, center.y + y, center.z + z);
			faceVertices[1].set(center.x + x, center.y - y, center.z + z);
			faceVertices[2].set(center.x + x, center.y - y, center.z - z);
			faceVertices[3].set(center.x + x, center.y + y, center.z - z);
			return faceVertices;
		case LEFT:
			faceVertices[0].set(center.x - x, center.y + y, center.z + z);
			faceVertices[1].set(center.x - x, center.y - y, center.z + z);
			faceVertices[2].set(center.x - x, center.y - y, center.z - z);
			faceVertices[3].set(center.x - x, center.y + y, center.z - z);
			return faceVertices;
		}
		return null;
	}

	Point3f getFaceCenter(byte face) {
		if (faceCenter == null)
			faceCenter = new Point3f();
		switch (face) {
		case FRONT:
			faceCenter.set(center.x, center.y, getMaxZ());
			return faceCenter;
		case REAR:
			faceCenter.set(center.x, center.y, getMinZ());
			return faceCenter;
		case TOP:
			faceCenter.set(center.x, getMaxY(), center.z);
			return faceCenter;
		case BOTTOM:
			faceCenter.set(center.x, getMinY(), center.z);
			return faceCenter;
		case RIGHT:
			faceCenter.set(getMaxX(), center.y, center.z);
			return faceCenter;
		case LEFT:
			faceCenter.set(getMinX(), center.y, center.z);
			return faceCenter;
		}
		return null;
	}

	public float getMinX() {
		return center.x - Math.abs(corner.x);
	}

	public float getMaxX() {
		return center.x + Math.abs(corner.x);
	}

	public float getMinY() {
		return center.y - Math.abs(corner.y);
	}

	public float getMaxY() {
		return center.y + Math.abs(corner.y);
	}

	public float getMinZ() {
		return center.z - Math.abs(corner.z);
	}

	public float getMaxZ() {
		return center.z + Math.abs(corner.z);
	}

	void setCenter(float x, float y, float z) {
		if (center == null) {
			center = new Point3f(x, y, z);
		}
		else {
			center.set(x, y, z);
		}
	}

	void moveCenter(float dx, float dy, float dz) {
		if (center == null)
			center = new Point3f();
		center.x += dx;
		center.y += dy;
		center.z += dz;
	}

	void setCorner(float x, float y, float z) {
		if (corner == null) {
			corner = new Point3f(x, y, z);
		}
		else {
			corner.set(x, y, z);
		}
	}

}