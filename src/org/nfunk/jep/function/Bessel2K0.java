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

/** added by Connie Chen to JEP
 *   Concord Consortium 8/2/2003
 */

package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;

public class Bessel2K0 extends PostfixMathCommand {

	public Bessel2K0() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The Bessel function of second kind of order 0: Y0(x)";
	}

	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(y0(param));// push the result on the inStack
	}

	public Object y0(Object param) throws ParseException {
		if (param instanceof Number) {
			double x = ((Number) param).doubleValue();
			return new Double(compute(x));
		}
		throw new ParseException("Invalid parameter type");
	}

	/**
	 * @param x
	 *            a double value
	 * @return the Bessel function of the second kind, of order 0 of the argument.
	 */

	public static double compute(double x) throws ArithmeticException {

		if (x < 8.0) {

			double y = x * x;
			double ans1 = -2957821389.0 + y
					* (7062834065.0 + y * (-512359803.6 + y * (10879881.29 + y * (-86327.92757 + y * 228.4622733))));
			double ans2 = 40076544269.0 + y
					* (745249964.8 + y * (7189466.438 + y * (47447.26470 + y * (226.1030244 + y * 1.0))));
			return (ans1 / ans2) + 0.636619772 * Bessel0.compute(x) * Math.log(x);

		}

		double z = 8.0 / x;
		double y = z * z;
		double xx = x - 0.785398164;
		double ans1 = 1.0 + y
				* (-0.1098628627e-2 + y * (0.2734510407e-4 + y * (-0.2073370639e-5 + y * 0.2093887211e-6)));
		double ans2 = -0.1562499995e-1 + y
				* (0.1430488765e-3 + y * (-0.6911147651e-5 + y * (0.7621095161e-6 + y * (-0.934945152e-7))));
		return Math.sqrt(0.636619772 / x) * (Math.sin(xx) * ans1 + z * Math.cos(xx) * ans2);

	}

}
