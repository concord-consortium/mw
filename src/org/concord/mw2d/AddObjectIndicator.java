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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import org.concord.mw2d.ui.ChainConfigure;

abstract class AddObjectIndicator {

	protected List<Shape> list;
	private boolean painted = true;

	AddObjectIndicator() {
		list = new ArrayList<Shape>();
	}

	void paint(Graphics2D g) {
		for (Shape s : list)
			g.draw(s);
	}

	abstract void setLocation(int x, int y);

	void setPainted(boolean b) {
		painted = b;
	}

	boolean isPainted() {
		return painted;
	}

	static class AddDiatomicMoleculeIndicator extends AddObjectIndicator {

		private float bondLength;
		private float massA, massB;

		AddDiatomicMoleculeIndicator(double sigmaA, double sigmaB, double massA, double massB, double bondLength) {
			super();
			list.add(new Ellipse2D.Float(-1, -1, (float) sigmaA, (float) sigmaA));
			list.add(new Ellipse2D.Float(-1, -1, (float) sigmaB, (float) sigmaB));
			list.add(new Line2D.Float(-1, -1, -1, -1));
			this.bondLength = (float) bondLength;
			this.massA = (float) massA;
			this.massB = (float) massB;
		}

		void setLocation(int x, int y) {
			Ellipse2D.Float a = (Ellipse2D.Float) list.get(0);
			Ellipse2D.Float b = (Ellipse2D.Float) list.get(1);
			float shiftA = massB / (massA + massB) * bondLength;
			a.x = x + shiftA - a.width * 0.5f;
			a.y = y - a.height * 0.5f;
			b.x = x - (bondLength - shiftA) - b.width * 0.5f;
			b.y = y - b.height * 0.5f;
			Line2D.Float c = (Line2D.Float) list.get(2);
			c.x1 = a.x;
			c.y1 = y;
			c.x2 = b.x + b.width;
			c.y2 = y;
		}

	}

	static class AddTriatomicMoleculeIndicator extends AddObjectIndicator {

		private float d12, d23, angle;
		private float massA, massB, massC;
		private final static double INI_ANGLE = Math.PI / 6.0;

		AddTriatomicMoleculeIndicator(double sigmaA, double sigmaB, double sigmaC, double massA, double massB,
				double massC, double d12, double d23, double angle) {
			super();
			list.add(new Ellipse2D.Float(-1, -1, (float) sigmaA, (float) sigmaA));
			list.add(new Ellipse2D.Float(-1, -1, (float) sigmaB, (float) sigmaB));
			list.add(new Ellipse2D.Float(-1, -1, (float) sigmaC, (float) sigmaC));
			list.add(new Line2D.Float(-1, -1, -1, -1));
			list.add(new Line2D.Float(-1, -1, -1, -1));
			this.d12 = (float) d12;
			this.d23 = (float) d23;
			this.angle = (float) Math.toRadians(angle);
			this.massA = (float) massA;
			this.massB = (float) massB;
			this.massC = (float) massC;
		}

		void setLocation(int x, int y) {
			float x1 = (float) (d12 * Math.cos(INI_ANGLE));
			float y1 = (float) (d12 * Math.sin(INI_ANGLE));
			float x2 = 0;
			float y2 = 0;
			float x3 = (float) (d23 * Math.cos(angle + INI_ANGLE));
			float y3 = (float) (d23 * Math.sin(angle + INI_ANGLE));
			float xm = (x1 * massA + x2 * massB + x3 * massC) / (massA + massB + massC);
			float ym = (y1 * massA + y2 * massB + y3 * massC) / (massA + massB + massC);
			Ellipse2D.Float a = (Ellipse2D.Float) list.get(0);
			Ellipse2D.Float b = (Ellipse2D.Float) list.get(1);
			Ellipse2D.Float c = (Ellipse2D.Float) list.get(2);
			a.x = x + x1 - xm - a.width * 0.5f;
			a.y = y + y1 - ym - a.height * 0.5f;
			b.x = x + x2 - xm - b.width * 0.5f;
			b.y = y + y2 - ym - b.height * 0.5f;
			c.x = x + x3 - xm - c.width * 0.5f;
			c.y = y + y3 - ym - c.height * 0.5f;
			Line2D.Float d = (Line2D.Float) list.get(3);
			Line2D.Float e = (Line2D.Float) list.get(4);
			d.x1 = a.x + a.width * .5f;
			d.y1 = a.y + a.height * .5f;
			d.x2 = b.x + b.width * .5f;
			d.y2 = b.y + b.height * .5f;
			e.x1 = d.x2;
			e.y1 = d.y2;
			e.x2 = c.x + c.width * .5f;
			e.y2 = c.y + c.height * .5f;
		}

	}

	static class AddBenzeneMoleculeIndicator extends AddObjectIndicator {

		private float outerRadius = 35;
		private float innerRadius = 20;
		private final static float ANGLE_INCR = (float) Math.toRadians(60.0);

		AddBenzeneMoleculeIndicator(double sigmaH, double sigmaC) {
			super();
			for (int i = 0; i < 6; i++) {
				list.add(new Ellipse2D.Float(-1, -1, (float) sigmaH, (float) sigmaH));
				list.add(new Ellipse2D.Float(-1, -1, (float) sigmaC, (float) sigmaC));
			}
			for (int i = 0; i < 12; i++) {
				list.add(new Line2D.Float(-1, -1, -1, -1));
			}

		}

		void setLocation(int x, int y) {

			float[] xa = new float[12];
			float[] ya = new float[12];
			double angle = 0;
			float xm = 0;
			float ym = 0;
			for (int n = 0; n < 6; n++) {
				xa[n + n] = outerRadius * (float) Math.cos(angle);
				ya[n + n] = outerRadius * (float) Math.sin(angle);
				xa[n + n + 1] = innerRadius * (float) Math.cos(angle);
				ya[n + n + 1] = innerRadius * (float) Math.sin(angle);
				angle += ANGLE_INCR;
				xm += xa[n + n] + xa[n + n + 1];
				ym += ya[n + n] + ya[n + n + 1];
			}
			xm /= 12;
			ym /= 12;

			for (int i = 0; i < 12; i++) {
				Ellipse2D.Float a = (Ellipse2D.Float) list.get(i);
				a.x = x + xa[i] - xm - a.width * 0.5f;
				a.y = y + ya[i] - ym - a.height * 0.5f;
			}

			Ellipse2D.Float a, b;
			Line2D.Float c;
			for (int i = 0; i < 6; i++) {
				a = (Ellipse2D.Float) list.get(i + i);
				b = (Ellipse2D.Float) list.get(i + i + 1);
				c = (Line2D.Float) list.get(i + 12);
				c.x1 = a.x + a.width * .5f;
				c.y1 = a.y + a.height * .5f;
				c.x2 = b.x + b.width * .5f;
				c.y2 = b.y + b.height * .5f;
			}

			for (int i = 0; i < 5; i++) {
				a = (Ellipse2D.Float) list.get(i + i + 1);
				b = (Ellipse2D.Float) list.get(i + i + 3);
				c = (Line2D.Float) list.get(i + 18);
				c.x1 = a.x + a.width * .5f;
				c.y1 = a.y + a.height * .5f;
				c.x2 = b.x + b.width * .5f;
				c.y2 = b.y + b.height * .5f;
			}
			a = (Ellipse2D.Float) list.get(11);
			b = (Ellipse2D.Float) list.get(1);
			c = (Line2D.Float) list.get(23);
			c.x1 = a.x + a.width * .5f;
			c.y1 = a.y + a.height * .5f;
			c.x2 = b.x + b.width * .5f;
			c.y2 = b.y + b.height * .5f;

		}

	}

	static class AddChainMoleculeIndicator extends AddObjectIndicator {

		private int natom;
		private int growMode;
		private float length, angle;

		AddChainMoleculeIndicator(double sigma, int natom, int growMode, float length, float angle) {

			super();
			this.natom = natom;
			for (int i = 0; i < natom; i++)
				list.add(new Ellipse2D.Float(-1, -1, (float) sigma, (float) sigma));
			for (int i = 0; i < natom - 1; i++)
				list.add(new Line2D.Float(-1, -1, -1, -1));
			this.growMode = growMode;
			this.length = length;
			this.angle = (float) Math.toRadians(angle);

		}

		void setLocation(int x, int y) {

			float theta = 0;
			float[] xa = new float[natom];
			float[] ya = new float[natom];
			float xm = 0, ym = 0;

			for (int k = 0; k < natom; k++) {
				if (growMode == ChainConfigure.SAWTOOTH) {
					theta = k % 2 == 0 ? angle * 0.5f : -angle * 0.5f;
				}
				else if (growMode == ChainConfigure.CURLUP) {
					theta += angle * 0.5f;
				}
				xa[k] = k == 0 ? length * (float) Math.cos(theta) : xa[k - 1] + length * (float) Math.cos(theta);
				ya[k] = k == 0 ? length * (float) Math.sin(theta) : ya[k - 1] + length * (float) Math.sin(theta);
				xm += xa[k];
				ym += ya[k];
			}
			xm /= natom;
			ym /= natom;

			Ellipse2D.Float a;
			for (int i = 0; i < natom; i++) {
				a = (Ellipse2D.Float) list.get(i);
				a.x = x + xa[i] - xm - a.width * 0.5f;
				a.y = y + ya[i] - ym - a.height * 0.5f;
			}

			Ellipse2D.Float b;
			Line2D.Float c;
			for (int i = 0; i < natom - 1; i++) {
				a = (Ellipse2D.Float) list.get(i);
				b = (Ellipse2D.Float) list.get(i + 1);
				c = (Line2D.Float) list.get(natom + i);
				c.x1 = a.x + a.width * .5f;
				c.y1 = a.y + a.height * .5f;
				c.x2 = b.x + b.width * .5f;
				c.y2 = b.y + b.height * .5f;
			}

		}

	}

}