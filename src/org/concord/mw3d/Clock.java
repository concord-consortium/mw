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

import java.awt.Font;
import java.awt.Graphics;
import java.text.DecimalFormat;

import org.concord.mw3d.models.MolecularModel;

class Clock {

	private final static Font SMALL_FONT = new Font("Arial", Font.PLAIN, 10);
	private MolecularModel model;
	private int x = 10, y;
	private final static DecimalFormat FORMAT = new DecimalFormat("######.#");

	Clock(MolecularModel model) {
		this.model = model;
	}

	void paint(Graphics g) {
		y = model.getView().getHeight() - 10;
		g.setColor(model.getView().contrastBackground());
		g.setFont(SMALL_FONT);
		g.drawString(FORMAT.format(model.getModelTime() * 0.001f) + " ps", x, y);
	}

}
