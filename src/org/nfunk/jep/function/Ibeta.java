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
 * This class computes the incomplete Beta function evaluated from 0 to x
 * 
 * @author Connie Chen
 */

public class Ibeta extends PostfixMathCommand {

	private static final double MACHEP = 1.11022302462515654042E-16;
	private static final double MAXLOG = 7.09782712893383996732E2;
	private static final double MINLOG = -7.451332191019412076235E2;
	private static final double MAXGAM = 171.624376956302725;

	public Ibeta() {
		numberOfParameters = 3;
	}

	public String toString() {
		return "The incomplete beta function";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack stack) throws ParseException {

		// Check if stack is null
		if (null == stack)
			throw new ParseException("Stack argument null");

		double a = 0, b = 0, x = 0;

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
			b = ((Number) o).doubleValue();
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
		stack.push(new Double(compute(a, b, x)));

	}

	/**
	 * @param aa
	 *            double value
	 * @param bb
	 *            double value
	 * @param xx
	 *            double value
	 * @return The Incomplete Beta Function evaluated from zero to xx. Converted to Java from<BR>
	 *         Cephes Math Library Release 2.3: July, 1995<BR>
	 *         Copyright 1984, 1995 by Stephen L. Moshier<BR>
	 *         Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
	 */

	public static double compute(double aa, double bb, double xx) throws ArithmeticException {

		if (aa <= 0.0 || bb <= 0.0)
			throw new ArithmeticException("ibeta: Domain error!");

		if ((xx <= 0.0) || (xx >= 1.0)) {
			if (xx == 0.0)
				return 0.0;
			if (xx == 1.0)
				return 1.0;
			throw new ArithmeticException("ibeta: Domain error!");
		}

		double a, b, t, x, xc, w, y;
		boolean flag;

		flag = false;
		if ((bb * xx) <= 1.0 && xx <= 0.95) {
			t = pseries(aa, bb, xx);
			return t;
		}

		w = 1.0 - xx;

		/* Reverse a and b if x is greater than the mean. */
		if (xx > (aa / (aa + bb))) {
			flag = true;
			a = bb;
			b = aa;
			xc = xx;
			x = w;
		}
		else {
			a = aa;
			b = bb;
			xc = w;
			x = xx;
		}

		if (flag && (b * x) <= 1.0 && x <= 0.95) {
			t = pseries(a, b, x);
			if (t <= MACHEP)
				t = 1.0 - MACHEP;
			else t = 1.0 - t;
			return t;
		}

		/* Choose expansion for better convergence. */
		y = x * (a + b - 2.0) - (a - 1.0);
		if (y < 0.0)
			w = incbcf(a, b, x);
		else w = incbd(a, b, x) / xc;

		/*
		 * Multiply w by the factor a b _ _ _ x (1-x) | (a+b) / ( a | (a) | (b) ) .
		 */

		y = a * Math.log(x);
		t = b * Math.log(xc);
		if ((a + b) < MAXGAM && Math.abs(y) < MAXLOG && Math.abs(t) < MAXLOG) {
			t = Math.pow(xc, b);
			t *= Math.pow(x, a);
			t /= a;
			t *= w;
			t *= Gamma.compute(a + b) / (Gamma.compute(a) * Gamma.compute(b));
			if (flag) {
				if (t <= MACHEP)
					t = 1.0 - MACHEP;
				else t = 1.0 - t;
			}
			return t;
		}
		/* Resort to logarithms. */
		y += t + LnGamma.compute(a + b) - LnGamma.compute(a) - LnGamma.compute(b);
		y += Math.log(w / a);
		if (y < MINLOG)
			t = 0.0;
		else t = Math.exp(y);

		if (flag) {
			if (t <= MACHEP)
				t = 1.0 - MACHEP;
			else t = 1.0 - t;
		}
		return t;
	}

	/* Continued fraction expansion #1 for incomplete beta integral */
	private static double incbcf(double a, double b, double x) throws ArithmeticException {

		double xk, pk, pkm1, pkm2, qk, qkm1, qkm2;
		double k1, k2, k3, k4, k5, k6, k7, k8;
		double r, t, ans, thresh;
		int n;
		double big = 4.503599627370496e15;
		double biginv = 2.22044604925031308085e-16;

		k1 = a;
		k2 = a + b;
		k3 = a;
		k4 = a + 1.0;
		k5 = 1.0;
		k6 = b - 1.0;
		k7 = k4;
		k8 = a + 2.0;

		pkm2 = 0.0;
		qkm2 = 1.0;
		pkm1 = 1.0;
		qkm1 = 1.0;
		ans = 1.0;
		r = 1.0;
		n = 0;
		thresh = 3.0 * MACHEP;
		do {
			xk = -(x * k1 * k2) / (k3 * k4);
			pk = pkm1 + pkm2 * xk;
			qk = qkm1 + qkm2 * xk;
			pkm2 = pkm1;
			pkm1 = pk;
			qkm2 = qkm1;
			qkm1 = qk;

			xk = (x * k5 * k6) / (k7 * k8);
			pk = pkm1 + pkm2 * xk;
			qk = qkm1 + qkm2 * xk;
			pkm2 = pkm1;
			pkm1 = pk;
			qkm2 = qkm1;
			qkm1 = qk;

			if (qk != 0)
				r = pk / qk;
			if (r != 0) {
				t = Math.abs((ans - r) / r);
				ans = r;
			}
			else t = 1.0;

			if (t < thresh)
				return ans;

			k1 += 1.0;
			k2 += 1.0;
			k3 += 2.0;
			k4 += 2.0;
			k5 += 1.0;
			k6 -= 1.0;
			k7 += 2.0;
			k8 += 2.0;

			if ((Math.abs(qk) + Math.abs(pk)) > big) {
				pkm2 *= biginv;
				pkm1 *= biginv;
				qkm2 *= biginv;
				qkm1 *= biginv;
			}
			if ((Math.abs(qk) < biginv) || (Math.abs(pk) < biginv)) {
				pkm2 *= big;
				pkm1 *= big;
				qkm2 *= big;
				qkm1 *= big;
			}
		} while (++n < 300);

		return ans;
	}

	/* Continued fraction expansion #2 for incomplete beta integral */
	private static double incbd(double a, double b, double x) throws ArithmeticException {

		double xk, pk, pkm1, pkm2, qk, qkm1, qkm2;
		double k1, k2, k3, k4, k5, k6, k7, k8;
		double r, t, ans, z, thresh;
		int n;
		double big = 4.503599627370496e15;
		double biginv = 2.22044604925031308085e-16;

		k1 = a;
		k2 = b - 1.0;
		k3 = a;
		k4 = a + 1.0;
		k5 = 1.0;
		k6 = a + b;
		k7 = a + 1.0;
		;
		k8 = a + 2.0;

		pkm2 = 0.0;
		qkm2 = 1.0;
		pkm1 = 1.0;
		qkm1 = 1.0;
		z = x / (1.0 - x);
		ans = 1.0;
		r = 1.0;
		n = 0;
		thresh = 3.0 * MACHEP;
		do {
			xk = -(z * k1 * k2) / (k3 * k4);
			pk = pkm1 + pkm2 * xk;
			qk = qkm1 + qkm2 * xk;
			pkm2 = pkm1;
			pkm1 = pk;
			qkm2 = qkm1;
			qkm1 = qk;

			xk = (z * k5 * k6) / (k7 * k8);
			pk = pkm1 + pkm2 * xk;
			qk = qkm1 + qkm2 * xk;
			pkm2 = pkm1;
			pkm1 = pk;
			qkm2 = qkm1;
			qkm1 = qk;

			if (qk != 0)
				r = pk / qk;
			if (r != 0) {
				t = Math.abs((ans - r) / r);
				ans = r;
			}
			else t = 1.0;

			if (t < thresh)
				return ans;

			k1 += 1.0;
			k2 -= 1.0;
			k3 += 2.0;
			k4 += 2.0;
			k5 += 1.0;
			k6 += 1.0;
			k7 += 2.0;
			k8 += 2.0;

			if ((Math.abs(qk) + Math.abs(pk)) > big) {
				pkm2 *= biginv;
				pkm1 *= biginv;
				qkm2 *= biginv;
				qkm1 *= biginv;
			}
			if ((Math.abs(qk) < biginv) || (Math.abs(pk) < biginv)) {
				pkm2 *= big;
				pkm1 *= big;
				qkm2 *= big;
				qkm1 *= big;
			}
		} while (++n < 300);

		return ans;

	}

	/* Power series for incomplete beta integral. Use when b*x is small and x not too close to 1. */
	static private double pseries(double a, double b, double x) throws ArithmeticException {

		double s, t, u, v, n, t1, z, ai;

		ai = 1.0 / a;
		u = (1.0 - b) * x;
		v = u / (a + 1.0);
		t1 = v;
		t = u;
		n = 2.0;
		s = 0.0;
		z = MACHEP * ai;
		while (Math.abs(v) > z) {
			u = (n - b) * x / n;
			t *= u;
			v = t / (a + n);
			s += v;
			n += 1.0;
		}
		s += t1;
		s += ai;

		u = a * Math.log(x);
		if ((a + b) < MAXGAM && Math.abs(u) < MAXLOG) {
			t = Gamma.compute(a + b) / (Gamma.compute(a) * Gamma.compute(b));
			s = s * t * Math.pow(x, a);
		}
		else {
			t = LnGamma.compute(a + b) - LnGamma.compute(a) - LnGamma.compute(b) + u + Math.log(s);
			if (t < MINLOG)
				s = 0.0;
			else s = Math.exp(t);
		}
		return s;
	}

}
