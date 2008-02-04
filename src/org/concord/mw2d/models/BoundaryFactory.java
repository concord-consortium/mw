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

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class BoundaryFactory {

	private BoundaryFactory() {
	}

	static void setRBC(MolecularModel model) {

		double xmin = model.boundary.getX();
		double xmax = model.boundary.getX() + model.boundary.getWidth();
		double ymin = model.boundary.getY();
		double ymax = model.boundary.getY() + model.boundary.getHeight();

		Atom[] atom = model.atom;
		for (int i = 0; i < model.numberOfAtoms; i++) {
			if (atom[i].rx < xmin + 0.5 * atom[i].sigma) {
				atom[i].rx = xmin + 0.5 * atom[i].sigma;
				atom[i].vx = -atom[i].vx;
			}
			else if (atom[i].rx >= xmax - 0.5 * atom[i].sigma) {
				atom[i].rx = xmax - 0.5 * atom[i].sigma;
				atom[i].vx = -atom[i].vx;
			}
			if (atom[i].ry < ymin + 0.5 * atom[i].sigma) {
				atom[i].ry = ymin + 0.5 * atom[i].sigma;
				atom[i].vy = -atom[i].vy;
			}
			else if (atom[i].ry >= ymax - 0.5 * atom[i].sigma) {
				atom[i].ry = ymax - 0.5 * atom[i].sigma;
				atom[i].vy = -atom[i].vy;
			}
		}

	}

	static void setPBC(MolecularModel model) {
		double x0 = model.boundary.getX();
		double y0 = model.boundary.getY();
		double dx = model.boundary.getWidth();
		double dy = model.boundary.getHeight();
		double x1 = x0 + dx;
		double y1 = y0 + dy;
		for (int i = 0; i < model.numberOfAtoms; i++) {
			if (model.atom[i].isBonded())
				continue;
			if (model.atom[i].rx < x0)
				model.atom[i].rx += dx;
			if (model.atom[i].rx > x1)
				model.atom[i].rx -= dx;
			if (model.atom[i].ry < y0)
				model.atom[i].ry += dy;
			if (model.atom[i].ry > y1)
				model.atom[i].ry -= dy;
		}
	}

	public static void setRBC(Molecule mol, RectangularBoundary boundary) {
		int x0 = (int) boundary.getMinX();
		int y0 = (int) boundary.getMinY();
		int x1 = (int) boundary.getMaxX();
		int y1 = (int) boundary.getMaxY();
		Atom atom, i_N = null, i_S = null, i_W = null, i_E = null;
		int xmax = 0, xmin = 1000, ymax = 0, ymin = 1000;
		Iterator i;
		synchronized (mol) {
			for (i = mol.iterator(); i.hasNext();) {
				atom = (Atom) i.next();
				if (atom.rx > xmax) {
					xmax = (int) atom.rx;
					i_E = atom;
				}
				else if (atom.rx < xmin) {
					xmin = (int) atom.rx;
					i_W = atom;
				}
				if (atom.ry > ymax) {
					ymax = (int) atom.ry;
					i_S = atom;
				}
				else if (atom.ry < ymin) {
					ymin = (int) atom.ry;
					i_N = atom;
				}
			}
			if (i_W != null) {
				if (xmin < x0 + 0.5 * i_W.sigma) {
					for (i = mol.iterator(); i.hasNext();) {
						atom = (Atom) i.next();
						atom.rx += (x0 + 0.5 * i_W.sigma - xmin);
					}
				}
			}
			if (i_E != null) {
				if (xmax > x1 - 0.5 * i_E.sigma) {
					for (i = mol.iterator(); i.hasNext();) {
						atom = (Atom) i.next();
						atom.rx -= (xmax - x1 + 0.5 * i_E.sigma);
					}
				}
			}
			if (i_N != null) {
				if (ymin < y0 + 0.5 * i_N.sigma) {
					for (i = mol.iterator(); i.hasNext();) {
						atom = (Atom) i.next();
						atom.ry += (y0 + 0.5 * i_N.sigma - ymin);
					}
				}
			}
			if (i_S != null) {
				if (ymax > y1 - 0.5 * i_S.sigma) {
					for (i = mol.iterator(); i.hasNext();) {
						atom = (Atom) i.next();
						atom.ry -= (ymax - y1 + 0.5 * i_S.sigma);
					}
				}
			}
		}
	}

	static void setPBC_BondCrossing(MolecularModel model) {
		if (model.molecules.isEmpty())
			return;
		double x0 = model.boundary.getX();
		double y0 = model.boundary.getY();
		double dx = model.boundary.getWidth();
		double dy = model.boundary.getHeight();
		double x1 = x0 + dx;
		double y1 = y0 + dy;
		Molecule mol;
		Point2D p;
		double delta_x, delta_y;
		synchronized (model.molecules) {
			for (Iterator it = model.molecules.iterator(); it.hasNext();) {
				mol = (Molecule) it.next();
				delta_x = 0.0;
				delta_y = 0.0;
				p = mol.getCenterOfMass2D();
				if (p.getX() < x0)
					delta_x = dx;
				if (p.getX() > x1)
					delta_x = -dx;
				if (p.getY() < y0)
					delta_y = dy;
				if (p.getY() > y1)
					delta_y = -dy;
				if (Math.abs(delta_x) > 0.001 || Math.abs(delta_y) > 0.001)
					mol.translateBy(delta_x, delta_y);
			}
		}
	}

	static void setXRYPBC_BondCrossing(MolecularModel model) {
		if (model.molecules.isEmpty())
			return;
		double y0 = model.boundary.getY();
		double dy = model.boundary.getHeight();
		double y1 = y0 + dy;
		Molecule mol;
		Point2D p;
		double delta_y;
		synchronized (model.molecules) {
			for (Iterator it = model.molecules.iterator(); it.hasNext();) {
				mol = (Molecule) it.next();
				delta_y = 0.0;
				p = mol.getCenterOfMass2D();
				if (p.getY() < y0)
					delta_y = dy;
				if (p.getY() > y1)
					delta_y = -dy;
				if (Math.abs(delta_y) > 0.001)
					mol.translateBy(0.0, delta_y);
			}
		}
	}

	static void setXPYRBC_BondCrossing(MolecularModel model) {
		if (model.molecules.isEmpty())
			return;
		double x0 = model.boundary.getX();
		double dx = model.boundary.getWidth();
		double x1 = x0 + dx;
		Molecule mol;
		Point2D p;
		double delta_x;
		synchronized (model.molecules) {
			for (Iterator it = model.molecules.iterator(); it.hasNext();) {
				mol = (Molecule) it.next();
				delta_x = 0.0;
				p = mol.getCenterOfMass2D();
				if (p.getX() < x0)
					delta_x = dx;
				if (p.getX() > x1)
					delta_x = -dx;
				if (Math.abs(delta_x) > 0.001)
					mol.translateBy(delta_x, 0.0);
			}
		}
	}

	/**
	 * create mirror images of bonds when applying periodic boundary conditions. This should be called whenever there
	 * are bonds in the central box.
	 * 
	 * @return a list containing the mirror bond lines
	 */
	public static List createMirrorBonds(MolecularModel model) {

		List box = model.boundary.mirrorBoxes;
		if (box == null || box.isEmpty())
			return null;
		List<Line2D.Float> lines = Collections.synchronizedList(new ArrayList<Line2D.Float>());
		List<Line2D> mirrors = Collections.synchronizedList(new ArrayList<Line2D>());
		RadialBond rBond;
		synchronized (model.bonds) {
			for (Iterator it = model.bonds.iterator(); it.hasNext();) {
				rBond = (RadialBond) it.next();
				lines.add(new Line2D.Float((float) rBond.getAtom1().rx, (float) rBond.getAtom1().ry, (float) rBond
						.getAtom2().rx, (float) rBond.getAtom2().ry));
			}
		}

		Rectangle rect;
		float dx, dy, x1, y1, x2, y2;
		int w = model.getView().getWidth();
		int h = model.getView().getHeight();
		synchronized (box) {
			for (Iterator i = box.iterator(); i.hasNext();) {
				rect = (Rectangle) i.next();
				dx = (float) (rect.x - model.boundary.x);
				dy = (float) (rect.y - model.boundary.y);
				synchronized (lines) {
					for (Line2D.Float l2d : lines) {
						x1 = l2d.x1 + dx;
						y1 = l2d.y1 + dy;
						x2 = l2d.x2 + dx;
						y2 = l2d.y2 + dy;
						if (Math.min(x1, x2) > 0 && Math.max(x1, x2) < w && Math.min(y1, y2) > 0
								&& Math.max(y1, y2) < h) {
							mirrors.add(new Line2D.Float(x1, y1, x2, y2));
						}
						else if (Line2D.linesIntersect(x1, y1, x2, y2, 0, 0, 0, h)
								|| Line2D.linesIntersect(x1, y1, x2, y2, w, 0, w, h)
								|| Line2D.linesIntersect(x1, y1, x2, y2, 0, 0, w, 0)
								|| Line2D.linesIntersect(x1, y1, x2, y2, 0, h, w, h)) {
							mirrors.add(new Line2D.Float(x1, y1, x2, y2));
						}
					}
				}
			}
		}
		return mirrors;
	}

}