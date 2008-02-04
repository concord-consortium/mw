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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

class StyledTextSelection implements ClipboardOwner, Transferable {

	public static DataFlavor styledTextFlavor;

	private static StyledDocument doc;
	private DataFlavor[] flavors = { styledTextFlavor };

	static {
		try {
			styledTextFlavor = new DataFlavor(Class.forName("javax.swing.text.StyledDocument"), "Styled Text");
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace(System.err);
		}
	}

	public StyledTextSelection() {
		if (doc == null)
			doc = new DefaultStyledDocument();
	}

	public void clearText() {
		int n = doc.getLength();
		try {
			if (n > 0)
				doc.remove(0, n);
		}
		catch (BadLocationException e) {
			e.printStackTrace(System.err);
		}
	}

	public int getLength() {
		return doc.getLength();
	}

	public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
		doc.insertString(offset, str, a);
	}

	public void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace) {
		doc.setCharacterAttributes(offset, length, s, replace);
	}

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor == null)
			return null;
		if (!flavor.equals(styledTextFlavor))
			throw new UnsupportedFlavorException(flavor);
		return doc;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if (flavor == null)
			return false;
		return flavor.equals(styledTextFlavor);
	}

	public void lostOwnership(Clipboard c, Transferable t) {
	}

}