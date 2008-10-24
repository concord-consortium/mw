/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;

import javax.swing.JComponent;

/**
 * @author Charles Xie
 */

public class Gauge extends JComponent {

	public final static byte INSTANTANEOUS = 0;
	public final static byte GROWING_POINT_RUNNING_AVERAGE = 1;
	public final static byte SIMPLE_RUNNING_AVERAGE = 2;
	public final static byte EXPONENTIAL_RUNNING_AVERAGE = 3;

	protected String format = "Fixed point";
	protected DecimalFormat formatter;

	private Font font;
	protected double value;
	private double min = -1.0, max = 1.0, average;
	private byte averageType = INSTANTANEOUS;
	private String description;
	private boolean paintTicks = true, paintLabels = true, paintTitle = true;
	private int majorTicks = 10, minorTicks = 20;
	private int majorTickLength = 10, minorTickLength = 5;
	private int margin = 20, panelHeight = 50;
	private Line2D.Float line;
	private BasicStroke thinStroke = new BasicStroke(1.0f);
	private BasicStroke needleStroke = new BasicStroke(2.0f);

	public Gauge() {
		setForeground(Color.black);
		formatter = new DecimalFormat("#.#");
		formatter.setMaximumFractionDigits(3);
		formatter.setMaximumIntegerDigits(3);
		line = new Line2D.Float();
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Font getFont() {
		return font;
	}

	public void setDescription(String s) {
		description = s;
		setToolTipText(s);
	}

	public String getDescription() {
		return description;
	}

	public void setAverageType(byte i) {
		averageType = i;
	}

	public byte getAverageType() {
		return averageType;
	}

	public void setValue(double d) {
		value = d;
	}

	public double getValue() {
		return value;
	}

	public void setAverage(double d) {
		average = d;
	}

	public double getAverage() {
		return average;
	}

	public void setMinimum(double d) {
		min = d;
	}

	public double getMinimum() {
		return min;
	}

	public void setMaximum(double d) {
		max = d;
	}

	public double getMaximum() {
		return max;
	}

	public void setPaintTicks(boolean b) {
		paintTicks = b;
	}

	public boolean getPaintTicks() {
		return paintTicks;
	}

	public void setPaintTitle(boolean b) {
		paintTitle = b;
	}

	public boolean getPaintTitle() {
		return paintTitle;
	}

	public void setPaintLabels(boolean b) {
		paintLabels = b;
	}

	public boolean getPaintLabels() {
		return paintLabels;
	}

	public void setMajorTicks(int n) {
		majorTicks = n;
	}

	public int getMajorTicks() {
		return majorTicks;
	}

	public void setMinorTicks(int n) {
		minorTicks = n;
	}

	public int getMinorTicks() {
		return minorTicks;
	}

	public void setFormat(String format) {
		this.format = format;
		if (format != null) {
			if (format.equalsIgnoreCase("scientific notation")) {
				formatter.applyPattern("0.###E00");
			}
			else if (format.equalsIgnoreCase("fixed point")) {
				formatter.applyPattern("#");
			}
			else {
				formatter.applyPattern("#");
			}
		}
		else {
			formatter.applyPattern("#");
		}
	}

	public String getFormat() {
		return format;
	}

	public void setMaximumFractionDigits(int i) {
		formatter.setMaximumFractionDigits(i);
	}

	public int getMaximumFractionDigits() {
		return formatter.getMaximumFractionDigits();
	}

	public void setMaximumIntegerDigits(int i) {
		formatter.setMaximumIntegerDigits(i);
	}

	public int getMaximumIntegerDigits() {
		return formatter.getMaximumIntegerDigits();
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
		g2.setColor(getBackground());
		g2.fillRect(0, 0, dim.width, dim.height);

		g2.setColor(getForeground());
		if (font != null)
			g2.setFont(font);
		FontMetrics fm = g2.getFontMetrics();
		int w;
		int h = fm.getAscent();
		if (paintTitle) {
			if (description != null) {
				w = fm.stringWidth(description);
				g2.drawString(description, (dim.width - w) / 2, dim.height - panelHeight + h * 2);
			}
		}

		int xc = dim.width / 2;
		int yc = dim.height - panelHeight;

		float r = yc - margin * 2;
		float alpha = dim.width - margin - xc;
		alpha = alpha <= r ? (float) Math.acos(alpha / r) : 0;
		float delta = (float) (Math.PI - 2 * alpha) / minorTicks;
		float cos, sin;

		if (paintTicks) {
			int ntick = minorTicks / majorTicks;
			if (ntick <= 0)
				ntick = 1;

			g2.setStroke(thinStroke);
			String s;
			for (int i = 0; i <= minorTicks; i++) {
				cos = (float) Math.cos(alpha + i * delta);
				sin = (float) Math.sin(alpha + i * delta);
				if (i % ntick == 0) {
					line.x1 = xc + (r + majorTickLength) * cos;
					line.y1 = yc - (r + majorTickLength) * sin;
					line.x2 = xc + r * cos;
					line.y2 = yc - r * sin;
					if (paintLabels) {
						s = formatter.format(max - i * (max - min) / (ntick * majorTicks));
						w = fm.stringWidth(s);
						g2.drawString(s, line.x1 + 10 * cos - w / 2, line.y1 - 10 * sin);
					}
				}
				else {
					line.x1 = xc + (r + minorTickLength) * cos;
					line.y1 = yc - (r + minorTickLength) * sin;
					line.x2 = xc + r * cos;
					line.y2 = yc - r * sin;
				}
				g2.draw(line);
			}
		}

		delta = (float) getAngle(averageType == INSTANTANEOUS ? value : average, alpha);
		cos = (float) Math.cos(delta);
		sin = (float) Math.sin(delta);
		line.x1 = xc + (r - minorTickLength) * cos;
		line.y1 = yc - (r - minorTickLength) * sin;
		line.x2 = xc;
		line.y2 = yc;
		g2.setStroke(needleStroke);
		g2.draw(line);

		g2.setStroke(thinStroke);
		g2.setColor(Color.white);
		g2.fillOval(xc - 4, yc - 4, 8, 8);
		g2.setColor(getForeground());
		g2.drawOval(xc - 4, yc - 4, 8, 8);

		if (getBorder() != null)
			paintBorder(g2);

	}

	private double getAngle(double x, float alpha) {
		return (max - x) / (max - min) * (Math.PI - 2 * alpha) + alpha;
	}

}