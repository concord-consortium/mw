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

/**
 * Energy minimization is a commonly used method in molecular modeling and simulation. It is based on the point that a
 * conformation of lower energy has better stability against thermal perturbations, searching an energy minimum can
 * often lead us to a conformation that is representative enough for a certain structure.
 */

public class Minimizer {

	private MolecularModel model;
	private static double[] congvxAtLastStep, congvyAtLastStep;
	private double modeGradientAtLastStep = 1.0;

	public Minimizer(MolecularModel model) {
		this.model = model;
		model.setUpdateList(true);
		model.computeForce(-1);
	}

	private void initCongArrays() {
		congvxAtLastStep = new double[model.atom.length];
		congvyAtLastStep = new double[model.atom.length];
	}

	/**
	 * Implement the fixed-steplength steepest descent method.
	 * 
	 * @param delta
	 *            the steplength of minimization
	 * @return the potential energy after this step of minization
	 */
	public double sd(double delta) {
		int n = model.getNumberOfAtoms();
		if (n <= 1)
			return -1.0;
		double potential = 0.0;
		double sumgrad = 0.0;
		for (int i = 0; i < n; i++) {
			if (!model.atom[i].isMovable())
				continue;
			sumgrad += model.atom[i].fx * model.atom[i].fx + model.atom[i].fy * model.atom[i].fy;
		}
		if (sumgrad > Particle.ZERO) {
			sumgrad = delta / Math.sqrt(sumgrad);
			for (int i = 0; i < n; i++) {
				if (!model.atom[i].isMovable())
					continue;
				model.atom[i].rx += model.atom[i].fx * sumgrad;
				model.atom[i].ry += model.atom[i].fy * sumgrad;
			}
			model.putInBounds();
			potential = model.computeForce(-1);
		}
		return potential;
	}

	/**
	 * Implement the fixed-steplength conjugate gradient method.
	 * 
	 * @param delta
	 *            the steplength of minimization
	 * @return the potential energy after this step of minization
	 */
	public double cg(double delta) {
		int n = model.getNumberOfAtoms();
		if (n <= 1)
			return -1.0;
		if (congvxAtLastStep == null || congvyAtLastStep == null)
			initCongArrays();
		double potential = 0.0;
		double scalar = 1.0;
		double sumgrad = 0.0;
		double tempx = 0.0, tempy = 0.0;
		for (int i = 0; i < n; i++)
			sumgrad += model.atom[i].fx * model.atom[i].fx + model.atom[i].fy * model.atom[i].fy;
		scalar = -sumgrad / modeGradientAtLastStep;
		modeGradientAtLastStep = sumgrad;
		sumgrad = 0.0;
		for (int i = 0; i < n; i++) {
			tempx = scalar * congvxAtLastStep[i] + model.atom[i].fx;
			tempy = scalar * congvyAtLastStep[i] + model.atom[i].fy;
			congvxAtLastStep[i] = tempx;
			congvyAtLastStep[i] = tempy;
			sumgrad += congvxAtLastStep[i] * congvxAtLastStep[i] + congvyAtLastStep[i] * congvyAtLastStep[i];
		}
		model.putInBounds();
		sumgrad = Math.sqrt(sumgrad);
		for (int i = 0; i < n; i++) {
			if (!model.atom[i].isMovable())
				continue;
			congvxAtLastStep[i] /= sumgrad;
			congvyAtLastStep[i] /= sumgrad;
			model.atom[i].rx = model.atom[i].rx + congvxAtLastStep[i] * delta;
			model.atom[i].ry = model.atom[i].ry + congvyAtLastStep[i] * delta;
		}
		potential = model.computeForce(-1);
		return potential;
	}

}