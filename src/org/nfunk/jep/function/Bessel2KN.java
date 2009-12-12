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
 * This class computes the Bessel function of order-n of the argument.
 * 
 * @author Connie Chen
 */

public class Bessel2KN extends PostfixMathCommand {

	public Bessel2KN() {
		numberOfParameters = 2;
	}

	public String toString() {
		return "The Bessel function of second kind of order n: Yn(x)";
	}

	public void run(Stack stack) throws ParseException {

		// Check if stack is null
		if (null == stack)
			throw new ParseException("Stack argument null");

		int n = 0;
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
			n = ((Number) o).intValue();
		}
		else {
			throw new ParseException("Invalid parameter type");
		}

		// push the result on the inStack
		stack.push(new Double(compute(n, x)));

	}

	/**
	 * @param n
	 *            integer order
	 * @param x
	 *            a double value
	 * @return the Bessel function of the second kind of order n of the argument.
	 */
	public static double compute(int n, double x) throws ArithmeticException {

		if (n == 0)
			return Bessel2K0.compute(x);
		if (n == 1)
			return Bessel2K1.compute(x);

		double by, bym, byp, tox;

		tox = 2.0 / x;
		by = Bessel2K1.compute(x);
		bym = Bessel2K0.compute(x);
		for (int j = 1; j < n; j++) {
			byp = j * tox * by - bym;
			bym = by;
			by = byp;
		}
		return by;
	}

}
