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

/**
 * This class serves mainly as an example of a function that accepts any number of parameters. Note that the
 * numberOfParameters is initialized to -1.
 */
public class Sum extends PostfixMathCommand {

	public Sum() {
		// Use a variable number of arguments
		numberOfParameters = -1;
	}

	public String toString() {
		return "The summation function";
	}

	/**
	 * Calculates the result of summing up all parameters, which are assumed to be of the Double type.
	 */
	public void run(Stack stack) throws ParseException {

		// Check if stack is null
		if (null == stack) {
			throw new ParseException("Stack argument null");
		}

		Object param = null;
		double result = 0;
		int i = 0;

		// repeat summation for each one of the current parameters
		while (i < curNumberOfParameters) {
			// get the parameter from the stack
			param = stack.pop();
			if (param instanceof Number) {
				// calculate the result
				result += ((Number) param).doubleValue();
			}
			else {
				throw new ParseException("Invalid parameter type");
			}

			i++;
		}

		// push the result on the inStack
		stack.push(new Double(result));
	}

}
