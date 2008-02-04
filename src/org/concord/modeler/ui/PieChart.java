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

package org.concord.modeler.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.text.NumberFormat;

import javax.swing.JComponent;

public class PieChart extends JComponent {

	private NumberFormat format;
	private float[] percent;
	private Color[] colors;
	private String[] legends;
	private String[] text;
	private Ellipse2D.Float[] hotspots;
	private Arc2D[] arcs;
	private Rectangle bound;
	private static Font font = new Font("Verdana", Font.PLAIN, 10);
	private Point2D endPoint;
	private int iselected = -1;
	private float total;

	/** construct a pie chart without specifying the partitions */
	protected PieChart() {
		setPreferredSize(new Dimension(200, 150));
		format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(1);
		bound = new Rectangle(20, 20, 80, 80);
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				processMousePressedEvent(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				processMouseMovedEvent(e);
			}

			public void mouseDragged(MouseEvent e) {
				processMouseDraggedEvent(e);
			}
		});
	}

	/** equipartition a pie chart with <tt>n</tt> arcs. */
	public PieChart(int n) {
		this();
		percent = new float[n];
		colors = new Color[n];
		legends = new String[n];
		arcs = new Arc2D.Float[n];
		int ic;
		for (int i = 0; i < n; i++) {
			percent[i] = 1.0f / n;
			ic = (int) (255.0f * percent[i] * i);
			colors[i] = new Color(ic, ic, ic);
			legends[i] = String.valueOf(i + 1);
			arcs[i] = new Arc2D.Float();
		}
		hotspots = new Ellipse2D.Float[n - 1];
		for (int i = 0; i < hotspots.length; i++)
			hotspots[i] = new Ellipse2D.Float(0, 0, 8, 8);
	}

	/** construct a pie chart with the given data. */
	public PieChart(float[] p, Color[] c, String[] s) {
		this();
		percent = p;
		colors = c;
		legends = s;
		arcs = new Arc2D.Float[p.length];
		for (int i = 0; i < arcs.length; i++)
			arcs[i] = new Arc2D.Float();
		hotspots = new Ellipse2D.Float[p.length - 1];
		for (int i = 0; i < hotspots.length; i++)
			hotspots[i] = new Ellipse2D.Float(0, 0, 8, 8);
	}

	/** set the frame rectangle of this pie chart */
	public void setPieFrame(float x, float y, float w, float h) {
		bound.setRect(x, y, w, h);
	}

	public void setPercent(float[] p) {
		percent = p;
	}

	public void setPercent(int i, float x) {
		percent[i] = x;
	}

	public float getPercent(int i) {
		return percent[i];
	}

	/** set the <tt>i</tt>-th arc's color to be <tt>c</tt> */
	public void setColor(int i, Color c) {
		colors[i] = c;
	}

	/** @return the <tt>i</tt>-th arc's color */
	public Color getColor(int i) {
		return colors[i];
	}

	public void setText(int i, String s) {
		if (text == null)
			text = new String[percent.length];
		text[i] = s;
	}

	public String getText(int i) {
		if (text == null)
			return null;
		return text[i];
	}

	public void setTotal(float total) {
		this.total = total;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		Dimension dim = getSize();
		int width = dim.width;
		int height = dim.height;
		g2.setColor(getBackground());
		g2.fillRect(0, 0, width, height);
		g2.setFont(font);

		bound.setRect(width / 10, height / 10, width / 3, width / 3);
		int r = bound.x + bound.width + 20;
		int s = bound.y + 10;

		int legendX1 = width / 8;
		int legendX2 = width / 4;

		float t = 0.0f;
		int n = percent.length;
		for (int i = 0; i < n; i++) {
			g2.setColor(colors[i]);
			arcs[i].setArc(bound, t, percent[i] * 360.0f, Arc2D.PIE);
			if (isEnabled()) {
				if (i < n - 1) {
					endPoint = arcs[i].getEndPoint();
					hotspots[i].x = (float) (endPoint.getX() - hotspots[i].width * 0.5);
					hotspots[i].y = (float) (endPoint.getY() - hotspots[i].height * 0.5);
				}
			}
			g2.fill(arcs[i]);
			g2.fillRect(r, s + i * 20, 20, 10);
			g2.setColor(Color.black);
			g2.draw(arcs[i]);
			g2.drawRect(r, s + i * 20, 20, 10);
			g2.drawString(legends[i], r + legendX1, s + 10 + i * 20);
			if (total == 0) {
				g2.drawString(format.format(percent[i] * 100.0) + "%", r + legendX2, s + 10 + i * 20);
			}
			else {
				g2.drawString(format.format(percent[i] * 100.0) + "% (" + format.format(percent[i] * total) + ")", r
						+ legendX2, s + 10 + i * 20);
			}
			t += percent[i] * 360.0f;
		}

		if (isEnabled()) {
			for (int i = 0; i < hotspots.length; i++) {
				g2.setColor(Color.white);
				g2.fill(hotspots[i]);
				g2.setColor(Color.black);
				g2.draw(hotspots[i]);
			}
		}

		if (text != null) {
			g2.setColor(Color.black);
			for (int i = 0; i < legends.length; i++) {
				if (text[i] != null)
					g2.drawString(text[i], bound.x, bound.y + bound.height + 30 + 20 * i);
			}
		}

	}

	protected void processMousePressedEvent(MouseEvent e) {
		if (!isEnabled())
			return;
		int x = e.getX();
		int y = e.getY();
		boolean b = false;
		iselected = -1;
		for (int i = 0, n = hotspots.length; i < n; i++) {
			b = hotspots[i].contains(x, y);
			if (b) {
				iselected = i;
				break;
			}
		}
	}

	protected void processMouseDraggedEvent(MouseEvent e) {
		if (!isEnabled())
			return;
		if (iselected == -1)
			return;
		int x = e.getX();
		int y = e.getY();
		int xc = bound.x + bound.width / 2;
		int yc = bound.y + bound.height / 2;
		double d2 = Math.sqrt((x - xc) * (x - xc) + (y - yc) * (y - yc));
		double angle = Math.toDegrees(y < yc ? Math.acos((x - xc) / d2) : 2 * Math.PI - Math.acos((x - xc) / d2));
		if (angle < 5)
			angle = 0;
		else if (angle > 355)
			angle = 360;
		if (percent.length > 2) {
			double ub;
			if (iselected == percent.length - 2) {
				ub = 360;
			}
			else {
				ub = arcs[iselected + 2].getAngleStart();
			}
			double lb;
			if (iselected == 0) {
				lb = 0;
			}
			else {
				lb = arcs[iselected - 1].getAngleStart() + arcs[iselected - 1].getAngleExtent();
			}
			if (angle > ub) {
				angle = ub;
			}
			else if (angle < lb) {
				angle = lb;
			}
			percent[iselected] = (float) ((angle - arcs[iselected].getAngleStart()) / 360);
			percent[iselected + 1] = 1;
			for (int i = 0; i < percent.length; i++) {
				if (i != iselected + 1)
					percent[iselected + 1] -= percent[i];
			}
		}
		else if (percent.length == 2) {
			percent[iselected] = (float) (angle / 360);
			percent[iselected + 1] = 1.0f - percent[iselected];
		}
		repaint();
	}

	protected void processMouseMovedEvent(MouseEvent e) {
		if (!isEnabled())
			return;
		int x = e.getX();
		int y = e.getY();
		boolean b = false;
		for (int i = 0, n = hotspots.length; i < n; i++) {
			b = hotspots[i].contains(x, y);
			if (b)
				break;
		}
		setCursor(b ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor
				.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

}
