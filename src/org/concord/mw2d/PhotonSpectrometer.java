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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.concord.modeler.g2d.Curve;
import org.concord.modeler.g2d.XYGrapher;
import org.concord.modeler.util.SwingWorker;
import org.concord.mw2d.models.Photon;

public class PhotonSpectrometer extends JComponent {

	public final static int EMISSION = 0;
	public final static int ABSORPTION = 1;

	private final static float TOLL = 0.01f;
	private final static float ZONE_WIDTH = (Photon.MAX_VISIBLE_FREQ - Photon.MIN_VISIBLE_FREQ) / 7;

	private int type;
	private int ntick = 20;
	private float lowerBound = 0, upperBound = 20;
	private Map<Float, Integer> lineMap;

	public PhotonSpectrometer() {
		lineMap = Collections.synchronizedMap(new TreeMap<Float, Integer>());
		setType(EMISSION);
	}

	public void setType(int i) {
		type = i;
		switch (type) {
		case EMISSION:
			setBackground(Color.black);
			setForeground(Color.white);
			break;
		case ABSORPTION:
			setBackground(Color.white);
			setForeground(Color.black);
			break;
		}
	}

	public int getType() {
		return type;
	}

	public void setNumberOfTicks(int n) {
		ntick = n;
	}

	public int getNumberOfTicks() {
		return ntick;
	}

	public void setLowerBound(float lb) {
		lowerBound = lb;
	}

	public float getLowerBound() {
		return lowerBound;
	}

	public void setUpperBound(float ub) {
		upperBound = ub;
	}

	public float getUpperBound() {
		return upperBound;
	}

	private Float containsFrequency(float freq) {
		if (lineMap.isEmpty())
			return null;
		synchronized (lineMap) {
			for (Float x : lineMap.keySet()) {
				if (Math.abs(x.floatValue() - freq) < TOLL)
					return x;
			}
		}
		return null;
	}

	public void clearLines() {
		lineMap.clear();
		repaint();
	}

	public void receivePhoton(Photon p) {
		if (p == null)
			return;
		Float freq0 = containsFrequency(p.getOmega());
		synchronized (lineMap) {
			if (freq0 == null) {
				lineMap.put(p.getOmega(), 1);
			}
			else {
				Integer o = lineMap.get(freq0);
				if (o != null) {
					lineMap.put(freq0, o + 1);
				}
			}
		}
		repaint();
	}

	private Color getColor(float omega) {
		if (omega <= Photon.MIN_VISIBLE_FREQ || omega >= Photon.MAX_VISIBLE_FREQ)
			return type == ABSORPTION ? Color.black : Color.white;
		float d = (Photon.MAX_VISIBLE_FREQ - Photon.MIN_VISIBLE_FREQ) / 7.0f;
		int i = (int) ((omega - Photon.MIN_VISIBLE_FREQ) / d);
		if (i == 6)
			return Photon.COLOR[i];
		float remainder = (omega - Photon.MIN_VISIBLE_FREQ - i * d) / d;
		int r1 = Photon.COLOR[i].getRed();
		int r2 = Photon.COLOR[i + 1].getRed();
		int g1 = Photon.COLOR[i].getGreen();
		int g2 = Photon.COLOR[i + 1].getGreen();
		int b1 = Photon.COLOR[i].getBlue();
		int b2 = Photon.COLOR[i + 1].getBlue();
		return new Color((int) (r1 + remainder * (r2 - r1)), (int) (g1 + remainder * (g2 - g1)), (int) (b1 + remainder
				* (b2 - b1)));
	}

	private double[] getLineIntensity() {
		double[] intensity = new double[200];
		double delta = (upperBound - lowerBound) * 0.01;
		for (int i = 0; i < 100; i++) {
			intensity[i * 2] = i * delta;
			intensity[i * 2 + 1] = 0;
		}
		int y;
		int k;
		synchronized (lineMap) {
			for (Float x : lineMap.keySet()) {
				y = lineMap.get(x).intValue();
				k = (int) ((x - lowerBound) / delta);
				if (k < 100)
					intensity[k * 2 + 1] += y;
			}
		}
		return intensity;
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Dimension dim = getSize();
		Insets insets = getInsets();

		g.setColor(getBackground());
		int x = insets.left;
		int y = insets.top;
		int w = dim.width - insets.left - insets.right;
		int h = dim.height - insets.top - insets.bottom;
		g.fillRect(x, y, w, h);

		float scale = w / (upperBound - lowerBound);

		if (type == ABSORPTION) {
			float leftEdge = 0;
			float rightEdge = 0;
			float start = 0;
			float end = 0;
			if (lowerBound < Photon.MIN_VISIBLE_FREQ) {
				leftEdge = (Photon.MIN_VISIBLE_FREQ - lowerBound) * scale;
				start = Photon.MIN_VISIBLE_FREQ;
			}
			else {
				leftEdge = x;
				start = lowerBound;
			}
			if (upperBound > Photon.MAX_VISIBLE_FREQ) {
				rightEdge = dim.width - insets.right - (upperBound - Photon.MAX_VISIBLE_FREQ) * scale;
				end = Photon.MAX_VISIBLE_FREQ;
			}
			else {
				rightEdge = w;
				end = upperBound;
			}
			int min = (int) ((start - Photon.MIN_VISIBLE_FREQ) / ZONE_WIDTH);
			int max = (int) ((end - Photon.MIN_VISIBLE_FREQ) / ZONE_WIDTH);
			int del = (int) ((rightEdge - leftEdge) / (max - min - 1));
			float x1 = 0;
			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint(new GradientPaint(leftEdge - del, 0, getBackground(), leftEdge, 0, Photon.COLOR[0]));
			g2.fillRect((int) (leftEdge - del), y, del, h);
			for (int i = min; i < max - 1; i++) {
				x1 = leftEdge + del * (i - min);
				g2.setPaint(new GradientPaint(x1, 0, Photon.COLOR[i], x1 + del, 0, Photon.COLOR[i + 1]));
				g2.fillRect((int) x1, y, del, h);
			}
			g2.setPaint(new GradientPaint(x1 + del, 0, Photon.COLOR[6], x1 + del * 2, 0, getBackground()));
			g2.fillRect((int) (x1 + del), y, del, h);
		}

		g.setColor(Color.gray);
		float d = (float) w / (float) ntick;
		for (int i = 0; i <= ntick; i++) {
			g.drawLine((int) (x + i * d), y + h, (int) (x + i * d), y + h - 5);
		}

		if (!lineMap.isEmpty()) {
			if (type == ABSORPTION) {
				synchronized (lineMap) {
					g.setColor(Color.black);
					for (Float a : lineMap.keySet()) {
						x = (int) (a * scale);
						g.drawLine(x, insets.top, x, dim.height - insets.bottom);
					}
				}
			}
			else {
				synchronized (lineMap) {
					for (Float a : lineMap.keySet()) {
						g.setColor(getColor(a));
						x = (int) (a * scale);
						g.drawLine(x, insets.top, x, dim.height - insets.bottom);
					}
				}
			}
		}

	}

	public int getNumberOfLines() {
		if (lineMap == null || lineMap.isEmpty())
			return 0;
		return lineMap.size();
	}

	public void showLineIntensity() {

		if (lineMap == null || lineMap.isEmpty()) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(PhotonSpectrometer.this),
							"Sorry, no line is found.");
				}
			});
			return;
		}

		final XYGrapher graph = new XYGrapher();
		graph.addSnapshotListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				/*
				 * JComponent c=(JComponent)e.getSource(); Object g=c.getClientProperty("graph"); if(g instanceof
				 * XYGrapher){ notifyPageComponentListeners (new PageComponentEvent(((XYGrapher)g).getGraph(),
				 * PageComponentEvent.SNAPSHOT_TAKEN));
				 */
			}
		});
		new SwingWorker("PhotonSpectrometer:showLineIntensity()") {
			public Object construct() {
				return getLineIntensity();
			}

			public void finished() {
				double[] data = (double[]) getValue();
				graph.getXAxis().setTitleText("Frequency");
				graph.getYAxis().setTitleText("Line Intensity");
				Curve c = new Curve();
				c.setData(data);
				graph.append(c);
				JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(PhotonSpectrometer.this),
						"Line Intensity", false);
				graph.setDialog(dialog);
				dialog.getContentPane().add(graph, BorderLayout.CENTER);
				dialog.setSize(300, 300);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.pack();
				dialog.setLocationRelativeTo(PhotonSpectrometer.this);
				dialog.setVisible(true);
			}
		}.start();

	}

}