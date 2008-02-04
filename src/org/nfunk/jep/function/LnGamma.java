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

/** The natural logarithm of the Gamma function
 *   
 *   @author onnie Chen
 *   Concord Consortium 8/2/2003
 */

package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;

public class LnGamma extends PostfixMathCommand {

	public LnGamma() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The natural logarithm of the gamma function";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(lgamma(param));// push the result on the inStack
		return;
	}

	public Object lgamma(Object param) throws ParseException {
		if (param instanceof Number) {
			double x = ((Number) param).doubleValue();
			if (x > 0)
				return new Double(compute(x));
			return new Double(Double.NaN);
		}
		throw new ParseException("Invalid parameter type");
	}

	/*
	 * Cephes Math Library Release 2.2: July, 1992 Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier Direct
	 * inquiries to 30 Frost Street, Cambridge, MA 02140
	 */

	public static double compute(double x) throws ArithmeticException {

		final double LOGPI = 1.14472988584940017414;

		double p, q, w, z;

		double A[] = { 8.11614167470508450300E-4, -5.95061904284301438324E-4, 7.93650340457716943945E-4,
				-2.77777777730099687205E-3, 8.33333333333331927722E-2 };
		double B[] = { -1.37825152569120859100E3, -3.88016315134637840924E4, -3.31612992738871184744E5,
				-1.16237097492762307383E6, -1.72173700820839662146E6, -8.53555664245765465627E5 };
		double C[] = {
		/* 1.00000000000000000000E0, */
		-3.51815701436523470549E2, -1.70642106651881159223E4, -2.20528590553854454839E5, -1.13933444367982507207E6,
				-2.53252307177582951285E6, -2.01889141433532773231E6 };

		if (x < -34.0) {
			q = -x;
			w = compute(q);
			p = Math.floor(q);
			if (p == q)
				throw new ArithmeticException("lgam: Overflow");
			z = q - p;
			if (z > 0.5) {
				p += 1.0;
				z = p - q;
			}
			z = q * Math.sin(Math.PI * z);
			if (z == 0.0)
				throw new ArithmeticException("lgamma: Overflow");
			z = LOGPI - Math.log(z) - w;
			return z;
		}

		if (x < 13.0) {
			z = 1.0;
			while (x >= 3.0) {
				x -= 1.0;
				z *= x;
			}
			while (x < 2.0) {
				if (x == 0.0)
					throw new ArithmeticException("lgamma: Overflow");
				z /= x;
				x += 1.0;
			}
			if (z < 0.0)
				z = -z;
			if (x == 2.0)
				return Math.log(z);
			x -= 2.0;
			p = x * Gamma.polevl(x, B, 5) / Gamma.p1evl(x, C, 6);
			return (Math.log(z) + p);
		}

		if (x > 2.556348e305)
			throw new ArithmeticException("lgamma: Overflow");

		q = (x - 0.5) * Math.log(x) - x + 0.91893853320467274178;
		if (x > 1.0e8)
			return (q);

		p = 1.0 / (x * x);
		if (x >= 1000.0)
			q += ((7.9365079365079365079365e-4 * p - 2.7777777777777777777778e-3) * p + 0.0833333333333333333333) / x;
		else q += Gamma.polevl(p, A, 4) / x;
		return q;
	}

}
