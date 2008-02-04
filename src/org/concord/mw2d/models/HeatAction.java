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

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;

class HeatAction extends AbstractAction {

	private MDModel model;

	HeatAction(MDModel model, boolean heat) {
		this.model = model;
		putValue(SMALL_ICON, IconPool.getIcon(heat ? "heat" : "cool"));
		putValue(NAME, heat ? "Heat" : "Cool");
		putValue(SHORT_DESCRIPTION, heat ? "Heat the system up" : "Cool the system down");
		putValue("increment", new Double(heat ? 0.01 : -0.01));
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		Object o = getValue("increment");
		if (o instanceof Double) {
			model.transferHeat(((Double) o).doubleValue());
			model.getView().repaint();
		}
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}