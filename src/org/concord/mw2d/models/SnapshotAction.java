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
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.ui.IconPool;

class SnapshotAction extends AbstractAction {

	private MDModel model;
	private boolean withDescription;

	SnapshotAction(MDModel model, boolean withDescription) {
		this.model = model;
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		putValue(SMALL_ICON, IconPool.getIcon("camera"));
		putValue(NAME, withDescription ? "Take a Snapshot" : "Take a Snapshot Without Description");
		putValue(SHORT_DESCRIPTION, withDescription ? "Take a snapshot" : "Take a snapshot without description");
		this.withDescription = withDescription;
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem) && ModelerUtilities.stopFiring(e))
			return;
		model.notifyPageComponentListeners(new PageComponentEvent(model.getView(),
				withDescription ? PageComponentEvent.SNAPSHOT_TAKEN : PageComponentEvent.SNAPSHOT_TAKEN_NODESCRIPTION));
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}