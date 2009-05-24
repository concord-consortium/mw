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
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.concord.modeler.util.FloatQueue;
import org.concord.mw2d.ViewAttribute;

/**
 * This is the abstract class from which you should derive a 2D uniaxial united-atom particle, such as the
 * <tt>GayBerneParticle</tt>. This class reserves the space to develop more complex united-atom models than the
 * elliptical Gay-Berne particle.
 * 
 * @author Qian Xie
 */

public abstract class UnitedAtom extends Particle {

	/* show angular velocity rectangles or not */
	private static boolean showORects;

	static boolean hideArrow;

	/** for CC's internal use */
	public static void setHideArrow(boolean b) {
		hideArrow = b;
	}

	/** for CC's internal use */
	public static boolean getHideArrow() {
		return hideArrow;
	}

	/*
	 * the small rectangle at the end of the angular velocity vector with which the user can drag to change the angular
	 * velocity of this particle. But dragging this hotspot cannot determine the direction of an angular velocity. It
	 * can determine ONLY the magnitude, AND the magnitude ranges from zero to a certain bound, which corresponds to 360
	 * degrees of a full arc. To give a direction, you have to interact with the <tt>flipHotSpot</tt>.
	 * 
	 * @see org.concord.mw2d.models.UnitedAtom#flipHotSpot
	 */
	static Rectangle oHotSpot = new Rectangle();

	/*
	 * hot spot for flipping an angular velocity. This hotspot is set for the user to switch the direction of an angular
	 * velocity vector to opposite direction. Together with <tt>oHotSpot</tt>, it can be used to arbitrarily change a
	 * selected angular velocity.
	 * 
	 * @see org.concord.mw2d.models.UnitedAtom#oHotSpot
	 */
	static Ellipse2D.Double flipHotSpot = new Ellipse2D.Double(0, 0, 8, 8);

	/* the color for dipole moment */
	Color dipoleColor = Color.black;

	/* the angle of the axis of this uniaxial particle in radians */
	volatile double theta;

	/* the angular velocity of the axis of this uniaxial particle */
	volatile double omega;

	/* the angular acceleration of the axis of this uniaxial particle */
	transient double alpha;

	/* the torque exerted on this uniaxial particle */
	transient double tau;

	/* the custom torque exerted on this uniaxial particle */
	float gamma;

	/* the angular displacement from previous angle */
	double delta;

	/* the moment of inertia of this uniaxial particle, default value 100.0 */
	double inertia = 100.0;

	/* the dipole moment of this united atom */
	double dipoleMoment;

	/* queue to store <tt>theta</tt> track */
	transient FloatQueue thetaQ;

	/* queue to store <tt>omega</tt> track */
	transient FloatQueue omegaQ;

	/* queue to store <tt>alpha</tt> track */
	transient FloatQueue alphaQ;

	transient boolean selectedToRotate;

	transient boolean selectedToResize;

	/* the arc used to represent angular variables */
	Arc2D.Double arc;

	/* the radius of arc used to represent angular variables */
	static int arcSize = 15;

	private transient boolean oselected;

	/* Make the transient properties BML-transient: */
	static {
		try {
			BeanInfo info = Introspector.getBeanInfo(UnitedAtom.class);
			PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
			for (int i = 0; i < propertyDescriptors.length; ++i) {
				PropertyDescriptor pd = propertyDescriptors[i];
				String name = pd.getName();
				if (name.equals("tau") || name.equals("alpha") || name.equals("selectedToRotate")
						|| name.equals("selectedToResize") || name.equals("OmegaSelection") || name.equals("OHotSpot")) {
					pd.setValue("transient", Boolean.TRUE);
				}
			}
		}
		catch (IntrospectionException e) {
		}
	}

	public void destroy() {
		super.destroy();
		thetaQ = null;
		omegaQ = null;
		alphaQ = null;
	}

	public Object clone() {
		Particle clone = null;
		try {
			clone = (UnitedAtom) super.clone();
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
		clone.copyRestraint(null);
		return clone;
	}

	public void erase() {
		super.erase();
		omega = 0;
		alpha = 0;
		tau = 0;
		dipoleMoment = 0.0;
		gamma = 0;
	}

	public void eraseProperties() {
		super.eraseProperties();
		omega = 0;
		alpha = 0;
		tau = 0;
		dipoleMoment = 0.0;
	}

	public void duplicate(Particle p, boolean copyLayers) {
		if (!(p instanceof UnitedAtom))
			throw new IllegalArgumentException("target not a UnitedAtom");
		super.duplicate(p, copyLayers);
		UnitedAtom ua = (UnitedAtom) p;
		theta = ua.theta;
		omega = ua.omega;
		gamma = ua.gamma;
		dipoleMoment = ua.dipoleMoment;
		inertia = ua.inertia;
		dipoleColor = ua.dipoleColor;
	}

	public void setMovable(boolean b) {
		super.setMovable(b);
		if (!b) {
			omega = 0;
		}
	}

	public double getDipoleMoment() {
		return dipoleMoment;
	}

	public void setDipoleMoment(double d) {
		dipoleMoment = d;
	}

	/** returns true if the specified amount of dipole moment is successfully added */
	public boolean addDipoleMoment(double d) {
		if (dipoleMoment <= 100.0 && dipoleMoment >= -100.0) {
			dipoleMoment += d;
			if (dipoleMoment > 100.0)
				dipoleMoment = 100.0;
			else if (dipoleMoment < -100.0)
				dipoleMoment = -100.0;
			return true;
		}
		return false;
	}

	public Color getDipoleColor() {
		return dipoleColor;
	}

	public void setDipoleColor(Color c) {
		dipoleColor = c;
	}

	public double getTheta() {
		return theta;
	}

	public void setTheta(double d) {
		theta = d;
	}

	public double getOmega() {
		return omega;
	}

	public void setOmega(double d) {
		omega = d;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double d) {
		alpha = d;
	}

	public double getTau() {
		return tau;
	}

	public void setTau(double d) {
		tau = d;
	}

	public float getGamma() {
		return gamma;
	}

	public void setGamma(float gamma) {
		this.gamma = gamma;
	}

	/** given a speed scalar, assign an angular velocity in a random direction */
	public void setRandomAngularVelocity(double speed) {
		if (Math.random() < 0.5)
			omega = speed;
		else omega = -speed;
	}

	public double getInertia() {
		return inertia;
	}

	public void setInertia(double d) {
		inertia = d;
	}

	public boolean getOmegaSelection() {
		return oselected;
	}

	public void setOmegaSelection(boolean b) {
		oselected = b;
		if (b) {
			showORects = true;
			setOHotSpot();
		}
	}

	public boolean isSelectedToRotate() {
		return selectedToRotate;
	}

	public void setSelectedToRotate(boolean b) {
		selectedToRotate = b;
	}

	public boolean isSelectedToResize() {
		return selectedToResize;
	}

	public void setSelectedToResize(boolean b) {
		selectedToResize = b;
	}

	/** @see org.concord.mw2d.models.UnitedAtom#oHotSpot */
	public Rectangle getOHotSpot() {
		return oHotSpot;
	}

	public void setOHotSpot(Rectangle hs) {
		oHotSpot = hs;
	}

	public Ellipse2D.Double getFHotSpot() {
		return flipHotSpot;
	}

	public void setFHotSpot(Ellipse2D.Double hs) {
		flipHotSpot = hs;
	}

	public int getArcSize() {
		return arcSize;
	}

	public void initializeMovieQ(int n) {
		super.initializeMovieQ(n);
		initializeThetaQ(n);
		initializeOmegaQ(n);
		initializeAlphaQ(n);
	}

	/** initialize angle queue. If the passed integer is less than 1, nullify it. */
	public void initializeThetaQ(int n) {
		if (thetaQ == null) {
			if (n < 1)
				return;
			thetaQ = new FloatQueue("Theta: " + toString(), n);
			thetaQ.setInterval(getMovieInterval());
			thetaQ.setPointer(0);
			thetaQ.setCoordinateQueue(getHostModel().getModelTimeQueue());
			getHostModel().getMovieQueueGroup().add(thetaQ);
		}
		else {
			thetaQ.setLength(n);
			if (n < 1) {
				getHostModel().getMovieQueueGroup().remove(thetaQ);
				thetaQ = null;
			}
			else {
				thetaQ.setPointer(0);
			}
		}
	}

	/** initialize angular velocity queue. If the passed integer is less than 1, nullify it. */
	public void initializeOmegaQ(int n) {
		if (omegaQ == null) {
			if (n < 1)
				return;
			omegaQ = new FloatQueue("Omega: " + toString(), n);
			omegaQ.setInterval(getMovieInterval());
			omegaQ.setPointer(0);
			omegaQ.setCoordinateQueue(getHostModel().getModelTimeQueue());
			getHostModel().getMovieQueueGroup().add(omegaQ);
		}
		else {
			omegaQ.setLength(n);
			if (n < 1) {
				getHostModel().getMovieQueueGroup().remove(omegaQ);
				omegaQ = null;
			}
			else {
				omegaQ.setPointer(0);
			}
		}
	}

	/** initialize angular acceleration queue. If the passed integer is less than 1, nullify it. */
	public void initializeAlphaQ(int n) {
		if (alphaQ == null) {
			if (n < 1)
				return;
			alphaQ = new FloatQueue("Alpha: " + toString(), n);
			alphaQ.setInterval(getMovieInterval());
			alphaQ.setPointer(0);
			alphaQ.setCoordinateQueue(getHostModel().getModelTimeQueue());
			getHostModel().getMovieQueueGroup().add(alphaQ);
		}
		else {
			alphaQ.setLength(n);
			if (n < 1) {
				getHostModel().getMovieQueueGroup().remove(alphaQ);
				alphaQ = null;
			}
			else {
				alphaQ.setPointer(0);
			}
		}
	}

	/**
	 * When an array is initialized and its elements subsequently filled, it occurs that until the array is full, some
	 * of the elements are empty, the pointer points to the begin index of unfilled segment. The pointer will stop at
	 * the last index of the array once the whole array is filled up.
	 */
	public synchronized int getThetaPointer() {
		if (thetaQ == null || thetaQ.isEmpty())
			return -1;
		return thetaQ.getPointer();
	}

	/** @see org.concord.mw2d.models.UnitedAtom#getThetaPointer */
	public synchronized void moveThetaPointer(int i) {
		if (thetaQ == null || thetaQ.isEmpty())
			return;
		thetaQ.setPointer(i);
	}

	/**
	 * When an array is initialized and its elements subsequently filled, it occurs that until the array is full, some
	 * of the elements are empty, the pointer points to the begin index of unfilled segment. The pointer will stop at
	 * the last index of the array once the whole array is filled up.
	 */
	public synchronized int getOmegaPointer() {
		if (omegaQ == null || omegaQ.isEmpty())
			return -1;
		return omegaQ.getPointer();
	}

	/** @see org.concord.mw2d.models.UnitedAtom#getOmegaPointer */
	public synchronized void moveOmegaPointer(int i) {
		if (omegaQ == null || omegaQ.isEmpty())
			return;
		omegaQ.setPointer(i);
	}

	/**
	 * When an array is initialized and its elements subsequently filled, it occurs that until the array is full, some
	 * of the elements are empty, the pointer points to the begin index of unfilled segment. The pointer will stop at
	 * the last index of the array once the whole array is filled up.
	 */
	public synchronized int getAlphaPointer() {
		if (alphaQ == null || alphaQ.isEmpty())
			return -1;
		return alphaQ.getPointer();
	}

	/** @see org.concord.mw2d.models.UnitedAtom#getAlphaPointer */
	public synchronized void moveAlphaPointer(int i) {
		if (alphaQ == null || alphaQ.isEmpty())
			return;
		alphaQ.setPointer(i);
	}

	/** push current angle into the theta queue */
	public synchronized void updateThetaQ() {
		if (thetaQ == null || thetaQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		thetaQ.update((float) theta);
	}

	/** push current angular velocity into the omega queue */
	public synchronized void updateOmegaQ() {
		if (omegaQ == null || omegaQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		omegaQ.update((float) omega);
	}

	/** push current angular acceleration into the alpha queue */
	public synchronized void updateAlphaQ() {
		if (alphaQ == null || alphaQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		alphaQ.update((float) alpha);
	}

	public FloatQueue getThetaQ() {
		return thetaQ;
	}

	public FloatQueue getOmegaQ() {
		return omegaQ;
	}

	public FloatQueue getAlphaQ() {
		return alphaQ;
	}

	/** If this particle's angular velocity vector is selected for action, render it to the graphics g. */
	public void drawSelectedOmegaVector(Graphics2D g, Color background, boolean adjusting) {

		if (!oselected || !showORects)
			return;

		if (adjusting) {

			g.setStroke(ViewAttribute.THIN);
			g.setColor(getContrastColor(background));
			double ex = oHotSpot.getX() - rx;
			double ey = oHotSpot.getY() - ry;
			double angle = Math.acos(ex / Math.hypot(ex, ey));
			if (omega < 0.0) {
				if (ey > 0.0) {
					angle = 2.0 * Math.PI - angle;
				}
			}
			else {
				if (ey < 0.0) {
					angle -= 2.0 * Math.PI;
				}
				else {
					angle = -angle;
				}
			}
			angle *= 180.0 / Math.PI;
			if (arc == null)
				arc = new Arc2D.Double();
			arc.setArc(rx - arcSize, ry - arcSize, arcSize + arcSize, arcSize + arcSize, 0.0, angle, Arc2D.PIE);
			g.draw(arc);

		}
		else {

			setOHotSpot();
			setFHotSpot();
			drawOmega(g, background);

		}

		g.setColor(Color.red);
		g.fill(oHotSpot);

	}

	public void drawOmega(Graphics2D g, Color background) {
		drawOmega(g, background, inertia * omega * omega * 20000);
	}

	private void drawOmega(Graphics2D g, Color background, double kin) {
		if (arc == null)
			arc = new Arc2D.Double();
		g.setStroke(ViewAttribute.THIN);
		arc.setArc(rx - arcSize, ry - arcSize, arcSize + arcSize, arcSize + arcSize, 0.0, kin, Arc2D.PIE);
		if (omega > 0.0) {
			arc.setAngleExtent(-kin);
			g.setColor(Color.red);
		}
		else {
			g.setColor(Color.green);
		}
		g.fill(arc);
		g.setColor(Color.black);
		g.draw(arc);
		if (oselected && showORects) {
			g.setColor(Color.white);
			g.fill(flipHotSpot);
			g.setColor(Color.black);
			g.draw(flipHotSpot);
		}
		if (omega < 0.0) {
			g.fillOval((int) (rx - 2.0), (int) (ry - 2.0), 4, 4);
		}
		else {
			g.drawLine((int) (rx - 2.0), (int) (ry - 2.0), (int) (rx + 2.0), (int) (ry + 2.0));
			g.drawLine((int) (rx + 2.0), (int) (ry - 2.0), (int) (rx - 2.0), (int) (ry + 2.0));
		}
	}

	void setOHotSpot() {
		double kin = inertia * omega * omega * 20000 * Math.signum(omega) / 180 * Math.PI;
		oHotSpot.setRect(rx + arcSize * Math.cos(kin) - 3, ry + arcSize * Math.sin(kin) - 3, 6, 6);
	}

	void setFHotSpot() {
		flipHotSpot.x = rx - 4;
		flipHotSpot.y = ry - 4;
	}

	/** a dialog box that allows the user to change the value of dipole moment */
	public void inputDipole() {
		new Inputter(this).input(Inputter.DIPOLE);
	}

}