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

package org.concord.mw2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.Particle;
import org.concord.mw2d.models.UnitedAtom;

public class PointHeater {

	public static final int MIN_DIAMETER = 20;

	private static Color coldColor = new Color(0, 0, 255, 128);
	private static Color hotColor = new Color(255, 0, 0, 128);
	private static Color lineColor = new Color(0, 0, 0, 128);
	private Ellipse2D.Float scope;
	private GeneralPath path;
	private float amount = 0.05f;
	private List<Particle> list;

	public PointHeater() {
		scope = new Ellipse2D.Float(0, 0, MIN_DIAMETER * 3, MIN_DIAMETER * 3);
	}

	public void paint(Graphics2D g, boolean heat) {
		if (scope.x < -0.5f * scope.width || scope.y < -0.5f * scope.height)
			return;
		g.setColor(heat ? hotColor : coldColor);
		g.fill(scope);
		g.setColor(lineColor);
		g.setStroke(ViewAttribute.THIN_DASHED);
		AffineTransform savedAT = g.getTransform();
		g.translate(scope.x + scope.width * 0.5f, scope.y + scope.height * 0.5f);
		drawSine(g);
		g.rotate(0.666666667 * Math.PI);
		drawSine(g);
		g.rotate(0.666666667 * Math.PI);
		drawSine(g);
		g.setTransform(savedAT);
		if (list == null)
			return;
		for (Particle p : list)
			p.drawVelocityVector(g);
	}

	public void reset() {
		if (list == null)
			return;
		list.clear();
	}

	private void drawSine(Graphics2D g) {
		if (path == null)
			path = new GeneralPath();
		else path.reset();
		path.moveTo(0, 0);
		for (int i = 0; i < 80; i += 2) {
			path.lineTo(i, (float) (10.0 * Math.sin(i * 0.1 * Math.PI)));
		}
		g.draw(path);
	}

	public void setForeground(Color c) {
		lineColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), lineColor.getAlpha());
	}

	public Ellipse2D getScope() {
		return scope;
	}

	public void setLocation(int x, int y) {
		scope.x = x - 0.5f * scope.width;
		scope.y = y - 0.5f * scope.height;
	}

	public void setSize(float diameter) {
		scope.width = scope.height = diameter;
	}

	public void setAmount(float amount) {
		if (amount < 0)
			amount = -amount;
		this.amount = amount;
	}

	public float getAmount() {
		return amount;
	}

	public void equiPartitionEnergy(MDModel model) {

		if (list == null) {
			list = new ArrayList<Particle>();
		}
		else {
			list.clear();
		}

		int n = model.getNumberOfParticles();
		for (int i = 0; i < n; i++) {
			Particle p = model.getParticle(i);
			if (p.isCenterOfMassContained(scope))
				list.add(p);
		}

		if (list.size() < 2)
			return;

		double v2 = 0.0;
		double omega = 0.0;
		for (Particle p : list) {
			v2 += p.getMass() * (p.getVx() * p.getVx() + p.getVy() * p.getVy());
			if (p instanceof UnitedAtom) {
				omega = ((UnitedAtom) p).getOmega();
				v2 += omega * omega * ((UnitedAtom) p).getInertia();
			}
		}
		v2 = v2 / list.size();
		for (Particle p : list) {
			if (p instanceof UnitedAtom) {
				// two translational degrees of freedom
				p.setRandomVelocity(Math.sqrt(0.66666667 * v2 / p.getMass()));
				// one rotational degree of freedom
				((UnitedAtom) p).setRandomAngularVelocity(Math.sqrt(0.33333 * v2 / ((UnitedAtom) p).getInertia()));
			}
			else {
				p.setRandomVelocity(Math.sqrt(v2 / p.getMass()));
			}
		}

	}

	public void doWork(MDModel model, boolean heat) {

		if (list == null) {
			list = new ArrayList<Particle>();
		}
		else {
			list.clear();
		}

		int n = model.getNumberOfParticles();
		Particle p = null;
		for (int i = 0; i < n; i++) {
			p = model.getParticle(i);
			if (p.isCenterOfMassContained(scope)) {
				list.add(p);
			}
		}

		if (!list.isEmpty()) {
			model.transferHeatToParticles(list, heat ? amount : -amount);
			model.getView().repaint();
		}

	}

}