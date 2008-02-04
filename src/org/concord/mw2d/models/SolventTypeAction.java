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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;

import org.concord.mw2d.MDView;

class SolventTypeAction extends AbstractAction {

	private MolecularModel model;
	private final static String[] TYPE = new String[] { "Water", "Vacuum", "Oil" };

	SolventTypeAction(MolecularModel model) {
		this.model = model;
		putValue(NAME, "Solvent Type");
		putValue(SHORT_DESCRIPTION, "Solvent Type");
		putValue("options", TYPE);
		String s = MDView.getInternationalText("Water");
		if (s != null)
			TYPE[0] = s;
		s = MDView.getInternationalText("Vacuum");
		if (s != null)
			TYPE[1] = s;
		s = MDView.getInternationalText("Oil");
		if (s != null)
			TYPE[2] = s;
	}

	public Object getValue(String key) {
		if (key.equalsIgnoreCase("state")) {
			int i = 1;
			if (model.solvent != null) {
				switch (model.solvent.getType()) {
				case Solvent.WATER:
					i = 0;
					break;
				case Solvent.OIL:
					i = 2;
					break;
				}
			}
			return TYPE[i];
		}
		return super.getValue(key);
	}

	public void actionPerformed(ActionEvent e) {
		if (!isEnabled())
			return;
		Object o = e.getSource();
		if (!model.changeApprovedByRecorder()) {
			if (o instanceof JComboBox) {
				if (!((JComboBox) o).isShowing())
					return;
				if (model.solvent == null) {
					((JComboBox) o).setSelectedIndex(1);
				}
				else {
					int v = model.solvent.getType();
					if (v <= -1)
						((JComboBox) o).setSelectedIndex(0);
					else if (v == 0)
						((JComboBox) o).setSelectedIndex(1);
					else ((JComboBox) o).setSelectedIndex(2);
				}
			}
			return;
		}
		if (o instanceof JComboBox) {
			if (!((JComboBox) o).isShowing())
				return;
			int n = ((JComboBox) o).getSelectedIndex();
			if (model.solvent == null)
				model.solvent = new Solvent();
			switch (n) {
			case 0:
				model.solvent.setType(Solvent.WATER);
				model.view.setBackground(Solvent.WATER_COLOR);
				break;
			case 1:
				model.solvent.setType(Solvent.VACUUM);
				model.view.setBackground(Color.white);
				break;
			case 2:
				model.solvent.setType(Solvent.OIL);
				model.view.setBackground(Solvent.OIL_COLOR);
				break;
			}
			model.universe.setDielectricConstant(model.solvent.getDielectricConstant());
			model.view.repaint();
			model.notifyChange();
		}
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}