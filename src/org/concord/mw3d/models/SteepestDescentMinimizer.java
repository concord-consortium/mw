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

package org.concord.mw3d.models;

import java.util.BitSet;

public class SteepestDescentMinimizer {

	private SteepestDescentMinimizer() {
	}

	/**
	 * Implement the fixed-steplength steepest descent method.
	 * 
	 * @param delta
	 *            the steplength of minimization
	 * @param selection
	 *            represents the selected atom
	 * @return the potential energy after this step of minization
	 */
	public static float minimize(MolecularModel model, float delta, BitSet selection) {
		int n = model.getAtomCount();
		if (n <= 1)
			return -1;
		float potential = 0;
		float sumgrad = 0;
		float x = 0;
		for (int i = 0; i < n; i++) {
			if (!selection.get(i) || !model.atom[i].isMovable())
				continue;
			x = model.atom[i].fx;
			sumgrad += x * x;
			x = model.atom[i].fy;
			sumgrad += x * x;
			x = model.atom[i].fz;
			sumgrad += x * x;
		}
		if (sumgrad > MolecularModel.ZERO) {
			sumgrad = delta / (float) Math.sqrt(sumgrad);
			for (int i = 0; i < n; i++) {
				if (!selection.get(i) || !model.atom[i].isMovable())
					continue;
				model.atom[i].rx += model.atom[i].fx * sumgrad;
				model.atom[i].ry += model.atom[i].fy * sumgrad;
				model.atom[i].rz += model.atom[i].fz * sumgrad;
			}
			model.applyBoundary();
			potential = model.compute(-1);
		}
		return potential;
	}

	public static float minimize(MolecularModel model, float delta) {
		int n = model.getAtomCount();
		if (n <= 1)
			return -1;
		float potential = 0;
		float sumgrad = 0;
		float a = 0;
		for (int i = 0; i < n; i++) {
			if (!model.atom[i].isMovable())
				continue;
			a = model.atom[i].fx;
			sumgrad += a * a;
			a = model.atom[i].fy;
			sumgrad += a * a;
			a = model.atom[i].fz;
			sumgrad += a * a;
		}
		if (sumgrad > MolecularModel.ZERO) {
			sumgrad = delta / (float) Math.sqrt(sumgrad);
			for (int i = 0; i < n; i++) {
				if (!model.atom[i].isMovable())
					continue;
				model.atom[i].rx += model.atom[i].fx * sumgrad;
				model.atom[i].ry += model.atom[i].fy * sumgrad;
				model.atom[i].rz += model.atom[i].fz * sumgrad;
			}
			model.applyBoundary();
			potential = model.compute(-1);
		}
		return potential;
	}

}