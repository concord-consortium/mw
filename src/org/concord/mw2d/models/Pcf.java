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

/**
 * compute the pair correlation function (PCF) for the specified pair of elements. Pair correlation functions are also
 * known as radial distribution functions (RDFs). In 2D, the formula used to compute the PCFs are:
 * 
 * <pre>
 *  g(r)=S/(2*pi*r*dr*N&circ;2)sum_i sum_j delta(r-r_ij)
 * </pre>
 */

class Pcf {

	private AtomicModel model;
	private short nslice = 100;

	final static class Parameter {

		byte element1;
		byte element2;
		int length;
		Rectangle2D area;

		Parameter(byte element1, byte element2, int length, Rectangle2D area) {
			this.element1 = element1;
			this.element2 = element2;
			this.length = length;
			this.area = area;
		}

		public String toString() {
			return element1 + ", " + element2 + ", " + length + ", " + area;
		}

	}

	Pcf(AtomicModel model) {
		this.model = model;
	}

	void setNSlice(short nslice) {
		this.nslice = nslice;
	}

	private double[] compute(Parameter parameter) {
		int m = model.getTapePointer();
		if (m <= 0)
			return null;
		int n1 = 0, n2 = 0;
		double rxi, ryi, rxj, ryj, dxij, dyij;
		boolean doAreaDetection = !parameter.area.equals(model.boundary);
		if (parameter.element1 != parameter.element2) {
			for (int i = 0; i < model.numberOfAtoms; i++) {
				if (model.atom[i].getID() != parameter.element1 && model.atom[i].getID() != parameter.element2)
					continue;
				if (doAreaDetection) {
					for (int k = 0; k < m; k++) {
						rxi = model.atom[i].getRxRyQueue().getQueue1().getData(k);
						ryi = model.atom[i].getRxRyQueue().getQueue2().getData(k);
						if (parameter.area.contains(rxi, ryi)) {
							if (model.atom[i].getID() == parameter.element1)
								n1++;
							else if (model.atom[i].getID() == parameter.element2)
								n2++;
						}
					}
				}
				else {
					if (model.atom[i].getID() == parameter.element1)
						n1 += m;
					else if (model.atom[i].getID() == parameter.element2)
						n2 += m;
				}
			}
		}
		else {
			for (int i = 0; i < model.numberOfAtoms; i++) {
				if (model.atom[i].getID() != parameter.element1)
					continue;
				if (doAreaDetection) {
					for (int k = 0; k < m; k++) {
						rxi = model.atom[i].getRxRyQueue().getQueue1().getData(k);
						ryi = model.atom[i].getRxRyQueue().getQueue2().getData(k);
						if (parameter.area.contains(rxi, ryi))
							n1++;
					}
				}
				else {
					n1 += m;
				}
			}
			n2 = n1;
		}
		if (n1 == 0 || n2 == 0)
			return null;
		double slice = (double) parameter.length / (double) nslice;
		double inverseSlice = 1.0 / slice;
		double[] rdf = new double[nslice];
		int ir;
		double xbox = model.boundary.width;
		double ybox = model.boundary.height;
		if (parameter.element1 != parameter.element2) {
			for (int i = 0; i < model.numberOfAtoms; i++) {
				for (int j = i + 1; j < model.numberOfAtoms; j++) {
					if ((model.atom[i].getID() == parameter.element1 && model.atom[j].getID() == parameter.element2)
							|| (model.atom[i].getID() == parameter.element2 && model.atom[j].getID() == parameter.element1)) {
						for (int k = 0; k < m; k++) {
							rxi = model.atom[i].getRxRyQueue().getQueue1().getData(k);
							ryi = model.atom[i].getRxRyQueue().getQueue2().getData(k);
							if (doAreaDetection && !parameter.area.contains(rxi, ryi))
								continue;
							rxj = model.atom[j].getRxRyQueue().getQueue1().getData(k);
							ryj = model.atom[j].getRxRyQueue().getQueue2().getData(k);
							dxij = rxi - rxj;
							dyij = ryi - ryj;
							switch (model.boundary.getType()) {
							case RectangularBoundary.PBC_ID:
								if (dxij > xbox * 0.5) {
									dxij -= xbox;
								}
								if (dxij <= -xbox * 0.5) {
									dxij += xbox;
								}
								if (dyij > ybox * 0.5) {
									dyij -= ybox;
								}
								if (dyij <= -ybox * 0.5) {
									dyij += ybox;
								}
								break;
							case RectangularBoundary.XRYPBC_ID:
								if (dyij > ybox * 0.5) {
									dyij -= ybox;
								}
								if (dyij <= -ybox * 0.5) {
									dyij += ybox;
								}
								break;
							case RectangularBoundary.XPYRBC_ID:
								if (dxij > xbox * 0.5) {
									dxij -= xbox;
								}
								if (dxij <= -xbox * 0.5) {
									dxij += xbox;
								}
								break;
							}
							ir = (int) (Math.hypot(dxij, dyij) * inverseSlice);
							if (ir < nslice)
								rdf[ir]++;
						}
					}
				}
			}
		}
		else {
			for (int i = 0; i < model.numberOfAtoms; i++) {
				if (model.atom[i].getID() == parameter.element1) {
					for (int j = i + 1; j < model.numberOfAtoms; j++) {
						if (model.atom[j].getID() == parameter.element2) {
							for (int k = 0; k < m; k++) {
								rxi = model.atom[i].getRxRyQueue().getQueue1().getData(k);
								ryi = model.atom[i].getRxRyQueue().getQueue2().getData(k);
								if (doAreaDetection && !parameter.area.contains(rxi, ryi))
									continue;
								rxj = model.atom[j].getRxRyQueue().getQueue1().getData(k);
								ryj = model.atom[j].getRxRyQueue().getQueue2().getData(k);
								// if(doAreaDetection && !parameter.area.contains(rxj, ryj)) continue;
								dxij = rxi - rxj;
								dyij = ryi - ryj;
								/* minimum image conventions */
								switch (model.boundary.getType()) {
								case RectangularBoundary.PBC_ID:
									if (dxij > xbox * 0.5) {
										dxij -= xbox;
									}
									if (dxij <= -xbox * 0.5) {
										dxij += xbox;
									}
									if (dyij > ybox * 0.5) {
										dyij -= ybox;
									}
									if (dyij <= -ybox * 0.5) {
										dyij += ybox;
									}
									break;
								case RectangularBoundary.XRYPBC_ID:
									if (dyij > ybox * 0.5) {
										dyij -= ybox;
									}
									if (dyij <= -ybox * 0.5) {
										dyij += ybox;
									}
									break;
								case RectangularBoundary.XPYRBC_ID:
									if (dxij > xbox * 0.5) {
										dxij -= xbox;
									}
									if (dxij <= -xbox * 0.5) {
										dxij += xbox;
									}
									break;
								}
								ir = (int) (Math.hypot(dxij, dyij) * inverseSlice);
								if (ir < nslice)
									rdf[ir]++;
							}
						}
					}
				}
			}
		}
		double[] f = new double[nslice + nslice];
		/* normalize: */
		double v = parameter.element1 == parameter.element2 ? m * parameter.area.getWidth()
				* parameter.area.getHeight() / (Math.PI * n1 * n2 * slice * slice) : m * parameter.area.getWidth()
				* parameter.area.getHeight() / (2 * Math.PI * n1 * n2 * slice * slice);
		for (int i = 0; i < nslice; i++) {
			f[i + i] = i * slice * 0.1;
			f[i + i + 1] = rdf[i] * v / (i + 1);
		}
		return f;
	}

	void show(final Parameter[] parameter) {
		String s2 = "";
		if (parameter.length > 1) {
			for (int i = 0; i < parameter.length - 1; i++) {
				s2 += Element.idToName(parameter[i].element1) + "-" + Element.idToName(parameter[i].element2) + "(" + i
						+ "), ";
			}
			s2 += Element.idToName(parameter[parameter.length - 1].element1) + "-"
					+ Element.idToName(parameter[parameter.length - 1].element2) + "(" + (parameter.length - 1) + ")";
		}
		else {
			s2 = Element.idToName(parameter[0].element1) + "-" + Element.idToName(parameter[0].element2);
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
		new SwingWorker() {
			public Object construct() {
				Curve[] c = new Curve[parameter.length];
				double[] data;
				for (int i = 0; i < parameter.length; i++) {
					data = compute(parameter[i]);
					if (data == null)
						return null;
					c[i] = new Curve();
					c[i].setLegend(new Legend(Element.idToName(parameter[i].element1) + "-"
							+ Element.idToName(parameter[i].element2) + "(" + i + ")", 200, 50 + i * 15));
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
					graph.getXAxis().setTitleText("r (\u00c5)");
					graph.getYAxis().setTitleText("g(r)");
					for (int i = 0; i < c.length; i++)
						graph.append(c[i]);
					JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(model.view),
							"Pair Correlation Function: " + s, false);
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
							"At least one of the selected PCFs: " + s + " has no data.", "No Data",
							JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}.start();
	}

}