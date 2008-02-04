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

public class Modulus extends PostfixMathCommand {

	public Modulus() {
		numberOfParameters = 2;
	}

	public String toString() {
		return "The modulus function";
	}

	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		if ((param1 instanceof Number) && (param2 instanceof Number)) {
			double divisor = ((Number) param2).doubleValue();
			double dividend = ((Number) param1).doubleValue();

			double result = dividend % divisor;

			inStack.push(new Double(result));
		}
		else {
			throw new ParseException("Invalid parameter type");
		}
		return;
	}
}
