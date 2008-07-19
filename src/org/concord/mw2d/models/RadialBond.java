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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Timer;

import org.concord.modeler.util.HashCodeUtil;
import org.concord.mw2d.ViewAttribute;

import static org.concord.mw2d.models.Particle.COS45;
import static org.concord.mw2d.models.Particle.SIN45;

public class RadialBond implements ModelComponent {

	public final static byte STANDARD_STICK_STYLE = 0x65;
	public final static byte LONG_SPRING_STYLE = 0x66;
	public final static byte SOLID_LINE_STYLE = 0x67;
	public final static byte GHOST_STYLE = 0x68;
	public final static byte UNICOLOR_STICK_STYLE = 0x69;
	public final static byte SHORT_SPRING_STYLE = 0x6a;

	public final static float PEPTIDE_BOND_LENGTH_PARAMETER = 0.6f;

	public final static byte TORQUE_AROUND_ATOM1 = 1;
	public final static byte TORQUE_AROUND_ATOM2 = 2;
	public final static byte TORQUE_AROUND_CENTER = 3;

	private final static Stroke STICK = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	private final static double ZERO = 0.000001;
	private final static double DISTANCE_AWAY_FROM_AXIS = 10.0;

	Atom atom1, atom2;
	double bondLength = 20, bondStrength = 0.2;
	float torque;
	byte torqueType;
	float amplitude;
	int period = 100;
	float phase;

	/*
	 * Chemical energy stored in this bond. This energy is always set positive, though in the total energy calculation
	 * it should be counted as negative. Note: The chemical energy is NOT the vibrational potential energy, though the
	 * criterion used to break the bond is: Chemical energy + vibrational potential enery > 0.
	 */
	double chemicalEnergy;

	private Color bondColor;
	private byte bondStyle = STANDARD_STICK_STYLE;
	private static Line2D.Double axis;
	private float stickWidth = 2;
	private boolean selected, blinking;
	private boolean marked;
	private boolean visible = true;
	private Rectangle hotspot;
	private MolecularModel model;
	private boolean smart;
	private boolean solid;
	private boolean closed;
	private static Line2D line;
	private static GeneralPath path;
	private static Rectangle[] rects;
	private static Color blinkColor;

	/**
	 * @param atom1
	 *            the first participant
	 * @param atom2
	 *            the second participant
	 * @param bondLength
	 *            the equilibrium length
	 * @throws java.lang.IllegalArgumentException
	 *             if the two input atoms are identical.
	 */
	public RadialBond(Atom atom1, Atom atom2, double bondLength) throws IllegalArgumentException {
		if (atom1 == atom2)
			throw new IllegalArgumentException("The two participants of a radial bond must not be identical!");
		this.atom1 = atom1;
		this.atom2 = atom2;
		this.bondLength = bondLength;
		axis = new Line2D.Double();
		setAxis();
	}

	/**
	 * @param atom1
	 *            the first participant
	 * @param atom2
	 *            the second participant
	 * @param bondLength
	 *            the equilibrium length
	 * @param bondStrength
	 *            the strength of the harmonical potential
	 * @throws java.lang.IllegalArgumentException
	 *             if the two input atoms are identical.
	 */
	public RadialBond(Atom atom1, Atom atom2, double bondLength, double bondStrength) throws IllegalArgumentException {
		if (atom1 == atom2)
			throw new IllegalArgumentException("The two participants of a radial bond must not be identical!");
		this.atom1 = atom1;
		this.atom2 = atom2;
		this.bondLength = bondLength;
		this.bondStrength = bondStrength;
		axis = new Line2D.Double();
		setAxis();
	}

	public RadialBond(Atom atom1, Atom atom2, double bondLength, double bondStrength, boolean smart, boolean solid,
			boolean closed, Color color) throws IllegalArgumentException {
		if (atom1 == atom2)
			throw new IllegalArgumentException("The two participants of a radial bond must not be identical!");
		this.atom1 = atom1;
		this.atom2 = atom2;
		this.bondLength = bondLength;
		this.bondStrength = bondStrength;
		this.smart = smart;
		this.solid = solid;
		this.closed = closed;
		this.bondColor = color;
		axis = new Line2D.Double();
		setAxis();
	}

	/**
	 * @param atom1
	 *            the first participant
	 * @param atom2
	 *            the second participant
	 * @param bondLength
	 *            the equilibrium length
	 * @param bondStrength
	 *            the strength of the harmonical potential
	 * @param chemicalEnergy
	 *            the chemical energy stored in this bond
	 * @throws java.lang.IllegalArgumentException
	 *             if the two input atoms are identical.
	 */
	public RadialBond(Atom atom1, Atom atom2, double bondLength, double bondStrength, double chemicalEnergy)
			throws IllegalArgumentException {
		this(atom1, atom2, bondLength, bondStrength);
		this.chemicalEnergy = chemicalEnergy;
	}

	RadialBond getCopy(Atom atom1, Atom atom2) {
		RadialBond b = new RadialBond(atom1, atom2, bondLength, bondStrength, smart, solid, closed, bondColor);
		b.setChemicalEnergy(chemicalEnergy);
		b.setVisible(visible);
		b.setBondStyle(bondStyle);
		b.setPhase(phase);
		b.setAmplitude(amplitude);
		b.setPeriod(period);
		b.setTorque(torque);
		b.setTorqueType(torqueType);
		model.view.copyAttachedLayeredComponents(this, b);
		return b;
	}

	/** TODO: currently this is not implemented */
	public void storeCurrentState() {
	}

	/** TODO: currently this is not implemented */
	public void restoreState() {
	}

	/** blink this bond */
	public void blink() {

		final Timer blinkTimer = new Timer(250, null);
		blinkTimer.setRepeats(true);
		blinkTimer.setInitialDelay(0);
		blinkTimer.start();
		setBlinking(true);

		blinkTimer.addActionListener(new ActionListener() {

			private int blinkIndex;

			public void actionPerformed(ActionEvent e) {
				if (blinkIndex < 8) {
					blinkIndex++;
					blinkColor = blinkIndex % 2 == 0 ? model.view.contrastBackground() : model.view.getBackground();
				}
				else {
					blinkTimer.stop();
					blinkIndex = 0;
					setBlinking(false);
				}
				model.view.repaint();
			}

		});

	}

	public void destroy() {
		atom1.setRadical(true);
		atom2.setRadical(true);
		atom1 = null;
		atom2 = null;
		model = null;
	}

	public int getIndex() {
		if (model.bonds == null)
			return -1;
		return model.bonds.indexOf(this);
	}

	/** return x-coordinate of the center of this bond */
	public double getRx() {
		return 0.5 * (atom1.getRx() + atom2.getRx());
	}

	/** return y-coordinate of the center of this bond */
	public double getRy() {
		return 0.5 * (atom1.getRy() + atom2.getRy());
	}

	/* return the angle in the range of (-90, 90) degrees. */
	double getAngle() {
		double dx = atom2.rx - atom1.rx;
		double dy = atom2.ry - atom1.ry;
		double t = dx / Math.hypot(dx, dy);
		return dy < 0 ? Math.PI - Math.acos(t) : Math.acos(t) - Math.PI;
	}

	public boolean equals(Object o) {
		if (!(o instanceof RadialBond))
			return false;
		return (atom1 == ((RadialBond) o).atom1 && atom2 == ((RadialBond) o).atom2)
				|| (atom2 == ((RadialBond) o).atom1 && atom1 == ((RadialBond) o).atom2);
	}

	public int hashCode() {
		return ((atom1.getIndex() & 0xFF) << 10) | ((atom2.getIndex() & 0xFF) << 0);
	}

	public Rectangle2D getBounds2D() {
		setAxis();
		return axis.getBounds2D();
	}

	public Point2D getP1() {
		return new Point2D.Double(atom1.rx, atom1.ry);
	}

	public Point2D getP2() {
		return new Point2D.Double(atom2.rx, atom2.ry);
	}

	public double getX1() {
		return atom1.rx;
	}

	public double getY1() {
		return atom1.ry;
	}

	public double getX2() {
		return atom2.rx;
	}

	public double getY2() {
		return atom2.ry;
	}

	/**
	 * @deprecated A covalent bond cannot be cloned, because it is a secondary structure depicting the relationship
	 *             between atoms. You can clone entities such as a particle or a molecule, but not the relationships
	 *             between entities.
	 */
	public Object clone() {
		throw new RuntimeException("Do not call this method");
	}

	/**
	 * return true if the coordinate is inside the hot spot of this bond. Note: This is different from the base class
	 * <tt>Line2D</tt>'s <tt>contains</tt> method's "always-return-false" behavior. Be aware of that this method
	 * can return true.
	 */
	public boolean contains(double x, double y) {
		setAxis();
		return hotspot.contains(x, y);
	}

	/**
	 * return true if the rectangular area is inside the hot spot of this bond. Note: This is different from the base
	 * class <tt>Line2D</tt>'s <tt>contains</tt> method's "always-return-false" behavior. Be aware of that this
	 * method can return true.
	 */
	public boolean contains(double x, double y, double w, double h) {
		setAxis();
		return hotspot.contains(x, y, w, h);
	}

	/**
	 * return true if the point is inside the hot spot of this bond. Note: This is different from the base class
	 * <tt>Line2D</tt>'s <tt>contains</tt> method's "always-return-false" behavior. Be aware of that this method
	 * can return true.
	 */
	public boolean contains(Point2D p) {
		setAxis();
		return hotspot.contains(p);
	}

	/**
	 * return true if the rectangle is inside the hot spot of this bond. Note: This is different from the base class
	 * <tt>Line2D</tt>'s <tt>contains</tt> method's "always-return-false" behavior. Be aware of that this method
	 * can return true.
	 */
	public boolean contains(Rectangle2D r) {
		setAxis();
		return hotspot.contains(r);
	}

	public Rectangle getBounds() {
		setAxis();
		return axis.getBounds();
	}

	public boolean intersects(double x, double y, double w, double h) {
		setAxis();
		return axis.intersects(x, y, w, h);
	}

	public boolean intersects(Rectangle2D r) {
		setAxis();
		return axis.intersects(r);
	}

	public boolean intersectsLine(double x1, double y1, double x2, double y2) {
		setAxis();
		return axis.intersectsLine(x1, y1, x2, y2);
	}

	/**
	 * <p>
	 * this method can be used to detect wether two covalent bonds intersect.
	 * </p>
	 * <p>
	 * The intersection of two bonds is chemically false, but occurs frequently in a model where bonds are not allowed
	 * to break and form but the bond lengths are much greater than the van der Waals radii of the atoms.
	 * </p>
	 */
	public boolean intersects(RadialBond rb) {
		setAxis();
		return axis.intersectsLine(RadialBond.axis);
	}

	/** @return the distance of a point to this bond's axis */
	public double ptLineDist(double px, double py) {
		setAxis();
		return axis.ptLineDist(px, py);
	}

	/** @return the distance of a point to this bond's axis */
	public double ptLineDist(Point2D p) {
		setAxis();
		return axis.ptLineDist(p);
	}

	/** @return the square of the distance of a point to this bond's axis */
	public double ptLineDistSq(double px, double py) {
		setAxis();
		return axis.ptLineDistSq(px, py);
	}

	/** @return the square of the distance of a point to this bond's axis */
	public double ptLineDistSq(Point2D p) {
		setAxis();
		return axis.ptLineDistSq(p);
	}

	/** @return the distance from a point to this bond */
	public double ptSegDist(double px, double py) {
		setAxis();
		return axis.ptSegDist(px, py);
	}

	/** @return the distance from a point to this bond */
	public double ptSegDis(Point2D p) {
		setAxis();
		return axis.ptSegDist(p);
	}

	/** @return the square of the distance from a point to this bond */
	public double ptSegDistSq(double px, double py) {
		setAxis();
		return axis.ptSegDistSq(px, py);
	}

	/** @return the square of the distance from a point to this bond */
	public double ptSegDistSq(Point2D p) {
		setAxis();
		return axis.ptSegDistSq(p);
	}

	/**
	 * @return an indicator of where the specified point lies with respect to this bond
	 */
	public int relativeCCW(double px, double py) {
		setAxis();
		return axis.relativeCCW(px, py);
	}

	/**
	 * @return an indicator of where the specified point lies with respect to this bond
	 */
	public int relativeCCW(Point2D p) {
		setAxis();
		return axis.relativeCCW(p);
	}

	public String toString() {
		return "Radial Bond#" + getIndex() + " [" + atom1 + ", " + atom2 + "]";
	}

	/** set the model this bond is associated with */
	public void setModel(MDModel m) {
		if (!(m instanceof MolecularModel))
			throw new IllegalArgumentException(m + ": must be a molecular model!");
		model = (MolecularModel) m;
	}

	public MDModel getHostModel() {
		return model;
	}

	public void setSelected(boolean b) {
		selected = b;
		if (b) {
			setRects();
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

	public synchronized void setVisible(boolean b) {
		visible = b;
	}

	public synchronized boolean isVisible() {
		return visible;
	}

	/** return the chemical energy stored in this bond * */
	public double getChemicalEnergy() {
		return chemicalEnergy;
	}

	/** set the chemical energy stored in this bond * */
	public void setChemicalEnergy(double d) {
		chemicalEnergy = d;
	}

	/**
	 * the area enclosed by the network of which this bond is a memember is solid or not
	 */
	public void setSolid(boolean b) {
		solid = b;
	}

	/**
	 * return true if the area enclosed by the network of which this bond is a memember is solid
	 */
	public boolean isSolid() {
		return solid;
	}

	/** if not solid, should the spline be closed? */
	public void setClosed(boolean b) {
		closed = b;
	}

	/** return true if the spline is closed */
	public boolean isClosed() {
		return closed;
	}

	/** does this bond belong to a smart spline? */
	public void setSmart(boolean b) {
		smart = b;
	}

	/** return true if this bond belongs to a smart spline */
	public boolean isSmart() {
		return smart;
	}

	public void setTorque(float t) {
		torque = t;
	}

	public float getTorque() {
		return torque;
	}

	public void setTorqueType(byte i) {
		torqueType = i;
	}

	public byte getTorqueType() {
		return torqueType;
	}

	/** sets the amplitude of forced vibration */
	public void setAmplitude(float amplitude) {
		this.amplitude = amplitude;
	}

	public float getAmplitude() {
		return amplitude;
	}

	/** sets the period of forced vibration, in femtoseconds */
	public void setPeriod(int period) {
		this.period = period;
	}

	public int getPeriod() {
		return period;
	}

	/** sets the phase of forced vibration (in radians) */
	public void setPhase(float phase) {
		this.phase = phase;
	}

	public float getPhase() {
		return phase;
	}

	/**
	 * set the coordinates <tt>x1,y1,x2,y2</tt> to the atoms' latest locations and relocate the hotspot.
	 */
	private void setAxis() {
		axis.setLine(atom1.rx, atom1.ry, atom2.rx, atom2.ry);
		if (hotspot == null)
			hotspot = new Rectangle();
		int minx = (int) Math.min(atom1.rx, atom2.rx);
		int miny = (int) Math.min(atom1.ry, atom2.ry);
		int maxx = (int) Math.max(atom1.rx, atom2.rx);
		int maxy = (int) Math.max(atom1.ry, atom2.ry);
		if (Math.abs(minx - maxx) < 10) {
			minx -= 5;
			maxx += 5;
		}
		if (Math.abs(miny - maxy) < 10) {
			miny -= 5;
			maxy += 5;
		}
		hotspot.setRect(minx + 2, miny + 2, maxx - minx - 4, maxy - miny - 4);
	}

	void setRects() {
		setAxis();
		double cost = axis.x2 - axis.x1;
		double sint = axis.y2 - axis.y1;
		double inv = DISTANCE_AWAY_FROM_AXIS / Math.hypot(cost, sint);
		cost *= inv;
		sint *= inv;
		if (rects == null) {
			rects = new Rectangle[4];
			for (int i = 0; i < 4; i++)
				rects[i] = new Rectangle();
		}
		rects[0].setRect(axis.x1 - sint - 3, axis.y1 + cost - 3, 6, 6);
		rects[1].setRect(axis.x1 + sint - 3, axis.y1 - cost - 3, 6, 6);
		rects[2].setRect(axis.x2 - sint - 3, axis.y2 + cost - 3, 6, 6);
		rects[3].setRect(axis.x2 + sint - 3, axis.y2 - cost - 3, 6, 6);
	}

	public boolean contains(Atom atom) {
		return atom1.equals(atom) || atom2.equals(atom);
	}

	public Molecule getMolecule() {
		return model.molecules.getMolecule(atom1);
	}

	public Atom getAtom1() {
		return atom1;
	}

	public Atom getAtom2() {
		return atom2;
	}

	public void setAtom1(Atom atom) {
		atom1 = atom;
	}

	public void setAtom2(Atom atom) {
		atom2 = atom;
	}

	public double getLength(int frame) {
		if (frame < 0)
			return Math.sqrt(atom1.distanceSquare(atom2));
		double dx = atom1.rxryQ.getQueue1().getData(frame) - atom2.rxryQ.getQueue1().getData(frame);
		double dy = atom1.rxryQ.getQueue2().getData(frame) - atom2.rxryQ.getQueue2().getData(frame);
		return Math.hypot(dx, dy);
	}

	public double getBondLength() {
		return bondLength;
	}

	public void setBondLength(double d) {
		bondLength = d;
	}

	public double getBondStrength() {
		return bondStrength;
	}

	public void setBondStrength(double d) {
		bondStrength = d;
	}

	public void setBondColor(Color c) {
		bondColor = c;
	}

	public Color getBondColor() {
		return bondColor;
	}

	public void setBondStyle(byte style) {
		bondStyle = style;
	}

	public byte getBondStyle() {
		return bondStyle;
	}

	private void drawSpring(Graphics2D g, int n, int m) {
		g.setStroke(ViewAttribute.THIN);
		g.setColor(bondColor == null ? model.view.contrastBackground() : bondColor);
		double x = atom2.rx - atom1.rx;
		double y = atom2.ry - atom1.ry;
		double length = Math.hypot(x, y);
		double costheta = x / length;
		double sintheta = y / length;
		double delta = length / n;
		if (path == null)
			path = new GeneralPath();
		else path.reset();
		path.moveTo((float) atom1.rx, (float) atom1.ry);
		for (int i = 0; i < n; i++) {
			if (i % 2 == 0) {
				x = atom1.rx + (i + 0.5) * costheta * delta - 0.5 * sintheta * m;
				y = atom1.ry + (i + 0.5) * sintheta * delta + 0.5 * costheta * m;
			}
			else {
				x = atom1.rx + (i + 0.5) * costheta * delta + 0.5 * sintheta * m;
				y = atom1.ry + (i + 0.5) * sintheta * delta - 0.5 * costheta * m;
			}
			path.lineTo((float) x, (float) y);
		}
		path.lineTo((float) atom2.rx, (float) atom2.ry);
		g.draw(path);
		if (selected && model.view.getShowSelectionHalo()) {
			g.setStroke(ViewAttribute.THIN_DASHED);
			g.draw(axis);
		}
	}

	private void drawColorStick(Graphics2D g, byte type) {
		if (!model.view.getUseJmol()) {
			double cos = axis.x2 - axis.x1;
			double sin = axis.y2 - axis.y1;
			double d = Math.hypot(cos, sin);
			cos /= d;
			sin /= d;
			g.setStroke(STICK);
			if (type == STANDARD_STICK_STYLE) {
				double p = 0.5 * d + 0.25 * (atom1.sigma - atom2.sigma);
				double x = axis.x1 + cos * p;
				double y = axis.y1 + sin * p;
				g.setColor(atom1.getColor());
				line.setLine(axis.x1, axis.y1, x, y);
				g.draw(line);
				g.setColor(atom2.getColor());
				line.setLine(axis.x2, axis.y2, x, y);
				g.draw(line);
			}
			else if (type == UNICOLOR_STICK_STYLE) {
				g.setColor(bondColor == null ? model.view.contrastBackground() : bondColor);
				g.draw(axis);
			}
			g.setColor(model.view.contrastBackground());
			g.setStroke(ViewAttribute.THIN);
			cos *= stickWidth;
			sin *= stickWidth;
			line.setLine(axis.x2 - sin, axis.y2 + cos, axis.x1 - sin, axis.y1 + cos);
			g.draw(line);
			line.setLine(axis.x2 + sin, axis.y2 - cos, axis.x1 + sin, axis.y1 - cos);
			g.draw(line);
		}
		if (selected && model.view.getShowSelectionHalo()) {
			drawFlankLines(g, null, 2, null);
		}
	}

	private void drawLine(Graphics2D g) {
		g.setStroke(STICK);
		g.setColor(bondColor == null ? model.view.contrastBackground() : bondColor);
		g.draw(axis);
		drawGhost(g);
	}

	private void drawGhost(Graphics2D g) {
		if (selected && model.view.getShowSelectionHalo()) {
			drawFlankLines(g, null, 2, null);
		}
	}

	private void drawFlankLines(Graphics2D g, Color c, float ratio, Stroke stroke) {
		g.setColor(c != null ? c : model.view.contrastBackground());
		double cos = axis.x2 - axis.x1;
		double sin = axis.y2 - axis.y1;
		double d = ratio * stickWidth / Math.hypot(cos, sin);
		cos *= d;
		sin *= d;
		g.setStroke(stroke != null ? stroke : ViewAttribute.THIN_DASHED);
		line.setLine(axis.x2 - sin, axis.y2 + cos, axis.x1 - sin, axis.y1 + cos);
		g.draw(line);
		line.setLine(axis.x2 + sin, axis.y2 - cos, axis.x1 + sin, axis.y1 - cos);
		g.draw(line);
	}

	private void drawArrow(Graphics2D g, double cost, double sint) {
		double wingx = 5 * (cost * COS45 + sint * SIN45);
		double wingy = 5 * (sint * COS45 - cost * SIN45);
		g.drawLine((int) line.getX2(), (int) line.getY2(), (int) (line.getX2() - wingx), (int) (line.getY2() - wingy));
		wingx = 5 * (cost * COS45 - sint * SIN45);
		wingy = 5 * (sint * COS45 + cost * SIN45);
		g.drawLine((int) line.getX2(), (int) line.getY2(), (int) (line.getX2() + wingx), (int) (line.getY2() + wingy));
	}

	public void render(Graphics2D g) {
		if (model == null)
			return;
		if (visible) {
			setAxis();
			if (line == null)
				line = new Line2D.Double();
			switch (bondStyle) {
			case LONG_SPRING_STYLE:
				drawSpring(g, 20, 20);
				break;
			case SHORT_SPRING_STYLE:
				drawSpring(g, 10, 10);
				break;
			case SOLID_LINE_STYLE:
				drawLine(g);
				break;
			case GHOST_STYLE:
				drawGhost(g);
				break;
			case UNICOLOR_STICK_STYLE:
				drawColorStick(g, UNICOLOR_STICK_STYLE);
				break;
			default:
				drawColorStick(g, STANDARD_STICK_STYLE);
			}
		}
		if (selected && model.view.getShowSelectionHalo()) {
			switch (bondStyle) {
			case STANDARD_STICK_STYLE:
				g.setColor(Color.red);
				if (rects != null)
					for (Rectangle r : rects)
						g.fill(r);
				break;
			}
		}
		if (isBlinking()) {
			drawFlankLines(g, blinkColor, 4, ViewAttribute.THICKER_DASHED);
		}
		if (Math.abs(torque) > ZERO) {
			if (!visible)
				setAxis();
			if (line == null)
				line = new Line2D.Double();
			g.setColor(model.view.contrastBackground());
			g.setStroke(ViewAttribute.THIN);
			switch (torqueType) {
			case TORQUE_AROUND_ATOM1:
				double cost = axis.x2 - axis.x1;
				double sint = axis.y2 - axis.y1;
				double inv = 1.0 / Math.hypot(cost, sint);
				cost *= inv;
				sint *= inv;
				double length = 0.5 * atom2.sigma + 10;
				if (torque < 0) {
					line.setLine(axis.x2, axis.y2, axis.x2 + sint * length, axis.y2 - cost * length);
					drawArrow(g, cost, sint);
				}
				else {
					line.setLine(axis.x2, axis.y2, axis.x2 - sint * length, axis.y2 + cost * length);
					drawArrow(g, -cost, -sint);
				}
				g.draw(line);
				break;
			case TORQUE_AROUND_ATOM2:
				cost = axis.x1 - axis.x2;
				sint = axis.y1 - axis.y2;
				inv = 1.0 / Math.hypot(cost, sint);
				cost *= inv;
				sint *= inv;
				length = 0.5 * atom1.sigma + 10;
				if (torque < 0) {
					line.setLine(axis.x1, axis.y1, axis.x1 + sint * length, axis.y1 - cost * length);
					drawArrow(g, cost, sint);
				}
				else {
					line.setLine(axis.x1, axis.y1, axis.x1 - sint * length, axis.y1 + cost * length);
					drawArrow(g, -cost, -sint);
				}
				g.draw(line);
				break;
			case TORQUE_AROUND_CENTER:
				cost = axis.x1 - axis.x2;
				sint = axis.y1 - axis.y2;
				inv = 1.0f / Math.hypot(cost, sint);
				cost *= inv;
				sint *= inv;
				length = 0.5 * atom1.sigma + 10;
				if (torque < 0) {
					line.setLine(axis.x1, axis.y1, axis.x1 + sint * length, axis.y1 - cost * length);
					drawArrow(g, cost, sint);
				}
				else {
					line.setLine(axis.x1, axis.y1, axis.x1 - sint * length, axis.y1 + cost * length);
					drawArrow(g, -cost, -sint);
				}
				g.draw(line);
				cost = axis.x2 - axis.x1;
				sint = axis.y2 - axis.y1;
				cost *= inv;
				sint *= inv;
				length = 0.5 * atom2.sigma + 10;
				if (torque < 0) {
					line.setLine(axis.x2, axis.y2, axis.x2 + sint * length, axis.y2 - cost * length);
					drawArrow(g, cost, sint);
				}
				else {
					line.setLine(axis.x2, axis.y2, axis.x2 - sint * length, axis.y2 + cost * length);
					drawArrow(g, -cost, -sint);
				}
				g.draw(line);
				break;
			}
		}
	}

	void forceVibration(float time) {
		if (Math.abs(amplitude) < ZERO)
			return;
		double cost = atom2.rx - atom1.rx;
		double sint = atom2.ry - atom1.ry;
		double k = MDModel.GF_CONVERSION_CONSTANT * amplitude * Math.cos(Math.PI * 2 * time / period + phase)
				/ Math.hypot(cost, sint);
		cost *= k;
		sint *= k;
		atom1.fx += cost / atom1.mass;
		atom1.fy += sint / atom1.mass;
		atom2.fx -= cost / atom2.mass;
		atom2.fy -= sint / atom2.mass;
	}

	void applyTorque() {
		if (Math.abs(torque) < ZERO)
			return;
		switch (torqueType) {
		case TORQUE_AROUND_ATOM1:
			double cost = atom2.rx - atom1.rx;
			double sint = atom2.ry - atom1.ry;
			double inv = torque / (atom2.mass * Math.hypot(cost, sint));
			cost *= inv;
			sint *= inv;
			atom2.fx -= sint;
			atom2.fy += cost;
			break;
		case TORQUE_AROUND_ATOM2:
			cost = atom1.rx - atom2.rx;
			sint = atom1.ry - atom2.ry;
			inv = torque / (atom1.mass * Math.hypot(cost, sint));
			cost *= inv;
			sint *= inv;
			atom1.fx -= sint;
			atom1.fy += cost;
			break;
		case TORQUE_AROUND_CENTER:
			cost = atom2.rx - atom1.rx;
			sint = atom2.ry - atom1.ry;
			double r = Math.hypot(cost, sint);
			inv = torque / (atom2.mass * r);
			cost *= inv;
			sint *= inv;
			atom2.fx -= sint;
			atom2.fy += cost;
			cost = atom1.rx - atom2.rx;
			sint = atom1.ry - atom2.ry;
			inv = torque / (atom1.mass * r);
			cost *= inv;
			sint *= inv;
			atom1.fx -= sint;
			atom1.fy += cost;
			break;
		}

	}

	public static class Delegate extends ComponentDelegate {

		private int atom1, atom2;
		private double bondLength, bondStrength;
		private double chemicalEnergy;
		private boolean smart;
		private boolean solid;
		private boolean closed;
		private boolean visible = true;
		private Color color;
		private byte style = STANDARD_STICK_STYLE;
		private float torque;
		private byte torqueType;
		private float amplitude, phase;
		private int period = 100;

		public Delegate() {
		}

		public Delegate(RadialBond rb) {
			atom1 = rb.atom1.getIndex();
			atom2 = rb.atom2.getIndex();
			bondLength = rb.bondLength;
			bondStrength = rb.bondStrength;
			chemicalEnergy = rb.chemicalEnergy;
			torque = rb.torque;
			torqueType = rb.torqueType;
			amplitude = rb.amplitude;
			period = rb.period;
			phase = rb.phase;
			solid = rb.solid;
			smart = rb.smart;
			closed = rb.closed;
			color = rb.bondColor;
			style = rb.bondStyle;
			visible = rb.visible;
		}

		public Delegate(int atom1, int atom2, double bondLength, double bondStrength, boolean smart, boolean solid,
				boolean closed, Color color, byte style) {
			if (atom1 == atom2)
				throw new IllegalArgumentException("The two participants of a radial bond must not be identical!");
			this.atom1 = atom1;
			this.atom2 = atom2;
			this.bondLength = bondLength;
			this.bondStrength = bondStrength;
			this.smart = smart;
			this.solid = solid;
			this.closed = closed;
			this.color = color;
			this.style = style;
		}

		public void setVisible(boolean b) {
			visible = b;
		}

		public boolean isVisible() {
			return visible;
		}

		public void setColor(Color c) {
			color = c;
		}

		public Color getColor() {
			return color;
		}

		public void setStyle(byte b) {
			style = b;
		}

		public byte getStyle() {
			return style;
		}

		public void setSmart(boolean b) {
			smart = b;
		}

		public boolean isSmart() {
			return smart;
		}

		public void setSolid(boolean b) {
			solid = b;
		}

		public boolean isSolid() {
			return solid;
		}

		public void setClosed(boolean b) {
			closed = b;
		}

		public boolean isClosed() {
			return closed;
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

		public double getBondLength() {
			return bondLength;
		}

		public void setBondLength(double d) {
			bondLength = d;
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

		public void setTorque(float t) {
			torque = t;
		}

		public float getTorque() {
			return torque;
		}

		public void setTorqueType(byte i) {
			torqueType = i;
		}

		public byte getTorqueType() {
			return torqueType;
		}

		public void setAmplitude(float amplitude) {
			this.amplitude = amplitude;
		}

		public float getAmplitude() {
			return amplitude;
		}

		public void setPeriod(int period) {
			this.period = period;
		}

		public int getPeriod() {
			return period;
		}

		public void setPhase(float phase) {
			this.phase = phase;
		}

		public float getPhase() {
			return phase;
		}

		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (!(obj instanceof RadialBond.Delegate))
				return false;
			RadialBond.Delegate rbd = (RadialBond.Delegate) obj;
			return atom1 == rbd.getAtom1() && atom2 == rbd.getAtom2()
					&& Math.abs(bondLength - rbd.getBondLength()) < ZERO
					&& Math.abs(bondStrength - rbd.getBondStrength()) < ZERO
					&& Math.abs(chemicalEnergy - rbd.getChemicalEnergy()) < ZERO;
		}

		public int hashCode() {
			int result = HashCodeUtil.hash(HashCodeUtil.SEED, atom1);
			result = HashCodeUtil.hash(result, atom2);
			result = HashCodeUtil.hash(result, bondLength);
			result = HashCodeUtil.hash(result, bondStrength);
			result = HashCodeUtil.hash(result, chemicalEnergy);
			return result;
		}

		public String toString() {
			return "[" + atom1 + "," + atom2 + "]";
		}

	}

}