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

/**
 * The walls of the rectangular box containing the atoms of a model. A container box can have four walls, if the
 * boundary conditions are reflecting, or two, if the boundary conditions are reflecting in one direction, or zero, if
 * the boundary conditions are periodic in both directions. A wall only reflects a particle. It can be elastic or
 * inelastic. It can have a temperature.
 */

public class Wall {

	public final static byte NORTH = 0;
	public final static byte SOUTH = 1;
	public final static byte WEST = 2;
	public final static byte EAST = 3;

	final static int MAX_WALL_TEMPERATURE = 10000;

	private MDModel model;
	private byte sinkSide = -1;
	private boolean[] isSink = new boolean[4];
	private boolean[] isElastic = new boolean[4];
	private int[] temperature = new int[4];
	private byte[] flowOutType;

	public Wall(MDModel model) {
		setModel(model);
		reset();
	}

	public void setModel(MDModel model) {
		this.model = model;
	}

	public MDModel getHostModel() {
		return model;
	}

	public void reset() {
		flowOutType = new byte[] { Element.ID_NT, Element.ID_PL, Element.ID_WS, Element.ID_CK };
		for (byte i = 0; i < 4; i++) {
			isElastic[i] = true;
			isSink[i] = false;
			temperature[i] = 1000;
		}
	}

	void setFlowOutType(byte[] b) {
		flowOutType = b;
	}

	byte[] getFlowOutType() {
		return flowOutType;
	}

	boolean toSink(byte element) {
		for (byte i = 0; i < flowOutType.length; i++) {
			if (element == flowOutType[i])
				return true;
		}
		return false;
	}

	public void setSink(byte side, boolean value) {
		for (byte i = 0; i < 4; i++)
			isSink[i] = false;
		if (side >= 0 && side < 4) {
			isSink[side] = value;
			if (value)
				sinkSide = side;
		}
		else {
			sinkSide = -1;
		}
	}

	public boolean isSink(byte side) {
		if (side >= 0 && side < 4)
			return isSink[side];
		return false;
	}

	byte getSinkSide() {
		return sinkSide;
	}

	public void setElastic(byte side, boolean value) {
		isElastic[side] = value;
		model.getView().repaint();
	}

	public boolean isElastic(byte side) {
		return isElastic[side];
	}

	public void setTemperature(byte side, int t) {
		temperature[side] = t;
		model.getView().repaint();
	}

	public int getTemperature(byte side) {
		return temperature[side];
	}

}