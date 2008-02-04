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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Iterator;

import org.concord.modeler.draw.FillMode;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.ViewAttribute;

public class CurvedSurface extends CurvedRibbon {

	public final static int NONE = 3551;
	public final static int CHARGE = 3552;

	private int colorMode = NONE;

	/** converts a molecule into a smart surface */
	public CurvedSurface(Molecule m) {
		super(m);
		RadialBondCollection rbc = model.getBonds();
		RadialBond rb;
		synchronized (rbc) {
			for (Iterator it = rbc.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				if (contains(rb.getAtom1()) && contains(rb.getAtom2())) {
					rb.setSolid(true);
				}
			}
		}
	}

	/**
	 * construct a smart surface whose vertices are given by those of the passed polygon
	 */
	public CurvedSurface(Polygon p, MolecularModel m2) {
		super(p, m2, true);
	}

	public void setColorMode(int colorMode) {
		this.colorMode = colorMode;
	}

	public int getColorMode() {
		return colorMode;
	}

	public void render(Graphics2D g) {

		Color oldColor = g.getColor();
		Stroke oldStroke = g.getStroke();

		if (path != null) {

			createClosedSpline();

			if (!model.view.getShowSites()) {

				boolean showField = false;
				if (colorMode == CHARGE) {
					Atom a;
					synchronized (getSynchronizedLock()) {
						for (Iterator it = iterator(); it.hasNext();) {
							a = (Atom) it.next();
							if (Math.abs(a.getCharge()) > Particle.ZERO) {
								showField = true;
								break;
							}
						}
					}
				}
				if (showField) {
					fieldColoring(g);
				}
				else {
					if (fillMode instanceof FillMode.PatternFill) {
						g.setPaint(((FillMode.PatternFill) fillMode).getPaint());
						g.fill(path);
					}
					else if (fillMode instanceof FillMode.GradientFill) {
					}
					else if (fillMode instanceof FillMode.ImageFill) {
					}
					else {
						g.setColor(isMarked() ? model.view.getMarkColor() : getBackground());
						g.fill(path);
					}
				}
				g.setColor(model.view.contrastBackground());
				g.setStroke(stroke);
				g.draw(path);
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

	private void fieldColoring(Graphics2D g) {
		Shape oldClip = g.getClip();
		Color oldColor = g.getColor();
		g.setClip(path);
		Rectangle bounds = path.getBounds();
		int x0 = bounds.x;
		int y0 = bounds.y;
		int x1 = x0 + bounds.width;
		int y1 = y0 + bounds.height;
		for (int x = x0; x < x1; x += 4) {
			for (int y = y0; y < y1; y += 4) {
				g.setColor(computeField(x + 2, y + 2));
				g.fillRect(x, y, 4, 4);
			}
		}
		g.setClip(oldClip);
		g.setColor(oldColor);
	}

	private Color computeField(int x, int y) {
		Atom a;
		double intensity = 0.0;
		double xij, yij, rij;
		synchronized (getSynchronizedLock()) {
			for (Iterator it = iterator(); it.hasNext();) {
				a = (Atom) it.next();
				if (a.getCharge() > Particle.ZERO) {
					xij = x - a.rx;
					yij = y - a.ry;
					rij = Math.sqrt(xij * xij + yij * yij);
					if (rij < 9)
						return new Color(255, 192, 192);
					intensity += a.getCharge() / rij;
				}
				else if (a.getCharge() < -Particle.ZERO) {
					xij = x - a.rx;
					yij = y - a.ry;
					rij = Math.sqrt(xij * xij + yij * yij);
					if (rij < 9)
						return new Color(192, 192, 255);
					intensity += a.getCharge() / rij;
				}
			}
		}
		int color = Math.abs(intensity * 10000) < 255 ? (int) Math.abs(intensity * 10000) : 255;
		color = color < 192 ? 192 : color;
		if (intensity > 0)
			return new Color(color, 192, 192);
		return new Color(192, 192, color);
	}

	/*
	 * synchronized void createDashed(){ if(selectedPolygon==null) return; selectedPolygon.reset();
	 * if(selectedPath==null) return; selectedPath.reset(); if(size()>beadSize.length) beadSize=new float[size()]; int
	 * i=0; Atom a; for(Iterator it=iterator(); it.hasNext();){ a=(Atom)it.next(); selectedPolygon.addPoint((int)a.rx,
	 * (int)a.ry); beadSize[i]=(float)(a.getSigma()); i++; } enlargePolygon(selectedPolygon);
	 * createSpline(selectedPolygon, selectedPath, SMOOTHNESS, true); }
	 */

	private void createClosedSpline() {
		polygon.reset();
		if (size() > beadSize.length)
			beadSize = new float[size()];
		int i = 0;
		Atom a;
		synchronized (getSynchronizedLock()) {
			for (Iterator it = iterator(); it.hasNext();) {
				a = (Atom) it.next();
				polygon.addPoint((int) a.rx, (int) a.ry);
				beadSize[i++] = (float) (a.getSigma() * 0.6);
			}
		}
		enlargePolygon(polygon);
		createSpline(polygon, path, SMOOTHNESS, true);
	}

	private static void enlargePolygon(Polygon poly) {
		int n = poly.npoints;
		float k = 0, p = 0;
		float x, y;
		if (n > xpoints.length || n > ypoints.length) {
			xpoints = new int[n];
			ypoints = new int[n];
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
				if (poly.ypoints[0] != poly.ypoints[n - 2]) {
					k = -(float) (poly.xpoints[0] - poly.xpoints[n - 2]) / (poly.ypoints[0] - poly.ypoints[n - 2]);
					p = (float) (1.0 / Math.sqrt(1 + k * k));
				}
				else {
					p = 0;
				}
			}
			else {
				if (poly.ypoints[i + 1] != poly.ypoints[i - 1]) {
					k = -(float) (poly.xpoints[i + 1] - poly.xpoints[i - 1])
							/ (poly.ypoints[i + 1] - poly.ypoints[i - 1]);
					p = (float) (1.0 / Math.sqrt(1 + k * k));
				}
				else {
					p = 0;
				}
			}
			if (Math.abs(p) > 0.0) {// when the normal is more horizontal
				x = poly.xpoints[i] - beadSize[i] * p;
				y = poly.ypoints[i] + k * (x - poly.xpoints[i]);
				if (!poly.contains(x, y)) {
					xpoints[i] = (int) x;
					ypoints[i] = (int) y;
				}
				else {
					x = poly.xpoints[i] + beadSize[i] * p;
					y = poly.ypoints[i] + k * (x - poly.xpoints[i]);
					if (!poly.contains(x, y)) {
						xpoints[i] = (int) x;
						ypoints[i] = (int) y;
					}
					else {
						xpoints[i] = poly.xpoints[i];
						ypoints[i] = poly.ypoints[i];
					}
				}
			}
			else {// when the normal is more vertical
				x = poly.xpoints[i];
				y = poly.ypoints[i] + beadSize[i];
				if (!poly.contains(x, y)) {
					xpoints[i] = (int) x;
					ypoints[i] = (int) y;
				}
				else {
					y = (int) (poly.ypoints[i] - beadSize[i]);
					if (!poly.contains(x, y)) {
						xpoints[i] = (int) x;
						ypoints[i] = (int) y;
					}
					else {
						xpoints[i] = poly.xpoints[i];
						ypoints[i] = poly.ypoints[i];
					}
				}
			}
		}
		for (int i = 0; i < n; i++) {
			poly.xpoints[i] = xpoints[i];
			poly.ypoints[i] = ypoints[i];
		}
	}

	public static void shrinkPolygon(Polygon poly, double sigma) {
		int n = poly.npoints;
		if (n > beadSize.length)
			beadSize = new float[n];
		for (int i = 0; i < n; i++) {
			beadSize[i] = (float) sigma;
		}
		shrinkPolygon(poly);
	}

	private static void shrinkPolygon(Polygon poly) {
		int n = poly.npoints;
		float k = 0, p = 0;
		float x, y;
		if (n > xpoints.length || n > ypoints.length) {
			xpoints = new int[n];
			ypoints = new int[n];
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
				if (poly.ypoints[0] != poly.ypoints[n - 2]) {
					k = -(float) (poly.xpoints[0] - poly.xpoints[n - 2]) / (poly.ypoints[0] - poly.ypoints[n - 2]);
					p = (float) (1.0 / Math.sqrt(1 + k * k));
				}
				else {
					p = 0;
				}
			}
			else {
				if (poly.ypoints[i + 1] != poly.ypoints[i - 1]) {
					k = -(float) (poly.xpoints[i + 1] - poly.xpoints[i - 1])
							/ (poly.ypoints[i + 1] - poly.ypoints[i - 1]);
					p = (float) (1.0 / Math.sqrt(1 + k * k));
				}
				else {
					p = 0;
				}
			}
			if (Math.abs(p) > 0.0) {// when the normal is more horizontal
				x = poly.xpoints[i] - beadSize[i] * p;
				y = poly.ypoints[i] + k * (x - poly.xpoints[i]);
				if (poly.contains(x, y)) {
					xpoints[i] = (int) x;
					ypoints[i] = (int) y;
				}
				else {
					x = poly.xpoints[i] + beadSize[i] * p;
					y = poly.ypoints[i] + k * (x - poly.xpoints[i]);
					if (poly.contains(x, y)) {
						xpoints[i] = (int) x;
						ypoints[i] = (int) y;
					}
					else {
						xpoints[i] = poly.xpoints[i];
						ypoints[i] = poly.ypoints[i];
					}
				}
			}
			else {// when the normal is more vertical
				x = poly.xpoints[i];
				y = poly.ypoints[i] + beadSize[i];
				if (poly.contains(x, y)) {
					xpoints[i] = (int) x;
					ypoints[i] = (int) y;
				}
				else {
					y = (int) (poly.ypoints[i] - beadSize[i]);
					if (poly.contains(x, y)) {
						xpoints[i] = (int) x;
						ypoints[i] = (int) y;
					}
					else {
						xpoints[i] = poly.xpoints[i];
						ypoints[i] = poly.ypoints[i];
					}
				}
			}
		}
		for (int i = 0; i < n; i++) {
			poly.xpoints[i] = xpoints[i];
			poly.ypoints[i] = ypoints[i];
		}
	}

}