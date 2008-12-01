/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
package org.concord.modeler.draw;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Charles Xie
 * 
 */
public class Triangle2D implements Shape {

	private Point2D.Float[] vertex = new Point2D.Float[3];

	public Triangle2D(float xA, float yA, float xB, float yB, float xC, float yC) {
		setVertex(0, xA, yA);
		setVertex(1, xB, yB);
		setVertex(2, xC, yC);
	}

	public void setVertex(int i, float x, float y) {
		if (i < 0 || i > 2)
			throw new IllegalArgumentException("index of vertex must be 0, 1, or 2.");
		if (vertex[i] == null)
			vertex[i] = new Point2D.Float(x, y);
		else vertex[i].setLocation(x, y);
	}

	public Point2D.Float getVertex(int i) {
		if (i < 0 || i > 2)
			throw new IllegalArgumentException("index of vertex must be 0, 1, or 2.");
		return vertex[i];
	}

	public Point2D.Float getCenter() {
		return new Point2D.Float((vertex[0].x + vertex[1].x + vertex[2].x) / 3.0f,
				(vertex[0].y + vertex[1].y + vertex[2].y) / 3.0f);
	}

	public void translate(float dx, float dy) {
		for (int i = 0; i < vertex.length; i++) {
			vertex[0].x += dx;
			vertex[0].y += dy;
		}
	}

	public boolean contains(Point2D p) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean contains(Rectangle2D r) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean contains(double x, double y) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean contains(double x, double y, double w, double h) {
		// TODO Auto-generated method stub
		return false;
	}

	public Rectangle getBounds() {
		int xmin = Integer.MAX_VALUE;
		int ymin = xmin;
		int xmax = -xmin;
		int ymax = -xmin;
		for (Point2D.Float v : vertex) {
			if (xmin > v.x)
				xmin = (int) v.x;
			if (ymin > v.y)
				ymin = (int) v.y;
			if (xmax < v.x)
				xmax = (int) v.x;
			if (ymax < v.y)
				ymax = (int) v.y;
		}
		return new Rectangle(xmin, ymin, xmax - xmin, ymax - ymin);
	}

	public Rectangle2D getBounds2D() {
		// TODO Auto-generated method stub
		return null;
	}

	public PathIterator getPathIterator(AffineTransform at) {
		// TODO Auto-generated method stub
		return null;
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean intersects(Rectangle2D r) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean intersects(double x, double y, double w, double h) {
		// TODO Auto-generated method stub
		return false;
	}

}
