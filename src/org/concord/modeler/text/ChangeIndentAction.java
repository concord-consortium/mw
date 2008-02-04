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

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TextAction;

class ChangeIndentAction extends TextAction {

	private Page page;
	private boolean increase;

	ChangeIndentAction(Page page, boolean increase) {
		super(increase ? Page.INCREASE_INDENT : Page.DECREASE_INDENT);
		this.page = page;
		this.increase = increase;
		putValue(NAME, increase ? Page.INCREASE_INDENT : Page.DECREASE_INDENT);
		putValue(SHORT_DESCRIPTION, increase ? "Increase indentation of the current paragraph"
				: "Decrease indentation of the current paragraph");

	}

	public void actionPerformed(ActionEvent e) {
		// find out the current indent of the selected paragraph
		AttributeSet as = page.getParagraphAttributes();
		float currentIndent = StyleConstants.getLeftIndent(as);
		// increase or decrease indent by INDENT_STEP
		StyledDocument doc = page.getStyledDocument();
		Element elem = doc.getParagraphElement(page.getCaretPosition());
		as = elem.getAttributes();
		SimpleAttributeSet sas = new SimpleAttributeSet(as);
		StyleConstants.setLeftIndent(sas, increase ? currentIndent + Page.INDENT_STEP : (currentIndent
				- Page.INDENT_STEP < 0 ? 0 : currentIndent - Page.INDENT_STEP));
		page.setParagraphAttributes(sas, false);
		if (page.isEditable())
			page.getSaveReminder().setChanged(true);
	}

}