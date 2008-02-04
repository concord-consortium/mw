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

import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class ListOrMapTransferHandler extends TransferHandler {

	private static DataFlavor localObjectFlavor;
	private JList source;
	private Object draggedItem;
	private Object data;
	private Runnable postJob;

	/** data must be either List or Map */
	public ListOrMapTransferHandler(Object data) {
		super();
		this.data = data;
		if (localObjectFlavor == null) {
			try {
				localObjectFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.Object");
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	public void setJobAfterTransfer(Runnable r) {
		postJob = r;
	}

	@SuppressWarnings("unchecked")
	public boolean importData(JComponent c, Transferable t) {

		JList target = null;
		if (!canImport(c, t.getTransferDataFlavors())) {
			return false;
		}
		try {
			target = (JList) c;
			if (!hasLocalObjectFlavor(t.getTransferDataFlavors()))
				return false;
			draggedItem = t.getTransferData(localObjectFlavor);
		}
		catch (UnsupportedFlavorException e) {
			e.printStackTrace(System.err);
			return false;
		}
		catch (IOException e) {
			e.printStackTrace(System.err);
			return false;
		}

		Object droppedItem = target.getSelectedValue(); // item closest to the dropped position
		if (source.equals(target)) {
			if (draggedItem == droppedItem) {
				return true;
			}
		}

		if (droppedItem != null) {
			DefaultListModel model = (DefaultListModel) target.getModel();
			int iDrag = model.indexOf(draggedItem);
			int iDrop = model.indexOf(droppedItem);
			model.removeElement(draggedItem);
			model.insertElementAt(draggedItem, iDrop);
			if (data instanceof List) {
				List list = (List) data;
				list.add(iDrop, list.remove(iDrag));
			}
			else if (data instanceof Map) {
				Map map = (Map) data;
				int n = map.size();
				List tempList = new ArrayList(n);
				for (Iterator it = map.keySet().iterator(); it.hasNext();) {
					tempList.add(it.next());
				}
				tempList.add(iDrop, tempList.remove(iDrag));
				Map tempMap = new HashMap();
				tempMap.putAll(map);
				map.clear();
				Object key;
				for (Iterator it = tempList.iterator(); it.hasNext();) {
					key = it.next();
					map.put(key, tempMap.get(key));
				}
			}
		}

		return true;

	}

	protected void exportDone(JComponent c, Transferable data, int action) {
		draggedItem = null;
		if (postJob != null)
			EventQueue.invokeLater(postJob);
	}

	private boolean hasLocalObjectFlavor(DataFlavor[] flavors) {
		if (localObjectFlavor == null) {
			return false;
		}
		for (int i = 0; i < flavors.length; i++) {
			if (flavors[i].equals(localObjectFlavor)) {
				return true;
			}
		}
		return false;
	}

	public boolean canImport(JComponent c, DataFlavor[] flavors) {
		return hasLocalObjectFlavor(flavors);
	}

	protected Transferable createTransferable(JComponent c) {
		if (c instanceof JList) {
			source = (JList) c;
			draggedItem = source.getSelectedValue();
			if (draggedItem == null)
				return null;
			return new ObjectTransferable(draggedItem);
		}
		return null;
	}

	public int getSourceActions(JComponent c) {
		return MOVE;
	}

	private class ObjectTransferable implements Transferable {

		private Object object;

		ObjectTransferable(Object o) {
			object = o;
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return object;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { localObjectFlavor };
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return localObjectFlavor.equals(flavor);
		}

	}

}