/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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

package org.concord.mw2d;

import javax.swing.JOptionPane;

import org.concord.modeler.process.Executable;
import org.concord.mw2d.models.MDModel;

class EditGFieldAction extends ModelAction {

	private GravityEditor gravityEditor;

	EditGFieldAction(MDModel m) {

		super(m);

		putValue(NAME, "Gravitational");
		putValue(SHORT_DESCRIPTION, "Change the gravitational field");

		setExecutable(new Executable() {
			public void execute() {
				if (gravityEditor == null) {
					gravityEditor = new GravityEditor(JOptionPane.getFrameForComponent(myModel.getView()));
					gravityEditor.pack();
					gravityEditor.setLocationRelativeTo(myModel.getView());
				}
				gravityEditor.setModel(myModel);
				gravityEditor.setCurrentValues();
				gravityEditor.setVisible(true);
			}
		});

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}