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

/** Collects the diagonalization methods. */

public class Diagonalize {

    private Diagonalize(){}
    
    /** <p>solve linear system with tridiagonal <tt>nxn</tt> matrix using Gaussian elimination without pivoting.
	<p>Credit to Tobias von Petersdorff</p>
	@param n NOTE: 1...n is used in all arrays, 0 is unused.
	@param sub   a(i,i-1) = sub[i]  for 2<=i<=n
	@param diag  a(i,i)   = diag[i] for 1<=i<=n
	@param sup a(i,i+1) = sup[i]  for 1<=i<=n-1 (the values sub[1], sup[n] are ignored)
	@param b right hand side vector b[1:n] is overwritten with solution 
    */
    public static void solveTridiag(float[] sub, float[] diag, float[] sup, float[] b, int n){

	int i;
	for(i=2; i<=n; i++){
	    sub[i] = sub[i]/diag[i-1];
	    diag[i] = diag[i] - sub[i]*sup[i-1];
	    b[i] = b[i] - sub[i]*b[i-1];
	}
	b[n] = b[n]/diag[n];
	for(i=n-1; i>=1; i--){
	    b[i] = (b[i] - sup[i]*b[i+1])/diag[i];
	}

    }


}
