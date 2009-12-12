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
 * This class computes the logarithm of arbitray base log(a, x)
 * 
 * @author Connie Chen
 */

public class Log extends PostfixMathCommand {

	public Log() {
		numberOfParameters = 2;
	}

	public String toString() {
		return "The logarithm of arbitrary base";
	}

	public void run(Stack stack) throws ParseException {

		// Check if stack is null
		if (null == stack)
			throw new ParseException("Stack argument null");

		double a = 0;
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
			a = ((Number) o).doubleValue();
		}
		else {
			throw new ParseException("Invalid parameter type");
		}

		// push the result on the inStack
		stack.push(new Double(compute(a, x)));

	}

	/**
	 * @param a
	 *            a double value
	 * @param x
	 *            a double value
	 * @return the logrithm with a as the base.
	 */
	public static double compute(double a, double x) throws ArithmeticException {

		if (Double.doubleToLongBits(a) == Double.doubleToLongBits(1.0))
			return Double.NaN;
		if (a <= 0.0 || x <= 0.0)
			return Double.NaN;

		return Math.log(x) / Math.log(a);

	}

}
