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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.Serializable;

import org.concord.mw2d.MDView;
import org.concord.mw2d.ViewAttribute;

public class UserField implements VectorField, Serializable {

	public final static float INCREMENT = 0.025f;
	private static final int GEAR_MAX = 20;
	private static boolean renderable = true;

	private double u = INCREMENT;
	Shape bounds;
	private int gear = 1;
	private double costheta, sintheta;

	/**
	 * Don't call this constructor. This constructor is for XML-encoding beans. The user has to know exactly where he
	 * introduce this field, otherwise it won't know how to calculate.
	 */
	public UserField() {
	}

	public UserField(double u, Rectangle d) throws IllegalArgumentException {
		this.u = u;
		bounds = d;
	}

	public void setLocal(boolean b) {
	}

	public boolean isLocal() {
		return false;
	}

	public static void setRenderable(boolean b) {
		renderable = b;
	}

	public static boolean isRenderable() {
		return renderable;
	}

	public double getIntensity() {
		return u;
	}

	public void setIntensity(double d) {
		u = d;
	}

	public void setAngle(double costheta, double sintheta) {
		this.costheta = costheta;
		this.sintheta = sintheta;
	}

	public int getOrientation() {
		if (Math.abs(costheta) < Particle.ZERO) {
			if (Math.abs(sintheta - 1) < Particle.ZERO)
				return SOUTH;
			if (Math.abs(sintheta + 1) < Particle.ZERO)
				return NORTH;
		}
		if (Math.abs(sintheta) < Particle.ZERO) {
			if (Math.abs(costheta - 1) < Particle.ZERO)
				return EAST;
			if (Math.abs(costheta + 1) < Particle.ZERO)
				return WEST;
		}
		return -1;
	}

	public void setBounds(Shape shape) {
		bounds = shape;
	}

	public synchronized void setGear(int i) {
		gear = i;
	}

	public synchronized int getGear() {
		return gear;
	}

	public void increaseGear() {
		if (gear < GEAR_MAX)
			gear++;
	}

	public void decreaseGear() {
		if (gear >= 2)
			gear--;
	}

	synchronized void dyn(Particle p) {
		if (Math.abs(u) < Particle.ZERO)
			return;
		p.fx += u * costheta / p.getMass() * MDModel.GF_CONVERSION_CONSTANT;
		p.fy += u * sintheta / p.getMass() * MDModel.GF_CONVERSION_CONSTANT;
	}

	synchronized void dyn(RectangularObstacle obs) {
		if (Math.abs(u) < Particle.ZERO)
			return;
		obs.ax += u * costheta / obs.getMass() * AtomicModel.GF_CONVERSION_CONSTANT;
		obs.ay += u * sintheta / obs.getMass() * AtomicModel.GF_CONVERSION_CONSTANT;
	}

	void render(Graphics2D g, Object p, boolean b) {

		if (!renderable)
			return;

		double x = 0, y = 0, grx = -5.0, gry = -5.0, gd = 10.0;
		if (p instanceof GayBerneParticle) {
			GayBerneParticle gb = (GayBerneParticle) p;
			x = gb.rx;
			y = gb.ry;
			grx += x - 0.2 * gb.breadth;
			gry += y - 0.2 * gb.breadth;
			gd += 0.4 * gb.breadth;
		}
		else if (p instanceof Atom) {
			Atom a = (Atom) p;
			x = a.rx;
			y = a.ry;
			grx += x - 0.5 * a.sigma;
			gry += y - 0.5 * a.sigma;
			gd += a.sigma;
		}
		else if (p instanceof RectangularObstacle) {
			RectangularObstacle obs = (RectangularObstacle) p;
			x = obs.x + 0.5 * obs.width;
			y = obs.y + 0.5 * obs.height;
			grx += x - 10;
			gry += y - 10;
			gd += 20;
		}
		if (p instanceof ModelComponent) {
			g.setColor(((MDView) ((ModelComponent) p).getHostModel().getView()).contrastBackground());
		}
		g.setStroke(b ? ViewAttribute.THIN : ViewAttribute.THIN_DASHED);
		g.drawOval((int) grx, (int) gry, (int) gd, (int) gd);
		g.drawOval((int) (x - 4), (int) (y - 4), 8, 8);
		int vx1 = (int) (grx + 0.5 * gd);
		int vy1 = (int) (gry - 2.5);
		int vy2 = (int) (gry + gd + 2.5);
		int hx1 = (int) (grx - 2.5);
		int hy1 = (int) (gry + 0.5 * gd);
		int hx2 = (int) (grx + gd + 2.5);
		g.drawLine(vx1, vy1, vx1, vy2);
		g.drawLine(hx1, hy1, hx2, hy1);
		g.drawLine(hx1 + 2, vy1 + 2, hx2 - 2, vy2 - 2);
		g.drawLine(hx1 + 2, vy2 - 2, hx2 - 2, vy1 + 2);

		if (!b)
			return;

		if (u > 0) {
			double finc = 0.5 * gd + Math.log(gear) * 10;
			double endx = x + finc * costheta;
			double endy = y + finc * sintheta;
			double arrowx = 1.0 * costheta;
			double arrowy = 1.0 * sintheta;
			g.setStroke(ViewAttribute.MODERATE);
			g.drawLine((int) x, (int) y, (int) endx, (int) endy);
			grx = 5 * (arrowx * Particle.COS45 + arrowy * Particle.SIN45);
			gry = 5 * (arrowy * Particle.COS45 - arrowx * Particle.SIN45);
			g.drawLine((int) endx, (int) endy, (int) (endx - grx), (int) (endy - gry));
			grx = 5 * (arrowx * Particle.COS45 - arrowy * Particle.SIN45);
			gry = 5 * (arrowy * Particle.COS45 + arrowx * Particle.SIN45);
			g.drawLine((int) endx, (int) endy, (int) (endx - grx), (int) (endy - gry));
		}

	}

}