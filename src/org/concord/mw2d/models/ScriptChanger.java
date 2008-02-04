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

class ScriptChanger extends AbstractChange {

	private MDModel model;
	private double min = 0, max = 10, stepSize = 1, value = 0;

	ScriptChanger(MDModel model) {
		this.model = model;
		putProperty(SHORT_DESCRIPTION, "Execute MW script");
	}

	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();
		if (o instanceof JSlider) {
			JSlider source = (JSlider) o;
			if (!source.getValueIsAdjusting()) {
				double x = 1.0;
				Object scale = source.getClientProperty(SCALE);
				if (scale instanceof Double) {
					x = 1.0 / ((Double) scale).doubleValue();
				}
				String script = (String) source.getClientProperty("Script");
				if (script != null) {
					script = script.replaceAll("(?i)%val", source.getValue() * x + "");
					script = script.replaceAll("(?i)%max", source.getMaximum() * x + "");
					script = script.replaceAll("(?i)%min", source.getMinimum() * x + "");
					model.runScript(script);
				}
			}
		}
		else if (o instanceof JSpinner) {
			JSpinner source = (JSpinner) o;
			String script = (String) source.getClientProperty("Script");
			if (script != null) {
				model.runScript(script.replaceAll("(?i)%val", source.getValue() + ""));
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
		return value;
	}

	public String toString() {
		return (String) getProperty(SHORT_DESCRIPTION);
	}

}