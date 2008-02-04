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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.io.Serializable;

import org.concord.mw2d.StyleConstant;
import org.concord.mw2d.ViewAttribute;

/**
 * This is an implementation of pointwise harmonic restraint, which can be applied to an <tt>Atom</tt> or a
 * <tt>UnitedAtom</tt> such as the <tt>GayBerneParticle</tt>.
 * 
 * @author Qian Xie
 * @see org.concord.mw2d.models.Atom
 * @see org.concord.mw2d.models.UnitedAtom
 * @see org.concord.mw2d.models.GayBerneParticle
 */

public class PointRestraint implements Serializable {

	double k = 20.0, x0, y0;

	private boolean stateStored;
	private double x0Undo, y0Undo, kUndo;
	private static boolean visible = true;
	private static GeneralPath path;

	public PointRestraint() {
	}

	/**
	 * @param x0
	 *            the x0 coordinate of the equilibrium position
	 * @param y0
	 *            the y0 coordinate of the equilibrium position
	 */
	public PointRestraint(double x0, double y0) {
		this.x0 = x0;
		this.y0 = y0;
	}

	/**
	 * @param k
	 *            the strength of restraining spring
	 * @param x0
	 *            the x0 coordinate of the equilibrium position
	 * @param y0
	 *            the y0 coordinate of the equilibrium position
	 */
	public PointRestraint(double k, double x0, double y0) {
		this.k = k;
		this.x0 = x0;
		this.y0 = y0;
	}

	public PointRestraint(PointRestraint r) {
		k = r.k;
		x0 = r.x0;
		y0 = r.y0;
	}

	public void storeCurrentState() {
		x0Undo = x0;
		y0Undo = y0;
		kUndo = k;
		stateStored = true;
	}

	public void restoreState() {
		if (!stateStored)
			return;
		x0 = x0Undo;
		y0 = y0Undo;
		k = kUndo;
	}

	public String toString() {
		return "k=" + k;
	}

	public static void setVisible(boolean b) {
		visible = b;
	}

	public static boolean isVisible() {
		return visible;
	}

	public void setK(double d) {
		k = d;
	}

	public double getK() {
		return k;
	}

	public void changeK(double d) {
		if (k + d < 0)
			return;
		k += d;
	}

	public void setX0(double d) {
		x0 = d;
	}

	public double getX0() {
		return x0;
	}

	public void setY0(double d) {
		y0 = d;
	}

	public double getY0() {
		return y0;
	}

	/** Do NOT call this method unless you know about it */
	void dyn(Atom atom) {
		double d = 0.01 * k * AtomicModel.GF_CONVERSION_CONSTANT / atom.mass;
		atom.fx -= d * (atom.rx - x0);
		atom.fy -= d * (atom.ry - y0);
	}

	/** Do NOT call this method unless you know about it */
	void dyn(GayBerneParticle gb) {
		double d = 0.01 * k * AtomicModel.GF_CONVERSION_CONSTANT / gb.mass;
		gb.fx -= d * (gb.rx - x0);
		gb.fy -= d * (gb.ry - y0);
	}

	double getEnergy(Atom atom) {
		return 0.005 * k * ((atom.rx - x0) * (atom.rx - x0) + (atom.ry - y0) * (atom.ry - y0));
	}

	double getEnergy(GayBerneParticle gb) {
		return 0.005 * k * ((gb.rx - x0) * (gb.rx - x0) + (gb.ry - y0) * (gb.ry - y0));
	}

	public void copy(PointRestraint r) {
		k = r.k;
		x0 = r.x0;
		y0 = r.y0;
	}

	/**
	 * tether the center of mass of the specified particle at its current position with the specified strength
	 */
	public static void tetherParticle(Particle p, double strength) {
		if (p.restraint == null) {
			p.setRestraint(new PointRestraint(strength, p.rx, p.ry));
		}
		else {
			p.restraint.k = strength;
			p.restraint.x0 = p.rx;
			p.restraint.y0 = p.ry;
		}
	}

	/** tether the center of mass of the specified particle at its current position */
	public static void tetherParticle(Particle p) {
		if (p.restraint == null) {
			p.setRestraint(new PointRestraint(p.rx, p.ry));
		}
		else {
			p.restraint.x0 = p.rx;
			p.restraint.y0 = p.ry;
		}
	}

	/** release the specified particle */
	public static void releaseParticle(Particle p) {
		p.setRestraint(null);
	}

	private void drawCross(Graphics g) {
		if (!visible)
			return;
		g.drawLine((int) (x0 - 2.0), (int) (y0 - 2.0), (int) (x0 + 2.0), (int) (y0 + 2.0));
		g.drawLine((int) (x0 - 2.0), (int) (y0 + 2.0), (int) (x0 + 2.0), (int) (y0 - 2.0));
	}

	private static void drawSpring(Graphics2D g, Particle p, int n, int m) {
		double x = p.restraint.x0 - p.rx;
		double y = p.restraint.y0 - p.ry;
		double length = Math.sqrt(x * x + y * y);
		if (length < 5)
			return;
		double costheta = x / length;
		double sintheta = y / length;
		double delta = length / n;
		if (path == null)
			path = new GeneralPath();
		else path.reset();
		path.moveTo((float) p.rx, (float) p.ry);
		for (int i = 0; i < n; i++) {
			if (i % 2 == 0) {
				x = p.rx + (i + 0.5) * costheta * delta - 0.5 * sintheta * m;
				y = p.ry + (i + 0.5) * sintheta * delta + 0.5 * costheta * m;
			}
			else {
				x = p.rx + (i + 0.5) * costheta * delta + 0.5 * sintheta * m;
				y = p.ry + (i + 0.5) * sintheta * delta - 0.5 * costheta * m;
			}
			path.lineTo((float) x, (float) y);
		}
		path.lineTo((float) p.restraint.x0, (float) p.restraint.y0);
		g.draw(path);
		g.drawOval((int) (p.restraint.x0 - 2), (int) (p.restraint.y0 - 2), 4, 4);
		g.drawOval((int) (p.rx - 2), (int) (p.ry - 2), 4, 4);
	}

	public static void render(Graphics2D g, Particle p) {
		if (p.restraint == null || p.restraint.getK() < Particle.ZERO)
			return;
		g.setColor(Color.black);
		g.setStroke(ViewAttribute.THIN);
		switch (p.getView().getRestraintStyle()) {
		case StyleConstant.RESTRAINT_CROSS_STYLE:
			p.restraint.drawCross(g);
			break;
		case StyleConstant.RESTRAINT_HEAVY_SPRING_STYLE:
			drawSpring(g, p, 20, 20);
			break;
		case StyleConstant.RESTRAINT_LIGHT_SPRING_STYLE:
			drawSpring(g, p, 10, 10);
			break;
		}
	}

	/**
	 * get the rectangle that contains the anchor symbol
	 * 
	 * @return a rectangular cover of the anchor
	 */
	public Rectangle anchorBox() {
		return new Rectangle((int) x0 - 3, (int) y0 - 3, 6, 6);
	}

}