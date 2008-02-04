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

package org.concord.modeler.text;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.concord.modeler.ModelerUtilities;

class HintAction extends AbstractAction {

	private Page page;
	private static ImageIcon icon;

	HintAction(Page page) {
		super();
		this.page = page;
		putValue(NAME, "Hint");
		putValue(SHORT_DESCRIPTION, "Hint");
		if (icon == null)
			icon = new ImageIcon(Page.class.getResource("images/Information24.gif"));
		putValue(SMALL_ICON, icon);
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		AbstractButton button = (AbstractButton) e.getSource();
		Object hint = button.getClientProperty("hint");
		if (hint instanceof String) {
			if (((String) hint).indexOf("<html>") != -1) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), new JLabel((String) hint),
						"Hint", JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), new JLabel("<html>"
						+ ((String) hint).replaceAll("\n", "<br>") + "</html>"), "Hint",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}