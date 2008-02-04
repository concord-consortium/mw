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

package org.concord.mw2d;

import java.awt.Graphics2D;

import org.concord.mw2d.models.Atom;

class Mw2dRenderer {

	private AtomisticView view;

	Mw2dRenderer(AtomisticView view) {
		this.view = view;
	}

	// render the selected atom on top of all the others
	void render(Graphics2D g) {
		int noa = view.getNumberOfAppearingAtoms();
		if (view.selectedComponent instanceof Atom) {
			int iat = ((Atom) view.selectedComponent).getIndex();
			for (int i = 0; i < iat; i++)
				view.atom[i].render(g);
			for (int i = iat + 1; i < noa; i++)
				view.atom[i].render(g);
			view.atom[iat].render(g);
		}
		else {
			for (int i = 0; i < noa; i++)
				view.atom[i].render(g);
		}
		noa = view.getModel().getNumberOfParticles();
		for (int i = 0; i < noa; i++) {
			view.atom[i].renderMeanPosition(g);
			view.atom[i].renderMeanForce(g);
		}
	}

}