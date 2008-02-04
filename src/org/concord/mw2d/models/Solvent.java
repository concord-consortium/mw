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

import java.awt.Color;
import java.io.Serializable;

/**
 * This class models a solvent environment for molecules. It contains a parameter that reflects the watery extent of the
 * solvent, when an effective hydrophobic/hydrophilic interaction is used. A negative value represents water environment
 * (usually -1). A positive one represents oil environment (usually 1). A zero value means vacuum.
 */

public class Solvent implements Serializable {

	public static final short WATER = 1;
	public static final short VACUUM = 0;
	public static final short OIL = -1;
	public static final float DIELECTRIC_WATER = 80;
	public static final float DIELECTRIC_VACUUM = 1;
	public static final float DIELECTRIC_OIL = 10;
	public static final Color WATER_COLOR = new Color(134, 187, 246);
	public static final Color OIL_COLOR = new Color(240, 244, 57);

	private short type = VACUUM;

	public Solvent() {
	}

	public Solvent(short type) {
		this();
		setType(type);
	}

	public void setType(short type) {
		this.type = type;
	}

	public short getType() {
		return type;
	}

	public float getDielectricConstant() {
		if (type == WATER)
			return DIELECTRIC_WATER;
		if (type == OIL)
			return DIELECTRIC_OIL;
		return DIELECTRIC_VACUUM;
	}

	/**
	 * return the temperature factor of the solvent to hydrophobic/hydrophilic forces a protein will experience
	 */
	public double getTemperatureFactor(double temperature) {
		return 0.00001;
		/*
		 * if(temperature!=previousT){ previousF=0.00001*Math.exp(-(temperature-t0)*(temperature-t0)*0.0001);
		 * previousT=temperature; } return previousF;
		 */
	}

}