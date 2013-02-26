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

	private float x = 200; // center x
	private float y = 50; // center y
	private float length = 200; // length
	private float lead = 20;
	private float pitch = 10;
	private float diameter = 20;
	private int turnCount = 10;
	private float rotation;

	private GeneralPath path;
	private Line2D.Float line;
	private Color color = Color.BLACK;
	private BasicStroke stroke = new BasicStroke(2);
	private BasicStroke axisStroke = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 1.5f }, 0.0f);
	private int resolution = 20;
	private boolean drawAxis = true;

	public CoilSpring(float rotation) {
		this.rotation = rotation;
	}

	public CoilSpring(float x, float y, float length) {
		setShape(x, y, length);
	}

	public void setShape(float x, float y, float length) {
		this.x = x;
		this.y = y;
		this.length = length;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
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

	public void setLead(float lead) {
		this.lead = lead;
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

		if (rotation != 0) {
			g2.rotate(rotation, x, y);
		}

		g2.setColor(color);

		float x1 = x - length * 0.5f;
		float x2 = x + length * 0.5f;
		if (line == null)
			line = new Line2D.Float();
		if (drawAxis) {
			g2.setStroke(axisStroke);
			line.setLine(x1, y, x2, y);
			g2.draw(line);
		}

		g2.setStroke(stroke);
		if (path == null)
			path = new GeneralPath();
		else
			path.reset();
		double rx, ry;
		int n = resolution * turnCount;
		double delta = (length - lead * 2) / (n + 1);
		double theta = 2 * Math.PI / resolution;
		double angle;
		for (int i = 0; i < n + 1; i++) {
			angle = Math.PI - theta * i;
			rx = x1 + lead + i * delta + pitch * Math.cos(angle) + pitch;
			ry = y + diameter * Math.sin(angle);
			if (i == 0)
				path.moveTo(rx, ry);
			else
				path.lineTo(rx, ry);
		}
		g2.draw(path);

		if (lead > 0) {
			line.setLine(x1, y, x1 + lead, y);
			g2.draw(line);
			line.setLine(x2, y, x2 - lead, y);
			g2.draw(line);
		}

		g2.setColor(oldColor);
		g2.setStroke(oldStroke);

		if (rotation != 0) {
			g2.rotate(-rotation, x, y);
		}

	}

}