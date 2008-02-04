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
 * Implements the magnetic field. Lorentz force law is used to compute the magnetic force on charged particles. If no
 * bound is set, we assume that the field is applied to the entire container.
 * 
 * @author Charles Xie
 */

public class MagneticField implements VectorField, Serializable {

	private double b = 1.0;
	private Shape bounds;
	private int o = INWARD;
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

	public MagneticField() {
	}

	public MagneticField(Rectangle rect) {
		setBounds(rect);
	}

	public MagneticField(double b, int o, Shape s) {
		this.b = b;
		this.o = o;
		setBounds(s);
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
	public void setB(double b) {
		setIntensity(b);
	}

	public void setIntensity(double b) {
		this.b = b;
		o = b > 0 ? INWARD : OUTWARD;
	}

	public double getIntensity() {
		return b;
	}

	/** the orientation of the magnetic field has to be either <tt>INWARD</tt> or <tt>OUTWARD</tt>. */
	public void setOrientation(int o) {
		if (o != INWARD && o != OUTWARD)
			throw new IllegalArgumentException("Direction of magnetic field must be perpendicular to the screen");
		this.o = o;
	}

	public int getOrientation() {
		return o;
	}

	void dyn(Particle p) {
		if (bounds != null && !bounds.contains(p.getRx(), p.getRy()))
			return;
		double temp = Math.abs(b) * p.charge * MDModel.GF_CONVERSION_CONSTANT / p.getMass();
		if (o == OUTWARD)
			temp = -temp;
		p.fx += temp * p.vy;
		p.fy -= temp * p.vx;
		if (p instanceof GayBerneParticle) {
			GayBerneParticle gb = (GayBerneParticle) p;
			if (Math.abs(gb.dipoleMoment) > 0) {
				temp = Math.cos(gb.theta) * gb.vx + Math.sin(gb.theta) * gb.vy;
				if (o == OUTWARD)
					temp = -temp;
				gb.tau -= gb.dipoleMoment * temp * b / gb.inertia * MDModel.GF_CONVERSION_CONSTANT;
			}
		}
	}

	/*
	 * The magnetic potential of an atom is always zero, because magnetic force does not do any work. But for a dipole
	 * moment, it is not zero. FIXME: I didn't find a closed-form solution for an electric dipole's magnetic potential.
	 */
	double getPotential(Particle p, float time) {
		if (bounds != null && !bounds.contains(p.getRx(), p.getRy()))
			return 0;
		if (local) // we cannot do energy conservation for a local field
			return 0;
		double poten = 0.0;
		if (p instanceof GayBerneParticle) {
			GayBerneParticle gb = (GayBerneParticle) p;
			if (Math.abs(gb.dipoleMoment) > 0) {
				double temp = Math.sin(gb.theta) * gb.vx - Math.cos(gb.theta) * gb.vy;
				if (o == OUTWARD)
					temp = -temp;
				poten = gb.dipoleMoment * b * temp;
			}
		}
		return poten;
	}

}