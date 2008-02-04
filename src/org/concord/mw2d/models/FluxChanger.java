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

class FluxChanger extends AbstractChange {

	private AtomicModel model;
	private double min = 10;
	private double max = 2010;
	private double stepSize = 10;

	FluxChanger(AtomicModel model) {
		this.model = model;
		putProperty(SHORT_DESCRIPTION, "Flux of flow");
	}

	public void stateChanged(ChangeEvent e) {
		if (model.atomSource == null)
			return;
		Object o = e.getSource();
		if (o instanceof JSlider) {
			JSlider source = (JSlider) o;
			Double scale = (Double) source.getClientProperty(SCALE);
			double s = scale == null ? 1.0 : scale.doubleValue();
			if (!source.getValueIsAdjusting()) {
				model.atomSource.setInterval((int) (source.getValue() / s));
				model.notifyChange();
			}
		}
		else if (o instanceof JSpinner) {
			Object v = ((JSpinner) o).getValue();
			if (v instanceof Number) {
				model.atomSource.setInterval(((Number) v).intValue());
				model.notifyChange();
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
		if (model.atomSource == null)
			return 0;
		return model.atomSource.getInterval();
	}

	public String toString() {
		return (String) getProperty(SHORT_DESCRIPTION);
	}

}