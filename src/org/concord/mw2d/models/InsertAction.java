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
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.concord.modeler.process.Executable;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.ModelAction;

class InsertAction extends ModelAction {

	private byte element;

	InsertAction(final AtomicModel model, byte e) {
		super(model);
		this.element = e;
		setExecutable(new Executable() {
			public void execute() {
				switch (element) {
				case Element.ID_NT:
					if (model.view.insertAnAtom((int) (0.5 * model.nt.getSigma()) + 2, (int) (AtomicModel.RANDOM
							.nextFloat() * model.view.getHeight()), element, true))
						model.atom[model.numberOfAtoms - 1].setVx(0.1);
					break;
				case Element.ID_PL:
					if (model.view.insertAnAtom((int) (AtomicModel.RANDOM.nextFloat() * model.view.getWidth()),
							model.view.getHeight() - (int) (0.5 * model.pl.getSigma()) - 2, element, true))
						model.atom[model.numberOfAtoms - 1].setVy(-0.1);
					break;
				case Element.ID_WS:
					if (model.view.insertAnAtom(model.view.getWidth() - (int) (0.5 * model.ws.getSigma()) - 2,
							(int) (AtomicModel.RANDOM.nextFloat() * model.view.getHeight()), element, true))
						model.atom[model.numberOfAtoms - 1].setVx(-0.1);
					break;
				case Element.ID_CK:
					if (model.view.insertAnAtom((int) (AtomicModel.RANDOM.nextFloat() * model.view.getWidth()),
							(int) (0.5 * model.ck.getSigma()) + 2, element, true))
						model.atom[model.numberOfAtoms - 1].setVy(0.1);
					break;
				}
				if (model.job != null) {
					if (model.job.isStopped())
						model.run();
				}
				else {
					model.initializeJob();
					model.run();
				}
			}
		});

		putValue(NAME, "Insert " + Element.idToName(element));
		putValue(SHORT_DESCRIPTION, "Insert " + Element.idToName(element));
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		Icon icon = null;
		switch (element) {
		case Element.ID_NT:
			icon = new ImageIcon(AtomisticView.class.getResource("images/editor/AddAtomA.gif"));
			break;
		case Element.ID_PL:
			icon = new ImageIcon(AtomisticView.class.getResource("images/editor/AddAtomB.gif"));
			break;
		case Element.ID_WS:
			icon = new ImageIcon(AtomisticView.class.getResource("images/editor/AddAtomC.gif"));
			break;
		case Element.ID_CK:
			icon = new ImageIcon(AtomisticView.class.getResource("images/editor/AddAtomD.gif"));
			break;
		}
		if (icon != null)
			putValue(SMALL_ICON, icon);
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}