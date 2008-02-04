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
import org.concord.mw2d.models.FieldArea;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MagneticField;
import org.concord.mw2d.models.VectorField;

class EditBFieldAction extends ModelAction {

	private MagneticFieldEditor magneticFieldEditor;

	EditBFieldAction(MDModel m, final boolean forLocal) {

		super(m);

		putValue(NAME, "Magnetic");
		putValue(SHORT_DESCRIPTION, "Change the magnetic field");

		setExecutable(new Executable() {
			public void execute() {
				if (magneticFieldEditor == null) {
					magneticFieldEditor = new MagneticFieldEditor(JOptionPane.getFrameForComponent(myModel.getView()));
					magneticFieldEditor.pack();
					magneticFieldEditor.setLocationRelativeTo(myModel.getView());
				}
				MagneticField mf = null;
				if (forLocal) {
					MDView view = (MDView) myModel.getView();
					if (view.getSelectedComponent() instanceof FieldArea) {
						final FieldArea area = (FieldArea) view.getSelectedComponent();
						VectorField vf = area.getVectorField();
						if (vf instanceof MagneticField) {
							mf = (MagneticField) vf;
						}
						magneticFieldEditor.setCallbackForAddingField(new Runnable() {
							public void run() {
								MagneticField m = new MagneticField(magneticFieldEditor.getB(), magneticFieldEditor
										.getDirection(), area.getBounds());
								m.setLocal(true);
								area.setVectorField(m);
								magneticFieldEditor.setField(m);
							}
						});
						magneticFieldEditor.setCallbackForRemovingField(new Runnable() {
							public void run() {
								area.setVectorField(null);
							}
						});
					}
				}
				else {
					mf = (MagneticField) myModel.getNonLocalField(MagneticField.class.getName());
					magneticFieldEditor.setCallbackForAddingField(new Runnable() {
						public void run() {
							MagneticField m = new MagneticField(magneticFieldEditor.getB(), magneticFieldEditor
									.getDirection(), myModel.getView().getBounds());
							myModel.addNonLocalField(m);
							magneticFieldEditor.setField(m);
						}
					});
					magneticFieldEditor.setCallbackForRemovingField(new Runnable() {
						public void run() {
							myModel.removeField(MagneticField.class.getName());
						}
					});
				}
				magneticFieldEditor.setField(mf);
				magneticFieldEditor.setVisible(true);
			}
		});

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}