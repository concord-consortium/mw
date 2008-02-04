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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

import org.concord.mw2d.MDView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.ViewAttribute;

class MeasuringTool {

	private List<Object> list;
	private MDModel model;
	private Particle owner;
	private static int[] xPoints = new int[4];
	private static int[] yPoints = new int[4];
	private final static Font FONT12 = new Font("Arial", Font.PLAIN, 12);

	public MeasuringTool(Particle owner) {
		this.owner = owner;
		list = new ArrayList<Object>();
	}

	public void setModel(MDModel model) {
		this.model = model;
	}

	public void clear() {
		list.clear();
	}

	public boolean hasNoMeasurement() {
		return list.isEmpty();
	}

	public void addMeasurement(int index) {
		if (index < 0)
			return;
		list.add(index);
	}

	public void addMeasurement(Point point) {
		if (point == null)
			return;
		list.add(point);
	}

	public void setMeasurement(int i, Object o) {
		if (o == null)
			list.remove(i);
		else list.set(i, o);
	}

	public void removeMeasurement(int i) {
		list.remove(i);
	}

	List getMeasurements() {
		return list;
	}

	public int getMeasurement(int x, int y) {
		if (list.isEmpty())
			return -1;
		for (Object o : list) {
			if (o instanceof Integer) {
				if (model != null) {
					Particle p = model.getParticle(((Integer) o).intValue());
					if (x >= p.rx - 4 && x <= p.rx + 4 && y >= p.ry - 4 && y <= p.ry + 4)
						return list.indexOf(o);
				}
			}
			else if (o instanceof Point) {
				Point p = (Point) o;
				if (x >= p.x - 4 && x <= p.x + 4 && y >= p.y - 4 && y <= p.y + 4)
					return list.indexOf(o);
			}
		}
		return -1;
	}

	public void render(Graphics2D g) {
		if (list.isEmpty())
			return;
		Stroke oldStroke = g.getStroke();
		Color oldColor = g.getColor();
		Color color = ((MDView) model.getView()).contrastBackground();
		int ex = 0, ey = 0, sw, sh, xc, yc;
		boolean b;
		String s;
		g.setFont(FONT12);
		for (Object o : list) {
			b = false;
			if (o instanceof Integer) {
				Particle p = model.getParticle(((Integer) o).intValue());
				ex = (int) p.rx;
				ey = (int) p.ry;
				b = true;
			}
			else if (o instanceof Point) {
				ex = ((Point) o).x;
				ey = ((Point) o).y;
				b = true;
			}
			if (b) {
				g.setColor(color);
				g.setStroke(ViewAttribute.DASHED);
				g.drawLine((int) owner.rx, (int) owner.ry, ex, ey);
				s = Particle.format.format(0.01 * Math.sqrt((owner.rx - ex) * (owner.rx - ex) + (owner.ry - ey)
						* (owner.ry - ey)))
						+ "nm";
				sw = g.getFontMetrics().stringWidth(s);
				sh = g.getFontMetrics().getHeight();
				xc = (int) ((owner.rx + ex) * 0.5);
				yc = (int) ((owner.ry + ey) * 0.5);
				g.setStroke(ViewAttribute.THIN);
				g.setColor(new Color(0xccffffff & model.getView().getBackground().darker().getRGB(), true));
				g.fillRect(xc - 5, yc - sh, sw + 10, sh + 6);
				g.setColor(color);
				g.drawRect(xc - 5, yc - sh, sw + 10, sh + 6);
				g.drawString(s, xc, yc);
				if (owner.isSelected() && ((MDView) model.getView()).getAction() == UserAction.MEAS_ID) {
					g.setColor(Color.green);
					xPoints[0] = ex - 4;
					yPoints[0] = ey;
					xPoints[1] = ex;
					yPoints[1] = ey + 4;
					xPoints[2] = ex + 4;
					yPoints[2] = ey;
					xPoints[3] = ex;
					yPoints[3] = ey - 4;
					g.fillPolygon(xPoints, yPoints, 4);
					g.setColor(color);
					g.drawPolygon(xPoints, yPoints, 4);
				}
				else {
					g.fillOval(ex - 2, ey - 2, 4, 4);
				}
			}
		}
		g.setStroke(oldStroke);
		g.setColor(oldColor);
	}

}