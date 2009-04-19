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

package org.concord.modeler.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextField;

public class PastableTextField extends JTextField {

	/* platform-independent action of Windows' equivalent of mouse right click */
	static boolean isRightClick(MouseEvent e) {
		if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
			return true;
		if (System.getProperty("os.name").startsWith("Mac")) {
			if (e.isControlDown())
				return true;
		}
		return false;
	}

	protected TextComponentPopupMenu popupMenu;

	protected MouseAdapter mouseAdapter = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
			if (isRightClick(e)) {
				PastableTextField.this.requestFocusInWindow();
				if (popupMenu == null)
					popupMenu = new TextComponentPopupMenu(PastableTextField.this);
				popupMenu.show(PastableTextField.this, e.getX(), e.getY());
			}
		}
	};

	public PastableTextField() {
		super();
		addMouseListener(mouseAdapter);
	}

	public PastableTextField(String text) {
		super(text);
		addMouseListener(mouseAdapter);
	}

	public PastableTextField(int columns) {
		super(columns);
		addMouseListener(mouseAdapter);
	}

	public PastableTextField(String text, int columns) {
		super(text, columns);
		addMouseListener(mouseAdapter);
	}

}
