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

/** translated from Numerical Recipes in F77 */

public class EllipticIntegral {

	private final static double CA = .0003;

	/** return the complete elliptic integral of the second kind. */
	public static double e(final double k) {
		double kc = Math.sqrt(1.0 - k * k);
		return cel(kc, 1.0, 1.0, kc * kc);
	}

	/** return the general complete elliptic integral of the second kind. */
	public static double cel(double qqc, double pp, double aa, double bb) throws ArithmeticException {

		if (qqc == 0)
			throw new ArithmeticException("failure in CEL, argument QQC was zero");

		double qc = Math.abs(qqc);
		double a = aa;
		double b = bb;
		double p = pp;
		double e = qc;
		double em = 1.;
		double f = 0.;
		double g = 0.;
		double q = 0.;

		if (p > 0.) {
			p = Math.sqrt(p);
			b /= p;
		}
		else {
			f = qc * qc;
			q = 1. - f;
			g = 1. - p;
			f -= p;
			q *= b - a * p;
			p = Math.sqrt(f / g);
			a = (a - b) / g;
			b = -q / (g * g * p) + a * p;
		}

		while (true) {
			f = a;
			a += b / p;
			g = e / p;
			b += f * g;
			b += b;
			p += g;
			g = em;
			em += qc;
			if (Math.abs(g - qc) > g * CA) {
				qc = Math.sqrt(e);
				qc += qc;
				e = qc * em;
			}
			else {
				break;
			}
		}

		return .5 * Math.PI * (b + a * em) / (em * (em + p));

	}

}
