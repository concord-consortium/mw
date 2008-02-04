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
import javax.swing.JComboBox;

class FieldDirectionAction extends AbstractAction {

	private MDModel model;
	private byte type;
	private final static String[] EWNS = new String[] { "East", "West", "North", "South" };
	private final static String[] IO = new String[] { "Inward", "Outward" };

	FieldDirectionAction(MDModel model, byte type) {
		this.model = model;
		this.type = type;
		switch (type) {
		case ToggleFieldAction.E_FIELD:
			putValue(NAME, "Electric Field Direction");
			putValue(SHORT_DESCRIPTION, "Electric Field Direction");
			putValue("options", EWNS);
			break;
		case ToggleFieldAction.B_FIELD:
			putValue(NAME, "Magnetic Field Direction");
			putValue(SHORT_DESCRIPTION, "Magnetic Field Direction");
			putValue("options", IO);
			break;
		}
	}

	public Object getValue(String key) {
		if (key.equalsIgnoreCase("state")) {
			switch (type) {
			case ToggleFieldAction.E_FIELD:
				ElectricField ef = (ElectricField) model.getNonLocalField(ElectricField.class.getName());
				if (ef != null) {
					int i = 0;
					switch (ef.getOrientation()) {
					case VectorField.WEST:
						i = 1;
						break;
					case VectorField.NORTH:
						i = 2;
						break;
					case VectorField.SOUTH:
						i = 3;
						break;
					}
					return EWNS[i];
				}
				break;
			case ToggleFieldAction.B_FIELD:
				MagneticField bf = (MagneticField) model.getNonLocalField(MagneticField.class.getName());
				if (bf != null) {
					return bf.getOrientation() == VectorField.INWARD ? IO[0] : IO[1];
				}
				break;
			}
		}
		return super.getValue(key);
	}

	public void actionPerformed(ActionEvent e) {

		switch (type) {
		case ToggleFieldAction.E_FIELD:
			VectorField f = model.getNonLocalField(ElectricField.class.getName());
			if (f == null)
				return;
			Object o = e.getSource();
			if (o instanceof JComboBox) {
				if (!((JComboBox) o).isShowing())
					return;
				String s = (String) (((JComboBox) o).getSelectedItem());
				short orie = VectorField.EAST;
				if (s == EWNS[1])
					orie = VectorField.WEST;
				else if (s == EWNS[2])
					orie = VectorField.NORTH;
				else if (s == EWNS[3])
					orie = VectorField.SOUTH;
				((ElectricField) f).setOrientation(orie);
				model.getView().repaint();
			}
			break;
		case ToggleFieldAction.B_FIELD:
			f = model.getNonLocalField(MagneticField.class.getName());
			if (f == null)
				return;
			o = e.getSource();
			if (o instanceof JComboBox) {
				if (!((JComboBox) o).isShowing())
					return;
				String s = (String) (((JComboBox) o).getSelectedItem());
				short orie = VectorField.INWARD;
				if (s == IO[1])
					orie = VectorField.OUTWARD;
				((MagneticField) f).setOrientation(orie);
				model.getView().repaint();
			}
			break;
		}

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}