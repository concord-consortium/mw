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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.Iterator;

import org.concord.modeler.math.CatmullRom;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.ViewAttribute;

public class CurvedRibbon extends MolecularObject {

	/* the default Catmull-Rom tension parameter (0.5) */
	final static double TENSION = 0.5;

	final static double[] xCR = new double[4];
	final static double[] yCR = new double[4];
	final static int SMOOTHNESS = 20;
	static int[] xpoints = new int[50];
	static int[] ypoints = new int[50];
	static int[] xpoints2 = new int[50];
	static int[] ypoints2 = new int[50];
	static float[] beadSize = new float[50];
	static int xtip1, ytip1;
	static int xtip2, ytip2;
	GeneralPath path, selectedPath, path2, selectedPath2;
	Polygon polygon, selectedPolygon, inner, outer;
	private Area area;
	private boolean closed;
	Color contrastBgColor;

	CurvedRibbon() {
		polygon = new Polygon();
		path = new GeneralPath();
	}

	/** converts a molecule into a smart spline */
	public CurvedRibbon(Molecule m) {
		this();
		setModel(m.getHostModel());
		if (model == null)
			throw new RuntimeException("Model not found");
		int n = m.size();
		for (int i = 0; i < n; i++)
			addAtom(m.getAtom(i));
		RadialBondCollection rbc = model.getBonds();
		RadialBond rb = null;
		synchronized (rbc.getSynchronizationLock()) {
			for (Iterator it = rbc.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				if (contains(rb.getAtom1()) && contains(rb.getAtom2())) {
					rb.setSmart(true);
					rb.setSolid(false);
					closed = rb.isClosed();
				}
			}
		}
		if (m instanceof CurvedRibbon) {
			setBackground(((CurvedRibbon) m).getBackground());
		}
	}

	/**
	 * construct a smart spline whose vertices are given by those of the passed polygon
	 */
	public CurvedRibbon(Polygon p, MolecularModel m2, boolean closed) {

		this();
		setModel(m2);
		this.closed = closed;

		if (p == null)
			throw new IllegalArgumentException("p cannot be null");

		if (p.npoints > xpoints.length) {
			xpoints = new int[p.npoints];
			ypoints = new int[p.npoints];
		}

		int n = model.getNumberOfAtoms();
		int m = p.npoints;

		double bondStrength = 0.5;
		double bendStrength = 200.0;

		// translate atoms to polygon vertices
		int k = n;
		for (int i = 0; i < m; i++) {
			model.atom[k].eraseProperties();
			model.atom[k].translateTo(p.xpoints[i], p.ypoints[i]);
			model.atom[k].setElement(model.getElement(element));
			model.atom[k].setColor(null);
			if (!model.getRecorderDisabled())
				model.atom[k].initializeMovieQ(model.getMovie().getCapacity());
			addAtom(model.atom[k]);
			k++;
		}
		model.setNumberOfAtoms(n + m);

		// build radial bonds
		RadialBond rb;
		double xij, yij, dij;
		for (int i = n; i < n + m - 1; i++) {
			xij = model.getAtom(i).rx - model.getAtom(i + 1).rx;
			yij = model.getAtom(i).ry - model.getAtom(i + 1).ry;
			dij = Math.sqrt(xij * xij + yij * yij);
			rb = new RadialBond(model.getAtom(i), model.getAtom(i + 1), dij, bondStrength);
			rb.setSmart(true);
			rb.setClosed(closed);
			if (this instanceof CurvedSurface)
				rb.setSolid(true);
			model.getBonds().add(rb);
		}
		if (closed) {
			// finish the loop
			xij = model.getAtom(n).rx - model.getAtom(n + m - 1).rx;
			yij = model.getAtom(n).ry - model.getAtom(n + m - 1).ry;
			dij = Math.sqrt(xij * xij + yij * yij);
			rb = new RadialBond(model.getAtom(n), model.getAtom(n + m - 1), dij, bondStrength);
			rb.setSmart(true);
			rb.setClosed(closed);
			if (this instanceof CurvedSurface)
				rb.setSolid(true);
			model.getBonds().add(rb);
		}

		// build angular bonds
		AngularBond ab;
		double xjk, yjk, djk;
		double theta;
		for (int i = n; i < n + m - 2; i++) {
			xij = model.getAtom(i).rx - model.getAtom(i + 1).rx;
			yij = model.getAtom(i).ry - model.getAtom(i + 1).ry;
			dij = Math.sqrt(xij * xij + yij * yij);
			xjk = model.getAtom(i + 1).rx - model.getAtom(i + 2).rx;
			yjk = model.getAtom(i + 1).ry - model.getAtom(i + 2).ry;
			djk = Math.sqrt(xjk * xjk + yjk * yjk);
			theta = computeTheta(-xij * xjk / (dij * djk) - yij * yjk / (dij * djk));
			ab = new AngularBond(model.getAtom(i), model.getAtom(i + 2), model.getAtom(i + 1), theta, bendStrength);
			model.getBends().add(ab);
		}
		if (closed) {
			// build extra angular bonds
			xij = model.getAtom(n + m - 2).rx - model.getAtom(n + m - 1).rx;
			yij = model.getAtom(n + m - 2).ry - model.getAtom(n + m - 1).ry;
			dij = Math.sqrt(xij * xij + yij * yij);
			xjk = model.getAtom(n + m - 1).rx - model.getAtom(n).rx;
			yjk = model.getAtom(n + m - 1).ry - model.getAtom(n).ry;
			djk = Math.sqrt(xjk * xjk + yjk * yjk);
			theta = computeTheta(-xij * xjk / (dij * djk) - yij * yjk / (dij * djk));
			ab = new AngularBond(model.getAtom(n + m - 2), model.getAtom(n), model.getAtom(n + m - 1), theta,
					bendStrength);
			model.getBends().add(ab);
			xij = model.getAtom(n + m - 1).rx - model.getAtom(n).rx;
			yij = model.getAtom(n + m - 1).ry - model.getAtom(n).ry;
			dij = Math.sqrt(xij * xij + yij * yij);
			xjk = model.getAtom(n).rx - model.getAtom(n + 1).rx;
			yjk = model.getAtom(n).ry - model.getAtom(n + 1).ry;
			djk = Math.sqrt(xjk * xjk + yjk * yjk);
			theta = computeTheta(-xij * xjk / (dij * djk) - yij * yjk / (dij * djk));
			ab = new AngularBond(model.getAtom(n + m - 1), model.getAtom(n + 1), model.getAtom(n), theta, bendStrength);
			model.getBends().add(ab);
		}

	}

	static double computeTheta(double crossProduct) {
		if (crossProduct > 1.0)
			crossProduct = 1.0;
		else if (crossProduct < -1.0)
			crossProduct = -1.0;
		return Math.acos(crossProduct);
	}

	public void setSelected(boolean b) {
		super.setSelected(b);
		if (b) {
			if (selectedPolygon == null) {
				selectedPolygon = new Polygon();
			}
			else {
				selectedPolygon.reset();
			}
			if (selectedPath == null) {
				selectedPath = new GeneralPath();
			}
			else {
				selectedPath.reset();
			}
			createDashed();
		}
		else {
			if (selectedPolygon != null)
				selectedPolygon = null;
			if (selectedPath != null)
				selectedPath = null;
		}
	}

	public Shape getShape() {
		return path;
	}

	public synchronized void render(Graphics2D g) {

		Color oldColor = g.getColor();
		Stroke oldStroke = g.getStroke();

		if (path != null) {

			createClosedSpline();

			if (!model.view.getShowSites()) {

				if (!closed) {

					g.setColor(isMarked() ? model.view.getMarkColor() : getBackground());
					g.fill(path);
					g.setColor(model.view.contrastBackground());
					g.setStroke(stroke);
					g.draw(path);

				}
				else {

					if (area != null) {
						g.setColor(getBackground());
						g.fill(area);
						g.setColor(model.view.contrastBackground());
						g.setStroke(stroke);
						g.draw(area);
					}

				}

				int i = 0xffffff ^ getBackground().getRGB();
				if (contrastBgColor == null || i != contrastBgColor.getRGB())
					contrastBgColor = new Color(i);
				g.setColor(contrastBgColor);
				g.setStroke(ViewAttribute.THIN);
				synchronized (getSynchronizedLock()) {
					Atom a;
					for (Iterator it = iterator(); it.hasNext();) {
						a = (Atom) it.next();
						if (model.view.getDrawCharge()) {
							if (a.getCharge() > Particle.ZERO) {
								g.drawLine((int) (a.rx - 4.0), (int) a.ry, (int) (a.rx + 4.0), (int) a.ry);
								g.drawLine((int) a.rx, (int) (a.ry - 4.0), (int) a.rx, (int) (a.ry + 4.0));
							}
							else if (a.getCharge() < -Particle.ZERO) {
								g.drawLine((int) (a.rx - 4.0), (int) a.ry, (int) (a.rx + 4.0), (int) a.ry);
							}
						}
						PointRestraint.render(g, a);
					}
				}

			}

		}

		if (isSelected() && model.view.getShowSelectionHalo()) {
			createDashed();
			if (selectedPath != null) {
				g.setStroke(ViewAttribute.THIN_DASHED);
				g.draw(selectedPath);
			}
			if (closed) {
				if (selectedPath2 != null) {
					g.draw(selectedPath2);
				}
			}
			if (model.view.getAction() == UserAction.ROTA_ID) {
				Point com = getCenterOfMass();
				g.fillOval(com.x - 5, com.y - 5, 10, 10);
				g.setStroke(ViewAttribute.THIN);
				g.drawOval(com.x - 40, com.y - 40, 80, 80);
				g.draw(rotateCrossLine[0]);
				g.setStroke(ViewAttribute.MODERATE);
				g.draw(rotateCrossLine[1]);
				g.setStroke(ViewAttribute.THIN);
				g.draw(rotateRect);
			}
		}

		if (((AtomisticView) model.getView()).velocityVectorShown())
			drawVelocityVectorOfCenterOfMass(g);
		if (((AtomisticView) model.getView()).momentumVectorShown())
			drawMomentumVectorOfCenterOfMass(g);

		g.setColor(oldColor);
		g.setStroke(oldStroke);

	}

	void createDashed() {
		if (selectedPolygon == null)
			return;
		selectedPolygon.reset();
		if (selectedPath == null)
			return;
		selectedPath.reset();
		if (size() > beadSize.length)
			beadSize = new float[size()];
		Atom a;
		int i = 0;
		synchronized (getSynchronizedLock()) {
			for (Iterator it = iterator(); it.hasNext();) {
				a = (Atom) it.next();
				selectedPolygon.addPoint((int) a.rx, (int) a.ry);
				beadSize[i++] = (float) (a.getSigma());
			}
		}
		if (closed) {
			createDoubleEnvelope(selectedPolygon);
			createSpline(outer, selectedPath, SMOOTHNESS, true);
			if (selectedPath2 == null)
				selectedPath2 = new GeneralPath();
			createSpline(inner, selectedPath2, SMOOTHNESS, true);
		}
		else {
			createEnvelope(selectedPolygon);
			createSpline(selectedPolygon, selectedPath, SMOOTHNESS, true);
		}
	}

	private void createClosedSpline() {
		polygon.reset();
		if (size() > beadSize.length)
			beadSize = new float[size()];
		Atom a;
		int i = 0;
		synchronized (getSynchronizedLock()) {
			for (Iterator it = iterator(); it.hasNext();) {
				a = (Atom) it.next();
				polygon.addPoint((int) a.rx, (int) a.ry);
				beadSize[i++] = (float) (a.getSigma() * 0.6);
			}
		}
		if (closed) {
			createDoubleEnvelope(polygon);
			createSpline(outer, path, SMOOTHNESS, true);
			if (path2 == null)
				path2 = new GeneralPath();
			createSpline(inner, path2, SMOOTHNESS, true);
			area = new Area(path);
			area.subtract(new Area(path2));
		}
		else {
			createEnvelope(polygon);
			createSpline(polygon, path, SMOOTHNESS, true);
		}
	}

	/**
	 * @param steps
	 *            the number of steps between two adjacent points; ideally, it should have been determined using the
	 *            length of the curved path between the points.
	 */
	public static void createSpline(Polygon poly, GeneralPath gpath, int steps, boolean closed) {

		if (poly == null || gpath == null || steps <= 0)
			throw new IllegalArgumentException("Wrong arguments");

		int n = poly.npoints;
		if (n < (closed ? 3 : 2))
			return;

		gpath.reset();
		if (closed) {
			gpath.moveTo(poly.xpoints[n - 1], poly.ypoints[n - 1]);
		}
		else {
			gpath.moveTo(poly.xpoints[0], poly.ypoints[0]);
		}

		if (steps > 1) {

			double u;
			double px, py;
			int index;
			double inv_steps = 1.0 / steps;

			int i0 = closed ? 0 : 1;
			for (int i = i0; i < n; i++) {

				for (int j = 0; j < 4; j++) { // Initialize points m-2, m-1, m, m+1
					index = (i + j - 2 + n) % n;
					xCR[j] = poly.xpoints[index];
					yCR[j] = poly.ypoints[index];
				}

				for (int k = 0; k < steps; k++) {
					u = k * inv_steps;
					px = CatmullRom.getBendingFunction(-2, u, TENSION) * xCR[0]
							+ CatmullRom.getBendingFunction(-1, u, TENSION) * xCR[1]
							+ CatmullRom.getBendingFunction(0, u, TENSION) * xCR[2]
							+ CatmullRom.getBendingFunction(1, u, TENSION) * xCR[3];
					py = CatmullRom.getBendingFunction(-2, u, TENSION) * yCR[0]
							+ CatmullRom.getBendingFunction(-1, u, TENSION) * yCR[1]
							+ CatmullRom.getBendingFunction(0, u, TENSION) * yCR[2]
							+ CatmullRom.getBendingFunction(1, u, TENSION) * yCR[3];
					gpath.lineTo((float) px, (float) py);
				}
			}

		}
		else {

			for (int i = 1; i < n; i++) {
				gpath.lineTo(poly.xpoints[i], poly.ypoints[i]);
			}

		}

		if (closed)
			gpath.closePath();

	}

	private synchronized void createEnvelope(Polygon poly) {
		int n = poly.npoints;
		float k = 0, p = 0;
		float x = 0, y = 0;
		if (n > xpoints.length || n > ypoints.length || n > xpoints2.length || n > ypoints2.length) {
			xpoints = new int[n];
			ypoints = new int[n];
			xpoints2 = new int[n];
			ypoints2 = new int[n];
		}
		for (int i = 0; i < n; i++) {
			if (i == 0) {
				if (!closed) {
					if (poly.ypoints[0] == poly.ypoints[1]) {
						if (poly.xpoints[0] > poly.xpoints[1]) {
							xtip1 = (int) (poly.xpoints[0] + beadSize[0]);
						}
						else {
							xtip1 = (int) (poly.xpoints[0] - beadSize[0]);
						}
						ytip1 = poly.ypoints[0];
						p = 0;
					}
					else if (poly.xpoints[0] == poly.xpoints[1]) {
						if (poly.ypoints[0] > poly.ypoints[1]) {
							ytip1 = (int) (poly.ypoints[0] + beadSize[0]);
						}
						else {
							ytip1 = (int) (poly.ypoints[0] - beadSize[0]);
						}
						xtip1 = poly.xpoints[0];
						p = 1;
					}
					else {
						k = -(float) (poly.xpoints[1] - poly.xpoints[0]) / (poly.ypoints[1] - poly.ypoints[0]);
						p = (float) (1.0 / Math.sqrt(1 + k * k));
						float k1 = -1.0f / k;
						float p1 = (float) (1.0 / Math.sqrt(1 + k1 * k1));
						if (poly.xpoints[0] > poly.xpoints[1]) {
							x = poly.xpoints[0] + beadSize[0] * p1;
						}
						else {
							x = poly.xpoints[0] - beadSize[0] * p1;
						}
						xtip1 = (int) x;
						ytip1 = (int) (poly.ypoints[0] + k1 * (x - poly.xpoints[0]));
					}
				}
				else {
					if (poly.ypoints[1] != poly.ypoints[n - 1]) {
						k = -(float) (poly.xpoints[1] - poly.xpoints[n - 1]) / (poly.ypoints[1] - poly.ypoints[n - 1]);
						p = (float) (1.0 / Math.sqrt(1 + k * k));
					}
					else {
						p = 0;
					}
				}
			}
			else if (i == n - 1) {
				if (!closed) {
					if (poly.ypoints[n - 1] == poly.ypoints[n - 2]) {
						if (poly.xpoints[n - 1] > poly.xpoints[n - 2]) {
							xtip2 = (int) (poly.xpoints[n - 1] + beadSize[n - 1]);
						}
						else {
							xtip2 = (int) (poly.xpoints[n - 1] - beadSize[n - 1]);
						}
						ytip2 = poly.ypoints[n - 1];
						p = 0;
					}
					else if (poly.xpoints[n - 1] == poly.xpoints[n - 2]) {
						if (poly.ypoints[n - 1] > poly.ypoints[n - 2]) {
							ytip2 = (int) (poly.ypoints[n - 1] + beadSize[n - 1]);
						}
						else {
							ytip2 = (int) (poly.ypoints[n - 1] - beadSize[n - 1]);
						}
						xtip2 = poly.xpoints[n - 1];
						p = 1;
					}
					else {
						k = -(float) (poly.xpoints[n - 1] - poly.xpoints[n - 2])
								/ (poly.ypoints[n - 1] - poly.ypoints[n - 2]);
						p = (float) (1.0 / Math.sqrt(1 + k * k));
						float k1 = -1.0f / k;
						float p1 = (float) (1.0 / Math.sqrt(1 + k1 * k1));
						if (poly.xpoints[n - 1] > poly.xpoints[n - 2]) {
							x = poly.xpoints[n - 1] + beadSize[n - 1] * p1;
						}
						else {
							x = poly.xpoints[n - 1] - beadSize[n - 1] * p1;
						}
						xtip2 = (int) x;
						ytip2 = (int) (poly.ypoints[n - 1] + k1 * (x - poly.xpoints[n - 1]));
					}
				}
				else {
					if (poly.ypoints[n - 2] != poly.ypoints[0]) {
						k = -(float) (poly.xpoints[n - 2] - poly.xpoints[0]) / (poly.ypoints[n - 2] - poly.ypoints[0]);
						p = (float) (1.0 / Math.sqrt(1 + k * k));
					}
					else {
						p = 0;
					}
				}
			}
			else {
				if (poly.ypoints[i - 1] != poly.ypoints[i + 1]) {
					k = -(float) (poly.xpoints[i + 1] - poly.xpoints[i - 1])
							/ (poly.ypoints[i + 1] - poly.ypoints[i - 1]);
					p = (float) (1.0 / Math.sqrt(1 + k * k));
				}
				else {
					p = 0;
				}
			}
			if (Double.doubleToLongBits(p) != Double.doubleToLongBits(0)) {
				x = poly.xpoints[i] - beadSize[i] * p;
				xpoints[i] = (int) x;
				ypoints[i] = (int) (poly.ypoints[i] + k * (x - poly.xpoints[i]));
				x = poly.xpoints[i] + beadSize[i] * p;
				xpoints2[i] = (int) x;
				ypoints2[i] = (int) (poly.ypoints[i] + k * (x - poly.xpoints[i]));
			}
			else {
				xpoints[i] = poly.xpoints[i];
				ypoints[i] = (int) (poly.ypoints[i] - beadSize[i]);
				xpoints2[i] = poly.xpoints[i];
				ypoints2[i] = (int) (poly.ypoints[i] + beadSize[i]);
			}
			// search crossings at nearest neighbors and correct them
			if (i > 0 && i < n) {
				if (Line2D.linesIntersect(xpoints[i - 1], ypoints[i - 1], xpoints[i], ypoints[i], poly.xpoints[i - 1],
						poly.ypoints[i - 1], poly.xpoints[i], poly.ypoints[i])
						|| (i > 1 && Line2D.linesIntersect(xpoints[i - 1], ypoints[i - 1], xpoints[i], ypoints[i],
								poly.xpoints[i - 2], poly.ypoints[i - 2], poly.xpoints[i - 1], poly.ypoints[i - 1]))
						|| (i < n - 1 && Line2D.linesIntersect(xpoints[i - 1], ypoints[i - 1], xpoints[i], ypoints[i],
								poly.xpoints[i], poly.ypoints[i], poly.xpoints[i + 1], poly.ypoints[i + 1]))) {
					x = xpoints[i];
					y = ypoints[i];
					xpoints[i] = xpoints2[i];
					ypoints[i] = ypoints2[i];
					xpoints2[i] = (int) x;
					ypoints2[i] = (int) y;
				}
			}
		}
		poly.reset();
		if (!closed) {
			poly.addPoint(xtip1, ytip1);
			for (int i = 0; i < n; i++) {
				poly.addPoint(xpoints[i], ypoints[i]);
			}
			poly.addPoint(xtip2, ytip2);
			for (int i = n - 1; i >= 0; i--) {
				poly.addPoint(xpoints2[i], ypoints2[i]);
			}
		}
		else {
			for (int i = 0; i < n; i++) {
				poly.addPoint(xpoints[i], ypoints[i]);
			}
			for (int i = 0; i < n; i++) {
				poly.addPoint(xpoints2[i], ypoints2[i]);
			}
		}
	}

	private synchronized void createDoubleEnvelope(Polygon poly) {
		int n = poly.npoints;
		float k = 0, p = 0;
		float x = 0, y = 0;
		if (n > xpoints.length || n > ypoints.length || n > xpoints2.length || n > ypoints2.length) {
			xpoints = new int[n];
			ypoints = new int[n];
			xpoints2 = new int[n];
			ypoints2 = new int[n];
		}
		for (int i = 0; i < n; i++) {
			if (i == 0) {
				if (poly.ypoints[1] != poly.ypoints[n - 1]) {
					k = -(float) (poly.xpoints[1] - poly.xpoints[n - 1]) / (poly.ypoints[1] - poly.ypoints[n - 1]);
					p = (float) (1.0 / Math.sqrt(1 + k * k));
				}
				else {
					p = 0;
				}
			}
			else if (i == n - 1) {
				if (poly.ypoints[n - 2] != poly.ypoints[0]) {
					k = -(float) (poly.xpoints[n - 2] - poly.xpoints[0]) / (poly.ypoints[n - 2] - poly.ypoints[0]);
					p = (float) (1.0 / Math.sqrt(1 + k * k));
				}
				else {
					p = 0;
				}
			}
			else {
				if (poly.ypoints[i - 1] != poly.ypoints[i + 1]) {
					k = -(float) (poly.xpoints[i + 1] - poly.xpoints[i - 1])
							/ (poly.ypoints[i + 1] - poly.ypoints[i - 1]);
					p = (float) (1.0 / Math.sqrt(1 + k * k));
				}
				else {
					p = 0;
				}
			}
			if (Double.doubleToLongBits(p) != Double.doubleToLongBits(0)) {
				x = poly.xpoints[i] - beadSize[i] * p;
				xpoints[i] = (int) x;
				ypoints[i] = (int) (poly.ypoints[i] + k * (x - poly.xpoints[i]));
				x = poly.xpoints[i] + beadSize[i] * p;
				xpoints2[i] = (int) x;
				ypoints2[i] = (int) (poly.ypoints[i] + k * (x - poly.xpoints[i]));
			}
			else {
				xpoints[i] = poly.xpoints[i];
				ypoints[i] = (int) (poly.ypoints[i] - beadSize[i]);
				xpoints2[i] = poly.xpoints[i];
				ypoints2[i] = (int) (poly.ypoints[i] + beadSize[i]);
			}
			// search crossings at nearest neighbors and correct them
			if (i > 0 && i < n) {
				if (Line2D.linesIntersect(xpoints[i - 1], ypoints[i - 1], xpoints[i], ypoints[i], poly.xpoints[i - 1],
						poly.ypoints[i - 1], poly.xpoints[i], poly.ypoints[i])
						|| (i > 1 && Line2D.linesIntersect(xpoints[i - 1], ypoints[i - 1], xpoints[i], ypoints[i],
								poly.xpoints[i - 2], poly.ypoints[i - 2], poly.xpoints[i - 1], poly.ypoints[i - 1]))
						|| (i < n - 1 && Line2D.linesIntersect(xpoints[i - 1], ypoints[i - 1], xpoints[i], ypoints[i],
								poly.xpoints[i], poly.ypoints[i], poly.xpoints[i + 1], poly.ypoints[i + 1]))) {
					x = xpoints[i];
					y = ypoints[i];
					xpoints[i] = xpoints2[i];
					ypoints[i] = ypoints2[i];
					xpoints2[i] = (int) x;
					ypoints2[i] = (int) y;
				}
			}
		}
		if (outer == null) {
			outer = new Polygon();
		}
		else {
			outer.reset();
		}
		if (inner == null) {
			inner = new Polygon();
		}
		else {
			inner.reset();
		}
		for (int i = 0; i < n; i++) {
			inner.addPoint(xpoints2[i], ypoints2[i]);
		}
		if (inner.contains(xpoints[0], ypoints[0])) {
			inner.reset();
			for (int i = 0; i < n; i++) {
				inner.addPoint(xpoints[i], ypoints[i]);
				outer.addPoint(xpoints2[i], ypoints2[i]);
			}
		}
		else {
			for (int i = 0; i < n; i++) {
				outer.addPoint(xpoints[i], ypoints[i]);
			}
		}
	}

}