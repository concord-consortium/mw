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

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.Serializable;

/** This is an implementation of fictitious field in a reference frame due to acceleration. */

public class AccelerationalField implements VectorField, Serializable {

	private int o = EAST;
	private double a = 0.01;
	private Shape bounds;
	private boolean local;

	public AccelerationalField() {
	}

	public AccelerationalField(Rectangle d) {
		bounds = d;
	}

	public AccelerationalField(double a, Rectangle d) {
		this.a = a;
		bounds = d;
	}

	public AccelerationalField(int o, double a, Rectangle d) {
		this.o = o;
		this.a = a;
		bounds = d;
	}

	public void setLocal(boolean b) {
		local = b;
	}

	public boolean isLocal() {
		return local;
	}

	public void setBounds(Shape shape) {
		bounds = shape;
	}

	public void setIntensity(double a) {
		this.a = a;
	}

	public double getIntensity() {
		return a;
	}

	public void setOrientation(int o) {
		this.o = o;
	}

	public int getOrientation() {
		return o;
	}

	void dyn(Particle p) {
		switch (o) {
		case SOUTH:
			p.fy += a * MDModel.GF_CONVERSION_CONSTANT;
			break;
		case NORTH:
			p.fy -= a * MDModel.GF_CONVERSION_CONSTANT;
			break;
		case EAST:
			p.fx += a * MDModel.GF_CONVERSION_CONSTANT;
			break;
		case WEST:
			p.fx -= a * MDModel.GF_CONVERSION_CONSTANT;
			break;
		}
	}

	void dyn(RectangularObstacle obs) {
		if (!obs.isMovable())
			return;
		switch (o) {
		case SOUTH:
			obs.ay += a * MDModel.GF_CONVERSION_CONSTANT;
			break;
		case NORTH:
			obs.ay -= a * MDModel.GF_CONVERSION_CONSTANT;
			break;
		case EAST:
			obs.ax += a * MDModel.GF_CONVERSION_CONSTANT;
			break;
		case WEST:
			obs.ax -= a * MDModel.GF_CONVERSION_CONSTANT;
			break;
		}
	}

	/*
	 * the accelerational potential equals zero at one of the border of simulation box, depends on the direction of the
	 * field. If you use periodic boundary conditions, you will have to substract an amount of potential energy <tt>maL</tt>
	 * each time after a particle crosses the border that corresponds to minimum potential energy in the scope of the
	 * box, or add that amount of potential energy each time after it crosses the opposite border. @param p the passed
	 * particle @return the accelerational potential of the particle in this field
	 */
	double getPotential(Particle p, float time) {
		double h = 0.0;
		switch (o) {
		case NORTH:
			h = p.ry;
			break;
		case SOUTH:
			h = bounds.getBounds().height - p.ry;
			break;
		case EAST:
			h = bounds.getBounds().width - p.rx;
			break;
		case WEST:
			h = p.rx;
			break;
		}
		return p.getMass() * a * h;
	}

	double getPotential(RectangularObstacle obs, float time) {
		if (!obs.isMovable())
			return 0.0;
		double h = 0.0;
		switch (o) {
		case NORTH:
			h = obs.y;
			break;
		case SOUTH:
			h = bounds.getBounds().height - obs.y;
			break;
		case EAST:
			h = bounds.getBounds().width - obs.x;
			break;
		case WEST:
			h = obs.x;
			break;
		}
		return obs.getMass() * a * h;
	}

}