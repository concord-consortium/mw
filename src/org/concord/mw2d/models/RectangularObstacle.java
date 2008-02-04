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
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.Timer;

import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.GradientFactory;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.FloatQueueTwin;
import org.concord.mw2d.MDView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.ViewAttribute;

/**
 * How we set 2D pressure unit from impulse: 2mv/dt? Imagine it were 3D, first we convert the internal units to the
 * standard unit: 10^-11 m/10^-15 s * (10^-3 kg/(6*10^23)) / (10^-22 m^2 * 10^-15 s). This factor evaluates to
 * 1.667*10^14 kg*m/s^2/m^2 = 1.667*10^14 Pa, which is too big. But the following two factors must be taken into
 * account: (1) To convert 2D measurement into 3D one, imagine an extrusion of a slab that is approximately 10 angstrom
 * thick, but not 0.1 angstrom thick as is in the above conversion. Molecules collide with the obstacle in such a thin
 * slab as frequently as in the 2D case. This will reduce the factor 100 times to 1.667*10^12 Pa, which is 1.667*10^7
 * bar. (2) We assume that the collision between a molecule and the obstacle is perfectly rigid, which means that the
 * impact energy is 100% elastic. In reality, the surface of the obstacle is composed of atoms that also interact with
 * the incoming molecule. Assume the inelasticity makes the pressure conversion rate to be as low as 10% (what a fake!).
 * Then we arrive at 1.667*10^6 bar. This is the factor used to convert the computed pressure to the common unit: bar.
 */

public class RectangularObstacle extends Rectangle2D.Double implements Obstacle {

	public final static short HEAVY = 500;
	public final static byte INSIDE = 0;
	public final static byte NW = 1;
	public final static byte NORTH = 2;
	public final static byte NE = 3;
	public final static byte EAST = 4;
	public final static byte SE = 5;
	public final static byte SOUTH = 6;
	public final static byte SW = 7;
	public final static byte WEST = 8;

	private static Rectangle2D rectN = new Rectangle2D.Float(), rectS = new Rectangle2D.Float(),
			rectE = new Rectangle2D.Float(), rectW = new Rectangle2D.Float(), rectNE = new Rectangle2D.Float(),
			rectNW = new Rectangle2D.Float(), rectSE = new Rectangle2D.Float(), rectSW = new Rectangle2D.Float();

	private static Rectangle2D.Double ballRect, intersectRect;
	private static int defaultRoundCornerRadius = 10;
	private static AffineTransform transform;

	final static String COLLISION_EVENT = "Collision event";

	private boolean stateStored;
	private double savedX, savedY, savedWidth, savedHeight;

	/* the x velocity of the center of mass */
	volatile double vx;

	/* the y velocity of the center of mass */
	volatile double vy;

	/* the x acceleration of the center of mass */
	transient double ax;

	/* the y acceleration of the center of mass */
	transient double ay;

	/* the displacement in x direction from previous position */
	double dx;

	/* the displacement in y direction from previous position */
	double dy;

	// the density of this obstacle. By default, it equals to 0.01, which means a 10x10 square object weighs 1.0.
	private double density = 0.01;

	float friction;

	float elasticity = 1.0f;

	/* the user field exerted on this object to steer its motion */
	private UserField userField;

	/* store <tt>rx, ry</tt> queues */
	FloatQueueTwin rxryQ;

	/* store <tt>vx, vy</tt> queues */
	FloatQueueTwin vxvyQ;

	/* store <tt>ax, ay</tt> queues */
	FloatQueueTwin axayQ;

	FloatQueue[] peQ, pwQ, pnQ, psQ;

	private transient boolean blinking;
	private transient boolean selected;
	private boolean marked;
	private boolean visible = true;
	private boolean bounceAtBoundary = true;
	private boolean partOfSystem = true;

	private MDModel model;
	private int cornerArcRadius;
	private int spacing = 10;
	private Vector<GeneralPath> group;
	private FillMode fillMode;
	private Image fullImage;
	private ImageIcon bgImage;
	private float[] pEast = new float[5];
	private float[] pWest = new float[5];
	private float[] pNorth = new float[5];
	private float[] pSouth = new float[5];
	private float[][] peBuffer = new float[5][40];
	private float[][] pwBuffer = new float[5][40];
	private float[][] psBuffer = new float[5][40];
	private float[][] pnBuffer = new float[5][40];
	private boolean westProbe, eastProbe, northProbe, southProbe;
	private float px, py;

	private static double va1, vo1;
	private List<Integer> colList;
	private List<FaceCollision> faceColList;
	private double delta = 10.0;
	private static Line2D tempLine;
	static Color blinkColor;

	public RectangularObstacle() {
		super();
		group = new Vector<GeneralPath>();
		this.density = HEAVY + HEAVY;
	}

	public RectangularObstacle(Rectangle r) {
		this(r.x, r.y, r.width, r.height);
	}

	public RectangularObstacle(double x, double y, double w, double h) {
		this();
		setRect(x, y, w, h);
	}

	public RectangularObstacle(double x, double y, double w, double h, FillMode fm) {
		this(x, y, w, h, 0, 0, 0, 0, null, 1, 0, HEAVY + HEAVY, false, false, false, false, true, true, false, fm);
	}

	public RectangularObstacle(double x, double y, double w, double h, double vx, double vy, float px, float py,
			UserField userField, float elasticity, float friction, double density, boolean westProbe,
			boolean northProbe, boolean eastProbe, boolean southProbe, boolean bounced, boolean visible,
			boolean roundCornered, FillMode fm) {
		this(x, y, w, h);
		setFillMode(fm);
		this.vx = vx;
		this.vy = vy;
		this.px = px;
		this.py = py;
		this.userField = userField;
		this.density = density;
		this.elasticity = elasticity;
		this.friction = friction;
		this.visible = visible;
		this.westProbe = westProbe;
		this.northProbe = northProbe;
		this.eastProbe = eastProbe;
		this.southProbe = southProbe;
		bounceAtBoundary = bounced;
		cornerArcRadius = roundCornered ? defaultRoundCornerRadius : 0;
	}

	public void storeCurrentState() {
		savedX = x;
		savedY = y;
		savedWidth = width;
		savedHeight = height;
		stateStored = true;
	}

	public void restoreState() {
		if (!stateStored)
			return;
		setRect(savedX, savedY, savedWidth, savedHeight);
	}

	public void destroy() {
		model = null;
		rxryQ = null;
		vxvyQ = null;
		axayQ = null;
		peQ = null;
		pwQ = null;
		psQ = null;
		pnQ = null;
		bgImage = null;
		fullImage = null;
	}

	public byte getPositionCode(int x, int y) {
		if (rectNW.contains(x, y))
			return NW;
		if (rectN.contains(x, y))
			return NORTH;
		if (rectNE.contains(x, y))
			return NE;
		if (rectE.contains(x, y))
			return EAST;
		if (rectSE.contains(x, y))
			return SE;
		if (rectS.contains(x, y))
			return SOUTH;
		if (rectSW.contains(x, y))
			return SW;
		if (rectW.contains(x, y))
			return WEST;
		if (contains(x, y))
			return INSIDE;
		return -1;
	}

	public void blink() {

		final Timer timer = new Timer(250, null);
		timer.setRepeats(true);
		timer.setInitialDelay(0);
		timer.start();
		if (model.getView() != null)
			setBlinking(true);

		timer.addActionListener(new ActionListener() {

			private int blinkIndex;

			public void actionPerformed(ActionEvent e) {
				if (model.getView() == null) {
					timer.stop(); // view could be set null while saving the model
					blinkIndex = 0;
					setBlinking(false);
				}
				if (blinkIndex < 6) {
					blinkIndex++;
					if (model.getView() != null)
						blinkColor = blinkIndex % 2 == 0 ? ((MDView) model.getView()).contrastBackground() : model
								.getView().getBackground();
				}
				else {
					timer.stop();
					blinkIndex = 0;
					setBlinking(false);
				}
				if (model.getView() != null)
					model.getView().paintImmediately((int) (x - 20), (int) (y - 20), (int) (width + 40),
							(int) (height + 40));
			}

		});

	}

	public static int getDefaultRoundCornerRadius() {
		return defaultRoundCornerRadius;
	}

	public void setRoundCornerRadius(int i) {
		cornerArcRadius = i;
	}

	public int getRoundCornerRadius() {
		return cornerArcRadius;
	}

	public void setElasticity(float f) {
		elasticity = f;
	}

	public float getElasticity() {
		return elasticity;
	}

	public void setBounced(boolean b) {
		bounceAtBoundary = b;
	}

	public boolean isBounced() {
		return bounceAtBoundary;
	}

	public void setWestProbe(boolean b) {
		westProbe = b;
	}

	public boolean isWestProbe() {
		return westProbe;
	}

	public void setEastProbe(boolean b) {
		eastProbe = b;
	}

	public boolean isEastProbe() {
		return eastProbe;
	}

	public void setSouthProbe(boolean b) {
		southProbe = b;
	}

	public boolean isSouthProbe() {
		return southProbe;
	}

	public void setNorthProbe(boolean b) {
		northProbe = b;
	}

	public boolean isNorthProbe() {
		return northProbe;
	}

	public void setExternalFx(float x) {
		px = x;
	}

	public float getExternalFx() {
		return px;
	}

	public void setExternalFy(float y) {
		py = y;
	}

	public float getExternalFy() {
		return py;
	}

	public void setPartOfSystem(boolean b) {
		partOfSystem = b;
	}

	public boolean isPartOfSystem() {
		return partOfSystem;
	}

	public void setMovable(boolean b) {
		setDensity(HEAVY + HEAVY);
	}

	public boolean isMovable() {
		return density < HEAVY;
	}

	public void setDensity(double m) {
		density = m;
		if (!isMovable()) {
			vx = vy = 0.0;
			ax = ay = 0.0;
		}
	}

	public double getDensity() {
		return density;
	}

	public double getMass() {
		return density * width * height;
	}

	public synchronized void setVx(double vx) {
		this.vx = vx;
	}

	public synchronized double getVx() {
		return vx;
	}

	public synchronized void setVy(double vy) {
		this.vy = vy;
	}

	public synchronized double getVy() {
		return vy;
	}

	public synchronized double getAx() {
		return ax;
	}

	public synchronized void setAx(double d) {
		ax = d;
	}

	public synchronized double getAy() {
		return ay;
	}

	public synchronized void setAy(double d) {
		ay = d;
	}

	public synchronized double getDx() {
		return dx;
	}

	public synchronized double getDy() {
		return dy;
	}

	public float getFriction() {
		return friction;
	}

	public void setFriction(float f) {
		if (f < 0.0f)
			throw new IllegalArgumentException("Fiction coefficient cannot be set negative");
		friction = f;
	}

	public UserField getUserField() {
		return userField;
	}

	public void setUserField(UserField uf) {
		userField = uf;
	}

	public synchronized void move(double dt, double dt2, int n, Atom[] atom) {

		if (!isMovable())
			return;

		double r = MDModel.GF_CONVERSION_CONSTANT * model.getUniverse().getViscosity() * friction;
		ax += px - r * vx;
		ay += py - r * vy;
		if (userField != null)
			userField.dyn(this);
		dx = vx * dt + ax * dt2;
		dy = vy * dt + ay * dt2;
		vx += ax * dt;
		vy += ay * dt;
		x += dx;
		y += dy;

		if (fillMode == null)
			translateDefaultPattern(dx, dy);

		// since moving the obstacle may put atoms back into its territory again, we need to rectify the motion

		if (colList == null || colList.isEmpty())
			return;

		double x0 = x;
		double y0 = y;
		double x1 = x + width;
		double y1 = y + height;
		r = 0.0;
		int i;
		synchronized (colList) {
			for (Iterator it = colList.iterator(); it.hasNext();) {
				i = ((Integer) it.next()).intValue();
				r = 0.5 * atom[i].sigma;
				if (atom[i].rx - r < x1 && atom[i].rx > x1)
					atom[i].rx = x1 + r;
				else if (atom[i].rx + r > x0 && atom[i].rx < x0)
					atom[i].rx = x0 - r;
				if (atom[i].ry - r < y1 && atom[i].ry > y1)
					atom[i].ry = y1 + r;
				else if (atom[i].ry + r > y0 && atom[i].ry < y0)
					atom[i].ry = y0 - r;
			}
		}

	}

	/**
	 * collision of atoms with this obstacle.
	 * 
	 * @param numberOfAtoms
	 *            number of atoms
	 * @param atom
	 *            the atom array
	 * @param fireCollisionEvents
	 *            fire collision events or not
	 * @return a list of collisions specified by the FaceCollision class.
	 * @see org.concord.mw2d.models.FaceCollision;
	 */
	public List<FaceCollision> collide(int numberOfAtoms, Atom[] atom, boolean fireCollisionEvents) {

		double mass = getMass();

		if (fireCollisionEvents) {
			if (faceColList == null) {
				faceColList = new ArrayList<FaceCollision>();
			}
			else {
				faceColList.clear();
			}
		}
		if (colList == null)
			colList = Collections.synchronizedList(new ArrayList<Integer>());
		else colList.clear();

		double x0 = getMinX();
		double y0 = getMinY();
		double x1 = getMaxX();
		double y1 = getMaxY();
		double radius = 0;
		double oldVelocity = 0;

		if (model != null && model.getJob() != null && (model.getJob().getIndexOfStep() - 1) % getMovieInterval() == 0) {
			if (eastProbe)
				Arrays.fill(pEast, 0);
			if (westProbe)
				Arrays.fill(pWest, 0);
			if (northProbe)
				Arrays.fill(pNorth, 0);
			if (southProbe)
				Arrays.fill(pSouth, 0);
		}

		int id;
		byte xing;
		boolean isFixed = !isMovable();

		for (short i = 0; i < numberOfAtoms; i++) {

			Atom a = atom[i];

			radius = a.sigma * 0.5;
			id = a.getID();

			if ((a.rx - radius < x1 && a.rx + radius > x0) && (a.ry - radius < y1 && a.ry + radius > y0)) {
				if (!isFixed)
					colList.add((int) i);
				xing = borderCross(radius, a.rx, a.ry, a.dx, a.dy, x0, y0, x1, y1);
				if (fireCollisionEvents)
					faceColList.add(new FaceCollision(xing, i, a.rx, a.ry, a.vx, a.vy));
				switch (xing) {
				case NORTH:
					if (isFixed) {
						if (northProbe) {
							if (id < 4) {
								pNorth[id] += 2 * a.mass * a.vy;
							}
							else {
								pNorth[4] += 2 * a.mass * a.vy;
							}
						}
						if (elasticity > Particle.ZERO) {
							a.vy = -elasticity * Math.abs(a.vy);
						}
						else {
							a.vx = 0;
							a.vy = 0;
						}
					}
					else {
						if (a.isMovable()) {
							if (northProbe)
								oldVelocity = a.vy;
							computeAfterVel(NORTH, a.mass, a.vy, mass, vy);
							a.vy = va1 * elasticity;
							if (northProbe) {
								if (id < 4) {
									pNorth[id] -= a.mass * (a.vy - oldVelocity);
								}
								else {
									pNorth[4] -= a.mass * (a.vy - oldVelocity);
								}
							}
							setVy(vo1 * elasticity);
						}
						else {
							vy = Math.abs(vy);
						}
					}
					break;
				case WEST:
					if (isFixed) {
						if (westProbe) {
							if (id < 4) {
								pWest[id] += 2 * a.mass * a.vx;
							}
							else {
								pWest[4] += 2 * a.mass * a.vx;
							}
						}
						if (elasticity > Particle.ZERO) {
							a.vx = -elasticity * Math.abs(a.vx);
						}
						else {
							a.vx = 0;
							a.vy = 0;
						}
					}
					else {
						if (a.isMovable()) {
							if (westProbe)
								oldVelocity = a.vx;
							computeAfterVel(WEST, a.mass, a.vx, mass, vx);
							a.vx = va1 * elasticity;
							if (westProbe) {
								if (id < 4) {
									pWest[id] -= a.mass * (a.vx - oldVelocity);
								}
								else {
									pWest[4] -= a.mass * (a.vx - oldVelocity);
								}
							}
							setVx(vo1 * elasticity);
						}
						else {
							vx = Math.abs(vx);
						}
					}
					break;
				case SOUTH:
					if (isFixed) {
						if (southProbe) {
							if (id < 4) {
								pSouth[id] -= 2 * a.mass * a.vy;
							}
							else {
								pSouth[4] -= 2 * a.mass * a.vy;
							}
						}
						if (elasticity > Particle.ZERO) {
							a.vy = elasticity * Math.abs(a.vy);
						}
						else {
							a.vx = 0;
							a.vy = 0;
						}
					}
					else {
						if (a.isMovable()) {
							if (southProbe)
								oldVelocity = a.vy;
							computeAfterVel(SOUTH, a.mass, a.vy, mass, vy);
							a.vy = va1 * elasticity;
							if (southProbe) {
								if (id < 4) {
									pSouth[id] += a.mass * (a.vy - oldVelocity);
								}
								else {
									pSouth[4] += a.mass * (a.vy - oldVelocity);
								}
							}
							setVy(vo1 * elasticity);
						}
						else {
							vy = -Math.abs(vy);
						}
					}
					break;
				case EAST:
					if (isFixed) {
						if (eastProbe) {
							if (id < 4) {
								pEast[id] -= 2 * a.mass * a.vx;
							}
							else {
								pEast[4] -= 2 * a.mass * a.vx;
							}
						}
						if (elasticity > Particle.ZERO) {
							a.vx = elasticity * Math.abs(a.vx);
						}
						else {
							a.vx = 0;
							a.vy = 0;
						}
					}
					else {
						if (a.isMovable()) {
							if (eastProbe)
								oldVelocity = a.vx;
							computeAfterVel(EAST, a.mass, a.vx, mass, vx);
							a.vx = va1 * elasticity;
							if (eastProbe) {
								if (id < 4) {
									pEast[id] += a.mass * (a.vx - oldVelocity);
								}
								else {
									pEast[4] += a.mass * (a.vx - oldVelocity);
								}
							}
							setVx(vo1 * elasticity);
						}
						else {
							vx = -Math.abs(vx);
						}
					}
					break;
				case -1:
					System.out.println("missed face hit: " + i);
					if (elasticity > Particle.ZERO) {
						a.vx *= -elasticity;
						a.vy *= -elasticity;
					}
					else {
						a.vx = 0;
						a.vy = 0;
					}
					break;
				}
			}
		}

		return faceColList;

	}

	private static void computeAfterVel(int side, double ma, double va, double mo, double vo) {
		double alpha = ma / mo;
		double c = ma * va / mo + vo;
		double d = ma * va * va / mo + vo * vo;
		double z1 = c / (1.0 + alpha);
		double z2 = Math.sqrt(alpha * (alpha * d + d - c * c)) / (1.0 + alpha);
		switch (side) {
		case NORTH:
			va1 = z1 - z2 / alpha;
			vo1 = z1 + z2;
			break;
		case WEST:
			va1 = z1 - z2 / alpha;
			vo1 = z1 + z2;
			break;
		case SOUTH:
			va1 = z1 + z2 / alpha;
			vo1 = z1 - z2;
			break;
		case EAST:
			va1 = z1 + z2 / alpha;
			vo1 = z1 - z2;
			break;
		}
	}

	byte borderCross(double rd, double rx, double ry, double dx, double dy, double x0, double y0, double x1, double y1) {

		if (ballRect == null) {
			ballRect = new Rectangle2D.Double(rx - rd, ry - rd, rx + rd, ry + rd);
		}
		else {
			ballRect.setFrame(rx - rd, ry - rd, rd + rd, rd + rd);
		}
		if (intersectRect == null)
			intersectRect = new Rectangle2D.Double();
		Rectangle2D.intersect(this, ballRect, intersectRect);

		double xc = intersectRect.getCenterX();
		double yc = intersectRect.getCenterY();

		if (Line2D.linesIntersect(rx - dx, ry - dy, xc, yc, x0, y0, x1, y0))
			return NORTH;
		if (Line2D.linesIntersect(rx - dx, ry - dy, xc, yc, x0, y0, x0, y1))
			return WEST;
		if (Line2D.linesIntersect(rx - dx, ry - dy, xc, yc, x0, y1, x1, y1))
			return SOUTH;
		if (Line2D.linesIntersect(rx - dx, ry - dy, xc, yc, x1, y0, x1, y1))
			return EAST;

		// old algorithm
		// if(Line2D.linesIntersect(rx-dx,ry-dy+rd,rx,ry+rd, x0, y0, x1, y0))
		// return NORTH;
		// if(Line2D.linesIntersect(rx-dx+rd,ry-dy,rx+rd,ry, x0, y0, x0, y1))
		// return WEST;
		// if(Line2D.linesIntersect(rx-dx,ry-dy-rd,rx,ry-rd, x0, y1, x1, y1))
		// return SOUTH;
		// if(Line2D.linesIntersect(rx-dx-rd,ry-dy,rx-rd,ry, x1, y0, x1, y1))
		// return EAST;

		return -1;

	}

	/** return the x coordinate of the center */
	public double getRx() {
		return getX() + 0.5 * getWidth();
	}

	/** return the y coordinate of the center */
	public double getRy() {
		return getY() + 0.5 * getHeight();
	}

	/** set the model this particle is associated with */
	public void setModel(MDModel m) {
		model = m;
	}

	public MDModel getHostModel() {
		return model;
	}

	public void setFillMode(FillMode fm) {
		fillMode = fm;
		if (fillMode instanceof FillMode.ImageFill) {
			String s = ((FillMode.ImageFill) fillMode).getURL();
			if (FileUtilities.isRemote(s)) {
				try {
					bgImage = new ImageIcon(new URL(s));
				}
				catch (MalformedURLException e) {
					e.printStackTrace(System.err);
				}
				fullImage = bgImage.getImage();
			}
			else {
				fullImage = Toolkit.getDefaultToolkit().createImage(s);
				bgImage = new ImageIcon(fullImage);
			}
		}
		else {
			bgImage = null;
			fullImage = null;
		}
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setSelected(boolean b) {
		selected = b;
		if (b) {
			setGrabRects();
			((MDView) model.getView()).setSelectedComponent(this);
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

	public synchronized void setVisible(boolean b) {
		visible = b;
	}

	public synchronized boolean isVisible() {
		return visible;
	}

	void setGrabRects() {
		float r2dx0 = (float) getMinX();
		float r2dy0 = (float) getMinY();
		float r2dx1 = (float) getMaxX();
		float r2dy1 = (float) getMaxY();
		rectNW.setRect(r2dx0 - 3, r2dy0 - 3, 6, 6);
		rectNE.setRect(r2dx1 - 3, r2dy0 - 3, 6, 6);
		rectSW.setRect(r2dx0 - 3, r2dy1 - 3, 6, 6);
		rectSE.setRect(r2dx1 - 3, r2dy1 - 3, 6, 6);
		if (r2dx1 - r2dx0 > 11.0f) {
			float r2dxm = 0.5f * (r2dx0 + r2dx1);
			rectN.setRect(r2dxm - 3, r2dy0 - 3, 6, 6);
			rectS.setRect(r2dxm - 3, r2dy1 - 3, 6, 6);
		}
		if (r2dy1 - r2dy0 > 11.0f) {
			float r2dym = 0.5f * (r2dy0 + r2dy1);
			rectE.setRect(r2dx1 - 3, r2dym - 3, 6, 6);
			rectW.setRect(r2dx0 - 3, r2dym - 3, 6, 6);
		}
	}

	/**
	 * create a new rectangular obstacle with the same properties. Override <tt>RectangularShape.clone()</tt> because
	 * it will not draw the stripes, an action requiring "deep copy" operations.
	 */
	public Object clone() {
		RectangularObstacle r = new RectangularObstacle(x, y, width, height);
		r.setFillMode(fillMode);
		r.setDensity(density);
		r.cornerArcRadius = cornerArcRadius;
		r.group.clear();
		return r;
	}

	public String toString() {
		return "Obstacle #" + model.getObstacles().indexOf(this);
	}

	private void translateDefaultPattern(double dx, double dy) {
		if (fillMode != null)
			return;
		if (group == null || group.isEmpty()) {
			setStripe();
		}
		else {
			if (transform == null)
				transform = new AffineTransform();
			transform.setToTranslation(dx, dy);
			for (GeneralPath gp : group)
				gp.transform(transform);
		}
	}

	public void translateTo(double x, double y) {
		double dx = x - this.x;
		double dy = y - this.y;
		this.x = x;
		this.y = y;
		if (fillMode == null)
			translateDefaultPattern(dx, dy);
		if (selected)
			setGrabRects();
	}

	public void translateCenterTo(double x, double y) {
		double dx = x - width * 0.5 - this.x;
		double dy = y - height * 0.5 - this.y;
		this.x += dx;
		this.y += dy;
		if (fillMode == null)
			translateDefaultPattern(dx, dy);
		if (selected)
			setGrabRects();
	}

	public void translateBy(double dx, double dy) {
		this.x += dx;
		this.y += dy;
		if (fillMode == null)
			translateDefaultPattern(dx, dy);
		if (selected)
			setGrabRects();
	}

	public void setRect(double x, double y, double w, double h) {
		super.setRect(x, y, w, h);
		if (fillMode == null)
			setStripe();
		if (selected)
			setGrabRects();
	}

	public void setRect(Rectangle2D r2d) {
		super.setRect(r2d);
		if (fillMode == null)
			setStripe();
		if (selected)
			setGrabRects();
	}

	// unused
	void setStripe(int spacing) {
		this.spacing = spacing;
		setStripe();
	}

	private void setStripe() {
		group.clear();
		if (fillMode != null)
			return;
		double d = Math.sqrt(width * width + height * height);
		double theta = Math.acos(width / d);
		float a = spacing * (float) Math.sqrt(0.5);
		int n = (int) (d * Math.cos(Math.PI * 0.25 - theta) / a) - 1;
		float[] xp, yp;
		float xf = (float) x;
		float yf = (float) y;
		float wf = (float) width;
		float hf = (float) height;
		float k, k1;
		for (int i = 0; i < n; i += 2) {
			k = (i + 1) * spacing;
			k1 = k + spacing;
			xp = new float[4];
			yp = new float[4];
			if (k <= wf && k <= hf) {
				/* upper left part */
				if (k1 <= wf && k1 <= hf) {
					xp[0] = xf + k;
					yp[0] = yf;
					xp[1] = xf + k1;
					yp[1] = yf;
					xp[2] = xf;
					yp[2] = yf + k1;
					xp[3] = xf;
					yp[3] = yf + k;
				}
				else if (k1 <= wf && k1 > hf) {
					xp = new float[5];
					yp = new float[5];
					xp[0] = xf + k;
					yp[0] = yf;
					xp[1] = xf + k1;
					yp[1] = yf;
					yp[2] = yf + hf;
					xp[2] = xf + yf + k1 - yp[2];
					xp[3] = xf;
					yp[3] = yf + hf;
					xp[4] = xf;
					yp[4] = yf + k;
				}
				else if (k1 > wf && k1 <= hf) {
					xp = new float[5];
					yp = new float[5];
					xp[0] = xf + k;
					yp[0] = yf;
					xp[1] = xf + wf;
					yp[1] = yf;
					xp[2] = xf + wf;
					yp[2] = xf + yf + k1 - xp[1];
					xp[3] = xf;
					yp[3] = yf + k1;
					xp[4] = xf;
					yp[4] = yf + k;
				}
				else {
					xp = new float[6];
					yp = new float[6];
					xp[0] = xf + k;
					yp[0] = yf;
					xp[1] = xf + wf;
					yp[1] = yf;
					xp[2] = xf + wf;
					yp[2] = xf + yf + k1 - xp[2];
					yp[3] = yf + hf;
					xp[3] = xf + yf + k1 - yp[3];
					xp[4] = xf;
					yp[4] = yf + hf;
					xp[5] = xf;
					yp[5] = xf + yf + k - xp[5];
				}
			}
			else if (k <= wf && k > hf) {
				/* lower left part */
				if (k1 <= wf) {
					xp[0] = xf + k;
					yp[0] = yf;
					xp[1] = xf + k1;
					yp[1] = yf;
					yp[2] = yf + hf;
					xp[2] = xf + yf + k1 - yp[2];
					yp[3] = yp[2];
					xp[3] = xf + yf + k - yp[3];
				}
				else {
					xp = new float[5];
					yp = new float[5];
					xp[0] = xf + k;
					yp[0] = yf;
					xp[1] = xf + wf;
					yp[1] = yf;
					xp[2] = xf + wf;
					yp[2] = xf + yf + k1 - xp[2];
					yp[3] = yf + hf;
					xp[3] = xf + yf + k1 - yp[3];
					yp[4] = yp[3];
					xp[4] = xf + yf + k - yp[4];
				}
			}
			else if (k <= hf && k > wf) {
				/* upper right part */
				if (k1 <= hf) {
					xp[0] = xf + wf;
					yp[0] = xf + yf + k - xp[0];
					xp[1] = xp[0];
					yp[1] = xf + yf + k1 - xp[1];
					xp[2] = xf;
					yp[2] = yf + k1;
					xp[3] = xf;
					yp[3] = yf + k;
				}
				else {
					xp = new float[5];
					yp = new float[5];
					xp[0] = xf + wf;
					yp[0] = xf + yf + k - xp[0];
					xp[1] = xp[0];
					yp[1] = xf + yf + k1 - xp[1];
					yp[2] = yf + hf;
					xp[2] = xf + yf + k1 - yp[2];
					yp[3] = yp[2];
					xp[3] = xf;
					xp[4] = xf;
					yp[4] = xf + yf + k - xp[4];
				}
			}
			else if (k > wf && k > hf) {
				/* lower right part */
				if (k1 <= wf + hf + xf + yf) {
					xp[0] = xf + wf;
					yp[0] = xf + yf + k - xp[0];
					xp[1] = xp[0];
					yp[1] = xf + yf + k1 - xp[1];
					yp[2] = yf + hf;
					xp[2] = xf + yf + k1 - yp[2];
					yp[3] = yp[2];
					xp[3] = xf + yf + k - yp[3];
				}
			}
			addElement(xp, yp);
		}
	}

	private void addElement(float[] xp, float[] yp) {
		GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD, xp.length);
		gp.moveTo(xp[0], yp[0]);
		for (int i = 1; i < xp.length; i++)
			gp.lineTo(xp[i], yp[i]);
		gp.closePath();
		group.add(gp);
	}

	public void render(Graphics2D g) {

		if (model == null)
			return;

		Color cbg = ((MDView) model.getView()).contrastBackground();

		if (visible) {

			if (fillMode instanceof FillMode.ColorFill) {
				g.setColor(((FillMode.ColorFill) fillMode).getColor());
				if (cornerArcRadius > 0) {
					g.fillRoundRect((int) x, (int) y, (int) width, (int) height, cornerArcRadius, cornerArcRadius);
				}
				else {
					g.fill(this);
				}
			}
			else if (fillMode instanceof FillMode.GradientFill) {
				FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
				GradientFactory.paintRect(g, gfm.getStyle(), gfm.getVariant(), gfm.getColor1(), gfm.getColor2(),
						(float) x, (float) y, (float) width, (float) height);
			}
			else if (fillMode instanceof FillMode.PatternFill) {
				FillMode.PatternFill tfm = (FillMode.PatternFill) fillMode;
				if (tfm.getPaint() != null)
					g.setPaint(tfm.getPaint());
				if (cornerArcRadius > 0) {
					g.fillRoundRect((int) x, (int) y, (int) width, (int) height, cornerArcRadius, cornerArcRadius);
				}
				else {
					g.fill(this);
				}
			}
			else if (fillMode instanceof FillMode.ImageFill) {
				if (bgImage != null) {
					if (bgImage.getIconWidth() != (int) width || bgImage.getIconHeight() != (int) height) {
						bgImage = new ImageIcon(fullImage.getScaledInstance((int) width, (int) height,
								Image.SCALE_DEFAULT));
					}
					bgImage.paintIcon(model.getView(), g, (int) x, (int) y);
				}
			}
			else { // default drawing
				g.setColor(Color.yellow);
				if (cornerArcRadius > 0) {
					g.fillRoundRect((int) x, (int) y, (int) width, (int) height, cornerArcRadius, cornerArcRadius);
				}
				else {
					g.fill(this);
				}
				g.setColor(Color.black);
				for (GeneralPath gp : group)
					g.fill(gp);
			}

			// draw the boundary
			g.setColor(cbg.darker());
			g.setStroke(ViewAttribute.THIN);
			if (cornerArcRadius > 0) {
				g.drawRoundRect((int) x, (int) y, (int) width, (int) height, cornerArcRadius, cornerArcRadius);
			}
			else {
				g.draw(this);
			}

			if (westProbe) {
				int n = (int) (height * 0.05);
				g.setColor(model.getView().getBackground());
				for (int i = 0; i < n; i++)
					g.fillRect((int) (x + 2), (int) (y + i * 20 + 10), 5, 5);
				g.setColor(cbg);
				for (int i = 0; i < n; i++)
					g.drawRect((int) (x + 2), (int) (y + i * 20 + 10), 5, 5);
			}
			if (eastProbe) {
				int n = (int) (height * 0.05);
				g.setColor(model.getView().getBackground());
				for (int i = 0; i < n; i++)
					g.fillRect((int) (x + width - 7), (int) (y + i * 20 + 10), 5, 5);
				g.setColor(cbg);
				for (int i = 0; i < n; i++)
					g.drawRect((int) (x + width - 7), (int) (y + i * 20 + 10), 5, 5);
			}
			if (northProbe) {
				int n = (int) (width * 0.05);
				g.setColor(model.getView().getBackground());
				for (int i = 0; i < n; i++)
					g.fillRect((int) (x + i * 20 + 10), (int) (y + 2), 5, 5);
				g.setColor(cbg);
				for (int i = 0; i < n; i++)
					g.drawRect((int) (x + i * 20 + 10), (int) (y + 2), 5, 5);
			}
			if (southProbe) {
				int n = (int) (width * 0.05);
				g.setColor(model.getView().getBackground());
				for (int i = 0; i < n; i++)
					g.fillRect((int) (x + i * 20 + 10), (int) (y + height - 7), 5, 5);
				g.setColor(cbg);
				for (int i = 0; i < n; i++)
					g.drawRect((int) (x + i * 20 + 10), (int) (y + height - 7), 5, 5);
			}

			if (Math.abs(px) > Particle.ZERO || Math.abs(py) > Particle.ZERO)
				paintExternalForce(g);

			if (userField != null && UserField.isRenderable()) {
				boolean b = model.getMovie().getCurrentFrameIndex() >= model.getTapePointer() - 1;
				if (b)
					paintUserField(g);
				userField.render(g, this, b);
			}

		}

		if (selected && ((MDView) model.getView()).getShowSelectionHalo()) {
			if (((MDView) model.getView()).getAction() == UserAction.SELE_ID) {
				if (width > 11.0) {
					g.setColor(Color.green);
					g.fill(rectN);
					g.fill(rectS);
					g.setColor(cbg);
					g.draw(rectN);
					g.draw(rectS);
				}
				if (height > 11.0) {
					g.setColor(Color.green);
					g.fill(rectW);
					g.fill(rectE);
					g.setColor(cbg);
					g.draw(rectW);
					g.draw(rectE);
				}
				g.setColor(Color.green);
				g.fill(rectNW);
				g.fill(rectNE);
				g.fill(rectSW);
				g.fill(rectSE);
				g.setColor(cbg);
				g.draw(rectNW);
				g.draw(rectNE);
				g.draw(rectSW);
				g.draw(rectSE);
			}
			else {
				g.setColor(cbg);
				g.setStroke(ViewAttribute.THIN_DASHED);
				g.drawRect((int) (x - 2), (int) (y - 2), (int) (width + 4), (int) (height + 4));
			}
		}

		if (isBlinking()) {
			g.setColor(blinkColor);
			g.setStroke(ViewAttribute.DASHED);
			g.drawRect((int) (x - 5), (int) (y - 5), (int) (width + 10), (int) (height + 10));
		}

		if (((MDView) model.getView()).getShowParticleIndex()) {
			g.setColor(cbg);
			g.setFont(Particle.SANSSERIF);
			g.drawString("" + model.getObstacles().indexOf(this), (int) (x + width * 0.5), (int) (y + height * 0.5));
		}

	}

	private void paintExternalForce(Graphics2D g) {

		g.setColor(((MDView) model.getView()).contrastBackground());

		double place1 = x + width;
		double place2 = place1 + 8.0;
		double place3 = y + height;
		double place4 = place3 + 8.0;
		int nx = (int) (width / delta);
		int ny = (int) (height / delta);
		int i;
		double half;

		if (tempLine == null)
			tempLine = new Line2D.Double();
		g.setStroke(ViewAttribute.THIN);

		if (px < -Particle.ZERO) {
			for (i = 0; i < ny; i++) {
				half = (i + 0.5) * delta;
				/* draw arrows on the right side */
				tempLine.setLine(place1 + 4, y + half, place2 + 4, y + half);
				g.draw(tempLine);
				tempLine.setLine(place1 + 4, y + half, place1 + 6, y + half + 2);
				g.draw(tempLine);
				tempLine.setLine(place1 + 4, y + half, place1 + 6, y + half - 2);
				g.draw(tempLine);
			}
		}
		else if (px > Particle.ZERO) {
			for (i = 0; i < ny; i++) {
				half = (i + 0.5) * delta;
				/* draw arrows on the left side */
				tempLine.setLine(x - 12, y + half, x - 4, y + half);
				g.draw(tempLine);
				tempLine.setLine(x - 6, y + half + 2, x - 4, y + half);
				g.draw(tempLine);
				tempLine.setLine(x - 6, y + half - 2, x - 4, y + half);
				g.draw(tempLine);
			}
		}
		if (py < -Particle.ZERO) {
			for (i = 0; i < nx; i++) {
				half = (i + 0.5) * delta;
				/* draw arrows on the lower side */
				tempLine.setLine(x + half, place3 + 4, x + half, place4 + 4);
				g.draw(tempLine);
				tempLine.setLine(x + half, place3 + 4, x + half + 2, place3 + 6);
				g.draw(tempLine);
				tempLine.setLine(x + half, place3 + 4, x + half - 2, place3 + 6);
				g.draw(tempLine);
			}
		}
		else if (py > Particle.ZERO) {
			for (i = 0; i < nx; i++) {
				half = (i + 0.5) * delta;
				/* draw arrows on the northern side */
				tempLine.setLine(x + half, y - 12, x + half, y - 4);
				g.draw(tempLine);
				tempLine.setLine(x + half + 2, y - 6, x + half, y - 4);
				g.draw(tempLine);
				tempLine.setLine(x + half - 2, y - 6, x + half, y - 4);
				g.draw(tempLine);
			}
		}
	}

	private void paintUserField(Graphics2D g) {

		if (userField.getIntensity() <= 0.0)
			return;

		g.setColor(((MDView) model.getView()).contrastBackground());

		double place1 = x + width;
		double place2 = place1 + 8.0;
		double place3 = y + height;
		double place4 = place3 + 8.0;
		int nx = (int) (width / delta);
		int ny = (int) (height / delta);
		int i;
		double half;

		if (tempLine == null)
			tempLine = new Line2D.Double();
		g.setStroke(new BasicStroke((float) userField.getIntensity()));

		switch (userField.getOrientation()) {

		case UserField.WEST:
			for (i = 0; i < ny; i++) {
				half = (i + 0.5) * delta;
				/* draw arrows on the right side */
				tempLine.setLine(place1 + 4, y + half, place2 + 4, y + half);
				g.draw(tempLine);
				tempLine.setLine(place1 + 4, y + half, place1 + 6, y + half + 2);
				g.draw(tempLine);
				tempLine.setLine(place1 + 4, y + half, place1 + 6, y + half - 2);
				g.draw(tempLine);
			}
			break;
		case UserField.EAST:
			for (i = 0; i < ny; i++) {
				half = (i + 0.5) * delta;
				/* draw arrows on the left side */
				tempLine.setLine(x - 12, y + half, x - 4, y + half);
				g.draw(tempLine);
				tempLine.setLine(x - 6, y + half + 2, x - 4, y + half);
				g.draw(tempLine);
				tempLine.setLine(x - 6, y + half - 2, x - 4, y + half);
				g.draw(tempLine);
			}
			break;
		case UserField.NORTH:
			for (i = 0; i < nx; i++) {
				half = (i + 0.5) * delta;
				/* draw arrows on the lower side */
				tempLine.setLine(x + half, place3 + 4, x + half, place4 + 4);
				g.draw(tempLine);
				tempLine.setLine(x + half, place3 + 4, x + half + 2, place3 + 6);
				g.draw(tempLine);
				tempLine.setLine(x + half, place3 + 4, x + half - 2, place3 + 6);
				g.draw(tempLine);
			}
			break;
		case UserField.SOUTH:
			for (i = 0; i < nx; i++) {
				half = (i + 0.5) * delta;
				/* draw arrows on the northern side */
				tempLine.setLine(x + half, y - 12, x + half, y - 4);
				g.draw(tempLine);
				tempLine.setLine(x + half + 2, y - 6, x + half, y - 4);
				g.draw(tempLine);
				tempLine.setLine(x + half - 2, y - 6, x + half, y - 4);
				g.draw(tempLine);
			}
			break;
		}
	}

	public void initializeMovieQ(int n) {
		initializeRQ(n);
		initializeVQ(n);
		initializeAQ(n);
		initializePEQ(n);
		initializePWQ(n);
		initializePSQ(n);
		initializePNQ(n);
	}

	private FloatQueue initializePPQ(boolean b, int n, FloatQueue q, String s) {
		if (q == null) {
			if (n < 1)
				return null; // already null
			if (b) {
				q = new FloatQueue(s + ":" + toString(), n);
				q.setInterval(getMovieInterval());
				q.setPointer(0);
				q.setCoordinateQueue(model.getModelTimeQueue());
				model.getMovieQueueGroup().add(q);
			}
		}
		else {
			q.setLength(n);
			if (n < 1) {
				model.getMovieQueueGroup().remove(q);
				q = null;
			}
			else {
				q.setPointer(0);
			}
		}
		return q;
	}

	public void initializePEQ(int n) {
		if (peQ == null)
			peQ = new FloatQueue[5];
		for (int i = 0; i < 4; i++)
			peQ[i] = initializePPQ(eastProbe, n, peQ[i], Element.idToName(i) + " east pressure");
		peQ[4] = initializePPQ(eastProbe, n, peQ[4], "East pressure");
	}

	public void initializePWQ(int n) {
		if (pwQ == null)
			pwQ = new FloatQueue[5];
		for (int i = 0; i < 4; i++)
			pwQ[i] = initializePPQ(westProbe, n, pwQ[i], Element.idToName(i) + " west pressure");
		pwQ[4] = initializePPQ(westProbe, n, pwQ[4], "West pressure");
	}

	public void initializePNQ(int n) {
		if (pnQ == null)
			pnQ = new FloatQueue[5];
		for (int i = 0; i < 4; i++)
			pnQ[i] = initializePPQ(northProbe, n, pnQ[i], Element.idToName(i) + " north pressure");
		pnQ[4] = initializePPQ(northProbe, n, pnQ[4], "North pressure");
	}

	public void initializePSQ(int n) {
		if (psQ == null)
			psQ = new FloatQueue[5];
		for (int i = 0; i < 4; i++)
			psQ[i] = initializePPQ(southProbe, n, psQ[i], Element.idToName(i) + " south pressure");
		psQ[4] = initializePPQ(southProbe, n, psQ[4], "South pressure");
	}

	/**
	 * initialize coordinate queue twin. If the passed integer is less than 1, nullify the queues.
	 */
	public void initializeRQ(int n) {
		if (rxryQ == null) {
			if (n < 1)
				return; // already null
			rxryQ = new FloatQueueTwin(new FloatQueue("X: " + toString(), n), new FloatQueue("Y: " + toString(), n));
			rxryQ.setInterval(getMovieInterval());
			rxryQ.setPointer(0);
			rxryQ.setCoordinateQueue(model.getModelTimeQueue());
			model.getMovieQueueGroup().add(rxryQ);
		}
		else {
			rxryQ.setLength(n);
			if (n < 1) {
				model.getMovieQueueGroup().remove(rxryQ);
				rxryQ = null;
			}
			else {
				rxryQ.setPointer(0);
			}
		}
	}

	/**
	 * initialize velocity queue twin. If the passed integer is less than 1, nullify the array.
	 */
	public void initializeVQ(int n) {
		if (vxvyQ == null) {
			if (n < 1)
				return; // already null
			vxvyQ = new FloatQueueTwin(new FloatQueue("Vx: " + toString(), n), new FloatQueue("Vy: " + toString(), n));
			vxvyQ.setInterval(getMovieInterval());
			vxvyQ.setPointer(0);
			vxvyQ.setCoordinateQueue(model.getModelTimeQueue());
			model.getMovieQueueGroup().add(vxvyQ);
		}
		else {
			vxvyQ.setLength(n);
			if (n < 1) {
				model.getMovieQueueGroup().remove(vxvyQ);
				vxvyQ = null;
			}
			else {
				vxvyQ.setPointer(0);
			}
		}
	}

	/**
	 * initialize acceleration queue twin. If the passed integer is less than 1, nullify the array.
	 */
	public void initializeAQ(int n) {
		if (axayQ == null) {
			if (n < 1)
				return; // already null
			axayQ = new FloatQueueTwin(new FloatQueue("Ax: " + toString(), n), new FloatQueue("Ay: " + toString(), n));
			axayQ.setInterval(getMovieInterval());
			axayQ.setPointer(0);
			axayQ.setCoordinateQueue(model.getModelTimeQueue());
		}
		else {
			axayQ.setLength(n);
			if (n < 1) {
				axayQ = null;
			}
			else {
				axayQ.setPointer(0);
			}
		}
	}

	// 1666667 is the conversion factor from the internal unit to bar, see the header.
	private void updatePEQ() {
		int n = peBuffer[0].length;
		float x;
		float sum = 0;
		float constant = 1666667f / (n * getMovieInterval() * (float) (height * model.getTimeStep()));
		for (int k = 0; k < 5; k++) {
			x = 0;
			for (int i = 0; i < n - 1; i++) {
				peBuffer[k][i] = peBuffer[k][i + 1];
				x += peBuffer[k][i];
			}
			peBuffer[k][n - 1] = pEast[k];
			x += pEast[k];
			sum += x;
			if (k < 4) {
				peQ[k].update(constant * x);
			}
			else {
				peQ[k].update(constant * sum);
			}
		}
	}

	private void updatePWQ() {
		int n = pwBuffer[0].length;
		float x;
		float sum = 0;
		float constant = 1666667f / (n * getMovieInterval() * (float) (height * model.getTimeStep()));
		for (int k = 0; k < 5; k++) {
			x = 0;
			for (int i = 0; i < n - 1; i++) {
				pwBuffer[k][i] = pwBuffer[k][i + 1];
				x += pwBuffer[k][i];
			}
			pwBuffer[k][n - 1] = pWest[k];
			x += pWest[k];
			sum += x;
			if (k < 4) {
				pwQ[k].update(constant * x);
			}
			else {
				pwQ[k].update(constant * sum);
			}
		}
	}

	private void updatePSQ() {
		int n = psBuffer[0].length;
		float x;
		float sum = 0;
		float constant = 1666667f / (n * getMovieInterval() * (float) (width * model.getTimeStep()));
		for (int k = 0; k < 5; k++) {
			x = 0;
			for (int i = 0; i < n - 1; i++) {
				psBuffer[k][i] = psBuffer[k][i + 1];
				x += psBuffer[k][i];
			}
			psBuffer[k][n - 1] = pSouth[k];
			x += pSouth[k];
			sum += x;
			if (k < 4) {
				psQ[k].update(constant * x);
			}
			else {
				psQ[k].update(constant * sum);
			}
		}
	}

	private void updatePNQ() {
		int n = pnBuffer[0].length;
		float x;
		float sum = 0;
		float constant = 1666667f / (n * getMovieInterval() * (float) (width * model.getTimeStep()));
		for (int k = 0; k < 5; k++) {
			x = 0;
			for (int i = 0; i < n - 1; i++) {
				pnBuffer[k][i] = pnBuffer[k][i + 1];
				x += pnBuffer[k][i];
			}
			pnBuffer[k][n - 1] = pNorth[k];
			x += pNorth[k];
			sum += x;
			if (k < 4) {
				pnQ[k].update(constant * x);
			}
			else {
				pnQ[k].update(constant * sum);
			}
		}
	}

	public synchronized void updatePQ() {
		if (eastProbe && peQ != null && peQ[0] != null)
			updatePEQ();
		if (westProbe && pwQ != null && pwQ[0] != null)
			updatePWQ();
		if (southProbe && psQ != null && psQ[0] != null)
			updatePSQ();
		if (northProbe && pnQ != null && pnQ[0] != null)
			updatePNQ();
	}

	/** push current coordinate into the coordinate queue */
	public synchronized void updateRQ() {
		if (rxryQ == null || rxryQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		rxryQ.update((float) x, (float) y);
	}

	/** push current velocity into the velocity queue */
	public synchronized void updateVQ() {
		if (vxvyQ == null || vxvyQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		vxvyQ.update((float) vx, (float) vy);
	}

	/** push current acceleration into the acceleration queue */
	public synchronized void updateAQ() {
		if (axayQ == null || axayQ.isEmpty())
			throw new RuntimeException("Attempt to write to the empty queue");
		axayQ.update((float) ax, (float) ay);
	}

	public synchronized int getPPointer() {
		if (eastProbe && peQ != null && peQ[0] != null)
			return peQ[0].getPointer();
		if (westProbe && pwQ != null && pwQ[0] != null)
			return pwQ[0].getPointer();
		if (southProbe && psQ != null && psQ[0] != null)
			return psQ[0].getPointer();
		if (northProbe && pnQ != null && pnQ[0] != null)
			return pnQ[0].getPointer();
		return -1;
	}

	public synchronized void movePPointer(int i) {
		if (eastProbe && peQ != null) {
			for (int k = 0; k < 5; k++) {
				if (peQ[k] != null)
					peQ[k].setPointer(i);
			}
		}
		if (westProbe && pwQ != null) {
			for (int k = 0; k < 5; k++) {
				if (pwQ[k] != null)
					pwQ[k].setPointer(i);
			}
		}
		if (southProbe && psQ != null) {
			for (int k = 0; k < 5; k++) {
				if (psQ[k] != null)
					psQ[k].setPointer(i);
			}
		}
		if (northProbe && pnQ != null) {
			for (int k = 0; k < 5; k++) {
				if (pnQ[k] != null)
					pnQ[k].setPointer(i);
			}
		}
	}

	public synchronized int getRPointer() {
		if (rxryQ == null || rxryQ.isEmpty())
			return -1;
		return rxryQ.getPointer();
	}

	public synchronized void moveRPointer(int i) {
		if (rxryQ == null || rxryQ.isEmpty())
			return;
		rxryQ.setPointer(i);
	}

	public synchronized int getVPointer() {
		if (vxvyQ == null || vxvyQ.isEmpty())
			return -1;
		return vxvyQ.getPointer();
	}

	public synchronized void moveVPointer(int i) {
		if (vxvyQ == null || vxvyQ.isEmpty())
			return;
		vxvyQ.setPointer(i);
	}

	public synchronized int getAPointer() {
		if (axayQ == null || axayQ.isEmpty())
			return -1;
		return axayQ.getPointer();
	}

	public synchronized void moveAPointer(int i) {
		if (axayQ == null || axayQ.isEmpty())
			return;
		axayQ.setPointer(i);
	}

	private int getMovieInterval() {
		return model.movieUpdater.getInterval();
	}

	public FloatQueueTwin getRxRyQueue() {
		return rxryQ;
	}

	public FloatQueueTwin getVxVyQueue() {
		return vxvyQ;
	}

	public FloatQueueTwin getAxAyQueue() {
		return axayQ;
	}

	/** This is a serializable delegate of the rectangular obstacle. */
	public static class Delegate extends ComponentDelegate {

		private double x, y, width, height;
		private double vx, vy;
		private float px, py;
		private double density = HEAVY + HEAVY;
		private boolean bounced = true;
		private boolean visible = true;
		private boolean roundCornered;
		private float friction;
		private float elasticity = 1.0f;
		private FillMode fillMode;
		private boolean westProbe, eastProbe, southProbe, northProbe;
		private UserField userField;

		public Delegate() {
		}

		public Delegate(double x, double y, double width, double height, double vx, double vy, float px, float py,
				UserField userField, float elasticity, float friction, double density, boolean westProbe,
				boolean northProbe, boolean eastProbe, boolean southProbe, boolean bounced, boolean visible,
				boolean roundCornered, FillMode fillMode) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.fillMode = fillMode;
			this.vx = vx;
			this.vy = vy;
			this.px = px;
			this.py = py;
			this.userField = userField;
			this.elasticity = elasticity;
			this.friction = friction;
			this.bounced = bounced;
			this.visible = visible;
			this.roundCornered = roundCornered;
			this.density = density;
			this.westProbe = westProbe;
			this.eastProbe = eastProbe;
			this.southProbe = southProbe;
			this.northProbe = northProbe;
		}

		public void setWestProbe(boolean b) {
			westProbe = b;
		}

		public boolean isWestProbe() {
			return westProbe;
		}

		public void setEastProbe(boolean b) {
			eastProbe = b;
		}

		public boolean isEastProbe() {
			return eastProbe;
		}

		public void setSouthProbe(boolean b) {
			southProbe = b;
		}

		public boolean isSouthProbe() {
			return southProbe;
		}

		public void setNorthProbe(boolean b) {
			northProbe = b;
		}

		public boolean isNorthProbe() {
			return northProbe;
		}

		public void setFriction(float f) {
			friction = f;
		}

		public float getFriction() {
			return friction;
		}

		public void setElasticity(float f) {
			elasticity = f;
		}

		public float getElasticity() {
			return elasticity;
		}

		public void setRoundCornered(boolean b) {
			roundCornered = b;
		}

		public boolean isRoundCornered() {
			return roundCornered;
		}

		public void setVisible(boolean b) {
			visible = b;
		}

		public boolean isVisible() {
			return visible;
		}

		public void setBounced(boolean b) {
			bounced = b;
		}

		public boolean isBounced() {
			return bounced;
		}

		public void setX(double d) {
			x = d;
		}

		public double getX() {
			return x;
		}

		public void setY(double d) {
			y = d;
		}

		public double getY() {
			return y;
		}

		public void setWidth(double d) {
			width = d;
		}

		public double getWidth() {
			return width;
		}

		public void setHeight(double d) {
			height = d;
		}

		public double getHeight() {
			return height;
		}

		public void setVx(double d) {
			vx = d;
		}

		public double getVx() {
			return vx;
		}

		public void setVy(double d) {
			vy = d;
		}

		public double getVy() {
			return vy;
		}

		public void setExternalFx(float x) {
			px = x;
		}

		public float getExternalFx() {
			return px;
		}

		public void setExternalFy(float y) {
			py = y;
		}

		public float getExternalFy() {
			return py;
		}

		public void setDensity(double d) {
			density = d;
		}

		public double getDensity() {
			return density;
		}

		public void setFillMode(FillMode fm) {
			fillMode = fm;
		}

		public FillMode getFillMode() {
			return fillMode;
		}

		public UserField getUserField() {
			return userField;
		}

		public void setUserField(UserField uf) {
			userField = uf;
		}

	}

}