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

/** Gamma function
 *   
 *   @author onnie Chen
 *   Concord Consortium 8/2/2003
 */

package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;

public class Gamma extends PostfixMathCommand {

	public Gamma() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The gamma function";
	}

	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(gamma(param));// push the result on the inStack
	}

	public Object gamma(Object param) throws ParseException {
		if (param instanceof Number) {
			double x = ((Number) param).doubleValue();
			if (x > 0)
				return new Double(compute(x));
			return new Double(Double.NaN);
		}
		throw new ParseException("Invalid parameter type");
	}

	/**
	 * @param x
	 *            a double value
	 * @return the Gamma function of the value.
	 *         <P>
	 *         Converted to Java from<BR>
	 *         Cephes Math Library Release 2.2: July, 1992<BR>
	 *         Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
	 *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
	 */
	public static double compute(double x) throws ArithmeticException {

		double P[] = { 1.60119522476751861407E-4, 1.19135147006586384913E-3, 1.04213797561761569935E-2,
				4.76367800457137231464E-2, 2.07448227648435975150E-1, 4.94214826801497100753E-1,
				9.99999999999999996796E-1 };
		double Q[] = { -2.31581873324120129819E-5, 5.39605580493303397842E-4, -4.45641913851797240494E-3,
				1.18139785222060435552E-2, 3.58236398605498653373E-2, -2.34591795718243348568E-1,
				7.14304917030273074085E-2, 1.00000000000000000320E0 };

		double p, z;

		double q = Math.abs(x);

		if (q > 33.0) {
			if (x < 0.0) {
				p = Math.floor(q);
				if (p == q)
					throw new ArithmeticException("gamma: overflow");
				z = q - p;
				if (z > 0.5) {
					p += 1.0;
					z = q - p;
				}
				z = q * Math.sin(Math.PI * z);
				if (z == 0.0)
					throw new ArithmeticException("gamma: overflow");
				z = Math.abs(z);
				z = Math.PI / (z * stirf(q));

				return -z;
			}
			return stirf(x);
		}

		z = 1.0;
		while (x >= 3.0) {
			x -= 1.0;
			z *= x;
		}

		while (x < 0.0) {
			if (x == 0.0) {
				throw new ArithmeticException("gamma: singular");
			}
			else if (x > -1.E-9) {
				return (z / ((1.0 + 0.5772156649015329 * x) * x));
			}
			z /= x;
			x += 1.0;
		}

		while (x < 2.0) {
			if (x == 0.0) {
				throw new ArithmeticException("gamma: singular");
			}
			else if (x < 1.e-9) {
				return (z / ((1.0 + 0.5772156649015329 * x) * x));
			}
			z /= x;
			x += 1.0;
		}

		if ((x == 2.0) || (x == 3.0))
			return z;

		x -= 2.0;
		p = polevl(x, P, 6);
		q = polevl(x, Q, 7);
		return z * p / q;

	}

	/*
	 * Gamma function computed by Stirling's formula. The polynomial STIR is valid for 33 <= x <= 172.
	 * 
	 * Cephes Math Library Release 2.2: July, 1992 Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier Direct
	 * inquiries to 30 Frost Street, Cambridge, MA 02140
	 */
	static private double stirf(double x) throws ArithmeticException {
		double STIR[] = { 7.87311395793093628397E-4, -2.29549961613378126380E-4, -2.68132617805781232825E-3,
				3.47222221605458667310E-3, 8.33333333333482257126E-2, };
		double MAXSTIR = 143.01608;
		double SQTPI = 2.50662827463100050242E0;

		double w = 1.0 / x;
		double y = Math.exp(x);

		w = 1.0 + w * polevl(w, STIR, 4);

		if (x > MAXSTIR) {
			/* Avoid overflow in Math.pow() */
			double v = Math.pow(x, 0.5 * x - 0.25);
			y = v * (v / y);
		}
		else {
			y = Math.pow(x, x - 0.5) / y;
		}
		y = SQTPI * y * w;
		return y;
	}

	static double polevl(double x, double coef[], int N) throws ArithmeticException {
		double ans;
		ans = coef[0];
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
