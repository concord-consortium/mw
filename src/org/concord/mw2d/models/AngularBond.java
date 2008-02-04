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
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.concord.modeler.util.HashCodeUtil;

public class AngularBond implements ModelComponent {

	/* the first end atom of this angular bond */
	Atom atom1;

	/* the second end atom of this angular bond */
	Atom atom2;

	/* the center atom of this angular bond */
	Atom atom3;

	/* the equilibrium angle of this bond */
	double bondAngle;

	/* the strength of this angular bond */
	double bondStrength = 50;

	/* the chemical energy stored in this angular bond * */
	double chemicalEnergy;

	private static final double ZERO = 0.0001;
	private static Stroke thinStroke = new BasicStroke(1.0f);
	private static Ellipse2D ellipseIndicator;
	private static Arc2D arcIndicator;

	private boolean selected, blinking;
	private MolecularModel model;
	private boolean marked;
	private Arc2D.Double angle;
	private Arc2D arc;

	/**
	 * @param atom1
	 *            the participant at one end
	 * @param atom2
	 *            the participant at the other end
	 * @param atom3
	 *            the participant in the middle
	 * @param bondAngle
	 *            the equilibrium angle
	 * @throws java.lang.IllegalArgumentException
	 *             if at least two of the three input atoms are identical.
	 */
	public AngularBond(Atom atom1, Atom atom2, Atom atom3, double bondAngle) throws IllegalArgumentException {

		if (atom1 == atom2 || atom2 == atom3 || atom3 == atom1)
			throw new IllegalArgumentException(
					"The three participants of an angular bond must be distinct from each other!");

		angle = new Arc2D.Double();
		angle.width = angle.height = 30;

		setAtom1(atom1);
		setAtom2(atom2);
		setAtom3(atom3);
		this.bondAngle = bondAngle;

	}

	/**
	 * @param atom1
	 *            the participant at one end
	 * @param atom2
	 *            the participant at the other end
	 * @param atom3
	 *            the participant in the middle
	 * @param bondAngle
	 *            the equilibrium angle
	 * @param bondStrength
	 *            the strength of the angular harmonical potential
	 * @throws java.lang.IllegalArgumentException
	 *             if at least two of the three input atoms are identical.
	 */
	public AngularBond(Atom atom1, Atom atom2, Atom atom3, double bondAngle, double bondStrength)
			throws IllegalArgumentException {

		if (atom1 == atom2 || atom2 == atom3 || atom3 == atom1)
			throw new IllegalArgumentException(
					"The three participants of an angular bond must be distinct from each other!");

		angle = new Arc2D.Double();
		angle.width = angle.height = 30;

		setAtom1(atom1);
		setAtom2(atom2);
		setAtom3(atom3);
		this.bondAngle = bondAngle;
		this.bondStrength = bondStrength;

	}

	/**
	 * @param atom1
	 *            the participant at one end
	 * @param atom2
	 *            the participant at the other end
	 * @param atom3
	 *            the participant in the middle
	 * @param bondAngle
	 *            the equilibrium angle
	 * @param bondStrength
	 *            the strength of the angular harmonical potential
	 * @param chemicalEnergy
	 *            the chemical energy stored in this bond
	 * @throws java.lang.IllegalArgumentException
	 *             if at least two of the three input atoms are identical.
	 */
	public AngularBond(Atom atom1, Atom atom2, Atom atom3, double bondAngle, double bondStrength, double chemicalEnergy)
			throws IllegalArgumentException {

		this(atom1, atom2, atom3, bondAngle, bondStrength);
		this.chemicalEnergy = chemicalEnergy;

	}

	/**
	 * @param rb1
	 *            a wing of the angular bond
	 * @param rb2
	 *            the other wing
	 * @param bondAngle
	 *            the equilibrium angle
	 * @param bondStrength
	 *            the strength of the angular harmonical potential
	 * @throws java.lang.IllegalArgumentException
	 *             if the two input radial bonds are identical, or do not connect (namely, jointly share an atom).
	 */
	public AngularBond(RadialBond rb1, RadialBond rb2, double bondAngle, double bondStrength)
			throws IllegalArgumentException {

		angle = new Arc2D.Double();
		angle.width = angle.height = 30;

		setAtom1(rb1.getAtom1());
		setAtom2(rb1.getAtom2());

		Atom atom4 = null;
		if (rb2.getAtom1() != atom1 && rb2.getAtom1() != atom2) {
			setAtom3(rb2.getAtom1());
			if (rb2.getAtom2() != atom1 && rb2.getAtom2() != atom2)
				atom4 = rb2.getAtom2();
		}
		else if (rb2.getAtom2() != atom1 && rb2.getAtom2() != atom2) {
			setAtom3(rb2.getAtom2());
			if (rb2.getAtom1() != atom1 && rb2.getAtom1() != atom2)
				atom4 = rb2.getAtom1();
		}

		if (atom3 == null)
			throw new IllegalArgumentException(
					"The two participating radial bond for an angular bond must be distinct from each other!");

		if (atom4 != null)
			throw new IllegalArgumentException(
					"The two participating radial bond for an angular bond must have a joint atom!");

		this.bondAngle = bondAngle;
		this.bondStrength = bondStrength;

	}

	/** TODO: currently this is not implemented */
	public void storeCurrentState() {
	}

	/** TODO: currently this is not implemented */
	public void restoreState() {
	}

	/** TODO: blink this angular bond */
	public void blink() {
	}

	/** As an angular bond is always invisible, calling this method has no effect */
	public void setVisible(boolean b) {
	}

	/** As an angular bond is always invisible, this method always returns false */
	public boolean isVisible() {
		return false;
	}

	public void destroy() {
		atom1 = null;
		atom2 = null;
		atom3 = null;
		model = null;
	}

	public int getIndex() {
		if (model.bends == null)
			return -1;
		return model.bends.indexOf(this);
	}

	/** return x-coordinate of the center of this bond */
	public double getRx() {
		return 0.333333333 * (atom1.getRx() + atom2.getRx() + atom3.getRx());
	}

	/** return y-coordinate of the center of this bond */
	public double getRy() {
		return 0.333333333 * (atom1.getRy() + atom2.getRy() + atom3.getRy());
	}

	/** @return the current angle of this bond */
	public double getAngleExtent() {
		setArc();
		return angle.getAngleExtent();
	}

	public boolean equals(Object o) {
		if (!(o instanceof AngularBond))
			return false;
		return atom1 == ((AngularBond) o).atom1 && atom2 == ((AngularBond) o).atom2 && atom3 == ((AngularBond) o).atom3;
	}

	public int hashCode() {
		return ((atom1.getIndex() & 0xFF) << 20) | ((atom2.getIndex() & 0xFF) << 10) | ((atom3.getIndex() & 0xFF) << 0);
	}

	/**
	 * @deprecated A covalent bond cannot be cloned, because it is a secondary structure depicting the relationship
	 *             between atoms. You can clone entities such as a particle or a molecule, but not the relationships
	 *             between entities.
	 */
	public Object clone() {
		throw new RuntimeException("Do not call this method");
	}

	public boolean contains(double x, double y) {
		setArc();
		double narrow = Math.abs(angle.extent) * 0.2;
		if (arc == null)
			arc = new Arc2D.Double();
		arc.setArc(atom3.rx - 0.5 * angle.width, atom3.ry - 0.5 * angle.height, angle.width, angle.height,
				angle.extent > 0 ? angle.start + narrow : angle.start - narrow, angle.extent > 0 ? angle.extent
						- narrow - narrow : angle.extent + narrow + narrow, Arc2D.PIE);
		return arc.contains(x, y);
	}

	public boolean contains(double x, double y, double w, double h) {
		setArc();
		return angle.contains(x, y, w, h);
	}

	public boolean contains(Point2D p) {
		return contains(p.getX(), p.getY());
	}

	public boolean contains(Rectangle2D r) {
		setArc();
		return angle.contains(r);
	}

	public boolean containsAngle(double d) {
		setArc();
		return angle.containsAngle(d);
	}

	public Rectangle2D getBounds2D() {
		setArc();
		return angle.getBounds2D();
	}

	public Rectangle getBounds() {
		setArc();
		return angle.getBounds();
	}

	public Rectangle2D getFrame() {
		setArc();
		return angle.getFrame();
	}

	public double getCenterX() {
		setArc();
		return angle.getCenterX();
	}

	public double getCenterY() {
		setArc();
		return angle.getCenterY();
	}

	public double getMaxX() {
		setArc();
		return angle.getMaxX();
	}

	public double getMaxY() {
		setArc();
		return angle.getMaxY();
	}

	public double getMinX() {
		setArc();
		return angle.getMinX();
	}

	public double getMinY() {
		setArc();
		return angle.getMinY();
	}

	public Point2D getEndPoint() {
		setArc();
		return angle.getEndPoint();
	}

	public Point2D getStartPoint() {
		setArc();
		return angle.getStartPoint();
	}

	public boolean intersects(double x, double y, double w, double h) {
		setArc();
		return angle.intersects(x, y, w, h);
	}

	public boolean intersects(Rectangle2D r) {
		setArc();
		return angle.intersects(r);
	}

	public String toString() {
		return "Angular Bond (" + atom1 + ", " + atom2 + ", " + atom3 + ", "
				+ Particle.format.format(Math.round(bondAngle * 180.0 / Math.PI)) + ", "
				+ Particle.format.format(bondStrength) + ")";
	}

	/** set the model this bond is associated with */
	public void setModel(MDModel m) {
		if (!(m instanceof MolecularModel))
			throw new IllegalArgumentException("not a molecular model!");
		model = (MolecularModel) m;
	}

	public MDModel getHostModel() {
		return model;
	}

	public void setSelected(boolean b) {
		selected = b;
		if (b) {
			setArc();
			setIndicators();
			model.view.setSelectedComponent(this);
		}
	}

	public boolean isSelected() {
		return selected;
	}

	public void setMarked(boolean b) {
		marked = b;
	}

	public boolean isMarked() {
		return marked;
	}

	public void setBlinking(boolean b) {
		blinking = b;
	}

	public boolean isBlinking() {
		return blinking;
	}

	private void setArc() {
		double r13 = Math.sqrt((atom3.rx - atom1.rx) * (atom3.rx - atom1.rx) + (atom3.ry - atom1.ry)
				* (atom3.ry - atom1.ry));
		double r23 = Math.sqrt((atom2.rx - atom3.rx) * (atom2.rx - atom3.rx) + (atom2.ry - atom3.ry)
				* (atom2.ry - atom3.ry));
		double t = ((atom3.rx - atom1.rx) * (atom2.rx - atom3.rx) + (atom3.ry - atom1.ry) * (atom2.ry - atom3.ry))
				/ (r13 * r23);
		angle.setAngleExtent((atom1.rx - atom3.rx) * (atom2.ry - atom3.ry) - (atom1.ry - atom3.ry)
				* (atom2.rx - atom3.rx) < 0 ? 180.0 - Math.toDegrees(Math.acos(t))
				: Math.toDegrees(Math.acos(t)) - 180.0);
		t = (atom1.rx - atom3.rx) / r13;
		angle.start = atom1.ry < atom3.ry ? Math.toDegrees(Math.acos(t)) : -Math.toDegrees(Math.acos(t));
		angle.x = atom3.rx - 0.5 * angle.width;
		angle.y = atom3.ry - 0.5 * angle.height;
	}

	static double getAngle(Atom a1, Atom a3, Atom a2) {
		double r13 = Math.sqrt((a3.rx - a1.rx) * (a3.rx - a1.rx) + (a3.ry - a1.ry) * (a3.ry - a1.ry));
		double r23 = Math.sqrt((a2.rx - a3.rx) * (a2.rx - a3.rx) + (a2.ry - a3.ry) * (a2.ry - a3.ry));
		double t = ((a3.rx - a1.rx) * (a2.rx - a3.rx) + (a3.ry - a1.ry) * (a2.ry - a3.ry)) / (r13 * r23);
		return (a1.rx - a3.rx) * (a2.ry - a3.ry) - (a1.ry - a3.ry) * (a2.rx - a3.rx) < 0 ? Math.PI - Math.acos(t)
				: Math.acos(t) - Math.PI;
	}

	static double getAngle(double x1, double y1, double x3, double y3, double x2, double y2) {
		double r13 = Math.sqrt((x3 - x1) * (x3 - x1) + (y3 - y1) * (y3 - y1));
		double r23 = Math.sqrt((x2 - x3) * (x2 - x3) + (y2 - y3) * (y2 - y3));
		double t = ((x3 - x1) * (x2 - x3) + (y3 - y1) * (y2 - y3)) / (r13 * r23);
		return (x1 - x3) * (y2 - y3) - (y1 - y3) * (x2 - x3) < 0 ? Math.PI - Math.acos(t) : Math.acos(t) - Math.PI;
	}

	double getAngle(int frame) {
		if (frame < 0)
			return getAngle(atom1, atom3, atom2);
		return getAngle(atom1.rxryQ.getQueue1().getData(frame), atom1.rxryQ.getQueue2().getData(frame), atom3.rxryQ
				.getQueue1().getData(frame), atom3.rxryQ.getQueue2().getData(frame), atom2.rxryQ.getQueue1().getData(
				frame), atom2.rxryQ.getQueue2().getData(frame));
	}

	public int indexOf(Atom atom) {
		if (atom == atom1)
			return 0;
		if (atom == atom2)
			return 1;
		if (atom == atom3)
			return 2;
		return -1;
	}

	public boolean contains(Atom atom) {
		return atom1 == atom || atom2 == atom || atom3 == atom;
	}

	public Atom getAtom1() {
		return atom1;
	}

	public Atom getAtom2() {
		return atom2;
	}

	public Atom getAtom3() {
		return atom3;
	}

	public void setAtom1(Atom atom) {
		atom1 = atom;
	}

	public void setAtom2(Atom atom) {
		atom2 = atom;
	}

	public void setAtom3(Atom atom) {
		atom3 = atom;
		angle.height = angle.width = atom3.sigma + 10;
	}

	public double getBondAngle() {
		return bondAngle;
	}

	public void setBondAngle(double d) {
		bondAngle = d;
	}

	public double getBondStrength() {
		return bondStrength;
	}

	public void setBondStrength(double d) {
		bondStrength = d;
	}

	public double getChemicalEnergy() {
		return chemicalEnergy;
	}

	public void setChemicalEnergy(double d) {
		chemicalEnergy = d;
	}

	private void setIndicators() {
		if (ellipseIndicator == null)
			ellipseIndicator = new Ellipse2D.Double();
		if (arcIndicator == null)
			arcIndicator = new Arc2D.Double();
		ellipseIndicator.setFrame(angle.x, angle.y, angle.width, angle.height);
		arcIndicator.setArc(angle.x, angle.y, angle.width, angle.height, angle.start, angle.extent, Arc2D.PIE);
	}

	public void render(Graphics2D g, Color c) {

		if (model == null)
			return;

		setArc();
		g.setStroke(thinStroke);
		g.setColor(c);
		g.draw(angle);

		if (selected && model.view.getShowSelectionHalo()) {
			g.setColor(Color.red);
			g.fill(arcIndicator);
			g.setColor(c);
			g.draw(ellipseIndicator);
		}

	}

	public static class Delegate extends ComponentDelegate {

		private int atom1, atom2, atom3;
		private double bondAngle, bondStrength;
		private double chemicalEnergy;

		public Delegate() {
		}

		public Delegate(AngularBond ab) {
			atom1 = ab.atom1.index;
			atom2 = ab.atom2.index;
			atom3 = ab.atom3.index;
			bondAngle = ab.bondAngle;
			bondStrength = ab.bondStrength;
			chemicalEnergy = ab.chemicalEnergy;
		}

		public Delegate(int atom1, int atom2, int atom3, double bondAngle, double bondStrength) {
			if (atom1 == atom2 || atom2 == atom3 || atom3 == atom1)
				throw new IllegalArgumentException(
						"The three participants of an angular bond must be distinct from each other!");
			this.atom1 = atom1;
			this.atom2 = atom2;
			this.atom3 = atom3;
			this.bondAngle = bondAngle;
			this.bondStrength = bondStrength;
		}

		public int getAtom1() {
			return atom1;
		}

		public void setAtom1(int a) {
			atom1 = a;
		}

		public int getAtom2() {
			return atom2;
		}

		public void setAtom2(int a) {
			atom2 = a;
		}

		public int getAtom3() {
			return atom3;
		}

		public void setAtom3(int a) {
			atom3 = a;
		}

		public double getBondAngle() {
			return bondAngle;
		}

		public void setBondAngle(double d) {
			bondAngle = d;
		}

		public double getBondStrength() {
			return bondStrength;
		}

		public void setBondStrength(double d) {
			bondStrength = d;
		}

		public double getChemicalEnergy() {
			return chemicalEnergy;
		}

		public void setChemicalEnergy(double d) {
			chemicalEnergy = d;
		}

		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (!(obj instanceof AngularBond.Delegate))
				return false;
			AngularBond.Delegate abd = (AngularBond.Delegate) obj;
			return atom1 == abd.getAtom1() && atom2 == abd.getAtom2() && atom3 == abd.getAtom3()
					&& Math.abs(bondAngle - abd.getBondAngle()) < ZERO
					&& Math.abs(bondStrength - abd.getBondStrength()) < ZERO
					&& Math.abs(chemicalEnergy - abd.getChemicalEnergy()) < ZERO;
		}

		public int hashCode() {
			int result = HashCodeUtil.hash(HashCodeUtil.SEED, atom1);
			result = HashCodeUtil.hash(result, atom2);
			result = HashCodeUtil.hash(result, atom3);
			result = HashCodeUtil.hash(result, bondAngle);
			result = HashCodeUtil.hash(result, bondStrength);
			result = HashCodeUtil.hash(result, chemicalEnergy);
			return result;
		}

		public String toString() {
			return "[" + atom1 + "," + atom2 + "," + atom3 + "]";
		}

	}

}