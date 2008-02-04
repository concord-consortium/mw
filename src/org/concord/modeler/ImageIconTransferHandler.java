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
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * @author Charles Xie
 * 
 */

class ImageIconTransferHandler extends TransferHandler {

	private static boolean transfered;

	ImageIconTransferHandler() {
	}

	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY;
	}

	protected void exportDone(JComponent source, Transferable data, int action) {
		super.exportDone(source, data, action);
		if (transfered) {
			if (source instanceof ThumbnailImagePanel) {
				((ThumbnailImagePanel) source).exportDone(data, action);
			}
			transfered = false;
		}
	}

	protected Transferable createTransferable(JComponent c) {
		if (!(c instanceof ThumbnailImagePanel))
			return null;
		ImageIcon icon = SnapshotGallery.sharedInstance().loadSelectedAnnotatedImage();
		return new ImageIconSelection(icon);
	}

	public boolean importData(JComponent c, Transferable t) {
		if (!(c instanceof ImageContainer))
			return false;
		if (hasImageIconFlavor(t.getTransferDataFlavors())) {
			try {
				ImageIcon image = (ImageIcon) t.getTransferData(ImageIconSelection.imageIconFlavor);
				((ImageContainer) c).setImage(image);
				c.repaint();
				transfered = true;
				return true;
			}
			catch (UnsupportedFlavorException ufe) {
				ufe.printStackTrace(System.err);
			}
			catch (IOException ioe) {
				ioe.printStackTrace(System.err);
			}
		}
		return false;
	}

	private boolean hasImageIconFlavor(DataFlavor[] flavors) {
		if (ImageIconSelection.imageIconFlavor == null)
			return false;
		for (DataFlavor i : flavors) {
			if (ImageIconSelection.imageIconFlavor.equals(i))
				return true;
		}
		return false;
	}

	public boolean canImport(JComponent c, DataFlavor[] flavors) {
		return hasImageIconFlavor(flavors);
	}

}