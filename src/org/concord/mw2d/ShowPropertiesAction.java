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

package org.concord.mw2d;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;

class ShowPropertiesAction extends AbstractAction {

	private AtomisticView view;
	private final static String[] TYPE = new String[] { "None", "Charge", "Hydrophobicity", "Lego" };

	ShowPropertiesAction(AtomisticView view) {
		super();
		this.view = view;
		putValue(NAME, "Show Properties");
		putValue(SHORT_DESCRIPTION, "Show Properties");
		putValue("options", TYPE);
		String s = MDView.getInternationalText("ShowNone");
		if (s != null)
			TYPE[0] = s;
		s = MDView.getInternationalText("ShowCharge");
		if (s != null)
			TYPE[1] = s;
		s = MDView.getInternationalText("ShowHydrophobicity");
		if (s != null)
			TYPE[2] = s;
	}

	public Object getValue(String key) {
		if (key.equalsIgnoreCase("state"))
			return view.getColorCoding();
		return super.getValue(key);
	}

	public void actionPerformed(ActionEvent e) {
		if (!isEnabled())
			return;
		Object o = e.getSource();
		if (o instanceof JComboBox) {
			if (!((JComboBox) o).isShowing())
				return;
			String s = null;
			switch (((JComboBox) o).getSelectedIndex()) {
			case 0:
				s = "None";
				break;
			case 1:
				s = "Charge";
				break;
			case 2:
				s = "Hydrophobicity";
				break;
			case 3:
				s = "Lego";
				break;
			}
			if (s != null) {
				view.setColorCoding(s);
				if (view.getUseJmol())
					view.refreshJmol();
				view.repaint();
				view.model.notifyChange();
			}
		}
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}