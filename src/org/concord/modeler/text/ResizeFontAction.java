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
import java.awt.event.KeyEvent;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

class ResizeFontAction extends AbstractAction {

	private Page page;
	private boolean increase;

	ResizeFontAction(Page page, boolean increase) {
		super();
		this.page = page;
		this.increase = increase;
		if (increase) {
			putValue(NAME, Page.INCREASE_FONT_SIZE);
			putValue(SHORT_DESCRIPTION, "Increase font size of the selected text by one");
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, KeyEvent.ALT_MASK, true));
		}
		else {
			putValue(NAME, Page.DECREASE_FONT_SIZE);
			putValue(SHORT_DESCRIPTION, "Decrease font size of the selected text by one");
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, KeyEvent.ALT_MASK, true));
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (page.getSelectedText() == null)
			return;
		Element el = page.getStyledDocument().getCharacterElement(page.getSelectionEnd() - 1);
		AttributeSet as = el.getAttributes();
		Enumeration en = as.getAttributeNames();
		Object key, val;
		int fs = -1;
		while (en.hasMoreElements()) {
			key = en.nextElement();
			if (key.toString().equals(StyleConstants.FontSize.toString())) {
				val = as.getAttribute(key);
				if (val instanceof Integer) {
					fs = ((Integer) val).intValue();
				}
			}
		}
		if (fs != -1) {
			SimpleAttributeSet sas = new SimpleAttributeSet(as);
			StyleConstants.setFontSize(sas, increase ? fs++ : fs--);
			page.setCharacterAttributes(sas, false);
			page.getSaveReminder().setChanged(true);
		}
	}

}