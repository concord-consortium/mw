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

package org.concord.molbio.engine;

public final class MutatorRandomizer {

	private double[] params = new double[Mutator.NUMB_POSSIBLE_MUTATORS];
	private double[] limits = new double[Mutator.NUMB_POSSIBLE_MUTATORS + 1];

	public MutatorRandomizer(int ident, int subst, int insert, int delet) {
		params[0] = ident;
		params[1] = subst;
		params[2] = insert;
		params[3] = delet;
		double summ = 0;
		for (int i = 0; i < params.length; i++)
			summ += params[i];
		for (int i = 0; i < params.length; i++)
			params[i] /= summ;
		limits[0] = 0;
		for (int i = 1; i < params.length; i++)
			limits[i] = limits[i - 1] + params[i - 1];
		limits[params.length] = 1;
	}

	public int getRandomMutatorType() {
		double newrandom = Math.random();
		for (int i = 0; i < limits.length - 1; i++) {
			if (newrandom >= limits[i] && newrandom < limits[i + 1])
				return getAllowedMutatorType(i);
		}
		return Mutator.MUTATOR_UNKNOWN;
	}

	protected int getAllowedMutatorType(int n) {
		if (n == Mutator.MUTATOR_IDENTITY)
			return Mutator.MUTATOR_IDENTITY;
		if (n == Mutator.MUTATOR_SUBSTITUTION)
			return Mutator.MUTATOR_SUBSTITUTION;
		if (n == Mutator.MUTATOR_DELETION)
			return Mutator.MUTATOR_DELETION;
		if (n == Mutator.MUTATOR_INSERTION)
			return Mutator.MUTATOR_INSERTION;
		return Mutator.MUTATOR_UNKNOWN;
	}

}