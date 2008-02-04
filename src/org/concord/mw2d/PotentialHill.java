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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;

import javax.swing.JComponent;

import org.concord.modeler.draw.PatternFactory;
import org.concord.mw2d.models.Reaction;

public class PotentialHill extends JComponent implements MouseListener, MouseMotionListener {

	private float scale, v1 = -1.0f, v2 = -1.0f, max = 0.1f, heat = 0.2f * max;
	private int ymin, ymax;
	private String thermic = "Exothermic";
	private ControlPoint p1;
	private Color barrierColor = new Color(255, 204, 0);
	private Dimension dim;
	private GeneralPath line;
	private String owner;
	private Reaction reaction;
	private static DecimalFormat format;

	public PotentialHill() {
		p1 = new ControlPoint();
		line = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);
		if (format == null)
			format = new DecimalFormat("##.##");
		addMouseListener(this);
		addMouseMotionListener(this);
		setToolTipText("<html>Colliding atoms must gain enough energy to<br>surmount the barrier for reactions in both<br>the forward and back directions. If the<br>colliding energy is not enough, the atoms<br>will bounce off without reacting.</html>");
	}

	public void setColor(Color c) {
		barrierColor = c;
	}

	public Color getColor() {
		return barrierColor;
	}

	public void setForeground(Color c) {
		super.setForeground(c);
		p1.setColor(c);
	}

	public void setPreferredSize(Dimension d) {
		int h0 = getPreferredSize().height;
		super.setPreferredSize(d);
		if (h0 == 0)
			return;
		v1 = (v1 * d.height) / h0;
	}

	public void setEnabled(boolean b) {
		super.setEnabled(b);
		setForeground(b ? Color.black : Color.gray);
	}

	public void setMaximumDepth(float m) {
		if (m < 0.0f)
			throw new IllegalArgumentException("Potential well depth cannot be negative");
		v1 *= max / m;
		max = m;
		if (format != null) {
			if (max >= 1000f)
				format.applyPattern("####");
			else if (max >= 100f)
				format.applyPattern("###");
			else if (max >= 10f)
				format.applyPattern("##.#");
			else if (max >= 1f)
				format.applyPattern("#.##");
			else if (max >= 0.1f)
				format.applyPattern("#.###");
			else if (max >= 0.01f)
				format.applyPattern("#.####");
			else if (max >= 0.001f)
				format.applyPattern("#.#####");
		}
	}

	public float getMaximumDepth() {
		return max;
	}

	public float getActivationEnergy() {
		return scale * v1;
	}

	public void setActivationEnergy(float d) {
		if (scale <= 0.0f) {
			if (isPreferredSizeSet()) {
				Dimension dim = getPreferredSize();
				ymin = dim.height / 4;
				ymax = dim.height * 9 / 10;
				scale = max / (ymax - ymin);
			}
			else {
				System.err.println("Preferred size not set");
				return;
			}
		}
		v1 = d / scale;
		if (isShowing())
			repaint();
	}

	public void setReactionHeat(float d) {
		if (scale <= 0.0f) {
			if (isPreferredSizeSet()) {
				Dimension dim = getPreferredSize();
				ymin = dim.height / 4;
				ymax = dim.height * 9 / 10;
				scale = max / (ymax - ymin);
			}
			else {
				System.err.println("Preferred size not set");
				return;
			}
		}
		v2 = v1 + d / scale;
		heat = d;
		thermic = heat < 0 ? "Endothermic" : heat == 0 ? "Isothermic" : "Exothermic";
		if (isShowing())
			repaint();
	}

	public void setOwner(String s) {
		owner = s;
	}

	public String getOwner() {
		return owner;
	}

	public void setReaction(Reaction reaction) {
		this.reaction = reaction;
	}

	public Reaction getReaction() {
		return reaction;
	}

	protected boolean isHandleSelected() {
		return p1.isSelected();
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (!isEnabled())
			return;
		int a = e.getX();
		int b = e.getY();
		p1.setSelected(p1.within(a, b));
		e.consume();
	}

	public void mouseReleased(MouseEvent e) {
		if (!isEnabled())
			return;
		if (p1.isSelected()) {
			repaint();
			p1.setSelected(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		e.consume();
	}

	public void mouseDragged(MouseEvent e) {
		if (!isEnabled())
			return;
		if (p1.isSelected()) {
			int y = e.getY();
			if (y > ymax)
				y = ymax;
			if (y > ymin) {
				v1 = y - ymin;
			}
			else {
				v1 = 0;
			}
			repaint();
		}
		e.consume();
	}

	public void mouseMoved(MouseEvent e) {
		if (!isEnabled())
			return;
		int a = e.getX();
		int b = e.getY();
		setCursor(p1.within(a, b) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor
				.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		e.consume();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		dim = getSize();

		if (isOpaque()) {
			g2.setColor(getBackground());
			g2.fillRect(0, 0, dim.width, dim.height);
		}

		g2.setColor(getForeground());
		g2.setStroke(ViewAttribute.THICK);

		int period = dim.width / 2;
		int zerox = dim.width / 10;
		int zeroy = dim.height / 10;

		ymin = dim.height / 4;
		ymax = dim.height - zeroy;
		scale = max / (ymax - ymin);

		if (v1 < 0.0f)
			v1 = dim.height * 0.1f;
		v2 = v1 + heat / scale;

		g2.setStroke(ViewAttribute.THIN);

		line.reset();
		line.moveTo(zerox, ymin + v1);

		int i;
		double alpha = Math.PI * 2.0 / period;
		for (i = period / 2; i < period; i++) {
			line.lineTo(i, ymin + 0.5f * v1 - (float) (Math.cos(alpha * i) * v1 * 0.5));
		}
		for (i = period; i < period / 2 * 3 + 1; i++) {
			line.lineTo(i, ymin + 0.5f * v2 - (float) (Math.cos(alpha * i) * v2 * 0.5));
		}
		line.lineTo(dim.width - zerox, ymin + v2);
		line.lineTo(dim.width - zerox, dim.height - zeroy);
		line.lineTo(zerox, dim.height - zeroy);
		line.closePath();
		Color c = isEnabled() ? barrierColor : getForeground().brighter();
		g2.setPaint(PatternFactory.createPattern(PatternFactory.DENSITY50, 4, 4, Color.white, c));
		g2.fill(line);

		g2.setColor(getForeground());
		line.reset();
		line.moveTo(zerox, ymin + v1);
		for (i = period / 2; i < period; i++) {
			line.lineTo(i, ymin + 0.5f * v1 - (float) (Math.cos(alpha * i) * v1 * 0.5));
		}
		for (i = period; i < period / 2 * 3 + 1; i++) {
			line.lineTo(i, ymin + 0.5f * v2 - (float) (Math.cos(alpha * i) * v2 * 0.5));
		}
		line.lineTo(dim.width - zerox, ymin + v2);
		g2.draw(line);
		g2.drawLine(zerox, zeroy, zerox, dim.height - zeroy);
		g2.drawLine(zerox, zeroy, zerox - 2, zeroy + 5);
		g2.drawLine(zerox, zeroy, zerox + 2, zeroy + 5);
		g2.drawLine(zerox, dim.height - zeroy, dim.width - 10, dim.height - zeroy);
		g2.drawLine(dim.width - 10, dim.height - zeroy, dim.width - 15, dim.height - zeroy - 2);
		g2.drawLine(dim.width - 10, dim.height - zeroy, dim.width - 15, dim.height - zeroy + 2);

		int p1x = period / 2 - 10;
		int p1y = (int) (ymin + v1);
		int p2x = period + p1x + 20;

		g2.setFont(ViewAttribute.LITTLE_FONT);
		if (reaction != null)
			g.drawString(reaction.toString(), 5, 10);
		g2.setColor(getForeground());

		FontMetrics fm = g2.getFontMetrics();
		int w = fm.stringWidth("Reaction Coordinate");
		int sx = (dim.width - w) / 2;
		int sy = dim.height - 5;
		g2.drawString("Reaction Coordinate", sx, sy);

		w = fm.stringWidth(thermic);
		g2.drawString(thermic, (int) ((period - w) * 0.5f), ymin - 5);
		g2.drawString("Chemical Potential = " + format.format(heat), period / 2, ymin - 25);

		g2.drawLine(p1x - 5, ymin, (p1x + p2x) / 2, ymin);
		w = fm.stringWidth("Activation Energy");
		sx = p1x + 4;
		sy = (int) ((p1y + ymin) * 0.5f);
		g2.drawString("Activation Energy", sx, sy);
		int hs = fm.getHeight();
		String s = format.format(getActivationEnergy()) + " eV";
		sy += hs;
		sx += (w - fm.stringWidth(s)) / 2;
		g2.drawString(s, sx, sy);
		g2.drawLine(p1x, p1y - ControlPoint.PT_SIZE, p1x, ymin);
		g2.drawLine(p1x, p1y - ControlPoint.PT_SIZE, p1x - 2, p1y - 2 - ControlPoint.PT_SIZE);
		g2.drawLine(p1x, p1y - ControlPoint.PT_SIZE, p1x + 2, p1y - 2 - ControlPoint.PT_SIZE);
		g2.drawLine(p1x, ymin, p1x - 2, ymin + 2);
		g2.drawLine(p1x, ymin, p1x + 2, ymin + 2);

		p1.x = p1x;
		p1.y = p1y;
		p1.paint(g2);

		w = fm.stringWidth("Energy");
		sx = zerox - fm.getHeight() / 2;
		sy = (dim.height + w) / 2;
		g2.rotate(Math.PI * 1.5, sx, sy);
		g2.drawString("Energy", sx, sy);
		g2.rotate(-Math.PI * 1.5, sx, sy);

	}

}