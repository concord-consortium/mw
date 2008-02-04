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
 * All function classes must implement this interface to ensure that the run() method is implemented.
 */
public interface PostfixMathCommandI {
	/**
	 * Run the function on the stack. Pops the arguments from the stack, and pushes the result on the top of the stack.
	 */
	public void run(Stack aStack) throws ParseException;

	/**
	 * Returns the number of required parameters, or -1 if any number of parameters is allowed.
	 */
	public int getNumberOfParameters();

	/**
	 * Sets the number of current number of parameters used in the next call of run(). This method is only called when
	 * the reqNumberOfParameters is -1.
	 */
	public void setCurNumberOfParameters(int n);
}
