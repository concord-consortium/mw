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

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * This is a transferable component that can be used in Drag-and-Drop. This object just wraps component with a
 * transferable interface. This object supports data flavor. Data flavor needs to be specified as JVM's local object
 * MIME type.
 * 
 * @author Qian Xie
 */

public class TransferableComponent implements Transferable {

	private DataFlavor dataFlavor;
	private Component component = null;

	public TransferableComponent(Component component) {
		try {
			dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
		}
		catch (ClassNotFoundException e) {
		}
		this.component = component;
	}

	public Object getTransferData(DataFlavor flavor) {
		if (!flavor.equals(dataFlavor))
			return null;
		return component;
	}

	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] df = { dataFlavor };
		return df;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(dataFlavor);
	}

}
