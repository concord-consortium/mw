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

class ImportAction extends AbstractAction {

	private MDModel model;

	ImportAction(MDModel model) {
		this.model = model;
		putValue(NAME, "Import Model");
		putValue(SHORT_DESCRIPTION, "Import a model");
	}

	public void actionPerformed(ActionEvent e) {
		if (!isEnabled())
			return;
		Object o = e.getSource();
		if (o instanceof JComboBox) {
			if (!((JComboBox) o).isShowing())
				return;
			final String s = (String) (((JComboBox) o).getSelectedItem());
			if (s == null)
				return;
			if (s.toLowerCase().endsWith(".mml") || s.toLowerCase().endsWith(".gbl")) {
				model.runScript("load " + s);
			}
		}
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}