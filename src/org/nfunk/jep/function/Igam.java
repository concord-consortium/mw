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

/** This class computes the incomplete gamma function.
*   @author Connie Chen
*/

public class Igam extends PostfixMathCommand {
	
	public Igam() {
		numberOfParameters = 2;
	}
	
	public String toString(){
		return "The incomplete gamma function";
	}
	
	@SuppressWarnings("unchecked")
	public void run(Stack stack) throws ParseException {
		
		// Check if stack is null
		if (null == stack) 
			throw new ParseException("Stack argument null");
		
		double a=0;
		double x=0;
		
		// get the parameter from the stack
		Object o=stack.pop();
		if (o instanceof Number)   {
			x = ((Number)o).doubleValue();
		} else {
			throw new ParseException("Invalid parameter type");
		}
		
		o=stack.pop();
		if (o instanceof Number)   {
			a = ((Number)o).doubleValue();
		} else {
			throw new ParseException("Invalid parameter type");
		}
		
		// push the result on the inStack
		stack.push(new Double(compute(a, x)));
		
	}
	
	/**
	* @param a double value
	* @param x double value
	* @return the Incomplete Gamma function.
	* Converted to Java from<BR>
	* Cephes Math Library Release 2.2:  July, 1992<BR>
	* Copyright 1984, 1987, 1989, 1992 by Stephen L. Moshier<BR>
	* Direct inquiries to 30 Frost Street, Cambridge, MA 02140<BR>
	**/
	public static double compute(double a, double x) throws ArithmeticException {
		
		if( x <= 0 || a <= 0 ) return 0.0;
		
		if( x > 1.0 && x > a ) return 1.0 - Igamc.compute(a,x);
		
    final double MACHEP =  1.11022302462515654042E-16;
		final double MAXLOG =  7.09782712893383996732E2;

		double ans, ax, c, r;
		
		/* Compute  x**a * exp(-x) / gamma(a)  */
		ax = a * Math.log(x) - x - LnGamma.compute(a);
		if( ax < -MAXLOG ) return( 0.0 );
		
		ax = Math.exp(ax);
		
		/* power series */
		r = a;
		c = 1.0;
		ans = 1.0;
		
		do {
			r += 1.0;
			c *= x/r;
			ans += c;
		}
		while( c/ans > MACHEP );
		
		return( ans * ax/a );
		
	}
	
}
