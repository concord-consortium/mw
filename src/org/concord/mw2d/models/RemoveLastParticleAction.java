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

package org.concord.mw2d.models;

import java.awt.event.KeyEvent;

import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.IconPool;
import org.concord.mw2d.MDView;
import org.concord.mw2d.ModelAction;

class RemoveLastParticleAction extends ModelAction {

	RemoveLastParticleAction(MDModel m) {

		super(m);

		setExecutable(new Executable() {
			public void execute() {
				if (myModel.getNumberOfParticles() > 0) {
					int i = myModel.getNumberOfParticles() - 1;
					if (myModel instanceof MesoModel) {
						myModel.setNumberOfParticles(i);
					}
					else {
						((MDView) myModel.getView()).setSelectedComponent(myModel.getParticle(i));
						((MDView) myModel.getView()).removeSelectedComponent();
					}
					myModel.getParticle(i).initializeMovieQ(-1);
					myModel.getView().repaint();
				}
			}
		});

		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		putValue(SMALL_ICON, IconPool.getIcon("cut"));
		putValue(NAME, "Remove Particle");
		putValue(SHORT_DESCRIPTION, "Remove a particle");

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}