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

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;

public class HyperlinkLabel extends JLabel {

	private Runnable action;

	public HyperlinkLabel() {
		super();
		addListeners();
	}

	public HyperlinkLabel(Icon image) {
		super(image);
		addListeners();
	}

	public HyperlinkLabel(String text) {
		super(text);
		addListeners();
	}

	public HyperlinkLabel(String text, int horizontalAlignment) {
		super(text, horizontalAlignment);
		addListeners();
	}

	public HyperlinkLabel(String text, Icon icon, int horizontalAlignment) {
		super(text, icon, horizontalAlignment);
		addListeners();
	}

	/**
	 * set the code to be executed when the user clicks this label. The code will be executed within the AWT event
	 * thread.
	 */
	public void setAction(Runnable action) {
		this.action = action;
	}

	private void addListeners() {

		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			public void mouseExited(MouseEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}

			public void mousePressed(MouseEvent e) {
				if (isEnabled() && action != null)
					EventQueue.invokeLater(action);
			}
		});

	}

}
