/*
 *   Copyright (C) 2013  The Concord Consortium, Inc.,
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

package org.concord.modeler.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

/**
 * This class draws a coil spring.
 * 
 * @author Charles Xie
 * 
 */

public class CoilSpring {

	private Color color = Color.BLACK;
	private BasicStroke stroke = new BasicStroke(2);
	private BasicStroke axisStroke = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 1.5f }, 0.0f);
	private float x1 = 50, y1 = 50, x2 = 250, y2 = 50;
	private float pitch = 10;
	private float diameter = 20;
	private float offset = diameter;
	private int turnCount = 10;
	private int resolution = 20;
	private boolean drawAxis = true;
	private GeneralPath path;
	private Line2D.Float line;

	public CoilSpring() {
	}

	public CoilSpring(float x1, float y1, float x2, float y2) {
		setEndPoints(x1, y1, x2, y2);
	}

	public void setEndPoints(float x1, float y1, float x2, float y2) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	public void setDrawAxis(boolean drawAxis) {
		this.drawAxis = drawAxis;
	}

	public void setTurnCount(int turnCount) {
		this.turnCount = turnCount;
	}

	public void setResolution(int resolution) {
		this.resolution = resolution;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public void setDiameter(float diameter) {
		this.diameter = diameter;
	}

	public void setOffset(float offset) {
		this.offset = offset;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setStroke(BasicStroke stroke) {
		this.stroke = stroke;
	}

	public void paint(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		Color oldColor = g2.getColor();
		Stroke oldStroke = g2.getStroke();

		g2.setColor(color);

		float dx = x2 - x1;
		float dy = y2 - y1;
		double length = Math.hypot(dx, dy);
		dx /= length;
		dy /= length;

		if (line == null)
			line = new Line2D.Float();
		if (drawAxis) {
			g2.setStroke(axisStroke);
			line.setLine(x1, y1, x2, y2);
			g2.draw(line);
		}

		g2.setStroke(stroke);
		if (path == null)
			path = new GeneralPath();
		else
			path.reset();
		double x, y;
		int n = resolution * turnCount;
		double delta = (length - offset * 2) / (n + 1);
		double theta = 2 * Math.PI / resolution;
		double angle, cos, sin;
		for (int i = 0; i < n + 1; i++) {
			angle = Math.PI - theta * i;
			cos = Math.cos(angle);
			sin = Math.sin(angle);
			x = x1 + (offset + i * delta) * dx + pitch * cos + pitch;
			y = y1 + (offset + i * delta) * dy + diameter * sin;
			if (i == 0)
				path.moveTo(x, y);
			else
				path.lineTo(x, y);
		}
		g2.draw(path);

		if (offset > 0) {
			line.setLine(x1, y1, x1 + offset * dx, y1 + offset * dy);
			g2.draw(line);
			line.setLine(x2, y2, x2 - offset * dx, y2 - offset * dy);
			g2.draw(line);
		}

		g2.setColor(oldColor);
		g2.setStroke(oldStroke);

	}

}