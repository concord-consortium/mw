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

public class Bessel0 extends PostfixMathCommand {

	public Bessel0() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The Bessel function of order 0: J0(x)";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(j0(param));// push the result on the inStack
		return;
	}

	public Object j0(Object param) throws ParseException {
		if (param instanceof Number) {
			double x = ((Number) param).doubleValue();
			return new Double(compute(x));
		}

		throw new ParseException("Invalid parameter type");
	}

	/**
	 * @param x
	 *            a double value
	 * @return the Bessel function of order 0 of the argument.
	 */
	public static double compute(double x) throws ArithmeticException {

		double ax;

		if ((ax = Math.abs(x)) < 8.0) {

			double y = x * x;
			double ans1 = 57568490574.0
					+ y
					* (-13362590354.0 + y * (651619640.7 + y * (-11214424.18 + y * (77392.33017 + y * (-184.9052456)))));
			double ans2 = 57568490411.0 + y
					* (1029532985.0 + y * (9494680.718 + y * (59272.64853 + y * (267.8532712 + y * 1.0))));

			return ans1 / ans2;

		}

		double z = 8.0 / ax;
		double y = z * z;
		double xx = ax - 0.785398164;
		double ans1 = 1.0 + y
				* (-0.1098628627e-2 + y * (0.2734510407e-4 + y * (-0.2073370639e-5 + y * 0.2093887211e-6)));
		double ans2 = -0.1562499995e-1 + y
				* (0.1430488765e-3 + y * (-0.6911147651e-5 + y * (0.7621095161e-6 - y * 0.934935152e-7)));

		return Math.sqrt(0.636619772 / ax) * (Math.cos(xx) * ans1 - z * Math.sin(xx) * ans2);

	}

}
