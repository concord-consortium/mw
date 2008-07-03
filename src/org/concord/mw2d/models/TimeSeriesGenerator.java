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

class TimeSeriesGenerator {

	final static byte LAC = 0x01;
	final static byte LAT = 0x02;
	final static byte LAP = 0x03;

	private AtomicModel model;

	final static class Parameter {

		byte type;
		byte[] elements;
		Rectangle2D area;

		Parameter(byte type, byte[] elements, Rectangle2D area) {
			this.type = type;
			this.elements = elements;
			this.area = area;
		}

		boolean containsElement(byte element) {
			for (int i = 0; i < elements.length; i++) {
				if (element == elements[i])
					return true;
			}
			return false;
		}

		public String toString() {
			String s = "";
			for (int i = 0; i < elements.length; i++)
				s += elements[i] + ", ";
			return s + "(" + (int) (area.getX()) + ", " + (int) (area.getY()) + ", " + (int) (area.getWidth()) + ", "
					+ (int) (area.getHeight()) + ")";
		}

	}

	TimeSeriesGenerator(AtomicModel model) {
		this.model = model;
	}

	private double[] compute(Parameter p) {
		int m = model.getTapePointer();
		if (m <= 0)
			return null;
		double[] result = new double[m];
		int noa = model.getNumberOfAtoms();
		Atom at;
		float rx, ry, vx, vy, ax, ay;
		int count;
		float volume = (float) (0.01 * p.area.getWidth() * p.area.getHeight());
		for (int i = 0; i < m; i++) {
			count = 0;
			for (int k = 0; k < noa; k++) {
				at = model.atom[k];
				if (!p.containsElement((byte) at.getID()))
					continue;
				rx = at.getRxRyQueue().getQueue1().getData(i);
				ry = at.getRxRyQueue().getQueue2().getData(i);
				if (p.area.contains(rx, ry)) {
					switch (p.type) {
					case LAC:
						result[i]++;
						break;
					case LAT:
						vx = at.getVxVyQueue().getQueue1().getData(i);
						vy = at.getVxVyQueue().getQueue2().getData(i);
						result[i] += at.mass * (vx * vx + vy * vy);
						count++;
						break;
					case LAP:
						vx = at.getVxVyQueue().getQueue1().getData(i);
						vy = at.getVxVyQueue().getQueue2().getData(i);
						ax = at.getAxAyQueue().getQueue1().getData(i);
						ay = at.getAxAyQueue().getQueue2().getData(i);
						result[i] += at.mass * ((vx * vx + vy * vy) * 2 * MDModel.EV_CONVERTER + rx * ax + ry * ay);
						break;
					}
				}
			}
			switch (p.type) {
			case LAT:
				result[i] *= MDModel.EV_CONVERTER * MDModel.UNIT_EV_OVER_KB / count;
				break;
			case LAP:
				result[i] *= 80 * 1.66667 / volume;
				break;
			// NOTE: This prefactor is for reconsiling with the pressure gauge
			}
		}
		double[] f = new double[m + m];
		for (int i = 0; i < m; i++) {
			f[i + i] = i * model.getTimeStep() * model.movieUpdater.getInterval();
			f[i + i + 1] = result[i];
		}
		return f;
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
		new SwingWorker("Time Series Generator") {
			public Object construct() {
				Curve[] c = new Curve[p.length];
				double[] data;
				for (int i = 0; i < p.length; i++) {
					data = compute(p[i]);
					if (data == null)
						return null;
					c[i] = new Curve();
					c[i].setLegend(new Legend(p[i] + "(" + i + ")", 100, 50 + i * 15));
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
					graph.getXAxis().setTitleText("t (fs)");
					for (int i = 0; i < c.length; i++)
						graph.append(c[i]);
					String title = "Graph";
					switch (p[0].type) {
					case LAC:
						title = "Local Area Number of Atoms vs. Time";
						graph.getYAxis().setTitleText("N");
						break;
					case LAT:
						title = "Local Area Temperature vs. Time";
						graph.getYAxis().setTitleText("T (K)");
						break;
					case LAP:
						title = "Local Area Pressure vs. Time";
						graph.getYAxis().setTitleText("P (bar)");
						break;
					}
					JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(model.view), title, false);
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
							"At least one of the selections results in no data.", "No Data",
							JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}.start();
	}

}