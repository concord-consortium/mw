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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.g2d.Curve;
import org.concord.modeler.g2d.CurveFlavor;
import org.concord.modeler.g2d.Legend;
import org.concord.modeler.g2d.XYGrapher;
import org.concord.modeler.util.SwingWorker;

class Mvd {

	private final static byte X_ONLY = 0x01;
	private final static byte Y_ONLY = 0x02;
	private final static byte XY_BOTH = 0x04;

	private AtomicModel model;
	private int nslice = 100;

	final static class Parameter {

		boolean scalar;
		String direction;
		float bound;
		byte element;
		Rectangle2D area;

		Parameter(boolean scalar, String direction, float bound, byte element, Rectangle2D area) {
			this.scalar = scalar;
			this.direction = direction;
			this.bound = bound;
			this.element = element;
			this.area = area;
		}

		public String toString() {
			return scalar + ", " + direction + ", " + bound + ", " + element + ", " + area;
		}

	}

	Mvd(AtomicModel model) {
		this.model = model;
	}

	void setNSlice(int nslice) {
		this.nslice = nslice;
	}

	private double[] compute(Parameter p) {
		int m = model.getTapePointer();
		if (m <= 0)
			return null;
		if (model.getNumberOfAtoms(p.element) <= 0)
			return null;
		if (p.bound < 0)
			p.bound = -p.bound;
		double[] mvd = new double[nslice];
		float delta = 1;
		float rx, ry, vx, vy;
		byte option = XY_BOTH;
		if ("x".equalsIgnoreCase(p.direction))
			option = X_ONLY;
		else if ("y".equalsIgnoreCase(p.direction))
			option = Y_ONLY;
		boolean doAreaDetection = !p.area.equals(model.boundary);
		if (p.scalar) {
			delta = p.bound / nslice;
			float invDelta = 1.0f / delta;
			for (int i = 0; i < model.numberOfAtoms; i++) {
				if (model.atom[i].getID() == p.element) {
					for (int k = 0; k < m; k++) {
						if (doAreaDetection) {
							rx = model.atom[i].getRxRyQueue().getQueue1().getData(k);
							ry = model.atom[i].getRxRyQueue().getQueue2().getData(k);
							if (!p.area.contains(rx, ry))
								continue;
						}
						switch (option) {
						case X_ONLY:
							vx = Math.abs(10000 * model.atom[i].getVxVyQueue().getQueue1().getData(k));
							if (vx < p.bound) {
								int a = Math.round(vx * invDelta);
								if (a >= 0 && a < mvd.length)
									mvd[a]++;
							}
							break;
						case Y_ONLY:
							vy = Math.abs(10000 * model.atom[i].getVxVyQueue().getQueue2().getData(k));
							if (vy < p.bound) {
								int a = Math.round(vy * invDelta);
								if (a >= 0 && a < mvd.length)
									mvd[a]++;
							}
							break;
						case XY_BOTH:
							vx = model.atom[i].getVxVyQueue().getQueue1().getData(k);
							vy = model.atom[i].getVxVyQueue().getQueue2().getData(k);
							vx = 10000 * (float) Math.hypot(vx, vy);
							if (vx < p.bound) {
								int a = Math.round(vx * invDelta);
								if (a >= 0 && a < mvd.length)
									mvd[a]++;
							}
							break;
						}
					}
				}
			}
		}
		else {
			delta = 2 * p.bound / nslice;
			float invDelta = 1.0f / delta;
			for (int i = 0; i < model.numberOfAtoms; i++) {
				if (model.atom[i].getID() == p.element) {
					for (int k = 0; k < m; k++) {
						if (doAreaDetection) {
							rx = model.atom[i].getRxRyQueue().getQueue1().getData(k);
							ry = model.atom[i].getRxRyQueue().getQueue2().getData(k);
							if (!p.area.contains(rx, ry))
								continue;
						}
						switch (option) {
						case X_ONLY:
							vx = 10000 * model.atom[i].getVxVyQueue().getQueue1().getData(k);
							if (vx < p.bound && vx > -p.bound) {
								int a = Math.round(vx * invDelta + 0.5f * nslice);
								if (a >= 0 && a < mvd.length)
									mvd[a]++;
							}
							break;
						case Y_ONLY:
							vy = 10000 * model.atom[i].getVxVyQueue().getQueue2().getData(k);
							if (vy < p.bound && vy > -p.bound) {
								int a = Math.round(vy * invDelta + 0.5f * nslice);
								if (a >= 0 && a < mvd.length)
									mvd[a]++;
							}
							break;
						case XY_BOTH:
							vx = 10000 * model.atom[i].getVxVyQueue().getQueue1().getData(k);
							vy = 10000 * model.atom[i].getVxVyQueue().getQueue2().getData(k);
							if (vx < p.bound && vx > -p.bound) {
								int a = Math.round(vx * invDelta + 0.5f * nslice);
								if (a >= 0 && a < mvd.length)
									mvd[a]++;
							}
							if (vy < p.bound && vy > -p.bound) {
								int a = Math.round(vy * invDelta + 0.5f * nslice);
								if (a >= 0 && a < mvd.length)
									mvd[a]++;
							}
							break;
						}
					}
				}
			}
		}
		double[] f = new double[nslice + nslice];
		if (p.scalar) {
			float x = 1.0f / (sum1(mvd) * delta);
			for (int i = 0; i < nslice; i++) {
				f[i + i] = delta * i;
				f[i + i + 1] = mvd[i] * i * x;
			}
		}
		else {
			float x = 1.0f / (sum2(mvd) * delta);
			for (int i = 0; i < nslice; i++) {
				f[i + i] = -p.bound + delta * i;
				f[i + i + 1] = mvd[i] * x;
			}
		}
		return f;
	}

	private float sum1(double[] mvd) {
		float x = 0;
		for (int i = 0; i < mvd.length; i++)
			x += mvd[i] * i;
		return x;
	}

	private float sum2(double[] mvd) {
		float x = 0;
		for (int i = 0; i < mvd.length; i++)
			x += mvd[i];
		return x;
	}

	void show(final Parameter[] p) {
		final XYGrapher graph = new XYGrapher();
		graph.addSnapshotListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComponent c = (JComponent) e.getSource();
				Object g = c.getClientProperty("graph");
				if (g instanceof XYGrapher) {
					model.notifyPageComponentListeners(new PageComponentEvent(((XYGrapher) g).getGraph(),
							PageComponentEvent.SNAPSHOT_TAKEN));
				}
			}
		});
		new SwingWorker() {
			public Object construct() {
				Curve[] c = new Curve[p.length];
				double[] data;
				for (int i = 0; i < p.length; i++) {
					data = compute(p[i]);
					if (data == null)
						return null;
					c[i] = new Curve();
					c[i].setLegend(new Legend(Element.idToName(p[i].element) + ":" + p[i].direction + "(" + i + ")",
							200, 50 + i * 15));
					switch (i) {
					case 1:
						c[i].setFlavor(new CurveFlavor(Color.blue));
						break;
					case 2:
						c[i].setFlavor(new CurveFlavor(Color.red));
						break;
					case 3:
						c[i].setFlavor(new CurveFlavor(Color.magenta));
						break;
					case 4:
						c[i].setFlavor(new CurveFlavor(Color.orange));
						break;
					}
					c[i].setData(data);
				}
				return c;
			}

			public void finished() {
				Curve[] c = (Curve[]) getValue();
				if (c != null) {
					graph.getXAxis().setTitleText("v (m/s)");
					graph.getYAxis().setTitleText("Probability density (s/m)");
					for (int i = 0; i < c.length; i++)
						graph.append(c[i]);
					JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(model.view),
							"Maxwell distribution(s)", false);
					graph.setDialog(dialog);
					dialog.getContentPane().add(graph, BorderLayout.CENTER);
					dialog.setSize(300, 300);
					dialog.setLocation(200, 200);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.pack();
					dialog.setVisible(true);
				}
				else {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(model.view),
							"At least one of the selected Maxwell distributions has no data.", "No Data",
							JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}.start();
	}

}