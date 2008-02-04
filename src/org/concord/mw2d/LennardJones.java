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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Draw a Lennard-Jones function graph that can be used to adjust the parameters graphically.
 * 
 * @author Qian Xie
 */

class LennardJones extends JComponent implements MouseListener, MouseMotionListener, KeyListener {

	private final static int NMAX = 250;
	private final static float LJ_CONSTANT = (float) Math.pow(2.0, 1.0 / 6.0);
	private final static int ZERO_POS = 100;
	private final static Color SEMITRANSPARENT_BLUE = new Color(50, 50, 255, 128);

	private final static DecimalFormat format = new DecimalFormat("0.###");
	private final static Stroke thinDashed = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.f,
			new float[] { 2.f }, 0.f);
	private final static Stroke moderateStroke = new BasicStroke(2.0f);
	private final static Stroke thinStroke = new BasicStroke(1.0f);

	private ElementEditor editor;
	private float vr[] = new float[NMAX];
	private float vr2[] = new float[NMAX];
	private float sigma, epsilon;
	private float xUnit, yUnit;

	private int width, height;
	private boolean firstTime = true;
	private Rectangle ctrl = new Rectangle(0, 0, 8, 8);
	private boolean pressOut;
	private int x, y;
	private int imin, xmin, ymin, istart;
	private int indent;
	private float interval;
	private static float division = 6 * NMAX;
	private boolean hate;
	private GeneralPath vrline;

	LennardJones(ElementEditor editor) {
		this.editor = editor;
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		setBackground(Color.white);
		setBorder(BorderFactory.createLoweredBevelBorder());
		vrline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 50);
	}

	public void setLJFunction(double sigma1, double epsilon1) {
		sigma = (float) sigma1;
		epsilon = (float) epsilon1;
		drawFunction();
		try {
			xmin = (int) (imin * xUnit);
			ymin = (int) (ZERO_POS - vr[imin] * yUnit);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			xmin = 30;
			ymin = 30;
		}
		ctrl.setLocation(xmin - 4, ymin - 4);
	}

	private void drawFunction() {
		int h = getHeight();
		if (h == 0)
			h = getPreferredSize().height;
		int w = getWidth();
		if (w == 0)
			w = getPreferredSize().width;
		indent = 0;
		interval = w / division;
		imin = (int) (LJ_CONSTANT * sigma / interval) - indent;
		if (imin < 0)
			imin = 0;
		float distance;
		float sr2, sr6, sr12;
		for (int i = 0; i < NMAX; i++) {
			distance = (i + indent) * interval;
			sr2 = sigma * sigma / (distance * distance);
			sr6 = sr2 * sr2 * sr2;
			sr12 = sr6 * sr6;
			vr[i] = 4 * epsilon * (sr12 - sr6);
			vr2[i] = 4 * epsilon * sr6;
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		width = getSize().width;
		height = getSize().height;
		yUnit = height;
		xUnit = (float) width / (float) NMAX;

		g2.setColor(Color.white);
		g2.fillRect(0, 0, width, height);
		g2.setColor(Color.black);
		g2.setStroke(thinStroke);

		// draw x axis
		String title = "R";
		g2.drawLine(20, ZERO_POS, width - 1, ZERO_POS);
		g2.drawLine(width - 2, ZERO_POS, width - 7, ZERO_POS - 5);
		g2.drawLine(width - 2, ZERO_POS, width - 7, ZERO_POS + 5);
		int sw = SwingUtilities.computeStringWidth(g2.getFontMetrics(), title);
		g2.drawString(title, width - sw - 5, ZERO_POS + 20);

		// draw x ticks and labels
		int nx;
		for (int i = 1; i < 6; i++) {
			nx = (int) ((10 * i / interval - indent) * xUnit);
			g2.setColor(Color.lightGray);
			g2.setStroke(thinDashed);
			g2.drawLine(nx, 1, nx, height - 1);
			g2.setColor(Color.black);
			sw = SwingUtilities.computeStringWidth(g2.getFontMetrics(), i + ".0") / 2;
			g2.drawString(i + ".0", nx - sw, ZERO_POS - 10);
		}

		// draw y axis
		g2.setColor(Color.black);
		g2.setStroke(thinStroke);
		String s = MDView.getInternationalText("PotentialEnergy");
		title = s != null ? s : "Potential energy (eV)";
		g2.drawLine(20, 5, 20, height - 5);
		g2.drawLine(20, 5, 15, 10);
		g2.drawLine(20, 5, 25, 10);
		sw = SwingUtilities.computeStringWidth(g2.getFontMetrics(), title) / 2;
		g2.rotate(Math.PI * 0.5, 6, ZERO_POS - sw);
		g2.drawString(title, 6, ZERO_POS - sw);
		g2.rotate(-Math.PI * 0.5, 6, ZERO_POS - sw);

		// draw y ticks and labels
		for (int i = -1; i < 4; i++) {
			nx = (int) (0.2 * i * yUnit + ZERO_POS);
			if (i != 0) {
				g2.setColor(Color.lightGray);
				g2.setStroke(thinDashed);
				g2.drawLine(1, nx, width - 1, nx);
			}
			if (i > 0) {
				g2.setColor(Color.black);
				sw = SwingUtilities.computeStringWidth(g2.getFontMetrics(), "-0." + (i * 2)) / 2;
				g2.drawString("-0." + (i * 2), 30, nx + 4);
			}
		}

		try {
			xmin = (int) (imin * xUnit);
			ymin = (int) (ZERO_POS - vr[imin] * yUnit);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			xmin = 30;
			ymin = 30;
		}

		g2.setStroke(moderateStroke);
		g2.setColor(Color.black);
		vrline.reset();

		switch (editor.getSelectedTabIndex()) {
		case 0:
			hate = false;
			break;
		case 1:
			hate = editor.getRepulsive("Nt", "Pl");
			break;
		case 2:
			hate = editor.getRepulsive("Nt", "Ws");
			break;
		case 3:
			hate = editor.getRepulsive("Nt", "Ck");
			break;
		case 4:
			hate = editor.getRepulsive("Pl", "Ws");
			break;
		case 5:
			hate = editor.getRepulsive("Pl", "Ck");
			break;
		case 6:
			hate = editor.getRepulsive("Ws", "Ck");
			break;
		}

		if (epsilon > 0.0) {
			if (!hate) {
				for (int i = 0; i < NMAX - 1; i++) {
					if (ZERO_POS - vr[i] * yUnit <= 0 && ZERO_POS - vr[i + 1] * yUnit > 0)
						istart = i;
				}
				vrline.moveTo((int) ((istart - 1) * xUnit), (int) (ZERO_POS - vr[istart] * yUnit));
				for (int i = istart; i < NMAX; i++) {
					vrline.lineTo((int) (i * xUnit), (int) (ZERO_POS - vr[i] * yUnit));
				}
			}
			else {
				for (int i = 0; i < NMAX - 1; i++) {
					if (ZERO_POS - vr2[i] * yUnit <= 0 && ZERO_POS - vr2[i + 1] * yUnit > 0)
						istart = i;
				}
				vrline.moveTo((int) ((istart - 1) * xUnit), (int) (ZERO_POS - vr2[istart] * yUnit));
				for (int i = istart; i < NMAX - 1; i++) {
					vrline.lineTo((int) (i * xUnit), (int) (ZERO_POS - vr2[i] * yUnit));
				}
			}
			g2.draw(vrline);
		}
		else {
			g2.setColor(Color.gray);
			g2.fillRect(width / 2 - 70, height / 2 - 20, 140, 40);
			g2.setColor(Color.red);
			s = MDView.getInternationalText("NoInteraction");
			if (s == null)
				s = "No interaction!";
			g2.drawString(s, (width - g2.getFontMetrics().stringWidth(s)) / 2, height / 2);
		}

		g2.setColor(Color.lightGray);
		g2.fillRoundRect(getPreferredSize().width > 0 ? getPreferredSize().width - 130 : 200, 10, 110, 50, 10, 10);
		g2.setColor(Color.black);
		g2.setStroke(thinStroke);
		g2.drawRoundRect(getPreferredSize().width > 0 ? getPreferredSize().width - 130 : 200, 10, 110, 50, 10, 10);
		g2.setColor(Color.black);
		int xlabel = getPreferredSize().width > 0 ? getPreferredSize().width - 125 : 205;
		switch (editor.getSelectedInteraction()) {
		case 1:
			s = "Nt-Nt";
			break;
		case 2:
			s = "Pl-Pl";
			break;
		case 3:
			s = "Ws-Ws";
			break;
		case 4:
			s = "Ck-Ck";
			break;
		case 5:
			s = "Nt-Pl";
			break;
		case 6:
			s = "Nt-Ws";
			break;
		case 7:
			s = "Nt-Ck";
			break;
		case 8:
			s = "Pl-Ws";
			break;
		case 9:
			s = "Pl-Ck";
			break;
		case 10:
			s = "Ws-Ck";
			break;
		}
		String s2 = MDView.getInternationalText("Parameter");
		if (s != null)
			g2.drawString(s + " " + (s2 != null ? s2 : "Parameters") + ":", xlabel, 22);
		g2.drawString("\u03c3=" + format.format(0.1 * sigma) + " \u00c5", xlabel, 38);
		g2.drawString("\u03b5=" + format.format(epsilon) + " eV", xlabel, 50);

		s = MDView.getInternationalText("DragRectangleToChangeCurve");
		g2.drawString(s != null ? s : "Drag the rectangle to change the curve,", 120, 250);
		s = MDView.getInternationalText("OrUseUpDownLeftRightArrowKeys");
		g2.drawString(s != null ? s : "or use the up, down, left and right arrow keys.", 120, 264);

		if (firstTime) {
			ctrl.setLocation(xmin - 4, ymin - 4);
			firstTime = false;
		}

		if (editor.getSelectedTabIndex() == 0 || (editor.getSelectedTabIndex() != 0 && isEnabled())) {
			g2.setStroke(thinStroke);
			g2.setColor(Color.blue);
			g2.draw(ctrl);
			g2.setColor(SEMITRANSPARENT_BLUE);
			g2.drawLine(ctrl.x + 4, ctrl.y - 16, ctrl.x + 4, ctrl.y + 24);
			g2.drawLine(ctrl.x - 16, ctrl.y + 4, ctrl.x + 24, ctrl.y + 4);
		}

	}

	public void mousePressed(MouseEvent e) {
		if (!isEnabled())
			return;
		requestFocus();
		x = e.getX();
		y = e.getY();
		if (ctrl.contains(x, y)) {
			x = ctrl.x - e.getX();
			y = ctrl.y - e.getY();
			updateLocation(e.getX(), e.getY());
		}
		else {
			pressOut = true;
		}
	}

	public void mouseMoved(MouseEvent e) {
		if (!isEnabled())
			return;
		x = e.getX();
		y = e.getY();
		setCursor(ctrl.contains(x, y) ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR) : Cursor
				.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public void mouseDragged(MouseEvent e) {
		if (!isEnabled())
			return;
		if (!pressOut)
			updateLocation(e.getX(), e.getY());
	}

	public void mouseReleased(MouseEvent e) {
		if (!isEnabled())
			return;
		updateTextFields();
		if (ctrl.contains(e.getX(), e.getY())) {
			updateLocation(e.getX(), e.getY());
		}
		else {
			pressOut = false;
		}
		if (editor != null)
			editor.getModel().notifyChange();
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		if (!isEnabled())
			return;
		boolean b = false;
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			if (epsilon >= 0.0f) {
				epsilon -= 0.001f;
				if (epsilon < 0)
					epsilon = 0;
				updateFunction();
				b = true;
			}
			break;
		case KeyEvent.VK_DOWN:
			if (epsilon < 0.625f) {
				epsilon += 0.001f;
				if (epsilon > 0.625f)
					epsilon = 0.625f;
				updateFunction();
				b = true;
			}
			break;
		case KeyEvent.VK_LEFT:
			if (sigma > 5f) {
				sigma -= 0.05f;
				updateFunction();
				b = true;
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (sigma < 50f) {
				sigma += 0.05f;
				updateFunction();
				b = true;
			}
			break;
		}
		if (b && editor != null)
			editor.getModel().notifyChange();
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}

	private void updateFunction() {
		updateTextFields();
		try {
			xmin = (int) (imin * xUnit);
			ymin = (int) (ZERO_POS - vr[imin] * yUnit);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			xmin = 30;
			ymin = 30;
		}
		ctrl.setLocation(xmin - 4, ymin - 4);
		drawFunction();
		repaint();
	}

	private void updateLocation(int ex, int ey) {
		int rectx, recty;
		if (ex < 30) {
			rectx = 30;
		}
		else if (ex > getWidth() - 5) {
			rectx = getWidth() - 5;
		}
		else {
			rectx = x + ex;
		}
		if (ey < ZERO_POS) {
			recty = ZERO_POS;
		}
		else if (ey > getHeight() - 5) {
			recty = getHeight() - 5;
		}
		else {
			recty = y + ey;
		}
		ctrl.setLocation(rectx - 4, recty - 4);
		epsilon = -(ZERO_POS - (float) recty) / yUnit;
		sigma = (rectx / xUnit + indent) * interval / LJ_CONSTANT;
		drawFunction();
		repaint();
	}

	void updateTextFields() {
		if (editor == null)
			return;
		switch (editor.getSelectedInteraction()) {
		case 1:
			editor.setPar("Nt", "Nt", epsilon, sigma);
			break;
		case 2:
			editor.setPar("Pl", "Pl", epsilon, sigma);
			break;
		case 3:
			editor.setPar("Ws", "Ws", epsilon, sigma);
			break;
		case 4:
			editor.setPar("Ck", "Ck", epsilon, sigma);
			break;
		case 5:
			if (!editor.getLBMixing("Nt", "Pl")) {
				editor.setPar("Nt", "Pl", epsilon, sigma);
			}
			break;
		case 6:
			if (!editor.getLBMixing("Nt", "Ws")) {
				editor.setPar("Nt", "Ws", epsilon, sigma);
			}
			break;
		case 7:
			if (!editor.getLBMixing("Nt", "Ck")) {
				editor.setPar("Nt", "Ck", epsilon, sigma);
			}
			break;
		case 8:
			if (!editor.getLBMixing("Pl", "Ws")) {
				editor.setPar("Pl", "Ws", epsilon, sigma);
			}
			break;
		case 9:
			if (!editor.getLBMixing("Pl", "Ck")) {
				editor.setPar("Pl", "Ck", epsilon, sigma);
			}
			break;
		case 10:
			if (!editor.getLBMixing("Ws", "Ck")) {
				editor.setPar("Ws", "Ck", epsilon, sigma);
			}
			break;
		}
	}

}