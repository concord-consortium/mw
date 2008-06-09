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
package org.jmol.api;

import java.io.Serializable;

import org.myjmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
public abstract class Attachment implements Serializable {

	public final static byte ATOM_HOST = 0;
	public final static byte BOND_HOST = 1;

	private byte hostType = ATOM_HOST;
	private int keyRgb = Graphics3D.getArgb(Graphics3D.GOLD);

	public void setKeyRgb(int i) {
		keyRgb = i;
	}

	public int getKeyRgb() {
		return keyRgb;
	}

	public void setHostType(byte b) {
		hostType = b;
	}

	public byte getHostType() {
		return hostType;
	}

}
