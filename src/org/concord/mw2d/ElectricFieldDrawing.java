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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;

import javax.swing.JComponent;

class ElectricFieldDrawing extends JComponent {

	private GeneralPath line;
	private double amplitude;
	private double frequency;
	private double dc;
	private float scale = 40;
	private int margin = 5;

	public ElectricFieldDrawing(double dc, double amplitude, double frequency) {
		this.dc = dc;
		this.amplitude = amplitude;
		this.frequency = frequency;
		setBackground(Color.white);
	}

	public void setFunction(double dc, double amplitude, double frequency) {
		this.dc = dc;
		this.amplitude = amplitude;
		this.frequency = frequency;
	}

	public void setDC(double dc) {
		this.dc = dc;
	}

	public void setAmplitude(double amplitude) {
		this.amplitude = amplitude;
	}

	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		int width = getWidth() - margin * 2;
		int height = getHeight() - margin * 2;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.white);
		g2.fillRect(margin, margin, width, height);

		int delta = 10;
		int nx = width / delta;
		int ny = height / delta;
		g2.setColor(Color.lightGray);
		g2.setStroke(ViewAttribute.THIN);
		for (int i = 0; i <= nx; i++) {
			g2.drawLine(margin + delta * i, margin, margin + delta * i, height + margin);
		}
		for (int i = 0; i <= ny; i++) {
			g2.drawLine(margin, margin + delta * i, width + margin, margin + delta * i);
		}

		g2.setColor(Color.gray);
		if (line == null)
			line = new GeneralPath(GeneralPath.WIND_EVEN_ODD, width / 2);
		else line.reset();

		int middle = margin + height / 2 - (int) (dc * scale);
		int i = margin;
		line.moveTo(i, (int) (middle - amplitude * scale * Math.sin(frequency * i)));
		for (i = margin; i < width + margin; i++) {
			line.lineTo(i, (int) (middle - amplitude * scale * Math.sin(frequency * i)));
		}
		g2.draw(line);

	}

}