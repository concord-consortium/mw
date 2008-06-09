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
package org.myjmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.myjmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class Cylinder implements Object3D {

	final static byte TOP = 0x00;
	final static byte BOTTOM = 0x01;
	final static byte LATERAL = 0x02;

	char axis = 'z';
	Point3f center;
	float a = 2, b = 2;
	float height = ExtendedViewer.MIN_OBSTACLE_SIZE;
	short colix = Graphics3D.CYAN;
	byte endcaps = Graphics3D.ENDCAPS_FLAT;
	int zDepth;

	// used when the axis is not x, y, or z.
	private Point3f end1, end2;

	private Point3f faceCenter;
	private Point3f tempPoint;
	private static byte[] iOn = new byte[3];
	private static Point3f reusedP3f1, reusedP3f2, reusedP3f3;
	private static Point3i reusedP3i1, reusedP3i2, reusedP3i3;

	Cylinder() {
		center = new Point3f();
	}

	void setEnds(Point3f p1, Point3f p2) {
		if (end1 == null)
			end1 = new Point3f();
		if (end2 == null)
			end2 = new Point3f();
		end1.set(p1);
		end2.set(p2);
	}

	Point3f getEnd1() {
		return end1;
	}

	Point3f getEnd2() {
		return end2;
	}

	void setColix(short colix) {
		this.colix = colix;
	}

	byte getFace(ExtendedViewer viewer, int x, int y) {
		byte i = 0;
		if (intersectFace(viewer, TOP, x, y)) {
			iOn[i++] = TOP;
		}
		if (intersectFace(viewer, BOTTOM, x, y)) {
			iOn[i++] = BOTTOM;
		}
		if (intersectFace(viewer, LATERAL, x, y)) {
			iOn[i++] = LATERAL;
		}
		if (reusedP3f1 == null) {
			reusedP3f1 = new Point3f();
			reusedP3f2 = new Point3f();
			reusedP3f3 = new Point3f();
		}
		if (reusedP3i1 == null) {
			reusedP3i1 = new Point3i();
			reusedP3i2 = new Point3i();
			reusedP3i3 = new Point3i();
		}
		byte face = -1;
		if (i == 3) {
			reusedP3f1.set(iOn[0] == LATERAL ? center : getFaceCenter(iOn[0]));
			reusedP3f2.set(iOn[1] == LATERAL ? center : getFaceCenter(iOn[1]));
			reusedP3f3.set(iOn[2] == LATERAL ? center : getFaceCenter(iOn[2]));
			viewer.transformPoint(reusedP3f1, reusedP3i1);
			viewer.transformPoint(reusedP3f2, reusedP3i2);
			viewer.transformPoint(reusedP3f3, reusedP3i3);
			zDepth = Math.min(reusedP3i1.z, Math.min(reusedP3i2.z, reusedP3i3.z));
			if (reusedP3i1.z == zDepth) {
				face = iOn[0];
			}
			else if (reusedP3i2.z == zDepth) {
				face = iOn[1];
			}
			else if (reusedP3i3.z == zDepth) {
				face = iOn[2];
			}
		}
		else if (i == 2) {
			reusedP3f1.set(iOn[0] == LATERAL ? center : getFaceCenter(iOn[0]));
			reusedP3f2.set(iOn[1] == LATERAL ? center : getFaceCenter(iOn[1]));
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
		else if (i == 1) {
			reusedP3f1.set(iOn[0] == LATERAL ? center : getFaceCenter(iOn[0]));
			viewer.transformPoint(reusedP3f1, reusedP3i1);
			face = iOn[0];
			zDepth = reusedP3i1.z;
		}
		return face;
	}

	Point3f getFaceCenter(byte face) {
		if (faceCenter == null)
			faceCenter = new Point3f();
		switch (face) {
		case TOP:
			switch (axis) {
			case 'x':
				faceCenter.set(center.x + 0.5f * height, center.y, center.z);
				break;
			case 'y':
				faceCenter.set(center.x, center.y + 0.5f * height, center.z);
				break;
			case 'z':
				faceCenter.set(center.x, center.y, center.z + 0.5f * height);
				break;
			}
			return faceCenter;
		case BOTTOM:
			switch (axis) {
			case 'x':
				faceCenter.set(center.x - 0.5f * height, center.y, center.z);
				break;
			case 'y':
				faceCenter.set(center.x, center.y - 0.5f * height, center.z);
				break;
			case 'z':
				faceCenter.set(center.x, center.y, center.z - 0.5f * height);
				break;
			}
			return faceCenter;
		}
		return null;
	}

	private boolean intersectFace(ExtendedViewer viewer, byte face, int x, int y) {
		Point3f p;
		float s12, t12;
		switch (axis) {
		case 'x':
			switch (face) {
			case TOP:
				p = viewer.findPointOnPlane('x', x, y, center.x + 0.5f * height);
				s12 = (p.y - center.y) / a;
				t12 = (p.z - center.z) / b;
				if (s12 * s12 + t12 * t12 <= 1)
					return true;
				break;
			case BOTTOM:
				p = viewer.findPointOnPlane('x', x, y, center.x - 0.5f * height);
				s12 = (p.y - center.y) / a;
				t12 = (p.z - center.z) / b;
				if (s12 * s12 + t12 * t12 <= 1)
					return true;
				break;
			case LATERAL:
				p = viewer.findPointOnPlane('y', x, y, center.y);
				if (Math.abs(p.x - center.x) <= 0.5f * height && Math.abs(p.z - center.z) <= b)
					return true;
				p = viewer.findPointOnPlane('z', x, y, center.z);
				if (Math.abs(p.x - center.x) <= 0.5f * height && Math.abs(p.y - center.y) <= a)
					return true;
				break;
			}
			break;
		case 'y':
			switch (face) {
			case TOP:
				p = viewer.findPointOnPlane('y', x, y, center.y + 0.5f * height);
				s12 = (p.x - center.x) / a;
				t12 = (p.z - center.z) / b;
				if (s12 * s12 + t12 * t12 <= 1)
					return true;
				break;
			case BOTTOM:
				p = viewer.findPointOnPlane('y', x, y, center.y - 0.5f * height);
				s12 = (p.x - center.x) / a;
				t12 = (p.z - center.z) / b;
				if (s12 * s12 + t12 * t12 <= 1)
					return true;
				break;
			case LATERAL:
				p = viewer.findPointOnPlane('z', x, y, center.z);
				if (Math.abs(p.y - center.y) <= 0.5f * height && Math.abs(p.x - center.x) <= a)
					return true;
				p = viewer.findPointOnPlane('x', x, y, center.x);
				if (Math.abs(p.y - center.y) <= 0.5f * height && Math.abs(p.z - center.z) <= b)
					return true;
				break;
			}
			break;
		case 'z':
			switch (face) {
			case TOP:
				p = viewer.findPointOnPlane('z', x, y, center.z + 0.5f * height);
				s12 = (p.x - center.x) / a;
				t12 = (p.y - center.y) / b;
				if (s12 * s12 + t12 * t12 <= 1)
					return true;
				break;
			case BOTTOM:
				p = viewer.findPointOnPlane('z', x, y, center.z - 0.5f * height);
				s12 = (p.x - center.x) / a;
				t12 = (p.y - center.y) / b;
				if (s12 * s12 + t12 * t12 <= 1)
					return true;
				break;
			case LATERAL:
				p = viewer.findPointOnPlane('x', x, y, center.x);
				if (Math.abs(p.z - center.z) <= 0.5f * height && Math.abs(p.y - center.y) <= b)
					return true;
				p = viewer.findPointOnPlane('y', x, y, center.y);
				if (Math.abs(p.z - center.z) <= 0.5f * height && Math.abs(p.x - center.x) <= a)
					return true;
				break;
			}
			break;
		}
		return false;
	}

	void setTop(Point3f p) {
		switch (axis) {
		case 'x':
			p.set(center.x + 0.5f * height, center.y, center.z);
			break;
		case 'y':
			p.set(center.x, center.y + 0.5f * height, center.z);
			break;
		case 'z':
			p.set(center.x, center.y, center.z + 0.5f * height);
			break;
		}
	}

	void setBottom(Point3f p) {
		switch (axis) {
		case 'x':
			p.set(center.x - 0.5f * height, center.y, center.z);
			break;
		case 'y':
			p.set(center.x, center.y - 0.5f * height, center.z);
			break;
		case 'z':
			p.set(center.x, center.y, center.z - 0.5f * height);
			break;
		}
	}

	void setCenter(float x, float y, float z) {
		center.set(x, y, z);
	}

	void moveCenter(float dx, float dy, float dz) {
		center.x += dx;
		center.y += dy;
		center.z += dz;
	}

	public float getMinX() {
		if (axis == 'x')
			return center.x - 0.5f * height;
		return center.x - a;
	}

	public float getMaxX() {
		if (axis == 'x')
			return center.x + 0.5f * height;
		return center.x + a;
	}

	public float getMinY() {
		if (axis == 'y')
			return center.y - 0.5f * height;
		if (axis == 'x')
			return center.y - a;
		return center.y - b;
	}

	public float getMaxY() {
		if (axis == 'y')
			return center.y + 0.5f * height;
		if (axis == 'x')
			return center.y + a;
		return center.y + b;
	}

	public float getMinZ() {
		if (axis == 'z')
			return center.z - 0.5f * height;
		return center.z - b;
	}

	public float getMaxZ() {
		if (axis == 'z')
			return center.z + 0.5f * height;
		return center.z + b;
	}

	public float getRotationRadius(Point3f p) {
		if (tempPoint == null)
			tempPoint = new Point3f();
		float x12, y12, inv;
		float d1 = 0, d2 = 0;
		switch (axis) {
		case 'x':
			x12 = p.y - center.y;
			y12 = p.z - center.z;
			inv = (float) (1.0 / Math.sqrt(x12 * x12 + y12 * y12));
			tempPoint.y = center.y - a * inv * x12;
			tempPoint.z = center.z - b * inv * y12;
			tempPoint.x = center.x - 0.5f * height;
			d1 = tempPoint.distance(p);
			tempPoint.x = center.x + 0.5f * height;
			d2 = tempPoint.distance(p);
			break;
		case 'y':
			x12 = p.x - center.x;
			y12 = p.z - center.z;
			inv = (float) (1.0 / Math.sqrt(x12 * x12 + y12 * y12));
			tempPoint.x = center.x - a * inv * x12;
			tempPoint.z = center.z - b * inv * y12;
			tempPoint.y = center.y - 0.5f * height;
			d1 = tempPoint.distance(p);
			tempPoint.y = center.y + 0.5f * height;
			d2 = tempPoint.distance(p);
			break;
		case 'z':
			x12 = p.x - center.x;
			y12 = p.y - center.y;
			inv = (float) (1.0 / Math.sqrt(x12 * x12 + y12 * y12));
			tempPoint.x = center.x - a * inv * x12;
			tempPoint.y = center.y - b * inv * y12;
			tempPoint.z = center.z - 0.5f * height;
			d1 = tempPoint.distance(p);
			tempPoint.z = center.z + 0.5f * height;
			d2 = tempPoint.distance(p);
			break;
		}
		return Math.max(d1, d2);
	}

}