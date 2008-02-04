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
import java.awt.geom.GeneralPath;

import org.concord.mw2d.ViewAttribute;

public class Photon implements ModelComponent {

	public final static float MIN_VISIBLE_FREQ = 5;
	public final static float MAX_VISIBLE_FREQ = 12;
	public final static Color[] COLOR = { Color.red, Color.orange, Color.yellow, Color.green, Color.cyan, Color.blue,
			new Color(139, 0, 255) };

	private float[] ysin = new float[40];

	private static float c = 0.2f;
	private static GeneralPath squiggle;

	float x;
	float y;
	private float xUndo;
	private float yUndo;
	private float omega = 1.0f;
	private float angle;
	private boolean fromLightSource;

	private MDModel model;
	private boolean selected;
	private boolean marked;
	private boolean visible;
	private boolean blinking;
	private Color color = Color.black;
	private boolean stateStored;

	public Photon(float x, float y, float freq) {
		setX(x);
		setY(y);
		setOmega(freq);
	}

	public Photon(Photon p) {
		setX(p.x);
		setY(p.y);
		setOmega(p.getOmega());
		angle = p.angle;
		model = p.model;
	}

	private void setShape(float freq) {
		double t = freq * 2 * Math.PI / ysin.length;
		for (int i = 0; i < ysin.length; i++) {
			ysin[i] = (float) (10 * Math.sin(i * t) / (1 + 0.01 * (i - 0.5 * ysin.length) * (i - 0.5 * ysin.length)));
		}
	}

	void setFromLightSource(boolean b) {
		fromLightSource = b;
	}

	public boolean isFromLightSource() {
		return fromLightSource;
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

	public void setVisible(boolean b) {
		visible = b;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setBlinking(boolean b) {
		blinking = b;
	}

	public boolean isBlinking() {
		return blinking;
	}

	public void blink() {
	}

	public boolean contains(double x, double y) {
		if ((this.x - x) * (this.x - x) + (this.y - y) * (this.y - y) < 16)
			return true;
		return false;
	}

	public void setModel(MDModel model) {
		this.model = model;
	}

	public MDModel getHostModel() {
		return model;
	}

	public void storeCurrentState() {
		xUndo = x;
		yUndo = y;
		stateStored = true;
	}

	public void restoreState() {
		if (!stateStored)
			return;
		x = xUndo;
		y = yUndo;
	}

	public void destroy() {
		model = null;
	}

	public double getRx() {
		return x;
	}

	public double getRy() {
		return y;
	}

	/** set the light speed. */
	public static void setC(float speed) {
		c = speed;
	}

	/** get the light speed. */
	public static float getC() {
		return c;
	}

	/** set the angular frequency of this photon. */
	public void setOmega(float omega) {
		this.omega = omega;
		setShape(omega);
		if (omega <= MIN_VISIBLE_FREQ || omega >= MAX_VISIBLE_FREQ) {
			color = Color.black;
		}
		else {
			float d = (MAX_VISIBLE_FREQ - MIN_VISIBLE_FREQ) / 7.0f;
			int i = (int) ((omega - MIN_VISIBLE_FREQ) / d);
			if (i == 6) {
				color = COLOR[i];
			}
			else {
				float remainder = (omega - MIN_VISIBLE_FREQ - i * d) / d;
				int r1 = COLOR[i].getRed();
				int r2 = COLOR[i + 1].getRed();
				int g1 = COLOR[i].getGreen();
				int g2 = COLOR[i + 1].getGreen();
				int b1 = COLOR[i].getBlue();
				int b2 = COLOR[i + 1].getBlue();
				color = new Color((int) (r1 + remainder * (r2 - r1)), (int) (g1 + remainder * (g2 - g1)),
						(int) (b1 + remainder * (b2 - b1)));
			}
		}
	}

	/** get the angular frequency of this photon. */
	public float getOmega() {
		return omega;
	}

	public void setEnergy(float e) {
		setOmega(e / MDModel.PLANCK_CONSTANT);
	}

	public double getEnergy() {
		return MDModel.PLANCK_CONSTANT * omega;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getX() {
		return x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getY() {
		return y;
	}

	/** the angle is already set between -pi and +pi */
	public void setAngle(float angle) {
		if (angle < -Math.PI)
			angle += Math.PI * 2;
		else if (angle > Math.PI)
			angle -= Math.PI * 2;
		this.angle = angle;
	}

	public float getAngle() {
		return angle;
	}

	public void move(float dt) {
		x += c * Math.cos(angle) * dt;
		y += c * Math.sin(angle) * dt;
	}

	/** render this photon on a graphics context */
	public synchronized void render(Graphics2D g) {

		g.translate(x, y);
		g.rotate(angle);

		g.setStroke(ViewAttribute.THIN);
		g.setColor(color);
		g.drawLine(0, 0, 4, 0);
		g.drawLine(4, 0, 2, -2);
		g.drawLine(4, 0, 2, 2);

		if (squiggle == null)
			squiggle = new GeneralPath();
		else squiggle.reset();

		squiggle.moveTo(0, 0);
		for (int i = 0; i < ysin.length; i++)
			squiggle.lineTo(-i, ysin[i]);
		g.draw(squiggle);

		g.rotate(-angle);
		g.translate(-x, -y);

	}

	public static class Delegate extends ComponentDelegate {

		private float x;
		private float y;
		private float omega;
		private float angle;

		public Delegate() {
		}

		public Delegate(Photon p) {
			x = p.x;
			y = p.y;
			omega = p.omega;
			angle = p.angle;
		}

		public void setX(float x) {
			this.x = x;
		}

		public float getX() {
			return x;
		}

		public void setY(float y) {
			this.y = y;
		}

		public float getY() {
			return y;
		}

		public void setOmega(float omega) {
			this.omega = omega;
		}

		public float getOmega() {
			return omega;
		}

		public void setAngle(float angle) {
			this.angle = angle;
		}

		public float getAngle() {
			return angle;
		}

	}

}