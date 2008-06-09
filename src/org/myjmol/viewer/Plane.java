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

import org.myjmol.g3d.Graphics3D;

/**
 * 
 * @author Charles Xie
 * 
 */
class Plane {

	final static byte LINE_MODE = 0x00;
	final static byte FILL_MODE = 0x01;
	final static byte FILL_AND_FACE_LINE_MODE = 0x02;
	final static byte FILL_WITH_DOTS_MODE = 0x03;
	final static byte GRID_MODE = 0x04;

	private byte mode = FILL_MODE;
	private short colix = Graphics3D.LIME;
	private Point3f center;
	private Point3f[] vertices = new Point3f[4];

	short m = 5, n = 5;
	char axis = 'o';

	Plane() {
		vertices[0] = new Point3f();
		vertices[1] = new Point3f();
		vertices[2] = new Point3f();
		vertices[3] = new Point3f();
		center = new Point3f();
	}

	void setVertices(Point3f[] p) {
		for (int i = 0; i < p.length; i++)
			vertices[i].set(p[i]);
		computeCenter();
	}

	void setVertices(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
			float x4, float y4, float z4) {
		vertices[0].x = x1;
		vertices[0].y = y1;
		vertices[0].z = z1;
		vertices[1].x = x2;
		vertices[1].y = y2;
		vertices[1].z = z2;
		vertices[2].x = x3;
		vertices[2].y = y3;
		vertices[2].z = z3;
		vertices[3].x = x4;
		vertices[3].y = y4;
		vertices[3].z = z4;
		computeCenter();
	}

	void computeCenter() {
		center.x = center.y = center.z = 0;
		for (int i = 0; i < 4; i++)
			center.add(vertices[i]);
		center.scale(0.25f);
	}

	Point3f getVertex(int i) {
		if (i < 0 || i >= 4)
			throw new IllegalArgumentException("vertice index out of bound: " + i);
		return vertices[i];
	}

	Point3f getCenter() {
		return center;
	}

	void move(float dx, float dy, float dz) {
		for (int i = 0; i < 4; i++) {
			vertices[i].x += dx;
			vertices[i].y += dy;
			vertices[i].z += dz;
		}
		computeCenter();
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

}