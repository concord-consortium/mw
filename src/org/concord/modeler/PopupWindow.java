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
package org.concord.modeler;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;

import org.concord.modeler.draw.CallOut;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.RoundEtchedBorder;

/**
 * @author Charles Xie
 * 
 */
class PopupWindow extends BasicPageTextBox implements CallOut {

	private final static Border EMPTY_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);
	private final static Border INNER_BORDER = BorderFactory
			.createCompoundBorder(new RoundEtchedBorder(), EMPTY_BORDER);
	private final static Color BG_COLOR = new Color(253, 237, 203);
	private Point callout;
	private int[] xPoints = new int[3];
	private int[] yPoints = new int[3];

	PopupWindow(String text, int x, int y, int w, int h, int xCallOut, int yCallOut) {
		super();
		setContentType("text/html");
		setText(text);
		setEditable(false);
		setBackground(BG_COLOR);
		setOpaque(true);
		setBounds(x, y, w, h);
		callout = new Point(xCallOut, yCallOut);
		setBorder(BorderFactory.createCompoundBorder(EMPTY_BORDER, INNER_BORDER));
	}

	public JPopupMenu getPopupMenu() {
		return null;
	}

	public void createPopupMenu() {
	}

	public void setPage(Page p) {
		super.setPage(p);
		cacheImages(p.getPathBase());
	}

	public void setCallOut(int xCallOut, int yCallOut) {
		callout.setLocation(xCallOut, yCallOut);
	}

	Point getCallOut() {
		return callout;
	}

	public void paintCallOut(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		xPoints[0] = callout.x;
		yPoints[0] = callout.y;
		Rectangle rectangle = getBounds();
		int oc = rectangle.outcode(callout.x, callout.y);
		boolean b = true;
		if ((oc & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM) {
			if ((oc & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
				xPoints[1] = rectangle.x;
				yPoints[1] = (int) (rectangle.y + rectangle.height * 0.8);
				xPoints[2] = (int) (rectangle.x + rectangle.width * 0.2);
				yPoints[2] = rectangle.y + rectangle.height;
			}
			else if ((oc & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
				xPoints[1] = rectangle.x + rectangle.width;
				yPoints[1] = (int) (rectangle.y + rectangle.height * 0.8);
				xPoints[2] = (int) (rectangle.x + rectangle.width * 0.8);
				yPoints[2] = rectangle.y + rectangle.height;
			}
			else {
				xPoints[1] = (int) (rectangle.x + rectangle.width * 0.4);
				xPoints[2] = (int) (rectangle.x + rectangle.width * 0.6);
				yPoints[2] = yPoints[1] = rectangle.y + rectangle.height;
			}
		}
		else if ((oc & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP) {
			if ((oc & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
				xPoints[1] = rectangle.x;
				yPoints[1] = (int) (rectangle.y + rectangle.height * 0.2);
				xPoints[2] = (int) (rectangle.x + rectangle.width * 0.2);
				yPoints[2] = rectangle.y;
			}
			else if ((oc & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
				xPoints[1] = (int) (rectangle.x + rectangle.width * 0.8);
				yPoints[1] = rectangle.y;
				xPoints[2] = rectangle.x + rectangle.width;
				yPoints[2] = (int) (rectangle.y + rectangle.height * 0.2);
			}
			else {
				xPoints[1] = (int) (rectangle.x + rectangle.width * 0.4);
				xPoints[2] = (int) (rectangle.x + rectangle.width * 0.6);
				yPoints[2] = yPoints[1] = rectangle.y + 1;
			}
		}
		else if ((oc & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT) {
			xPoints[2] = xPoints[1] = rectangle.x + 1;
			yPoints[1] = (int) (rectangle.y + rectangle.height * 0.4);
			yPoints[2] = (int) (rectangle.y + rectangle.height * 0.6);
		}
		else if ((oc & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT) {
			xPoints[2] = xPoints[1] = rectangle.x + rectangle.width;
			yPoints[1] = (int) (rectangle.y + rectangle.height * 0.4);
			yPoints[2] = (int) (rectangle.y + rectangle.height * 0.6);
		}
		else {
			b = false;
		}
		if (b) {
			g.setColor(getBackground());
			g.fillPolygon(xPoints, yPoints, 3);
		}
	}

}