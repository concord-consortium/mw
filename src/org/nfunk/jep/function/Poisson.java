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

package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;

/**
 * This class computes the sum of the first k terms of the Poisson distribution.
 * 
 * @author Connie Chen
 */

public class Poisson extends PostfixMathCommand {

	public Poisson() {
		numberOfParameters = 2;
	}

	public String toString() {
		return "The sum of the first k terms of the Poisson distribution";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack stack) throws ParseException {

		// Check if stack is null
		if (null == stack)
			throw new ParseException("Stack argument null");

		int k = 0;
		double x = 0;

		// get the parameter from the stack
		Object o = stack.pop();
		if (o instanceof Number) {
			x = ((Number) o).doubleValue();
		}
		else {
			throw new ParseException("Invalid parameter type");
		}

		o = stack.pop();
		if (o instanceof Number) {
			k = ((Number) o).intValue();
		}
		else {
			throw new ParseException("Invalid parameter type");
		}

		// push the result on the inStack
		stack.push(new Double(compute(k, x)));

	}

	/**
	 * Returns the sum of the first k terms of the Poisson distribution.
	 * 
	 * @param k
	 *            number of terms
	 * @param x
	 *            double value
	 */

	static public double compute(int k, double x) throws ArithmeticException {

		if (k < 0 || x < 0)
			return Double.NaN;

		return Igamc.compute(k + 1, x);
	}

}
