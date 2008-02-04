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

package org.concord.functiongraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

/**
 * This is a graph component for drawing and interacting with math functions. Property change events are fired in the
 * following three cases: (1) A data source is added; (2) A data source is removed; (3) A data source is changed.
 * 
 * <p>
 * Keyboard actions registed with this graph:
 * <ul>
 * <li> 'i' ---- zoom in
 * <li> 'o' ---- zoom out
 * <li> 'r' ---- reset
 * <li> ^n ---- open a dialog to input a new function
 * <li> ^d ---- open a dialog to display numeric data
 * <li> ^x ---- open a dialog to select function(s) and remove
 * <li> ^a ---- prompt to remove all functions
 * <li> ^g ---- prompt to change the scales of x and/or y axises
 * <li> ^f ---- display the symbols of the supported functions
 * <li> Up ---- move the coordinate system up
 * <li> Down ---- move the coordinate system down
 * <li> Left ---- move the coordinate system left
 * <li> Right ---- move the coordinate system right
 * </ul>
 * </p>
 * 
 * @author Connie J. Chen
 */

public class Graph extends JComponent {

	final static DecimalFormat format = new DecimalFormat("###.##");

	private static boolean isUSLocale;
	private static ResourceBundle bundle;

	// a random number to indicate that a parameter is not set
	private final static int OLD_VALUE = 787878;
	private final static float TOLERANCE = 0.0001f;

	// store data sources in this graph
	List<DataSource> data;
	Axis xAxis;
	Axis yAxis;
	float oldXmin = OLD_VALUE, oldXmax = OLD_VALUE, oldYmin = OLD_VALUE, oldYmax = OLD_VALUE;
	private Point origin; // place of origin mapping to the GUI coordinate system
	private float xScale = 1.0f, yScale = 1.0f; // scale of mapping data to the GUI coordinate system
	private float movePercent = 0.05f; // percentage of each move of the coordinate plane
	private Color borderColor = new Color(120, 120, 120);
	private GeneralPath path = new GeneralPath();
	private Ellipse2D.Float handle = new Ellipse2D.Float(); // hold the current hotspot
	private boolean isADataSourceSelected;
	private int xPressed, yPressed;
	private float xminPressed, xmaxPressed, yminPressed, ymaxPressed;
	private final static Stroke handleStroke = new BasicStroke(8.0f);
	private final static Stroke borderStroke = new BasicStroke(3.0f);
	private float clipRatio;
	private int zoomScale;
	private int zoomScaleMin = -20, zoomScaleMax = 20;
	private boolean recompute;
	private long pressTime, dragTime;
	private int dragRespondingTime = 100; // in milliseconds
	private JPopupMenu popupMenu; // popup menu when user right-clicks
	private boolean popupMenuEnabled = true;
	private boolean allowDragCoordinatePlane = true;
	private long maximumSearchingTime = 3000L;
	private boolean detectSingularities = true;
	private static float[] deriv;
	private float minDeriv = 50.f; // absolute value of minimum derivative jump to be considered as a singularity point
	private boolean caughtSingularity;

	private Action removeFunctionAction;
	private Action removeAllFunctionsAction;
	private Action inputFunctionAction;
	private Action viewDataAction;
	private Action changeScaleAction;
	private Action viewSupportedFunctionsAction;
	private MouseWheelListener mouseWheelListener;

	public Graph() {

		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			try {
				bundle = ResourceBundle.getBundle("org.concord.functiongraph.resources.Graph", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		setPreferredSize(new Dimension(350, 350));

		data = Collections.synchronizedList(new ArrayList<DataSource>());
		xAxis = new Axis(Axis.X_AXIS);
		xAxis.setGraph(this);
		yAxis = new Axis(Axis.Y_AXIS);
		yAxis.setGraph(this);
		Dimension dim = getPreferredSize();
		origin = new Point(dim.width >> 1, dim.height >> 1);

		// hook up listeners with this graph
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				processMousePressedEvent(e);
			}

			public void mouseClicked(MouseEvent e) {
				processMouseClickedEvent(e);
			}

			public void mouseReleased(MouseEvent e) {
				processMouseReleasedEvent(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				processMouseMovedEvent(e);
			}

			public void mouseDragged(MouseEvent e) {
				processMouseDraggedEvent(e);
			}
		});
		mouseWheelListener = new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				processMouseWheelMovedEvent(e);
			}
		};
		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				addMouseWheelListener(mouseWheelListener);
			}

			public void focusLost(FocusEvent e) {
				removeMouseWheelListener(mouseWheelListener);
			}
		});
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				processKeyPressedEvent(e);
			}
		});

		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				setPreferredSize(getSize());
				if (xAxis.getMin() < xAxis.getMax() - TOLERANCE) {
					setXBounds(xAxis.getMin(), xAxis.getMax());
				}
				if (yAxis.getMin() < yAxis.getMax() - TOLERANCE) {
					setYBounds(yAxis.getMin(), yAxis.getMax());
				}
			}

			public void componentResized(ComponentEvent e) {
				setPreferredSize(getSize());
				if (xAxis.getMin() < xAxis.getMax() - TOLERANCE) {
					setXBounds(xAxis.getMin(), xAxis.getMax());
				}
				if (yAxis.getMin() < yAxis.getMax() - TOLERANCE) {
					setYBounds(yAxis.getMin(), yAxis.getMax());
				}
			}
		});

		// add actions
		removeFunctionAction = new RemoveFunctionAction(this);
		getActionMap().put(removeFunctionAction.toString(), removeFunctionAction);
		removeAllFunctionsAction = new RemoveAllFunctionsAction(this);
		getActionMap().put(removeAllFunctionsAction.toString(), removeAllFunctionsAction);
		inputFunctionAction = new InputFunctionAction(this);
		getActionMap().put(inputFunctionAction.toString(), inputFunctionAction);
		viewDataAction = new ViewDataAction(this);
		getActionMap().put(viewDataAction.toString(), viewDataAction);
		changeScaleAction = new ChangeScaleAction(this);
		getActionMap().put(changeScaleAction.toString(), changeScaleAction);
		viewSupportedFunctionsAction = new ViewSupportedFunctionsAction(this);
		getActionMap().put(viewSupportedFunctionsAction.toString(), viewSupportedFunctionsAction);

		popupMenu = createPopupMenu();

	}

	static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (name == null)
			return null;
		if (isUSLocale)
			return null;
		String s = null;
		try {
			s = bundle.getString(name);
		}
		catch (MissingResourceException e) {
			s = null;
		}
		return s;
	}

	protected void processKeyPressedEvent(KeyEvent e) {
		int keyCode = e.getKeyCode();
		switch (keyCode) {
		case KeyEvent.VK_I:
			if (!e.isControlDown()) {
				zoomIn(0.5f);
			}
			break;
		case KeyEvent.VK_O:
			if (!e.isControlDown()) {
				zoomOut(0.5f);
			}
			break;
		case KeyEvent.VK_N:
			// propagate this key event to the corresponding action
			if (e.isControlDown()) {
				inputFunctionAction.actionPerformed(new ActionEvent(this, e.getID(), (String) inputFunctionAction
						.getValue(Action.NAME), e.getWhen(), e.getModifiers()));
			}
			break;
		case KeyEvent.VK_X:
			// propagate this key event to the corresponding action
			if (e.isControlDown()) {
				removeFunctionAction.actionPerformed(new ActionEvent(this, e.getID(), (String) removeFunctionAction
						.getValue(Action.NAME), e.getWhen(), e.getModifiers()));
			}
			break;
		case KeyEvent.VK_A:
			// propagate this key event to the corresponding action
			if (e.isControlDown()) {
				removeAllFunctionsAction.actionPerformed(new ActionEvent(this, e.getID(),
						(String) removeAllFunctionsAction.getValue(Action.NAME), e.getWhen(), e.getModifiers()));
			}
			break;
		case KeyEvent.VK_D:
			// propagate this key event to the corresponding action
			if (e.isControlDown()) {
				viewDataAction.actionPerformed(new ActionEvent(this, e.getID(), (String) viewDataAction
						.getValue(Action.NAME), e.getWhen(), e.getModifiers()));
			}
			break;
		case KeyEvent.VK_G:
			// propagate this key event to the corresponding action
			if (e.isControlDown()) {
				changeScaleAction.actionPerformed(new ActionEvent(this, e.getID(), (String) changeScaleAction
						.getValue(Action.NAME), e.getWhen(), e.getModifiers()));
			}
			break;
		case KeyEvent.VK_F:
			// propagate this key event to the corresponding action
			if (e.isControlDown()) {
				viewSupportedFunctionsAction.actionPerformed(new ActionEvent(this, e.getID(),
						(String) changeScaleAction.getValue(Action.NAME), e.getWhen(), e.getModifiers()));
			}
			break;
		case KeyEvent.VK_UP:
			keyTranslate(keyCode);
			break;
		case KeyEvent.VK_DOWN:
			keyTranslate(keyCode);
			break;
		case KeyEvent.VK_LEFT:
			keyTranslate(keyCode);
			break;
		case KeyEvent.VK_RIGHT:
			keyTranslate(keyCode);
			break;
		case KeyEvent.VK_R:
			resetScope();
			break;
		}
		e.consume();
	}

	protected void processMouseWheelMovedEvent(MouseWheelEvent e) {
		if (e.getWheelRotation() > 0) {
			zoomIn(0.9f);
		}
		else {
			zoomOut(0.9f);
		}
		repaint();

	}

	protected void processMousePressedEvent(MouseEvent e) {

		requestFocus(); // must call to validate keyboard inputs

		// assume that only when the user clicks with left button, he means to
		// manipulate the data or coordinate system
		if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {

			e.translatePoint(-origin.x, -origin.y);
			int x = e.getX();
			int y = e.getY();

			// detect if a data source is chosen
			if (isEnabled()) {
				Ellipse2D hp;
				// lock "data" to prevent another thread from adding or removing data sources
				synchronized (data) {
					if (!data.isEmpty()) {
						for (DataSource ds : data) {
							if (!ds.getHideHotSpot()) {
								hp = ds.getHotSpot();
								if (hp != null) {
									linkHotSpotToHandle(hp);
									if (handle.contains(x, y) && !isADataSourceSelected) {
										ds.setSelected(true);
										isADataSourceSelected = true;
									}
									else {
										ds.setSelected(false);
									}
								}
							}
							else {
								Point2D p = ds.isProximate(x / xScale, y / yScale, Math.max(5 / xScale, 5 / yScale));
								if (p != null && !isADataSourceSelected) {
									ds.setSelected(true);
									isADataSourceSelected = true;
								}
								else {
									ds.setSelected(false);
								}
							}
						}
					}
				}
			}
			else {
				isADataSourceSelected = false;
			}

			// if no data source is chosen, store this current press point and the
			// information about the coordinate system's bounds (namely those of the axises)
			if (!isADataSourceSelected) {
				if (allowDragCoordinatePlane) {
					setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
					xPressed = x;
					yPressed = y;
					xminPressed = xAxis.getMin();
					xmaxPressed = xAxis.getMax();
					yminPressed = yAxis.getMin();
					ymaxPressed = yAxis.getMax();
				}
			}

			pressTime = System.currentTimeMillis();
			repaint();

		}

		e.consume();

	}

	protected void processMouseDraggedEvent(MouseEvent e) {

		// drag ONLY with left button
		if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == 0)
			return;

		e.translatePoint(-origin.x, -origin.y);
		int x = e.getX();
		int y = e.getY();
		dragTime = System.currentTimeMillis();

		if (isADataSourceSelected) {

			// if a data source is ever selected, change it according to the current mouse position
			if (dragTime - pressTime > dragRespondingTime) {
				Ellipse2D hp;
				synchronized (data) {// lock "data"
					if (!data.isEmpty()) {
						for (DataSource ds : data) {
							hp = ds.getHotSpot();
							if (hp != null) {
								if (ds.isSelected()) {
									float min = xAxis.getMin() * xScale;
									float max = xAxis.getMax() * xScale;
									if (x < min) {
										handle.x = (int) min;
									}
									else if (x > max - hp.getWidth()) {
										handle.x = (int) (max - hp.getWidth());
									}
									else {
										handle.x = x;
									}
									min = yAxis.getMin() * yScale;
									max = yAxis.getMax() * yScale;
									y = -y;// flip y---this is ugly and should have been taken care in a more
									// protective manner
									if (y < min) {
										handle.y = (int) min;
									}
									else if (y > max - hp.getHeight()) {
										handle.y = (int) (max - hp.getHeight());
									}
									else {
										handle.y = y;
									}
									changeData(ds);
									repaint();
									pressTime = dragTime;
								}
							}
						}
					}
				}
			}

		}
		else {
			// if no data source is selected, change the coordinate system's bounds
			// according to the current mouse position
			if (allowDragCoordinatePlane) {
				if (dragTime - pressTime > dragRespondingTime) {
					float dx = xmaxPressed - xminPressed;
					dx = -0.5f * ((float) (x - xPressed) / (float) getWidth()) * dx;
					setXBounds(xminPressed + dx, xmaxPressed + dx);
					float dy = ymaxPressed - yminPressed;
					dy = 0.5f * ((float) (y - yPressed) / (float) getHeight()) * dy;
					setYBounds(yminPressed + dy, ymaxPressed + dy);
					repaint();
					pressTime = dragTime;
				}
			}
		}

		e.consume();

	}

	protected void processMouseReleasedEvent(MouseEvent e) {
		e.translatePoint(-origin.x, -origin.y);
		isADataSourceSelected = false;
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		repaint();
		e.consume();
	}

	protected void processMouseMovedEvent(MouseEvent e) {

		// let the tool tip show the current coordinates
		setToolTipText("(" + format.format((e.getX() - origin.x) / xScale) + ", "
				+ format.format(-(e.getY() - origin.y) / yScale) + ")");

		e.translatePoint(-origin.x, -origin.y);
		int x = e.getX();
		int y = e.getY();

		if (isEnabled()) {

			Ellipse2D hp;
			boolean b = false;

			synchronized (data) {// lock "data"
				if (!data.isEmpty()) {
					for (DataSource ds : data) {
						if (!ds.getHideHotSpot()) {
							hp = ds.getHotSpot();
							if (hp != null) {
								linkHotSpotToHandle(hp);
								if (handle.contains(x, y)) {
									b = true;
									break;
								}
							}
						}
						else {
							Point2D p = ds.isProximate(x / xScale, y / yScale, Math.max(5 / xScale, 5 / yScale));
							if (p != null) {
								b = true;
								break;
							}
						}
					}
				}
			}
			setCursor(b ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor
					.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		e.consume();

	}

	protected void processMouseClickedEvent(MouseEvent e) {
		// when the user clicks with the right button
		if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
			if (popupMenuEnabled) {
				popupMenu.show(this, e.getX(), e.getY());
			}
		}
		e.consume();
	}

	void deselectAllPoints() {
		if (data.isEmpty())
			return;
		synchronized (data) {
			for (DataSource ds : data) {
				ds.setShowSelectedPoint(false);
			}
		}
		repaint();
	}

	private JPopupMenu createPopupMenu() {

		JPopupMenu pop = new JPopupMenu();

		JMenuItem mi = new JMenuItem(inputFunctionAction);
		String s = getInternationalText("InputFunction");
		mi.setText((s != null ? s : mi.getText()) + "...");
		pop.add(mi);

		mi = new JMenuItem(removeFunctionAction);
		s = getInternationalText("RemoveFunction");
		mi.setText((s != null ? s : mi.getText()) + "...");
		pop.add(mi);

		mi = new JMenuItem(removeAllFunctionsAction);
		s = getInternationalText("RemoveAllFunctions");
		if (s != null)
			mi.setText(s);
		pop.add(mi);
		pop.addSeparator();

		mi = new JMenuItem(changeScaleAction);
		s = getInternationalText("ChangeScale");
		mi.setText((s != null ? s : mi.getText()) + "...");
		pop.add(mi);

		mi = new JMenuItem(viewDataAction);
		s = getInternationalText("ViewData");
		mi.setText((s != null ? s : mi.getText()) + "...");
		pop.add(mi);

		mi = new JMenuItem(viewSupportedFunctionsAction);
		s = getInternationalText("ViewSupportedFunctions");
		mi.setText((s != null ? s : mi.getText()) + "...");
		pop.add(mi);

		return pop;

	}

	public void enablePopupMenu(boolean b) {
		popupMenuEnabled = b;
	}

	public boolean isPopupMenuEnabled() {
		return popupMenuEnabled;
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void enableSingularityDetection(boolean b) {
		detectSingularities = b;
	}

	public boolean isSingularityDetectionEnabled() {
		return detectSingularities;
	}

	/**
	 * whether or not there is at least a singularity point in the current data window of this graph
	 */
	public boolean hasSingularity() {
		paintImmediately(0, 0, getWidth(), getHeight());
		return caughtSingularity;
	}

	/**
	 * obstain an action supported by this graph. Actions are available in a hash map by their names.
	 */
	public Action getAction(String name) {
		return getActionMap().get(name);
	}

	/**
	 * this allows you to put a customized action into this graph's ActionMap. The added action MUST be assigned a name,
	 * which will be used as the key to retrieve it from the ActionMap
	 */
	public void addAction(Action action) {
		if (action == null)
			throw new NullPointerException("You can't put a null into the action HashMap!");
		if (action.getValue(Action.NAME) == null)
			throw new RuntimeException("You can't put an action that doesn't have a name to the action HashMap!");
		getActionMap().put(action.getValue(Action.NAME), action);
	}

	/** remove an action from this graph's ActionMap. */
	public void removeAction(Action action) {
		if (action == null)
			throw new NullPointerException("You can't put a null into the action HashMap!");
		if (action.getValue(Action.NAME) == null)
			throw new RuntimeException("You can't put an action that doesn't have a name to the action HashMap!");
		getActionMap().remove(action.getValue(Action.NAME));
	}

	public Point getToolTipLocation(MouseEvent e) {
		return new Point(e.getX() + 20 + origin.x, e.getY() - 20 + origin.y);
	}

	/**
	 * set the responding time of this graph's data sources to mouse dragging actions. By default, it is 100
	 * milliseconds. Setting a drag responding time is primarily for avoiding doing expensive computation every time the
	 * system's event dispatcher fires a mouse drag event. If we do not prevent this, too many computing tasks (many of
	 * them may be unnecessary) will jam up the CPU. As a result, the graph may become significantly irresponsive to
	 * user's dragging actions. We recommend 100 ms for computers with ~500 MHz CPU, but you may use larger values if
	 * you have to work with slower school computers.
	 */
	public void setDragRespondingTime(int i) {
		dragRespondingTime = i;
	}

	/**
	 * get the responding time of this graph's data sources to mouse dragging actions
	 */
	public int getDragRespondingTime() {
		return dragRespondingTime;
	}

	public void setBorderColor(Color c) {
		borderColor = c;
	}

	public Color getBorderColor() {
		return borderColor;
	}

	public void setClipRatio(float x) {
		clipRatio = x;
	}

	public float getClipRatio() {
		return clipRatio;
	}

	public void setCoordinatePlaneDraggable(boolean b) {
		allowDragCoordinatePlane = b;
	}

	public boolean isCoordinatePlaneDraggable() {
		return allowDragCoordinatePlane;
	}

	/**
	 * set the original scope of the graph. These information is used for resetting the graph.
	 */
	public void setOriginalScope(float xmin, float xmax, float ymin, float ymax) {
		oldXmin = xmin;
		oldXmax = xmax;
		oldYmin = ymin;
		oldYmax = ymax;
	}

	/**
	 * reset the scope of this graph. If the original scope is not set, calling this method will cause no effect.
	 */
	public synchronized void resetScope() {
		if (oldXmin != OLD_VALUE && oldXmax != OLD_VALUE) {
			setXBounds(oldXmin, oldXmax);
		}
		else {
		}
		if (oldYmin != OLD_VALUE && oldYmax != OLD_VALUE) {
			setYBounds(oldYmin, oldYmax);
		}
		else {
		}
		zoomScale = 0;
		repaint();
	}

	/** this is used for translating the graph when the use presses a key */
	private synchronized void keyTranslate(int direction) {

		if (allowDragCoordinatePlane) {

			float xmin = xAxis.getMin();
			float xmax = xAxis.getMax();
			float ymin = yAxis.getMin();
			float ymax = yAxis.getMax();

			switch (direction) {
			case KeyEvent.VK_RIGHT:
				float dx = xmax - xmin;
				xmin -= movePercent * dx;
				xmax -= movePercent * dx;
				setXBounds(xmin, xmax);
				break;
			case KeyEvent.VK_LEFT:
				dx = xmax - xmin;
				xmin += movePercent * dx;
				xmax += movePercent * dx;
				setXBounds(xmin, xmax);
				break;
			case KeyEvent.VK_UP:
				float dy = ymax - ymin;
				ymin -= movePercent * dy;
				ymax -= movePercent * dy;
				if (ymin * yScale > -getHeight() * 2)
					setYBounds(ymin, ymax);
				break;
			case KeyEvent.VK_DOWN:
				dy = ymax - ymin;
				ymin += movePercent * dy;
				ymax += movePercent * dy;
				if (ymax * yScale < getHeight() * 2)
					setYBounds(ymin, ymax);
				break;
			}
		}

		repaint();

	}

	/** zoom in this graph for a step */
	public synchronized void zoomIn(float zoomPercent) {

		if (zoomScale >= zoomScaleMax)
			return;

		float xmin = xAxis.getMin();
		float xmax = xAxis.getMax();
		float ymin = yAxis.getMin();
		float ymax = yAxis.getMax();

		float dx = xmax - xmin;
		float num = dx / (xAxis.getMajorTic());
		if (dx / num * zoomPercent < 0.001) {
			zoomScale = zoomScaleMax;
			return;
		}
		if (oldXmin != OLD_VALUE && oldXmax != OLD_VALUE) {
			dx = oldXmax - oldXmin;
		}
		xmin *= zoomPercent;
		xmax *= zoomPercent;
		if (xmin >= xmax - TOLERANCE)
			return;

		float dy = ymax - ymin;
		num = dy / yAxis.getMajorTic();
		if (dy / num * zoomPercent < 0.001) {
			zoomScale = zoomScaleMax;
			return;
		}
		if (oldYmin != OLD_VALUE && oldYmax != OLD_VALUE) {
			dy = oldYmax - oldYmin;
		}
		ymin *= zoomPercent;
		ymax *= zoomPercent;
		if (ymin >= ymax - TOLERANCE)
			return;

		setXBounds(xmin, xmax);
		setYBounds(ymin, ymax);

		zoomScale++;
		repaint();

	}

	/** zoom out this graph for a step */
	public synchronized void zoomOut(float zoomPercent) {

		if (zoomScale <= zoomScaleMin)
			return;

		float xmin = xAxis.getMin();
		float xmax = xAxis.getMax();
		float ymin = yAxis.getMin();
		float ymax = yAxis.getMax();

		if (xmin <= -10000 || xmax >= 10000 || ymin <= -10000 || ymax >= 10000) {
			zoomScale = zoomScaleMin;
			return;
		}
		xmin /= zoomPercent;
		xmax /= zoomPercent;
		setXBounds(xmin, xmax);

		ymin /= zoomPercent;
		ymax /= zoomPercent;
		setYBounds(ymin, ymax);

		zoomScale--;
		repaint();

	}

	/**
	 * set whether or not the curves should be recomputed once the bounds of the x axis are changed
	 */
	public void setRecompute(boolean b) {
		recompute = b;
	}

	/**
	 * whether or not the curves should be recomputed once the bounds of the x axis is changed
	 */
	public boolean getRecompute() {
		return recompute;
	}

	/** set maximum number of zooming-out actions allowed */
	public void setZoomScaleMin(int num) {
		zoomScaleMin = num;
	}

	/** return maximum number of zooming-out actions allowed */
	public int getZoomScaleMin() {
		return zoomScaleMin;
	}

	/** set maximum number of zooming-in actions allowed */
	public void setZoomScaleMax(int num) {
		zoomScaleMax = num;
	}

	/** return maximum number of zooming-in actions allowed */
	public int getZoomScaleMax() {
		return zoomScaleMax;
	}

	/** set the percentage of each move of the coordinate system by key */
	public void setMovePercent(float percent) {
		if (percent <= 0.0f || percent >= 1.0f)
			throw new IllegalArgumentException("Please input a number greater than 0 and less than 1");
		movePercent = percent;
	}

	/** get the percentage of each move of the coordinate system by key */
	public float getMovePercenter() {
		return movePercent;
	}

	/** set the upper and lower bounds of this graph in x direction */
	public synchronized void setXBounds(float xmin, float xmax) {
		if (xmin >= xmax)
			throw new IllegalArgumentException("Max must be greater than min");
		xScale = getPreferredSize().width * (1.0f - clipRatio - clipRatio) / (xmax - xmin);
		if (xmin * xmax < 0.0f) {
			origin.x = (int) (-xmin * xScale + getPreferredSize().width * clipRatio);
		}
		else if (xmin >= 0 && xmax > 0) {
			origin.x = (int) (getPreferredSize().width * clipRatio - xmin * xScale);
		}
		else if (xmin < 0 && xmax <= 0) {
			origin.x = (int) (getPreferredSize().width * (1.0f - clipRatio) - xmax * xScale);
		}
		float dx = xAxis.getMax() - xAxis.getMin();
		float it = xAxis.getMajorTic() / dx;
		xAxis.setMin(xmin);
		xAxis.setMax(xmax);
		xAxis.setMajorTic(it * (xmax - xmin));
		if (recompute)
			recomputeData();
	}

	/** set the upper and lower bounds of this graph in y direction */
	public synchronized void setYBounds(float ymin, float ymax) {
		if (ymin >= ymax)
			throw new IllegalArgumentException("Max must be greater than min");
		yScale = getPreferredSize().height * (1.0f - clipRatio - clipRatio) / (ymax - ymin);
		if (ymin * ymax < 0.0f) {
			origin.y = (int) (ymax * yScale + getPreferredSize().height * clipRatio);
		}
		else if (ymin >= 0 && ymax > 0) {
			origin.y = (int) (getPreferredSize().height * (1.0f - clipRatio) + ymin * yScale);
		}
		else if (ymin < 0 && ymax <= 0) {
			origin.y = (int) (getPreferredSize().height * clipRatio + ymax * yScale);
		}
		float dy = yAxis.getMax() - yAxis.getMin();
		float it = yAxis.getMajorTic() / dy;
		yAxis.setMin(ymin);
		yAxis.setMax(ymax);
		yAxis.setMajorTic(it * (ymax - ymin));
	}

	/** recompute all the data sources in this graph */
	private void recomputeData() {
		if (data.isEmpty())
			return;
		synchronized (data) {
			for (DataSource ds : data) {
				recomputeData(ds);
			}
		}
	}

	/** recompute the specified data source */
	private void recomputeData(DataSource ds) {
		float dx = (float) (ds.getHotSpot().getX() - ds.getOldHotSpot().getX());
		float dy = (float) (ds.getHotSpot().getY() - ds.getOldHotSpot().getY());
		ds.generateData(xAxis.getMin() - dx, xAxis.getMax() - dx);
		ds.translateData(dx, -dy);
	}

	/**
	 * change the specified data source according to the hotspot's current position
	 */
	private void changeData(DataSource ds) {
		Ellipse2D hp = ds.getHotSpot();
		// if a math expression is available for this data source, regenerate the data
		if (ds.getExpression() != null) {
			hp.setFrame((handle.getX() + handle.getWidth() * 0.5) / xScale, (handle.getY() + handle.getHeight() * 0.5)
					/ yScale, hp.getWidth(), hp.getHeight());
			recomputeData(ds);
		}
		else {// otherwise just translate the data array
			double dx = hp.getX();
			double dy = hp.getY();
			hp.setFrame((handle.getX() + handle.getWidth() * 0.5) / xScale, (handle.getY() + handle.getHeight() * 0.5)
					/ yScale, hp.getWidth(), hp.getHeight());
			dx = hp.getX() - dx;
			dy -= hp.getY();
			ds.translateData((float) dx, (float) dy);
		}
		firePropertyChange("Data changed", null, hp);
	}

	/** return the scale in x direction */
	public float getXScale() {
		return xScale;
	}

	/** return the scale in y direction */
	public float getYScale() {
		return yScale;
	}

	public DataSource getDataSource(int i) {
		if (data == null)
			return null;
		if (i < 0 || i >= data.size())
			return null;
		return data.get(i);
	}

	/**
	 * add a data source to this graph. A property change event entitled with "Data source added" will be fired to
	 * notify whomever has registered interest with this graph's property change events. The newly added data source is
	 * referenced by the property change event's newValue field.
	 */
	public void addDataSource(DataSource ds) {
		if (ds == null)
			return;
		data.add(ds);
		repaint();
		firePropertyChange("Data source added", null, ds);
	}

	/**
	 * remove a data source from this graph. A property change event entitled with "Data source removed" will be fired
	 * if the data source has been successfully removed. The removed data source is referenced by the property change
	 * event's newValue field.
	 */
	public void removeDataSource(DataSource ds) {
		if (data.isEmpty())
			return;
		if (data.remove(ds)) {
			repaint();
			firePropertyChange("Data source removed", null, ds);
		}
	}

	/**
	 * remove all data sources. A property change event entitled with "All removed" will be fired if there were at least
	 * one data source before removal.
	 */
	public void removeAll() {
		if (data.isEmpty())
			return;
		data.clear();
		repaint();
		firePropertyChange("All removed", false, true);
	}

	public List<String> getExpressions() {
		if (data == null || data.isEmpty())
			return null;
		List<String> list = new ArrayList<String>();
		for (DataSource ds : data) {
			list.add(ds.getExpression());
		}
		return list;
	}

	/** return the number of data sources in this graph */
	public int getNumberOfDataSources() {
		return data.size();
	}

	public boolean isEmpty() {
		if (data == null)
			return true;
		return data.isEmpty();
	}

	/**
	 * set the maximum time you would tolerate to determine whether a function is a proper one. The default value is
	 * 10,000 milliseconds (10 seconds).
	 */
	public void setMaximumSearchTime(long l) {
		maximumSearchingTime = l;
	}

	/**
	 * get the maximum time you would tolerate to determine whether a function is a proper one
	 */
	public long getMaximumSearchingTime() {
		return maximumSearchingTime;
	}

	/**
	 * add a data source which will be given the data array generated using the specified math expression. Note: This
	 * math expression MUST contain x as the ONLY variable.
	 * 
	 * @param expression
	 *            the math expression that represents the added data source
	 * @param hotspot
	 *            if true, the added function will have a hotspot; otherwise, it won't.
	 */
	public synchronized void addFunction(String expression, boolean hotspot) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		DataSource dataSource = new DataSource();
		dataSource.setVariableValue("x", 0);
		dataSource.setExpression(expression);
		dataSource.setPrincipalVariable("x");
		try {
			dataSource.generateData(xAxis.getMin(), xAxis.getMax());
			float x0 = 0.5f * (xAxis.getMin() + xAxis.getMax());
			float dx = 0.5f * (xAxis.getMax() - xAxis.getMin());
			long time0 = System.currentTimeMillis();
			// be careful here! We must avoid choosing a singularity point as hotspot
			try {
				float y0 = dataSource.evaluate(x0);
				long longBits = Double.doubleToLongBits(y0);
				while (longBits == Double.doubleToLongBits(Double.NaN)
						|| longBits == Double.doubleToLongBits(Double.NEGATIVE_INFINITY)
						|| longBits == Double.doubleToLongBits(Double.POSITIVE_INFINITY)) {
					x0 += dx * (0.5 - Math.random());
					// use a random number to avoid being trapped by periodicity
					y0 = dataSource.evaluate(x0);
					longBits = Double.doubleToLongBits(y0);
					if (System.currentTimeMillis() - time0 > maximumSearchingTime) {
						JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(Graph.this), "Your input <"
								+ expression + "> is found to be an improper function" + '\n'
								+ "in the current data window: [" + xAxis.getMin() + ", " + xAxis.getMax() + "]."
								+ '\n' + "Please input another one, or adjust the range of the data window" + '\n'
								+ "and try again.", "Error", JOptionPane.ERROR_MESSAGE);
						setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						return;
					}
				}
				dataSource.setHotSpot(x0, y0, 10, 10);
			}
			catch (Exception exception) {// if x0 happens to be a singular point
				exception.printStackTrace(System.err);
			}
			dataSource.setHideHotSpot(!hotspot);
		}
		catch (Exception exception) {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(Graph.this), "Your input <" + expression
					+ "> cannot be recognized. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		dataSource.setName(expression);
		addDataSource(dataSource);
		repaint();
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	/** return either axis of this graph */
	public Axis getAxis(int orient) {
		if (orient == Axis.X_AXIS)
			return xAxis;
		if (orient == Axis.Y_AXIS)
			return yAxis;
		return null;
	}

	/**
	 * set the origin of this graph in its intrinsic coordinate system. Remember, the origin bears the coordinates in
	 * the component's coordinate system, NOT in the data source's. (Therefore, it is a Point.)
	 */
	public void setOrigin(Point p) {
		origin = new Point(p);
	}

	/** return the origin of this graph */
	public Point getOrigin() {
		return origin;
	}

	/** set the handle's current position to be that of a data source's hotspot */
	private void linkHotSpotToHandle(Ellipse2D hp) {
		handle.setFrame(hp.getX() * xScale - hp.getWidth() * 0.5, -hp.getY() * yScale - hp.getHeight() * 0.5, hp
				.getWidth(), hp.getHeight());
	}

	/** visualize the graph */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		// use high-quality graphcis
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// store the original stroke and color before painting
		Stroke oldStroke = g2.getStroke();
		Color oldColor = g.getColor();

		// paint the background
		Dimension size = getSize();
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, size.width, size.height);
		}

		// set the clip area
		/*
		 * g.clipRect((int) (size.width * clipRatio), (int) (size.height * clipRatio), (int) (size.width * (1.0f -
		 * clipRatio - clipRatio)), (int) (size.height * (1.0f - clipRatio - clipRatio)));
		 */

		// render the axises
		xAxis.setLength(size.width);
		xAxis.draw(g);
		yAxis.setLength(size.height);
		yAxis.draw(g);

		// translate this component's intrinsic coordinate system to the origin
		if (origin == null)
			origin = new Point(size.width / 2, size.height / 2);
		g.translate(origin.x, origin.y);
		Rectangle rect = getBounds();

		// render the data sources in this graph
		if (!data.isEmpty()) {

			float[] s;
			Ellipse2D hotSpot;
			float xp, yp;
			int pointer;
			int wmin, wmax, hmin, hmax;
			float deltaX = 0.0f;
			boolean caught = false;
			caughtSingularity = false;

			// lock "data" to prevent another thread from adding or removing a data source while painting
			synchronized (data) {

				for (DataSource ds : data) {

					s = ds.getDataArray();
					pointer = ds.getPointer();

					if (detectSingularities) {
						if (deriv == null) {
							deriv = new float[pointer / 2];
						}
						else if (deriv.length < pointer / 2) {
							deriv = new float[pointer / 2];
						}
					}

					if (s != null && pointer > 2) {

						g.setColor(ds.isSelected() ? (ds.getHighlightColor() == null ? ds.getColor() : ds
								.getHighlightColor()) : ds.getColor());
						g2.setStroke(ds.getStroke());
						path.reset(); // always reuse the GeneralPath
						path.moveTo(xScale * s[0], yScale * s[1]);
						if (detectSingularities)
							deriv[0] = ds.derivative(0);
						int j2 = 0;
						for (int j = 2; j < pointer - 2; j += 2) {
							xp = xScale * s[j];
							yp = yScale * s[j + 1];
							wmin = -origin.x - size.width;
							wmax = size.width * 2 - origin.x;
							hmin = -origin.y - size.height;
							hmax = size.height * 2 - origin.y;
							caught = false;
							j2 = j / 2;
							// detect singularities per draw is not very slow, but it is not ideal either
							if (detectSingularities) {
								deriv[j2] = ds.derivative(j2);
								if (Math.abs(deriv[j2] - deriv[j2 - 1]) > minDeriv) {
									// use the change of derivative as the criterion
									caught = true;
									deltaX = (s[j] - s[j - 2]) * xScale;
									if (s[j + 1] * s[j - 1] < 0.0f) {
										if (s[j + 1] > 0) {
											yp = hmin - 10;
											path.lineTo(xp - deltaX * 0.5f, yp);
											yp = hmax + 10;
											path.moveTo(xp - deltaX * 0.5f, yp);
											// moveTo must be used to break the line, same below
										}
										else {
											yp = hmax + 10;
											path.lineTo(xp - deltaX * 0.5f, yp);
											yp = hmin - 10;
											path.moveTo(xp - deltaX * 0.5f, yp);
										}
									}
									else {
										if (s[j + 1] > 0 || s[j - 1] > 0) {
											yp = hmax + 10;
										}
										else {
											yp = hmin - 10;
										}
										path.lineTo(xp - deltaX * 0.5f, yp);
										path.moveTo(xp - deltaX * 0.5f, yp);
									}
								}
								if (!caught) {
									if (xp > wmin && xp < wmax && yp > hmin && yp < hmax) {
										path.lineTo(xp, yp);
									}
								}
								else {
									caughtSingularity = true;
									// System.out.println(s[j]+": "+s[j+1]+","+s[j-1]);
								}
							}
							else {
								if (xp > wmin && xp < wmax && yp > hmin && yp < hmax) {
									path.lineTo(xp, yp);
								}
							}
						}
						g2.draw(path);

						if (!ds.getHideHotSpot()) {
							hotSpot = ds.getHotSpot();
							if (hotSpot != null) {// draw the hotspot of the current data source
								linkHotSpotToHandle(hotSpot);
								if (rect.intersects(handle.getBounds())) {
									g2.setStroke(handleStroke);
									g.setColor(Color.black);
									g2.draw(handle);
									/*
									 * if(!mouseBeingDragged){ g.setColor(ds.getHandleColor()); } else {
									 * g.setColor(ds.isSelected()? Color.yellow:ds.getHandleColor()); }
									 */
									g.setColor(ds.getHandleColor());
									g2.fill(handle);
								}
							}
						}

						if (ds.isSelectedPointShown()) {
							g.setColor(new Color(255 - getBackground().getRed(), 255 - getBackground().getGreen(),
									255 - getBackground().getBlue()));
							g.fillOval((int) (ds.getSelectedX() * xScale - 4), (int) (-ds.getSelectedY() * yScale - 4),
									8, 8);
							g.setColor(Color.white);
							g.fillOval((int) (ds.getSelectedX() * xScale - 2), (int) (-ds.getSelectedY() * yScale - 2),
									4, 4);
						}

					}

				}

			}

		}

		g.setColor(borderColor);
		g2.setStroke(borderStroke);
		g.translate(-origin.x, -origin.y);
		g.drawRect(1, 1, size.width - 3, size.height - 3);

		// restore the original stroke and color before leaving this method
		g2.setStroke(oldStroke);
		g.setColor(oldColor);
		g.dispose();

	}

	public void init() {

		// setEnabled(false);
		// setCoordinatePlaneDraggable(false);
		setToolTipText("Graph");
		setPreferredSize(new Dimension(400, 400));
		setBackground(Color.white);
		// setClipRatio(0.0f);
		setXBounds(-5f, 5f);
		setYBounds(-5f, 5f);
		setOriginalScope(-5f, 5f, -5f, 5f);
		xAxis.setMajorTic(1f);
		yAxis.setMajorTic(1f);
		xAxis.setTitle("x");
		yAxis.setTitle("Y");
		setDragRespondingTime(100);
		setRecompute(true);
		enableSingularityDetection(true);

		/*
		 * DataSource dataSource = new DataSource(200); dataSource.setVariableValue("a", 1);
		 * dataSource.setVariableValue("b", 1.5f); dataSource.setVariableValue("x", 1);
		 * dataSource.setExpression("tan(x)"); dataSource.setPrincipalVariable("x");
		 * dataSource.generateData(xAxis.getMin(), xAxis.getMax()); dataSource.setHotSpot(1, dataSource.evaluate(1), 10,
		 * 10); dataSource.setColor(Color.red); dataSource.setStroke(new BasicStroke(3.0f));
		 * dataSource.setHideHotSpot(true); addDataSource(dataSource);
		 * 
		 * DataSource dataSource1 = new DataSource(200); dataSource1.setVariableValue("a", 0.5f);
		 * dataSource1.setVariableValue("b", 0); dataSource1.setVariableValue("c", -4);
		 * dataSource1.setExpression("sin(x)"); dataSource1.setPrincipalVariable("x");
		 * dataSource1.generateData(xAxis.getMin(), xAxis.getMax()); dataSource1.setHotSpot(0, dataSource1.evaluate(0),
		 * 10, 10); dataSource1.setHideHotSpot(true); dataSource1.setColor(Color.blue); dataSource1.setStroke(new
		 * BasicStroke(3.0f)); dataSource1.setName("sine(x)"); dataSource1.setHighlightColor(null);
		 * addDataSource(dataSource1);
		 */

	}

	/** test the graph. There are also examples of using the graph. */
	public static void main(String agvs[]) {
		JFrame f = new JFrame("Graphing Window");
		Graph graph = new Graph();
		graph.init();
		f.setContentPane(graph);
		f.pack();
		f.setVisible(true);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
	}

}