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
public abstract class Obstacle {

	public final static float MIN_THICKNESS = 0.5f;

	final static byte OUT_FRONT = 1;
	final static byte OUT_REAR = 2;
	final static byte OUT_TOP = 4;
	final static byte OUT_BOTTOM = 8;
	final static byte OUT_RIGHT = 16;
	final static byte OUT_LEFT = 32;

	MolecularModel model;
	Point3f center;

	private Color color = Color.cyan;
	private boolean translucent;

	Obstacle(float x, float y, float z) {
		center = new Point3f(x, y, z);
	}

	public void setTranslucent(boolean b) {
		translucent = b;
	}

	public boolean isTranslucent() {
		return translucent;
	}

	public void setColor(Color c) {
		color = c;
	}

	public Color getColor() {
		return color;
	}

	public void setCenter(float x, float y, float z) {
		center.set(x, y, z);
	}

	public void setCenter(Point3f p) {
		center.set(p);
	}

	public Point3f getCenter() {
		return center;
	}

	public void setModel(MolecularModel model) {
		this.model = model;
	}

	public MolecularModel getModel() {
		return model;
	}

	public boolean overlapWithAtoms() {
		int n = model.getAtomCount();
		if (n <= 0)
			return false;
		for (int i = 0; i < n; i++) {
			if (contains(model.getAtom(i)))
				return true;
		}
		return false;
	}

	abstract public boolean contains(Atom at);

	abstract public boolean contains(Point3f p);

	abstract public boolean isContained(char axis);

	abstract void collide();

}