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

import org.concord.modeler.util.FloatQueue;

public class StructureFactor {

	public final static byte LINEAR_SCALING = 101;
	public final static byte LOG_SCALING = 102;
	public final static byte X_RAY = 0;
	public final static byte NEUTRON = 1;

	private final static double TWOPI = Math.PI + Math.PI;

	private float reciprocalUnit = 0.002f;
	private int width, height;
	private int xmin, ymin, xmax, ymax;
	private int lod = LINEAR_SCALING;

	/* Debye-Waller factors */
	private float[] bFactor;

	private float[] f;
	private int[] pixels;

	public StructureFactor(int halfWidth, int halfHeight) {
		xmin = -halfWidth;
		xmax = halfWidth;
		ymin = -halfHeight;
		ymax = halfHeight;
		width = halfWidth + halfWidth + 1;
		height = halfHeight + halfHeight + 1;
		f = new float[width * height];
	}

	public void setLevelOfDetails(int i) {
		lod = i;
	}

	public int getLevelOfDetails() {
		return lod;
	}

	public void setZooming(int i) {
		if (i <= 0)
			throw new IllegalArgumentException("zooming ratio cannot be zero or negative.");
		reciprocalUnit = 0.0002f * i;
	}

	public int getZooming() {
		return Math.round(reciprocalUnit * 5000);
	}

	private static float getMeanDisplacement(FloatQueue q) {
		if (q == null)
			throw new IllegalStateException("the recorder must be turned on to do this calculation.");
		int n = Math.min(q.getPointer(), q.getLength());
		if (n < 2)
			return 0;
		float tx = 0;
		float[] x = (float[]) q.getData();
		for (int i = 1; i < n; i++) {
			tx += Math.abs(x[i] - x[i - 1]);
		}
		return tx / (n - 1);
	}

	public void compute(AtomicModel model, final int type) {

		int m = model.getNumberOfAtoms();

		// computer the Debye-Waller factors (B-factors)

		if (type == X_RAY) {
			if (bFactor == null || bFactor.length < m)
				bFactor = new float[m];
			if (model.getTapePointer() > 0) {
				float dx, dy;
				for (int i = 0; i < m; i++) {
					dx = getMeanDisplacement(model.atom[i].rQ.getQueue1());
					dy = getMeanDisplacement(model.atom[i].rQ.getQueue2());
					bFactor[i] = (0.1f * (dx * dx + dy * dy)) * reciprocalUnit;
				}
			}
			else {
				for (int i = 0; i < m; i++) {
					bFactor[i] = 0.1f * reciprocalUnit;
				}
			}
		}

		// compute the structure factors

		double realSum;
		double imagSum;
		double prefactor;
		double constant = TWOPI * reciprocalUnit;
		double exponent;
		double rx, ry;
		int incr = 0;
		boolean hasMovie = model.getTapePointer() > 0;
		for (int j = xmin; j <= xmax; j++) {
			for (int i = ymin; i <= ymax; i++) {
				realSum = 0.0;
				imagSum = 0.0;
				for (int n = 0; n < m; n++) {
					if (hasMovie) {
						rx = model.atom[n].getRxRyQueue().getQueue1().getAverage();
						ry = model.atom[n].getRxRyQueue().getQueue2().getAverage();
					}
					else {
						rx = model.atom[n].rx;
						ry = model.atom[n].ry;
					}
					// NOTE!!! the reason there is a minus before i*ry is because our molecular view's coordinate
					// system points downward. In order for the X-ray image to be positioned consistently with
					// the molecular structure, the wave vector must also assumes a downward direction to be
					// positive.
					exponent = (j * rx - i * ry) * constant;
					if (type == X_RAY) {
						prefactor = model.atom[n].getSigma() * Math.exp(-(i * i + j * j) * bFactor[n]);
						realSum += Math.cos(exponent) * prefactor;
						imagSum += Math.sin(exponent) * prefactor;
					}
					else if (type == NEUTRON) {
						realSum += Math.cos(exponent);
						imagSum += Math.sin(exponent);
					}
				}
				f[incr++] = (float) (realSum * realSum + imagSum * imagSum);
			}
		}

	}

	/* create the diffraction image pixels */
	public int[] getDiffractionImage() {

		if (pixels == null)
			pixels = new int[width * height];

		float min = Float.MAX_VALUE;
		float max = -min;
		int n = width * height;
		for (int k = 0; k < n; k++) {
			min = Math.min(min, f[k]);
			max = Math.max(max, f[k]);
		}

		n = 0;
		int c;
		float delta;
		switch (lod) {
		case LOG_SCALING:
			delta = (float) Math.log(max - min + 1) / 255.0f;
			for (int j = 0; j < width; j++) {
				for (int i = 0; i < height; i++) {
					c = (int) (Math.log(f[n] - min + 1) / delta);
					pixels[n++] = 0xff << 24 | c << 16 | c << 8 | c;
				}
			}
			break;
		case LINEAR_SCALING:
			delta = (max - min) / 255.0f;
			for (int j = 0; j < width; j++) {
				for (int i = 0; i < height; i++) {
					c = (int) ((f[n] - min) / delta);
					pixels[n++] = 0xff << 24 | c << 16 | c << 8 | c;
				}
			}
			break;
		}

		return pixels;

	}

}