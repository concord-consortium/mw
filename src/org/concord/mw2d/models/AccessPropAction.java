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
import javax.swing.KeyStroke;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;

class AccessPropAction extends AbstractAction {

	private MDModel model;

	AccessPropAction(MDModel model) {
		this.model = model;
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		putValue(SMALL_ICON, IconPool.getIcon("properties"));
		putValue(NAME, "Access Model Properties");
		putValue(SHORT_DESCRIPTION, "Access model properties");
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_M, KeyEvent.ALT_MASK | KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_M,
				KeyEvent.ALT_MASK | KeyEvent.CTRL_MASK, true));
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		model.setModelProperties();
		model.getModelProperties().setModel(model);
		model.getModelProperties().setLocationRelativeTo(model.getView());
		model.getModelProperties().setVisible(true);
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}