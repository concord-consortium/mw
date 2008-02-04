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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import javax.swing.JComponent;

class NeighborListAdjustor extends JComponent implements MouseListener, MouseMotionListener {

	private final static Font font = new Font("Arial", Font.PLAIN, 10);
	private int width, height;

	private float rCutOff = 3.0f, rList = 3.5f;
	private int diff;
	private float spacing = 20.0f;
	private int xcen, ycen;

	private Rectangle ctrl1 = new Rectangle(0, 0, 10, 10);
	private Rectangle ctrl2 = new Rectangle(0, 0, 10, 10);
	private Ellipse2D circle1 = new Ellipse2D.Float();
	private Ellipse2D circle2 = new Ellipse2D.Float();

	private GeneralPath vr1 = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);
	private GeneralPath vr2 = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);

	private int x, y;
	private int selected = 1;
	private boolean pressOut = false;

	private int xmin, ymax;
	private int rectx, recty;

	private final static Stroke thinStroke = new BasicStroke(1.0f);
	private final static Stroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
			new float[] { 2.0f }, 0.0f);

	private AtomicModel model;

	public NeighborListAdjustor(AtomicModel mod) {
		model = mod;
		setPreferredSize(new Dimension(250, 250));
		setRequestFocusEnabled(true);
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public int getSelected() {
		return selected;
	}

	public float getCutOff() {
		return rCutOff;
	}

	public void setCutOff(float f) {
		rCutOff = f;
	}

	public float getRList() {
		return rList;
	}

	public void setRList(float f) {
		rList = f;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Dimension dim = getSize();
		width = dim.width;
		height = dim.height;
		xcen = width / 2;
		ycen = height / 2;
		xmin = width / 2 + (int) spacing;
		ymax = height - 20;

		double var6 = 0.0, var12 = 0.0;
		vr1.moveTo(xcen, 0);
		vr2.moveTo(xcen, 0);
		for (int i = 15; i < 75; i++) {
			var6 = (spacing / i);
			var6 = var6 * var6 * var6;
			var6 = var6 * var6;
			var12 = var6 * var6;
			vr1.lineTo(xcen + i, (float) (ycen * (1.0 - var12 + var6)));
			vr2.lineTo(xcen - i, (float) (ycen * (1.0 - var12 + var6)));
		}

		g2.setColor(Color.white);
		g2.fillRect(0, 0, width, height);

		g2.setColor(Color.lightGray);
		float radius = 2.0f;
		float diameter = radius + radius;
		int xgrid = (int) (width / spacing);
		int ygrid = (int) (height / spacing);
		for (int i = -xgrid; i < xgrid; i++) {
			for (int j = -ygrid; j < ygrid; j++) {
				if (xcen + i * spacing > radius && xcen + i * spacing < width - radius && ycen + j * spacing > radius
						&& ycen + j * spacing < height - radius) {
					g2.fillOval((int) (xcen + i * spacing - radius), (int) (ycen + j * spacing - radius),
							(int) diameter, (int) diameter);
				}
			}
		}

		g2.setColor(Color.gray);
		g2.fillRect((int) (xcen - spacing), 0, (int) (spacing + spacing), ycen);

		g2.setColor(Color.black);
		g2.drawLine(10, ycen, width - 10, ycen);
		g2.drawLine(xcen, 10, xcen, height - 10);
		g2.setColor(Color.gray);
		g2.draw(vr1);
		g2.draw(vr2);

		int r1 = (int) (rCutOff * spacing);
		int r2 = (int) (rList * spacing);
		diff = r2 - r1;
		rectx = r1 + xcen;
		recty = r2 + ycen;
		ctrl1.setLocation(rectx - 5, ycen - 5);
		ctrl2.setLocation(xcen - 5, recty - 5);
		circle1.setFrame(xcen - r1, ycen - r1, r1 + r1, r1 + r1);
		circle2.setFrame(xcen - r2, ycen - r2, r2 + r2, r2 + r2);

		g2.setColor(Color.black);
		g2.setStroke(thinStroke);
		g2.draw(circle1);
		g2.setStroke(dashed);
		g2.draw(circle2);

		g2.setColor(Color.red);
		g2.setStroke(thinStroke);
		g2.draw(ctrl1);
		g2.draw(ctrl2);

		g2.setFont(font);
		g2.setColor(Color.black);
		g2.drawString("Cutoff Radius = " + rCutOff + " x Atomic Radius", 20, height - 20);
		g2.drawString("Neighbor List Radius = " + rList + " x Atomic Radius", 20, height - 10);

	}

	public void mousePressed(MouseEvent e) {
		x = e.getX();
		y = e.getY();
		if (ctrl1.contains(x, y)) {
			x = ctrl1.x - e.getX();
			y = ctrl1.y - e.getY();
			selected = 1;
			updateLocation(e);
		}
		else if (ctrl2.contains(x, y)) {
			x = ctrl2.x - e.getX();
			y = ctrl2.y - e.getY();
			selected = 2;
			updateLocation(e);
		}
		else {
			selected = -1;
			pressOut = true;
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (ctrl1.contains(e.getX(), e.getY()) || ctrl2.contains(e.getX(), e.getY())) {
			updateLocation(e);
			// if(selected==1) model.setCutOffMatrix(rCutOff);
			// else if(selected==2) model.setListMatrix(rList);
		}
		else {
			pressOut = false;
		}
	}

	public void mouseMoved(MouseEvent e) {
		x = e.getX();
		y = e.getY();
		if (ctrl1.contains(x, y) || ctrl2.contains(x, y)) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}
		else {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (!pressOut)
			updateLocation(e);
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void updateLocation(MouseEvent e) {
		if (selected == 1) {
			int ex = e.getX();
			if (ex < xmin) {
				rectx = xmin;
			}
			else if (ex > circle2.getMaxX()) {
				rectx = (int) circle2.getMaxX();
			}
			else {
				rectx = x + ex;
			}
			ctrl1.setLocation(rectx - 5, (int) (ctrl1.getY()));
			int newRadius = rectx - xcen;
			circle1.setFrame(xcen - newRadius, ycen - newRadius, newRadius + newRadius, newRadius + newRadius);
			recty = (int) ctrl1.getX() + diff - 5;
			ctrl2.setLocation((int) ctrl2.getX(), recty - 5);
			newRadius += diff;
			circle2.setFrame(xcen - newRadius, ycen - newRadius, newRadius + newRadius, newRadius + newRadius);
			rCutOff = (rectx - xcen) / spacing;
			model.setCutOffMatrix(rCutOff);
		}
		else if (selected == 2) {
			int ey = e.getY();
			if (ey < rectx) {
				recty = rectx;
			}
			else if (ey > ymax) {
				recty = ymax;
			}
			else {
				recty = y + ey;
			}
			ctrl2.setLocation((int) (ctrl2.getX()), recty - 5);
			int newRadius = recty - ycen;
			circle2.setFrame(xcen - newRadius, ycen - newRadius, newRadius + newRadius, newRadius + newRadius);
			diff = (recty - ycen) - (rectx - xcen);
			rList = (recty - ycen) / spacing;
			model.setListMatrix(rList);
		}
		repaint();
	}

}