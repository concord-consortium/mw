/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

public class CubeRoot extends PostfixMathCommand {

	public CubeRoot() {
		numberOfParameters = 1;
	}

	public String toString() {
		return "The cube root function";
	}

	/**
	 * Applies the function to the parameters on the stack.
	 */
	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(cbrt(param));// push the result on the inStack
		return;
	}

	/**
	 * Calculates the cube root of the parameter. The parameter must be of type Double.
	 * 
	 * @return The cube root of the parameter.
	 */
	public Object cbrt(Object param) throws ParseException {
		if (param instanceof Number) {
			double value = ((Number) param).doubleValue();
			return Math.cbrt(value);
		}
		throw new ParseException("Invalid parameter type");
	}

}