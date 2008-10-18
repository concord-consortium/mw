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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.concord.mw2d.UserAction;
import org.concord.mw2d.ViewAttribute;

public class Molecule implements ModelComponent, Rotatable {

	private final static Color DARK_GREEN = new Color(0x99cc00);

	List<Atom> atoms;
	private Map<Atom, Point2D.Double> savedCRD;
	private double savedHandleX, savedHandleY;
	private Point2D savedCenter;
	private boolean marked;
	private boolean selected, blinking;
	private MolecularTorque torque;
	private double xCenter, yCenter;
	private boolean selectedToRotate;

	MolecularModel model;

	/*
	 * The graphical handle that the user can grab to rotate this molecule. If null, no graphics associated with this
	 * method will be displayed.
	 */
	static Rectangle2D.Double rotateRect;

	/*
	 * The graphical components for rotating this molecule. If null, no graphics associated with this method will be
	 * displayed.
	 */
	static Line2D.Double[] rotateCrossLine;

	public Molecule() {
		super();
		atoms = Collections.synchronizedList(new ArrayList<Atom>());
		if (rotateRect == null)
			rotateRect = new Rectangle2D.Double();
		if (rotateCrossLine == null)
			rotateCrossLine = new Line2D.Double[] { new Line2D.Double(), new Line2D.Double() };
	}

	public void addAtom(Atom a) {
		atoms.add(a);
	}

	public Atom getAtom(int i) {
		return atoms.get(i);
	}

	public int indexOfAtom(Atom a) {
		return atoms.indexOf(a);
	}

	public Iterator iterator() {
		return atoms.iterator();
	}

	public Object getSynchronizedLock() {
		return atoms;
	}

	public void addAll(List<Atom> l) {
		atoms.addAll(l);
	}

	@SuppressWarnings("unchecked")
	public void sortAtoms() {
		Collections.sort(atoms);
	}

	public Molecule getCopy() {
		Molecule m = new Molecule();
		m.atoms.addAll(atoms);
		return m;
	}

	public int size() {
		return atoms.size();
	}

	public void clear() {
		atoms.clear();
	}

	public boolean contains(Object o) {
		if (o instanceof Atom)
			return atoms.contains(o);
		if (o instanceof RadialBond)
			return atoms.contains(((RadialBond) o).atom1);
		if (o instanceof AngularBond)
			return atoms.contains(((AngularBond) o).atom1);
		return false;
	}

	public boolean equals(Object o) {
		if (!(o instanceof Molecule))
			return false;
		Molecule m = (Molecule) o;
		if (m.size() != size())
			return false;
		synchronized (m.getSynchronizedLock()) {
			for (Iterator i = m.iterator(); i.hasNext();) {
				if (!contains(i.next()))
					return false;
			}
		}
		return true;
	}

	// from Joshua Bloch's book
	public int hashCode() {
		int result = 17;
		synchronized (atoms) {
			for (Atom a : atoms)
				result = 37 * result + a.getIndex();
		}
		return result;
	}

	public void destroy() {
		clear();
		if (savedCRD != null)
			savedCRD.clear();
		model = null;
	}

	/** @return a string that indicate the indices of atoms of this molecule */
	public String toString() {
		StringBuffer sb = new StringBuffer("[");
		synchronized (atoms) {
			for (Atom a : atoms) {
				sb.append(a.getIndex());
				sb.append(", ");
			}
		}
		sb.replace(sb.length() - 2, sb.length() - 1, "]");
		return sb.toString();
	}

	/**
	 * return the kinetic energy of this molecule.
	 */
	public double getKineticEnergy() {
		if (atoms.isEmpty())
			return 0.0;
		double x = 0.0;
		synchronized (atoms) {
			for (Atom a : atoms)
				x += (a.vx * a.vx + a.vy * a.vy) * a.mass;
		}
		x /= size();
		// the prefactor 0.5 doesn't show up here because of mass unit conversion.
		return x * MDModel.EV_CONVERTER;
	}

	/** @return true if the specified location is inside the bounding box of this molecule */
	public boolean contains(double x, double y) {
		return getBounds2D().contains(x, y);
	}

	/** @return true if the specified area is inside the bounding box of this molecule */
	public boolean contains(double x, double y, double w, double h) {
		return getBounds2D().contains(x, y, w, h);
	}

	/** @return true if the specified point is inside the bounding box of this molecule */
	public boolean contains(Point2D p) {
		return getBounds2D().contains(p);
	}

	/** @return true if the specified rectangle is inside the bounding box of this molecule */
	public boolean contains(Rectangle2D r) {
		return getBounds2D().contains(r);
	}

	/** @return the bounding box of this molecule */
	public Rectangle getBounds() {
		return getBounds(0);
	}

	/** @return the bounding box of this molecule */
	public Rectangle2D getBounds2D() {
		return getBounds(0).getBounds2D();
	}

	/** @return true if the bounding box of this molecule intersects with the specified rectangular area */
	public boolean intersects(double x, double y, double w, double h) {
		return getBounds2D().intersects(x, y, w, h);
	}

	/** @return true if the bounding box of this molecule intersects with the specified rectangle */
	public boolean intersects(Rectangle2D r) {
		return getBounds2D().intersects(r);
	}

	public void setModel(MDModel m) {
		if (!(m instanceof MolecularModel))
			throw new IllegalArgumentException("must be a MolecularModel");
		model = (MolecularModel) m;
	}

	public MDModel getHostModel() {
		return model;
	}

	public void setSelected(boolean b) {
		for (Atom x : atoms)
			x.setSelected(b);
		List<RadialBond> bonds = getBonds();
		if (bonds != null)
			for (RadialBond x : bonds)
				x.setSelected(b);
		if (b) {
			locateRotationHandles();
			model.view.setSelectedComponent(this);
		}
		selected = b;
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

	public void setVisible(boolean b) {
		for (Atom a : atoms)
			a.setVisible(b);
		List<RadialBond> bonds = getBonds();
		for (RadialBond x : bonds)
			x.setVisible(b);
	}

	public boolean isVisible() {
		if (atoms.isEmpty())
			return false;
		return atoms.get(0).isVisible();
	}

	public void setDraggable(boolean b) {
		for (Atom a : atoms)
			a.setDraggable(b);
	}

	public boolean isDraggable() {
		if (atoms.isEmpty())
			return false;
		return atoms.get(0).isDraggable();
	}

	public void setTorque(MolecularTorque mt) {
		torque = mt;
	}

	public MolecularTorque getTorque() {
		return torque;
	}

	/* count the bonded partners for a given atom of this molecule. */
	int getBondedPartnerCount(List<RadialBond> bond, Atom atom) {
		int count = 0;
		for (RadialBond rb : bond) {
			if (atom.equals(rb.getAtom1()) || atom.equals(rb.getAtom2()))
				count++;
		}
		return count;
	}

	/* return the atoms that are bonded to the specified atom */
	@SuppressWarnings("unchecked")
	Atom[] getBondedPartners(List<RadialBond> bond, Atom atom) {
		List<Atom> list = new ArrayList<Atom>();
		for (RadialBond rb : bond) {
			if (atom.equals(rb.getAtom1()))
				list.add(rb.getAtom2());
			if (atom.equals(rb.getAtom2()))
				list.add(rb.getAtom1());
		}
		if (list.isEmpty())
			return new Atom[0];
		Collections.sort(list);
		Atom[] a = new Atom[list.size()];
		for (int i = 0; i < a.length; i++)
			a[i] = list.get(i);
		return a;
	}

	/**
	 * get the terminal atoms when this molecule is a Polypeptide. Caution: Do NOT call this method for general
	 * molecules. This method is not moved to Polypeptide because of legacy problem.
	 */
	public Atom[] getTermini() {
		Atom[] a = new Atom[2];
		int n = 0;
		synchronized (atoms) {
			for (Atom i : atoms) {
				if (model.bonds.getBondedPartnerCount(i) == 1) {
					a[n] = i;
					n++;
				}
			}
		}
		if (n != 2)
			throw new RuntimeException("terminus incorrect: " + n);
		return a;
	}

	boolean isPolypeptide() {
		synchronized (atoms) {
			for (Atom a : atoms) {
				if (!a.isAminoAcid())
					return false;
			}
		}
		return true;
	}

	boolean isDNAStrand() {
		synchronized (atoms) {
			for (Atom a : atoms) {
				if (!a.isNucleotide())
					return false;
			}
		}
		return true;
	}

	public void setFriction(float friction) {
		if (size() == 0)
			return;
		synchronized (atoms) {
			for (Atom a : atoms)
				a.setFriction(friction);
		}
	}

	public void initializeMovieQ(int n) {
		if (size() == 0)
			return;
		synchronized (atoms) {
			for (Atom a : atoms)
				a.initializeMovieQ(n);
		}
	}

	public double getAngle() {
		return 0;
	}

	public int getRotationHandle(int x, int y) {
		return rotateRect.contains(x, y) ? 0 : -1;
	}

	public boolean isSelectedToRotate() {
		return selectedToRotate;
	}

	public void setSelectedToRotate(boolean b) {
		selectedToRotate = b;
	}

	private void locateRotationHandles() {
		Point com = getCenterOfMass();
		if (com.x == Math.round(rotateCrossLine[0].x1) && com.y == Math.round(rotateCrossLine[0].y1))
			return;
		rotateRect.setRect(com.x - 64, com.y - 4, 8, 8);
		rotateCrossLine[0].setLine(com.x, com.y, com.x - 60, com.y);
		rotateCrossLine[1].setLine(com.x - 60, com.y - 10, com.x - 60, com.y + 10);
	}

	public void storeCurrentState() {
		savedCenter = getCenterOfMass2D();
		if (savedCRD == null) {
			savedCRD = new HashMap<Atom, Point2D.Double>();
		}
		else {
			savedCRD.clear();
		}
		synchronized (atoms) {
			for (Atom a : atoms)
				savedCRD.put(a, new Point2D.Double(a.getRx(), a.getRy()));
		}
		if (selected) {
			savedHandleX = rotateCrossLine[0].x2;
			savedHandleY = rotateCrossLine[0].y2;
		}
	}

	public void restoreState() {
		if (savedCRD == null || savedCRD.isEmpty())
			return;
		Point2D point;
		synchronized (atoms) {
			for (Atom a : atoms) {
				point = savedCRD.get(a);
				if (point != null) {
					a.setRx(point.getX());
					a.setRy(point.getY());
				}
			}
		}
	}

	/** TODO: blink this molecule as a whole */
	public void blink() {
	}

	/**
	 * check if any atom of this molecule is not contained in the specified rectangle.
	 * 
	 * @return true if atoms are out of bounds
	 */
	public boolean isOutside(Boundary bound) {
		synchronized (atoms) {
			for (Atom a : atoms) {
				if (!bound.contains(a.rx, a.ry))
					return true;
			}
		}
		return false;
	}

	/**
	 * translate this molecule by a given distance.
	 * 
	 * @param x
	 *            the x-component of the translation
	 * @param y
	 *            the y-component of the translation
	 */
	public void translateBy(double x, double y) {
		synchronized (atoms) {
			for (Atom a : atoms) {
				a.rx += x;
				a.ry += y;
			}
		}
	}

	/**
	 * translate the center of mass of this molecule to a given position.
	 * 
	 * @param x
	 *            the x-component of the translation
	 * @param y
	 *            the y-component of the translation
	 */
	public void translateTo(double x, double y) {
		Point2D p2d = getCenterOfMass2D();
		double xDisplacement = x - p2d.getX();
		double yDisplacement = y - p2d.getY();
		synchronized (atoms) {
			for (Atom a : atoms) {
				a.rx += xDisplacement;
				a.ry += yDisplacement;
			}
		}
	}

	public void translateTo(Point2D p) {
		translateTo(p.getX(), p.getY());
	}

	/** translate the molecule by moving the specified bond to the specified location */
	public void translateBondCenterTo(RadialBond rb, double x, double y) {
		if (contains(rb)) {
			double x0 = rb.getRx();
			double y0 = rb.getRy();
			translateBy(x - x0, y - y0);
		}
		else {
			throw new IllegalArgumentException("RadialBond " + rb + " doesn't belong to this molecule");
		}
	}

	/** translate the molecule by moving the specified atom to the specified location */
	public void translateAtomTo(Atom a, double x, double y) {
		if (contains(a)) {
			double x0 = a.getRx();
			double y0 = a.getRy();
			translateBy(x - x0, y - y0);
		}
		else {
			throw new IllegalArgumentException("Atom " + a + " doesn't belong to this molecule");
		}
	}

	void rotateBondToAngle(RadialBond rb, double angle) {
		double delta = angle - rb.getAngle();
		double costheta0 = Math.cos(delta);
		double sintheta0 = Math.sin(delta);
		if (savedCenter == null)
			savedCenter = getCenterOfMass2D();
		double costheta, sintheta, distance;
		synchronized (atoms) {
			for (Atom at : atoms) {
				costheta = at.rx - savedCenter.getX();
				sintheta = at.ry - savedCenter.getY();
				distance = Math.hypot(costheta, sintheta);
				if (distance > 0.1) {
					costheta /= distance;
					sintheta /= distance;
					at.rx = savedCenter.getX() + distance * (costheta * costheta0 - sintheta * sintheta0);
					at.ry = savedCenter.getY() + distance * (sintheta * costheta0 + costheta * sintheta0);
				}
			}
		}
	}

	/**
	 * rotate this molecule such that it points to a specified point (usually the mouse cursor).
	 * 
	 * @param x
	 *            the x coordinate of the hot spot
	 * @param y
	 *            the y coordinate of the hot spot
	 * @param handle
	 *            the index of the handle (0 for now, as there is only one handle)
	 */
	public void rotateTo(int x, int y, int handle) {

		if (savedCenter == null)
			savedCenter = getCenterOfMass2D();

		double dx = x - savedCenter.getX();
		double dy = y - savedCenter.getY();
		double distance = Math.hypot(dx, dy);
		if (distance < 1.0)
			return;

		double sintheta0 = 0, costheta0 = 0;
		boolean b = savedHandleX != 0 && savedHandleY != 0;
		if (b) {
			costheta0 = savedHandleX - savedCenter.getX();
			sintheta0 = savedHandleY - savedCenter.getY();
			double r = Math.hypot(costheta0, sintheta0);
			costheta0 /= r;
			sintheta0 /= r;
		}

		double costheta = dx / distance;
		double sintheta = dy / distance;
		x = (int) (savedCenter.getX() + 60.0 * costheta);
		y = (int) (savedCenter.getY() + 60.0 * sintheta);
		rotateRect.setRect(x - 4, y - 4, 8, 8);
		rotateCrossLine[0].setLine(savedCenter.getX(), savedCenter.getY(), x, y);
		rotateCrossLine[1].setLine(x - 10.0 * sintheta, y + 10.0 * costheta, x + 10.0 * sintheta, y - 10.0 * costheta);

		if (b) {
			// delta = theta - theta0
			dx = costheta * costheta0 + sintheta * sintheta0;
			dy = sintheta * costheta0 - costheta * sintheta0;
			costheta = dx;
			sintheta = dy;
		}

		Point2D oldPoint;
		double oldX, oldY;
		synchronized (atoms) {
			for (Atom at : atoms) {
				if (savedCRD == null || savedCRD.isEmpty()) {
					oldX = at.rx;
					oldY = at.ry;
				}
				else {
					oldPoint = savedCRD.get(at);
					oldX = oldPoint.getX();
					oldY = oldPoint.getY();
				}
				dx = oldX - savedCenter.getX();
				dy = oldY - savedCenter.getY();
				distance = Math.hypot(dx, dy);
				if (distance > 0.1) {
					costheta0 = dx / distance;
					sintheta0 = dy / distance;
					// theta0 + delta
					at.rx = savedCenter.getX() + distance * (costheta * costheta0 - sintheta * sintheta0);
					at.ry = savedCenter.getY() + distance * (sintheta * costheta0 + costheta * sintheta0);
				}
			}
		}

	}

	/**
	 * rotate this molecule by the specified angle, around its center of mass.
	 * 
	 * @param angle
	 *            the angle this molecule is to rotate, in degrees. Clockwise rotation angle is positive.
	 */
	public void rotateBy(double angleInDegrees) {
		BitSet bs = new BitSet(model.getNumberOfAtoms());
		for (Atom a : atoms)
			bs.set(a.getIndex());
		model.setSelectionSet(bs);
		model.rotateSelectedParticles(angleInDegrees);
	}

	public void flipVertical() {
		BitSet bs = new BitSet(model.getNumberOfAtoms());
		for (Atom a : atoms)
			bs.set(a.getIndex());
		model.setSelectionSet(bs);
		model.flipSelectedParticles((byte) 0);
	}

	public void flipHorizontal() {
		BitSet bs = new BitSet(model.getNumberOfAtoms());
		for (Atom a : atoms)
			bs.set(a.getIndex());
		model.setSelectionSet(bs);
		model.flipSelectedParticles((byte) 1);
	}

	public double getRx() {
		return getCenterOfMass2D().getX();
	}

	public double getRy() {
		return getCenterOfMass2D().getY();
	}

	/** get the center of mass of this molecule */
	public Point2D getCenterOfMass2D() {
		double xcm = 0.0, ycm = 0.0;
		double mass = 0.0;
		synchronized (atoms) {
			for (Atom a : atoms) {
				xcm += a.rx * a.mass;
				ycm += a.ry * a.mass;
				mass += a.mass;
			}
		}
		xcm /= mass;
		ycm /= mass;
		return new Point2D.Double(xcm, ycm);
	}

	/** get the center of mass (in pixels) of this molecule */
	public Point getCenterOfMass() {
		double xcm = 0.0, ycm = 0.0;
		double mass = 0.0;
		synchronized (atoms) {
			for (Atom a : atoms) {
				xcm += a.rx * a.mass;
				ycm += a.ry * a.mass;
				mass += a.mass;
			}
		}
		return new Point((int) (xcm / mass), (int) (ycm / mass));
	}

	/**
	 * get the radius of gyration of this molecule around a given pivot
	 * 
	 * @param pivot
	 *            the pivotal point
	 */
	public float getRadiusOfGyration(Point2D pivot) {
		double xd = 0.0, yd = 0.0;
		synchronized (atoms) {
			for (Atom a : atoms) {
				xd += (a.rx - pivot.getX()) * (a.rx - pivot.getX());
				yd += (a.ry - pivot.getY()) * (a.ry - pivot.getY());
			}
		}
		return (float) Math.sqrt((xd + yd) / size());
	}

	/**
	 * get a rectangle fully covering this molecule
	 * 
	 * @param skin
	 *            the thickness of skin of the rectangle
	 * @return the rectangle that contains this molecule, with a skin thickness (which means a rectangle with a
	 *         peripheral zone). Returns a rectangle with zero size, if this molecule is empty, e.g. when it is used as
	 *         a ghost molecule.
	 */
	public Rectangle getBounds(int skin) {
		if (size() == 0)
			return new Rectangle(0, 0, 0, 0);
		double xmin = 10000, ymin = 10000, xmax = 0, ymax = 0;
		double left = 0, right = 0, top = 0, bottom = 0;
		synchronized (atoms) {
			for (Atom a : atoms) {
				left = a.rx - 0.5 * a.sigma;
				right = a.rx + 0.5 * a.sigma;
				top = a.ry - 0.5 * a.sigma;
				bottom = a.ry + 0.5 * a.sigma;
				if (right > xmax)
					xmax = right;
				if (bottom > ymax)
					ymax = bottom;
				if (left < xmin)
					xmin = left;
				if (top < ymin)
					ymin = top;
			}
		}
		int x = (int) xmin - skin;
		int y = (int) ymin - skin;
		int w = (int) (xmax - xmin) + skin + skin;
		int h = (int) (ymax - ymin) + skin + skin;
		return new Rectangle(x, y, w, h);
	}

	/** get the radial bonds of this molecule */
	public List<RadialBond> getBonds() {
		if (model == null)
			return null;
		if (model.bonds == null || model.bonds.isEmpty())
			return null;
		List<RadialBond> list = null;
		RadialBond rBond = null;
		synchronized (model.bonds.getSynchronizationLock()) {
			for (Iterator it = model.bonds.iterator(); it.hasNext();) {
				rBond = (RadialBond) it.next();
				if (contains(rBond.getAtom1()) && contains(rBond.getAtom2())) {
					if (list == null)
						list = new ArrayList<RadialBond>();
					list.add(rBond);
				}
			}
		}
		return list;
	}

	/** get the angular bonds of this molecule */
	public List<AngularBond> getBends() {
		if (model == null)
			return null;
		if (model.bends == null || model.bends.isEmpty())
			return null;
		List<AngularBond> list = null;
		AngularBond aBond = null;
		synchronized (model.bends.getSynchronizationLock()) {
			for (Iterator it = model.bends.iterator(); it.hasNext();) {
				aBond = (AngularBond) it.next();
				if (contains(aBond.getAtom1()) && contains(aBond.getAtom2()) && contains(aBond.getAtom3())) {
					if (list == null)
						list = new ArrayList<AngularBond>();
					list.add(aBond);
				}
			}
		}
		return list;
	}

	public void computeCenter() {
		xCenter = yCenter = 0.0;
		int[] exclusion = torque.getExclusion();
		int m = exclusion != null ? exclusion.length : 0;
		synchronized (atoms) {
			outerloop: for (Atom a : atoms) {
				if (m > 0 && exclusion != null) {
					for (int i : exclusion) {
						if (i == a.index)
							continue outerloop;
					}
				}
				xCenter += a.rx;
				yCenter += a.ry;
			}
		}
		int n = size() - m;
		xCenter /= n;
		yCenter /= n;
	}

	void applyTorque() {
		if (torque == null)
			return;
		if (Math.abs(torque.getForce()) < Particle.ZERO)
			return;
		computeCenter();
		int[] exclusion = torque.getExclusion();
		double k, sinx, cosx;
		outerloop2: for (Atom a : atoms) {
			if (exclusion != null && exclusion.length > 0) {
				for (int i : exclusion) {
					if (i == a.index)
						continue outerloop2;
				}
			}
			k = (xCenter - a.rx) / (a.ry - yCenter);
			cosx = (a.ry > yCenter ? -torque.getForce() : torque.getForce()) / Math.sqrt(1.0 + k * k);
			sinx = k * cosx;
			a.fx += cosx;
			a.fy += sinx;
		}
	}

	/**
	 * This is a skeletal method: you may initialize a molecule in a subclass, but calling this method at this level
	 * causes nothing to happen.
	 */
	public void init(MolecularModel model) {
	}

	/**
	 * This is a skeletal method: you may build radial bonds for a molecule in a subclass, but calling this method at
	 * this level returns null.
	 */
	public List<RadialBond> buildBonds(MolecularModel model) {
		return null;
	}

	/**
	 * This is a skeletal method: you may build angular bonds for a molecule in a subclass, but calling this method at
	 * this level returns null.
	 */
	public List<AngularBond> buildBends(MolecularModel model) {
		return null;
	}

	public void render(Graphics2D g) {

		if (model == null)
			return;

		Color oldColor = g.getColor();
		Stroke oldStroke = g.getStroke();

		if (torque != null && Math.abs(torque.getForce()) > Particle.ZERO) {
			if (xCenter == 0 && yCenter == 0)
				computeCenter();
			int r = (int) (torque.getRadius() * 10);
			g.setColor(DARK_GREEN);
			g.fillOval((int) (xCenter - r), (int) (yCenter - r), r + r, r + r);
			g.setColor(Color.black);
			g.setStroke(ViewAttribute.THIN);
			g.drawOval((int) (xCenter - r), (int) (yCenter - r), r + r, r + r);
			g.drawArc((int) (xCenter - r * 0.5), (int) (yCenter - r * 0.5), r, r, 0, 270);
			if (torque.getForce() < 0) {
				int x1 = (int) xCenter;
				int y1 = (int) (yCenter + r * 0.5);
				g.drawLine(x1, y1, x1 - 5, y1 - 5);
				g.drawLine(x1, y1, x1 - 5, y1 + 2);
			}
			else {
				int x1 = (int) (xCenter + r * 0.5);
				int y1 = (int) yCenter;
				g.drawLine(x1, y1, x1 - 5, y1 - 5);
				g.drawLine(x1, y1, x1 + 2, y1 - 5);
			}
		}

		if (selected && model.view.getShowSelectionHalo()) {

			g.setColor(model.view.contrastBackground());
			g.setStroke(ViewAttribute.THIN_DASHED);
			double dashR, dashD;
			Ellipse2D.Double elli = new Ellipse2D.Double();
			synchronized (atoms) {
				for (Atom a : atoms) {
					dashD = a.sigma + 4;
					dashR = dashD * 0.5;
					elli.setFrame(a.rx - dashR, a.ry - dashR, dashD, dashD);
					g.draw(elli);
				}
			}

			if (model.view.getAction() == UserAction.ROTA_ID) {
				Point com = getCenterOfMass();
				g.fillOval(com.x - 5, com.y - 5, 10, 10);
				g.setStroke(ViewAttribute.THIN);
				g.drawOval(com.x - 40, com.y - 40, 80, 80);
				g.draw(rotateCrossLine[0]);
				g.setStroke(ViewAttribute.MODERATE);
				g.draw(rotateCrossLine[1]);
				g.setStroke(ViewAttribute.THIN);
				g.draw(rotateRect);
			}

		}

		g.setColor(oldColor);
		g.setStroke(oldStroke);

	}

	/**
	 * <p>
	 * duplicate this molecule. If the molecule is an empty one or if no more molecule can be inserted to the model
	 * because of the capacity limit, return null. NOTE: The duplicated molecule is not equal to the original one, i.e.
	 * duplicate.equals(original) returns false. IMPORTANT: if duplication is not successful, the bonds and bends must
	 * be removed from the main bond and bend array; the bonds and bends associated with this molecule can be obtained
	 * by calling <code>getBonds()</code> and <code>getBends()</code>.
	 * </p>
	 * 
	 * <p>
	 * The new atoms are appended to the end of existing atoms. An internal mapping between the relative indices of the
	 * newly added atoms and those of their parents, as illustrated in the following:
	 * 
	 * <pre>
	 *       +--  0
	 *    o  |    1
	 *    l  |    2
	 *    d  |    .
	 *       |    .
	 *    p  |    i------------+
	 *    o  |    j---------+  |
	 *    o  |    k---------|--|--+
	 *    l  |    l---------|--|--|--+
	 *       |    .         |  |  |  |
	 *       +--  oldNOA-1  |  |  |  |
	 *       |    oldNOA  --+  |  |  |  ===mapping relationship
	 *    n  |    oldNOA+1-----+  |  |      stored in an internal HashMap
	 *    e  |    oldNOA+2--------|--+      
	 *    w  |    .               |
	 *       |    .               |
	 *       +--  newNOA-1--------+
	 * </pre>
	 * 
	 * is used in duplicating the bonds associated with the molecule.
	 * </p>
	 * 
	 * @see org.concord.mw2d.models.Molecule#getBonds
	 * @see org.concord.mw2d.models.Molecule#getBends
	 */
	public Molecule duplicate() {

		if (model == null)
			throw new RuntimeException(this + " is not associated with any model!");

		int oldNOA = model.getNumberOfAtoms();
		if (size() == 0 || oldNOA + size() > model.atom.length)
			return null;

		Molecule mol = new Molecule();
		mol.setModel(model);

		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		int k = 0;
		synchronized (atoms) {
			for (Atom at : atoms) {
				map.put(at.getIndex(), k);
				model.atom[oldNOA + k].duplicate(at);
				mol.addAtom(model.atom[oldNOA + k]);
				k++;
			}
		}

		/* copy radial bonds */

		if (model.bonds.isEmpty())
			throw new RuntimeException("empty bond error!");
		int iAtom;
		RadialBond rBond;
		int relativeOrigin, relativeDestin;
		List<RadialBond> newBonds = new ArrayList<RadialBond>();
		synchronized (model.bonds.getSynchronizationLock()) {
			for (Iterator i = model.bonds.iterator(); i.hasNext();) {
				rBond = (RadialBond) i.next();
				for (Iterator<Integer> j = map.keySet().iterator(); j.hasNext();) {
					iAtom = j.next().intValue();
					if (rBond.contains(model.atom[iAtom])) {
						relativeOrigin = map.get(rBond.getAtom1().getIndex());
						relativeDestin = map.get(rBond.getAtom2().getIndex());
						newBonds.add(rBond.getCopy(model.atom[oldNOA + relativeOrigin], model.atom[oldNOA
								+ relativeDestin]));
						break;
					}
				}
			}
		}

		if (!newBonds.isEmpty()) {
			for (RadialBond rb : newBonds) {
				model.bonds.add(rb);
			}
		}

		/* clone angular bonds */

		if (!model.bends.isEmpty()) {

			List<AngularBond> newBends = new ArrayList<AngularBond>();
			int relativeMiddle;
			AngularBond aBond;
			synchronized (model.bends.getSynchronizationLock()) {
				for (Iterator i = model.bends.iterator(); i.hasNext();) {
					aBond = (AngularBond) i.next();
					synchronized (map) {
						for (Iterator<Integer> j = map.keySet().iterator(); j.hasNext();) {
							iAtom = j.next().intValue();
							if (aBond.contains(model.atom[iAtom])) {
								relativeOrigin = map.get(aBond.getAtom1().getIndex());
								relativeDestin = map.get(aBond.getAtom2().getIndex());
								relativeMiddle = map.get(aBond.getAtom3().getIndex());
								newBends.add(aBond.getCopy(model.atom[oldNOA + relativeOrigin], model.atom[oldNOA
										+ relativeDestin], model.atom[oldNOA + relativeMiddle]));
								break;
							}
						}
					}
				}
			}

			if (!newBends.isEmpty()) {
				for (AngularBond ab : newBends) {
					model.bends.add(ab);
				}
			}

		}

		return mol;

	}
}