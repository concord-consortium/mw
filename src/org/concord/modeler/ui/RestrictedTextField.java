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

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * This is a text field for receiving only certain characters given by a char array. The length of text is limited by a
 * given integer. Case sensitivity can be specified.
 */

public class RestrictedTextField extends PastableTextField {

	private char[] permit;
	private boolean caseSensitive;

	public RestrictedTextField(char[] permit, int length) throws IllegalArgumentException {
		this(permit, length, false);
	}

	public RestrictedTextField(char[] permit, int length, boolean caseSensitive) throws IllegalArgumentException {
		super();
		if (length <= 0)
			throw new IllegalArgumentException("length must be greater than 0");
		this.permit = permit;
		// this.length=length;
		this.caseSensitive = caseSensitive;
	}

	public void setLength(int length) {
		// this.length=length;
	}

	public void setCaseSensitive(boolean b) {
		caseSensitive = b;
	}

	protected Document createDefaultModel() {

		return new PlainDocument() {

			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

				if (permit != null) {

					char[] source = str.toCharArray();
					char[] result = new char[source.length];
					int j = 0;

					for (int i = 0; i < result.length; i++) {
						for (int k = 0; k < permit.length; k++) {
							if (caseSensitive) {
								if (source[i] == permit[k]) {
									result[j++] = source[i];
								}
							}
							else {
								if (source[i] == permit[k] || source[i] == permit[k] - 32
										|| source[i] == permit[k] + 32) {
									result[j++] = source[i];
								}
							}
						}
					}

					// if(old==null || (old!=null && length>0 && old.length<length)){
					super.insertString(offs, new String(result, 0, j), a);
					// }

				}

				else {

					super.insertString(offs, str, a);

				}

			}

		};

	}

}
