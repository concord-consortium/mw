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

package org.concord.modeler.g2d;

import java.util.Vector;

public class ContourMap {

	/** The minimum length of a curve before it gets a label */
	public static final int MINCELLS = 30;

	/** Default number of contour levels */
	public static final int NLEVELS = 10;

	private double[] grid;
	private int nx, ny;
	private double xmin, xmax;
	private double ymin, ymax;
	private double zmin, zmax;

	private boolean logLevels = false;
	private boolean expLevels = false;
	private boolean autoLevels = true;

	private Vector[] curves;
	private double[] levels;

	public ContourMap(int nx, int ny, double[] grid) {
		this.nx = nx;
		this.ny = ny;
		this.grid = grid;
	}

	public void setLogLevels(boolean logLevels) {
		this.logLevels = logLevels;
	}

	public void setExpLevels(boolean expLevels) {
		this.expLevels = expLevels;
	}

	/** set range of grid */
	public void setRange(double xmin, double xmax, double ymin, double ymax) {
		if (xmin >= xmax || ymin >= ymax)
			return;
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
	}

	/* Calculate the range of the grid */
	private void zrange() {
		int i;
		zmin = grid[0];
		zmax = grid[1];
		for (i = 0; i < grid.length; i++) {
			zmin = Math.min(zmin, grid[i]);
			zmax = Math.max(zmax, grid[i]);
		}
		if (zmin == zmax) {
			javax.swing.JOptionPane.showMessageDialog(null, "Cannot produce contours of a constant surface.",
					"Flat surface", javax.swing.JOptionPane.WARNING_MESSAGE);
		}
		// if(zmin <= 0 || zmax <= 0) logLevels = false;
		// if(zmin<=0) {for(i=0;i<grid.length;i++)grid[i]+=-zmin+0.01;}
	}

	/** Set the number of contour levels. */
	public void setNLevels(int l) {
		if (l <= 0)
			return;
		levels = new double[l];
		calcLevels();
		curves = null;
	}

	/* Calculate the contour levels */
	private void calcLevels() {
		int i;
		if (!autoLevels)
			return;

		if (levels == null)
			levels = new double[NLEVELS];

		zrange();

		if (logLevels) {
			double inc = Math.log(zmax - zmin) / (levels.length + 1);
			try {
				for (i = 0; i < levels.length; i++)
					levels[i] = zmin + Math.exp((i + 1) * inc);
			}
			catch (Exception e) {
				System.out.println("Error calculateing Log levels!");
				System.out.println("... calculating linear levels instead");
				logLevels = false;
				calcLevels();
			}
		}
		else if (expLevels) {
			double inc = Math.exp(zmax - zmin) / (levels.length + 1);
			for (i = 0; i < levels.length; i++)
				levels[i] = zmin + Math.log((i + 1) * inc);
		}
		else {
			double inc = (zmax - zmin) / (levels.length + 1);
			for (i = 0; i < levels.length; i++)
				levels[i] = zmin + (i + 1) * inc;
		}

	}

	@SuppressWarnings("unchecked")
	public Vector[] getCurves() {
		int i;
		int j;
		double[] data;
		double xscale = (xmax - xmin) / (nx - 1);
		double yscale = (ymax - ymin) / (ny - 1);

		IsoCurve isocurve = new IsoCurve(grid, nx, ny);

		if (zmin == zmax)
			return null;

		curves = new Vector[levels.length];

		for (i = 0; i < levels.length; i++) {
			// System.out.println("Calculating Contours: level="+levels[i]);
			isocurve.setValue(levels[i]);

			curves[i] = new Vector();

			while ((data = isocurve.getCurve()) != null) {
				for (j = 0; j < data.length;) {
					data[j] = xmin + data[j] * xscale;
					j++;
					data[j] = ymin + data[j] * yscale;
					j++;
				}

				try {
					curves[i].addElement(data);
				}
				catch (Exception e) {
					System.out.println("Error loading contour into DataSet!");
					System.out.println("...Contour Level " + levels[i]);
				}
			}

		}

		return curves;

	}
}
