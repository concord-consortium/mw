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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.ImageIcon;

/**
 * @author Charles Xie
 * 
 */

class ImageIconSelection implements Transferable {

	static DataFlavor imageIconFlavor;

	private static DataFlavor[] dataFlavors;
	private ImageIcon icon;

	ImageIconSelection(ImageIcon icon) {
		this.icon = icon;
		createImageIconFlavor();
	}

	private static void createImageIconFlavor() {
		if (imageIconFlavor == null) {
			try {
				imageIconFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=javax.swing.ImageIcon");
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * @see java.awt.datatransfer.Transferable#getTransferData(java.awt.datatransfer.DataFlavor)
	 */
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (isDataFlavorSupported(flavor))
			return icon;
		return null;
	}

	/**
	 * @see java.awt.datatransfer.Transferable#getTransferDataFlavors()
	 */
	public DataFlavor[] getTransferDataFlavors() {
		if (dataFlavors == null)
			dataFlavors = new DataFlavor[] { imageIconFlavor };
		return dataFlavors;
	}

	/**
	 * @see java.awt.datatransfer.Transferable#isDataFlavorSupported(java.awt.datatransfer.DataFlavor)
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if (flavor == null)
			return false;
		return flavor.equals(imageIconFlavor);
	}

}