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

/*****************************************************************************

 JEP - Java Math Expression Parser 2.24
 December 30 2002
 (c) Copyright 2002, Nathan Funk
 See LICENSE.txt for license information.

 *****************************************************************************/
package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.type.Complex;

public class Power extends PostfixMathCommand {

	public Power() {
		numberOfParameters = 2;
	}

	public String toString() {
		return "The power function";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack); // check the stack
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		inStack.push(power(param1, param2));
	}

	public Object power(Object param1, Object param2) throws ParseException {
		if (param1 instanceof Number) {
			if (param2 instanceof Number) {
				return power((Number) param1, (Number) param2);
			}
			else if (param2 instanceof Complex) {
				return power((Number) param1, (Complex) param2);
			}
		}
		else if (param1 instanceof Complex) {
			if (param2 instanceof Number) {
				return power((Complex) param1, (Number) param2);
			}
			else if (param2 instanceof Complex) {
				return power((Complex) param1, (Complex) param2);
			}
		}

		throw new ParseException("Invalid parameter type");
	}

	public Object power(Number d1, Number d2) {
		if (d1.doubleValue() < 0 && d2.doubleValue() != d2.intValue()) {
			Complex c = new Complex(d1.doubleValue(), 0.0);
			return c.power(d2.doubleValue());
		}
		return new Double(Math.pow(d1.doubleValue(), d2.doubleValue()));
	}

	public Object power(Complex c1, Complex c2) {
		Complex temp = c1.power(c2);

		if (temp.im() == 0)
			return new Double(temp.re());
		return temp;
	}

	public Object power(Complex c, Number d) {
		Complex temp = c.power(d.doubleValue());

		if (temp.im() == 0)
			return new Double(temp.re());
		return temp;
	}

	public Object power(Number d, Complex c) {
		Complex base = new Complex(d.doubleValue(), 0.0);
		Complex temp = base.power(c);

		if (temp.im() == 0)
			return new Double(temp.re());
		return temp;
	}

}
