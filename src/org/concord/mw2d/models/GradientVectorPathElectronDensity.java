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

class GradientVectorPathElectronDensity {

	Atom probe;
	int width, height;
	public static int flux = 36;
	public static int increment = 8;

	public void setNumberOfFluxLines(int number) {
		flux = number;
	}

	public int getNumberOfFluxLines() {
		return flux;
	}

	public static void setIncrement(int incr) {
		increment = incr;
	}

	public static int getIncrement() {
		return increment;
	}

	public void setProbe(Atom probe, int width, int height) {
		this.probe = probe;
		this.width = width;
		this.height = height;
	}

	public GeneralPath[][] getGradientVectorPath(int numberOfAtoms, Atom[] atom, String boundary) {
		GeneralPath[][] path = new GeneralPath[flux][numberOfAtoms];
		double delta = 360.0 / flux;
		int xVector, yVector, radius;
		double distance;
		double xdis = 1.0, ydis = 1.0;
		double sigmaSquare, sr2;
		double wij, fxi, fyi;

		for (int m = 0; m < numberOfAtoms; m++) {
			for (int i = 0; i < flux; i++) {
				path[i][m] = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);
				radius = (int) (0.5 * atom[m].sigma);
				xVector = (int) (atom[m].rx + radius * Math.cos(Math.toRadians(delta * i)));
				yVector = (int) (atom[m].ry + radius * Math.sin(Math.toRadians(delta * i)));
				path[i][m].moveTo(xVector, yVector);
				do {
					radius += increment;
					fxi = 0.0;
					fyi = 0.0;
					for (int n = 0; n < numberOfAtoms; n++) {
						xdis = xVector - atom[n].rx;
						ydis = yVector - atom[n].ry;
						if (boundary.equals("PBC")) {
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
						sigmaSquare = atom[n].sigma * atom[n].sigma;
						sr2 = distance / sigmaSquare;
						wij = -2.0 * Math.sqrt(distance) / sigmaSquare * Math.exp(-sr2);
						fxi += wij / distance * xdis;
						fyi += wij / distance * ydis;
					}
					xVector = (int) (atom[m].rx - radius * fxi / Math.sqrt(fxi * fxi + fyi * fyi));
					yVector = (int) (atom[m].ry - radius * fyi / Math.sqrt(fxi * fxi + fyi * fyi));
					path[i][m].lineTo(xVector, yVector);
				} while (xVector < width && xVector > 0 && yVector > 0 && yVector < height);
			}
		}

		return path;

	}

}