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

package org.concord.jmol;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.concord.modeler.FileFilterFactory;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.FileChooser;

class StructureReader extends AbstractAction {

	private JmolContainer viewer;
	private FileChooser fileChooser;
	private String selectedExtension;

	StructureReader() {
		super();
		putValue(NAME, "Open Model");
		putValue(SHORT_DESCRIPTION, "Open model");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_O, KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
		putValue(SMALL_ICON, IconPool.getIcon("open"));
	}

	public void setMolecularViewer(JmolContainer viewer) {
		this.viewer = viewer;
	}

	public void setFileChooser(FileChooser fc) {
		fileChooser = fc;
	}

	public void actionPerformed(ActionEvent e) {

		if (viewer == null || fileChooser == null)
			return;

		fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("out"));
		fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("xyz"));
		fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("mol"));
		fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("mol2"));
		fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("cif"));
		fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("gz"));
		fileChooser.addChoosableFileFilter(FileFilterFactory.getFilter("pdb"));
		if ("all files".equals(selectedExtension)) {
			fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
		}
		else {
			fileChooser.setFileFilter(FileFilterFactory
					.getFilter(selectedExtension == null ? "pdb" : selectedExtension));
		}
		fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		fileChooser.setApproveButtonMnemonic('L');
		fileChooser.setAccessory(null);
		String latestPath = fileChooser.getLastVisitedPath();
		if (latestPath != null)
			fileChooser.setCurrentDirectory(new File(latestPath));
		fileChooser.clearTextField();

		if (fileChooser.showOpenDialog(JOptionPane.getFrameForComponent(viewer)) == JFileChooser.APPROVE_OPTION) {
			final File file = fileChooser.getSelectedFile();
			selectedExtension = fileChooser.getFileFilter().getDescription().toLowerCase();
			Thread t = new Thread("Structure Reader") {
				public void run() {
					viewer.clear();
					viewer.setResourceAddress(file.getAbsolutePath());
					viewer.loadCurrentStructure();
				}
			};
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
			fileChooser.rememberPath(fileChooser.getCurrentDirectory().toString());
		}

		fileChooser.resetChoosableFileFilters();

	}

}