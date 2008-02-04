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
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.concord.modeler.ModelerUtilities;

class ScriptAction extends AbstractAction {

	private MDModel model;

	ScriptAction(MDModel model) {
		this.model = model;
		putValue(NAME, "Execute MW Script");
		putValue(SHORT_DESCRIPTION, "Execute MW script");
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		Object o = e.getSource();
		if (o instanceof JCheckBox) {
			JCheckBox cb = (JCheckBox) o;
			if (cb.isSelected()) {
				Object o2 = cb.getClientProperty("selection script");
				if (o2 instanceof String) {
					String s = (String) o2;
					if (!s.trim().equals(""))
						model.runScript(s);
				}
			}
			else {
				Object o2 = cb.getClientProperty("deselection script");
				if (o2 instanceof String) {
					String s = (String) o2;
					if (!s.trim().equals(""))
						model.runScript(s);
				}
			}
		}
		else if (o instanceof AbstractButton) {
			Object o2 = ((AbstractButton) o).getClientProperty("script");
			if (o2 instanceof String) {
				String s = (String) o2;
				if (!s.trim().equals(""))
					model.runScript(s);
			}
		}
		else if (o instanceof JComboBox) {
			JComboBox cb = (JComboBox) o;
			Object s = cb.getClientProperty("script" + cb.getSelectedIndex());
			if (s == null)
				return;
			model.runScript((String) s);
		}
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}