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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JToggleButton;

import org.concord.modeler.ModelerUtilities;

class ToggleFieldAction extends AbstractAction {

	final static byte A_FIELD = 0x00;
	final static byte G_FIELD = 0x01;
	final static byte E_FIELD = 0x02;
	final static byte B_FIELD = 0x03;

	private MDModel model;
	private byte type;
	private VectorField gField0;
	private VectorField aField0;
	private VectorField eField0;
	private VectorField bField0;

	ToggleFieldAction(MDModel model, byte type) {
		this.model = model;
		this.type = type;
		switch (type) {
		case A_FIELD:
			putValue(NAME, "Accelerational Field");
			putValue(SHORT_DESCRIPTION, "Fictitious field due to acceleration (e.g. centrifugal)");
			break;
		case G_FIELD:
			putValue(NAME, "Gravitational Field");
			putValue(SHORT_DESCRIPTION, "Gravitational Field");
			break;
		case E_FIELD:
			putValue(NAME, "Electric Field");
			putValue(SHORT_DESCRIPTION, "Electric Field");
			break;
		case B_FIELD:
			putValue(NAME, "Magnetic Field");
			putValue(SHORT_DESCRIPTION, "Magnetic Field");
			break;
		}
	}

	public Object getValue(String key) {
		if (key.equalsIgnoreCase("state")) {
			switch (type) {
			case A_FIELD:
				return model.getNonLocalField(AccelerationalField.class.getName()) != null;
			case G_FIELD:
				return model.getNonLocalField(GravitationalField.class.getName()) != null;
			case E_FIELD:
				return model.getNonLocalField(ElectricField.class.getName()) != null;
			case B_FIELD:
				return model.getNonLocalField(MagneticField.class.getName()) != null;
			}
		}
		return super.getValue(key);
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		Object o = e.getSource();
		if (o instanceof JToggleButton) {
			JToggleButton tb = (JToggleButton) o;
			if (tb.isSelected()) {
				switch (type) {
				case A_FIELD:
					if (aField0 == null)
						aField0 = new AccelerationalField(model.getView().getBounds());
					model.addNonLocalField(aField0);
					break;
				case G_FIELD:
					if (gField0 == null)
						gField0 = new GravitationalField(model.getView().getBounds());
					model.addNonLocalField(gField0);
					break;
				case E_FIELD:
					if (eField0 == null)
						eField0 = new ElectricField(model.getView().getBounds());
					model.addNonLocalField(eField0);
					break;
				case B_FIELD:
					if (bField0 == null)
						bField0 = new MagneticField(model.getView().getBounds());
					model.addNonLocalField(bField0);
					break;
				}
			}
			else {
				switch (type) {
				case A_FIELD:
					aField0 = model.getNonLocalField(AccelerationalField.class.getName());
					if (aField0 != null)
						model.removeNonLocalField(aField0);
					break;
				case G_FIELD:
					gField0 = model.getNonLocalField(GravitationalField.class.getName());
					if (gField0 != null)
						model.removeNonLocalField(gField0);
					break;
				case E_FIELD:
					eField0 = model.getNonLocalField(ElectricField.class.getName());
					if (eField0 != null)
						model.removeNonLocalField(eField0);
					break;
				case B_FIELD:
					bField0 = model.getNonLocalField(MagneticField.class.getName());
					if (bField0 != null)
						model.removeNonLocalField(bField0);
					break;
				}
			}
			model.getView().repaint();
		}
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}