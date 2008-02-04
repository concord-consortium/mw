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
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;

import org.concord.modeler.event.AbstractChange;

class LightSourceIntensityChanger extends AbstractChange {

	private AtomicModel model;
	private double min;
	private double max = 10;
	private double stepSize = 1;

	LightSourceIntensityChanger(AtomicModel model) {
		this.model = model;
		putProperty(SHORT_DESCRIPTION, "Intensity of Light Source");
	}

	public void stateChanged(ChangeEvent e) {
		if (model.getLightSource() == null)
			return;
		Object o = e.getSource();
		if (o instanceof JSlider) {
			JSlider source = (JSlider) o;
			if (!source.getValueIsAdjusting()) {
				model.setLightSourceInterval((int) ((200 + (source.getMaximum() - source.getValue()) * 20) / model
						.getTimeStep()));
				model.notifyChange();
			}
		}
		else if (o instanceof JSpinner) {
			JSpinner source = (JSpinner) o;
			Object v = source.getValue();
			if (v instanceof Number) {
				int i = (int) ((Number) v).doubleValue();
				SpinnerModel spinnerModel = source.getModel();
				if (spinnerModel instanceof SpinnerNumberModel) {
					Comparable c = ((SpinnerNumberModel) spinnerModel).getMaximum();
					if (c instanceof Number) {
						double x = ((Number) c).doubleValue();
						model.setLightSourceInterval((int) ((200 + (int) ((x + 1 - i) * 20)) / model.getTimeStep()));
						model.notifyChange();
					}
				}
			}
			else {
				System.err.println("Incorrect spinner value");
			}
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
		if (model.getLightSource() == null)
			return 0;
		return model.getLightSource().getRadiationPeriod() * 0.005;
	}

	public String toString() {
		return (String) getProperty(SHORT_DESCRIPTION);
	}

}