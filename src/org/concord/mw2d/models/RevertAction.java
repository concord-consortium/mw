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

import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.concord.modeler.process.Executable;
import org.concord.mw2d.ModelAction;

/**
 * revert the model's state to that before it is run. Note that this is different from <code>reload()</code>.
 * 
 * @see org.concord.mw2d.models.MDModel#reload
 */

class RevertAction extends ModelAction {

	RevertAction(MDModel m) {

		super(m);

		setExecutable(new Executable() {
			public void execute() {
				myModel.revert();
				setEnabled(false);
			}
		});

		putValue(SMALL_ICON, new ImageIcon(ModelAction.class.getResource("images/reload.gif")));
		putValue(NAME, "Revert to the State Before Running");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_V));
		putValue(SHORT_DESCRIPTION, "Revert to the state before running");
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_R, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK,
				true));

	}

}