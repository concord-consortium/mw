/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
import java.awt.geom.Point2D;

import org.concord.modeler.draw.AbstractTriangle;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.LineStyle;
import org.concord.mw2d.MDView;

public class TriangleComponent extends AbstractTriangle implements ModelComponent, Layered, FieldArea {

	private boolean blinking, marked;
	private int layer = IN_FRONT_OF_PARTICLES;
	private MDModel model;
	private Point2D.Float savedPointA, savedPointB, savedPointC;
	private boolean stateStored;
	private ModelComponent host;
	private VectorField vectorField;
	private boolean visible = true;
	private boolean draggable = true;
	private float viscosity;

	public TriangleComponent() {
		super();
		setLineColor(Color.black);
	}

	public TriangleComponent(TriangleComponent r) {
		this();
		for (int i = 0; i < 3; i++)
			setVertex(i, r.getVertex(i).x, r.getVertex(i).y);
		setFillMode(r.getFillMode());
		setAlpha(r.getAlpha());
		setLineStyle(r.getLineStyle());
		setLineWeight(r.getLineWeight());
		setLineColor(r.getLineColor());
		setLayer(r.layer);
		setModel(r.model);
		setViscosity(r.viscosity);
		setVectorField(VectorFieldFactory.getCopy(r.vectorField));
	}

	public void set(Delegate d) {
		setVisible(d.visible);
		setDraggable(d.draggable);
		setVertex(0, d.getXa(), d.getYa());
		setVertex(1, d.getXb(), d.getYb());
		setVertex(2, d.getXc(), d.getYc());
		setAngle(d.angle);
		setAlpha((short) d.getAlpha());
		setFillMode(d.getFillMode());
		setLineColor(d.getLineColor());
		setLineStyle((byte) d.getLineStyle());
		setLineWeight((byte) d.getLineWeight());
		setLayer(d.getLayer());
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
		setViscosity(d.viscosity);
		setVectorField(d.vectorField);
	}

	public void storeCurrentState() {
		if (savedPointA == null)
			savedPointA = new Point2D.Float();
		savedPointA.setLocation(getVertex(0).x, getVertex(0).y);
		if (savedPointB == null)
			savedPointB = new Point2D.Float();
		savedPointB.setLocation(getVertex(1).x, getVertex(1).y);
		if (savedPointC == null)
			savedPointC = new Point2D.Float();
		savedPointC.setLocation(getVertex(2).x, getVertex(2).y);
		stateStored = true;
		HostStateManager.storeCurrentState(host);
	}

	public void restoreState() {
		if (!stateStored)
			return;
		setVertex(0, savedPointA.x, savedPointA.y);
		setVertex(1, savedPointB.x, savedPointB.y);
		setVertex(2, savedPointC.x, savedPointC.y);
		HostStateManager.restoreState(host);
	}

	/** TODO */
	public void blink() {
	}

	public void destroy() {
		model = null;
		host = null;
	}

	public void interact(Particle p) {
		if (!contains(p.rx, p.ry))
			return;
		if (viscosity > Particle.ZERO) {
			double dmp = MDModel.GF_CONVERSION_CONSTANT * viscosity / p.getMass();
			p.fx -= dmp * p.vx;
			p.fy -= dmp * p.vy;
		}
	}

	public int getParticleCount() {
		int n = model.getNumberOfParticles();
		Particle p;
		int m = 0;
		for (int i = 0; i < n; i++) {
			p = model.getParticle(i);
			if (contains(p.rx, p.ry))
				m++;
		}
		return m;
	}

	public void setViscosity(float viscosity) {
		this.viscosity = viscosity;
	}

	public float getViscosity() {
		return viscosity;
	}

	public void setVectorField(VectorField vectorField) {
		if (model != null) {
			model.fields.remove(this.vectorField);
			if (vectorField != null)
				model.fields.add(vectorField);
		}
		this.vectorField = vectorField;
		if (this.vectorField != null)
			this.vectorField.setBounds(getShape());
	}

	public VectorField getVectorField() {
		return vectorField;
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

	/** set a model component this line attaches to. */
	public void setHost(ModelComponent mc) {
		if (mc != null)
			storeCurrentState();
		host = mc;
	}

	/** get a model component this line attaches to. */
	public ModelComponent getHost() {
		return host;
	}

	protected void attachToHost() {
		if (host == null)
			return;
		setLocation(host.getRx(), host.getRy());
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

		private Color lineColor = Color.black;
		private int lineWeight = 1;
		private int lineStyle = LineStyle.STROKE_NUMBER_1;
		private float xA, yA;
		private float xB, yB;
		private float xC, yC;
		private int alpha = 255;
		private float angle;
		private FillMode fillMode = FillMode.getNoFillMode();
		private float viscosity;
		private VectorField vectorField;

		public Delegate() {
		}

		public Delegate(TriangleComponent r) {
			if (r == null)
				throw new IllegalArgumentException("arg can't be null");
			xA = r.getVertex(0).x;
			yA = r.getVertex(0).y;
			xB = r.getVertex(1).x;
			yB = r.getVertex(1).y;
			xC = r.getVertex(2).x;
			yC = r.getVertex(2).y;
			angle = r.getAngle();
			alpha = r.getAlpha();
			fillMode = r.getFillMode();
			lineColor = r.getLineColor();
			lineWeight = r.getLineWeight();
			lineStyle = r.getLineStyle();
			layer = r.getLayer();
			layerPosition = (byte) ((MDView) r.getHostModel().getView()).getLayerPosition(r);
			if (r.getHost() != null) {
				hostType = r.getHost().getClass().toString();
				if (r.getHost() instanceof Particle) {
					hostIndex = ((Particle) r.getHost()).getIndex();
				}
				else if (r.getHost() instanceof RectangularObstacle) {
					hostIndex = r.getHostModel().getObstacles().indexOf(r.getHost());
				}
			}
			viscosity = r.getViscosity();
			vectorField = r.getVectorField();
			draggable = r.draggable;
			visible = r.visible;
		}

		public void setViscosity(float viscosity) {
			this.viscosity = viscosity;
		}

		public float getViscosity() {
			return viscosity;
		}

		public void setVectorField(VectorField vectorField) {
			this.vectorField = vectorField;
		}

		public VectorField getVectorField() {
			return vectorField;
		}

		public void setAlpha(int i) {
			alpha = i;
		}

		public int getAlpha() {
			return alpha;
		}

		public void setFillMode(FillMode c) {
			fillMode = c;
		}

		public FillMode getFillMode() {
			return fillMode;
		}

		public void setXa(float xA) {
			this.xA = xA;
		}

		public float getXa() {
			return xA;
		}

		public void setYa(float yA) {
			this.yA = yA;
		}

		public float getYa() {
			return yA;
		}

		public void setXb(float xB) {
			this.xB = xB;
		}

		public float getXb() {
			return xB;
		}

		public void setYb(float yB) {
			this.yB = yB;
		}

		public float getYb() {
			return yB;
		}

		public void setXc(float xC) {
			this.xC = xC;
		}

		public float getXc() {
			return xC;
		}

		public void setYc(float yC) {
			this.yC = yC;
		}

		public float getYc() {
			return yC;
		}

		public void setAngle(float angle) {
			this.angle = angle;
		}

		public float getAngle() {
			return angle;
		}

		public void setLineColor(Color c) {
			lineColor = c;
		}

		public Color getLineColor() {
			return lineColor;
		}

		public void setLineStyle(int i) {
			lineStyle = i;
		}

		public int getLineStyle() {
			return lineStyle;
		}

		public void setLineWeight(int i) {
			lineWeight = i;
		}

		public int getLineWeight() {
			return lineWeight;
		}

	}

}