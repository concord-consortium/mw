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

/** The Error Function
 *   @author Connie Chen
 *   Concord Consortium 8/2/2003
 */

package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;

public class ErrorFunction extends PostfixMathCommand {

	public ErrorFunction() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The error function";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(erf(param));// push the result on the inStack
		return;
	}

	public Object erf(Object param) throws ParseException {
		if (param instanceof Number) {
			double x = ((Number) param).doubleValue();
			return new Double(compute(x));
		}

		throw new ParseException("Invalid parameter type");
	}

	/**
	 * @param a
	 *            double value
	 * @return The Error function
	 *         <P>
	 *         Converted to Java from<BR>
	 *         Cephes Math Library Release 2.2: July, 1992<BR>
	 *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
	 *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
	 */

	public static double compute(double x) throws ArithmeticException {

		double y, z;
		double T[] = { 9.60497373987051638749E0, 9.00260197203842689217E1, 2.23200534594684319226E3,
				7.00332514112805075473E3, 5.55923013010394962768E4 };
		double U[] = {
				// 1.00000000000000000000E0,
				3.35617141647503099647E1, 5.21357949780152679795E2, 4.59432382970980127987E3, 2.26290000613890934246E4,
				4.92673942608635921086E4 };

		if (Math.abs(x) > 1.0)
			return (1.0 - ComplementaryErrorFunction.compute(x));
		z = x * x;
		y = x * polevl(z, T, 4) / p1evl(z, U, 5);
		return y;

	}

	static double polevl(double x, double coef[], int N) throws ArithmeticException {
		double ans = coef[0];
		for (int i = 1; i <= N; i++) {
			ans = ans * x + coef[i];
		}
		return ans;
	}

	static double p1evl(double x, double coef[], int N) throws ArithmeticException {
		double ans = x + coef[0];
		for (int i = 1; i < N; i++) {
			ans = ans * x + coef[i];
		}
		return ans;
	}

}
