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

/**
 * This is an implementation of gravitational field. A gravitational field always points downwards. A graviational field
 * cannot be local.
 */

public class GravitationalField implements VectorField, Serializable {

	private double g = 0.01;
	private Shape bounds;

	public GravitationalField() {
	}

	public GravitationalField(Rectangle d) {
		bounds = d;
	}

	public GravitationalField(double g, Rectangle d) {
		this.g = g;
		bounds = d;
	}

	public void setLocal(boolean b) {
	}

	public boolean isLocal() {
		return false;
	}

	public void setBounds(Shape shape) {
		bounds = shape;
	}

	public void setIntensity(double g) {
		this.g = g;
	}

	public double getIntensity() {
		return g;
	}

	void dyn(Particle p) {
		p.fy += g * MDModel.GF_CONVERSION_CONSTANT;
	}

	void dyn(RectangularObstacle obs) {
		if (obs.isMovable())
			obs.ay += g * MDModel.GF_CONVERSION_CONSTANT;
	}

	/*
	 * the gravitational potential equals zero at the bottom of the simulation box. If you use periodic boundary
	 * conditions, you will have to substract an amount of potential energy <tt>mgH</tt> each time after a particle
	 * crosses the bottom border, or add that amount of potential energy each time after it crosses the top border.
	 * @param p the pasaed particle @param time current time @return the gravitational potential of the particle in this
	 * field
	 */
	double getPotential(Particle p, float time) {
		return p.getMass() * g * (bounds.getBounds().height - p.ry);
	}

	double getPotential(RectangularObstacle obs, float time) {
		if (obs.getDensity() >= RectangularObstacle.HEAVY)
			return 0.0;
		return obs.getMass() * g * (bounds.getBounds().height - obs.y);
	}

}