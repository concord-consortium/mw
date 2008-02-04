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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TextAction;

class TextActionFactory {

	public final static byte NUMBERING = 1;

	private final static float INDENT_STEP = 20;

	private TextActionFactory() {
	}

	static Action create(byte type) {
		switch (type) {
		case NUMBERING:
			return createNumberingAction();
		}
		return null;
	}

	private static Action createNumberingAction() {

		return new TextAction("Numbering") {

			public Object getValue(String key) {
				if (key == null)
					return null;
				if (key.equals(NAME))
					return "Numbering";
				if (key.equals(SHORT_DESCRIPTION))
					return "Add numbers to selected paragraphs";
				return super.getValue(key);
			}

			public void actionPerformed(ActionEvent e) {

				Page p = (Page) getTextComponent(e);
				if (p == null)
					return;
				Element[] elem = p.getSelectedParagraphs();
				if (elem == null)
					return;

				float currentIndent = 0.0f;
				AttributeSet as = null, headAS = null;
				StyledDocument doc = p.getStyledDocument();
				Element head = null;
				Icon icon = null;
				Color bg = p.getBackground(), fg = null;
				String fontFamily = null;
				int fontSize = 10;
				int style = Font.PLAIN;
				boolean isBold = false, isItalic = false;

				for (int i = 0; i < elem.length; i++) {

					// find out the current indent of this selected paragraph
					as = elem[i].getAttributes();
					currentIndent = StyleConstants.getLeftIndent(as);

					// increase indent by INDENT_STEP
					head = doc.getCharacterElement(elem[i].getStartOffset());
					headAS = head.getAttributes();
					icon = StyleConstants.getIcon(headAS);
					fg = StyleConstants.getForeground(headAS);
					fontFamily = StyleConstants.getFontFamily(headAS);
					fontSize = StyleConstants.getFontSize(headAS);
					isBold = StyleConstants.isBold(headAS);
					isItalic = StyleConstants.isItalic(headAS);
					if (isBold && isItalic) {
						style = Font.BOLD | Font.ITALIC;
					}
					else if (isBold && !isItalic) {
						style = Font.BOLD;
					}
					else if (!isBold && isItalic) {
						style = Font.ITALIC;
					}
					else {
						style = Font.PLAIN;
					}

					if (!(icon instanceof BulletIcon)) {
						SimpleAttributeSet sas = new SimpleAttributeSet(as);
						StyleConstants.setLeftIndent(sas, currentIndent + INDENT_STEP);
						doc.setParagraphAttributes(elem[i].getStartOffset(), elem[i].getEndOffset()
								- elem[i].getStartOffset(), sas, false);
						as = new SimpleAttributeSet();
						StyleConstants.setIcon((MutableAttributeSet) as, new BulletIcon.NumberIcon(new Font(fontFamily,
								style, fontSize), bg, fg, i + 1));
						try {
							doc.insertString(elem[i].getStartOffset(), "  ", null);
							doc.insertString(elem[i].getStartOffset(), " ", as);
						}
						catch (BadLocationException ble) {
							ble.printStackTrace(System.err);
						}
					}
					else {
						SimpleAttributeSet sas = new SimpleAttributeSet(as);
						StyleConstants.setLeftIndent(sas, currentIndent - INDENT_STEP > 0 ? currentIndent - INDENT_STEP
								: 0);
						doc.setParagraphAttributes(elem[i].getStartOffset(), elem[i].getEndOffset()
								- elem[i].getStartOffset(), sas, false);
						try {
							doc.remove(elem[i].getStartOffset(), 3);
						}
						catch (BadLocationException ble) {
							ble.printStackTrace(System.err);
						}
					}

				}
			}
		};

	}

}