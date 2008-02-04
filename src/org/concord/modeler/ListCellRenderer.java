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

package org.concord.modeler;

import java.awt.Component;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JList;

import org.concord.modeler.ui.TabListCellRenderer;

class ListCellRenderer extends TabListCellRenderer {

	private ImageIcon icon, icon2;

	ListCellRenderer() {
		icon = new ImageIcon(getClass().getResource("images/Person.gif"));
		icon2 = new ImageIcon(getClass().getResource("images/SmilingFace.gif"));
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		if (cellHasFocus) {
			setIcon(icon2);
		}
		else {
			setIcon(icon);
		}

		String text = getText();
		int whiteSpace = text.indexOf(" ");
		if (whiteSpace != -1) {
			try {
				text = text.substring(whiteSpace + 1, text.length()) + "\t"
						+ new Date(Long.parseLong(text.substring(0, whiteSpace)));
			}
			catch (NumberFormatException e) {
				// do nothing
				return this;
			}
			setText(text);
		}

		return this;

	}

}