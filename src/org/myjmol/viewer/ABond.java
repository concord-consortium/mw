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
package org.myjmol.viewer;

import org.myjmol.g3d.Graphics3D;

/*
 * @author Charles Xie
 * 
 */
class ABond {

	int atom1 = -1;
	int atom2 = -1;
	int atom3 = -1;
	short colix = Graphics3D.GRAY;

	ABond(int i, int j, int k) {
		atom1 = i;
		atom2 = j;
		atom3 = k;
	}

	public String toString() {
		return "(" + atom1 + "," + atom2 + "," + atom3 + ")";
	}

}