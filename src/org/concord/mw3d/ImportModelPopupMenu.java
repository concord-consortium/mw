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
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;
import javax.vecmath.Point3f;

import org.concord.modeler.FileFilterFactory;

class ImportModelPopupMenu extends JPopupMenu {

	private MolecularContainer container;
	private Point3f position;

	ImportModelPopupMenu(MolecularContainer c) {

		super("Import Model");
		container = c;

		JMenuItem mi = new JMenuItem("Import a Model Here");
		String s = MolecularContainer.getInternationalText("ImportModelHere");
		if (s != null)
			mi.setText(s);
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (position == null)
					return;
				importModel();
			}
		});
		add(mi);

		pack();

	}

	void setPosition(Point3f p) {
		position = p;
	}

	private void importModel() {
		FileFilter filter = FileFilterFactory.getFilter("xyz");
		container.fileChooser.setAcceptAllFileFilterUsed(false);
		container.fileChooser.addChoosableFileFilter(filter);
		container.fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		String s = MolecularContainer.getInternationalText("OpenStructure");
		container.fileChooser.setDialogTitle(s != null ? s : "Open Structure");
		container.fileChooser.setApproveButtonMnemonic('O');
		String latestPath = container.fileChooser.getLastVisitedPath();
		if (latestPath != null)
			container.fileChooser.setCurrentDirectory(new File(latestPath));
		container.fileChooser.setAccessory(null);
		if (container.fileChooser.showOpenDialog(JOptionPane.getFrameForComponent(container)) == JFileChooser.APPROVE_OPTION) {
			File file = container.fileChooser.getSelectedFile();
			int n0 = container.model.getAtomCount();
			container.inputXyzWithoutClearing(file);
			int n1 = container.model.getAtomCount();
			if (container.model.translateAtomsTo(n0, n1, position)) {
				container.view.setOrientation(container.view.getViewer().getCurrentOrientation());
				container.view.renderModel(false);
				container.notifyChange();
			}
			else {
				container.view.errorReminder.show(ErrorReminder.OBJECT_OVERLAP);
			}
			container.fileChooser.rememberPath(container.fileChooser.getCurrentDirectory().toString());
		}
		container.fileChooser.resetChoosableFileFilters();
	}

}