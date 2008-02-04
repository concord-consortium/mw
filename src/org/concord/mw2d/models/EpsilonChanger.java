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
import javax.swing.event.ChangeListener;

import org.concord.modeler.event.AbstractChange;

class EpsilonChanger extends AbstractChange {

	private byte element;
	private AtomicModel model;
	private double min;
	private double max = 0.5;
	private double stepSize = 0.05;

	EpsilonChanger(AtomicModel model, byte element) {
		this.model = model;
		this.element = element;
		putProperty(SHORT_DESCRIPTION, "Element " + model.getElement(element).getName() + ": Epsilon");
	}

	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();
		if (o instanceof JSlider) {
			final JSlider source = (JSlider) o;
			Double scale = (Double) source.getClientProperty(SCALE);
			final double s = scale == null ? 1.0 : scale.doubleValue();
			if (!source.getValueIsAdjusting()) {
				if (model.changeApprovedByRecorder()) {
					model.getElement(element).setEpsilon(source.getValue() / s);
				}
				else {
					setSliderValue(source, model.getElement(element).getEpsilon() * s);
				}
			}
		}
		else if (o instanceof JSpinner) {
			Object v = ((JSpinner) o).getValue();
			if (v instanceof Number) {
				model.getElement(element).setEpsilon(((Number) v).doubleValue());
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
		return model.getElement(element).getEpsilon();
	}

	public String toString() {
		return (String) getProperty(SHORT_DESCRIPTION);
	}

	/** set the value of a slider without notifying its change listeners */
	static void setSliderValue(JSlider source, double value) {
		ChangeListener[] cl = source.getChangeListeners();
		if (cl != null) {
			for (int i = 0; i < cl.length; i++)
				source.removeChangeListener(cl[i]);
		}
		source.setValue((int) value);
		source.repaint();
		if (cl != null) {
			for (int i = 0; i < cl.length; i++)
				source.addChangeListener(cl[i]);
		}
	}

}