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

public class SimpleMath {

    private SimpleMath(){}
	
    /** @return the sign of the input double, 0 if it is in the
     *  interval <tt>[-10*Double.MIN_VALUE, 10*Double.MIN_VALUE]</tt>.
     *  @see java.lang.Double
     */
    public static int sign(double x){
	int i;
	if(x>10.0*Double.MIN_VALUE) {
	    i=1;
	} else if(x<-10.0*Double.MIN_VALUE) {
	    i=-1;
	} else {
	    i=0;
	}
	return i;
    }

    /** @return the sign of the input float, 0 if it is in the
     *  interval <tt>[-10*Float.MIN_VALUE, 10*Float.MIN_VALUE]</tt>.
     *  @see java.lang.Float
     */
    public static int sign(float x){
	int i;
	if(x>10.0f*Float.MIN_VALUE) {
	    i=1;
	} else if(x<-10.0f*Float.MIN_VALUE) {
	    i=-1;
	} else {
	    i=0;
	}
	return i;
    }

    /** @return the sign of the input float, 0 if it is 0. */
    public static int sign(int x){
	int i;
	if(x>0) {
	    i=1;
	} else if(x<0) {
	    i=-1;
	} else {
	    i=0;
	}
	return i;
    }

}

