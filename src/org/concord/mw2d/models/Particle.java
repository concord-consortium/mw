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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.List;

import javax.swing.Timer;

import org.concord.modeler.VectorFlavor;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.FloatQueueTwin;
import org.concord.mw2d.MDView;
import org.concord.mw2d.StyleConstant;
import org.concord.mw2d.ViewAttribute;

import static org.concord.mw2d.models.Trigonometry.*;

/**
 * This is the abstract class from which you should derive a Newtonian particle in two dimension, such as an
 * <tt>Atom</tt> or a <tt>UnitedAtom</tt>.
 * 
 * @author Charles Xie
 */

public abstract class Particle implements Comparable, Cloneable, Serializable, ModelComponent {

	final static double ZERO = 0.000000000000001;
	final static Font FONT_ON_TOP = new Font("Arial", Font.PLAIN | Font.BOLD, 9);

	static NumberFormat format;
	static Color blinkColor;
	static int[] xPoints = new int[4];
	static int[] yPoints = new int[4];

	/* the background color of this particle */
	volatile Color color;

	/* color for the charge sign */
	Color chargeColor = Color.black;

	/* color for the trajectory line */
	Color trajectoryColor;

	/* the name of this particle */
	transient String name;

	/* the net charge of this particle */
	volatile double charge;

	/*
	 * the hydrophobicity of this particle. Negative means hydrophilic. This property is used when the particle is used
	 * to model a molecule in solution. For simplicity, this parameter is set to be an integer.
	 */
	volatile int hydrophobic;

	/* Friction coefficent. The friction force is proportional to the velocity. */
	float friction;

	byte dampType = MDModel.AIR_DRAG;

	/* the x coordinate of the center of mass */
	volatile double rx;

	/* the y coordinate of the center of mass */
	volatile double ry;

	/* the x velocity of the center of mass */
	volatile double vx;

	/* the y velocity of the center of mass */
	volatile double vy;

	/* the custom property */
	volatile float custom;

	/* the x acceleration of the center of mass */
	transient double ax;

	/* the y acceleration of the center of mass */
	transient double ay;

	/* the x-component of the force the center of mass feels */
	transient double fx;

	/* the y-component of the force the center of mass feels */
	transient double fy;

	/*
	 * the stepwise displacement in x direction from the previous position before this integration step
	 */
	double dx;

	/*
	 * the stepwise displacement in y direction from the previous position before this integration step
	 */
	double dy;

	/*
	 * custom external force applied to this particle. Unit: 10eV/A
	 */
	float hx, hy;

	/* the harmonic potential restraint to the center of mass */
	PointRestraint restraint;

	/* the user field exerted on this particle to steer its motion */
	UserField userField;

	/* store <tt>rx, ry</tt> queues */
	transient FloatQueueTwin rQ;

	/* store <tt>vx, vy</tt> queues */
	transient FloatQueueTwin vQ;

	/* store <tt>ax, ay</tt> queues */
	transient FloatQueueTwin aQ;

	/* true if this particle is selected */
	transient boolean selected;

	/*
	 * true if this particle is marked. Note: Being marked is different from being selected in that an action will have
	 * no effect on a marked particle.
	 */
	boolean marked;

	boolean visible = true;

	boolean draggable = true;

	boolean movable = true;

	/* if the trajectory of the center of mass of this particle is shown */
	boolean showRTraj;

	/* if the mean position of the center of mass of this particle is shown */
	boolean showRMean;

	/* if the mean force vector on the center of mass of this particle is shown */
	boolean showFMean;

	MeasuringTool measuringTool;
	List measurements;

	/* show the surrounding rectangular handlers for this particle */
	boolean showRects;

	transient int index;

	static Point vHotSpot = new Point(20, 20);

	/* show velocity rectangles or not */
	private static boolean showVRects;

	private transient boolean blinking;
	private GeneralPath lineUp;
	private boolean vselected;
	private double arrowx, arrowy, wingx, wingy, lengthx, lengthy;

	/* Make the transient properties BML-transient: */
	static {
		try {
			BeanInfo info = Introspector.getBeanInfo(Particle.class);
			PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
			for (PropertyDescriptor pd : propertyDescriptors) {
				String name = pd.getName();
				if (name.equals("name") || name.equals("index") || name.equals("ax") || name.equals("ay") || name.equals("fx") || name.equals("fy")
						|| name.equals("selected") || name.equals("blinking")) {
					pd.setValue("transient", Boolean.TRUE);
				}
			}
		}
		catch (Throwable e) {
		}
	}

	public Particle() {
		measuringTool = new MeasuringTool(this);
		measurements = measuringTool.getMeasurements();
		if (format == null) {
			format = NumberFormat.getNumberInstance();
			format.setMaximumFractionDigits(2);
		}
	}

	public void destroy() {
		rQ = null;
		vQ = null;
		aQ = null;
		restraint = null;
		userField = null;
	}

	abstract boolean outOfView();

	/**
	 * get a shallow copy of this particle except its restraint properties. This method differs from the following copy
	 * methods in that a new instance is created to hold the particle, whereas the other methods do not involve any
	 * instantiation but just transferring the properties data between particles.
	 * 
	 * @return a clone of this particle
	 */
	public Object clone() {
		Particle clone = null;
		try {
			clone = (Particle) super.clone();
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
		clone.copyRestraint(null);
		clone.setUserField(null);
		return clone;
	}

	void removeAttachedLayeredComponents() {
		if (getView() == null)
			return;
		List<Layered> l = getView().getLayeredComponentHostedBy(this);
		if (l != null && !l.isEmpty()) {
			for (Layered x : l)
				getView().removeLayeredComponent(x);
		}
	}

	/** get the background color of this particle. */
	public Color getColor() {
		return color;
	}

	/** set the background color of this particle. */
	public void setColor(Color c) {
		color = c;
	}

	public abstract Rectangle getBounds(int skin);

	public abstract Rectangle2D getBounds2D();

	/**
	 * return true if the center of mass of this particle is contained in the Shape.
	 */
	public boolean isCenterOfMassContained(Shape shape) {
		if (shape == null)
			throw new IllegalArgumentException("null shape input");
		return shape.contains(rx, ry);
	}

	/**
	 * return true if the center of mass of this particle is contained in the specified rectangular area.
	 */
	public boolean isCenterOfMassContained(double x, double y, double w, double h) {
		return rx >= x && rx < x + w && ry >= y && ry < y + h;
	}

	/** set the model this particle is associated with */
	public abstract void setModel(MDModel m);

	/** get the model this particle is associated with */
	public abstract MDModel getHostModel();

	/*
	 * predict this particle's new state using the second-order Taylor expansion:
	 * 
	 * <pre> &lt;b&gt;x&lt;/b&gt;(t+dt)=&lt;b&gt;x&lt;/b&gt;(t)+&lt;b&gt;v&lt;/b&gt;(t)dt+0.5&lt;b&gt;a&lt;/b&gt;(t)dtdt
	 * </pre>
	 * 
	 * @param dt the time increment @param dt2 half of the square of the time increment, i.e. 0.5dt^2. This parameter
	 * should be precomputed in order to avoid unnecessary recalculations.
	 */
	abstract void predict(double dt, double dt2);

	/**
	 * assign to this particle its index in the collection (e.g. an array or a List). The index of a particle is
	 * referenced in serializable delegates of objects connecting a set of particles, such as a <code>RadialBond</code>
	 * or an <code>AngularBond</code>.
	 */
	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setSelected(boolean b) {
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
		visible = b;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setDraggable(boolean b) {
		draggable = b;
	}

	public boolean isDraggable() {
		return draggable;
	}

	public void setMovable(boolean b) {
		movable = b;
		if (!movable) {
			vx = vy = 0;
		}
	}

	public boolean isMovable() {
		return movable;
	}

	public String getName() {
		return name;
	}

	public void setName(String s) {
		name = s;
	}

	public abstract double getMass();

	public double getRx() {
		return rx;
	}

	public void setRx(double d) {
		rx = d;
	}

	public double getRy() {
		return ry;
	}

	public void setRy(double d) {
		ry = d;
	}

	public double distanceSquare(Particle p) {
		return distanceSquare(p.rx, p.ry);
	}

	double distanceSquare(double x, double y) {
		return (rx - x) * (rx - x) + (ry - y) * (ry - y);
	}

	public double getVx() {
		return vx;
	}

	public void setVx(double d) {
		vx = d;
	}

	public double getVy() {
		return vy;
	}

	public void setVy(double d) {
		vy = d;
	}

	public void setCustom(float custom) {
		this.custom = custom;
	}

	public float getCustom() {
		return custom;
	}

	public double getAx() {
		return ax;
	}

	public void setAx(double d) {
		ax = d;
	}

	public double getAy() {
		return ay;
	}

	public void setAy(double d) {
		ay = d;
	}

	public double getFx() {
		return fx;
	}

	public void setFx(double d) {
		fx = d;
	}

	public double getFy() {
		return fy;
	}

	public void setFy(double d) {
		fy = d;
	}

	public float getHx() {
		return hx;
	}

	public void setHx(float hx) {
		this.hx = hx;
	}

	public float getHy() {
		return hy;
	}

	public void setHy(float hy) {
		this.hy = hy;
	}

	/** rx=rx*d */
	public void scaleRx(double d) {
		rx *= d;
	}

	/** ry=ry*d */
	public void scaleRy(double d) {
		ry *= d;
	}

	/** rx=rx+d */
	public void addRx(double d) {
		rx += d;
	}

	/** ry=ry+d */
	public void addRy(double d) {
		ry += d;
	}

	/** vx=vx*d */
	public void scaleVx(double d) {
		vx *= d;
	}

	/** vy=vy*d */
	public void scaleVy(double d) {
		vy *= d;
	}

	/** given the speed scalar, assign a velocity vector in a random direction */
	public void setRandomVelocity(double speed) {
		double t = 2.0 * Math.PI * Math.random();
		vx = speed * Math.cos(t);
		vy = speed * Math.sin(t);
	}

	/**
	 * FIXME: set a random velocity to this particle according to the current temperature
	 */
	public void setRandomVelocity() {
		double t = 0;
		if (getHostModel().heatBathActivated()) {
			t = getHostModel().getHeatBath().getExpectedTemperature();
		}
		else {
			t = getHostModel().getTemperature();
		}
		setRandomVelocity(Math.sqrt(2 * t / getMass()) * MDModel.VT_CONVERSION_CONSTANT * 25);
	}

	public double getCharge() {
		return charge;
	}

	public void setCharge(double d) {
		charge = d;
	}

	/** returns true if the specified amount of charge is successfully added */
	public boolean addCharge(double d) {
		if (charge <= 5.0 && charge >= -5.0) {
			charge += d;
			if (charge > 5.0)
				charge = 5.0;
			else if (charge < -5.0)
				charge = -5.0;
			return true;
		}
		return false;
	}

	public Color getChargeColor() {
		return chargeColor;
	}

	public void setChargeColor(Color c) {
		chargeColor = c;
	}

	public Color getTrajectoryColor() {
		return trajectoryColor;
	}

	public void setTrajectoryColor(Color c) {
		trajectoryColor = c;
	}

	public int getHydrophobicity() {
		return hydrophobic;
	}

	public void setHydrophobicity(int a) {
		hydrophobic = a;
	}

	public byte getDampType() {
		return dampType;
	}

	public void setDampType(byte dampType) {
		this.dampType = dampType;
	}

	public float getFriction() {
		return friction;
	}

	public void setFriction(float f) {
		if (f < 0.0f)
			throw new IllegalArgumentException("Fiction coefficient cannot be set negative");
		friction = f;
	}

	/** returns true if the specified amount of damping is successfully increased */
	public boolean addFriction(float d) {
		if (friction < 100.0f && friction > -0.000001f) {
			friction += d;
			if (friction > 100.0f)
				friction = 100.0f;
			else if (friction < 0.0f)
				friction = 0.0f;
			return true;
		}
		return false;
	}

	public PointRestraint getRestraint() {
		return restraint;
	}

	public void setRestraint(PointRestraint pr) {
		if (pr != null && pr.getK() < ZERO)
			restraint = null;
		else restraint = pr;
	}

	public UserField getUserField() {
		return userField;
	}

	public void setUserField(UserField uf) {
		userField = uf;
	}

	double getVelocityAngle() {
		if (Math.abs(vx) < ZERO)
			return vy > 0 ? Math.PI * 0.5 : -Math.PI * 0.5;
		if (Math.abs(vy) < ZERO)
			return vx > 0 ? 0 : Math.PI;
		return vx > 0 ? Math.atan(vy / vx) : Math.PI + Math.atan(vy / vx);
	}

	/** set the selection state of this particle's linear velocity vector */
	public void setVelocitySelection(boolean b) {
		vselected = b;
		if (b) {
			showVRects = true;
			putVHotSpotAtVelocityTip();
		}
	}

	public boolean isVelocityHandleSelected(int x, int y) {
		return x > vHotSpot.x - 5 && x < vHotSpot.x + 5 && y > vHotSpot.y - 5 && y < vHotSpot.y + 5;
	}

	public void setVelocityHandleLocation(int x, int y) {
		vHotSpot.setLocation(x, y);
	}

	/** get the selection state of this particle's linear velocity vector */
	public boolean velocitySelected() {
		return vselected;
	}

	void putVHotSpotAtVelocityTip() {
		vHotSpot.setLocation((int) (rx + getView().getVelocityFlavor().getLength() * vx), (int) (ry + getView().getVelocityFlavor().getLength() * vy));
	}

	/**
	 * DO NOT CALL: This method is for serializing the measurements set in the measuring tool.
	 */
	public void setMeasurements(List list) {
		measurements = list;
	}

	/**
	 * DO NOT CALL: This method is for serializing the measurements set in the measuring tool.
	 */
	public List getMeasurements() {
		return measurements;
	}

	public int getMeasurement(int x, int y) {
		return measuringTool.getMeasurement(x, y);
	}

	public void clearMeasurements() {
		measuringTool.clear();
	}

	public int getNumberOfMeasurements() {
		return measuringTool.getMeasurements().size();
	}

	public Object getMeasurement(int i) {
		return measuringTool.getMeasurements().get(i);
	}

	public void addMeasurement(int index) {
		measuringTool.addMeasurement(index);
	}

	public void addMeasurement(Point point) {
		measuringTool.addMeasurement(point);
	}

	public void setMeasurement(int i, Object o) {
		measuringTool.setMeasurement(i, o);
	}

	public void removeMeasurement(int i) {
		measuringTool.removeMeasurement(i);
	}

	public void renderMeasurements(Graphics2D g) {
		measuringTool.render(g);
	}

	/**
	 * erase the properties that have been set for this particle. These properties includes: charge, restraint, dipole
	 * moment, user field, movie queues, and so on.
	 */
	public void erase() {
		eraseProperties();
		if (!getHostModel().getRecorderDisabled())
			initializeMovieQ(-1);
	}

	/** This method just erases the properties but, unlike erase(), does not remove the movie queues. */
	public void eraseProperties() {
		custom = 0;
		vx = vy = 0;
		ax = ay = 0;
		fx = fy = 0;
		hx = hy = 0;
		visible = true;
		draggable = true;
		movable = true;
		marked = false;
		selected = false;
		color = null;
		setRestraint(null);
		setName(null);
		setCharge(0);
		setFriction(0);
		setDampType(MDModel.AIR_DRAG);
		setUserField(null);
		setShowRTraj(false);
		setShowRMean(false);
		setShowFMean(false);
		measuringTool.clear();
		if (getView() != null)
			getView().removeAttachedLayeredComponents(this);
	}

	/**
	 * set all the proporties of the specified particle to this one. This method is used to copy the state of a particle
	 * for non-GUI operations.
	 */
	public abstract void set(Particle p);

	/**
	 * duplicate all the properties the specified particle except for the location-specific ones (such as restraints).
	 * This method is used in GUI operations.
	 */
	public void duplicate(Particle p, boolean copyLayers) {
		if (p == null)
			throw new IllegalArgumentException("null input");
		if (getHostModel() == null)
			setModel(p.getHostModel());
		rx = p.rx;
		ry = p.ry;
		vx = p.vx;
		vy = p.vy;
		hx = p.hx;
		hy = p.hy;
		custom = p.custom;
		name = p.name;
		charge = p.charge;
		friction = p.friction;
		dampType = p.dampType;
		chargeColor = p.chargeColor;
		marked = p.marked;
		movable = p.movable;
		visible = p.visible;
		showRMean = p.showRMean;
		showFMean = p.showFMean;
		if (copyLayers && getView() != null)
			getView().copyAttachedLayeredComponents(p, this);
	}

	/**
	 * translate this particle to the given position
	 * 
	 * @param rx
	 *            x coordindate to be translated to
	 * @param ry
	 *            y coordindate to be translated to
	 */
	public void translateTo(double rx, double ry) {
		this.rx = rx;
		this.ry = ry;
	}

	/**
	 * translate this particle to the given position
	 * 
	 * @param point
	 *            the location this particle is to be translated to
	 */
	public void translateTo(Point2D point) {
		rx = point.getX();
		ry = point.getY();
	}

	/**
	 * translate this particle by the given x,y displacements.
	 * 
	 * @param deltaX
	 *            displacement in x direction
	 * @param deltaY
	 *            displacement in y direction
	 */
	public void translateBy(double deltaX, double deltaY) {
		rx += deltaX;
		ry += deltaY;
	}

	public Point2D getLocation() {
		return new Point2D.Double(rx, ry);
	}

	void copyRestraint(PointRestraint r) {
		if (r != null) {
			if (restraint != null) {
				restraint.copy(r);
			}
			else {
				restraint = new PointRestraint(r);
			}
		}
		else {
			restraint = null;
		}
	}

	int getMovieInterval() {
		return getHostModel().movieUpdater.getInterval();
	}

	public void initializeMovieQ(int n) {
		initializeRQ(n);
		initializeVQ(n);
		initializeAQ(n);
	}

	/**
	 * initialize coordinate queue twin. If the passed integer is less than 1, nullify the queues.
	 */
	public void initializeRQ(int n) {
		if (rQ == null) {
			if (n < 1)
				return; // already null
			rQ = new FloatQueueTwin(new FloatQueue("Rx: " + toString(), n), new FloatQueue("Ry: " + toString(), n));
			rQ.setInterval(getMovieInterval());
			rQ.setPointer(0);
			rQ.setCoordinateQueue(getHostModel().getModelTimeQueue());
			getHostModel().getMovieQueueGroup().add(rQ);
		}
		else {
			rQ.setLength(n);
			if (n < 1) {
				getHostModel().getMovieQueueGroup().remove(rQ);
				rQ = null;
			}
			else {
				rQ.setPointer(0);
			}
		}
	}

	/**
	 * initialize velocity queue twin. If the passed integer is less than 1, nullify the array.
	 */
	public void initializeVQ(int n) {
		if (vQ == null) {
			if (n < 1)
				return; // already null
			vQ = new FloatQueueTwin(new FloatQueue("Vx: " + toString(), n), new FloatQueue("Vy: " + toString(), n));
			vQ.setInterval(getMovieInterval());
			vQ.setPointer(0);
			vQ.setCoordinateQueue(getHostModel().getModelTimeQueue());
			getHostModel().getMovieQueueGroup().add(vQ);
		}
		else {
			vQ.setLength(n);
			if (n < 1) {
				getHostModel().getMovieQueueGroup().remove(vQ);
				vQ = null;
			}
			else {
				vQ.setPointer(0);
			}
		}
	}

	/**
	 * initialize acceleration queue twin. If the passed integer is less than 1, nullify the array.
	 */
	public void initializeAQ(int n) {
		if (aQ == null) {
			if (n < 1)
				return; // already null
			aQ = new FloatQueueTwin(new FloatQueue("Ax: " + toString(), n), new FloatQueue("Ay: " + toString(), n));
			aQ.setInterval(getMovieInterval());
			aQ.setPointer(0);
			aQ.setCoordinateQueue(getHostModel().getModelTimeQueue());
			getHostModel().getMovieQueueGroup().add(aQ);
		}
		else {
			aQ.setLength(n);
			if (n < 1) {
				getHostModel().getMovieQueueGroup().remove(aQ);
				aQ = null;
			}
			else {
				aQ.setPointer(0);
			}
		}
	}

	/** push current coordinate into the coordinate queue */
	public synchronized void updateRQ() {
		if (rQ == null || rQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		rQ.update(rx, ry);
	}

	/** push current velocity into the velocity queue */
	public synchronized void updateVQ() {
		if (vQ == null || vQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		vQ.update(vx, vy);
	}

	/** push current acceleration into the acceleration queue */
	public synchronized void updateAQ() {
		if (aQ == null || aQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		aQ.update(ax, ay);
	}

	/**
	 * When an array is initialized and its elements subsequently filled, it occurs that until the array is full, some
	 * of the elements are empty, the pointer points to the begin index of unfilled segment. The pointer will stop at
	 * the last index of the array once the whole array is filled up.
	 */
	public synchronized int getRPointer() {
		if (rQ == null || rQ.isEmpty())
			return -1;
		return rQ.getPointer();
	}

	/** @see org.concord.mw2d.models.Particle#getRPointer */
	public synchronized void moveRPointer(int i) {
		if (rQ == null || rQ.isEmpty())
			return;
		rQ.setPointer(i);
	}

	/** @see org.concord.mw2d.models.Particle#getRPointer */
	public synchronized int getVPointer() {
		if (vQ == null || vQ.isEmpty())
			return -1;
		return vQ.getPointer();
	}

	/** @see org.concord.mw2d.models.Particle#getRPointer */
	public synchronized void moveVPointer(int i) {
		if (vQ == null || vQ.isEmpty())
			return;
		vQ.setPointer(i);
	}

	/** @see org.concord.mw2d.models.Particle#getRPointer */
	public synchronized int getAPointer() {
		if (aQ == null || aQ.isEmpty())
			return -1;
		return aQ.getPointer();
	}

	/** @see org.concord.mw2d.models.Particle#getRPointer */
	public synchronized void moveAPointer(int i) {
		if (aQ == null || aQ.isEmpty())
			return;
		aQ.setPointer(i);
	}

	public FloatQueueTwin getRxRyQueue() {
		return rQ;
	}

	public FloatQueueTwin getVxVyQueue() {
		return vQ;
	}

	public FloatQueueTwin getAxAyQueue() {
		return aQ;
	}

	public void setShowRTraj(boolean b) {
		showRTraj = b;
		if (!b)
			lineUp = null;
	}

	public boolean getShowRTraj() {
		return showRTraj;
	}

	void renderRTraj(Graphics2D g) {
		if (!movable)
			return;
		if (rQ == null || rQ.isEmpty())
			return;

		Stroke oldStroke = g.getStroke();
		Color oldColor = g.getColor();
		if (marked) {
			g.setColor(getView().getMarkColor());
		}
		else {
			g.setColor(trajectoryColor != null ? trajectoryColor : getView().contrastBackground());
		}
		switch (getView().getTrajectoryStyle()) {
		case StyleConstant.TRAJECTORY_LINE_STYLE:
			if (lineUp == null)
				lineUp = new GeneralPath();
			else lineUp.reset();
			g.setStroke(ViewAttribute.THIN_DASHED);
			break;
		case StyleConstant.TRAJECTORY_DOTTEDLINE_STYLE:
		case StyleConstant.TRAJECTORY_CIRCLES_STYLE:
			g.setStroke(ViewAttribute.THIN);
			g.setColor(new Color(0x55ffffff & g.getColor().getRGB(), true));
			break;
		}

		int ibeg = 0;
		int iend = rQ.getPointer();
		while (ibeg < iend - 1 && Math.abs(rQ.getQueue1().getData(ibeg) + rQ.getQueue2().getData(ibeg)) < ZERO)
			ibeg++;
		if (iend > ibeg) {
			float x = rQ.getQueue1().getData(ibeg);
			float y = rQ.getQueue2().getData(ibeg);
			switch (getView().getTrajectoryStyle()) {
			case StyleConstant.TRAJECTORY_LINE_STYLE:
				lineUp.moveTo(x, y);
				break;
			case StyleConstant.TRAJECTORY_DOTTEDLINE_STYLE:
				g.drawOval((int) x, (int) y, 1, 1);
				break;
			case StyleConstant.TRAJECTORY_CIRCLES_STYLE:
				if (this instanceof Atom) {
					double d = ((Atom) this).sigma;
					g.drawOval((int) (x - d * 0.5), (int) (y - d * 0.5), (int) d, (int) d);
				}
				break;
			}
			for (int n = ibeg; n < iend; n++) {
				x = rQ.getQueue1().getData(n);
				y = rQ.getQueue2().getData(n);
				switch (getView().getTrajectoryStyle()) {
				case StyleConstant.TRAJECTORY_LINE_STYLE:
					if (lineUp != null) // in case lineUp has been nullified in the middle of this loop
						lineUp.lineTo(x, y);
					break;
				case StyleConstant.TRAJECTORY_DOTTEDLINE_STYLE:
					g.drawOval((int) (x - 1), (int) (y - 1), 2, 2);
					break;
				case StyleConstant.TRAJECTORY_CIRCLES_STYLE:
					if (this instanceof Atom) {
						double d = ((Atom) this).sigma;
						g.drawOval((int) (x - d * 0.5), (int) (y - d * 0.5), (int) d, (int) d);
					}
					break;
				}
			}
		}
		switch (getView().getTrajectoryStyle()) {
		case StyleConstant.TRAJECTORY_LINE_STYLE:
			g.draw(lineUp);
			break;
		}

		g.setStroke(oldStroke);
		g.setColor(oldColor);

	}

	MDView getView() {
		if (getHostModel() == null)
			return null;
		return ((MDView) getHostModel().getView());
	}

	public void setShowRMean(boolean b) {
		showRMean = b;
	}

	public boolean getShowRMean() {
		return showRMean;
	}

	public void renderMeanPosition(Graphics2D g) {
		if (!movable || !visible)
			return;
		if (!showRMean)
			return;
		if (rQ == null || rQ.isEmpty())
			return;
		if (outOfView())
			return;
		int x = (int) rQ.getQueue1().getAverage();
		int y = (int) rQ.getQueue2().getAverage();
		if (x <= 0 && y <= 0)
			return;
		Stroke oldStroke = g.getStroke();
		Color oldColor = g.getColor();
		g.setColor(getView().getBackground());
		g.fillOval(x - 2, y - 2, 4, 4);
		g.setColor(marked ? getView().getMarkColor() : getView().contrastBackground());
		g.setStroke(ViewAttribute.THIN);
		g.drawOval(x - 3, y - 3, 6, 6);
		g.drawLine(x - 6, y, x + 6, y);
		g.drawLine(x, y - 6, x, y + 6);
		g.setStroke(ViewAttribute.THIN_DASHED);
		g.drawLine(x, y, (int) rx, (int) ry);
		g.setStroke(oldStroke);
		g.setColor(oldColor);
	}

	public void setShowFMean(boolean b) {
		showFMean = b;
	}

	public boolean getShowFMean() {
		return showFMean;
	}

	public void renderMeanForce(Graphics2D g) {
		if (!showFMean)
			return;
		if (aQ == null || aQ.isEmpty())
			return;
		if (outOfView())
			return;
		float x = aQ.getQueue1().getAverage();
		float y = aQ.getQueue2().getAverage();
		if (Math.abs(x) <= ZERO && Math.abs(y) <= ZERO)
			return;
		g.setColor(getView().getBackground());
		g.fillOval((int) (rx - 2), (int) (ry - 2), 4, 4);
		g.setColor(marked ? getView().getMarkColor() : getView().contrastBackground());
		g.setStroke(ViewAttribute.THIN);
		g.drawOval((int) (rx - 3), (int) (ry - 3), 6, 6);
		drawVector(g, x * getMass() * 120, y * getMass() * 120, getView().getForceFlavor());
	}

	/** draw this particle's velocity vector on the passed graphics */
	public void drawVelocityVector(Graphics2D g) {
		drawVector(g, vx, vy, getView().getVelocityFlavor());
	}

	/** render this particle's momemtum vector on the passed graphics */
	public void drawMomentumVector(Graphics2D g) {
		drawVector(g, vx * getMass() * 120, vy * getMass() * 120, getView().getMomentumFlavor());
	}

	/**
	 * render this particle's acceleration vector on the passed graphics. Draw the accelerator vectors 10 times shorter
	 * than the force ones if the flavor length is the same (we can't use the mass as the reducing factor, because the
	 * mass can easily be large enough to make the vectors too short).
	 */
	public void drawAccelerationVector(Graphics2D g) {
		drawVector(g, ax * 12, ay * 12, getView().getAccelerationFlavor());
	}

	/** render this particle's force vector on the passed graphics */
	public void drawForceVector(Graphics2D g) {
		drawVector(g, fx * 120, fy * 120, getView().getForceFlavor());
	}

	/** render this particle's custom force vector on the passed graphics */
	public void drawExternalForceVector(Graphics2D g) {
		if (Math.abs(hx) > ZERO || Math.abs(hy) > ZERO)
			drawVector(g, hx, hy, getView().getForceFlavor());
	}

	private void drawVector(Graphics2D g, double x, double y, VectorFlavor flavor) {
		if (outOfView())
			return;
		double r = Math.hypot(x, y);
		if (r < ZERO)
			return;
		arrowx = x / r;
		arrowy = y / r;
		g.setColor(flavor.getColor());
		g.setStroke(flavor.getStroke());
		lengthx = flavor.getLength() * x;
		lengthy = flavor.getLength() * y;
		g.drawLine((int) rx, (int) ry, (int) (rx + lengthx), (int) (ry + lengthy));
		float arrowLength = Math.max(5, 3 * flavor.getWidth());
		wingx = arrowLength * (arrowx * COS45 + arrowy * SIN45);
		wingy = arrowLength * (arrowy * COS45 - arrowx * SIN45);
		g.drawLine((int) (rx + lengthx), (int) (ry + lengthy), (int) (rx + lengthx - wingx), (int) (ry + lengthy - wingy));
		wingx = arrowLength * (arrowx * COS45 - arrowy * SIN45);
		wingy = arrowLength * (arrowy * COS45 + arrowx * SIN45);
		g.drawLine((int) (rx + lengthx), (int) (ry + lengthy), (int) (rx + lengthx - wingx), (int) (ry + lengthy - wingy));
	}

	/**
	 * If this particle's velocity vector is selected for action, render it to the passed graphics.
	 */
	public void drawSelectedVelocityVector(Graphics2D g, Color background, boolean adjusting) {
		if (!vselected || !showVRects)
			return;
		if (adjusting) {
			g.setColor(getContrastColor(background));
			g.setStroke(ViewAttribute.THIN_DASHED);
			g.drawLine((int) rx, (int) ry, vHotSpot.x, vHotSpot.y);
		}
		else {
			putVHotSpotAtVelocityTip();
			drawVelocityVector(g);
		}
		xPoints[0] = vHotSpot.x - 4;
		yPoints[0] = vHotSpot.y;
		xPoints[1] = vHotSpot.x;
		yPoints[1] = vHotSpot.y + 4;
		xPoints[2] = vHotSpot.x + 4;
		yPoints[2] = vHotSpot.y;
		xPoints[3] = vHotSpot.x;
		yPoints[3] = vHotSpot.y - 4;
		g.setColor(Color.red);
		g.fillPolygon(xPoints, yPoints, 4);
		g.setColor(((MDView) getHostModel().getView()).contrastBackground());
		g.setStroke(ViewAttribute.THIN);
		g.drawPolygon(xPoints, yPoints, 4);
	}

	/** a dialog that allows the user to change the charge of the particle */
	public void inputCharge() {
		new Inputter(this).input(Inputter.CHARGE);
	}

	/**
	 * a dialog that allows the user to change the pointwise restraint exerted on the particle
	 */
	public void inputRestraint() {
		new Inputter(this).input(Inputter.RESTRAINT);
	}

	/** blink this particle */
	public void blink() {
		final Timer timer = new Timer(250, null);
		timer.setRepeats(true);
		timer.setInitialDelay(0);
		timer.start();
		setBlinking(true);
		timer.addActionListener(new ActionListener() {
			private int blinkIndex;

			public void actionPerformed(ActionEvent e) {
				if (blinkIndex < 8) {
					blinkIndex++;
					blinkColor = blinkIndex % 2 == 0 ? ((MDView) getHostModel().getView()).contrastBackground() : getHostModel().getView()
							.getBackground();
				}
				else {
					timer.stop();
					blinkIndex = 0;
					setBlinking(false);
				}
				getHostModel().getView().paintImmediately(getBounds(20));
			}
		});
	}

	/* @return a contrasting color for the input one */
	static Color getContrastColor(Color color) {
		return new Color(0xffffff ^ color.getRGB());
	}

}