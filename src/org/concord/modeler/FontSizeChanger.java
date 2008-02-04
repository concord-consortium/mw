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

import java.util.Enumeration;

import javax.swing.text.AbstractDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.concord.modeler.text.Page;

class FontSizeChanger {

	/** traverse the document and change the font size of each character element for the specified page. */
	public static void step(Page page, int step) {

		StyledDocument doc = page.getStyledDocument();
		AbstractDocument.BranchElement section = (AbstractDocument.BranchElement) doc.getDefaultRootElement();

		if (step < 0 && isSmallest(section))
			return;

		Enumeration enum1 = section.children();
		AbstractDocument.BranchElement paragraph = null;
		AbstractDocument.LeafElement content = null;
		Enumeration enum2 = null, enum3 = null;
		Object name = null, attr = null;
		int fontSize = 0;
		boolean fontSizeSet = false;
		MutableAttributeSet mas = null;

		while (enum1.hasMoreElements()) {

			paragraph = (AbstractDocument.BranchElement) enum1.nextElement();
			enum2 = paragraph.children();

			while (enum2.hasMoreElements()) {

				content = (AbstractDocument.LeafElement) enum2.nextElement();
				enum3 = content.getAttributeNames();
				mas = new SimpleAttributeSet(content.getAttributes());

				fontSizeSet = false;

				while (enum3.hasMoreElements()) {

					name = enum3.nextElement();
					attr = content.getAttribute(name);

					if (name.toString().equals(StyleConstants.FontSize.toString())) {
						fontSize = ((Integer) attr).intValue() + step;
						if (fontSize < 8)
							fontSize = 8;
						StyleConstants.setFontSize(mas, fontSize);
						doc.setCharacterAttributes(content.getStartOffset(), content.getEndOffset()
								- content.getStartOffset(), mas, false);
						fontSizeSet = true;
					}

				}

				/* if font size of this content element is not set, it takes the default value 12. */
				if (!fontSizeSet) {
					fontSize = 12 + step;
					if (fontSize < 8)
						fontSize = 8;
					StyleConstants.setFontSize(mas, fontSize);
					doc.setCharacterAttributes(content.getStartOffset(), content.getEndOffset()
							- content.getStartOffset(), mas, false);
				}

			}

		}

		page.setFontIncrement(page.getFontIncrement() + step);

	}

	/* traverse the page first to see if there is any text smaller than 8, the smallest font allowed. */
	private static boolean isSmallest(AbstractDocument.BranchElement section) {

		Enumeration enum1 = section.children();
		AbstractDocument.BranchElement paragraph = null;
		AbstractDocument.LeafElement content = null;
		Enumeration enum2 = null, enum3 = null;
		Object name = null, attr = null;
		int fontSize = 0;

		while (enum1.hasMoreElements()) {
			paragraph = (AbstractDocument.BranchElement) enum1.nextElement();
			enum2 = paragraph.children();
			while (enum2.hasMoreElements()) {
				content = (AbstractDocument.LeafElement) enum2.nextElement();
				enum3 = content.getAttributeNames();
				while (enum3.hasMoreElements()) {
					name = enum3.nextElement();
					attr = content.getAttribute(name);
					if (name.toString().equals(StyleConstants.FontSize.toString())) {
						fontSize = ((Integer) attr).intValue();
						if (fontSize <= 8)
							return true;
					}
				}
			}
		}

		return false;

	}

}