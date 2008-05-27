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
import java.awt.Point;

import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.TextContainer;
import org.concord.mw2d.MDView;

public class TextBoxComponent extends TextContainer implements ModelComponent, Layered {

	private boolean blinking, marked;
	private int layer = FRONT;
	private MDModel model;
	private double savedX = -1.0, savedY = -1.0;
	private boolean stateStored;
	private ModelComponent host;

	public TextBoxComponent() {
		super("");
	}

	public TextBoxComponent(String text) {
		super(text);
	}

	public TextBoxComponent(TextBoxComponent tb) {
		super(tb);
		setLayer(tb.layer);
		setModel(tb.model);
	}

	public void set(Delegate d) {
		setText(d.getText());
		set2(d);
	}

	private void set2(Delegate d) {
		setRx(d.getX());
		setRy(d.getY());
		setAngle(d.getAngle());
		setFont(d.getFont());
		setFillMode(d.getFillMode());
		setBorderType((byte) d.getBorderType());
		setShadowType((byte) d.getShadowType());
		setForegroundColor(d.getForegroundColor());
		setLayer(d.getLayer());
		setCallOut(d.isCallOut());
		if (isCallOut())
			getCallOutPoint().setLocation(d.getCallOutPoint());
		setAttachmentPosition(d.getAttachmentPosition());
		String s = d.getHostType();
		if (s != null) {
			int index = d.getHostIndex();
			if (s.endsWith("Atom")) {
				if (model instanceof MolecularModel) {
					setHost(((MolecularModel) model).getAtom(index));
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
		savedX = getRx();
		savedY = getRy();
		stateStored = true;
		if (host instanceof Atom) {
			((Atom) host).storeCurrentState();
		}
		else if (host instanceof RadialBond) {
			if (model instanceof MolecularModel) {
				Molecule m = ((MolecularModel) model).molecules.getMolecule((RadialBond) host);
				m.storeCurrentState();
			}
		}
		else if (host instanceof RectangularObstacle) {
			((RectangularObstacle) host).storeCurrentState();
		}
		else if (host instanceof GayBerneParticle) {
			((GayBerneParticle) host).storeCurrentState();
		}
	}

	public void restoreState() {
		if (!stateStored)
			return;
		setRx(savedX);
		setRy(savedY);
		if (host instanceof Atom) {
			((Atom) host).restoreState();
		}
		else if (host instanceof RadialBond) {
			if (model instanceof MolecularModel) {
				Molecule m = ((MolecularModel) model).molecules.getMolecule((RadialBond) host);
				m.restoreState();
			}
		}
		else if (host instanceof RectangularObstacle) {
			((RectangularObstacle) host).restoreState();
		}
		else if (host instanceof GayBerneParticle) {
			((GayBerneParticle) host).restoreState();
		}
	}

	/** TODO */
	public void blink() {
	}

	public void destroy() {
		model = null;
		host = null;
	}

	public void setVisible(boolean b) {
	}

	public boolean isVisible() {
		return true;
	}

	/** set a model component this text box should mount on. */
	public void setHost(ModelComponent mc) {
		if (mc != null)
			storeCurrentState();
		host = mc;
	}

	/** get a model component this text box will mount on. */
	public ModelComponent getHost() {
		return host;
	}

	protected void attachToHost() {
		if (host == null)
			return;
		switch (getAttachmentPosition()) {
		case BOX_CENTER:
			setLocation(host.getRx() - 0.5 * getWidth(), host.getRy() - 0.5 * getHeight());
			break;
		case ARROW_HEAD:
			setCallOutLocation((int) host.getRx(), (int) host.getRy());
			break;
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

	public static class Delegate extends LayeredComponentDelegate {

		private Font font = new Font("Arial", Font.PLAIN, 12);
		private Color fgColor = Color.black;
		private int borderType, shadowType;
		private String text;
		private FillMode fillMode = FillMode.getNoFillMode();
		private byte attachmentPosition = ARROW_HEAD;
		private boolean callOut;
		private Point callOutPoint = new Point(20, 20);
		private float angle;

		public Delegate() {
		}

		public Delegate(TextBoxComponent t) {
			if (t == null)
				throw new IllegalArgumentException("arg can't be null");
			text = t.getText();
			font = t.getFont();
			angle = t.getAngle();
			x = t.getRx();
			y = t.getRy();
			fgColor = t.getForegroundColor();
			fillMode = t.getFillMode();
			borderType = t.getBorderType();
			shadowType = t.getShadowType();
			layer = t.getLayer();
			layerPosition = (byte) ((MDView) t.getHostModel().getView()).getLayerPosition(t);
			if (t.getHost() != null) {
				hostType = t.getHost().getClass().toString();
				if (t.getHost() instanceof Particle) {
					hostIndex = ((Particle) t.getHost()).getIndex();
				}
				else if (t.getHost() instanceof RectangularObstacle) {
					hostIndex = t.getHostModel().getObstacles().indexOf(t.getHost());
				}
			}
			attachmentPosition = t.getAttachmentPosition();
			callOut = t.isCallOut();
			callOutPoint.setLocation(t.getCallOutPoint());
		}

		public void setFont(Font font) {
			this.font = font;
		}

		public Font getFont() {
			return font;
		}

		public void setForegroundColor(Color c) {
			fgColor = c;
		}

		public Color getForegroundColor() {
			return fgColor;
		}

		public void setFillMode(FillMode c) {
			fillMode = c;
		}

		public FillMode getFillMode() {
			return fillMode;
		}

		public void setText(String s) {
			text = s;
		}

		public String getText() {
			return text;
		}

		public void setBorderType(int i) {
			borderType = i;
		}

		public int getBorderType() {
			return borderType;
		}

		public void setShadowType(int i) {
			shadowType = i;
		}

		public int getShadowType() {
			return shadowType;
		}

		public void setAttachmentPosition(byte b) {
			attachmentPosition = b;
		}

		public byte getAttachmentPosition() {
			return attachmentPosition;
		}

		public void setCallOut(boolean b) {
			callOut = b;
		}

		public boolean isCallOut() {
			return callOut;
		}

		public void setCallOutPoint(Point p) {
			callOutPoint.setLocation(p);
		}

		public Point getCallOutPoint() {
			return callOutPoint;
		}

		public void setAngle(float angle) {
			this.angle = angle;
		}

		public float getAngle() {
			return angle;
		}

	}

}