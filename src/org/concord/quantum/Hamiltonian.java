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

package org.concord.quantum;

public abstract class Hamiltonian {

    protected double[][] h;
    protected double[] e;
    protected double[][] v;

    public double[][] getMatrix(){
	return h;
    }

    /** returns all eigenvalues, sorted from the lowest to the highest. */
    public double[] getEigenValues(){
	return e;
    }

    /** return all eigenvectors. Eigenvectors are stored columnwise in the two-dimensional
     **  matrix <code>v</code>*/
    public double[][] getEigenVectors(){
	return v;
    }

    /** return the <i>i</i>-th eigenvector. Eigenvectors are stored columnwise in the two-dimensional
     **  matrix <code>v</code>*/
    public double[] getEigenVector(int i){
	if(v==null) return null;
	if(i<0) throw new IllegalArgumentException("i cannot be negative");
	int n=v[0].length;
	double[] x=new double[n];
	for(int m=0; m<n; m++) x[m]=v[m][i];
	return x;
    }

}
