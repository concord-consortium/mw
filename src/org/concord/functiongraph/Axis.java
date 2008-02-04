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

package org.concord.functiongraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.text.NumberFormat;

/**
 * Axis of a graph, including tic marks and labels
 * 
 * @author Connie J. Chen
 */

public class Axis implements Drawable {

	public final static int X_AXIS = 0;
	public final static int Y_AXIS = 1;

	public static Color gridMajCol = new Color(204, 204, 204);
	public static Color axisCol = new Color(120, 120, 120);
	public static Color axisLabelBgCol = new Color(255, 255, 255);
	public static Color axisLabelFgCol = new Color(120, 120, 120);

	public static Stroke gridStroke = new BasicStroke(1.0f);
	public static Stroke axisStroke = new BasicStroke(2.0f);

	private int orient = X_AXIS;
	private Graph graph;

	private int length;
	private float min;
	private float max;
	private float majTic = 40.0f;
	private float minTic = 20.0f;
	private final static Font ticFont = new Font("Arial", Font.PLAIN, 9);

	private String title;
	private final static Font titleFont = new Font("Arial", Font.ITALIC | Font.BOLD, 12);

	public Axis(int orient) {
		this.orient = orient;
		if (orient == X_AXIS)
			title = "x";
		else title = "y";
	}

	/** set the title of this axis */
	public void setTitle(String s) {
		title = s;
	}

	/** get the title of this axis */
	public String getTitle() {
		return title;
	}

	/**
	 * set the GUI length of this axis. This method is usually called to make
	 * sure that the length of the axis is identical to the width or height of
	 * the graph (so that the axis will always draw through the graph.
	 */
	public void setLength(int len) {
		length = len;
	}

	/** get the GUI length of this axis */
	public int getLength() {
		return length;
	}

	/** set the lower bound of data range for this axis */
	public synchronized void setMin(float min) {
		this.min = min;
	}

	/** get the lower bound of data range for this axis */
	public synchronized float getMin() {
		return min;
	}

	/** set the upper bound of data range for this axis */
	public synchronized void setMax(float max) {
		this.max = max;
	}

	/** get the upper bound of data range for this axis */
	public synchronized float getMax() {
		return max;
	}

	/** set the data interval between major tic marks */
	public void setMajorTic(float tic) {
		majTic = tic;
	}

	/** get the data interval between major tick marks */
	public float getMajorTic() {
		return majTic;
	}

	/** set the data interval between minor tic marks */
	public void setMinorTic(float tic) {
		minTic = tic;
	}

	/** get the data interval between minor tick marks */
	public float getMinorTic() {
		return minTic;
	}

	/**
	 * set the graph this axis is associated with. This method MUST be called in
	 * order for the axis to know how to coordinate itself in the graph.
	 */
	public void setGraph(Graph graph) {
		this.graph = graph;
	}

	/** paint this axis onto the passed graphics */
	public void draw(Graphics g) {

		if (graph == null)
			return;

		int w = graph.getWidth();
		int h = graph.getHeight();
		Point origin = graph.getOrigin();
		Rectangle bound = graph.getBounds();

		int leftMargin = bound.x;
		int rightMargin = w - bound.width - leftMargin;
		int topMargin = bound.y;
		int bottomMargin = h - bound.height - topMargin;
		int minTic, maxTic, tic;

		if (orient == X_AXIS) { // draw x-axis
			((Graphics2D) g).setStroke(gridStroke);
			minTic = (int) (min / majTic);
			maxTic = (int) (max / majTic);
			int oy = origin.y;
			if (oy < topMargin) {
				oy = topMargin;
			}
			else if (oy > length - bottomMargin) {
				oy = length - bottomMargin;
			}
			for (int i = minTic; i <= maxTic; i++) {
				if (i != 0) {
					tic = (int) (origin.x + majTic * i * graph.getXScale());
					g.setColor(gridMajCol);
					g.drawLine(tic, oy, tic, h - bottomMargin);
					g.drawLine(tic, oy, tic, topMargin);
					g.setColor(axisLabelFgCol);
					// use NumberFormat to format tic marks
					g.setFont(ticFont);
					if (origin.y >= h - bottomMargin - 10) {
						g.drawString(NumberFormat.getInstance().format(i * majTic), tic - 5, h - bottomMargin - 8);
					}
					else if (origin.y <= topMargin) {
						g.drawString(NumberFormat.getInstance().format(i * majTic), tic - 5, topMargin + 15);
					}
					else {
						g.drawString(NumberFormat.getInstance().format(i * majTic), tic - 5, origin.y + 15);
					}
				}
			}
			g.setColor(axisCol);
			int[] xPts = { leftMargin + 3, leftMargin + 15, leftMargin + 15 };
			int[] yPts = { oy, oy + 5, oy - 5 };
			g.fillPolygon(xPts, yPts, 3);
			xPts[0] = length - rightMargin - 3;
			xPts[1] = length - rightMargin - 15;
			xPts[2] = xPts[1];
			g.fillPolygon(xPts, yPts, 3);
			((Graphics2D) g).setStroke(axisStroke);
			g.drawLine(0, oy, length, oy);
			g.setFont(titleFont);
			g.drawString(title, length - rightMargin - 15, oy - 10);
		}
		else { // draw y-axis
			((Graphics2D) g).setStroke(gridStroke);
			maxTic = (int) (max / majTic);
			minTic = (int) (min / majTic);
			int ox = origin.x;
			if (origin.x < leftMargin) {
				ox = leftMargin;
			}
			else if (origin.x > length - rightMargin) {
				ox = length - rightMargin;
			}
			for (int i = minTic; i <= maxTic; i++) {
				if (i != 0) {
					tic = (int) (origin.y - majTic * i * graph.getYScale());
					g.setColor(gridMajCol);
					g.drawLine(ox, tic, w - rightMargin, tic);
					g.drawLine(ox, tic, leftMargin, tic);
					g.setColor(axisLabelFgCol);
					// use NumberFormat to format tic marks
					g.setFont(ticFont);
					if (origin.x >= w - rightMargin) {
						g.drawString(NumberFormat.getInstance().format(i * majTic), w - rightMargin - 20, tic + 5);
					}
					else if (origin.x <= leftMargin + 20) {
						g.drawString(NumberFormat.getInstance().format(i * majTic), leftMargin + 5, tic + 5);
					}
					else {
						g.drawString(NumberFormat.getInstance().format(i * majTic), origin.x - 20, tic + 5);
					}
				}
			}
			g.setColor(axisCol);
			int[] xPts = { ox, ox + 5, ox - 5 };
			int[] yPts = { topMargin + 3, topMargin + 15, topMargin + 15 };
			g.fillPolygon(xPts, yPts, 3);
			yPts[0] = length - bottomMargin - 3;
			yPts[1] = length - bottomMargin - 15;
			yPts[2] = yPts[1];
			g.fillPolygon(xPts, yPts, 3);
			((Graphics2D) g).setStroke(axisStroke);
			g.drawLine(ox, topMargin, ox, topMargin + length);
			g.setFont(titleFont);
			g.drawString(title, ox + 10, topMargin + 15);
		}
	}
}