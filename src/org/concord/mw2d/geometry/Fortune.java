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

package org.concord.mw2d.geometry;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;

import org.concord.mw2d.models.Particle;

public class Fortune {

	int width, height;
	int xpos;
	boolean drawVoronoi = true, drawDelaunay;
	EventQueue queue;
	ArcTree tree;
	VoronoiDiagram voronoi;
	DelaunayLine delaunay;

	public void showVoronoi(boolean b) {
		drawVoronoi = b;
	}

	public boolean voronoiIsShown() {
		return drawVoronoi;
	}

	public void showDelaunay(boolean b) {
		drawDelaunay = b;
	}

	public boolean delaunayIsShown() {
		return drawDelaunay;
	}

	public void init(int n, Particle[] p, int width, int height) {
		this.width = width;
		this.height = height;
		voronoi = new VoronoiDiagram(width, height, n, p);
		xpos = 0;
		tree = new ArcTree();
		queue = new EventQueue();
		delaunay = new DelaunayLine();
		for (int i = 0; i < voronoi.size(); i++) {
			if (voronoi.get(i) instanceof MyPoint)
				queue.insert(new EventPoint((MyPoint) voronoi.get(i)));
		}
	}

	public void paintVoronoi(Graphics g) {
		if (drawVoronoi)
			voronoi.paint(g);
	}

	public void paintDelaunay(Graphics g) {
		if (drawDelaunay)
			delaunay.paint(g);
	}

	public synchronized void compute() {
		EventPoint eventpoint = queue.pop();
		if (eventpoint != null) {
			do {
				xpos = Math.max(xpos, (int) eventpoint.x);
				eventpoint.action(this);
				eventpoint = queue.pop();
			} while (eventpoint != null);
		}
		if (xpos < width + 1000)
			xpos = width + 1000;
		tree.checkBounds(this, xpos);
	}

}

class DelaunayLine extends ArrayList {

	public void paint(Graphics g) {
		for (Iterator i = iterator(); i.hasNext();)
			((Paintable) i.next()).paint(g);
	}

}

class VoronoiDiagram extends ArrayList {

	@SuppressWarnings("unchecked")
	public VoronoiDiagram(int width, int height, int n, Particle[] p) {
		if (p == null)
			return;
		for (int i = 0; i < n; i++) {
			add(new MyPoint((int) p[i].getRx(), (int) p[i].getRy()));
		}
		checkDegenerate();
	}

	public void checkDegenerate() {
		if (size() > 1) {
			MyPoint min = (MyPoint) get(0), next = min;
			for (int i = 1; i < size(); i++) {
				Object element = get(i);
				if (element instanceof MyPoint) {
					if (((MyPoint) element).x <= min.x) {
						next = min;
						min = (MyPoint) element;
					}
					else if (((MyPoint) element).x <= min.x) {
						next = (MyPoint) element;
					}
				}
			}
			if (min.x == next.x && min != next) {
				min.x -= 1;
			}
		}
	}

	public void paint(Graphics g) {
		for (int i = 0; i < size(); i++)
			((Paintable) get(i)).paint(g);
	}

}