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
 * compute the area under the Gaussian probability density function, integrated from minus infinity to x
 * 
 * @author Connie Chen
 */

public class Normal extends PostfixMathCommand {

	private static final double SQRTH = 7.07106781186547524401E-1;

	public Normal() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The integral of the Gaussian function from negative infinity to x";
	}

	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(normal(param));// push the result on the inStack
	}

	private Object normal(Object param) throws ParseException {
		if (param instanceof Number) {
			return new Double(compute(((Number) param).doubleValue()));
		}
		throw new ParseException("Invalid parameter type");
	}

	/**
	 * @param a
	 *            double value
	 * @return The area under the Gaussian probability density function, integrated from minus infinity to x:
	 */

	public static double compute(double a) throws ArithmeticException {

		double x, y, z;

		x = a * SQRTH;
		z = Math.abs(x);

		if (z < SQRTH)
			y = 0.5 + 0.5 * ErrorFunction.compute(x);
		else {
			y = 0.5 * ComplementaryErrorFunction.compute(z);
			if (x > 0)
				y = 1.0 - y;
		}

		return y;

	}

}
