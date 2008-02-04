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

package org.concord.mw3d.models;

import java.lang.reflect.Array;

final class Util {

	private Util() {
	}

	static int[] doubleLength(int[] a) {
		return setLength(a, 2 * a.length);
	}

	private static int[] setLength(int[] a, int newLength) {
		int[] t = new int[newLength];
		int oldLength = a.length;
		System.arraycopy(a, 0, t, 0, oldLength < newLength ? oldLength : newLength);
		return t;
	}

	static float[] doubleLength(float[] a) {
		return setLength(a, 2 * a.length);
	}

	private static float[] setLength(float[] a, int newLength) {
		float[] t = new float[newLength];
		int oldLength = a.length;
		System.arraycopy(a, 0, t, 0, oldLength < newLength ? oldLength : newLength);
		return t;
	}

	static double[] doubleLength(double[] a) {
		return setLength(a, 2 * a.length);
	}

	private static double[] setLength(double[] a, int newLength) {
		double[] t = new double[newLength];
		int oldLength = a.length;
		System.arraycopy(a, 0, t, 0, oldLength < newLength ? oldLength : newLength);
		return t;
	}

	static Atom[] doubleLength(Atom[] a) {
		return setLength(a, 2 * a.length);
	}

	private static Atom[] setLength(Atom[] a, int newLength) {
		Atom[] t = new Atom[newLength];
		int oldLength = a.length;
		System.arraycopy(a, 0, t, 0, oldLength < newLength ? oldLength : newLength);
		return t;
	}

	static Object doubleLength(Object array) {
		return setLength(array, 2 * Array.getLength(array));
	}

	private static Object setLength(Object array, int newLength) {
		Object t = Array.newInstance(array.getClass().getComponentType(), newLength);
		int oldLength = Array.getLength(array);
		System.arraycopy(array, 0, t, 0, oldLength < newLength ? oldLength : newLength);
		return t;
	}

}
