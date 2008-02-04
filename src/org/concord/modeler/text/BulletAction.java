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
import javax.swing.Icon;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TextAction;

class BulletAction extends TextAction {

	BulletAction() {
		super(Page.BULLET);
		putValue(NAME, Page.BULLET);
		putValue(SHORT_DESCRIPTION, "Add bullets to selected paragraphs");
	}

	public void actionPerformed(ActionEvent e) {

		final Page p = (Page) getTextComponent(e);

		Element[] elem = p.getSelectedParagraphs();
		if (elem == null)
			return;

		float currentIndent = 0.0f;
		AttributeSet as = null;
		StyledDocument doc = p.getStyledDocument();
		Element head = null;
		Icon icon = null;

		for (int i = 0; i < elem.length; i++) {

			// find out the current indent of this selected paragraph
			as = elem[i].getAttributes();
			currentIndent = StyleConstants.getLeftIndent(as);
			head = doc.getCharacterElement(elem[i].getStartOffset());
			icon = StyleConstants.getIcon(head.getAttributes());

			if (!(icon instanceof BulletIcon)) {
				SimpleAttributeSet sas = new SimpleAttributeSet(as);
				StyleConstants.setLeftIndent(sas, currentIndent + Page.INDENT_STEP);
				doc.setParagraphAttributes(elem[i].getStartOffset(), elem[i].getEndOffset() - elem[i].getStartOffset(),
						sas, false);
				as = new SimpleAttributeSet();
				StyleConstants.setIcon((MutableAttributeSet) as, BulletIcon.get(BulletIcon.OPEN_SQUARE_BULLET));
				try {
					doc.insertString(elem[i].getStartOffset(), "  ", null);
					doc.insertString(elem[i].getStartOffset(), " ", as);
				}
				catch (BadLocationException ble) {
					ble.printStackTrace(System.err);
				}
				if (p.isEditable())
					p.getSaveReminder().setChanged(true);
			}
			else {
				SimpleAttributeSet sas = new SimpleAttributeSet(as);
				StyleConstants.setLeftIndent(sas, currentIndent - Page.INDENT_STEP > 0 ? currentIndent
						- Page.INDENT_STEP : 0);
				doc.setParagraphAttributes(elem[i].getStartOffset(), elem[i].getEndOffset() - elem[i].getStartOffset(),
						sas, false);
				try {
					doc.remove(elem[i].getStartOffset(), 3);
				}
				catch (BadLocationException ble) {
					ble.printStackTrace(System.err);
				}
				if (p.isEditable())
					p.getSaveReminder().setChanged(true);
			}

		}
	}

}