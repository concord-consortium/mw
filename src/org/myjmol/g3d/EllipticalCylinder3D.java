/*
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.myjmol.g3d;

/**
 * Charles Xie: This doesn't work yet. The problem is that the ellipse will rotate when the system rotates. Right now,
 * it renders only circular cylinders.
 * 
 * Another thing to notice is that the coordinates must be provided in floating numbers, not integers as in Cylinder3D,
 * which is mostly assumed to render sticks. For thin cylinders, there is a problem that the angles (theta and phi)
 * cannot be calculated when the coordinates are less than one.
 * 
 */

class EllipticalCylinder3D {

	private final static int MAX_FIX = 5000;
	private final Graphics3D g3d;
	private final Line3D line3d;
	private short colix;
	private int[] shades;
	private boolean isScreened;
	private float xA, yA, zA;
	private float dxB, dyB, dzB;
	private boolean tEvenDiameter;
	private int a, b;
	private byte endcaps;
	private boolean tEndcapOpen;
	private int xEndcap, yEndcap, zEndcap;
	private int argbEndcap;
	private short colixEndcap;
	private int intensityEndcap;
	private float radius, radius2, cosTheta, cosPhi, sinPhi;
	private boolean notClipped;
	private int rasterCount;
	private float[] tRaster = new float[32];
	private float[] txRaster = new float[32];
	private float[] tyRaster = new float[32];
	private float[] tzRaster = new float[32];
	private int[] xRaster = new int[32];
	private int[] yRaster = new int[32];
	private int[] zRaster = new int[32];
	private int[] fp8IntensityUp = new int[32];
	private int yMin, yMax, xMin, xMax, zXMin, zXMax;

	EllipticalCylinder3D(Graphics3D g3d) {
		this.g3d = g3d;
		this.line3d = g3d.line3d;
	}

	void render(short colix, byte endcaps, int a, int b, float xAf, float yAf, float zAf, float xBf, float yBf,
			float zBf) {

		dxB = xBf - xAf;
		dyB = yBf - yAf;
		if (dxB * dxB + dyB * dyB > 400.0 * g3d.width * g3d.width)
			return;
		dzB = zBf - zAf;

		int r = a + 1;
		int codeMinA = line3d.clipCode((int) (xAf - r), (int) (yAf - r), (int) (zAf - r));
		int codeMaxA = line3d.clipCode((int) (xAf + r), (int) (yAf + r), (int) (zAf + r));
		int codeMinB = line3d.clipCode((int) (xBf - r), (int) (yBf - r), (int) (zBf - r));
		int codeMaxB = line3d.clipCode((int) (xBf + r), (int) (yBf + r), (int) (zBf + r));
		// all bits 0 --> no clipping
		notClipped = ((codeMinA | codeMaxA | codeMinB | codeMaxB) == 0);
		// any two bits same in all cases --> fully clipped
		if ((codeMinA & codeMaxB & codeMaxA & codeMinB) != 0)
			return; // fully clipped;

		if (a <= 1 || b <= 1) {
			line3d.plotLineDelta(g3d.getColixArgb(colix), Graphics3D.isColixTranslucent(colix),
					g3d.getColixArgb(colix), Graphics3D.isColixTranslucent(colix), (int) xAf, (int) yAf, (int) zAf,
					(int) dxB, (int) dyB, (int) dzB, notClipped);
			return;
		}
		this.a = a;
		this.b = b;
		this.xA = xAf;
		this.yA = yAf;
		this.zA = zAf;
		this.shades = g3d.getShades(this.colix = colix);
		this.isScreened = (colix & Graphics3D.TRANSLUCENT_MASK) != 0;
		this.endcaps = endcaps;
		calcArgbEndcap(true);

		generateBaseEllipse();

		if (endcaps == Graphics3D.ENDCAPS_FLAT)
			renderFlatEndcap(true);
		for (int i = rasterCount; --i >= 0;)
			plotRaster(i);

	}

	private void generateBaseEllipse() {
		tEvenDiameter = ((a + b) & 1) == 0;
		radius = a;
		radius2 = radius * radius;
		float mag2d2 = dxB * dxB + dyB * dyB;
		if (mag2d2 == 0) {
			cosTheta = 1;
			cosPhi = 1;
			sinPhi = 0;
		}
		else {
			float mag2d = (float) Math.sqrt(mag2d2);
			float mag3d = (float) Math.sqrt(mag2d2 + dzB * dzB);
			cosTheta = dzB / mag3d;
			cosPhi = dxB / mag2d;
			sinPhi = dyB / mag2d;
		}
		calcRotatedPoint(0f, 0, true);
		calcRotatedPoint(0.5f, 1, true);
		calcRotatedPoint(1f, 2, true);
		rasterCount = 3;
		interpolate(0, 1);
		interpolate(1, 2);
		for (int i = 0; i < rasterCount; i++) {
			xRaster[i] = (int) Math.floor(txRaster[i]);
			yRaster[i] = (int) Math.floor(tyRaster[i]);
			zRaster[i] = (int) Math.floor(tzRaster[i]);
		}
	}

	private void calcRotatedPoint(float t, int i, boolean isPrecision) {
		tRaster[i] = t;
		double tPI = t * Math.PI;
		double xT = Math.sin(tPI) * cosTheta;
		double yT = Math.cos(tPI);
		double xR = radius * (xT * cosPhi - yT * sinPhi);
		double yR = radius * (xT * sinPhi + yT * cosPhi);
		double z2 = radius2 - (xR * xR + yR * yR);
		double zR = (z2 > 0 ? Math.sqrt(z2) : 0);

		if (isPrecision) {
			txRaster[i] = (float) xR;
			tyRaster[i] = (float) yR;
			tzRaster[i] = (float) zR;
		}
		else if (tEvenDiameter) {
			xRaster[i] = (int) (xR - 0.5);
			yRaster[i] = (int) (yR - 0.5);
			zRaster[i] = (int) (zR + 0.5);
		}
		else {
			xRaster[i] = (int) (xR);
			yRaster[i] = (int) (yR);
			zRaster[i] = (int) (zR + 0.5);
		}
		fp8IntensityUp[i] = Shade3D.calcFp8Intensity((float) xR, (float) yR, (float) zR);
	}

	private void interpolate(int iLower, int iUpper) {
		int dx = (int) Math.floor(txRaster[iUpper]) - (int) Math.floor(txRaster[iLower]);
		if (dx < 0)
			dx = -dx;
		float dy = (int) Math.floor(tyRaster[iUpper]) - (int) Math.floor(tyRaster[iLower]);
		if (dy < 0)
			dy = -dy;
		if ((dx + dy) <= 1)
			return;
		float tLower = tRaster[iLower];
		float tUpper = tRaster[iUpper];
		int iMid = allocRaster(true);
		for (int j = 4; --j >= 0;) {
			float tMid = (tLower + tUpper) / 2;
			calcRotatedPoint(tMid, iMid, true);
			if (((int) Math.floor(txRaster[iMid]) == (int) Math.floor(txRaster[iLower]))
					&& ((int) Math.floor(tyRaster[iMid]) == (int) Math.floor(tyRaster[iLower]))) {
				fp8IntensityUp[iLower] = (fp8IntensityUp[iLower] + fp8IntensityUp[iMid]) / 2;
				tLower = tMid;
			}
			else if (((int) Math.floor(txRaster[iMid]) == (int) Math.floor(txRaster[iUpper]))
					&& ((int) Math.floor(tyRaster[iMid]) == (int) Math.floor(tyRaster[iUpper]))) {
				fp8IntensityUp[iUpper] = (fp8IntensityUp[iUpper] + fp8IntensityUp[iMid]) / 2;
				tUpper = tMid;
			}
			else {
				interpolate(iLower, iMid);
				interpolate(iMid, iUpper);
				return;
			}
		}
		txRaster[iMid] = txRaster[iLower];
		tyRaster[iMid] = tyRaster[iUpper];
	}

	private void plotRaster(int i) {
		int fp8Up = fp8IntensityUp[i];
		int x = xRaster[i];
		int y = yRaster[i];
		int z = zRaster[i];
		if (tEndcapOpen) {
			if (notClipped) {
				g3d.plotPixelUnclipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap - z - 1);
				g3d.plotPixelUnclipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap + z - 1);
			}
			else {
				g3d.plotPixelClipped(argbEndcap, xEndcap + x, yEndcap + y, zEndcap - z - 1);
				g3d.plotPixelClipped(argbEndcap, xEndcap - x, yEndcap - y, zEndcap + z - 1);
			}
		}
		line3d.plotLineDelta(shades, isScreened, shades, isScreened, fp8Up >> 8, (int) (xA + x), (int) (yA + y),
				(int) (zA - z), (int) dxB, (int) dyB, (int) dzB, notClipped);
		if (endcaps == Graphics3D.ENDCAPS_OPEN) {
			line3d.plotLineDelta(shades[0], isScreened, shades[0], isScreened, (int) (xA - x), (int) (yA - y),
					(int) (zA + z), (int) dxB, (int) dyB, (int) dzB, notClipped);
		}
	}

	private int[] realloc(int[] a) {
		int[] t;
		t = new int[a.length * 2];
		System.arraycopy(a, 0, t, 0, a.length);
		return t;
	}

	private float[] realloc(float[] a) {
		float[] t;
		t = new float[a.length * 2];
		System.arraycopy(a, 0, t, 0, a.length);
		return t;
	}

	private int allocRaster(boolean isPrecision) {
		if (rasterCount >= MAX_FIX)
			return rasterCount;
		while (rasterCount >= xRaster.length) {
			xRaster = realloc(xRaster);
			yRaster = realloc(yRaster);
			zRaster = realloc(zRaster);
			tRaster = realloc(tRaster);
		}
		while (rasterCount >= fp8IntensityUp.length)
			fp8IntensityUp = realloc(fp8IntensityUp);
		if (isPrecision)
			while (rasterCount >= txRaster.length) {
				txRaster = realloc(txRaster);
				tyRaster = realloc(tyRaster);
				tzRaster = realloc(tzRaster);
			}
		return rasterCount++;
	}

	private void findMinMaxY() {
		yMin = yMax = yRaster[0];
		for (int i = rasterCount; --i > 0;) {
			int y = yRaster[i];
			if (y < yMin)
				yMin = y;
			else if (y > yMax)
				yMax = y;
			else {
				y = -y;
				if (y < yMin)
					yMin = y;
				else if (y > yMax)
					yMax = y;
			}
		}
	}

	private void findMinMaxX(int y) {
		xMin = Integer.MAX_VALUE;
		xMax = Integer.MIN_VALUE;
		for (int i = rasterCount; --i >= 0;) {
			if (yRaster[i] == y) {
				int x = xRaster[i];
				if (x < xMin) {
					xMin = x;
					zXMin = zRaster[i];
				}
				if (x > xMax) {
					xMax = x;
					zXMax = zRaster[i];
				}
			}
			if (yRaster[i] == -y) { // 0 will run through here too
				int x = -xRaster[i];
				if (x < xMin) {
					xMin = x;
					zXMin = -zRaster[i];
				}
				if (x > xMax) {
					xMax = x;
					zXMax = -zRaster[i];
				}
			}
		}
	}

	private void renderFlatEndcap(boolean tCylinder) {
		if (dzB == 0)
			return;
		float xT = xA, yT = yA, zT = zA;
		if (tCylinder && dzB < 0) {
			xT += dxB;
			yT += dyB;
			zT += dzB;
		}
		findMinMaxY();
		for (int y = yMin; y <= yMax; ++y) {
			findMinMaxX(y);
			int count = xMax - xMin + 1;
			g3d.setColorNoisy(colixEndcap, intensityEndcap);
			g3d.plotPixelsClipped(count, (int) (xT + xMin), (int) (yT + y), (int) (zT - zXMin - 1),
					(int) (zT - zXMax - 1), null, null);
		}
	}

	private void calcArgbEndcap(boolean tCylinder) {
		tEndcapOpen = false;
		if ((endcaps == Graphics3D.ENDCAPS_SPHERICAL) || (dzB == 0))
			return;
		xEndcap = (int) xA;
		yEndcap = (int) yA;
		zEndcap = (int) zA;
		int[] shadesEndcap;
		if (dzB >= 0 || !tCylinder) {
			intensityEndcap = Shade3D.calcIntensity(-dxB, -dyB, dzB);
			colixEndcap = colix;
			shadesEndcap = shades;
		}
		else {
			intensityEndcap = Shade3D.calcIntensity(dxB, dyB, -dzB);
			colixEndcap = colix;
			shadesEndcap = shades;
			xEndcap += dxB;
			yEndcap += dyB;
			zEndcap += dzB;
		}
		// limit specular glare on endcap
		if (intensityEndcap > Graphics3D.intensitySpecularSurfaceLimit)
			intensityEndcap = Graphics3D.intensitySpecularSurfaceLimit;
		argbEndcap = shadesEndcap[intensityEndcap];
		tEndcapOpen = (endcaps == Graphics3D.ENDCAPS_OPEN);
	}

}