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

/** This non-mutatable class models a complex double number.
 **
 ** @author Charles Xie
 */

public final class ComplexDouble {

    private double u;
    private double v;

    public ComplexDouble(double x, double y) {
	u=x;
	v=y;
    }

    /** @return the real part of this complex number. */
    public double getReal() {
	return u;
    }

    /** @return the imaginary part of this complex number. */
    public double getImag() {
	return v;
    }

    /** @return the modulus of this complex number. */
    public double getModulus() { 
	return Math.sqrt(u*u+v*v);
    }

    /** @return the square of modulus of this complex number. */
    public double getSquareModulus() {
	return u*u+v*v;
    }

    /** @return the argument of this complex number in radians, in the range from -pi to pi. */
    public double getArgument() {
	return Math.atan2(u, v);
    }

    /** @return the conjugate of this complex number. */
    public ComplexDouble conjugate() {
	return new ComplexDouble(u, -v);
    }

    /** The two square roots of a+bi are (x +yi) and -(x +yi) with y = sqrt((r - a)/2) and x = b/(2.y).
     ** This method returns the first one.
     ** @return a square root of this complex number. */
    public ComplexDouble sqrt1() {
	double r=Math.sqrt(u*u+v*v);
	double y=Math.sqrt((r-u)*0.5);
	return new ComplexDouble(v*0.5/y, y);
    }

    /** The two square roots of a+bi are (x +yi) and -(x +yi) with y = sqrt((r - a)/2) and x = b/(2.y).
     ** This method returns the second one.
     ** @return a square root of this complex number. */
    public ComplexDouble sqrt2() {
	double r=Math.sqrt(u*u+v*v);
	double y=-Math.sqrt((r-u)*0.5);
	return new ComplexDouble(v*0.5/y, y);	
    }

    /** return w+z, w is this complex number. */
    public ComplexDouble add(ComplexDouble z) {
	return new ComplexDouble(u+z.u, v+z.v);
    }

    /** return w-z, w is this complext number. */
    public ComplexDouble subtract(ComplexDouble z) {
	return new ComplexDouble(u-z.u, v-z.v);
    }

    /** return w*z, w is this complext number. */
    public ComplexDouble multiply(ComplexDouble z) {
	return new ComplexDouble(u*z.u-v*z.v, u*z.v+v*z.u);
    }
    
    /** return w/z, w is this complext number. */
    public ComplexDouble divide(ComplexDouble z) {
	double rz=1.0/z.getSquareModulus(); 
	return new ComplexDouble((u*z.u+v*z.v)*rz, (v*z.u-u*z.v)*rz);
    }

}
