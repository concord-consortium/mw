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

/** methods selected from Hang T. Lau, "A numerical library in Java for scientists and engineers".**/

final class Basic {

    public static void ichrow(int l, int u, int i, int j, double a[][]){
	double r;
	for (; l<=u; l++) {
	    r=a[i][l];
	    a[i][l]=a[j][l];
	    a[j][l]=r;
	}
    }

    public static void ichcol(int l, int u, int i, int j, double a[][]){
	double r;
	for (; l<=u; l++) {
	    r=a[l][i];
	    a[l][i]=a[l][j];
	    a[l][j]=r;
	}
    }

    public static double mattam(int l, int u, int i, int j, double a[][], double b[][]){
	int k;
	double s=0.0;
	for (k=l; k<=u; k++) s += a[i][k]*b[j][k];
	return (s);
    }

    public static double matvec(int l, int u, int i, double a[][], double b[]){
	int k;
	double s=0.0;
	for (k=l; k<=u; k++) s += a[i][k]*b[k];
	return s;
    }

    public static void dupcolvec(int l, int u, int j, double a[][], double b[]){
	for (; l<=u; l++) a[l][j]=b[l];
    }

    public static void dupvecrow(int l, int u, int i, double a[], double b[][]){
	for (; l<=u; l++) a[l]=b[i][l];
    }

    public static double onenrmrow(int l, int u, int i, double a[][]) {
	double sum=0.0;
	for (; l<=u; l++) sum += Math.abs(a[i][l]);
	return sum;
    }

    public static double infnrmmat(int lr, int ur, int lc, int uc, int kr[], double a[][]) {
	double r, max=0.0;
	kr[0]=lr;
	for (; lr<=ur; lr++) {
	    r=onenrmrow(lc,uc,lr,a);
	    if (r > max) {
		max=r;
		kr[0]=lr;
	    }
	}
	return max;
    }

    public static double vecvec(int l, int u, int shift, double a[], double b[]) {
	int k;
	double s=0.0;
	for (k=l; k<=u; k++) s += a[k]*b[k+shift];
	return s;
    }

    public static double tammat(int l, int u, int i, int j, double a[][], double b[][]) {
	int k;
	double 	s=0.0;
	for (k=l; k<=u; k++) s += a[k][i]*b[k][j];
	return s;
    }

    public static double matmat(int l, int u, int i, int j, double a[][], double b[][]) {
	int k;
	double s=0.0;
	for (k=l; k<=u; k++) s += a[i][k]*b[k][j];
	return s;
    }

    public static void elmveccol(int l, int u, int i, double a[], double b[][], double x){
	for (; l<=u; l++) a[l] += b[l][i]*x;
    }

    public static double tamvec(int l, int u, int i, double a[][], double b[]) {
	int k;
	double s=0.0;
	for (k=l; k<=u; k++) s += a[k][i]*b[k];
	return s;
    }

    public static void elmcol(int l, int u, int i, int j, double a[][], double b[][], double x) {
	for (; l<=u; l++) a[l][i] += b[l][j]*x;
    }

    public static void elmcolvec(int l, int u, int i, double a[][], double b[], double x) {
	for (; l<=u; l++) a[l][i] += b[l]*x;
    }

    public static void rotcol(int l, int u, int i, int j, double a[][], double c, double s){
	double x, y;
	for (; l<=u; l++) {
	    x=a[l][i];
	    y=a[l][j];
	    a[l][i]=x*c+y*s;
	    a[l][j]=y*c-x*s;
	}
    }

}
