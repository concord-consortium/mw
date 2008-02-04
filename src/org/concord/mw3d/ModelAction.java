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

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.Executable;
import org.concord.mw3d.models.MolecularModel;

/**
 * Ideally, all editing actions must be passed to this for final approval, as an editing action will most * likely
 * trigger the reset of the movie recorder. * *
 * 
 * @author Charles Xie
 */

abstract class ModelAction extends AbstractAction {

	MolecularModel myModel;
	Executable executable;

	protected ModelAction(MolecularModel m) {
		setModel(m);
	}

	public ModelAction(MolecularModel m, Executable ex) {
		this(m);
		setExecutable(ex);
	}

	public void setModel(MolecularModel m) {
		myModel = m;
	}

	public MolecularModel getModel() {
		return myModel;
	}

	public void setExecutable(Executable ex) {
		executable = ex;
	}

	public Executable getExecutable() {
		return executable;
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		if (myModel.hasEmbeddedMovie()) {
			if (myModel.resetTape() != JOptionPane.NO_OPTION) {
				executable.execute();
				myModel.getView().reactToTapeReset();
			}
			else {
				myModel.getView().setActionID(UserAction.DEFA_ID);
			}
		}
		else {
			executable.execute();
		}
	}

}
