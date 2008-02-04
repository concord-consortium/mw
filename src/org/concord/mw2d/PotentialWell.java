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

public class PotentialWell extends JComponent implements MouseListener, MouseMotionListener {

	private float scale;
	private float max = 1.0f;
	private float v = -1.0f;
	private int ymax, ymin;
	private ControlPoint p;
	private Dimension dim;
	private GeneralPath line;
	private static DecimalFormat format;
	private String owner;
	private Reaction reaction;
	private Color barrierColor = new Color(255, 204, 0);

	public PotentialWell() {
		p = new ControlPoint();
		line = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);
		if (format == null)
			format = new DecimalFormat("##.##");
		addMouseListener(this);
		addMouseMotionListener(this);
		setToolTipText("<html>An amount of energy is needed to break a chemical<br>bond. This energy is called the dissociation energy.<br>Conversely, when a bond of the same type is made,<br>the molecule evolves(releases) the same amount of<br>energy.</html>");
	}

	public void setColor(Color c) {
		barrierColor = c;
	}

	public Color getColor() {
		return barrierColor;
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

	public void setPreferredSize(Dimension d) {
		int h0 = getPreferredSize().height;
		super.setPreferredSize(d);
		if (h0 == 0)
			return;
		v = (v * d.height) / h0;
	}

	public void setForeground(Color c) {
		super.setForeground(c);
		p.setColor(c);
	}

	public void setEnabled(boolean b) {
		super.setEnabled(b);
		setForeground(b ? Color.black : Color.gray);
	}

	public void setMaximumDepth(float m) {
		if (m < 0.0f)
			throw new IllegalArgumentException("Potential well depth cannot be negative");
		v *= max / m;
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
		repaint();
	}

	public float getMaximumDepth() {
		return max;
	}

	public float getDissociationEnergy() {
		return scale * v;
	}

	public void setDissociationEnergy(float d) {
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
		v = d / scale;
		if (isShowing())
			repaint();
	}

	protected boolean isHandleSelected() {
		return p.isSelected();
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
		p.setSelected(p.within(a, b));
		e.consume();
	}

	public void mouseReleased(MouseEvent e) {
		if (!isEnabled())
			return;
		if (p.isSelected()) {
			p.setSelected(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			repaint();
		}
		e.consume();
	}

	public void mouseDragged(MouseEvent e) {
		if (!isEnabled())
			return;
		if (p.isSelected()) {
			int y = e.getY();
			if (y > ymax)
				y = ymax;
			if (y > ymin) {
				v = y - ymin;
			}
			else {
				v = 0;
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
		setCursor(p.within(a, b) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor
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

		if (v < 0.0f)
			v = dim.height * 0.3333f;

		if (isOpaque()) {
			g2.setColor(getBackground());
			g2.fillRect(0, 0, dim.width, dim.height);
		}

		g2.setColor(getForeground());

		g2.setFont(ViewAttribute.LITTLE_FONT);
		if (reaction != null)
			g.drawString(reaction.toString(), 5, 10);

		g2.setStroke(ViewAttribute.THIN);

		int period = dim.width / 2;
		int zerox = dim.width / 10;
		int zeroy = dim.height / 10;

		ymin = dim.height / 4;
		ymax = dim.height - zeroy;
		scale = max / (ymax - ymin);

		line.reset();
		line.moveTo(zerox, ymin + v);

		int i;
		double alpha = Math.PI / period;
		for (i = period / 2; i < period / 2 * 3 + 1; i++) {
			line.lineTo(i, ymin + v * 0.5f + (float) (Math.sin(alpha * i) * v * 0.5));
		}
		line.lineTo(dim.width - zerox, ymin);
		line.lineTo(dim.width - zerox, dim.height - zeroy);
		line.lineTo(zerox, dim.height - zeroy);
		line.closePath();
		Color c = isEnabled() ? barrierColor : getForeground().brighter();
		g2.setPaint(PatternFactory.createPattern(PatternFactory.DENSITY50, 4, 4, Color.white, c));
		g2.fill(line);

		g2.setColor(getForeground());
		line.reset();
		line.moveTo(zerox, ymin + v);
		for (i = period / 2; i < period / 2 * 3 + 1; i++) {
			line.lineTo(i, ymin + v * 0.5f + (float) (Math.sin(alpha * i) * v * 0.5));
		}
		line.lineTo(dim.width - zerox, ymin);
		g2.draw(line);
		g2.drawLine(zerox, zeroy, zerox, dim.height - zeroy);
		g2.drawLine(zerox, zeroy, zerox - 2, zeroy + 5);
		g2.drawLine(zerox, zeroy, zerox + 2, zeroy + 5);
		g2.drawLine(zerox, dim.height - zeroy, dim.width - 10, dim.height - zeroy);
		g2.drawLine(dim.width - 10, dim.height - zeroy, dim.width - 15, dim.height - zeroy - 2);
		g2.drawLine(dim.width - 10, dim.height - zeroy, dim.width - 15, dim.height - zeroy + 2);

		p.x = period / 2;
		p.y = (int) (ymin + v);
		p.paint(g);

		FontMetrics fm = g2.getFontMetrics();
		int w = fm.stringWidth("Reaction Coordinate");
		int sx = (dim.width - w) / 2;
		int sy = dim.height - 5;
		g2.drawString("Reaction Coordinate", sx, sy);

		int temp = p.y - ControlPoint.PT_SIZE;
		if (temp > ymin) {
			g2.drawLine(p.x, temp, p.x, ymin);
			g2.drawLine(p.x, temp, p.x - 2, temp - 2);
			g2.drawLine(p.x, temp, p.x + 2, temp - 2);
			g2.drawLine(p.x, ymin, p.x - 2, ymin + 2);
			g2.drawLine(p.x, ymin, p.x + 2, ymin + 2);
		}
		g2.setStroke(ViewAttribute.THIN_DASHED);
		g2.drawLine(zerox, ymin, 3 * period / 2, ymin);
		g2.setStroke(ViewAttribute.THICK);
		w = fm.stringWidth("Dissociation energy");
		sx = p.x + 4;
		sy = (p.y + ymin) / 2;
		g2.drawString("Dissociation energy", sx, sy);
		String s = format.format(getDissociationEnergy()) + " eV";
		sy += fm.getHeight() + 2;
		sx += (w - fm.stringWidth(s)) / 2;
		g2.drawString(s, sx, sy);

		w = fm.stringWidth("Energy");
		sx = zerox - fm.getHeight() / 2;
		sy = (dim.height + w) / 2;
		g2.rotate(Math.PI * 1.5, sx, sy);
		g2.drawString("Energy", sx, sy);
		g2.rotate(-Math.PI * 1.5, sx, sy);

	}

}