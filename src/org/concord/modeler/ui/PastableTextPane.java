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

import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

import org.concord.modeler.ModelerUtilities;

public class PastableTextPane extends JTextPane {

	protected TextComponentPopupMenu popupMenu;

	protected MouseAdapter mouseAdapter = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
			if (ModelerUtilities.isRightClick(e)) {
				PastableTextPane.this.requestFocus();
				if (popupMenu == null)
					popupMenu = new TextComponentPopupMenu(PastableTextPane.this);
				popupMenu.show(PastableTextPane.this, e.getX(), e.getY());
			}
		}
	};

	public PastableTextPane() {
		super();
		addMouseListener(mouseAdapter);
	}

	public PastableTextPane(StyledDocument doc) {
		super(doc);
		addMouseListener(mouseAdapter);
	}

}
