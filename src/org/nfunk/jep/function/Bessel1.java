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

public class Bessel1 extends PostfixMathCommand {

	public Bessel1() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The Bessel function of order 1: J1(x)";
	}

	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(j1(param));// push the result on the inStack
	}

	public Object j1(Object param) throws ParseException {
		if (param instanceof Number)
			return new Double(compute(((Number) param).doubleValue()));
		throw new ParseException("Invalid parameter type");
	}

	/**
	 * @param x
	 *            a double value
	 * @return the Bessel function of order 1 of the argument.
	 */

	public static double compute(double x) throws ArithmeticException {

		double ax;
		double y;
		double ans1, ans2;

		if ((ax = Math.abs(x)) < 8.0) {

			y = x * x;
			ans1 = x
					* (72362614232.0 + y
							* (-7895059235.0 + y
									* (242396853.1 + y * (-2972611.439 + y * (15704.48260 + y * (-30.16036606))))));
			ans2 = 144725228442.0 + y
					* (2300535178.0 + y * (18583304.74 + y * (99447.43394 + y * (376.9991397 + y * 1.0))));
			return ans1 / ans2;

		}

		double z = 8.0 / ax;
		double xx = ax - 2.356194491;
		y = z * z;

		ans1 = 1.0 + y * (0.183105e-2 + y * (-0.3516396496e-4 + y * (0.2457520174e-5 + y * (-0.240337019e-6))));
		ans2 = 0.04687499995 + y
				* (-0.2002690873e-3 + y * (0.8449199096e-5 + y * (-0.88228987e-6 + y * 0.105787412e-6)));
		double ans = Math.sqrt(0.636619772 / ax) * (Math.cos(xx) * ans1 - z * Math.sin(xx) * ans2);
		if (x < 0.0)
			ans = -ans;
		return ans;

	}

}
