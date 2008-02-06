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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JDialog;

import org.concord.modeler.util.PrinterUtilities;

/**
 * <p>
 * This is the main plotting class. It partitions the canvas to contain the specified axes with the remaining space
 * taken with the plotting region. Axes are packed against the walls of the canvas. The <B>paint</B> and <B>update</B>
 * methods of this class handle all the drawing operations of the graph. This means that independent components like
 * Axis and DataSets must be registered with this class to be incorporated into the plot.
 * </p>
 * 
 * @version $Revision: 1.15 $, $Date: 2007-09-10 21:26:53 $
 * @author Leigh Brookshaw
 * @author Modified by Charles Xie
 */

public class Graph2D extends JComponent implements Pageable, Printable {

	private Color DefaultBackground = Color.white;
	private transient PageFormat currentPageFormat;
	private Dimension oldSize;
	private Tracer tracer;

	/**
	 * A vector list of All the axes attached
	 * 
	 * @see org.concord.modeler.g2d.Graph2D#attachAxis(org.concord.modeler.g2d.Axis)
	 */
	protected Vector<Axis> axis = new Vector<Axis>(4);

	/**
	 * A vector list of All the DataSets attached
	 * 
	 * @see org.concord.modeler.g2d.Graph2D#attachDataSet(org.concord.modeler.g2d.DataSet)
	 * @see org.concord.modeler.g2d.DataSet
	 */
	protected Vector<DataSet> dataset = new Vector<DataSet>(10);

	/**
	 * The markers that may have been loaded
	 * 
	 * @see org.concord.modeler.g2d.Graph2D#setMarkers(org.concord.modeler.g2d.Markers)
	 */

	protected Markers markers = null;

	/**
	 * The background color for the data window
	 */
	protected Color DataBackground = Color.white;

	/**
	 * If this is greater than zero it means that data loading threads are active so the message "loading data" is
	 * flashed on the plot canvas. When it is back to zero the plot progresses normally
	 */
	public int loadingData = 0;

	/**
	 * The width of the border at the top of the canvas. This allows slopover from axis labels, legends etc.
	 */
	public int borderTop = 20;

	/**
	 * The width of the border at the bottom of the canvas. This allows slopover from axis labels, legends etc.
	 */
	public int borderBottom = 20;

	/**
	 * The width of the border at the left of the canvas. This allows slopover from axis labels, legends etc.
	 */
	public int borderLeft = 20;

	/**
	 * The width of the border at the right of the canvas. This allows slopover from axis labels, legends etc.
	 */
	public int borderRight = 20;

	/**
	 * If set <I>true</I> a frame will be drawn around the data window. Any axes will overlay this frame.
	 */
	public boolean frame = true;

	/**
	 * The color of the frame to be drawn
	 */
	public Color framecolor;

	/**
	 * If set <I>true</I> (the default) a grid will be drawn over the data window. The grid will align with the major
	 * tic marks of the Innermost axes.
	 */
	public boolean drawgrid = true;

	/**
	 * The color of the grid to be drawn
	 */
	public Color gridcolor = Color.pink;

	/**
	 * If set <I>true</I> (the default) a grid line will be drawn across the data window at the zeros of the innermost
	 * axes.
	 */
	public boolean drawzero = true;

	/**
	 * The color of the zero grid lines.
	 */
	public Color zerocolor = Color.orange;

	/**
	 * The rectangle that the data will be plotted within. This is an output variable only.
	 */
	public Rectangle datarect = new Rectangle();

	/**
	 * If set <I>true</I> (the default) the canvas will be set to the background color (erasing the plot) when the
	 * update method is called. This would only be changed for special effects.
	 */
	public boolean clearAll = true;

	/**
	 * If set <I>true</I> (the default) everything associated with the plot will be drawn when the update method or
	 * paint method are called. Normally only modified for special effects
	 */
	public boolean paintAll = true;

	/**
	 * Modify the position of the axis and the range of the axis so that the aspect ratio of the major tick marks are 1
	 * and the plot is square on the screen
	 */
	public boolean square = false;

	/**
	 * Text to be painted Last onto the Graph Canvas.
	 */
	public TextLine lastText = null;

	/**
	 * define printer page format and add resize listener to the canvas.
	 */
	public Graph2D() {
		setPreferredSize(new Dimension(200, 200));
		setBackground(Color.white);
		setForeground(Color.black);
		setGraphBackground(Color.white);
		setDataBackground(Color.white);
		currentPageFormat = new PageFormat();
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (dataset.isEmpty())
					return;
				if (oldSize == null) {
					oldSize = new Dimension();
				}
				else {
					int dx = getWidth() - oldSize.width;
					int dy = getHeight() - oldSize.height;
					synchronized (dataset) {
						for (DataSet ds : dataset) {
							ds.moveLegendLocation(dx, dy);
						}
					}
				}
				oldSize.setSize(getWidth(), getHeight());
			}
		});
	}

	public PageFormat getCurrentPageFormat() {
		return currentPageFormat;
	}

	public void setCurrentPageFormat(PageFormat f) {
		currentPageFormat = f;
	}

	/**
	 * <p>
	 * Load and Attach a DataSet from an array. The method loads the data into a DataSet class and attaches the class to
	 * the graph for plotting.
	 * </p>
	 * 
	 * <p>
	 * The data is assumed to be stored in the form x,y,x,y,x,y.... A local copy of the data is made.
	 * </p>
	 * 
	 * @param data
	 *            The data to be loaded in the form x,y,x,y,...
	 * @param n
	 *            The number of (x,y) data points. This means that the minimum length of the data array is 2*n.
	 * @return The DataSet constructed containing the data read.
	 */
	public DataSet loadDataSet(double data[], int n) {
		DataSet d;
		try {
			d = new DataSet(data, n);
			dataset.addElement(d);
			d.g2d = this;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return d;
	}

	DataSet loadDataSet(double[] data) {
		DataSet d;
		try {
			d = new DataSet(data);
			dataset.addElement(d);
			d.g2d = this;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return d;
	}

	public DataSet loadDataSet(double x[], double y[], int n, boolean nozero) {
		int i, j;
		if (nozero) {
			int k = 0;
			for (i = 0; i < n; i++) {
				if (Math.abs(y[i]) > 1000.0 * Double.MIN_VALUE) {
					k++;
				}
			}
			double[] data = new double[2 * k];
			k = 0;
			for (i = 0; i < n; i++) {
				if (Math.abs(y[i]) > 1000.0 * Double.MIN_VALUE) {
					data[k] = x[i];
					data[k + 1] = y[i];
					k += 2;
				}
			}
			return loadDataSet(data, k / 2);
		}
		double[] data = new double[2 * n];
		for (i = j = 0; i < n; i++, j += 2) {
			data[j] = x[i];
			data[j + 1] = y[i];
		}
		return loadDataSet(data, n);
	}

	/**
	 * Attach a DataSet to the graph. By attaching the data set the class can draw the data through its paint method.
	 */
	public void attachDataSet(DataSet d) {
		if (d != null) {
			dataset.addElement(d);
			d.g2d = this;
		}
	}

	/**
	 * Detach the DataSet from the class. Data associated with the DataSet will nolonger be plotted.
	 * 
	 * @param d
	 *            The DataSet to detach.
	 */
	public void detachDataSet(DataSet d) {
		if (d != null) {
			if (d.xaxis != null)
				d.xaxis.detachDataSet(d);
			if (d.yaxis != null)
				d.yaxis.detachDataSet(d);
			dataset.removeElement(d);
		}
	}

	/**
	 * Detach All the DataSets from the class.
	 */
	public void detachDataSets() {
		if (dataset == null | dataset.isEmpty())
			return;
		synchronized (dataset) {
			for (DataSet d : dataset) {
				if (d.xaxis != null)
					d.xaxis.detachDataSet(d);
				if (d.yaxis != null)
					d.yaxis.detachDataSet(d);
			}
		}
		dataset.removeAllElements();
	}

	public Vector getAxises() {
		return axis;
	}

	/**
	 * Create and attach an Axis to the graph. The position of the axis is one of Axis.TOP, Axis.BOTTOM, Axis.LEFT or
	 * Axis.RIGHT.
	 * 
	 * @param position
	 *            Position of the axis in the drawing window.
	 * 
	 */
	public Axis createAxis(int position) {
		Axis a;
		try {
			a = new Axis(position);
			a.g2d = this;
			axis.addElement(a);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return a;
	}

	/**
	 * Attach a previously created Axis. Only Axes that have been attached will be drawn
	 * 
	 * @param the
	 *            Axis to attach.
	 */
	public void attachAxis(Axis a) {
		if (a == null)
			return;
		try {
			axis.addElement(a);
			a.g2d = this;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Detach a previously attached Axis.
	 * 
	 * @param the
	 *            Axis to dettach.
	 */
	public void detachAxis(Axis a) {
		if (a != null) {
			a.detachAll();
			a.g2d = null;
			axis.removeElement(a);
		}
	}

	/**
	 * Detach All attached Axes.
	 */
	public void detachAxes() {
		if (axis == null | axis.isEmpty())
			return;
		for (Axis a : axis) {
			a.detachAll();
			a.g2d = null;
		}
		axis.removeAllElements();
	}

	/**
	 * Add a dialog for curves in the specified coordinate system set up by the two passed axises.
	 */
	public JDialog addCurveDialog(Axis xaxis, Axis yaxis) {
		return new CurveDialog(this, xaxis, yaxis);
	}

	/**
	 * Get the Maximum X value of all attached DataSets.
	 * 
	 * @return The maximum value
	 */
	public double getXmax() {
		double max = 0.0;
		if (dataset == null | dataset.isEmpty())
			return max;
		DataSet d;
		for (int i = 0; i < dataset.size(); i++) {
			d = dataset.elementAt(i);
			if (i == 0)
				max = d.getXmax();
			else max = Math.max(max, d.getXmax());
		}
		return max;
	}

	/**
	 * Get the Maximum Y value of all attached DataSets.
	 * 
	 * @return The maximum value
	 */
	public double getYmax() {
		double max = 0.0;
		if (dataset == null | dataset.isEmpty())
			return max;
		DataSet d;
		for (int i = 0; i < dataset.size(); i++) {
			d = dataset.elementAt(i);
			if (i == 0)
				max = d.getYmax();
			else max = Math.max(max, d.getYmax());
		}
		return max;
	}

	/**
	 * Get the Minimum X value of all attached DataSets.
	 * 
	 * @return The minimum value
	 */
	public double getXmin() {
		double min = 0.0;
		if (dataset == null | dataset.isEmpty())
			return min;
		DataSet d;
		for (int i = 0; i < dataset.size(); i++) {
			d = dataset.elementAt(i);
			if (i == 0)
				min = d.getXmin();
			else min = Math.min(min, d.getXmin());
		}
		return min;
	}

	/**
	 * Get the Minimum Y value of all attached DataSets.
	 * 
	 * @return The minimum value
	 */
	public double getYmin() {
		double min = 0.0;
		if (dataset == null | dataset.isEmpty())
			return min;
		DataSet d;
		for (int i = 0; i < dataset.size(); i++) {
			d = dataset.elementAt(i);
			if (i == 0)
				min = d.getYmin();
			else min = Math.min(min, d.getYmin());
		}
		return min;
	}

	/**
	 * Set the markers for the plot.
	 * 
	 * @param m
	 *            Marker class containing the defined markers
	 * @see Markers
	 */
	public void setMarkers(Markers m) {
		markers = m;
	}

	/**
	 * Get the markers
	 * 
	 * @return defined Marker class
	 * @see Markers
	 */
	public Markers getMarkers() {
		return markers;
	}

	/**
	 * Set the background color for the entire canvas.
	 * 
	 * @param c
	 *            The color to set the canvas
	 */
	public void setGraphBackground(Color c) {
		if (c == null)
			return;
		setBackground(c);
	}

	public Color getGraphBackground() {
		return getBackground();
	}

	/**
	 * Set the background color for the data window.
	 * 
	 * @param c
	 *            The color to set the data window.
	 */
	public void setDataBackground(Color c) {
		if (c == null)
			return;
		DataBackground = c;
	}

	public Color getDataBackground() {
		return DataBackground;
	}

	public void setLegendFont(String name, int style, int size) {
		if (dataset.isEmpty())
			return;
		Font font = new Font(name, style, size);
		synchronized (dataset) {
			for (DataSet ds : dataset) {
				ds.legendFont(font);
			}
		}
	}

	public void setLegendFont(Font font) {
		if (font == null)
			return;
		if (dataset.isEmpty())
			return;
		synchronized (dataset) {
			for (DataSet ds : dataset) {
				ds.legendFont(font);
			}
		}
	}

	public void setLegendLocation(int x, int y) {
		if (dataset.isEmpty())
			return;
		int i = 0;
		synchronized (dataset) {
			for (DataSet ds : dataset) {
				ds.setLegendLocation(x, y + (i++) * 15);
			}
		}
	}

	public void setLegendLocation(Point p) {
		if (p == null)
			return;
		setLegendLocation(p.x, p.y);
	}

	public Point getLegendLocation() {
		if (dataset.isEmpty())
			return null;
		return dataset.elementAt(0).getLegendLocation();
	}

	public void setAxisFont(Font font) {
		if (font == null)
			return;
		for (Axis a : axis) {
			a.setLabelFont(font);
			a.setTitleFont(font);
			a.setExponentFont(font);
		}
	}

	/**
	 * A hook into the Graph2D.paint method. This is called before anything is plotted. The rectangle passed is the
	 * dimension of the canvas minus the border dimensions.
	 * 
	 * @param g
	 *            Graphics state
	 * @param r
	 *            Rectangle containing the graph
	 */
	public void paintFirst(Graphics g, Rectangle r) {
	}

	/**
	 * A hook into the Graph2D.paint method. This is called before the data is drawn but after the axis. The rectangle
	 * passed is the dimension of the data window.
	 * 
	 * @param g
	 *            Graphics state
	 * @param r
	 *            Rectangle containing the data
	 */
	public void paintBeforeData(Graphics g, Rectangle r) {
	}

	/**
	 * A hook into the Graph2D.paint method. This is called after everything has been drawn. The rectangle passed is the
	 * dimension of the data window.
	 * 
	 * @param g
	 *            Graphics state
	 * @param r
	 *            Rectangle containing the data
	 */
	public void paintLast(Graphics g, Rectangle r) {
		if (lastText != null) {
			lastText.draw(g, r.width >> 1, r.height >> 1, TextLine.CENTER);
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void setTracer(Tracer t) {
		tracer = t;
	}

	/**
	 * This method is called via the Graph2D.repaint() method. All it does is blank the canvas (with the background
	 * color) before calling paint.
	 */
	public void update(Graphics g) {

		Insets insets = getInsets();

		if (clearAll) {
			Color c = g.getColor();
			Rectangle r = getBounds();
			r.x = 0;
			r.y = 0;
			g.setColor(getBackground());
			g.fillRect(r.x + insets.left, r.y + insets.top, r.width - insets.left - insets.right, r.height - insets.top
					- insets.bottom);
			g.setColor(c);
		}

		Rectangle r = getBounds();

		// The r.x and r.y returned from bounds is relative to the parents space so set them equal to zero.
		r.x = insets.left;
		r.y = insets.top;

		if (getBorder() != null) {
			getBorder().paintBorder(this, g, r.x, r.y, r.width - insets.left - insets.right,
					r.height - insets.top - insets.bottom);
		}

		if (DefaultBackground == null)
			DefaultBackground = this.getBackground();
		if (DataBackground == null)
			DataBackground = this.getBackground();

		if (!paintAll)
			return;

		r.x += borderLeft;
		r.y += borderTop;
		r.width -= borderLeft + borderRight + insets.left + insets.right;
		r.height -= borderBottom + borderTop + insets.top + insets.bottom;

		paintFirst(g, r);

		if (!axis.isEmpty()) {
			r = drawAxis(g, r);
		}
		else {
			if (clearAll) {
				Color c = g.getColor();
				g.setColor(DataBackground);
				g.fillRect(r.x, r.y, r.width, r.height);
				g.setColor(c);
			}
			drawFrame(g, r.x, r.y, r.width, r.height);
		}

		paintBeforeData(g, r);

		if (!dataset.isEmpty()) {
			datarect.x = r.x;
			datarect.y = r.y;
			datarect.width = r.width;
			datarect.height = r.height;
			synchronized (dataset) {
				for (DataSet ds : dataset)
					ds.draw_data(g, r);
			}
		}

		paintLast(g, r);

		if (tracer != null)
			tracer.paint(g);

	}

	/**
	 * Force the plot to have an aspect ratio of 1 by forcing the axes to have the same range. If the range of the axes
	 * are very different some extremely odd things can occur. All axes are forced to have the same range, so more than
	 * 2 axis is pointless.
	 */
	protected Rectangle ForceSquare(Graphics g, Rectangle r) {
		Rectangle dr;
		int x = r.x;
		int y = r.y;
		int width = r.width;
		int height = r.height;

		double xrange = 0.0;
		double yrange = 0.0;
		double range;

		if (dataset == null | dataset.isEmpty())
			return r;

		// Force all the axis to have the same range. This of course means that anything other than one xaxis and
		// one yaxis is a bit pointless.
		for (Axis a : axis) {
			range = a.maximum - a.minimum;
			if (a.isVertical()) {
				yrange = Math.max(range, yrange);
			}
			else {
				xrange = Math.max(range, xrange);
			}
		}

		if (xrange <= 0 | yrange <= 0)
			return r;

		if (xrange > yrange)
			range = xrange;
		else range = yrange;

		for (Axis a : axis) {
			a.maximum = a.minimum + range;
		}

		// Get the new data rectangle
		dr = getDataRectangle(g, r);

		// Modify the data rectangle so that it is square.
		if (dr.width > dr.height) {
			x += (dr.width - dr.height) * 0.5;
			width -= dr.width - dr.height;
		}
		else {
			y += (dr.height - dr.width) * 0.5;
			height -= dr.height - dr.width;
		}

		return new Rectangle(x, y, width, height);

	}

	/**
	 * Calculate the rectangle occupied by the data
	 */
	protected Rectangle getDataRectangle(Graphics g, Rectangle r) {
		int waxis;
		int x = r.x;
		int y = r.y;
		int width = r.width;
		int height = r.height;
		for (Axis a : axis) {
			waxis = a.getAxisWidth(g);
			switch (a.getAxisPos()) {
			case Axis.LEFT:
				x += waxis;
				width -= waxis;
				break;
			case Axis.RIGHT:
				width -= waxis;
				break;
			case Axis.TOP:
				y += waxis;
				height -= waxis;
				break;
			case Axis.BOTTOM:
				height -= waxis;
				break;
			}
		}
		return new Rectangle(x, y, width, height);
	}

	/**
	 * Draw the Axis. As each axis is drawn and aligned less of the canvas is avaliable to plot the data. The returned
	 * Rectangle is the canvas area that the data is plotted in.
	 */
	protected Rectangle drawAxis(Graphics g, Rectangle r) {
		Rectangle dr;
		int x;
		int y;
		int width;
		int height;
		if (square)
			r = ForceSquare(g, r);
		dr = getDataRectangle(g, r);
		x = dr.x;
		y = dr.y;
		width = dr.width;
		height = dr.height;
		if (clearAll) {
			Color c = g.getColor();
			g.setColor(DataBackground);
			g.fillRect(x, y, width, height);
			g.setColor(c);
		}
		// Draw a frame around the data area (If requested)
		if (frame)
			drawFrame(g, x, y, width, height);
		// Now draw the axis in the order specified aligning them with the final data area.
		for (Axis a : axis) {
			a.data_window = new Dimension(width, height);
			switch (a.getAxisPos()) {
			case Axis.LEFT:
				r.x += a.width;
				r.width -= a.width;
				a.positionAxis(r.x, r.x, y, y + height);
				if (r.x == x) {
					a.gridcolor = gridcolor;
					a.drawgrid = drawgrid;
					a.zerocolor = zerocolor;
					a.drawzero = drawzero;
				}
				a.drawAxis(g);
				a.drawgrid = false;
				a.drawzero = false;
				break;
			case Axis.RIGHT:
				r.width -= a.width;
				a.positionAxis(r.x + r.width, r.x + r.width, y, y + height);
				if (r.x + r.width == x + width) {
					a.gridcolor = gridcolor;
					a.drawgrid = drawgrid;
					a.zerocolor = zerocolor;
					a.drawzero = drawzero;
				}
				a.drawAxis(g);
				a.drawgrid = false;
				a.drawzero = false;
				break;
			case Axis.TOP:
				r.y += a.width;
				r.height -= a.width;
				a.positionAxis(x, x + width, r.y, r.y);
				if (r.y == y) {
					a.gridcolor = gridcolor;
					a.drawgrid = drawgrid;
					a.zerocolor = zerocolor;
					a.drawzero = drawzero;
				}
				a.drawAxis(g);
				a.drawgrid = false;
				a.drawzero = false;
				break;
			case Axis.BOTTOM:
				r.height -= a.width;
				a.positionAxis(x, x + width, r.y + r.height, r.y + r.height);
				if (r.y + r.height == y + height) {
					a.gridcolor = gridcolor;
					a.drawgrid = drawgrid;
					a.zerocolor = zerocolor;
					a.drawzero = drawzero;
				}
				a.drawAxis(g);
				a.drawgrid = false;
				a.drawzero = false;
				break;
			}
		}
		return r;
	}

	/**
	 * Draws a frame around the data area.
	 */
	protected void drawFrame(Graphics g, int x, int y, int width, int height) {
		Color c = g.getColor();
		if (framecolor != null)
			g.setColor(framecolor);
		g.drawRect(x, y, width, height);
		g.setColor(c);
	}

	public DataSet getSet(int i) {
		return dataset.elementAt(i);
	}

	public Vector getDataSets() {
		return dataset;
	}

	public int getDataSetCount() {
		if (dataset == null)
			return 0;
		return dataset.size();
	}

	/**
	 * Find the closest data set to the cursor in the coordinate system specified by the two axises, if the minimum
	 * distance is within the selection distance, then pick up the data set, otherwise, return null.
	 */
	public DataSet getSet(int ix, int iy, Axis xaxis, Axis yaxis) {
		int i = -1, j = -1;
		int distsq = -1;
		int min = 50;
		int[] a = new int[3];
		synchronized (dataset) {
			for (DataSet ds : dataset) {
				a = ds.getClosestPixel(ix, iy, xaxis, yaxis);
				if (distsq < 0 || distsq > a[2]) {
					distsq = a[2];
					j = i;
				}
			}
		}
		if (distsq >= 0 && distsq < min && j >= 0)
			return dataset.elementAt(j);
		return null;
	}

	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
		Dimension d = PrinterUtilities.scaleToPaper(pageFormat.getOrientation(), pageFormat.getPaper(), getSize());
		Graphics2D g2d = (Graphics2D) g;
		g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
		BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Color c1 = getBackground();
		Color c2 = DataBackground;
		setGraphBackground(Color.white);
		setDataBackground(Color.white);
		Graphics2D temp = bi.createGraphics();
		temp.setColor(Color.white);
		temp.fillRect(0, 0, getWidth(), getHeight());
		paint(bi.createGraphics());
		g2d.drawImage(bi.getScaledInstance(d.width, d.height, Image.SCALE_SMOOTH), 0, 0, d.width, d.height, this);
		setGraphBackground(c1);
		setDataBackground(c2);
		return PAGE_EXISTS;
	}

	public int getNumberOfPages() {
		return 1;
	}

	public PageFormat getPageFormat(int pageIndex) {
		return currentPageFormat;
	}

	public Printable getPrintable(int pageIndex) {
		return this;
	}

}
