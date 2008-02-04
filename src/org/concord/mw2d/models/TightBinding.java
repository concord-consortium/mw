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

package org.concord.mw2d.models;

import org.concord.modeler.math.LinearAlgebra;
import org.concord.quantum.Hamiltonian;

public class TightBinding extends Hamiltonian {

    private int n;
    private double delta=0.1;
    private static double[] em=new double[6];
    private static double[] aux=new double[6];
    private static double[] ubound, lbound;

    static {

	/** entry: **/
	em[0]=1.0e-6;    // the machine precision
	em[2]=1.0e-5;    // the relative tolerance for the QR iteration
	em[4]=100.0;     // the maximum allowed number of iterations
	aux[0]=0.0;      // the relative precision of the matrix elements
	aux[2]=1.0e-6;   // the relative tolerance for the residual matrix; the iteration ends
	                 // when the maximum absolute value of the residual elements is smaller
	                 // than aux[1]*aux[2]
	aux[4]=10.0;     // the maximum number of iterations allowed

	/** exit:
	    em[1]  --- the infinity norm of the matrix
	    em[3]  --- the maximum absolute value of the codiagonal elements neglected
	    em[5]  --- the number of iterations performed
	    aux[1] --- the infinity norm of the matrix
	    aux[3] --- the maximum absolute element of the residual matrix
	    aux[5] --- the number of iterations
	 **/	

    }

    

    public TightBinding(MolecularModel model){

	n=model.getNumberOfAtoms();
	h=new double[n+1][n+1];
	e=new double[n+1];
	v=new double[n+1][n+1];
	lbound=new double[n+1];
	ubound=new double[n+1];
	
	double xij, yij, rij, dij;
	int i1, j1;

	for(int i=1; i<=n; i++){
	    i1=i-1;
	    //h[i][i]=model.atom[i1].getOrbital().getEnergy();
	    h[i][i]=0;
	    for(int j=1; j<i; j++){
		j1=j-1;
		xij=model.atom[i1].rx-model.atom[j1].rx;
		yij=model.atom[i1].ry-model.atom[j1].ry;
		rij=Math.sqrt(xij*xij+yij*yij);
		dij=0.25*(model.atom[i1].getSigma()+model.atom[j1].getSigma());
		h[i][j]=h[j][i]=v[i][j]=v[j][i]=-Math.exp(-delta*(rij-dij));
	    }
	}

    }

    public void diagonalize(){
	LinearAlgebra.qrisym(v, n, e, em);
	LinearAlgebra.symeigimp(n, h, v, e, lbound, ubound, aux);
    }

}
