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
import java.awt.Graphics;

public class ElectricForceField {

	private final Object lock = new Object();

	private int width, height, cellSize = 10, nx, ny;
	private float[] fx, fy;
	private double x1, y1, x2, y2, wingx, wingy, cosx, sinx; // reuse to reduce heap allocation while painting
	private int m;
	private double distance;

	public void setWindow(int width, int height) {
		if (width == this.width && height == this.height)
			return;
		this.width = width;
		this.height = height;
		nx = (int) ((float) width / (float) cellSize);
		ny = (int) ((float) height / (float) cellSize);
		fx = new float[nx * ny];
		fy = new float[nx * ny];
	}

	public int getCellSize() {
		synchronized (lock) {
			return cellSize;
		}
	}

	public void setCellSize(int i) {
		if (i == cellSize)
			return;
		synchronized (lock) {
			cellSize = i;
			nx = (int) ((float) width / (float) cellSize);
			ny = (int) ((float) height / (float) cellSize);
			fx = new float[nx * ny];
			fy = new float[nx * ny];
		}
	}

	public void computeForceGrid(MolecularModel model) {

		if (nx * ny <= 0)
			return;
		if (model.numberOfAtoms <= 0)
			return;

		synchronized (lock) {

			m = 0;

			for (int i = 0; i < ny; i++) {
				for (int j = 0; j < nx; j++) {

					fx[m] = 0;
					fy[m] = 0;

					y1 = (i + 0.5) * cellSize;
					x1 = (j + 0.5) * cellSize;

					for (int k = 0; k < model.numberOfAtoms; k++) {
						y2 = y1 - model.atom[k].ry;
						x2 = x1 - model.atom[k].rx;
						switch (model.boundary.type) {
						case Boundary.PBC_ID:
							if (x2 > width * 0.5)
								x2 -= width;
							if (x2 <= -width * 0.5)
								x2 += width;
							if (y2 > height * 0.5)
								y2 -= height;
							if (y2 <= -height * 0.5)
								y2 += height;
							break;
						case RectangularBoundary.XRYPBC_ID:
							if (y2 > height * 0.5)
								y2 -= height;
							if (y2 <= -height * 0.5)
								y2 += height;
							break;
						case RectangularBoundary.XPYRBC_ID:
							if (x2 > width * 0.5)
								x2 -= width;
							if (x2 <= -width * 0.5)
								x2 += width;
							break;
						}
						distance = x2 * x2 + y2 * y2;
						if (distance > 0.25 * model.atom[k].sigma * model.atom[k].sigma) {
							distance = model.atom[k].charge / (Math.sqrt(distance) * distance);
							fx[m] += distance * x2;
							fy[m] += distance * y2;
						}
					}

					ElectricField field = (ElectricField) model.getNonLocalField(ElectricField.class.getName());
					if (field != null) {
						switch (field.getOrientation()) {
						case VectorField.NORTH:
						case VectorField.SOUTH:
							fy[m] += field.getForce(model.getModelTime());
							break;
						case VectorField.EAST:
						case VectorField.WEST:
							fx[m] += field.getForce(model.getModelTime());
							break;
						}
					}

					m++;

				}

			}

		}

	}

	public void render(Graphics g, Color c) {
		g.setColor(c);
		synchronized (lock) {
			for (int i = 0; i < ny; i++) {
				for (int j = 0; j < nx; j++) {
					m = i * nx + j;
					x1 = 1.0 / Math.sqrt(fx[m] * fx[m] + fy[m] * fy[m]);
					cosx = fx[m] * x1;
					sinx = fy[m] * x1;
					x1 = (j + 0.5 * (1.0 - cosx)) * cellSize;
					y1 = (i + 0.5 * (1.0 - sinx)) * cellSize;
					x2 = (j + 0.5 * (1.0 + cosx)) * cellSize;
					y2 = (i + 0.5 * (1.0 + sinx)) * cellSize;
					g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
					wingx = 5 * (cosx * Particle.COS45 + sinx * Particle.SIN45);
					wingy = 5 * (sinx * Particle.COS45 - cosx * Particle.SIN45);
					g.drawLine((int) x2, (int) y2, (int) (x2 - wingx), (int) (y2 - wingy));
					wingx = 5 * (cosx * Particle.COS45 - sinx * Particle.SIN45);
					wingy = 5 * (sinx * Particle.COS45 + cosx * Particle.SIN45);
					g.drawLine((int) x2, (int) y2, (int) (x2 - wingx), (int) (y2 - wingy));
				}
			}
		}
	}

}