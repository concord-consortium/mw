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

public class CatmullRom {

	private CatmullRom() {
	}

	public final static double getBendingFunction(int i, double u, double tension) {

		if (i < -2 || i > 1)
			throw new IllegalArgumentException("i cannot be " + i);
		if (u < 0.0 || u > 1.0)
			throw new IllegalArgumentException("u=" + u + " is outside [0,1]");

		switch (i) {

		case -2:
			return u * tension * (-1 + u * (2 - u));

		case -1:
			return 1 + u * u * ((tension - 3) + (2 - tension) * u);

		case 0:
			return u * (tension + u * ((3 - 2 * tension) + (tension - 2) * u));

		case 1:
			return tension * u * u * (-1 + u);

		}

		return 0.0;

	}

}
