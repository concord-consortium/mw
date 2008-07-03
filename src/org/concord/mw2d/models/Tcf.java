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

class Tcf {

	private final static byte RX = 0x01;
	private final static byte RY = 0x02;
	private final static byte VX = 0x03;
	private final static byte VY = 0x04;
	private final static byte AX = 0x05;
	private final static byte AY = 0x06;

	private AtomicModel model;

	final static class Parameter {

		String fx, fy;
		byte element;
		int length;
		Rectangle2D area;

		Parameter(String fx, String fy, byte element, int length, Rectangle2D area) {
			this.fx = fx;
			this.fy = fy;
			this.element = element;
			this.length = length;
			this.area = area;
		}

		public String toString() {
			return fx + ", " + fy + ", " + element + ", " + length + ", " + area;
		}

	}

	Tcf(AtomicModel model) {
		this.model = model;
	}

	private double[] compute(Parameter p) {
		int m = model.getTapePointer();
		if (m <= 0)
			return null;
		double rxi1, ryi1, rxi2, ryi2;
		int length = p.length;
		if (length * 2 > m)
			length = m / 2;
		double[] tcf = new double[length];
		int j;
		double f1 = 0, f2 = 0;
		int sum;
		byte bx = RX;
		if ("ry".equalsIgnoreCase(p.fx))
			bx = RY;
		else if ("vx".equalsIgnoreCase(p.fx))
			bx = VX;
		else if ("vy".equalsIgnoreCase(p.fx))
			bx = VY;
		else if ("ax".equalsIgnoreCase(p.fx))
			bx = AX;
		else if ("ay".equalsIgnoreCase(p.fx))
			bx = AY;
		byte by = RX;
		if ("ry".equalsIgnoreCase(p.fy))
			by = RY;
		else if ("vx".equalsIgnoreCase(p.fy))
			by = VX;
		else if ("vy".equalsIgnoreCase(p.fy))
			by = VY;
		else if ("ax".equalsIgnoreCase(p.fy))
			by = AX;
		else if ("ay".equalsIgnoreCase(p.fy))
			by = AY;
		boolean doAreaDetection = !p.area.equals(model.boundary);
		for (int x = 0; x < length; x++) {
			sum = 0;
			for (int i = 0; i < model.numberOfAtoms; i++) {
				if (model.atom[i].getID() == p.element) {
					for (int k = 0; k < m; k++) {
						j = k + x;
						if (j >= m)
							continue;
						if (doAreaDetection) {
							rxi1 = model.atom[i].getRxRyQueue().getQueue1().getData(k);
							ryi1 = model.atom[i].getRxRyQueue().getQueue2().getData(k);
							if (!p.area.contains(rxi1, ryi1))
								continue;
							rxi2 = model.atom[i].getRxRyQueue().getQueue1().getData(j);
							ryi2 = model.atom[i].getRxRyQueue().getQueue2().getData(j);
							if (!p.area.contains(rxi2, ryi2))
								continue;
						}
						switch (bx) {
						case RX:
							f1 = model.atom[i].getRxRyQueue().getQueue1().getData(k);
							break;
						case RY:
							f1 = model.atom[i].getRxRyQueue().getQueue2().getData(k);
							break;
						case VX:
							f1 = model.atom[i].getVxVyQueue().getQueue1().getData(k);
							break;
						case VY:
							f1 = model.atom[i].getVxVyQueue().getQueue2().getData(k);
							break;
						case AX:
							f1 = model.atom[i].getAxAyQueue().getQueue1().getData(k);
							break;
						case AY:
							f1 = model.atom[i].getAxAyQueue().getQueue2().getData(k);
							break;
						}
						switch (by) {
						case RX:
							f2 = model.atom[i].getRxRyQueue().getQueue1().getData(j);
							break;
						case RY:
							f2 = model.atom[i].getRxRyQueue().getQueue2().getData(j);
							break;
						case VX:
							f2 = model.atom[i].getVxVyQueue().getQueue1().getData(j);
							break;
						case VY:
							f2 = model.atom[i].getVxVyQueue().getQueue2().getData(j);
							break;
						case AX:
							f2 = model.atom[i].getAxAyQueue().getQueue1().getData(j);
							break;
						case AY:
							f2 = model.atom[i].getAxAyQueue().getQueue2().getData(j);
							break;
						}
						tcf[x] += f1 * f2;
						sum++;
					}
				}
			}
			if (sum > 0)
				tcf[x] /= sum;
		}
		double[] f = new double[length + length];
		/* normalize: */
		double interval = model.movieUpdater.getInterval() * model.getTimeStep();
		for (int i = 0; i < length; i++) {
			f[i + i] = i * interval;
			f[i + i + 1] = tcf[i] / tcf[0];
		}
		return f;
	}

	void show(final Parameter[] p) {
		String s2 = "";
		if (p.length > 1) {
			int i = 0;
			for (; i < p.length - 1; i++) {
				s2 += Element.idToName(p[i].element) + ": " + p[i].fx + "-" + p[i].fy + "(" + i + "), ";
			}
			i = p.length - 1;
			s2 += Element.idToName(p[i].element) + ": " + p[i].fx + "-" + p[i].fy + "(" + i + ")";
		}
		else {
			s2 = Element.idToName(p[0].element) + ": " + p[0].fx + "-" + p[0].fy;
		}
		final String s = s2;
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
		new SwingWorker("Tcf") {
			public Object construct() {
				Curve[] c = new Curve[p.length];
				double[] data;
				for (int i = 0; i < p.length; i++) {
					data = compute(p[i]);
					if (data == null)
						return null;
					c[i] = new Curve();
					c[i].setLegend(new Legend(Element.idToName(p[i].element) + ":" + p[i].fx + "-" + p[i].fy + "(" + i
							+ ")", 150, 50 + i * 15));
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
					graph.getXAxis().setTitleText("\u03c4 (fs)");
					graph.getYAxis().setTitleText("c(\u03c4)");
					for (int i = 0; i < c.length; i++)
						graph.append(c[i]);
					JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(model.view),
							"Time Correlation Function: " + s, false);
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
							"At least one of the selected TCFs: " + s + " has no data.", "No Data",
							JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}.start();
	}

}