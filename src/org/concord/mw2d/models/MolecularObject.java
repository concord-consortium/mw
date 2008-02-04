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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import org.concord.modeler.draw.FillMode;
import org.concord.modeler.math.Vector2D;

public abstract class MolecularObject extends Molecule {

	/* default element */
	static byte element = Element.ID_MO;

	final static Stroke stroke = new BasicStroke(1.5f);
	private final static double RIGID_BOND = 50;
	private final static double RIGID_BEND = 1000;
	private final static double SOFT_BOND = 0.05;
	private final static double SOFT_BEND = 10;

	private static Color defaultBackground = new Color(255, 255, 128);
	private Color background = defaultBackground;

	FillMode fillMode = new FillMode.ColorFill(defaultBackground);

	public abstract Shape getShape();

	public boolean intersects(Atom a) {
		if (getShape() == null)
			return false;
		if (getShape().intersects(a.getBounds2D()))
			return true;
		if (getShape().contains(a.getRx(), a.getRy()))
			return true;
		return false;
	}

	/** @return true if this intersects with the specified rectangular area */
	public boolean intersects(double x, double y, double w, double h) {
		if (getShape() == null)
			return false;
		return getShape().intersects(x, y, w, h);
	}

	/** @return true if this intersects with the specified rectangle */
	public boolean intersects(Rectangle2D r) {
		if (getShape() == null)
			return false;
		return getShape().intersects(r);
	}

	public synchronized boolean contains(double x, double y) {
		if (getShape() == null)
			return false;
		return getShape().contains(x, y);
	}

	/** @return true if the specified area is inside this object */
	public boolean contains(double x, double y, double w, double h) {
		if (getShape() == null)
			return false;
		return getShape().contains(x, y, w, h);
	}

	/** @return true if the specified point is inside this object */
	public boolean contains(Point2D p) {
		if (getShape() == null)
			return false;
		return getShape().contains(p);
	}

	/** @return true if the specified rectangle is inside this object */
	public boolean contains(Rectangle2D r) {
		if (getShape() == null)
			return false;
		return getShape().contains(r);
	}

	public static void setElement(byte id) {
		element = id;
	}

	public static byte getElement() {
		return element;
	}

	public void setBackground(Color c) {
		if (c == null) {
			background = Color.lightGray;
		}
		else {
			background = c;
		}
		if (fillMode instanceof FillMode.ColorFill)
			((FillMode.ColorFill) fillMode).setColor(c);
	}

	public Color getBackground() {
		return background;
	}

	public static Color getDefaultBackground() {
		return defaultBackground;
	}

	public void setFillMode(FillMode fm) {
		fillMode = fm;
		if (fillMode == FillMode.getNoFillMode()) {
			setBackground(model.getView().getBackground());
		}
		else if (fillMode instanceof FillMode.ColorFill) {
			setBackground(((FillMode.ColorFill) fillMode).getColor());
		}
		model.getView().repaint();
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void rigidify() {
		RadialBondCollection rbc = model.getBonds();
		RadialBond rb;
		synchronized (rbc.getSynchronizationLock()) {
			for (Iterator it = rbc.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				if (contains(rb.getAtom1()) && contains(rb.getAtom2())) {
					rb.setBondStrength(RIGID_BOND);
				}
			}
		}
		AngularBondCollection abc = model.getBends();
		AngularBond ab;
		synchronized (abc.getSynchronizationLock()) {
			for (Iterator it = abc.iterator(); it.hasNext();) {
				ab = (AngularBond) it.next();
				if (contains(ab.getAtom1()) && contains(ab.getAtom2()) && contains(ab.getAtom3())) {
					ab.setBondStrength(RIGID_BEND);
				}
			}
		}
	}

	public void soften() {
		RadialBondCollection rbc = model.getBonds();
		RadialBond rb;
		synchronized (rbc.getSynchronizationLock()) {
			for (Iterator it = rbc.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				if (contains(rb.getAtom1()) && contains(rb.getAtom2())) {
					rb.setBondStrength(SOFT_BOND);
				}
			}
		}
		AngularBondCollection abc = model.getBends();
		AngularBond ab;
		synchronized (abc.getSynchronizationLock()) {
			for (Iterator it = abc.iterator(); it.hasNext();) {
				ab = (AngularBond) it.next();
				if (contains(ab.getAtom1()) && contains(ab.getAtom2()) && contains(ab.getAtom3())) {
					ab.setBondStrength(SOFT_BEND);
				}
			}
		}
	}

	public void increaseStretchingFlexibility(boolean b) {
		RadialBondCollection rbc = model.getBonds();
		RadialBond rb;
		synchronized (rbc.getSynchronizationLock()) {
			for (Iterator it = rbc.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				if (contains(rb.getAtom1()) && contains(rb.getAtom2())) {
					rb.setBondStrength(b ? rb.getBondStrength() / 2 : rb.getBondStrength() * 2);
				}
			}
		}
	}

	public void increaseBendingFlexibility(boolean b) {
		AngularBondCollection abc = model.getBends();
		AngularBond ab;
		synchronized (abc.getSynchronizationLock()) {
			for (Iterator it = abc.iterator(); it.hasNext();) {
				ab = (AngularBond) it.next();
				if (contains(ab.getAtom1()) && contains(ab.getAtom2()) && contains(ab.getAtom3())) {
					ab.setBondStrength(b ? ab.getBondStrength() / 2 : ab.getBondStrength() * 2);
				}
			}
		}
	}

	void drawVelocityVectorOfCenterOfMass(Graphics2D g) {
		Vector2D v = Statistics.getVelocityOfCenterOfMass(atoms);
		double arrowLength = Math.sqrt(v.getX() * v.getX() + v.getY() * v.getY());
		double arrowx = arrowLength < Particle.ZERO ? 0.0 : v.getX() / arrowLength;
		double arrowy = arrowLength < Particle.ZERO ? 0.0 : v.getY() / arrowLength;
		g.setColor(model.view.getVelocityFlavor().getColor());
		g.setStroke(model.view.getVelocityFlavor().getStroke());
		double lengthx = model.view.getVelocityFlavor().getLength() * v.getX();
		double lengthy = model.view.getVelocityFlavor().getLength() * v.getY();
		Point2D r = getCenterOfMass2D();
		g.drawLine((int) r.getX(), (int) r.getY(), (int) (r.getX() + lengthx), (int) (r.getY() + lengthy));
		double wingx = 5 * (arrowx * Particle.COS45 + arrowy * Particle.SIN45);
		double wingy = 5 * (arrowy * Particle.COS45 - arrowx * Particle.SIN45);
		g.drawLine((int) (r.getX() + lengthx), (int) (r.getY() + lengthy), (int) (r.getX() + lengthx - wingx), (int) (r
				.getY()
				+ lengthy - wingy));
		wingx = 5 * (arrowx * Particle.COS45 - arrowy * Particle.SIN45);
		wingy = 5 * (arrowy * Particle.COS45 + arrowx * Particle.SIN45);
		g.drawLine((int) (r.getX() + lengthx), (int) (r.getY() + lengthy), (int) (r.getX() + lengthx - wingx), (int) (r
				.getY()
				+ lengthy - wingy));
	}

	void drawMomentumVectorOfCenterOfMass(Graphics2D g) {
		Vector2D v = Statistics.getMomentumOfCenterOfMass(atoms);
		double arrowLength = Math.sqrt(v.getX() * v.getX() + v.getY() * v.getY());
		double arrowx = arrowLength < Particle.ZERO ? 0.0 : v.getX() / arrowLength;
		double arrowy = arrowLength < Particle.ZERO ? 0.0 : v.getY() / arrowLength;
		g.setColor(model.view.getMomentumFlavor().getColor());
		g.setStroke(model.view.getMomentumFlavor().getStroke());
		double lengthx = model.view.getMomentumFlavor().getLength() * v.getX();
		double lengthy = model.view.getMomentumFlavor().getLength() * v.getY();
		Point2D r = getCenterOfMass2D();
		g.drawLine((int) r.getX(), (int) r.getY(), (int) (r.getX() + lengthx), (int) (r.getY() + lengthy));
		double wingx = 5 * (arrowx * Particle.COS45 + arrowy * Particle.SIN45);
		double wingy = 5 * (arrowy * Particle.COS45 - arrowx * Particle.SIN45);
		g.drawLine((int) (r.getX() + lengthx), (int) (r.getY() + lengthy), (int) (r.getX() + lengthx - wingx), (int) (r
				.getY()
				+ lengthy - wingy));
		wingx = 5 * (arrowx * Particle.COS45 - arrowy * Particle.SIN45);
		wingy = 5 * (arrowy * Particle.COS45 + arrowx * Particle.SIN45);
		g.drawLine((int) (r.getX() + lengthx), (int) (r.getY() + lengthy), (int) (r.getX() + lengthx - wingx), (int) (r
				.getY()
				+ lengthy - wingy));
	}

}