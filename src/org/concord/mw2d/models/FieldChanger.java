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

import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;

import org.concord.modeler.event.AbstractChange;

class FieldChanger extends AbstractChange {

	private MDModel model;
	private double min;
	private double max;
	private double stepSize;
	private byte type;

	FieldChanger(MDModel model, byte type) {
		this.model = model;
		this.type = type;
		switch (type) {
		case ToggleFieldAction.A_FIELD:
			putProperty(SHORT_DESCRIPTION, "Fictitious field due to acceleration (e.g. centrifugal)");
			max = 0.05;
			stepSize = 0.001;
			break;
		case ToggleFieldAction.G_FIELD:
			putProperty(SHORT_DESCRIPTION, "Gravitational Field");
			max = 0.01;
			stepSize = 0.001;
			break;
		case ToggleFieldAction.E_FIELD:
			putProperty(SHORT_DESCRIPTION, "Electric Field (D.C.)");
			max = 10;
			stepSize = 1;
			break;
		case ToggleFieldAction.B_FIELD:
			putProperty(SHORT_DESCRIPTION, "Magnetic Field");
			max = 1;
			stepSize = 0.1;
			break;
		}
	}

	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();
		if (o instanceof JSlider) {
			JSlider source = (JSlider) o;
			Double scale = (Double) source.getClientProperty(SCALE);
			double s = scale == null ? 1.0 : scale.doubleValue();
			if (!source.getValueIsAdjusting()) {
				switch (type) {
				case ToggleFieldAction.A_FIELD:
					model.setAField(source.getValue() / s);
					break;
				case ToggleFieldAction.G_FIELD:
					model.setGField(source.getValue() / s);
					break;
				case ToggleFieldAction.E_FIELD:
					model.setEField(source.getValue() / s);
					break;
				case ToggleFieldAction.B_FIELD:
					model.setBField(source.getValue() / s);
					break;
				}
			}
		}
		else if (o instanceof JSpinner) {
			Object v = ((JSpinner) o).getValue();
			if (v instanceof Number) {
				switch (type) {
				case ToggleFieldAction.A_FIELD:
					model.setAField(((Number) v).doubleValue());
					break;
				case ToggleFieldAction.G_FIELD:
					model.setGField(((Number) v).doubleValue());
					break;
				case ToggleFieldAction.E_FIELD:
					model.setEField(((Number) v).doubleValue());
					break;
				case ToggleFieldAction.B_FIELD:
					model.setBField(((Number) v).doubleValue());
					break;
				}
			}
			else System.err.println("Incorrect spinner value");
		}
	}

	public double getMinimum() {
		return min;
	}

	public double getMaximum() {
		return max;
	}

	public double getStepSize() {
		return stepSize;
	}

	public double getValue() {
		VectorField f = null;
		switch (type) {
		case ToggleFieldAction.A_FIELD:
			f = model.getNonLocalField(AccelerationalField.class.getName());
			break;
		case ToggleFieldAction.G_FIELD:
			f = model.getNonLocalField(GravitationalField.class.getName());
			break;
		case ToggleFieldAction.E_FIELD:
			f = model.getNonLocalField(ElectricField.class.getName());
			break;
		case ToggleFieldAction.B_FIELD:
			f = model.getNonLocalField(MagneticField.class.getName());
			break;
		}
		if (f == null)
			return 0.0;
		return Math.abs(f.getIntensity());
	}

	public String toString() {
		return (String) getProperty(SHORT_DESCRIPTION);
	}

}