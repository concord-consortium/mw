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
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;

/**
 * This is an implementation of electric field, which has a DC as well as an AC component. The AC component is
 * time-dependent.
 */

public class ElectricField implements VectorField, Serializable {

	private double dc = 1.0;
	private Shape bounds;
	private int o = EAST;
	private double amp, frq;
	private boolean local;

	/* Make the transient properties BML-transient: */
	static {
		try {
			BeanInfo info = Introspector.getBeanInfo(Atom.class);
			PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
			for (PropertyDescriptor pd : propertyDescriptors) {
				String name = pd.getName();
				if ("local".equals(name) || "bounds".equals(name)) {
					pd.setValue("transient", Boolean.TRUE);
				}
			}
		}
		catch (IntrospectionException e) {
		}
	}

	public ElectricField() {
	}

	public ElectricField(Rectangle d) {
		bounds = d;
	}

	public ElectricField(double dc, double amp, double frq, int o, Shape s) {
		this.dc = dc;
		this.amp = amp;
		this.frq = frq;
		bounds = s;
		this.o = o;
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

	/** same as <code>setIntensity()</code> */
	public void setDC(double dc) {
		setIntensity(dc);
	}

	public void setIntensity(double dc) {
		this.dc = Math.abs(dc);
		if (o == WEST || o == EAST) {
			o = dc > 0 ? EAST : WEST;
		}
		else if (o == SOUTH || o == NORTH) {
			o = dc > 0 ? SOUTH : NORTH;
		}
	}

	public double getIntensity() {
		return dc;
	}

	public void setOrientation(int o) {
		this.o = o;
	}

	public int getOrientation() {
		return o;
	}

	public void setAmplitude(double a) {
		amp = a;
	}

	public double getAmplitude() {
		return amp;
	}

	public void setFrequency(double f) {
		frq = f;
	}

	public double getFrequency() {
		return frq;
	}

	double getDcForce() {
		switch (o) {
		case NORTH:
			return -dc;
		case SOUTH:
			return dc;
		case WEST:
			return -dc;
		case EAST:
			return dc;
		}
		return dc;
	}

	double getForce(float time) {
		return getDcForce() + amp * Math.sin(frq * time);
	}

	/*
	 * the electric potential equals zero at one of the border of simulation box, depends on the direction of the field.
	 * If you use periodic boundary conditions, you will have to substract an amount of potential energy <tt>qEL</tt>
	 * each time after a particle crosses the border that corresponds to minimum potential energy in the scope of the
	 * box, or add that amount of potential energy each time after it crosses the opposite border.
	 * 
	 * @param p the passed particle @return the electric potential of the particle in this field
	 */
	double getPotential(Particle p, float time) {
		if (bounds != null && !bounds.contains(p.getRx(), p.getRy()))
			return 0;
		if (local) // we cannot do energy conservation for a local field
			return 0;
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
		h *= p.charge;
		if (p instanceof GayBerneParticle) {
			GayBerneParticle gb = (GayBerneParticle) p;
			if (gb.dipoleMoment != 0.0) {
				double angle = 0.0;
				switch (o) {
				case EAST:
					angle = Math.PI + gb.theta;
					break;
				case WEST:
					angle = Math.PI + gb.theta;
					break;
				case NORTH:
					angle = 0.5 * Math.PI + gb.theta;
					break;
				case SOUTH:
					angle = 0.5 * Math.PI + gb.theta;
					break;
				}
				h += gb.dipoleMoment * (1.0 - Math.cos(angle));
			}
		}
		return h * (getDcForce() + amp * Math.sin(frq * time));
	}

	void dyn(float dielectric, Particle p, float time) {
		if (bounds != null && !bounds.contains(p.getRx(), p.getRy()))
			return;
		double fx = 0.0, fy = 0.0;
		if (o == NORTH || o == SOUTH) {
			fy = (getDcForce() + amp * Math.sin(frq * time)) * p.charge / p.getMass();
		}
		else if (o == EAST || o == WEST) {
			fx = (getDcForce() + amp * Math.sin(frq * time)) * p.charge / p.getMass();
		}
		p.fx += fx * MDModel.GF_CONVERSION_CONSTANT / dielectric;
		p.fy += fy * MDModel.GF_CONVERSION_CONSTANT / dielectric;
		if (p instanceof GayBerneParticle) {
			GayBerneParticle gb = (GayBerneParticle) p;
			if (Math.abs(gb.dipoleMoment) > 0) {
				double angle = 0.0;
				switch (o) {
				case NORTH:
					angle = 0.5 * Math.PI + gb.theta;
					break;
				case SOUTH:
					angle = 0.5 * Math.PI + gb.theta;
					break;
				case EAST:
					angle = Math.PI + gb.theta;
					break;
				case WEST:
					angle = Math.PI + gb.theta;
					break;
				}
				gb.tau -= gb.dipoleMoment * Math.sin(angle) * MDModel.GF_CONVERSION_CONSTANT
						* (getDcForce() + amp * Math.sin(frq * time)) / gb.inertia;
			}
		}
	}

}