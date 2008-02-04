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
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import org.concord.mw2d.MDView;
import org.concord.mw2d.ViewAttribute;

/**
 * GRID is the method to relate micro and macro. The basical idea is to divide the simulation box into a lot of small
 * grid cells. When the molecular dynamics is performed, count the number of times atoms visit each cell and accumulate
 * the data of these atoms. The statistic averages of these micro flow data may reveal the influence of certain external
 * conditions on micro motion. Note: The GRID method is more useful in gas and liquid simulation. For solid state, since
 * most of the time the atoms move little, GRID analysis may not give informative results.
 */

public class Grid implements ComponentListener {

	public final static byte ATOMIC = 0;
	public final static byte DENSITY = 1;
	public final static byte MASS = 2;
	public final static byte CHARGE = 3;
	public final static byte TEMPERATURE = 4;
	public final static byte VELOCITY = 5;
	public final static byte FORCE = 6;

	private final static byte NPROP = 9; // 9 because of vx, vy, fx and fy
	private static Rectangle2D.Float tempRect;
	private static Line2D.Double tempLine;

	private int n[] = { 50, 20 };
	private float[][][] gridData;
	private int steps;
	private Rectangle2D bound;
	private float x0, y0, dx, dy;
	private double wingx, wingy, arrowx, arrowy;

	Grid(Rectangle2D bound) {
		init(bound);
	}

	Grid(Rectangle2D bound, int[] n) {
		this.n = n;
		this.bound = bound;
		x0 = (float) bound.getX();
		y0 = (float) bound.getY();
		dx = (float) bound.getWidth() / n[0];
		dy = (float) bound.getHeight() / n[1];
		gridData = new float[n[0]][n[1]][NPROP];
		zeroGridData();
	}

	/** set the grid background to be the input rectangle */
	public void setBound(Rectangle2D bound) {
		this.bound = bound;
		x0 = (float) bound.getX();
		y0 = (float) bound.getY();
		dx = (float) bound.getWidth() / n[0];
		dy = (float) bound.getHeight() / n[1];
		zeroGridData();
	}

	/** set the x,y divisions of the grid */
	public void setDivisions(int[] n) {
		this.n = n;
		dx = (float) bound.getWidth() / n[0];
		dy = (float) bound.getHeight() / n[1];
		gridData = new float[n[0]][n[1]][NPROP];
		zeroGridData();
	}

	public int[] getDivisions() {
		return n;
	}

	public float getx0() {
		return x0;
	}

	public float gety0() {
		return y0;
	}

	public float getdx() {
		return dx;
	}

	public float getdy() {
		return dy;
	}

	/**
	 * get grid data.
	 * 
	 * @return statistic data of all grid cell.
	 */
	public float[][][] getGridData() {
		return gridData;
	}

	public void setGridData(float[][][] f) {
		gridData = f;
	}

	/**
	 * get number of cells.
	 * 
	 * @return the number of grid cells
	 */
	public int getNumberOfCells() {
		return n[0] * n[1];
	}

	/** get number of steps that GRID has been called. */
	public int getSteps() {
		return steps;
	}

	public void setSteps(int steps) {
		this.steps = steps;
	}

	/**
	 * divide the simulation box into grid cells, and initialize each cell. The initial kinetic energy value is set to
	 * be 1.0 because its log will be used to obtain the contrasted distribution map.
	 */
	private void zeroGridData() {
		steps = 0;
		for (int i = 0; i < n[1]; i++) {
			for (int j = 0; j < n[0]; j++) {
				Arrays.fill(gridData[j][i], 0.0f);
				gridData[j][i][3] = 0.001f; // kinetic energy
			}
		}
	}

	/** accumulate atomic data in each cell. */
	void cellAccumulate(int numberOfAtoms, Atom[] atom) {
		steps++;
		int ix, iy;
		float xd = 1.0f / dx;
		float yd = 1.0f / dy;
		for (int i = 0; i < numberOfAtoms; i++) {
			ix = (int) (((float) atom[i].rx - x0) * xd);
			iy = (int) (((float) atom[i].ry - y0) * yd);
			if (ix > n[0] - 1)
				ix = n[0] - 1;
			if (iy > n[1] - 1)
				iy = n[1] - 1;
			if (ix < 0)
				ix = 0;
			if (iy < 0)
				iy = 0;
			gridData[ix][iy][0]++;
			gridData[ix][iy][1] += atom[i].mass;
			gridData[ix][iy][2] += atom[i].charge;
			gridData[ix][iy][3] += atom[i].mass * (atom[i].vx * atom[i].vx + atom[i].vy * atom[i].vy);
			gridData[ix][iy][4] += atom[i].vx;
			gridData[ix][iy][5] += atom[i].vy;
			gridData[ix][iy][6] += atom[i].fx * atom[i].fx + atom[i].fy * atom[i].fy;
			gridData[ix][iy][7] += atom[i].fx;
			gridData[ix][iy][8] += atom[i].fy;
		}
	}

	/** search the minima of grid cell properties */
	private float[] getMin() {
		float[] min = new float[NPROP];
		boolean init = true;
		for (int j = 0; j < n[1]; j++) {
			for (int i = 0; i < n[0]; i++) {
				if (init) {
					for (int n = 0; n < NPROP; n++)
						min[n] = gridData[i][j][n];
					init = false;
				}
				else {
					for (int n = 0; n < NPROP; n++) {
						if (gridData[i][j][n] < min[n])
							min[n] = gridData[i][j][n];
					}
				}
			}
		}
		return min;
	}

	/** search the maxima of grid cell properties */
	private float[] getMax() {
		float[] max = new float[NPROP];
		boolean init = true;
		for (int j = 0; j < n[1]; j++) {
			for (int i = 0; i < n[0]; i++) {
				if (init) {
					for (int n = 0; n < NPROP; n++)
						max[n] = gridData[i][j][n];
					init = false;
				}
				else {
					for (int n = 0; n < NPROP; n++) {
						if (gridData[i][j][n] > max[n])
							max[n] = gridData[i][j][n];
					}
				}
			}
		}
		return max;
	}

	private void init(Rectangle2D bound) {
		this.bound = bound;
		x0 = (float) bound.getX();
		y0 = (float) bound.getY();
		float w = (float) bound.getWidth();
		float h = (float) bound.getHeight();
		n[1] = (int) (h / w * n[0]);
		dx = w / n[0];
		dy = h / n[1];
		gridData = new float[n[0]][n[1]][NPROP];
		zeroGridData();
	}

	/** reset the grid as component is resized. */
	public void componentResized(ComponentEvent e) {
		Object source = e.getSource();
		if (source instanceof MDView) {
			MDView view = (MDView) source;
			RectangularBoundary bound = view.getBoundary();
			if (bound.getType() != RectangularBoundary.DBC_ID) {
				init(view.getBoundary());
			}
			else {
				init(view.getBounds());
			}
		}
	}

	/** do nothing */
	public void componentHidden(ComponentEvent e) {
	}

	/** do nothing */
	public void componentMoved(ComponentEvent e) {
	}

	/** do nothing */
	public void componentShown(ComponentEvent e) {
	}

	public void paint(byte gridMode, Graphics2D g) {
		if (gridMode < ATOMIC)
			return;
		float[] gmin = getMin();
		float[] gmax = getMax();
		int ncolor = 0;
		float scale = 1.0f;
		float magVec = Math.min(dx, dy);
		switch (gridMode) {
		case DENSITY:
			scale = 255.0f / (gmax[0] - gmin[0]);
			break;
		case MASS:
			scale = 255.0f / (gmax[1] - gmin[1]);
			break;
		case CHARGE:
			if (Math.abs(gmax[2] - gmin[2]) > Particle.ZERO) {
				scale = 255.0f / (gmax[2] - gmin[2]);
			}
			else {
				scale = 0.0f;
			}
			break;
		case TEMPERATURE:
			scale = 255.0f / (float) (Math.log(gmax[3]) - Math.log(gmin[3]));
			break;
		case VELOCITY:
			g.setStroke(ViewAttribute.THIN);
			scale = 0.707f
					* magVec
					/ Math.max(Math.max(Math.abs(gmax[4]), Math.abs(gmax[5])), Math.max(Math.abs(gmin[4]), Math
							.abs(gmin[5])));
			break;
		case FORCE:
			g.setStroke(ViewAttribute.THIN);
			scale = 0.707f
					* magVec
					/ Math.max(Math.max(Math.abs(gmax[7]), Math.abs(gmax[8])), Math.max(Math.abs(gmin[7]), Math
							.abs(gmin[8])));
			break;
		}
		double arrowLength = (magVec * 0.5 > 4.0) ? 4.0 : magVec * 0.5;
		double xc, yc;
		double vxc, vyc;
		double tempInv;
		if (tempLine == null)
			tempLine = new Line2D.Double();
		if (tempRect == null)
			tempRect = new Rectangle2D.Float();
		tempRect.setRect(x0, y0, n[0] * dx, n[1] * dy);
		g.setColor(Color.black);
		g.fill(tempRect);
		for (int jg = 0; jg < n[1]; jg++) {
			for (int ig = 0; ig < n[0]; ig++) {
				if (Math.round(gridData[ig][jg][0]) > 0) {
					switch (gridMode) {
					case DENSITY:
						ncolor = Math.round(scale * (gridData[ig][jg][0] - gmin[0]));
						break;
					case MASS:
						ncolor = Math.round(scale * (gridData[ig][jg][1] - gmin[1]));
						break;
					case TEMPERATURE:
						ncolor = Math.round(scale * (float) (Math.log(gridData[ig][jg][3]) - Math.log(gmin[3])));
						break;
					}
					ncolor = ncolor > 255 ? 255 : ncolor;
					ncolor = ncolor < 0 ? 0 : ncolor;
					switch (gridMode) {
					case DENSITY: // population scalar
						g.setColor(new Color(ncolor, ncolor, ncolor));
						tempRect.setRect(x0 + ig * dx, y0 + jg * dy, dx, dy);
						g.fill(tempRect);
						break;
					case MASS: // mass scalar
						g.setColor(new Color(ncolor, 0, ncolor));
						tempRect.setRect(x0 + ig * dx, y0 + jg * dy, dx, dy);
						g.fill(tempRect);
						break;
					case CHARGE: // charge scalar
						float gcharge = gridData[ig][jg][2];
						if (Math.abs(gcharge) > Particle.ZERO) {
							g.setColor(setGridChargeColor(gcharge, gmin[2], gmax[2]));
							tempRect.setRect(x0 + ig * dx, y0 + jg * dy, dx, dy);
							g.fill(tempRect);
						}
						break;
					case TEMPERATURE: // kinetic energy scalar
						g.setColor(new Color(ncolor, 0, 0));
						tempRect.setRect(x0 + ig * dx, y0 + jg * dy, dx, dy);
						g.fill(tempRect);
						break;
					case VELOCITY: // velocity vector
						vxc = gridData[ig][jg][4] * scale;
						vyc = gridData[ig][jg][5] * scale;
						if (Math.abs(vxc) < 1.0 && Math.abs(vyc) < 1.0)
							break;
						g.setColor(Color.blue);
						xc = x0 + ig * dx + dx * 0.5;
						yc = y0 + jg * dy + dy * 0.5;
						tempLine.setLine(xc, yc, xc + vxc, yc + vyc);
						g.draw(tempLine);
						tempInv = arrowLength / Math.sqrt(vxc * vxc + vyc * vyc);
						arrowx = vxc * tempInv;
						arrowy = vyc * tempInv;
						wingx = arrowx * Particle.SIN60 + arrowy * Particle.COS60;
						wingy = arrowy * Particle.SIN60 - arrowx * Particle.COS60;
						tempLine.setLine(xc + vxc, yc + vyc, xc + vxc - wingx, yc + vyc - wingy);
						g.draw(tempLine);
						wingx = arrowx * Particle.SIN60 - arrowy * Particle.COS60;
						wingy = arrowy * Particle.SIN60 + arrowx * Particle.COS60;
						tempLine.setLine(xc + vxc, yc + vyc, xc + vxc - wingx, yc + vyc - wingy);
						g.draw(tempLine);
						break;
					case FORCE: // stress vector
						vxc = gridData[ig][jg][7] * scale;
						vyc = gridData[ig][jg][8] * scale;
						if (Math.abs(vxc) < 1.0 && Math.abs(vyc) < 1.0)
							break;
						g.setColor(Color.orange);
						xc = x0 + ig * dx + dx * 0.5;
						yc = y0 + jg * dy + dy * 0.5;
						tempLine.setLine(xc, yc, xc + vxc, yc + vyc);
						g.draw(tempLine);
						tempInv = arrowLength / Math.sqrt(vxc * vxc + vyc * vyc);
						arrowx = vxc * tempInv;
						arrowy = vyc * tempInv;
						wingx = arrowx * Particle.SIN60 + arrowy * Particle.COS60;
						wingy = arrowy * Particle.SIN60 - arrowx * Particle.COS60;
						tempLine.setLine(xc + vxc, yc + vyc, xc + vxc - wingx, yc + vyc - wingy);
						g.draw(tempLine);
						wingx = arrowx * Particle.SIN60 - arrowy * Particle.COS60;
						wingy = arrowy * Particle.SIN60 + arrowx * Particle.COS60;
						tempLine.setLine(xc + vxc, yc + vyc, xc + vxc - wingx, yc + vyc - wingy);
						g.draw(tempLine);
						break;
					}
				}
			}
		}

	}

	/*
	 * when showing charge distribution on grid, positive and negative charges have to be displayed in different colors.
	 */
	private static Color setGridChargeColor(float q, float qmin, float qmax) {
		if (Math.round(q) == 0)
			return Color.black;
		// pos. blue, neg. green, neu. black
		float scale = 1.0f;
		int ncolor;
		int isign = Math.round(qmax * qmin);
		if (isign > 0) {
			scale = 255.0f / (qmax - qmin); // all pos. or neg.
			ncolor = Math.round(scale * (q - qmin));
		}
		else if (isign < 0) {
			scale = q > 0 ? 255.0f / qmax : 255.0f / qmin; // some pos., some neg.
			ncolor = Math.round(scale * q);
		}
		else {
			if (Math.round(qmax) > 0) {
				scale = 255.0f / qmax; // some pos., some neu., no neg.
			}
			else if (Math.round(qmin) < 0) {
				scale = 255.0f / qmin; // some neg., some neu., no pos.
			}
			ncolor = Math.round(scale * q);
		}
		ncolor = ncolor > 255 ? 255 : ncolor;
		ncolor = ncolor < 0 ? 0 : ncolor;
		return q > 0 ? new Color(0, 0, ncolor) : new Color(0, ncolor, 0);
	}

}