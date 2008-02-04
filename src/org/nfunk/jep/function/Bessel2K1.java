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

public class Bessel2K1 extends PostfixMathCommand {

	public Bessel2K1() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The Bessel function of second kind of order 1: Y1(x)";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(y1(param));// push the result on the inStack
		return;
	}

	public Object y1(Object param) throws ParseException {
		if (param instanceof Number) {
			double x = ((Number) param).doubleValue();
			return new Double(compute(x));
		}

		throw new ParseException("Invalid parameter type");
	}

	/**
	 * @param x
	 *            a double value
	 * @return the Bessel function of the second kind of order 1 of the argument.
	 */
	public static double compute(double x) throws ArithmeticException {

		if (x < 8.0) {

			double y = x * x;
			double ans1 = x
					* (-0.4900604943e13 + y
							* (0.1275274390e13 + y
									* (-0.5153438139e11 + y
											* (0.7349264551e9 + y * (-0.4237922726e7 + y * 0.8511937935e4)))));
			double ans2 = 0.2499580570e14
					+ y
					* (0.4244419664e12 + y
							* (0.3733650367e10 + y * (0.2245904002e8 + y * (0.1020426050e6 + y * (0.3549632885e3 + y)))));
			return (ans1 / ans2) + 0.636619772 * (Bessel1.compute(x) * Math.log(x) - 1.0 / x);

		}

		double z = 8.0 / x;
		double y = z * z;
		double xx = x - 2.356194491;
		double ans1 = 1.0 + y * (0.183105e-2 + y * (-0.3516396496e-4 + y * (0.2457520174e-5 + y * (-0.240337019e-6))));
		double ans2 = 0.04687499995 + y
				* (-0.2002690873e-3 + y * (0.8449199096e-5 + y * (-0.88228987e-6 + y * 0.105787412e-6)));

		return Math.sqrt(0.636619772 / x) * (Math.sin(xx) * ans1 + z * Math.cos(xx) * ans2);

	}

}
