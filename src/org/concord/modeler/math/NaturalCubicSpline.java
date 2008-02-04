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

package org.concord.modeler.math;

/**
 * calculates the natural cubic spline that interpolates y[0], y[1], ... * y[n]
 * The first segment is returned as C[0].a + C[0].b*u + * C[0].c*u^2 +
 * C[0].d*u^3 0<=u <1 the other segments are in C[1], * C[2], ... C[n-1]
 */

public class NaturalCubicSpline {

	public static CubicPolynomial[] compute(int n, int[] x) {

		float[] gamma = new float[n + 1];
		float[] delta = new float[n + 1];
		float[] D = new float[n + 1];
		int i;

		/*
		 * We solve the equation [2 1 ] [D[0]] [3(x[1] - x[0]) ] |1 4 1 | |D[1]|
		 * |3(x[2] - x[0]) | | 1 4 1 | | . | = | . | | ..... | | . | | . | | 1 4
		 * 1| | . | |3(x[n] - x[n-2])| [ 1 2] [D[n]] [3(x[n] - x[n-1])]
		 * 
		 * by using row operations to convert the matrix to upper triangular and
		 * then back sustitution. The D[i] are the derivatives at the knots.
		 */

		gamma[0] = 1.0f / 2.0f;
		for (i = 1; i < n; i++) {
			gamma[i] = 1 / (4 - gamma[i - 1]);
		}
		gamma[n] = 1 / (2 - gamma[n - 1]);

		delta[0] = 3 * (x[1] - x[0]) * gamma[0];
		for (i = 1; i < n; i++) {
			delta[i] = (3 * (x[i + 1] - x[i - 1]) - delta[i - 1]) * gamma[i];
		}
		delta[n] = (3 * (x[n] - x[n - 1]) - delta[n - 1]) * gamma[n];

		D[n] = delta[n];
		for (i = n - 1; i >= 0; i--) {
			D[i] = delta[i] - gamma[i] * D[i + 1];
		}

		/* now compute the coefficients of the cubics */
		CubicPolynomial[] C = new CubicPolynomial[n];
		for (i = 0; i < n; i++) {
			C[i] = new CubicPolynomial(x[i], D[i], 3 * (x[i + 1] - x[i]) - 2 * D[i] - D[i + 1], 2 * (x[i] - x[i + 1])
					+ D[i] + D[i + 1]);
		}
		return C;

	}

	public static CubicPolynomial[] compute(int n, short[] x) {

		float[] gamma = new float[n + 1];
		float[] delta = new float[n + 1];
		float[] D = new float[n + 1];
		int i;

		gamma[0] = 1.0f / 2.0f;
		for (i = 1; i < n; i++) {
			gamma[i] = 1 / (4 - gamma[i - 1]);
		}
		gamma[n] = 1 / (2 - gamma[n - 1]);

		delta[0] = 3 * (x[1] - x[0]) * gamma[0];
		for (i = 1; i < n; i++) {
			delta[i] = (3 * (x[i + 1] - x[i - 1]) - delta[i - 1]) * gamma[i];
		}
		delta[n] = (3 * (x[n] - x[n - 1]) - delta[n - 1]) * gamma[n];

		D[n] = delta[n];
		for (i = n - 1; i >= 0; i--) {
			D[i] = delta[i] - gamma[i] * D[i + 1];
		}

		CubicPolynomial[] C = new CubicPolynomial[n];
		for (i = 0; i < n; i++) {
			C[i] = new CubicPolynomial(x[i], D[i], 3 * (x[i + 1] - x[i]) - 2 * D[i] - D[i + 1], 2
					* (x[i] - x[i + 1]) + D[i] + D[i + 1]);
		}
		return C;

	}

}
