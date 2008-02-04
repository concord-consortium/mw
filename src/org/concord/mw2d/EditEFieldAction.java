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
import org.concord.mw2d.models.ElectricField;
import org.concord.mw2d.models.FieldArea;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.VectorField;

class EditEFieldAction extends ModelAction {

	private ElectricFieldEditor electricFieldEditor;

	EditEFieldAction(MDModel m, final boolean forLocal) {

		super(m);

		putValue(NAME, "Electric");
		putValue(SHORT_DESCRIPTION, "Change the electric field");

		setExecutable(new Executable() {
			public void execute() {
				if (electricFieldEditor == null) {
					electricFieldEditor = new ElectricFieldEditor(JOptionPane.getFrameForComponent(myModel.getView()));
					electricFieldEditor.pack();
					electricFieldEditor.setLocationRelativeTo(myModel.getView());
				}
				ElectricField ef = null;
				if (forLocal) {
					MDView view = (MDView) myModel.getView();
					if (view.getSelectedComponent() instanceof FieldArea) {
						final FieldArea area = (FieldArea) view.getSelectedComponent();
						VectorField vf = area.getVectorField();
						if (vf instanceof ElectricField) {
							ef = (ElectricField) vf;
						}
						electricFieldEditor.setCallbackForAddingField(new Runnable() {
							public void run() {
								double dcIntensity = electricFieldEditor.getDCIntensity();
								double acAmplitude = electricFieldEditor.getACAmplitude();
								double acFrequency = electricFieldEditor.getACFrequency();
								int direction = electricFieldEditor.getDirection();
								ElectricField e = new ElectricField(dcIntensity, acAmplitude, acFrequency, direction,
										area.getBounds());
								e.setLocal(true);
								area.setVectorField(e);
								electricFieldEditor.setField(e);
							}
						});
						electricFieldEditor.setCallbackForRemovingField(new Runnable() {
							public void run() {
								area.setVectorField(null);
							}
						});
					}
				}
				else {
					ef = (ElectricField) myModel.getNonLocalField(ElectricField.class.getName());
					electricFieldEditor.setCallbackForAddingField(new Runnable() {
						public void run() {
							double dcIntensity = electricFieldEditor.getDCIntensity();
							double acAmplitude = electricFieldEditor.getACAmplitude();
							double acFrequency = electricFieldEditor.getACFrequency();
							int direction = electricFieldEditor.getDirection();
							ElectricField e = new ElectricField(dcIntensity, acAmplitude, acFrequency, direction,
									myModel.getView().getBounds());
							myModel.addNonLocalField(e);
							electricFieldEditor.setField(e);
						}
					});
					electricFieldEditor.setCallbackForRemovingField(new Runnable() {
						public void run() {
							myModel.removeField(ElectricField.class.getName());
						}
					});
				}
				electricFieldEditor.setField(ef);
				electricFieldEditor.setVisible(true);
			}
		});

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}