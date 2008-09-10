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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.SwingUtilities;

import org.concord.modeler.math.SimpleMath;
import org.concord.mw2d.ViewAttribute;
import org.concord.mw2d.ui.GayBerneConfigure;

public class GayBerneParticle extends UnitedAtom implements Rotatable {

	private final static short MIN_LENGTH = 15;
	private final static short MAX_LENGTH = 240;
	private final static short MIN_BREADTH = 10;
	private final static short MAX_BREADTH = 60;

	private transient MesoModel model;
	private boolean stateStored;
	private double savedRx, savedRy, savedVx, savedVy;
	private double savedTheta, savedOmega, savedBreadth, savedLength;

	/* the length of this GB particle */
	double length = 20.0;

	/* the breadth of this GB particle */
	double breadth = 10.0;

	/*
	 * the factor that describes the distribution of mass in the elliptical area. The default value 1.0 corresponds to a
	 * uniform distribution.
	 */
	double mdFactor = 1.0;

	/* end-to-end well depth vs. side-by-side well depth */
	double eeVsEs = 1.0;

	/* potential well depth */
	double epsilon0 = 0.1;

	/* the mass of this particle */
	double mass = 2.0f;

	private Ellipse2D.Double ellipse;
	private Rectangle2D.Double bound;
	private Polygon polygon;
	private AffineTransform transform;

	/* the handles to rotate this particle */
	private static Ellipse2D.Double[] rotc = { new Ellipse2D.Double(), new Ellipse2D.Double(), new Ellipse2D.Double(),
			new Ellipse2D.Double() };
	private static Rectangle[] rect = { new Rectangle(), new Rectangle() };

	private Line2D.Double dipLine[] = new Line2D.Double[6];

	public GayBerneParticle() {
		super();
		color = Color.blue;
		ellipse = new Ellipse2D.Double();
	}

	public GayBerneParticle(GayBerneParticle gbp) {
		this();
		set(gbp);
	}

	boolean outOfView() {
		return false;
	}

	public int getResizeHandle(int x, int y) {
		if (rect[0].contains(x, y))
			return 0;
		if (rect[1].contains(x, y))
			return 1;
		return -1;
	}

	public int getRotationHandle(int x, int y) {
		for (int i = 0; i < rotc.length; i++) {
			if (rotc[i].contains(x, y))
				return i;
		}
		return -1;
	}

	public void setModel(MDModel m) {
		if (m == null) {
			model = null;
			return;
		}
		if (!(m instanceof MesoModel))
			throw new IllegalArgumentException("wrong type of model");
		model = (MesoModel) m;
		measuringTool.setModel(model);
	}

	public MDModel getHostModel() {
		return model;
	}

	public void destroy() {
		super.destroy();
		model = null;
	}

	public void storeCurrentState() {
		savedRx = rx;
		savedRy = ry;
		savedVx = vx;
		savedVy = vy;
		savedTheta = theta;
		savedOmega = omega;
		savedBreadth = breadth;
		savedLength = length;
		stateStored = true;
	}

	public void restoreState() {
		if (!stateStored)
			return;
		rx = savedRx;
		ry = savedRy;
		vx = savedVx;
		vy = savedVy;
		theta = savedTheta;
		omega = savedOmega;
		breadth = savedBreadth;
		length = savedLength;
		locateRects();
		locateRotationHandles();
	}

	public Rectangle2D getBounds2D() {
		if (bound == null)
			bound = new Rectangle2D.Double();
		bound.setRect(rx - 0.5 * breadth, ry - 0.5 * length, breadth, length);
		return bound;
	}

	public boolean contains(double x, double y) {
		return getShape().contains(x, y);
	}

	public boolean contains(Point2D p) {
		return getShape().contains(p);
	}

	public boolean contains(double x, double y, double w, double h) {
		return getShape().contains(x, y, w, h);
	}

	public boolean contains(Rectangle2D r) {
		return getShape().contains(r);
	}

	public boolean intersects(double x, double y, double w, double h) {
		if (theta == 0.0 || theta == Math.PI || theta == 2 * Math.PI)
			return getShape().intersects(x, y, w, h);
		double x2 = x + w;
		double y2 = y + h;
		convertToPolygon();
		if (intersects(polygon, x, y, x2, y))
			return true;
		if (intersects(polygon, x, y2, x2, y2))
			return true;
		if (intersects(polygon, x, y, x, y2))
			return true;
		if (intersects(polygon, x2, y, x2, y2))
			return true;
		return false;
	}

	public double getMinX() {
		return rx - 0.5 * length;
	}

	public double getMinY() {
		return ry - 0.5 * breadth;
	}

	public double getMaxX() {
		return rx + 0.5 * length;
	}

	public double getMaxY() {
		return ry + 0.5 * breadth;
	}

	public int compareTo(Object o) {
		if (!(o instanceof GayBerneParticle))
			throw new IllegalArgumentException("Cannot compare with an object that is not an GB");
		if (getIndex() < ((GayBerneParticle) o).getIndex())
			return -1;
		if (getIndex() > ((GayBerneParticle) o).getIndex())
			return 1;
		return 0;
	}

	public String toString() {
		return "Particle #" + getIndex() + " [ length=" + format.format(0.1 * length) + "nm, breadth="
				+ format.format(0.1 * breadth) + "nm ]";
	}

	public void set(Particle target) {
		duplicate(target);
		copyRestraint(target.restraint);
	}

	public void duplicate(Particle target) {
		if (!(target instanceof GayBerneParticle))
			throw new IllegalArgumentException("target not GB");
		super.duplicate(target);
		GayBerneParticle gb = (GayBerneParticle) target;
		mass = gb.mass;
		color = target.color;
		epsilon0 = gb.epsilon0;
		eeVsEs = gb.eeVsEs;
		breadth = gb.breadth;
		length = gb.length;
		ellipse.x = gb.ellipse.x;
		ellipse.y = gb.ellipse.y;
		ellipse.width = gb.ellipse.width;
		ellipse.height = gb.ellipse.height;
	}

	/* advance this particle using 2nd order Taylor expansion */
	void predict(double dt, double dt2) {
		dx = vx * dt + ax * dt2;
		dy = vy * dt + ay * dt2;
		delta = omega * dt + alpha * dt2;
		rx += dx;
		ry += dy;
		theta += delta;
		vx += ax * dt;
		vy += ay * dt;
		omega += alpha * dt;
	}

	/*
	 * routine to correct the position predicted by the <tt>advance</tt> method. <b>Important</b>: <tt>fx, fy, tau</tt>
	 * were used in the force calculation routine to store the new acceleration data. <tt>ax, ay, alpha</tt> were used
	 * to hold the old acceleration data before calling this method. After calling this method, new acceleration data
	 * will be assigned to <tt>ax, ay, alpha</tt>, whereas the forces and torques to <tt>fx, fy, tau</tt>. <b>Be
	 * aware</b>: the acceleration and force properties of a particle are correct ONLY after this correction method has
	 * been called.
	 * 
	 * @param dt <tt>dt2</tt> half of the time increment
	 */
	void correct(double dt2) {
		vx += dt2 * (fx - ax);
		vy += dt2 * (fy - ay);
		omega += dt2 * (tau - alpha);
		ax = fx;
		ay = fy;
		alpha = tau;
		fx *= mass;
		fy *= mass;
		tau *= inertia;
	}

	public void setSelected(boolean b) {
		super.setSelected(b);
		if (b) {
			locateRects();
			locateRotationHandles();
			model.view.setSelectedComponent(this);
		}
	}

	public void setSelectedToRotate(boolean b) {
		super.setSelectedToRotate(b);
		if (b) {
			locateRotationHandles();
			setSelectedToResize(false);
		}
	}

	public void setSelectedToResize(boolean b) {
		super.setSelectedToResize(b);
		if (b) {
			locateRects();
			setSelectedToRotate(false);
		}
	}

	public double getLength() {
		return length;
	}

	public void setLength(double d) {
		length = d;
		ellipse.width = length;
		if (selected) {
			locateRects();
			locateRotationHandles();
		}
	}

	public double getBreadth() {
		return breadth;
	}

	public void setBreadth(double d) {
		breadth = d;
		ellipse.height = breadth;
		if (selected) {
			locateRects();
			locateRotationHandles();
		}
	}

	public double getMass() {
		return mass;
	}

	public void setMass(double d) {
		mass = d;
	}

	public void setRx(double d) {
		super.setRx(d);
		ellipse.x = d - 0.5 * length;
	}

	public void setRy(double d) {
		super.setRy(d);
		ellipse.y = d - 0.5 * breadth;
	}

	public double getEeVsEs() {
		return eeVsEs;
	}

	public void setEeVsEs(double d) {
		eeVsEs = d;
	}

	public double getEpsilon0() {
		return epsilon0;
	}

	public void setEpsilon0(double d) {
		epsilon0 = d;
	}

	public double getMdFactor() {
		return mdFactor;
	}

	public void setMdFactor(double d) {
		mdFactor = d;
	}

	/**
	 * get the minimum square that fully contains this GB and has a peripheral layer with the given thickness.
	 * 
	 * @param skin
	 *            the thickness of the skin
	 * @return the bounding square of the GB molecule with the specified skin
	 */
	public Rectangle getBounds(int skin) {
		double z = Math.max(breadth, length);
		int x0 = (int) (rx - 0.5 * z) - skin;
		int y0 = (int) (ry - 0.5 * z) - skin;
		int d = (int) z + skin + skin;
		return SwingUtilities.computeIntersection(0, 0, model.view.getWidth(), model.view.getHeight(), new Rectangle(
				x0, y0, d, d));
	}

	/** @return the ellipse of this GB particle when theta=0 */
	public Ellipse2D getShape() {
		ellipse.setFrame(rx - 0.5 * breadth, ry - 0.5 * length, breadth, length);
		return ellipse;
	}

	private void locateRotationHandles() {
		double cosTheta = Math.cos(theta);
		double sinTheta = Math.sin(theta);
		/* southeast circle */
		double xold = 0.5 * length;
		double yold = 0.5 * breadth;
		double xpos = rx + xold * cosTheta - yold * sinTheta;
		double ypos = ry + xold * sinTheta + yold * cosTheta;
		rotc[0].setFrame(xpos - 3, ypos - 3, 6, 6);
		/* southwest circle */
		xold = -0.5 * length;
		yold = 0.5 * breadth;
		xpos = rx + xold * cosTheta - yold * sinTheta;
		ypos = ry + xold * sinTheta + yold * cosTheta;
		rotc[1].setFrame(xpos - 3, ypos - 3, 6, 6);
		/* northwest circle */
		xold = -0.5 * length;
		yold = -0.5 * breadth;
		xpos = rx + xold * cosTheta - yold * sinTheta;
		ypos = ry + xold * sinTheta + yold * cosTheta;
		rotc[2].setFrame(xpos - 3, ypos - 3, 6, 6);
		/* northeast circle */
		xold = 0.5 * length;
		yold = -0.5 * breadth;
		xpos = rx + xold * cosTheta - yold * sinTheta;
		ypos = ry + xold * sinTheta + yold * cosTheta;
		rotc[3].setFrame(xpos - 3, ypos - 3, 6, 6);
	}

	private void locateRects() {
		rect[0].setRect(rx - 0.5 * length - 2, ry - 2, 4, 4);
		rect[1].setRect(rx - 2, ry - 0.5 * breadth - 2, 4, 4);
	}

	/** Inertia of a GB particle is 0.5*mass*breadth*length*mdFactor. */
	public double getInertia() {
		inertia = 0.5 * mass * breadth * length * mdFactor;
		return inertia;
	}

	public boolean contains(GayBerneParticle gb) {
		if (gb == null)
			throw new IllegalArgumentException("null particle");
		if (gb == this)
			return true;
		return getBounds2D().contains(gb.getBounds2D());
	}

	/**
	 * Whether or not this GB particle intersects with the specified GB particle. This uses the paths generated by
	 * calling the <code> getPathIterator(AffineTransform at)</code> of the <code>Ellipse2D
	 *  </code> class to determine
	 * intersections.
	 * 
	 * @return true if the two particles intersect
	 */
	public boolean intersects(GayBerneParticle gb) {
		if (gb == null)
			throw new IllegalArgumentException("null particle");
		if (gb == this)
			return true;
		if (contains(gb) || gb.contains(this))
			return true;
		convertToPolygon();
		gb.convertToPolygon();
		return intersects(polygon, gb.polygon);
	}

	public boolean intersects(Rectangle2D r2d) {
		if (r2d == null)
			return false;
		double x1 = r2d.getMinX();
		double x2 = r2d.getMaxX();
		double y1 = r2d.getMinY();
		double y2 = r2d.getMaxY();
		convertToPolygon();
		if (intersects(polygon, x1, y1, x2, y1))
			return true;
		if (intersects(polygon, x1, y2, x2, y2))
			return true;
		if (intersects(polygon, x1, y1, x1, y2))
			return true;
		if (intersects(polygon, x2, y1, x2, y2))
			return true;
		return false;
	}

	/**
	 * rotate to a given direction specified by the position.
	 * 
	 * @param x
	 *            the x coordinate of the hot spot
	 * @param y
	 *            the y coordinate of the hot spot
	 * @param handle
	 *            the index of one of the four handles
	 */
	public void rotateTo(int x, int y, int handle) {
		double distance = Math.hypot(rx - x, ry - y);
		double theta = (x - rx) / distance;
		theta = y > ry ? Math.acos(theta) : 2.0 * Math.PI - Math.acos(theta);
		double theta0;
		double xold = 1.0, yold = 0.0;
		switch (handle) {
		case 0:
			xold = 0.5 * length;
			yold = 0.5 * breadth;
			break;
		case 1:
			xold = -0.5 * length;
			yold = 0.5 * breadth;
			break;
		case 2:
			xold = -0.5 * length;
			yold = -0.5 * breadth;
			break;
		case 3:
			xold = 0.5 * length;
			yold = -0.5 * breadth;
			break;
		}
		distance = Math.hypot(xold, yold);
		theta0 = xold / distance;
		theta0 = yold > 0.0 ? Math.acos(theta0) : 2.0 * Math.PI - Math.acos(theta0);
		setTheta(theta - theta0);
		locateRotationHandles();
		model.view.repaint();
	}

	/**
	 * resize to a given dimension. This method checks that the breadth of the GB molecule cannot be longer than 60 and
	 * shorter than 10, whereas the length cannot be longer than 120 and shorter than 15.
	 * 
	 * @param x
	 *            the x coordinate of the position
	 * @param y
	 *            the y coordinate of the position
	 * @param direction
	 *            in which direction this molecule is resized
	 */
	public synchronized void resizeTo(int x, int y, int direction) {
		if (direction == 1) {
			setBreadth(2.0 * Math.abs(ry - y));
		}
		else if (direction == 0) {
			setLength(2.0 * Math.abs(rx - x));
		}
		if (breadth < MIN_BREADTH)
			setBreadth(MIN_BREADTH);
		else if (breadth > MAX_BREADTH)
			setBreadth(MAX_BREADTH);
		if (length < MIN_LENGTH)
			setLength(MIN_LENGTH);
		else if (length > MAX_LENGTH)
			setLength(MAX_LENGTH);
		if (breadth > length) {
			double temp = length;
			setLength(breadth);
			setBreadth(temp);
		}
		if (length - breadth < GayBerneConfigure.GB_LIMIT) {
			setLength(breadth + GayBerneConfigure.GB_LIMIT);
		}
		locateRects();
		model.view.repaint();
	}

	public void render(Graphics2D g, AffineTransform at, AffineTransform savedAT) {

		if (isVisible()) {

			if (ellipse == null)
				ellipse = new Ellipse2D.Double();
			ellipse.setFrame(rx - 0.5 * length, ry - 0.5 * breadth, length, breadth);
			at.setToRotation(theta, rx, ry);
			g.transform(at);
			g.setColor(marked ? model.view.getMarkColor() : color);
			g.fill(ellipse);
			g.setColor(model.view.contrastBackground());
			g.setStroke(ViewAttribute.THIN);
			g.draw(ellipse);

			if (model.view.getDrawDipole() && Math.abs(dipoleMoment) > ZERO) {
				g.setStroke(ViewAttribute.MODERATE);
				g.setColor(dipoleColor == null ? getContrastColor(color) : dipoleColor);
				setDipLines();
				for (int i = (hideArrow ? 3 : 0); i < (hideArrow ? dipLine.length : 3); i++)
					g.draw(dipLine[i]);
			}

			if (isBlinking()) {
				g.setColor(blinkColor);
				g.setStroke(ViewAttribute.DASHED);
				g.drawOval((int) (rx - 0.6 * length), (int) (ry - 0.6 * breadth), (int) (1.2 * length),
						(int) (1.2 * breadth));
			}

			if (selected && model.view.getShowSelectionHalo()) {
				if (!selectedToRotate && !selectedToResize) {
					ellipse.setFrame(rx - 0.5 * length - 2, ry - 0.5 * breadth - 2, length + 4, breadth + 4);
					g.setColor(model.view.contrastBackground());
					g.setStroke(ViewAttribute.THIN_DASHED);
					g.draw(ellipse);
				}
			}

			g.setTransform(savedAT);

			if (restraint != null)
				PointRestraint.render(g, this);

			if (model.view.getDrawCharge()) {
				g.setColor(chargeColor);
				g.setStroke(ViewAttribute.MODERATE);
				if (charge > ZERO) {
					g.drawLine((int) (rx - 6.0), (int) ry, (int) (rx + 6.0), (int) ry);
					g.drawLine((int) rx, (int) (ry - 6.0), (int) rx, (int) (ry + 6.0));
				}
				else if (charge < -ZERO) {
					g.drawLine((int) (rx - 6.0), (int) ry, (int) (rx + 6.0), (int) ry);
				}
			}

			if (model.view.getShowParticleIndex()) {
				g.setFont(SANSSERIF);
				FontMetrics fm = g.getFontMetrics();
				int sw = fm.stringWidth("" + getIndex());
				g.drawString("" + getIndex(), (int) (rx - 0.4 * sw), (int) (ry + 0.4 * fm.getHeight()));
			}

			if (userField != null)
				userField.render(g, this, model.getMovie().getCurrentFrameIndex() >= model.getTapePointer() - 1);

		}

		if (selected && model.view.getShowSelectionHalo()) {
			if (selectedToRotate) {
				g.setStroke(ViewAttribute.THIN);
				g.setColor(Color.green);
				for (Ellipse2D i : rotc)
					g.fill(i);
				g.setColor(Color.black);
				for (Ellipse2D i : rotc)
					g.draw(i);
			}
			else if (selectedToResize) {
				g.setColor(Color.lightGray);
				g.setStroke(ViewAttribute.THIN_DASHED);
				g.draw(ellipse);
				g.drawLine(0, (int) ry, model.view.getWidth(), (int) ry);
				g.drawLine((int) rx, 0, (int) rx, model.view.getHeight());
				g.setStroke(ViewAttribute.THIN);
				g.setColor(Color.yellow);
				for (Rectangle i : rect)
					g.fill(i);
				g.setColor(Color.black);
				for (Rectangle i : rect)
					g.draw(i);
			}
		}

		if (showRTraj)
			renderRTraj(g);

	}

	static Line2D[] convertToLines(Polygon p) {
		if (p == null || p.npoints == 0)
			return null;
		int n = p.npoints;
		Line2D[] lines = new Line2D.Float[n];
		for (int i = 0; i < n - 1; i++) {
			lines[i] = new Line2D.Float(p.xpoints[i], p.ypoints[i], p.xpoints[i + 1], p.ypoints[i + 1]);
		}
		lines[n - 1] = new Line2D.Float(p.xpoints[n - 1], p.ypoints[n - 1], p.xpoints[0], p.ypoints[0]);
		return lines;
	}

	static boolean intersects(Polygon p1, Polygon p2) {
		if (p1 == null || p1.npoints == 0)
			return false;
		if (p2 == null || p2.npoints == 0)
			return false;
		Line2D[] line1 = convertToLines(p1);
		Line2D[] line2 = convertToLines(p2);
		for (Line2D i : line1) {
			for (Line2D j : line2) {
				if (i.intersectsLine(j))
					return true;
			}
		}
		return false;
	}

	/* test if the polygon p intersects with a line specified by its start and end points. */
	static boolean intersects(Polygon p, double x1, double y1, double x2, double y2) {
		if (p == null || p.npoints == 0)
			return false;
		Line2D[] line = convertToLines(p);
		for (Line2D i : line) {
			if (i.intersectsLine(x1, y1, x2, y2))
				return true;
		}
		return false;
	}

	private void convertToPolygon() {

		if (polygon == null) {
			polygon = new Polygon();
		}
		else {
			polygon.reset();
		}
		if (transform == null) {
			transform = new AffineTransform();
		}
		else {
			transform.setToIdentity();
		}
		transform.setToRotation(theta, rx, ry);

		PathIterator pi = ellipse.getPathIterator(transform);
		double[] xy = new double[6];
		while (!pi.isDone()) {
			int type = pi.currentSegment(xy);
			if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO || type == PathIterator.SEG_CUBICTO
					|| type == PathIterator.SEG_QUADTO) {
				polygon.addPoint((int) xy[0], (int) xy[1]);
			}
			pi.next();
		}

	}

	private void setDipLines() {
		if (Math.abs(dipoleMoment) < ZERO)
			return;
		double half = dipoleMoment < 0.0 ? (hideArrow ? 0.2 : 0.35) : (hideArrow ? -0.2 : -0.35);
		if (dipLine[0] == null) {
			dipLine[0] = new Line2D.Double(rx - half * length, ry, rx + half * length, ry);
		}
		else {
			dipLine[0].setLine(rx - half * length, ry, rx + half * length, ry);
		}
		if (dipLine[1] == null) {
			dipLine[1] = new Line2D.Double(rx + half * length, ry, rx + half * length - half * 10, ry + 5);
		}
		else {
			dipLine[1].setLine(rx + half * length, ry, rx + half * length - half * 10, ry + 5);
		}
		if (dipLine[2] == null) {
			dipLine[2] = new Line2D.Double(rx + half * length, ry, rx + half * length - half * 10, ry - 5);
		}
		else {
			dipLine[2].setLine(rx + half * length, ry, rx + half * length - half * 10, ry - 5);
		}
		if (dipLine[3] == null) {
			dipLine[3] = new Line2D.Double(rx + half * length + 3 * SimpleMath.sign(half), ry, rx + half * length + 9
					* SimpleMath.sign(half), ry);
		}
		else {
			dipLine[3].setLine(rx + half * length + 3 * SimpleMath.sign(half), ry, rx + half * length + 9
					* SimpleMath.sign(half), ry);
		}
		if (dipLine[4] == null) {
			dipLine[4] = new Line2D.Double(rx + half * length + 6 * SimpleMath.sign(half), ry + 3, rx + half * length
					+ 6 * SimpleMath.sign(half), ry - 3);
		}
		else {
			dipLine[4].setLine(rx + half * length + 6 * SimpleMath.sign(half), ry + 3, rx + half * length + 6
					* SimpleMath.sign(half), ry - 3);
		}
		if (dipLine[5] == null) {
			dipLine[5] = new Line2D.Double(rx - half * length - 3 * SimpleMath.sign(half), ry, rx - half * length - 9
					* SimpleMath.sign(half), ry);
		}
		else {
			dipLine[5].setLine(rx - half * length - 3 * SimpleMath.sign(half), ry, rx - half * length - 9
					* SimpleMath.sign(half), ry);
		}
	}

}