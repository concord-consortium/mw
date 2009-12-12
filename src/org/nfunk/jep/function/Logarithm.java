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

public class Logarithm extends PostfixMathCommand {

	public Logarithm() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The logarithm function of base 10";
	}

	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(log(param));// push the result on the inStack
	}

	public Object log(Object param) throws ParseException {

		if (param instanceof Number) {

			// Complex temp = new Complex(((Number)param).doubleValue());
			// Complex temp2 = new Complex(Math.log(10), 0);
			// return temp.log().div(temp2);

			// modified by Connie Chen
			return new Double(Math.log(((Number) param).doubleValue()) / Math.log(10));

		}
		else if (param instanceof Complex) {

			Complex temp = new Complex(Math.log(10), 0);
			return ((Complex) param).log().div(temp);
		}

		throw new ParseException("Invalid parameter type");
	}

}
