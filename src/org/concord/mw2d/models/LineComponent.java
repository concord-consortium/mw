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
import java.awt.geom.Line2D;

import javax.vecmath.Vector2d;

import org.concord.modeler.draw.AbstractLine;
import org.concord.modeler.draw.ArrowRectangle;
import org.concord.modeler.draw.LineStyle;
import org.concord.mw2d.MDView;

public class LineComponent extends AbstractLine implements ModelComponent, Layered {

	private boolean blinking, marked;
	private int layer = FRONT;
	private boolean reflector;
	private MDModel model;
	private float savedX1 = -1, savedY1 = -1, savedX2 = -1, savedY2 = -1;
	private boolean stateStored;
	private ModelComponent host;
	private Vector2d axis, velo;

	public LineComponent() {
		super();
		setColor(Color.black);
	}

	public LineComponent(LineComponent l) {
		this();
		setLine(l);
		setLineStyle(l.getLineStyle());
		setLineWeight(l.getLineWeight());
		setColor(l.getColor());
		setLayer(l.layer);
		setModel(l.model);
		setReflector(l.isReflector());
		setBeginStyle(l.getBeginStyle());
		setEndStyle(l.getEndStyle());
		setOption(l.getOption());
	}

	public void set(Delegate d) {
		setLine((float) (d.getX() + 0.5 * d.getX12()), (float) (d.getY() + 0.5 * d.getY12()),
				(float) (d.getX() - 0.5 * d.getX12()), (float) (d.getY() - 0.5 * d.getY12()));
		setColor(d.getColor());
		setOption(d.getOption());
		setLineStyle((byte) d.getStyle());
		setLineWeight((byte) d.getWeight());
		setLayer(d.getLayer());
		setBeginStyle(d.getBeginStyle());
		setEndStyle(d.getEndStyle());
		setAttachmentPosition(d.getAttachmentPosition());
		setReflector(d.isReflector());
		String s = d.getHostType();
		if (s != null) {
			int index = d.getHostIndex();
			if (s.endsWith("Atom")) {
				if (model instanceof MolecularModel) {
					setHost(((MolecularModel) model).getAtom(index));
				}
			}
			else if (s.endsWith("RadialBond")) {
				if (model instanceof MolecularModel) {
					MolecularModel mm = (MolecularModel) model;
					if (mm.bonds != null) {
						int n = mm.bonds.size();
						if (index < n && index >= 0)
							setHost(mm.bonds.get(index));
					}
				}
			}
			else if (s.endsWith("GayBerneParticle")) {
				if (model instanceof MesoModel) {
					setHost(((MesoModel) model).getParticle(index));
				}
			}
			else if (s.endsWith("Obstacle")) {
				setHost(model.getObstacles().get(index));
			}
		}
	}

	public void storeCurrentState() {
		savedX1 = getX1();
		savedY1 = getY1();
		savedX2 = getX2();
		savedY2 = getY2();
		stateStored = true;
	}

	public void restoreState() {
		if (!stateStored)
			return;
		setEndPoint1(savedX1, savedY1);
		setEndPoint2(savedX2, savedY2);
	}

	/** TODO */
	public void blink() {
	}

	public void destroy() {
		model = null;
		host = null;
	}

	public void setReflector(boolean b) {
		reflector = b;
	}

	public boolean isReflector() {
		return reflector;
	}

	public void setVisible(boolean b) {
	}

	public boolean isVisible() {
		return true;
	}

	/** set a model component this line attaches to. */
	public void setHost(ModelComponent mc) {
		if (mc != null)
			storeCurrentState();
		host = mc;
		if (host == null)
			setAttachmentPosition(CENTER);
	}

	/** get a model component this line attaches to. */
	public ModelComponent getHost() {
		return host;
	}

	protected void attachToHost() {
		if (host == null)
			return;
		if (host instanceof Particle || host instanceof RectangularObstacle) {
			switch (getAttachmentPosition()) {
			case CENTER:
				setLocation(host.getRx(), host.getRy());
				break;
			case ENDPOINT1:
				float length = (float) getLength();
				float invLength = 1.0f / length;
				float cos = (getX1() - getX2()) * invLength;
				float sin = (getY1() - getY2()) * invLength;
				setLocation(host.getRx() - length * cos * 0.5f, host.getRy() - length * sin * 0.5f);
				break;
			case ENDPOINT2:
				length = (float) getLength();
				invLength = 1.0f / length;
				cos = (getX2() - getX1()) * invLength;
				sin = (getY2() - getY1()) * invLength;
				setLocation(host.getRx() - length * cos * 0.5f, host.getRy() - length * sin * 0.5f);
				break;
			}
		}
		else if (host instanceof RadialBond) {
			RadialBond bond = (RadialBond) host;
			double length = bond.getLength(-1);
			double invLength = 0.5 / length;
			double cos = (bond.atom2.rx - bond.atom1.rx) * invLength;
			double sin = (bond.atom2.ry - bond.atom1.ry) * invLength;
			setX1((float) (bond.atom1.rx - bond.atom1.sigma * cos));
			setY1((float) (bond.atom1.ry - bond.atom1.sigma * sin));
			setX2((float) (bond.atom2.rx + bond.atom2.sigma * cos));
			setY2((float) (bond.atom2.ry + bond.atom2.sigma * sin));
		}
	}

	public void setSelected(boolean b) {
		super.setSelected(b);
		if (b) {
			try {
				((MDView) model.getView()).setSelectedComponent(this);
				setSelectionDrawn(((MDView) model.getView()).getShowSelectionHalo());
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
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

	public void setLayer(int i) {
		layer = i;
	}

	public int getLayer() {
		return layer;
	}

	public void setModel(MDModel model) {
		this.model = model;
		setComponent(model.getView());
	}

	public MDModel getHostModel() {
		return model;
	}

	void reflect(Atom a) {
		if (!reflector)
			return;
		if (a == null)
			return;
		if (!a.movable)
			return;
		if (Math.abs(getX1() - getX2()) < 10) { // vertical line
			if (a.ry + a.sigma * 0.5 < Math.min(getY1(), getY2()) || a.ry - a.sigma * 0.5 > Math.max(getY1(), getY2()))
				return;
			double dx = a.rx + a.sigma * 0.5 - getX1() + getLineWeight() * 0.5;
			if (dx > 0 && dx - a.dx < 0) { // the atom crossed the line from the left
				a.vx = -Math.abs(a.vx);
			}
			else {
				dx = a.rx - a.sigma * 0.5 - getX1() - getLineWeight() * 0.5;
				if (dx < 0 && dx - a.dx > 0) { // the atom crossed the line from the right
					a.vx = Math.abs(a.vx);
				}
			}
		}
		else if (Math.abs(getY1() - getY2()) < 10) { // horizontal line
			if (a.rx + a.sigma * 0.5 < Math.min(getX1(), getX2()) || a.rx - a.sigma * 0.5 > Math.max(getX1(), getX2()))
				return;
			double dy = a.ry + a.sigma * 0.5 - getY1() + getLineWeight() * 0.5;
			if (dy > 0 && dy - a.dy < 0) { // the atom crossed the line from above
				a.vy = -Math.abs(a.vy);
			}
			else {
				dy = a.ry - a.sigma * 0.5 - getY1() - getLineWeight() * 0.5;
				if (dy < 0 && dy - a.dy > 0) { // the atom crossed the line from below
					a.vy = Math.abs(a.vy);
				}
			}
		}
		else {
			double contact = (getLineWeight() + a.sigma) * 0.5;
			double seqDistSq = Line2D.ptSegDistSq(getX1(), getY1(), getX2(), getY2(), a.rx, a.ry);
			if (seqDistSq > contact * contact)
				return;
			if (axis == null)
				axis = new Vector2d();
			if (velo == null)
				velo = new Vector2d();
			double dx = getX2() - getX1();
			double dy = getY2() - getY1();
			if (dx < 0) {
				dx = -dx;
				dy = -dy;
			}
			double k = 1.0 / Math.sqrt(dx * dx + dy * dy);
			double cos = dx * k;
			double sin = dy * k;

			// check if the particle is leaving the line or not
			k = dy / dx;
			double lx = (k * (a.ry - getY1()) + (k * k * getX1() + a.rx)) / (k * k + 1);
			double ly = a.ry - (lx - a.rx) / k;
			lx = a.rx - lx;
			ly = a.ry - ly;
			boolean leaving = lx * a.vx + ly * a.vy >= 0;

			boolean centerAbove = a.ry - (k * (a.rx - getX1()) + getY1()) < 0;
			int p = Math.abs(k) > 1 ? -1 : 1;
			double rx = a.rx + contact * sin * (centerAbove ? p : -p);
			double ry = a.ry + contact * cos * (centerAbove ? p : -p);
			seqDistSq = ry - (k * (rx - getX1()) + getY1());
			if (seqDistSq < 0) { // atom above line now
				if (seqDistSq - a.dy + k * a.dx > 0) { // atom below line last step
					if (!leaving) {
						axis.set(dx, dy);
						velo.set(a.vx, a.vy);
						k = 2 * velo.angle(axis);
						cos = Math.cos(k);
						sin = Math.sin(k);
						dx = a.vx * cos - a.vy * sin;
						dy = a.vx * sin + a.vy * cos;
						a.setVx(dx);
						a.setVy(dy);
					}
				}
			}
			else { // atom below line now
				if (seqDistSq - a.dy + k * a.dx < 0) { // atom above line last step
					if (!leaving) {
						axis.set(dx, dy);
						velo.set(a.vx, a.vy);
						k = -2 * velo.angle(axis);
						cos = Math.cos(k);
						sin = Math.sin(k);
						dx = a.vx * cos - a.vy * sin;
						dy = a.vx * sin + a.vy * cos;
						a.setVx(dx);
						a.setVy(dy);
					}
				}
			}
		}
	}

	public static class Delegate extends LayeredComponentDelegate {

		private Color color = Color.black;
		private byte option = DEFAULT;
		private int weight = 1;
		private int style = LineStyle.STROKE_NUMBER_1;
		private float x12, y12;
		private byte beginStyle = ArrowRectangle.NO_ARROW;
		private byte endStyle = ArrowRectangle.NO_ARROW;
		private byte attachmentPosition = CENTER;
		private boolean reflector;

		public Delegate() {
		}

		public Delegate(LineComponent t) {
			if (t == null)
				throw new IllegalArgumentException("arg can't be null");
			x = t.getRx();
			y = t.getRy();
			x12 = t.getX1() - t.getX2();
			y12 = t.getY1() - t.getY2();
			color = t.getColor();
			option = t.getOption();
			weight = t.getLineWeight();
			style = t.getLineStyle();
			layer = t.getLayer();
			layerPosition = (byte) ((MDView) t.getHostModel().getView()).getLayerPosition(t);
			if (t.getHost() != null) {
				hostType = t.getHost().getClass().toString();
				if (t.getHost() instanceof Particle) {
					hostIndex = ((Particle) t.getHost()).getIndex();
				}
				else if (t.getHost() instanceof RadialBond) {
					hostIndex = ((RadialBond) t.getHost()).getIndex();
				}
				else if (t.getHost() instanceof RectangularObstacle) {
					hostIndex = t.getHostModel().getObstacles().indexOf(t.getHost());
				}
			}
			attachmentPosition = t.getAttachmentPosition();
			beginStyle = t.getBeginStyle();
			endStyle = t.getEndStyle();
			reflector = t.isReflector();
		}

		public void setReflector(boolean b) {
			reflector = b;
		}

		public boolean isReflector() {
			return reflector;
		}

		public void setX12(float x12) {
			this.x12 = x12;
		}

		public float getX12() {
			return x12;
		}

		public void setY12(float y12) {
			this.y12 = y12;
		}

		public float getY12() {
			return y12;
		}

		public void setOption(byte i) {
			option = i;
		}

		public byte getOption() {
			return option;
		}

		public void setColor(Color c) {
			color = c;
		}

		public Color getColor() {
			return color;
		}

		public void setStyle(int i) {
			style = i;
		}

		public int getStyle() {
			return style;
		}

		public void setWeight(int i) {
			weight = i;
		}

		public int getWeight() {
			return weight;
		}

		public void setBeginStyle(byte i) {
			beginStyle = i;
		}

		public byte getBeginStyle() {
			return beginStyle;
		}

		public void setEndStyle(byte i) {
			endStyle = i;
		}

		public byte getEndStyle() {
			return endStyle;
		}

		public void setAttachmentPosition(byte b) {
			attachmentPosition = b;
		}

		public byte getAttachmentPosition() {
			return attachmentPosition;
		}

	}

}