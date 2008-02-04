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

public class PotentialContour {

	private Atom probe;
	private int width, height, cellSize = 2;
	private int nx, ny;
	private double[] contour;
	private boolean charged;
	private int increment;
	private double distance, xdis, ydis, x0, y0;
	private double sigmaSquare, sr2, sr6, sr12;
	private double constant;

	public void setProbe(Atom probe, int width, int height) {
		this.probe = probe;
		if (width == this.width && height == this.height)
			return;
		this.width = width;
		this.height = height;
		nx = (int) ((float) width / (float) cellSize);
		ny = (int) ((float) height / (float) cellSize);
		contour = new double[nx * ny];
	}

	public int getCellSize() {
		return cellSize;
	}

	public void setConstant(double coulombConstant, double dielectricConstant) {
		constant = coulombConstant / dielectricConstant;
	}

	public double[] getContour(int numberOfAtoms, Atom[] atom, int boundaryType) {

		if (nx * ny <= 0)
			return null;
		if (numberOfAtoms <= 0)
			return null;

		increment = 0;
		charged = Math.abs(probe.charge) > Particle.ZERO;

		for (int i = 0; i < ny; i++) {
			for (int j = 0; j < nx; j++) {

				contour[increment] = 0.0;
				y0 = (i + 0.5) * cellSize;
				x0 = (j + 0.5) * cellSize;

				for (int m = 0; m < numberOfAtoms; m++) {
					ydis = y0 - atom[m].ry;
					xdis = x0 - atom[m].rx;
					switch (boundaryType) {
					case Boundary.PBC_ID:
						if (xdis > width * 0.5)
							xdis -= width;
						if (xdis <= -width * 0.5)
							xdis += width;
						if (ydis > height * 0.5)
							ydis -= height;
						if (ydis <= -height * 0.5)
							ydis += height;
						break;
					case RectangularBoundary.XRYPBC_ID:
						if (ydis > height * 0.5)
							ydis -= height;
						if (ydis <= -height * 0.5)
							ydis += height;
						break;
					case RectangularBoundary.XPYRBC_ID:
						if (xdis > width * 0.5)
							xdis -= width;
						if (xdis <= -width * 0.5)
							xdis += width;
						break;
					}
					distance = xdis * xdis + ydis * ydis;
					if (distance > probe.sigma * atom[m].sigma) {
						sigmaSquare = probe.sigma * atom[m].sigma;
						sr2 = sigmaSquare / distance;
						sr6 = sr2 * sr2 * sr2;
						sr12 = sr6 * sr6;
						contour[increment] += 0.5 * (probe.epsilon + atom[m].epsilon) * (sr12 - sr6);
						if (charged && Math.abs(atom[m].charge) > Particle.ZERO)
							contour[increment] += atom[m].charge * probe.charge / Math.sqrt(distance) * constant;
					}
				}

				increment++;

			}

		}

		return contour;

	}

}