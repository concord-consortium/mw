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
 * This class computes the complemented incomplete gamma function.
 * 
 * @author Connie Chen
 */

public class Igamc extends PostfixMathCommand {

	public Igamc() {
		numberOfParameters = 2;
	}

	public String toString() {
		return "The complemented incomplete gamma function";
	}

	@SuppressWarnings("unchecked")
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
	 *            double value
	 * @param x
	 *            double value
	 * @return the Complemented Incomplete Gamma function. Converted to Java from<BR>
	 *         Cephes Math Library Release 2.2: July, 1992<BR>
	 *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
	 *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
	 */

	public static double compute(double a, double x) throws ArithmeticException {

		if (x <= 0 || a <= 0)
			return 1.0;

		if (x < 1.0 || x < a)
			return 1.0 - Igam.compute(a, x);

		final double MACHEP = 1.11022302462515654042E-16;
		final double MAXLOG = 7.09782712893383996732E2;

		double big = 4.503599627370496e15;
		double biginv = 2.22044604925031308085e-16;
		double ans, ax, c, yc, r, t, y, z;
		double pk, pkm1, pkm2, qk, qkm1, qkm2;

		ax = a * Math.log(x) - x - LnGamma.compute(a);
		if (ax < -MAXLOG)
			return 0.0;

		ax = Math.exp(ax);

		/* continued fraction */
		y = 1.0 - a;
		z = x + y + 1.0;
		c = 0.0;
		pkm2 = 1.0;
		qkm2 = x;
		pkm1 = x + 1.0;
		qkm1 = z * x;
		ans = pkm1 / qkm1;

		do {
			c += 1.0;
			y += 1.0;
			z += 2.0;
			yc = y * c;
			pk = pkm1 * z - pkm2 * yc;
			qk = qkm1 * z - qkm2 * yc;
			if (qk != 0) {
				r = pk / qk;
				t = Math.abs((ans - r) / r);
				ans = r;
			}
			else t = 1.0;

			pkm2 = pkm1;
			pkm1 = pk;
			qkm2 = qkm1;
			qkm1 = qk;
			if (Math.abs(pk) > big) {
				pkm2 *= biginv;
				pkm1 *= biginv;
				qkm2 *= biginv;
				qkm1 *= biginv;
			}
		} while (t > MACHEP);

		return ans * ax;
	}

}
