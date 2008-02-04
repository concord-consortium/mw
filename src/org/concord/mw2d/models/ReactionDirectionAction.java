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
import javax.swing.JMenuItem;

class ReactionDirectionAction extends AbstractAction {

	private ReactionModel model;
	private final static String[] OPTIONS = new String[] { "Equilibrium position at the right",
			"Equilibrium position near the right", "Equilibrium position in the middle",
			"Equilibrium position near the left", "Equilibrium position at the left" };

	ReactionDirectionAction(ReactionModel model) {
		this.model = model;
		putValue(NAME, "Reaction Direction");
		putValue(SHORT_DESCRIPTION, "Reaction Direction");
		putValue("options", OPTIONS);
	}

	public Object getValue(String key) {
		if (key.equalsIgnoreCase("state")) {
			if (model.type instanceof Reaction.A2_B2__2AB) {
				int i = 0;
				if (model.type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue() == 0.1
						&& model.type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue() == 0.1
						&& model.type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue() == 2.0)
					i = 0;
				else if (model.type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue() == 0.3
						&& model.type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue() == 0.3
						&& model.type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue() == 0.4)
					i = 1;
				else if (model.type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue() == 0.5
						&& model.type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue() == 0.5
						&& model.type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue() == 0.5)
					i = 2;
				else if (model.type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue() == 0.4
						&& model.type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue() == 0.4
						&& model.type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue() == 0.3)
					i = 3;
				else if (model.type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue() == 2.0
						&& model.type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue() == 2.0
						&& model.type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue() == 0.1)
					i = 4;
				return OPTIONS[i];
			}
		}
		return super.getValue(key);
	}

	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		String s = null;
		if (o instanceof JComboBox)
			s = (String) (((JComboBox) o).getSelectedItem());
		else if (o instanceof JMenuItem)
			s = ((JMenuItem) o).getText();
		if (s == null)
			return;
		boolean b = false;
		if (model.type instanceof Reaction.A2_B2__2AB) {
			if (s.equals(OPTIONS[0])) {
				model.type.putParameter(Reaction.A2_B2__2AB.VAA, new Double(0.1));
				model.type.putParameter(Reaction.A2_B2__2AB.VBB, new Double(0.1));
				model.type.putParameter(Reaction.A2_B2__2AB.VAB, new Double(2.0));
			}
			else if (s.equals(OPTIONS[1])) {
				model.type.putParameter(Reaction.A2_B2__2AB.VAA, new Double(0.3));
				model.type.putParameter(Reaction.A2_B2__2AB.VBB, new Double(0.3));
				model.type.putParameter(Reaction.A2_B2__2AB.VAB, new Double(0.4));
			}
			else if (s.equals(OPTIONS[2])) {
				model.type.putParameter(Reaction.A2_B2__2AB.VAA, new Double(0.5));
				model.type.putParameter(Reaction.A2_B2__2AB.VBB, new Double(0.5));
				model.type.putParameter(Reaction.A2_B2__2AB.VAB, new Double(0.5));
			}
			else if (s.equals(OPTIONS[3])) {
				model.type.putParameter(Reaction.A2_B2__2AB.VAA, new Double(0.4));
				model.type.putParameter(Reaction.A2_B2__2AB.VBB, new Double(0.4));
				model.type.putParameter(Reaction.A2_B2__2AB.VAB, new Double(0.3));
			}
			else if (s.equals(OPTIONS[4])) {
				model.type.putParameter(Reaction.A2_B2__2AB.VAA, new Double(2.0));
				model.type.putParameter(Reaction.A2_B2__2AB.VBB, new Double(2.0));
				model.type.putParameter(Reaction.A2_B2__2AB.VAB, new Double(0.1));
			}
			b = true;
		}
		if (b) {
			model.changeChemicalEnergies();
		}
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}