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

package org.concord.modeler.g2d;

import java.awt.Button;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * This class is an extension of Graph2D class. It adds interactive selection of the plotting range and can display the
 * mouse position in user coordinates.
 * 
 * <h4>Mouse Events</h4>
 * <dl>
 * <dt>MouseDown
 * <dd>Starts the range selection
 * <dt>MouseDrag
 * <dd>Drag out a rectangular range selection
 * <dt>MouseUp
 * <dd>Replot with modified plotting range.
 * <dt>
 * </dl>
 * <h4>KeyDown Events</h4>
 * <dl>
 * <dt>R
 * <dd>Redraw plot with default limits
 * <dt>r
 * <dd>Redraw plot using current limits
 * <dt>m
 * <dd>Pop window to enter manually plot range
 * <dt>c
 * <dd>Toggle pop-up window that displays the mouse position in user coordinates
 * <dt>d
 * <dd>Show coordinates of the closest data point to the cursor
 * <dt>D
 * <dd>Hide data coordinates pop-window
 * <dt>h
 * <dd>This key pressed in Any pop-window at any-time will hide it.
 * </dl>
 * <P>
 * <B>Note:</B> To hide Any pop-window press the key <B>h</B> in the window. This will hide the window at any time.
 * Depending on your windowing system the mouse button might have to be pressed in the popup window to ensure it has the
 * keyboard focus.
 * 
 * @version $Revision: 1.10 $, $Date: 2007-07-16 14:00:42 $.
 * @author Leigh Brookshaw
 * @author Modified by Qian Xie to be compliant with JDK1.3
 */

public class G2Dint extends Graph2D implements KeyListener, ActionListener, MouseListener, MouseMotionListener {

	/**
	 * Set to true when a rectangle is being dragged out by the mouse
	 */
	protected boolean drag = false;

	/**
	 * User limits. The user has set the limits using the mouse drag option
	 */
	protected boolean userlimits = false;

	/**
	 * Ths popup window for the cursor position command
	 */
	private Gin cpgin = null;

	/**
	 * Ths popup window for the data point command
	 */
	private Gin dpgin = null;

	/**
	 * The popup window to manually set the range
	 */
	private Range range = null;

	/**
	 * Button Down position
	 */
	private int x0, y0;

	/**
	 * Button Drag position
	 */
	private int x1, y1;

	/*
	 * * Previous Button Drag position
	 */
	private int x1old, y1old;

	/**
	 * Attached X Axis which must be registered with this class. This is one of the axes used to find the drag range. If
	 * no X axis is registered no mouse drag.
	 */
	protected Axis xaxis;

	/**
	 * Attached Y Axis which must be registered with this class. This is one of the axes used to find the drag range. If
	 * no Y axis is registered no mouse drag.
	 */
	protected Axis yaxis;

	public G2Dint() {

		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);

	}

	/** Create Xaxis to be used for the drag scaling */
	public Axis createXAxis() {
		xaxis = super.createAxis(Axis.BOTTOM);
		return xaxis;
	}

	/** Create Yaxis to be used for the drag scaling */
	public Axis createYAxis() {
		yaxis = super.createAxis(Axis.LEFT);
		return yaxis;
	}

	/**
	 * Create and attach an Axis to the graph. The position of the axis is one of Axis.TOP, Axis.BOTTOM, Axis.LEFT or
	 * Axis.RIGHT.
	 * 
	 * @param position
	 *            Position of the axis in the drawing window.
	 */
	public Axis createAxis(int position) {

		if (position == Axis.TOP || position == Axis.BOTTOM) {
			xaxis = super.createAxis(position);
			return xaxis;
		}
		else if (position == Axis.LEFT || position == Axis.RIGHT) {
			yaxis = super.createAxis(position);
			return yaxis;
		}

		return null;

	}

	/**
	 * Attach axis to be used for the drag scaling. X axes are assumed to have Axis position Axis.BOTTOM or Axis.TOP. Y
	 * axes are assumed to have position Axis.LEFT or Axis.RIGHT.
	 * 
	 * @param a
	 *            Axis to attach
	 * @see Axis
	 */
	public void attachAxis(Axis a) {
		if (a == null)
			return;

		super.attachAxis(a);

		if (a.getAxisPos() == Axis.BOTTOM || a.getAxisPos() == Axis.TOP) {
			xaxis = a;
		}
		else {
			yaxis = a;
		}
	}

	/**
	 * New update method incorporating mouse dragging.
	 */
	public void update(Graphics g) {
		Rectangle r = getBounds();
		Color c = g.getColor();

		/*
		 * The r.x and r.y returned from bounds is relative to the * parents space so set them equal to zero
		 */
		r.x = 0;
		r.y = 0;

		if (drag) {
			/**
			 * Set the dragColor. Do it everytime just incase someone is playing silly buggers with the background
			 * color.
			 */
			g.setColor(DataBackground);

			float hsb[] = Color.RGBtoHSB(DataBackground.getRed(), DataBackground.getGreen(), DataBackground.getBlue(),
					null);

			if (hsb[2] < 0.5)
				g.setXORMode(Color.white);
			else g.setXORMode(Color.black);

			/*
			 * Drag out the new box. Use drawLine instead of drawRect to avoid problems when width and heights become
			 * negative. Seems drawRect can't handle it!
			 */

			/*
			 * Draw over old box to erase it. This works because XORMode has been set. If from one call to the next the
			 * background color changes going to get some odd results.
			 */
			g.drawLine(x0, y0, x1old, y0);
			g.drawLine(x1old, y0, x1old, y1old);
			g.drawLine(x1old, y1old, x0, y1old);
			g.drawLine(x0, y1old, x0, y0);
			/*
			 * draw out new box
			 */
			g.drawLine(x0, y0, x1, y0);
			g.drawLine(x1, y0, x1, y1);
			g.drawLine(x1, y1, x0, y1);
			g.drawLine(x0, y1, x0, y0);
			/*
			 * Set color back to default color
			 */
			g.setColor(c);

			x1old = x1;
			y1old = y1;

			return;
		}

		if (clearAll) {
			g.setColor(getBackground());
			g.fillRect(r.x, r.y, r.width, r.height);
			g.setColor(c);
		}
		if (paintAll)
			paint(g);
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {

		if (xaxis == null || yaxis == null)
			return;

		char key = e.getKeyChar();

		switch (key) {

		case 'R':
			xaxis.resetRange();
			yaxis.resetRange();
			userlimits = false;
			repaint();
			return;

		case 'r':
			repaint();
			return;

		case 'c':
			if (cpgin == null)
				cpgin = new Gin("Position");
			if (cpgin.isVisible()) {
				cpgin.setVisible(false);
			}
			else {
				cpgin.setVisible(true);
			}
			return;

		case 'D':
			if (dpgin != null)
				dpgin.setVisible(false);
			return;

		case 'd':
			if (dpgin == null)
				dpgin = new Gin("Data Point");
			dpgin.setVisible(true);
			double d[] = getClosestPoint(x1, y1);
			dpgin.setXlabel(d[0]);
			dpgin.setYlabel(d[1]);
			int ix = xaxis.getInteger(d[0]);
			int iy = yaxis.getInteger(d[1]);
			if (ix >= datarect.x && ix <= datarect.x + datarect.width && iy >= datarect.y
					&& iy <= datarect.y + datarect.height) {
				Graphics g = getGraphics();
				g.fillOval(ix - 4, iy - 4, 8, 8);
			}
			return;

		case 'm':
			if (range == null)
				range = new Range(this);
			range.setVisible(true);
			range.requestFocus();
			userlimits = true;
			return;
		}

	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {

		if (xaxis == null || yaxis == null)
			return;

		/*
		 * Soon as the mouse button is pressed request the Focus otherwise we will miss key events
		 */
		requestFocusInWindow();

		x0 = e.getX();
		y0 = e.getY();

		drag = true;
		x1old = x0;
		y1old = y0;

		if (x0 < datarect.x) {
			x0 = datarect.x;
		}
		else if (x0 > datarect.x + datarect.width) {
			x0 = datarect.x + datarect.width;
		}

		if (y0 < datarect.y) {
			y0 = datarect.y;
		}
		else if (y0 > datarect.y + datarect.height) {
			y0 = datarect.y + datarect.height;
		}

	}

	public void mouseReleased(MouseEvent e) {

		if (xaxis == null || yaxis == null)
			return;

		x1 = e.getX();
		y1 = e.getY();

		if (drag)
			userlimits = true;

		drag = false;

		if (x1 < datarect.x) {
			x1 = datarect.x;
		}
		else if (x1 > datarect.x + datarect.width) {
			x1 = datarect.x + datarect.width;
		}

		if (y1 < datarect.y) {
			y1 = datarect.y;
		}
		else if (y1 > datarect.y + datarect.height) {
			y1 = datarect.y + datarect.height;
		}

		if (Math.abs(x0 - x1) > 5 && Math.abs(y0 - y1) > 5) {

			if (x0 < x1) {
				xaxis.minimum = xaxis.getDouble(x0);
				xaxis.maximum = xaxis.getDouble(x1);
			}
			else {
				xaxis.maximum = xaxis.getDouble(x0);
				xaxis.minimum = xaxis.getDouble(x1);
			}

			if (y0 > y1) {
				yaxis.minimum = yaxis.getDouble(y0);
				yaxis.maximum = yaxis.getDouble(y1);
			}
			else {
				yaxis.maximum = yaxis.getDouble(y0);
				yaxis.minimum = yaxis.getDouble(y1);
			}

			repaint();

		}
	}

	public void mouseDragged(MouseEvent e) {

		if (xaxis == null || yaxis == null)
			return;

		x1 = e.getX();
		y1 = e.getY();

		if (drag) {

			if (x1 < datarect.x) {
				x1 = datarect.x;
			}
			else {
				if (x1 > datarect.x + datarect.width)
					x1 = datarect.x + datarect.width;
			}

			if (y1 < datarect.y) {
				y1 = datarect.y;
			}
			else {
				if (y1 > datarect.y + datarect.height)
					y1 = datarect.y + datarect.height;
			}

			if (cpgin != null && cpgin.isVisible()) {
				cpgin.setXlabel(xaxis.getDouble(x1));
				cpgin.setYlabel(yaxis.getDouble(y1));

			}

			repaint();

		}

	}

	public void mouseMoved(MouseEvent e) {

		if (xaxis == null || yaxis == null)
			return;

		x1 = e.getX();
		y1 = e.getY();

		if (cpgin != null && cpgin.isVisible()) {
			cpgin.setXlabel(xaxis.getDouble(x1));
			cpgin.setYlabel(yaxis.getDouble(y1));
		}

	}

	/**
	 * Allows external classes (pop-up windows etc.) to communicate to this class asyncronously.
	 */
	public void actionPerformed(ActionEvent e) {

		if (xaxis == null || yaxis == null)
			return;

		Object o = e.getSource();

		if (o instanceof Button) {

			Button b = (Button) o;

			if (b.getName().equals("Done")) {

				if (range != null) {

					Double d;
					double txmin = xaxis.minimum;
					double txmax = xaxis.maximum;
					double tymin = yaxis.minimum;
					double tymax = yaxis.maximum;

					d = range.getXmin();
					if (d != null)
						txmin = d.doubleValue();
					d = range.getXmax();
					if (d != null)
						txmax = d.doubleValue();
					d = range.getYmin();
					if (d != null)
						tymin = d.doubleValue();
					d = range.getYmax();
					if (d != null)
						tymax = d.doubleValue();

					if (txmax > txmin && tymax > tymin) {
						xaxis.minimum = txmin;
						xaxis.maximum = txmax;
						yaxis.minimum = tymin;
						yaxis.maximum = tymax;
					}

					repaint();

				}
			}
		}
	}

	/**
	 * Find the closest data point to the cursor
	 */
	protected double[] getClosestPoint(int ix, int iy) {
		DataSet ds;
		int i;
		double a[] = new double[3];
		double distsq = -1.0;
		double data[] = { 0.0, 0.0 };
		double x = xaxis.getDouble(ix);
		double y = yaxis.getDouble(iy);

		for (i = 0; i < dataset.size(); i++) {
			ds = (DataSet) (dataset.elementAt(i));

			a = ds.getClosestPoint(x, y);

			if (distsq < 0.0 || distsq > a[2]) {
				data[0] = a[0];
				data[1] = a[1];
				distsq = a[2];
			}
		}
		return data;

	}

}
