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

import java.awt.EventQueue;

import org.concord.modeler.process.AbstractLoadable;

class AtomSource extends AbstractLoadable {

	private AtomicModel model;
	private byte wall = Wall.WEST;
	private byte[] type = new byte[] { Element.ID_NT };
	private Runnable runnable;

	AtomSource(AtomicModel model) {
		this.model = model;
		setInterval(500);
	}

	void setWall(byte wall) {
		this.wall = wall;
	}

	byte getWall() {
		return wall;
	}

	void setType(byte[] type) {
		this.type = type;
	}

	byte[] getType() {
		return type;
	}

	public void execute() {
		if (type == null || type.length == 0)
			return;
		if (runnable == null) {
			runnable = new Runnable() {
				public void run() {
					work();
				}
			};
		}
		EventQueue.invokeLater(runnable);
	}

	private void work() {
		double rtemp = Math.sqrt(model.heatBathActivated() ? model.getHeatBath().getExpectedTemperature() : model.getTemperature()) * MDModel.VT_CONVERSION_CONSTANT * 100;
		if (rtemp < Particle.ZERO)
			rtemp = Particle.ZERO;
		double v = type.length * Math.random();
		// if(v>type.length-1) v=type.length-1;
		int id = type[(int) v];
		Element elem = model.getElement(id);
		if (elem == null)
			return;
		double sigma = elem.getSigma();
		switch (wall) {
		case Wall.WEST:
			if (model.view.insertAnAtom(sigma * 0.5 + 2, sigma * 0.5 + MDModel.RANDOM.nextFloat() * (model.view.getHeight() - sigma), id, true))
				model.atom[model.numberOfAtoms - 1].setVx(rtemp);
			break;
		case Wall.EAST:
			if (model.view.insertAnAtom(model.view.getWidth() - sigma * 0.5 - 2, sigma * 0.5 + MDModel.RANDOM.nextFloat() * (model.view.getHeight() - sigma), id, true))
				model.atom[model.numberOfAtoms - 1].setVx(-rtemp);
			break;
		case Wall.SOUTH:
			if (model.view.insertAnAtom(sigma * 0.5 + MDModel.RANDOM.nextFloat() * (model.view.getWidth() - sigma), model.view.getHeight() - sigma * 0.5 - 2, id, true))
				model.atom[model.numberOfAtoms - 1].setVy(-rtemp);
			break;
		case Wall.NORTH:
			if (model.view.insertAnAtom(sigma * 0.5 + MDModel.RANDOM.nextFloat() * (model.view.getWidth() - sigma), sigma * 0.5 + 2, id, true))
				model.atom[model.numberOfAtoms - 1].setVy(rtemp);
			break;
		}
	}

	public String getName() {
		return "Atom source";
	}

	public int getLifetime() {
		return ETERNAL;
	}

	public String getDescription() {
		return "This task models flow of atoms into the simulation box.";
	}

}