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

import java.text.DecimalFormat;

import org.concord.modeler.math.LinearAlgebra;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.modeler.util.FloatQueue;

public class CovarianceMatrix {

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

    private static CovarianceMatrix sharedInstance=new CovarianceMatrix();

    private final static DecimalFormat DECIMAL_FORMAT=new DecimalFormat("#.##E00");

    private double[][] matrix;
    private double[][] vector;
    private double[] eigenv;
    private double[] aux2=new double[4];
    private static boolean debug;

    private CovarianceMatrix(){
	aux2[2]=1.0e-6;
    }

    public static CovarianceMatrix sharedInstance(){
	return sharedInstance;
    }

    public void generateMatrix(AtomicModel model){

	int n=model.getNumberOfAtoms();
	int n2=n+n;
	
	if(matrix==null || n2+1!=matrix.length) matrix=new double[n2+1][n2+1];
	if(vector==null || n2+1!=vector.length) vector=new double[n2+1][n2+1];
	if(eigenv==null || n2+1!=eigenv.length) eigenv=new double[n2+1];
	if(lbound==null || n2+1!=lbound.length) lbound=new double[n2+1];
	if(ubound==null || n2+1!=ubound.length) ubound=new double[n2+1];

	Atom atom;
	FloatQueue[] q=new FloatQueue[n2];
	double[] ave=new double[n2];

	for(int i=0; i<n; i++){
	    atom=model.getAtom(i);
	    q[i+i]=new FloatQueue(atom.rQ.getQueue1());
	    q[i+i+1]=new FloatQueue(atom.rQ.getQueue2());
	}

	int m=q[0].getPointer();
	float xm, ym;
	for(int k=0; k<m; k++){
	    xm=ym=0;
	    for(int i=0; i<n; i++){
		xm+=q[i+i].getData(k);
		ym+=q[i+i+1].getData(k);
	    }
	    xm/=n;
	    ym/=n;
	    for(int i=0; i<n; i++){
		q[i+i].setData(k, q[i+i].getData(k)-xm);
		q[i+i+1].setData(k, q[i+i+1].getData(k)-ym);
	    }
	}

	for(int i=0; i<n2; i++){
	    ave[i]=q[i].getAverage();
	}

	for(int i=1; i<=n2; i++){
	    for(int j=1; j<i; j++){
		matrix[i][j]=matrix[j][i]
		    =DataQueueUtilities.getDotProduct(q[i-1], q[j-1])-ave[i-1]*ave[j-1];
	    }
	    matrix[i][i]=DataQueueUtilities.getDotProduct(q[i-1], q[i-1])-ave[i-1]*ave[i-1];
	}

	if(debug){
	    System.out.println("--------Orginal matrix----------");
	    for(int i=1; i<=n2; i++){
		for(int j=1; j<=n2; j++){
		    System.out.print(DECIMAL_FORMAT.format(matrix[i][j])+", ");
		}
		System.out.println("");
	    }
	}

	//LinearAlgebra.decinv(matrix, 4, aux2);

	if(debug){
	    System.out.println("--------Inverted matrix----------");
	    for(int i=1; i<=n2; i++){
		for(int j=1; j<=n2; j++){
		    System.out.print(DECIMAL_FORMAT.format(matrix[i][j])+", ");
		}
		System.out.println("");
	    }
	}

	/*
	if(debug){
	    LinearAlgebra.decinv(matrix, 4, aux2);
	    System.out.println("--------Checked matrix----------");
	    for(int i=1; i<=n2; i++){
		for(int j=1; j<=n2; j++){
		    System.out.print(DECIMAL_FORMAT.format(matrix[i][j])+", ");
		}
		System.out.println("");
	    }
	}
	*/

	for(int i=1; i<=n2; i++){
	    for(int j=1; j<i; j++){
		vector[i][j]=vector[j][i]=matrix[i][j];
	    }
	    vector[i][i]=matrix[i][i];
	}

	LinearAlgebra.qrisym(vector, n2, eigenv, em);
	LinearAlgebra.symeigimp(n2, matrix, vector, eigenv, lbound, ubound, aux);

	for(int i=1; i<=n2; i++){
	    System.out.print(DECIMAL_FORMAT.format(eigenv[i])+", ");
	    System.out.println("");
	}
	System.out.println("");

    }
    

}