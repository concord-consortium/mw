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

import java.awt.Component;
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
import org.concord.modeler.util.FileUtilities;

class SaveModelAction extends AbstractAction {

	private MolecularContainer container;

	SaveModelAction(MolecularContainer c) {
		super();
		container = c;
		putValue(NAME, "Save Model As");
		putValue(SHORT_DESCRIPTION, "Save model as");
		putValue(SMALL_ICON, IconPool.getIcon("save"));
		putValue(ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_S, KeyEvent.META_MASK | KeyEvent.SHIFT_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_S,
				KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
	}

	public void actionPerformed(ActionEvent e) {
		FileFilter filter = FileFilterFactory.getFilter("mdd");
		container.fileChooser.setAcceptAllFileFilterUsed(false);
		container.fileChooser.addChoosableFileFilter(filter);
		container.fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		String s = MolecularContainer.getInternationalText("SaveModel");
		container.fileChooser.setDialogTitle(s != null ? s : "Save Model");
		container.fileChooser.setApproveButtonMnemonic('S');
		String latestPath = container.fileChooser.getLastVisitedPath();
		if (latestPath != null)
			container.fileChooser.setCurrentDirectory(new File(latestPath));
		container.fileChooser.setAccessory(null);
		container.fileChooser.setSelectedFile(null);
		container.fileChooser.resetTextField();
		if (container.fileChooser.showSaveDialog(JOptionPane.getFrameForComponent(container)) == JFileChooser.APPROVE_OPTION) {
			final File file = new File(FileUtilities
					.fileNameAutoExtend(filter, container.fileChooser.getSelectedFile()));
			if (!validateFileName(file, container)) {
				container.fileChooser.resetChoosableFileFilters();
				return;
			}
			if (file.exists()) {
				if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(container), "File " + file.getName()
						+ " exists, overwrite?", "File exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
					container.fileChooser.resetChoosableFileFilters();
					return;
				}
			}
			container.output(file);
			container.fileChooser.rememberPath(container.fileChooser.getCurrentDirectory().toString());
		}
		container.fileChooser.resetChoosableFileFilters();
	}

	static boolean validateFileName(File file, Component c) {
		if (file == null)
			return false;
		switch (FileUtilities.checkFileName(file)) {
		case FileUtilities.DIRECTORY_ERROR:
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(c), "Directory error: " + file,
					"Path error", JOptionPane.ERROR_MESSAGE);
			return false;
		case FileUtilities.ILLEGAL_CHARACTER:
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(c),
					"A file name cannot contain any of the following characters:\n\\/:*?\"<>|", "Path error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}
