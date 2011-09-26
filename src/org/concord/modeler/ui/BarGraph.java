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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * <p>
 * General purpose bar graph component. Can be used to, for example, display the potential energy, the kinetic energy
 * and the total energy dynamically. Bars can be in either vertical or horizontal direction.
 * </p>
 * 
 * <p>
 * Please supply upper and lower bounds for a bar graph. If a variable being drawn exceeds the bounds, you will see
 * overflow. When a bar is drawn, the absolulte value is used to determine its length. If the value is positive, the bar
 * is drawn above the zero line. Otherwise, it is below it.
 * </p>
 * 
 * @author Charles Xie
 */

public class BarGraph extends JComponent implements SwingConstants {

	protected float multiplier = 1.0f;
	protected float addend;
	protected String format = "Fixed point";
	protected DecimalFormat formatter;

	private Font font;
	private int orientation = VERTICAL;
	protected double value;
	private double min = -1.0, max = 1.0, average;
	private String description;
	private boolean paintTicks = true, paintLabels = true, paintTitle = true;
	private int majorTicks = 5, minorTicks = 10;
	private int[] a, b;
	private boolean averageOnly;

	public BarGraph() {
		setForeground(Color.black);
		formatter = new DecimalFormat("#.#");
		formatter.setMaximumFractionDigits(3);
		formatter.setMaximumIntegerDigits(3);
	}

	public BarGraph(int orientation) {
		this();
		setOrientation(orientation);
	}

	public void setAverageOnly(boolean b) {
		averageOnly = b;
	}

	public boolean getAverageOnly() {
		return averageOnly;
	}

	public void setAddend(float addend) {
		this.addend = addend;
	}

	public float getAddend() {
		return addend;
	}

	public void setMultiplier(float multiplier) {
		this.multiplier = multiplier;
	}

	public float getMultiplier() {
		return multiplier;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Font getFont() {
		return font;
	}

	public void setOrientation(int i) {
		orientation = i;
	}

	public int getOrientation() {
		return orientation;
	}

	public void setDescription(String s) {
		description = s;
		setToolTipText(s);
	}

	public String getDescription() {
		return description;
	}

	public void setValue(double d) {
		value = d * multiplier + addend;
		setToolTipText(description + ": " + formatter.format(value));
	}

	public double getValue() {
		return value;
	}

	public void setAverage(double d) {
		average = d * multiplier + addend;
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

	private Color getMedianColor() {
		return new Color((getBackground().getRed() + getForeground().getRed()) / 2, (getBackground().getGreen() + getForeground().getGreen()) / 2,
				(getBackground().getBlue() + getForeground().getBlue()) / 2);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		Dimension dim = getSize();
		if (isOpaque()) {
			g2.setColor(getBackground());
			g2.fillRect(0, 0, dim.width, dim.height);
		}

		if (font != null)
			g2.setFont(font);

		FontMetrics fm = g2.getFontMetrics();
		int h = fm.getAscent();

		float inverseMajorTicks = 1.0f / majorTicks;

		switch (orientation) {

		case VERTICAL:

			double tubeUnit = dim.height / (max - min);
			int tubeWidth = dim.width - 1;
			if (paintTicks)
				tubeWidth -= 6;
			if (paintLabels)
				tubeWidth -= fm.stringWidth(formatter.toPattern()) + 4;
			if (paintTitle)
				tubeWidth -= h + h + h;
			g2.setColor(getBackground());
			g2.fillRect(0, 0, tubeWidth, dim.height - 1);
			g2.setColor(getForeground());
			int tube = 0;
			int aver = 0;
			if (max * min >= 0.0) {
				if (averageOnly) {
					tube = (int) Math.round((average - min) * tubeUnit);
					g2.fillRect(0, dim.height - tube, tubeWidth, tube);
				}
				else {
					tube = (int) Math.round((value - min) * tubeUnit);
					g2.fillRect(0, dim.height - tube, tubeWidth, tube);
					if (average != 0.0) {
						aver = (int) Math.round((average - min) * tubeUnit);
						g2.setColor(getMedianColor());
						int delta = tubeWidth / 2 > 5 ? 5 : tubeWidth / 2;
						int tubeh = dim.height - aver;
						if (a == null)
							a = new int[3];
						if (b == null)
							b = new int[3];
						a[0] = tubeWidth;
						a[1] = tubeWidth;
						a[2] = tubeWidth - delta;
						b[0] = tubeh - delta;
						b[1] = tubeh + delta;
						b[2] = tubeh;
						g2.fillPolygon(a, b, 3);
						g2.setColor(Color.black);
						g2.drawPolygon(a, b, 3);
					}
				}
			}
			else {
				if (averageOnly) {
					tube = (int) Math.round(average * tubeUnit);
					int zero = (int) Math.round(max * tubeUnit);
					if (tube >= 0) {
						g2.fillRect(0, zero - tube, tubeWidth, tube);
					}
					else {
						g2.fillRect(0, zero, tubeWidth, -tube);
					}
				}
				else {
					tube = (int) Math.round(value * tubeUnit);
					int zero = (int) Math.round(max * tubeUnit);
					if (tube >= 0) {
						g2.fillRect(0, zero - tube, tubeWidth, tube);
					}
					else {
						g2.fillRect(0, zero, tubeWidth, -tube);
					}
					if (average != 0.0) {
						aver = (int) Math.round(average * tubeUnit);
						g2.setColor(getMedianColor());
						int delta = tubeWidth / 2 > 5 ? 5 : tubeWidth / 2;
						int tubeh = zero - aver;
						if (a == null)
							a = new int[3];
						if (b == null)
							b = new int[3];
						a[0] = tubeWidth;
						a[1] = tubeWidth;
						a[2] = tubeWidth - delta;
						b[0] = tubeh - delta;
						b[1] = tubeh + delta;
						b[2] = tubeh;
						g2.fillPolygon(a, b, 3);
						g2.setColor(Color.black);
						g2.drawPolygon(a, b, 3);
					}
				}
			}
			g2.setColor(Color.black);
			g2.drawRect(0, 0, tubeWidth, dim.height - 1);
			if (paintTicks) {
				float dely = (float) (dim.height - 1) / (float) minorTicks;
				int mult = (int) Math.round(minorTicks * inverseMajorTicks);
				if (mult == 0)
					mult = 1;
				for (int i = 0; i <= minorTicks; i++) {
					if (i % mult == 0) {
						g2.drawLine(tubeWidth, (int) Math.round(dely * i), tubeWidth + 4, (int) Math.round(dely * i));
					}
					else {
						g2.drawLine(tubeWidth, (int) Math.round(dely * i), tubeWidth + 2, (int) Math.round(dely * i));
					}
				}
			}
			if (paintLabels) {
				float dely = (dim.height - 1) * inverseMajorTicks;
				String s = formatter.format(min);
				g2.drawString(s, tubeWidth + 8, dim.height - h / 4);
				s = formatter.format(max);
				g2.drawString(s, tubeWidth + 8, h);
				for (int i = 1; i < majorTicks; i++) {
					s = formatter.format(min + i * (max - min) * inverseMajorTicks);
					g2.drawString(s, tubeWidth + 8, dim.height - (int) Math.round(dely * i) + h / 4);
				}
			}
			if (paintTitle) {
				if (description != null) {
					int w = fm.stringWidth(description);
					int sx = dim.width - h - h;
					int sy = (dim.height - w) / 2;
					g2.rotate(Math.PI * 0.5, sx, sy);
					g2.drawString(description, sx, sy);
					g2.rotate(-Math.PI * 0.5, sx, sy);
				}
			}
			break;

		case HORIZONTAL:

			tubeUnit = dim.width / (max - min);
			int tubeHeight = dim.height - 1;
			if (paintTicks)
				tubeHeight -= 6;
			if (paintLabels)
				tubeHeight -= h + h + 4;
			if (paintTitle)
				tubeHeight -= h + h + 4;
			g2.setColor(getBackground());
			g2.fillRect(0, 0, dim.width - 1, tubeHeight);
			g2.setColor(getForeground());
			if (min * max >= 0) {
				if (averageOnly) {
					tube = (int) Math.round((average - min) * tubeUnit);
					g2.fillRect(1, 0, tube, tubeHeight);
				}
				else {
					tube = (int) Math.round((value - min) * tubeUnit);
					g2.fillRect(1, 0, tube, tubeHeight);
					if (average != 0.0) {
						aver = (int) Math.round((average - min) * tubeUnit);
						g2.setColor(getMedianColor());
						int delta = tubeHeight / 2 > 5 ? 5 : tubeHeight / 2;
						if (a == null)
							a = new int[3];
						if (b == null)
							b = new int[3];
						a[0] = aver - delta;
						a[1] = aver + delta;
						a[2] = aver;
						b[0] = tubeHeight;
						b[1] = tubeHeight;
						b[2] = tubeHeight - delta;
						g2.fillPolygon(a, b, 3);
						g2.setColor(Color.black);
						g2.drawPolygon(a, b, 3);
					}
				}
			}
			else {
				if (averageOnly) {
					tube = (int) Math.round(average * tubeUnit);
					int zero = -(int) Math.round(min * tubeUnit);
					if (tube >= 0) {
						g2.fillRect(zero, 0, tube, tubeHeight);
					}
					else {
						g2.fillRect(zero + tube, 0, -tube, tubeHeight);
					}
				}
				else {
					tube = (int) Math.round(value * tubeUnit);
					int zero = -(int) Math.round(min * tubeUnit);
					if (tube >= 0) {
						g2.fillRect(zero, 0, tube, tubeHeight);
					}
					else {
						g2.fillRect(zero + tube, 0, -tube, tubeHeight);
					}
					if (average != 0.0) {
						aver = (int) Math.round(average * tubeUnit);
						g2.setColor(getMedianColor());
						int delta = tubeHeight / 2 > 5 ? 5 : tubeHeight / 2;
						int tubeh = zero + aver;
						if (a == null)
							a = new int[3];
						if (b == null)
							b = new int[3];
						a[0] = tubeh - delta;
						a[1] = tubeh + delta;
						a[2] = tubeh;
						b[0] = tubeHeight;
						b[1] = tubeHeight;
						b[2] = tubeHeight - delta;
						g2.fillPolygon(a, b, 3);
						g2.setColor(Color.black);
						g2.drawPolygon(a, b, 3);
					}
				}
			}
			g2.setColor(Color.black);
			g2.drawRect(0, 0, dim.width - 1, tubeHeight);
			if (paintTicks) {
				float delx = (float) (dim.width - 1) / (float) minorTicks;
				int mult = (int) Math.round(minorTicks * inverseMajorTicks);
				if (mult == 0)
					mult = 1;
				for (int i = 0; i <= minorTicks; i++) {
					if (i % mult == 0) {
						g2.drawLine((int) Math.round(delx * i), tubeHeight, (int) Math.round(delx * i), tubeHeight + 4);
					}
					else {
						g2.drawLine((int) Math.round(delx * i), tubeHeight, (int) Math.round(delx * i), tubeHeight + 2);
					}
				}
			}
			if (paintLabels) {
				float delx = (dim.width - 1) * inverseMajorTicks;
				String s = formatter.format(min);
				int w = fm.stringWidth(s);
				g2.drawString(s, 0, tubeHeight + h + h);
				s = formatter.format(max);
				w = fm.stringWidth(s);
				g2.drawString(s, dim.width - w, tubeHeight + h + h);
				for (int i = 1; i < majorTicks; i++) {
					s = formatter.format(min + i * (max - min) * inverseMajorTicks);
					w = fm.stringWidth(s);
					g2.drawString(s, (int) Math.round(delx * i - w / 2.0), tubeHeight + h + h);
				}
			}
			if (paintTitle) {
				if (description != null) {
					int w = fm.stringWidth(description);
					g2.drawString(description, (dim.width - w - 1) / 2, dim.height - h - 4);
				}
			}
			break;

		}

		if (getBorder() != null)
			paintBorder(g2);

	}

}