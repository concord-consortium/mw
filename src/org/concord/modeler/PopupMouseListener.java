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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Charles Xie
 * 
 */
class PopupMouseListener extends MouseAdapter {

	private boolean popupTrigger;
	private Embeddable embeddable;

	public PopupMouseListener(Embeddable embeddable) {
		this.embeddable = embeddable;
	}

	/*
	 * From JDK doc: Popup menus are triggered differently on different systems. Therefore, isPopupTrigger should be
	 * checked in both mousePressed and mouseReleased for proper cross-platform functionality.
	 */
	public void mousePressed(MouseEvent e) {
		popupTrigger = e.isPopupTrigger();
		int i = embeddable.getPage().getPosition((Component) embeddable);
		if (i != -1)
			embeddable.getPage().setCaretPosition(i);
		e.consume();
	}

	public void mouseReleased(MouseEvent e) {
		if (isTextSelected()) {
			e.consume();
			return;
		}
		if (popupTrigger || e.isPopupTrigger()) {
			if (embeddable.getPopupMenu() == null)
				embeddable.createPopupMenu();
			embeddable.getPopupMenu().show(embeddable.getPopupMenu().getInvoker(), e.getX() + 5, e.getY() + 5);
		}
		e.consume();
	}

	private boolean isTextSelected() {
		if (embeddable instanceof PageMultipleChoice) {
			return ((PageMultipleChoice) embeddable).isTextSelected();
		}
		if (embeddable instanceof BasicPageTextBox) {
			return ((PageTextBox) embeddable).isTextSelected();
		}
		if (embeddable instanceof ImageQuestion) {
			return ((ImageQuestion) embeddable).isTextSelected();
		}
		return false;
	}

}
