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

package org.concord.mw2d.ui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.concord.modeler.ui.PastableTextField;

class CodonTextField extends PastableTextField {

	private final static char[] CHARS = new char[] { 'A', 'C', 'G', 'T', 'U' };

	public CodonTextField() {
		super();
	}

	protected Document createDefaultModel() {

		return new PlainDocument() {

			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

				char[] source = str.toCharArray();
				char[] result = new char[source.length];
				int j = 0;

				for (int i = 0; i < result.length; i++) {
					for (int k = 0; k < CHARS.length; k++) {
						if (source[i] == CHARS[k] || source[i] == CHARS[k] - 32 || source[i] == CHARS[k] + 32) {
							result[j++] = source[i];
						}
					}
				}

				super.insertString(offs, new String(result, 0, j), a);

			}

		};

	}

}