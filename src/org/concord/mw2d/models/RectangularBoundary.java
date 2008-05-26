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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Dimension2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.math.SimpleMath;
import org.concord.modeler.util.ObjectQueue;
import org.concord.mw2d.MDView;
import org.concord.mw2d.UserAction;

public class RectangularBoundary extends Rectangle2D.Double implements Boundary {

	/** XRYPBC stands for x-reflecting-y-periodic boundary conditions */
	public final static short XRYPBC_ID = 5564;

	/** XPYRBC stands for x-periodic-y-reflecting boundary conditions */
	public final static short XPYRBC_ID = 5565;

	public final static short UPPER_LEFT = 7861;
	public final static short LOWER_LEFT = 7862;
	public final static short UPPER_RIGHT = 7863;
	public final static short LOWER_RIGHT = 7864;

	int type = DBC_ID;
	List<Rectangle> mirrorBoxes;

	private static Stroke thickStroke = new BasicStroke(5.0f);
	private static Stroke thinStroke = new BasicStroke(1.0f);
	private static Stroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
			new float[] { 2.0f }, 0.0f);

	private static Rectangle2D tempRect;
	private static Line2D tempLine;

	private Rectangle2D.Double container, indent;
	private int selectedHandle = -1;
	private boolean borderAlwaysOn = true;
	private Rectangle2D rect1, rect2, rect3, rect4;
	private Wall wall;
	private boolean pistonOn;
	private boolean pistonXIsHeavy, pistonYIsHeavy;
	private float thicknessX = 0.5f, thicknessY = 0.5f;
	private double delta = 10.0;
	private boolean lightThrough = true;

	private boolean inelasticByRescaling;
	private double scaleFactor = 0.9;
	private int magnifier = 5;
	private transient boolean blinking;
	private boolean visible = true;

	private MDModel model;
	private ObjectQueue queue;
	private List<Integer> markedAtomList;

	public RectangularBoundary(MDModel model) {
		container = new Rectangle2D.Double();
		indent = new Rectangle2D.Double();
		wall = new Wall(model);
		this.model = model;
	}

	public RectangularBoundary(float x, float y, float w, float h, MDModel model) {
		super(x, y, w, h);
		container = new Rectangle2D.Double(x, y, w, h);
		indent = new Rectangle2D.Double(x + 6, y + 6, w - 12, h - 12);
		wall = new Wall(model);
		this.model = model;
	}

	/** TODO */
	public void storeCurrentState() {
	}

	/** TODO */
	public void restoreState() {
	}

	public void destroy() {
		model = null;
		queue = null;
		wall = null;
		mirrorBoxes.clear();
		mirrorBoxes = null;
	}

	/** TODO */
	public void blink() {
	}

	/** equivalent to <code>getX()</code> */
	public double getRx() {
		return getX();
	}

	/** equivalent to <code>getY()</code> */
	public double getRy() {
		return getY();
	}

	/** TODO */
	public void setSelected(boolean b) {
	}

	/** TODO */
	public boolean isSelected() {
		return false;
	}

	/** TODO */
	public void setMarked(boolean b) {
	}

	/** TODO */
	public boolean isMarked() {
		return false;
	}

	/** TODO */
	public void setBlinking(boolean b) {
		blinking = b;
	}

	/** TODO */
	public boolean isBlinking() {
		return blinking;
	}

	/** TODO */
	public synchronized void setVisible(boolean b) {
		visible = b;
	}

	/** TODO */
	public synchronized boolean isVisible() {
		return visible;
	}

	public void setModel(MDModel model) {
		this.model = model;
	}

	public MDModel getHostModel() {
		return model;
	}

	public void setLightThrough(boolean b) {
		lightThrough = b;
	}

	public boolean getLightThrough() {
		return lightThrough;
	}

	public ObjectQueue getQueue() {
		return queue;
	}

	public void setQueue(ObjectQueue q) {
		queue = q;
	}

	/**
	 * push the current boundary delegate into the queue, when the boundary is flexible like in the NPT simulation.
	 */
	public void updateQueue() {
		if (queue == null)
			return;
		if (queue.isEmpty())
			return;
		queue.update(createDelegate());
	}

	public void constructFromDelegate(Delegate dg) {
		setType(dg.getType());
		setFrame(dg.getX(), dg.getY(), dg.getWidth(), dg.getHeight());
		setBorderAlwaysOn(dg.getShowBorder());
		Dimension d = dg.getViewSize();
		setView(new Rectangle(0, 0, d.width, d.height));
	}

	public Delegate createDelegate() {
		return new Delegate(type, x, y, width, height, (int) container.width, (int) container.height, borderAlwaysOn);
	}

	public void setRect(double x, double y, double w, double h) {
		super.setRect(x, y, w, h);
		if (rect1 == null) {
			rect1 = new Rectangle2D.Double(x - 4, y - 4, 8, 8);
		}
		else {
			updateRect(rect1, x, y, container.width, container.height);
		}
		if (rect2 == null) {
			rect2 = new Rectangle2D.Double(x - 4 + w, y - 4, 8, 8);
		}
		else {
			updateRect(rect2, x, y + h, container.width, container.height);
		}
		if (rect3 == null) {
			rect3 = new Rectangle2D.Double(x - 4, y - 4 + h, 8, 8);
		}
		else {
			updateRect(rect3, x + w, y, container.width, container.height);
		}
		if (rect4 == null) {
			rect4 = new Rectangle2D.Double(x - 4 + w, y - 4 + h, 8, 8);
		}
		else {
			updateRect(rect4, x + w, y + h, container.width, container.height);
		}
		createMirrorBoxes();
	}

	public void setHandle(int i) {
		selectedHandle = i;
	}

	public int getHandle() {
		return selectedHandle;
	}

	/* override all setSize methods to guarantee same behaviors */
	public void setRect(Rectangle2D r) {
		setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	public void setFrame(double x, double y, double w, double h) {
		setRect(x, y, w, h);
	}

	public void setFrame(Rectangle2D r) {
		setRect(r);
	}

	public void setFrame(Point2D loc, Dimension2D size) {
		setRect(loc.getX(), loc.getY(), size.getWidth(), size.getHeight());
	}

	public void setFrameFromCenter(double centerX, double centerY, double cornerX, double cornerY) {
		double x = cornerX < centerX ? cornerX : centerX + centerX - cornerX;
		double y = cornerY < centerY ? cornerY : centerY + centerY - cornerY;
		double w = Math.abs(centerX - cornerX);
		double h = Math.abs(centerY - cornerY);
		setRect(x, y, w, h);
	}

	public void setFrameFromCenter(Point2D center, Point2D corner) {
		setFrameFromCenter(center.getX(), center.getY(), corner.getX(), corner.getY());
	}

	public void setFrameFromDiagonal(double x1, double y1, double x2, double y2) {
		double x = x1 > x2 ? x2 : x1;
		double y = y1 > y2 ? y2 : y1;
		double w = Math.abs(x1 - x2);
		double h = Math.abs(y1 - y2);
		setRect(x, y, w, h);
	}

	public void setFrameFromDiagonal(Point2D p1, Point2D p2) {
		setFrameFromDiagonal(p1.getX(), p1.getY(), p2.getX(), p2.getY());
	}

	public boolean contains(Rectangle2D r) {
		if (java.lang.Double.doubleToLongBits(r.getWidth()) == java.lang.Double.doubleToLongBits(0))
			return contains(r.getX(), r.getY()) && contains(r.getX(), r.getY() + r.getHeight());
		if (java.lang.Double.doubleToLongBits(r.getHeight()) == java.lang.Double.doubleToLongBits(0))
			return contains(r.getX(), r.getY()) && contains(r.getX() + r.getWidth(), r.getY());
		return super.contains(r);
	}

	public void setView(Shape s) {
		boolean b = false;
		if (s instanceof Rectangle2D) {
			container.setRect(((Rectangle2D) s).getFrame());
			b = true;
		}
		else if (s instanceof Rectangle) {
			container.setRect(((Rectangle) s).getBounds2D());
			b = true;
		}
		if (!b)
			return;
		indent.setRect(container.x + 6, container.y + 6, container.width - 12, container.height - 12);
		if (type == DBC_ID)
			setRect(container);
		createMirrorBoxes();
	}

	public Shape getView() {
		return container;
	}

	public Dimension getSize() {
		if (type == DBC_ID)
			return new Dimension((int) container.width, (int) container.height);
		return new Dimension((int) width, (int) height);
	}

	public int getType() {
		return type;
	}

	public void setType(int i) {
		if (type == DBC_ID)
			setRect(indent);
		type = i;
		createMirrorBoxes();
	}

	public void setWall(Wall wall) {
		this.wall = wall;
	}

	public Wall getWall() {
		return wall;
	}

	public void setPistonOn(boolean b) {
		pistonOn = b;
	}

	public boolean pistonIsOn() {
		return pistonOn;
	}

	public void setPistonXIsHeavy(boolean b) {
		pistonXIsHeavy = b;
	}

	public boolean pistonXIsHeavy() {
		return pistonXIsHeavy;
	}

	public void setPistonYIsHeavy(boolean b) {
		pistonYIsHeavy = b;
	}

	public boolean pistonYIsHeavy() {
		return pistonYIsHeavy;
	}

	public void setBorderAlwaysOn(boolean b) {
		borderAlwaysOn = b;
	}

	public boolean borderIsAlwaysOn() {
		return borderAlwaysOn;
	}

	public void setVisiblePressureInX(float fx) {
		thicknessX = fx;
	}

	public void setVisiblePressureInY(float fy) {
		thicknessY = fy;
	}

	public Rectangle2D getBoundRectUpperLeft() {
		return rect1;
	}

	public Rectangle2D getBoundRectLowerLeft() {
		return rect2;
	}

	public Rectangle2D getBoundRectUpperRight() {
		return rect3;
	}

	public Rectangle2D getBoundRectLowerRight() {
		return rect4;
	}

	/** return true if the boundary is periodic in at least one direction */
	public boolean isPeriodic() {
		return type == PBC_ID || type == XRYPBC_ID || type == XPYRBC_ID;
	}

	void createMirrorBoxes() {
		if (!isPeriodic()) {
			if (mirrorBoxes != null)
				mirrorBoxes.clear();
			return;
		}
		if (mirrorBoxes == null) {
			mirrorBoxes = Collections.synchronizedList(new ArrayList<Rectangle>());
		}
		else {
			mirrorBoxes.clear();
		}
		int xx, yy, m0, n0, i0, j0, x0, y0;
		switch (type) {
		case RectangularBoundary.XRYPBC_ID:
			j0 = (int) (container.height / height + 1);
			n0 = (int) (y / height + 1);
			y0 = (int) (y - n0 * height);
			for (int j = 0; j <= j0; j++) {
				yy = (int) (y0 + j * height);
				if (yy != (int) y)
					mirrorBoxes.add(new Rectangle((int) x, yy, (int) width, (int) height));
			}
			break;
		case RectangularBoundary.XPYRBC_ID:
			i0 = (int) (container.width / width + 1);
			m0 = (int) (x / width + 1);
			x0 = (int) (x - m0 * width);
			for (int i = 0; i <= i0; i++) {
				xx = (int) (x0 + i * width);
				if (xx != (int) x)
					mirrorBoxes.add(new Rectangle(xx, (int) y, (int) width, (int) height));
			}
			break;
		case RectangularBoundary.PBC_ID:
			i0 = (int) (container.width / width + 1);
			j0 = (int) (container.height / height + 1);
			m0 = (int) (x / width + 1);
			n0 = (int) (y / height + 1);
			x0 = (int) (x - m0 * width);
			y0 = (int) (y - n0 * height);
			for (int i = 0; i <= i0; i++) {
				for (int j = 0; j <= j0; j++) {
					xx = (int) (x0 + i * width);
					yy = (int) (y0 + j * height);
					if (xx != (int) x || yy != (int) y)
						mirrorBoxes.add(new Rectangle(xx, yy, (int) width, (int) height));
				}
			}
			break;
		}
	}

	/** create mirror images of atoms when applying periodic boundary conditions */
	public int createMirrorAtoms() {
		int nop = model.getNumberOfParticles();
		if (nop >= MolecularModel.getMaximumNumberOfAtoms())
			return nop;
		if (!isPeriodic())
			return nop;
		if (mirrorBoxes == null || mirrorBoxes.isEmpty())
			return nop;
		int npbc = nop;
		double xn = 0, yn = 0;
		Atom[] atom = ((MolecularModel) model).atom;
		synchronized (mirrorBoxes) {
			terminate: for (Rectangle mirror : mirrorBoxes) {
				for (int n = 0; n < nop; n++) {
					xn = mirror.x - x;
					yn = mirror.y - y;
					if (atom[n].rx + xn > -10 && atom[n].rx + xn < container.width + 10 && atom[n].ry + yn > -10
							&& atom[n].ry + yn < container.height + 10) {
						atom[npbc].set(atom[n]);
						atom[npbc].addRx(xn);
						atom[npbc].addRy(yn);
						npbc++;
						if (npbc >= atom.length - 1)
							break terminate;
					}
				}
			}
		}
		return npbc;
	}

	/**
	 * create mirror images of bonds when applying periodic boundary conditions. This should be called whenever there
	 * are bonds in the central box.
	 * 
	 * @return a list containing the mirror bond lines
	 */
	public List createMirrorBonds() {
		if (!(model instanceof MolecularModel))
			return null;
		if (mirrorBoxes == null || mirrorBoxes.isEmpty())
			return null;
		RadialBondCollection bonds = ((MolecularModel) model).bonds;
		List<Line2D.Float> lines = Collections.synchronizedList(new ArrayList<Line2D.Float>());
		List<Line2D> mirrors = Collections.synchronizedList(new ArrayList<Line2D>());
		RadialBond rBond;
		synchronized (bonds) {
			for (Iterator it = bonds.iterator(); it.hasNext();) {
				rBond = (RadialBond) it.next();
				lines.add(new Line2D.Float((float) rBond.getAtom1().rx, (float) rBond.getAtom1().ry, (float) rBond
						.getAtom2().rx, (float) rBond.getAtom2().ry));
			}
		}

		Rectangle rect;
		float dx, dy, x1, y1, x2, y2;
		int w = model.getView().getWidth();
		int h = model.getView().getHeight();
		synchronized (mirrorBoxes) {
			for (Iterator i = mirrorBoxes.iterator(); i.hasNext();) {
				rect = (Rectangle) i.next();
				dx = (float) (rect.x - model.boundary.x);
				dy = (float) (rect.y - model.boundary.y);
				synchronized (lines) {
					for (Line2D.Float l2d : lines) {
						x1 = l2d.x1 + dx;
						y1 = l2d.y1 + dy;
						x2 = l2d.x2 + dx;
						y2 = l2d.y2 + dy;
						if (Math.min(x1, x2) > 0 && Math.max(x1, x2) < w && Math.min(y1, y2) > 0
								&& Math.max(y1, y2) < h) {
							mirrors.add(new Line2D.Float(x1, y1, x2, y2));
						}
						else if (Line2D.linesIntersect(x1, y1, x2, y2, 0, 0, 0, h)
								|| Line2D.linesIntersect(x1, y1, x2, y2, w, 0, w, h)
								|| Line2D.linesIntersect(x1, y1, x2, y2, 0, 0, w, 0)
								|| Line2D.linesIntersect(x1, y1, x2, y2, 0, h, w, h)) {
							mirrors.add(new Line2D.Float(x1, y1, x2, y2));
						}
					}
				}
			}
		}
		return mirrors;
	}

	public void setInelasticByRescaling(boolean value) {
		inelasticByRescaling = value;
	}

	public boolean getInelasticByRescaling() {
		return inelasticByRescaling;
	}

	public void setScaleFactor(double x) {
		scaleFactor = x;
	}

	public double getScaleFactor() {
		return scaleFactor;
	}

	/** apply the Periodic Boundary Condition. */
	public void setPBC() {
		int n = model.getNumberOfParticles();
		if (n <= 0)
			return;
		double x1 = x + width;
		double y1 = y + height;
		if (model instanceof AtomicModel) {
			AtomicModel am = (AtomicModel) model;
			Atom at;
			for (int i = 0; i < n; i++) {
				at = am.atom[i];
				if (at.isBonded())
					continue;
				if (at.rx < x)
					at.rx += width;
				else if (at.rx > x1)
					at.rx -= width;
				if (at.ry < y)
					at.ry += height;
				else if (at.ry > y1)
					at.ry -= height;
			}
			List<Photon> photonList = am.getPhotons();
			if (photonList != null && !photonList.isEmpty()) {
				synchronized (photonList) {
					for (Photon photon : photonList) {
						if (photon.x < x)
							photon.x += width;
						else if (photon.x > x1)
							photon.x -= width;
						if (photon.y < y)
							photon.y += height;
						else if (photon.y > y1)
							photon.y -= height;
					}
				}
			}
		}
		else if (model instanceof MesoModel) {
			MesoModel mm = (MesoModel) model;
			GayBerneParticle gb;
			for (int i = 0; i < n; i++) {
				gb = mm.gb[i];
				if (gb.rx < x)
					gb.rx += width;
				else if (gb.rx >= x1)
					gb.rx -= width;
				if (gb.ry < y)
					gb.ry += height;
				else if (gb.ry >= y1)
					gb.ry -= height;
			}
		}
	}

	/**
	 * apply reflectory boundary conditions to all atoms. This method cares about the visual atom-size effect. As soon
	 * as the edge of a ball (not the center of mass) hits the wall it is bounced back.
	 */
	public void setRBC() {

		double xmin, ymin, xmax, ymax;
		if (type != DBC_ID) {
			xmin = x;
			ymin = y;
			xmax = x + width;
			ymax = y + height;
		}
		else {
			xmin = container.x;
			ymin = container.y;
			xmax = container.x + container.width;
			ymax = container.y + container.height;
		}

		if (!model.getObstacles().isEmpty()) {
			RectangularObstacle rect = null;
			synchronized (model.getObstacles().getSynchronizationLock()) {
				for (int iobs = 0, nobs = model.getObstacles().size(); iobs < nobs; iobs++) {
					rect = model.getObstacles().get(iobs);
					if (rect.x < xmin) {
						rect.x = xmin;
						rect.vx = rect.isBounced() ? Math.abs(rect.vx) : 0;
						rect.ax = 0;
					}
					else if (rect.x + rect.width > xmax) {
						rect.x = xmax - rect.width;
						rect.vx = rect.isBounced() ? -Math.abs(rect.vx) : 0;
						rect.ax = 0;
					}
					if (rect.y < ymin) {
						rect.y = ymin;
						rect.vy = rect.isBounced() ? Math.abs(rect.vy) : 0;
						rect.ay = 0;
					}
					else if (rect.y + rect.height > ymax) {
						rect.y = ymax - rect.height;
						rect.vy = rect.isBounced() ? -Math.abs(rect.vy) : 0;
						rect.ay = 0;
					}
				}
			}
		}

		if (model instanceof AtomicModel) {

			AtomicModel am = (AtomicModel) model;

			boolean isWestWallSink = wall.isSink(Wall.WEST);
			boolean isEastWallSink = wall.isSink(Wall.EAST);
			boolean isSouthWallSink = wall.isSink(Wall.SOUTH);
			boolean isNorthWallSink = wall.isSink(Wall.NORTH);

			Atom at;
			boolean toSink;
			double radius;
			for (int i = 0, nat = am.getNumberOfAtoms(); i < nat; i++) {
				at = am.atom[i];
				toSink = wall.toSink((byte) at.getID());
				radius = 0.5 * at.sigma;
				if (!isWestWallSink || (isWestWallSink && !toSink)) {
					if (at.rx < xmin + radius) {
						// at.rx=xmin+radius; //do not use this: it causes potential energy shift
						if (wall.isElastic(Wall.WEST)) {
							at.vx = Math.abs(at.vx);
						}
						else {
							if (!inelasticByRescaling) {
								double theta = Math.random() * Math.PI * 2.0;
								at.vx = -SimpleMath.sign(at.vx) * Math.sqrt(wall.getTemperature(Wall.WEST))
										* AtomicModel.VT_CONVERSION_CONSTANT * magnifier * Math.cos(theta);
								at.vy = Math.sqrt(wall.getTemperature(Wall.WEST)) * AtomicModel.VT_CONVERSION_CONSTANT
										* magnifier * Math.sin(theta);
							}
							else {
								at.vx = scaleFactor * Math.abs(at.vx);
							}
						}
					}
				}
				if (!isEastWallSink || (isEastWallSink && !toSink)) {
					if (at.rx > xmax - radius) {
						// at.rx=xmax-radius;
						if (wall.isElastic(Wall.EAST)) {
							at.vx = -Math.abs(at.vx);
						}
						else {
							if (!inelasticByRescaling) {
								double theta = Math.random() * Math.PI * 2.0;
								at.vx = -SimpleMath.sign(at.vx) * Math.sqrt(wall.getTemperature(Wall.EAST))
										* AtomicModel.VT_CONVERSION_CONSTANT * magnifier * Math.cos(theta);
								at.vy = Math.sqrt(wall.getTemperature(Wall.EAST)) * AtomicModel.VT_CONVERSION_CONSTANT
										* magnifier * Math.sin(theta);
							}
							else {
								at.vx = -scaleFactor * Math.abs(at.vx);
							}
						}
					}
				}
				if (!isNorthWallSink || (isNorthWallSink && !toSink)) {
					if (at.ry < ymin + radius) {
						// at.ry=ymin+radius;
						if (wall.isElastic(Wall.NORTH)) {
							at.vy = Math.abs(at.vy);
						}
						else {
							if (!inelasticByRescaling) {
								double theta = Math.random() * Math.PI * 2.0;
								at.vy = -SimpleMath.sign(at.vy) * Math.sqrt(wall.getTemperature(Wall.NORTH))
										* AtomicModel.VT_CONVERSION_CONSTANT * magnifier * Math.sin(theta);
								at.vx = Math.sqrt(wall.getTemperature(Wall.NORTH)) * AtomicModel.VT_CONVERSION_CONSTANT
										* magnifier * Math.cos(theta);
							}
							else {
								at.vy = scaleFactor * Math.abs(at.vy);
							}
						}
					}
				}
				if (!isSouthWallSink || (isSouthWallSink && !toSink)) {
					if (at.ry > ymax - radius) {
						// at.ry=ymax-radius;
						if (wall.isElastic(Wall.SOUTH)) {
							at.vy = -Math.abs(at.vy);
						}
						else {
							if (!inelasticByRescaling) {
								double theta = Math.random() * Math.PI * 2.0;
								at.vy = -SimpleMath.sign(at.vy) * Math.sqrt(wall.getTemperature(Wall.SOUTH))
										* AtomicModel.VT_CONVERSION_CONSTANT * magnifier * Math.sin(theta);
								at.vx = Math.sqrt(wall.getTemperature(Wall.SOUTH)) * AtomicModel.VT_CONVERSION_CONSTANT
										* magnifier * Math.cos(theta);
							}
							else {
								at.vy = -scaleFactor * Math.abs(at.vy);
							}
						}
					}
				}
			}

			// sink simulation
			if (model.getJob().getIndexOfStep() % 10 == 0) {
				if (markedAtomList == null)
					markedAtomList = new ArrayList<Integer>();
				else markedAtomList.clear();
				for (int i = 0, nat = am.getNumberOfAtoms(); i < nat; i++) {
					at = am.atom[i];
					radius = 0.5 * at.sigma;
					if (!wall.toSink((byte) at.getID()))
						continue;
					if (isWestWallSink && at.rx < xmin - radius) {
						markedAtomList.add(i);
					}
					if (isEastWallSink && at.rx > xmax + radius) {
						markedAtomList.add(i);
					}
					if (isNorthWallSink && at.ry < ymin - radius) {
						markedAtomList.add(i);
					}
					if (isSouthWallSink && at.ry > ymax + radius) {
						markedAtomList.add(i);
					}
				}
				if (!markedAtomList.isEmpty())
					am.view.removeMarkedAtoms(markedAtomList);
			}

			List<Photon> photonList = am.getPhotons();
			if (photonList != null && !photonList.isEmpty()) {
				if (lightThrough) {
					xmin -= 50;
					xmax += 50;
					ymin -= 50;
					ymax += 50;
					Photon photon;
					synchronized (photonList) {
						for (Iterator it = photonList.iterator(); it.hasNext();) {
							photon = (Photon) it.next();
							if (photon.x < xmin || photon.x > xmax || photon.y < ymin || photon.y > ymax) {
								it.remove();
								if (!photon.isFromLightSource())
									am.notifyModelListeners(new ModelEvent(am, "Photon emitted", null, photon));
							}
						}
					}
				}
				else {
					synchronized (photonList) {
						for (Photon photon : photonList) {
							if (photon.x < xmin) {
								photon.x = (float) xmin;
								photon.setAngle(photon.getAngle() + (float) Math.PI);
							}
							else if (photon.x > xmax) {
								photon.x = (float) xmax;
								photon.setAngle(photon.getAngle() + (float) Math.PI);
							}
							if (photon.y < ymin) {
								photon.y = (float) ymin;
								photon.setAngle(-photon.getAngle());
							}
							else if (photon.y > ymax) {
								photon.y = (float) ymax;
								photon.setAngle(-photon.getAngle());
							}
						}
					}
				}
			}
		}

		else if (model instanceof MesoModel) {
			MesoModel mm = (MesoModel) model;
			int n = mm.getNumberOfParticles();
			if (n <= 0)
				return;
			double sinTheta, cosTheta;
			double dx, dy;
			GayBerneParticle gb;
			for (int i = 0; i < n; i++) {
				gb = mm.gb[i];
				sinTheta = Math.sin(gb.theta);
				cosTheta = Math.cos(gb.theta);
				dx = gb.breadth * gb.breadth * sinTheta * sinTheta + gb.length * gb.length * cosTheta * cosTheta;
				dx = Math.sqrt(dx) * 0.5;
				dy = gb.breadth * gb.breadth * cosTheta * cosTheta + gb.length * gb.length * sinTheta * sinTheta;
				dy = Math.sqrt(dy) * 0.5;
				if (gb.rx < x + dx) {
					// gb.rx=x+dx;
					gb.vx = -gb.vx;
					gb.omega = -gb.omega;
				}
				else if (gb.rx >= xmax - dx) {
					// gb.rx=xmax-dx;
					gb.vx = -gb.vx;
					gb.omega = -gb.omega;
				}
				if (gb.ry < y + dy) {
					// gb.ry=y+dy;
					gb.vy = -gb.vy;
					gb.omega = -gb.omega;
				}
				else if (gb.ry >= ymax - dy) {
					// gb.ry=ymax-dy;
					gb.vy = -gb.vy;
					gb.omega = -gb.omega;
				}
			}
		}
	}

	public void setRBC(Atom atom) {
		double xmin, ymin, xmax, ymax;
		if (type != DBC_ID) {
			xmin = x;
			ymin = y;
			xmax = x + width;
			ymax = y + height;
		}
		else {
			xmin = container.x;
			ymin = container.y;
			xmax = container.x + container.width;
			ymax = container.y + container.height;
		}
		double radius = 0.5 * atom.sigma;
		if (atom.rx < xmin + radius)
			atom.rx = xmin + radius;
		else if (atom.rx >= xmax - radius)
			atom.rx = xmax - radius;
		if (atom.ry < ymin + radius)
			atom.ry = ymin + radius;
		else if (atom.ry >= ymax - radius)
			atom.ry = ymax - radius;
	}

	public void setRBC(Molecule mol) {
		if (mol == null || mol.size() == 0)
			return;
		int x0 = (int) getMinX();
		int y0 = (int) getMinY();
		int x1 = (int) getMaxX();
		int y1 = (int) getMaxY();
		Atom i_N = null, i_S = null, i_W = null, i_E = null;
		int xmax = 0, xmin = 2000, ymax = 0, ymin = 2000;
		synchronized (mol) {
			for (Atom atom : mol.atoms) {
				if (atom.rx > xmax) {
					xmax = (int) atom.rx;
					i_E = atom;
				}
				else if (atom.rx < xmin) {
					xmin = (int) atom.rx;
					i_W = atom;
				}
				if (atom.ry > ymax) {
					ymax = (int) atom.ry;
					i_S = atom;
				}
				else if (atom.ry < ymin) {
					ymin = (int) atom.ry;
					i_N = atom;
				}
			}
			if (i_W != null) {
				if (xmin < x0 + 0.5 * i_W.sigma) {
					for (Atom atom : mol.atoms) {
						atom.rx += (x0 + 0.5 * i_W.sigma - xmin);
					}
				}
			}
			if (i_E != null) {
				if (xmax > x1 - 0.5 * i_E.sigma) {
					for (Atom atom : mol.atoms) {
						atom.rx -= (xmax - x1 + 0.5 * i_E.sigma);
					}
				}
			}
			if (i_N != null) {
				if (ymin < y0 + 0.5 * i_N.sigma) {
					for (Atom atom : mol.atoms) {
						atom.ry += (y0 + 0.5 * i_N.sigma - ymin);
					}
				}
			}
			if (i_S != null) {
				if (ymax > y1 - 0.5 * i_S.sigma) {
					for (Atom atom : mol.atoms) {
						atom.ry -= (ymax - y1 + 0.5 * i_S.sigma);
					}
				}
			}
		}
	}

	/** apply reflectory boundary conditions in x-axis and periodic boundary conditions in y-axis. */
	public void setXRYPBC() {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel mm = (MolecularModel) model;
		double xmin = x;
		double xmax = x + width;
		if (!mm.getObstacles().isEmpty()) {
			RectangularObstacle rect = null;
			synchronized (mm.getObstacles().getSynchronizationLock()) {
				for (int iobs = 0, nobs = mm.getObstacles().size(); iobs < nobs; iobs++) {
					rect = mm.getObstacles().get(iobs);
					if (rect.x < xmin) {
						rect.x = xmin;
						rect.vx = rect.isBounced() ? Math.abs(rect.vx) : 0;
					}
					else if (rect.x + rect.width > xmax) {
						rect.x = xmax - rect.width;
						rect.vx = rect.isBounced() ? -Math.abs(rect.vx) : 0;
					}
				}
			}
		}
		Atom at = null;
		double radius;
		for (int i = 0, nat = mm.getNumberOfAtoms(); i < nat; i++) {
			at = mm.atom[i];
			radius = 0.5 * at.sigma;
			if (at.rx < xmin + radius) {
				if (wall.isElastic(Wall.WEST)) {
					at.vx = Math.abs(at.vx);
				}
				else {
					double theta = Math.random() * Math.PI * 2.0;
					at.vx = Math.sqrt(wall.getTemperature(Wall.WEST)) * AtomicModel.VT_CONVERSION_CONSTANT * magnifier
							* Math.cos(theta);
					at.vy = Math.sqrt(wall.getTemperature(Wall.WEST)) * AtomicModel.VT_CONVERSION_CONSTANT * magnifier
							* Math.sin(theta);
				}
			}
			else if (at.rx > xmax - radius) {
				if (wall.isElastic(Wall.EAST)) {
					at.vx = -Math.abs(at.vx);
				}
				else {
					double theta = Math.random() * Math.PI * 2.0;
					at.vx = Math.sqrt(wall.getTemperature(Wall.EAST)) * AtomicModel.VT_CONVERSION_CONSTANT * magnifier
							* Math.cos(theta);
					at.vy = Math.sqrt(wall.getTemperature(Wall.EAST)) * AtomicModel.VT_CONVERSION_CONSTANT * magnifier
							* Math.sin(theta);
				}
			}
			if (at.isBonded())
				continue;
			if (at.ry < y)
				at.ry += height;
			else if (at.ry > y + height)
				at.ry -= height;
		}
		List<Photon> photonList = mm.getPhotons();
		if (photonList != null && !photonList.isEmpty()) {
			synchronized (photonList) {
				if (lightThrough) {
					xmin -= 50;
					xmax += 50;
					Photon photon;
					synchronized (photonList) {
						for (Iterator it = photonList.iterator(); it.hasNext();) {
							photon = (Photon) it.next();
							if (photon.x < xmin || photon.x > xmax) {
								it.remove();
								if (!photon.isFromLightSource())
									mm.notifyModelListeners(new ModelEvent(mm, "Photon emitted", null, photon));
							}
						}
					}
				}
				else {
					for (Photon photon : photonList) {
						if (photon.x < xmin) {
							photon.x = (float) xmin;
							photon.setAngle(photon.getAngle() + (float) Math.PI);
						}
						else if (photon.x > xmax) {
							photon.x = (float) xmax;
							photon.setAngle(photon.getAngle() + (float) Math.PI);
						}
					}
				}
				for (Photon photon : photonList) {
					if (photon.y < y)
						photon.y += height;
					else if (photon.y > y + height)
						photon.y -= height;
				}
			}
		}
	}

	/** apply periodic boundary conditions in x-axis and reflectory boundary conditions in y-axis. */
	public void setXPYRBC() {
		if (!(model instanceof MolecularModel))
			return;
		MolecularModel mm = (MolecularModel) model;
		double ymin = y;
		double ymax = y + height;
		if (!mm.getObstacles().isEmpty()) {
			RectangularObstacle rect = null;
			synchronized (mm.getObstacles().getSynchronizationLock()) {
				for (int iobs = 0, nobs = mm.getObstacles().size(); iobs < nobs; iobs++) {
					rect = mm.getObstacles().get(iobs);
					if (rect.y < ymin) {
						rect.y = ymin;
						rect.vy = rect.isBounced() ? Math.abs(rect.vy) : 0;
					}
					else if (rect.y + rect.height > ymax) {
						rect.y = ymax - rect.height;
						rect.vy = rect.isBounced() ? -Math.abs(rect.vy) : 0;
					}
				}
			}
		}
		Atom at = null;
		double radius;
		for (int i = 0, nat = mm.getNumberOfAtoms(); i < nat; i++) {
			at = mm.atom[i];
			radius = 0.5 * at.sigma;
			if (at.ry < ymin + radius) {
				if (wall.isElastic(Wall.NORTH)) {
					at.vy = Math.abs(at.vy);
				}
				else {
					double theta = Math.random() * Math.PI * 2.0;
					at.vy = Math.sqrt(wall.getTemperature(Wall.NORTH)) * AtomicModel.VT_CONVERSION_CONSTANT * magnifier
							* Math.cos(theta);
					at.vx = Math.sqrt(wall.getTemperature(Wall.NORTH)) * AtomicModel.VT_CONVERSION_CONSTANT * magnifier
							* Math.sin(theta);
				}
			}
			else if (at.ry > ymax - radius) {
				if (wall.isElastic(Wall.SOUTH)) {
					at.vy = -Math.abs(at.vy);
				}
				else {
					double theta = Math.random() * Math.PI * 2.0;
					at.vy = Math.sqrt(wall.getTemperature(Wall.SOUTH)) * AtomicModel.VT_CONVERSION_CONSTANT * magnifier
							* Math.cos(theta);
					at.vx = Math.sqrt(wall.getTemperature(Wall.SOUTH)) * AtomicModel.VT_CONVERSION_CONSTANT * magnifier
							* Math.sin(theta);
				}
			}
			if (at.isBonded())
				continue;
			if (at.rx < x)
				at.rx += width;
			else if (at.rx > x + width)
				at.rx -= width;
		}
		List<Photon> photonList = mm.getPhotons();
		if (photonList != null && !photonList.isEmpty()) {
			synchronized (photonList) {
				if (lightThrough) {
					ymin -= 50;
					ymax += 50;
					Photon photon;
					for (Iterator it = photonList.iterator(); it.hasNext();) {
						photon = (Photon) it.next();
						if (photon.y < ymin || photon.y > ymax) {
							it.remove();
							if (!photon.isFromLightSource())
								mm.notifyModelListeners(new ModelEvent(mm, "Photon emitted", null, photon));
						}
					}
				}
				else {
					for (Photon photon : photonList) {
						if (photon.y < ymin) {
							photon.y = (float) ymin;
							photon.setAngle(-photon.getAngle());
						}
						else if (photon.y > ymax) {
							photon.y = (float) ymax;
							photon.setAngle(-photon.getAngle());
						}
					}
				}
				for (Photon photon : photonList) {
					if (photon.x < x)
						photon.x += width;
					else if (photon.x > x + width)
						photon.x -= width;
				}
			}
		}
	}

	void processBondCrossingUnderPBC() {
		if (!(model instanceof MolecularModel))
			return;
		MoleculeCollection molecules = ((MolecularModel) model).molecules;
		if (molecules.isEmpty())
			return;
		double x0 = getX();
		double y0 = getY();
		double dx = getWidth();
		double dy = getHeight();
		double x1 = x0 + dx;
		double y1 = y0 + dy;
		Molecule mol;
		Point2D p;
		double delta_x, delta_y;
		synchronized (molecules) {
			for (Iterator it = molecules.iterator(); it.hasNext();) {
				mol = (Molecule) it.next();
				delta_x = 0.0;
				delta_y = 0.0;
				p = mol.getCenterOfMass2D();
				if (p.getX() < x0)
					delta_x = dx;
				if (p.getX() > x1)
					delta_x = -dx;
				if (p.getY() < y0)
					delta_y = dy;
				if (p.getY() > y1)
					delta_y = -dy;
				if (Math.abs(delta_x) > 0.001 || Math.abs(delta_y) > 0.001)
					mol.translateBy(delta_x, delta_y);
			}
		}
	}

	void processBondCrossingUnderXRYPBC() {
		if (!(model instanceof MolecularModel))
			return;
		MoleculeCollection molecules = ((MolecularModel) model).molecules;
		if (molecules.isEmpty())
			return;
		double y0 = getY();
		double dy = getHeight();
		double y1 = y0 + dy;
		Molecule mol;
		Point2D p;
		double delta_y;
		synchronized (molecules) {
			for (Iterator it = molecules.iterator(); it.hasNext();) {
				mol = (Molecule) it.next();
				delta_y = 0.0;
				p = mol.getCenterOfMass2D();
				if (p.getY() < y0)
					delta_y = dy;
				if (p.getY() > y1)
					delta_y = -dy;
				if (Math.abs(delta_y) > 0.001)
					mol.translateBy(0.0, delta_y);
			}
		}
	}

	void processBondCrossingUnderXPYRBC() {
		if (!(model instanceof MolecularModel))
			return;
		MoleculeCollection molecules = ((MolecularModel) model).molecules;
		if (molecules.isEmpty())
			return;
		double x0 = getX();
		double dx = getWidth();
		double x1 = x0 + dx;
		Molecule mol;
		Point2D p;
		double delta_x;
		synchronized (molecules) {
			for (Iterator it = molecules.iterator(); it.hasNext();) {
				mol = (Molecule) it.next();
				delta_x = 0.0;
				p = mol.getCenterOfMass2D();
				if (p.getX() < x0)
					delta_x = dx;
				if (p.getX() > x1)
					delta_x = -dx;
				if (Math.abs(delta_x) > 0.001)
					mol.translateBy(delta_x, 0.0);
			}
		}
	}

	/**
	 * @param gb
	 *            the particle in action
	 * @param action
	 *            the type of action, either TRANSLATION or ROTATION
	 */
	public void setRBC(GayBerneParticle gb, int action) {
		if (gb == null)
			return;
		if (action == TRANSLATION || action == ROTATION) {
			double sinTheta = Math.sin(gb.theta);
			double cosTheta = Math.cos(gb.theta);
			double dx = 0.5 * Math.sqrt(gb.breadth * gb.breadth * sinTheta * sinTheta + gb.length * gb.length
					* cosTheta * cosTheta);
			double dy = 0.5 * Math.sqrt(gb.breadth * gb.breadth * cosTheta * cosTheta + gb.length * gb.length
					* sinTheta * sinTheta);
			if (gb.rx < x + dx)
				gb.rx = x + dx;
			else if (gb.rx >= x + width - dx)
				gb.rx = x + width - dx;
			if (gb.ry < y + dy)
				gb.ry = y + dy;
			else if (gb.ry >= y + height - dy)
				gb.ry = y + height - dy;
		}
	}

	/** deal with boundary conditions for grid cells */
	public void paintGridMirrors(int w, int h, Graphics2D g2d) {

		if (type == DBC_ID || type == RBC_ID)
			return;
		if (mirrorBoxes == null)
			return;

		int dx, dy, x0, y0, x1, y1, x2, y2, lx, ly;
		synchronized (mirrorBoxes) {
			for (Rectangle rect : mirrorBoxes) {
				x0 = (int) x;
				y0 = (int) y;
				lx = (int) width;
				ly = (int) height;
				dx = (int) (rect.x - x);
				dy = (int) (rect.y - y);
				x1 = x0 + dx;
				y1 = y0 + dy;
				if (x1 < 0) {
					x0 -= x1;
					lx += x1;
				}
				if (y1 < 0) {
					y0 -= y1;
					ly += y1;
				}
				x2 = x1 + lx;
				y2 = y1 + ly;
				if (x2 > w) {
					lx -= x2 - w;
				}
				if (y2 > h) {
					ly -= y2 - h;
				}
				g2d.copyArea(x0, y0, lx, ly, dx, dy);
			}
		}

	}

	public void drawReservoir(Graphics2D g) {
		int w1 = (int) container.width;
		int h1 = (int) container.height;
		if (tempRect == null)
			tempRect = new Rectangle2D.Double();
		for (byte i = 0; i < 4; i++) {
			if (!wall.isElastic(i)) {
				double fraction = (double) wall.getTemperature(i) / Wall.MAX_WALL_TEMPERATURE;
				g.setColor(new Color((int) (fraction * 255.0), 0, (int) ((1.0 - fraction) * 255.0)));
				switch (i) {
				case Wall.NORTH:
					if (type == RBC_ID) {
						tempRect.setRect(x, 0, width, y);
						g.fill(tempRect);
					}
					else if (type == XPYRBC_ID) {
						tempRect.setRect(0, 0, w1, y);
						g.fill(tempRect);
					}
					break;
				case Wall.SOUTH:
					if (type == RBC_ID) {
						tempRect.setRect(x, y + height, width, h1 - y - height);
						g.fill(tempRect);
					}
					else if (type == XPYRBC_ID) {
						tempRect.setRect(0, y + height, w1, h1 - y - height);
						g.fill(tempRect);
					}
					break;
				case Wall.WEST:
					if (type == RBC_ID) {
						tempRect.setRect(0, y, x, height);
						g.fill(tempRect);
					}
					else if (type == XRYPBC_ID) {
						tempRect.setRect(0, 0, x, h1);
						g.fill(tempRect);
					}
					break;
				case Wall.EAST:
					if (type == RBC_ID) {
						tempRect.setRect(x + width, y, w1 - x - width, height);
						g.fill(tempRect);
					}
					else if (type == XRYPBC_ID) {
						tempRect.setRect(x + width, 0, w1 - x - width, h1);
						g.fill(tempRect);
					}
					break;
				}
				g.setColor(((MDView) model.getView()).contrastBackground());
				switch (i) {
				case Wall.NORTH:
					g.drawString("Tw=" + Integer.toString(wall.getTemperature(i)) + "K",
							(float) (x + width * 0.5 - 25), (float) (y - 5));
					break;
				case Wall.SOUTH:
					g.drawString("Tw=" + Integer.toString(wall.getTemperature(i)) + "K",
							(float) (x + width * 0.5 - 25), (float) (y + height + 15));
					break;
				case Wall.WEST:
					float string_x0 = (float) (x - 15.0);
					float string_y0 = (float) (y + height * 0.5 - 25);
					g.rotate(Math.PI * 0.5, string_x0, string_y0);
					g.drawString("Tw=" + Integer.toString(wall.getTemperature(i)) + "K", string_x0, string_y0);
					g.rotate(-Math.PI * 0.5, string_x0, string_y0);
					break;
				case Wall.EAST:
					string_x0 = (float) (x + width + 5.0);
					string_y0 = (float) (y + height * 0.5 - 25);
					g.rotate(Math.PI * 0.5, string_x0, string_y0);
					g.drawString("Tw=" + Integer.toString(wall.getTemperature(i)) + "K", string_x0, string_y0);
					g.rotate(-Math.PI * 0.5, string_x0, string_y0);
					break;
				}
			}
		}
	}

	public void render(Graphics2D g, int actionID) {

		if (model == null)
			return;

		if (tempRect == null)
			tempRect = new Rectangle2D.Double();
		if (tempLine == null)
			tempLine = new Line2D.Double();

		if (type == PBC_ID) {
			if (borderAlwaysOn) {
				g.setColor(((MDView) model.getView()).contrastBackground());
				g.setStroke(thickStroke);
				g.drawRect((int) x, (int) y, (int) width, (int) height);
				g.setStroke(dashed);
				synchronized (mirrorBoxes) {
					for (Rectangle r : mirrorBoxes)
						g.draw(r);
				}
			}
		}
		else if (type == RBC_ID) {
			g.setColor(model.getView().getBackground().darker());
			double bxmin = x;
			double bymin = y;
			double bxmax = x + width;
			double bymax = y + height;
			tempRect.setRect(0, 0, container.width, bymin);
			g.fill(tempRect);
			tempRect.setRect(0, bymax, container.width, container.height - bymax);
			g.fill(tempRect);
			tempRect.setRect(0, bymin, bxmin, height);
			g.fill(tempRect);
			tempRect.setRect(bxmax, bymin, container.width - bxmax, height);
			g.fill(tempRect);
			g.setColor(((MDView) model.getView()).contrastBackground());
			g.setStroke(thickStroke);
			g.drawRect((int) x, (int) y, (int) width, (int) height);
		}
		else if (type == XRYPBC_ID || type == XPYRBC_ID) {
			g.setColor(Color.gray);
			if (type == XRYPBC_ID) {
				tempRect.setRect(0, 0, x, container.height);
				g.fill(tempRect);
				tempRect.setRect(x + width, 0, container.width - (x + width), container.height);
				g.fill(tempRect);
			}
			else {
				tempRect.setRect(0, 0, container.width, y);
				g.fill(tempRect);
				tempRect.setRect(0, y + height, container.width, container.height - (y + height));
				g.fill(tempRect);
			}
			g.setColor(((MDView) model.getView()).contrastBackground());
			g.setStroke(thickStroke);
			if (borderAlwaysOn) {
				g.drawRect((int) x, (int) y, (int) width, (int) height);
				g.setStroke(dashed);
				synchronized (mirrorBoxes) {
					for (Rectangle r : mirrorBoxes)
						g.draw(r);
				}
			}
			else {
				if (type == XRYPBC_ID) {
					tempLine.setLine(x, 0, x, container.height);
					g.draw(tempLine);
					tempLine.setLine(x + width, 0, x + width, container.height);
					g.draw(tempLine);
				}
				else {
					tempLine.setLine(container.x, y, container.width, y);
					g.draw(tempLine);
					tempLine.setLine(container.x, y + height, container.width, y + height);
					g.draw(tempLine);
				}
			}
		}

		if (type != DBC_ID && borderAlwaysOn && actionID == UserAction.SBOU_ID) {
			g.setColor(Color.white);
			g.fill(rect1);
			g.fill(rect2);
			g.fill(rect3);
			g.fill(rect4);
			g.setStroke(thinStroke);
			g.setColor(((MDView) model.getView()).contrastBackground());
			g.draw(rect1);
			g.draw(rect2);
			g.draw(rect3);
			g.draw(rect4);
		}

		if (pistonOn)
			paintPiston(g);

	}

	private void paintPiston(Graphics2D g) {

		double place1 = x + width;
		double place2 = place1 + 8.0;
		double place3 = y + height;
		double place4 = place3 + 8.0;
		int nx = (int) (width / delta);
		int ny = (int) (height / delta);
		int i;

		g.setColor(Color.black);
		if (tempLine == null)
			tempLine = new Line2D.Double();

		if (!pistonXIsHeavy) {
			if (thicknessX > 100.0f * java.lang.Float.MIN_VALUE) {
				g.setStroke(new BasicStroke(thicknessX));
				double i2;
				for (i = 0; i < ny; i++) {
					i2 = (i + 0.5) * delta;
					/* draw arrows on the eastern side */
					tempLine.setLine(place1 + 4, y + i2, place2 + 4, y + i2);
					g.draw(tempLine);
					tempLine.setLine(place1 + 4, y + i2, place1 + 6, y + i2 + 2);
					g.draw(tempLine);
					tempLine.setLine(place1 + 4, y + i2, place1 + 6, y + i2 - 2);
					g.draw(tempLine);
					/* draw arrows on the western side */
					tempLine.setLine(x - 12, y + i2, x - 4, y + i2);
					g.draw(tempLine);
					tempLine.setLine(x - 6, y + i2 + 2, x - 4, y + i2);
					g.draw(tempLine);
					tempLine.setLine(x - 6, y + i2 - 2, x - 4, y + i2);
					g.draw(tempLine);
				}
			}
		}

		if (!pistonYIsHeavy) {
			if (thicknessY > 100.0f * java.lang.Float.MIN_VALUE) {
				g.setStroke(new BasicStroke(thicknessY));
				double i2;
				for (i = 0; i < nx; i++) {
					i2 = (i + 0.5) * delta;
					/* draw arrows on the southern side */
					tempLine.setLine(x + i2, place3 + 4, x + i2, place4 + 4);
					g.draw(tempLine);
					tempLine.setLine(x + i2, place3 + 4, x + i2 + 2, place3 + 6);
					g.draw(tempLine);
					tempLine.setLine(x + i2, place3 + 4, x + i2 - 2, place3 + 6);
					g.draw(tempLine);
					/* draw arrows on the northern side */
					tempLine.setLine(x + i2, y - 12, x + i2, y - 4);
					g.draw(tempLine);
					tempLine.setLine(x + i2 + 2, y - 6, x + i2, y - 4);
					g.draw(tempLine);
					tempLine.setLine(x + i2 - 2, y - 6, x + i2, y - 4);
					g.draw(tempLine);
				}
			}
		}
	}

	/* make sure the rect is not out of view */
	private void updateRect(Rectangle2D rect, double xnew, double ynew, double xmax, double ymax) {
		double rectX, rectY;
		if (xnew < 10.0) {
			rectX = 10.0;
		}
		else if (xnew > xmax - 10.0) {
			rectX = xmax - 10.0;
		}
		else {
			rectX = xnew;
		}
		if (ynew < 10.0) {
			rectY = 10.0;
		}
		else if (ynew > ymax - 10.0) {
			rectY = ymax - 10.0;
		}
		else {
			rectY = ynew;
		}
		rect.setRect(rectX - 4.0, rectY - 4.0, 8.0, 8.0);
	}

	/** This is a serializable delegate of the rectangular boundary. */
	public static class Delegate extends ComponentDelegate {

		private Dimension viewSize;
		private double x, y, width, height;
		private int type = DBC_ID;
		private boolean borderOn = true;

		public Delegate() {
		}

		public Delegate(int type, double x, double y, double width, double height, int viewX, int viewY,
				boolean borderOn) {
			this.type = type;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.borderOn = borderOn;
			viewSize = new Dimension(viewX, viewY);
		}

		public void setViewSize(Dimension d) {
			viewSize = d;
		}

		public Dimension getViewSize() {
			return viewSize;
		}

		public void setType(int i) {
			type = i;
		}

		public int getType() {
			return type;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getX() {
			return x;
		}

		public void setY(double y) {
			this.y = y;
		}

		public double getY() {
			return y;
		}

		public void setWidth(double w) {
			width = w;
		}

		public double getWidth() {
			return width;
		}

		public void setHeight(double h) {
			height = h;
		}

		public double getHeight() {
			return height;
		}

		public void setShowBorder(boolean b) {
			borderOn = b;
		}

		public boolean getShowBorder() {
			return borderOn;
		}

	}

}