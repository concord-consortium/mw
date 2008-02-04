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

import org.concord.modeler.process.AbstractLoadable;

/** This task realizes the effect of a heat bath: Periodically set the temperature to a set value. */

public class HeatBath extends AbstractLoadable {

	private final static float TMAX = 50000;

	private float expectedValue = 100;
	private transient MolecularModel model;

	public HeatBath() {
	}

	public HeatBath(MolecularModel model) {
		this();
		this.model = model;
	}

	public MolecularModel getModel() {
		return model;
	}

	public void setExpectedTemperature(float temperature) {
		expectedValue = temperature;
	}

	public float getExpectedTemperature() {
		return expectedValue;
	}

	public void changeExpectedTemperature(float percent) {
		if (expectedValue < 0.1f)
			expectedValue = 0.1f;
		expectedValue *= (1 + percent);
		if (expectedValue > TMAX)
			expectedValue = TMAX;
	}

	public void execute() {
		if (model != null)
			model.setTemperature(expectedValue);
	}

	public String getName() {
		return "Heat bath";
	}

	public String toString() {
		return getName();
	}

	/* release dependence on other objects to allow garbage collection */
	void destroy() {
		model = null;
	}

	public String getDescription() {
		return "Periodically set the temperature to the desired value.";
	}

}