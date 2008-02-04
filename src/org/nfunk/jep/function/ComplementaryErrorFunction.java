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

/** The Complementary Error Function
 *   @author Connie Chen
 *   Concord Consortium 8/2/2003
 */

package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;

public class ComplementaryErrorFunction extends PostfixMathCommand {

	public ComplementaryErrorFunction() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The complementary error function";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(erfc(param));// push the result on the inStack
		return;
	}

	public Object erfc(Object param) throws ParseException {
		if (param instanceof Number) {
			double x = ((Number) param).doubleValue();
			return new Double(compute(x));
		}

		throw new ParseException("Invalid parameter type");
	}

	/**
	 * @param a
	 *            double value
	 * @return The complementary Error function Converted to Java from<BR>
	 *         Cephes Math Library Release 2.2: July, 1992<BR>
	 *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
	 *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
	 */

	public static double compute(double a) throws ArithmeticException {

		double MAXLOG = 7.09782712893383996732E2;

		double x, y, z, p, q;

		double P[] = { 2.46196981473530512524E-10, 5.64189564831068821977E-1, 7.46321056442269912687E0,
				4.86371970985681366614E1, 1.96520832956077098242E2, 5.26445194995477358631E2, 9.34528527171957607540E2,
				1.02755188689515710272E3, 5.57535335369399327526E2 };
		double Q[] = {
				// 1.0
				1.32281951154744992508E1, 8.67072140885989742329E1, 3.54937778887819891062E2, 9.75708501743205489753E2,
				1.82390916687909736289E3, 2.24633760818710981792E3, 1.65666309194161350182E3, 5.57535340817727675546E2 };

		double R[] = { 5.64189583547755073984E-1, 1.27536670759978104416E0, 5.01905042251180477414E0,
				6.16021097993053585195E0, 7.40974269950448939160E0, 2.97886665372100240670E0 };
		double S[] = {
				// 1.00000000000000000000E0,
				2.26052863220117276590E0, 9.39603524938001434673E0, 1.20489539808096656605E1, 1.70814450747565897222E1,
				9.60896809063285878198E0, 3.36907645100081516050E0 };

		if (a < 0.0)
			x = -a;
		else x = a;

		if (x < 1.0)
			return 1.0 - ErrorFunction.compute(a);

		z = -a * a;

		if (z < -MAXLOG) {
			if (a < 0)
				return (2.0);
			return (0.0);
		}

		z = Math.exp(z);

		if (x < 8.0) {
			p = ErrorFunction.polevl(x, P, 8);
			q = ErrorFunction.p1evl(x, Q, 8);
		}
		else {
			p = ErrorFunction.polevl(x, R, 5);
			q = ErrorFunction.p1evl(x, S, 6);
		}

		y = (z * p) / q;

		if (a < 0)
			y = 2.0 - y;

		if (y == 0.0) {
			if (a < 0)
				return 2.0;
			return (0.0);
		}

		return y;

	}

}
