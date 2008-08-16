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

package org.concord.mw3d;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import org.concord.modeler.FileFilterFactory;
import org.concord.modeler.ui.IconPool;

class OpenModelAction extends AbstractAction {

	private MolecularContainer container;

	OpenModelAction(MolecularContainer c) {
		super();
		container = c;
		putValue(NAME, "Open Model");
		putValue(SHORT_DESCRIPTION, "Open model");
		putValue(SMALL_ICON, IconPool.getIcon("open"));
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_O, KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
	}

	public void actionPerformed(ActionEvent e) {
		FileFilter filter = FileFilterFactory.getFilter("mdd");
		container.fileChooser.setAcceptAllFileFilterUsed(false);
		container.fileChooser.addChoosableFileFilter(filter);
		container.fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		String s = MolecularContainer.getInternationalText("OpenModel");
		container.fileChooser.setDialogTitle(s != null ? s : "Open Model");
		container.fileChooser.setApproveButtonMnemonic('O');
		String latestPath = container.fileChooser.getLastVisitedPath();
		if (latestPath != null)
			container.fileChooser.setCurrentDirectory(new File(latestPath));
		container.fileChooser.clearTextField();
		container.fileChooser.setAccessory(null);
		if (container.fileChooser.showOpenDialog(JOptionPane.getFrameForComponent(container)) == JFileChooser.APPROVE_OPTION) {
			File file = container.fileChooser.getSelectedFile();
			container.input(file, false);
			container.fileChooser.rememberPath(container.fileChooser.getCurrentDirectory().toString());
		}
		container.fileChooser.resetChoosableFileFilters();
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}
