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

import java.awt.geom.GeneralPath;

class GradientVectorPath {

	public final static String FLUX12 = "12 flux lines";
	public final static String FLUX18 = "18 flux lines";
	public final static String FLUX24 = "24 flux lines";
	public final static String FLUX30 = "30 flux lines";
	public final static String FLUX36 = "36 flux lines";

	private int flux = 36;
	private Atom probe;
	private int width, height;
	private int numberOfChargedAtoms = 0;
	private int increment = 5;

	public int getNumberOfChargedAtoms() {
		return numberOfChargedAtoms;
	}

	public void setNumberOfFluxLines(int number) {
		flux = number;
	}

	public int getNumberOfFluxLines() {
		return flux;
	}

	public void setIncrement(int increment) {
		this.increment = increment;
	}

	public int getIncrement() {
		return increment;
	}

	public void setProbe(Atom probe, int width, int height) {
		this.probe = probe;
		this.width = width;
		this.height = height;
	}

	public GeneralPath[][] getVDWGradientVectorPath(int numberOfAtoms, Atom[] atom, int boundaryType) {

		GeneralPath[][] path = new GeneralPath[flux][numberOfAtoms];
		double delta = 360.0 / flux;
		float xVector, yVector, radius;
		double distance;
		double xdis = 1.0, ydis = 1.0;
		double sigmaSquare;
		double sr2, sr6, sr12, wij, fxi, fyi;
		double temp;

		for (int m = 0; m < numberOfAtoms; m++) {
			for (int i = 0; i < flux; i++) {
				path[i][m] = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);
				radius = (float) (0.5 * atom[m].sigma);
				xVector = (float) (atom[m].rx + radius * Math.cos(Math.toRadians(delta * i)));
				yVector = (float) (atom[m].ry + radius * Math.sin(Math.toRadians(delta * i)));
				path[i][m].moveTo(xVector, yVector);
				do {
					radius += increment;
					fxi = 0.0;
					fyi = 0.0;
					for (int n = 0; n < numberOfAtoms; n++) {
						xdis = xVector - atom[n].rx;
						ydis = yVector - atom[n].ry;
						if (boundaryType == Boundary.PBC_ID) {
							if (xdis > width * 0.5)
								xdis -= width;
							if (xdis <= -(double) width * 0.5)
								xdis += width;
							if (ydis > height * 0.5)
								ydis -= height;
							if (ydis <= -(double) height * 0.5)
								ydis += height;
						}
						distance = xdis * xdis + ydis * ydis;
						sigmaSquare = probe.sigma * atom[n].sigma;
						sr2 = sigmaSquare / distance;
						sr6 = sr2 * sr2 * sr2;
						sr12 = sr6 * sr6;
						wij = 0.5 * (probe.epsilon + atom[n].epsilon) * (2.0 * sr12 - sr6);
						fxi += wij / distance * xdis;
						fyi += wij / distance * ydis;
					}
					temp = 1.0 / Math.sqrt(fxi * fxi + fyi * fyi);
					xVector = (float) (atom[m].rx - radius * fxi * temp);
					yVector = (float) (atom[m].ry - radius * fyi * temp);
					path[i][m].lineTo(xVector, yVector);
				} while (xVector < width && xVector > 0 && yVector > 0 && yVector < height);
			}
		}

		return path;

	}

	public GeneralPath[][] getCoulombGradientVectorPath(int numberOfAtoms, Atom[] atom, int boundaryType) {

		if (probe.charge == 0)
			return null;
		numberOfChargedAtoms = 0;
		for (int i = 0; i < numberOfAtoms; i++) {
			if (atom[i].charge != 0)
				numberOfChargedAtoms++;
		}
		if (numberOfChargedAtoms == 0)
			return null;

		GeneralPath[][] path = new GeneralPath[flux][numberOfChargedAtoms];
		double delta = 360.0 / flux;
		float xVector, yVector, radius;
		double distance;
		double xdis = 1.0, ydis = 1.0;
		double coul, fxi, fyi;
		int index = 0;
		double temp;

		for (int m = 0; m < numberOfAtoms; m++) {
			if (atom[m].charge != 0) {
				for (int i = 0; i < flux; i++) {
					path[i][index] = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);
					radius = (float) (0.5 * atom[m].sigma);
					xVector = (float) (atom[m].rx + radius * Math.cos(Math.toRadians(delta * i)));
					yVector = (float) (atom[m].ry + radius * Math.sin(Math.toRadians(delta * i)));
					path[i][index].moveTo(xVector, yVector);
					do {
						radius += increment;
						fxi = 0.0;
						fyi = 0.0;
						for (int n = 0; n < numberOfAtoms; n++) {
							if (atom[n].charge != 0) {
								xdis = xVector - atom[n].rx;
								ydis = yVector - atom[n].ry;
								if (boundaryType == Boundary.PBC_ID) {
									if (xdis > width * 0.5)
										xdis -= width;
									if (xdis <= -(double) width * 0.5)
										xdis += width;
									if (ydis > height * 0.5)
										ydis -= height;
									if (ydis <= -(double) height * 0.5)
										ydis += height;
								}
								distance = xdis * xdis + ydis * ydis;
								coul = atom[n].charge / Math.sqrt(distance);
								fxi += coul / distance * xdis;
								fyi += coul / distance * ydis;
							}
						}
						temp = 1.0 / Math.sqrt(fxi * fxi + fyi * fyi);
						xVector = (float) (atom[m].rx - radius * fxi * temp);
						yVector = (float) (atom[m].ry - radius * fyi * temp);
						path[i][index].lineTo(xVector, yVector);
					} while (xVector < width && xVector > 0 && yVector > 0 && yVector < height);
				}
				index++;
			}
		}

		return path;

	}

}