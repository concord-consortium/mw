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
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.vecmath.Point3f;

import org.concord.modeler.FileFilterFactory;

class ImportStructureAction extends AbstractAction {

	private MolecularContainer container;

	ImportStructureAction(MolecularContainer c) {
		super();
		container = c;
		putValue(NAME, "Import Structure");
		putValue(SHORT_DESCRIPTION, "Import structure");
		putValue(SMALL_ICON, new ImageIcon(MolecularContainer.class.getResource("resources/ImportStructure.gif")));
	}

	public void actionPerformed(ActionEvent e) {
		FileFilter filter = FileFilterFactory.getFilter("xyz");
		container.fileChooser.setAcceptAllFileFilterUsed(false);
		container.fileChooser.addChoosableFileFilter(filter);
		container.fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		String s = MolecularContainer.getInternationalText("ImportStructure");
		container.fileChooser.setDialogTitle(s != null ? s : "Import Structure");
		container.fileChooser.setApproveButtonMnemonic('I');
		String latestPath = container.fileChooser.getLastVisitedPath();
		if (latestPath != null)
			container.fileChooser.setCurrentDirectory(new File(latestPath));
		container.fileChooser.clearTextField();
		container.fileChooser.setAccessory(null);
		if (container.fileChooser.showOpenDialog(JOptionPane.getFrameForComponent(container)) == JFileChooser.APPROVE_OPTION) {
			File file = container.fileChooser.getSelectedFile();
			container.inputXyz(file);
			container.model.translateAtomsTo(new Point3f());
			container.model.formMolecules();
			container.view.setOrientation(container.view.getViewer().getCurrentOrientation());
			container.view.renderModel(false);
			container.view.setMolecularStyle(container.view.getMolecularStyle());
			container.fileChooser.rememberPath(container.fileChooser.getCurrentDirectory().toString());
		}
		container.fileChooser.resetChoosableFileFilters();
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}