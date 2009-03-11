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

package org.concord.modeler.draw;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.ui.PrintableComponent;
import org.concord.modeler.ui.TextBox;
import org.concord.modeler.draw.ui.EllipsePropertiesPanel;
import org.concord.modeler.draw.ui.LinePropertiesPanel;
import org.concord.modeler.draw.ui.RectanglePropertiesPanel;
import org.concord.modeler.draw.ui.TextBoxPanel;
import org.concord.modeler.draw.ui.TrianglePropertiesPanel;

public abstract class Draw extends PrintableComponent {

	public final static byte DEFAULT_MODE = 0x00;
	public final static byte LINE_MODE = 0x01;
	public final static byte RECT_MODE = 0x02;
	public final static byte ELLIPSE_MODE = 0x03;
	public final static byte TRIANGLE_MODE = 0x04;
	public final static byte MEASURE_MODE = 0x05;

	private static PrinterJob printerJob;
	private static Cursor rulerCursor;
	private static ResourceBundle bundle;
	private static boolean isUSLocale;

	private byte mode = DEFAULT_MODE;
	private boolean editable = true;
	private boolean showElements = true;
	private List<DrawingElement> componentList;
	private Point clickPoint = new Point();
	private Point dragPoint = new Point();
	private Point pressPoint = new Point();
	private boolean dragging;
	private Object bufferedElement;
	private DrawingElement selectedElement;
	private short copyCounter = 1;
	private boolean showGrid;
	private short cellSize = 20;
	private Color gridColor = Color.lightGray;
	private Color measureLineColor = Color.red;
	private Rectangle selectedArea = new Rectangle();
	private Point anchorPoint = new Point();
	private Color contrastBgColor;
	private boolean lineHasArrowByDefault;

	private JPopupMenu popupMenu;
	private CallOut callOutWindow;
	private boolean isCallOutWindowShown;

	public Draw() {

		if (rulerCursor == null)
			rulerCursor = createCustomCursor("resources/RulerCursor.gif", new Point(10, 9), "ruler");

		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			if (!isUSLocale) {
				try {
					bundle = ResourceBundle.getBundle("org.concord.modeler.draw.resources.Draw", Locale.getDefault());
				}
				catch (MissingResourceException e) {
				}
			}
		}

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				processMousePressed(e);
			}

			public void mouseReleased(MouseEvent e) {
				processMouseReleased(e);
			}

			public void mouseEntered(MouseEvent e) {
				processMouseEntered(e);
			}

			public void mouseExited(MouseEvent e) {
				processMouseExited(e);
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				processMouseMoved(e);
			}

			public void mouseDragged(MouseEvent e) {
				processMouseDragged(e);
			}
		});

		addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				processMouseWheelMoved(e);
			}
		});

		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				processKeyPressed(e);
			}

			public void keyReleased(KeyEvent e) {
				processKeyReleased(e);
			}
		});

		Action a = new AbstractAction("Cut") {
			public void actionPerformed(ActionEvent e) {
				if (selectedElement != null)
					removeElement(selectedElement);
			}
		};
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_U));
		a.putValue(Action.NAME, "Cut");
		a.putValue(Action.SHORT_DESCRIPTION, "Remove the selected element");
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_X, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK,
				true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Cut");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), "Cut");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, true), "Cut");
		getActionMap().put("Cut", a);

		a = new AbstractAction("Copy") {
			public void actionPerformed(ActionEvent e) {
				copySelectedElement();
			}
		};
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		a.putValue(Action.NAME, "Copy");
		a.putValue(Action.SHORT_DESCRIPTION, "Copy");
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_C, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK,
				true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Copy");
		getActionMap().put("Copy", a);

		a = new AbstractAction("Paste") {
			public void actionPerformed(ActionEvent e) {
				pasteElement();
			}
		};
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		a.putValue(Action.NAME, "Paste");
		a.putValue(Action.SHORT_DESCRIPTION, "Paste");
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_V, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK,
				true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Paste");
		getActionMap().put("Paste", a);

		a = new AbstractAction("Print") {
			public void actionPerformed(ActionEvent e) {
				if (System.getProperty("os.name").startsWith("Mac")) { // Mac OS X, try 2nd time
					try {
						print();
					}
					catch (Throwable t) {
						t.printStackTrace();
						print();
					}
				}
				else {
					print();
				}
			}
		};
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		a.putValue(Action.NAME, "Print");
		a.putValue(Action.SHORT_DESCRIPTION, "Print");
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_P, KeyEvent.META_MASK, true) : KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK,
				true));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Print");
		getActionMap().put("Print", a);

		componentList = new ArrayList<DrawingElement>();

	}

	private static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (isUSLocale)
			return null;
		if (name == null)
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

	public void clear() {
		componentList.clear();
		if (selectedElement != null) {
			selectedElement.setSelected(false);
			selectedElement = null;
		}
		hidePopupText();
	}

	public void setLineHasArrowByDefault(boolean b) {
		lineHasArrowByDefault = b;
	}

	/** return the 24-bit color that is in contrast to the background. */
	public Color contrastBackground() {
		int i = 0xffffff ^ getBackground().getRGB();
		if (contrastBgColor == null || i != contrastBgColor.getRGB())
			contrastBgColor = new Color(i);
		return contrastBgColor;
	}

	public void setCallOutWindow(CallOut c) {
		callOutWindow = c;
	}

	public CallOut getCallOutWindow() {
		return callOutWindow;
	}

	public void showPopupText(final String text, final int bgRgb, final int x, final int y, final int w, final int h,
			final int xCallOut, final int yCallOut) {
		if (callOutWindow instanceof JComponent) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					final JComponent c = (JComponent) callOutWindow;
					c.setBackground(new Color(bgRgb));
					if (c instanceof TextBox) {
						((TextBox) c).decodeText(text);
					}
					c.setBounds(x, y, w, h);
					callOutWindow.setCallOut(xCallOut, yCallOut);
					add(c);
					isCallOutWindowShown = true;
					repaint();
					if (c instanceof TextBox) { // scroll back to the top position
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								((TextBox) c).setViewPosition(0, 0);
							}
						});
					}
				}
			});
		}
	}

	public void hidePopupText() {
		if (callOutWindow instanceof JComponent) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					remove((Component) callOutWindow);
					isCallOutWindowShown = false;
					repaint();
				}
			});
		}
	}

	public void setEditable(boolean b) {
		editable = b;
	}

	public boolean isEditable() {
		return editable;
	}

	public void showElements(boolean b) {
		showElements = b;
	}

	public boolean areElementsShown() {
		return showElements;
	}

	public void setMode(byte i) {
		mode = i;
		switch (mode) {
		case DEFAULT_MODE:
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			break;
		case LINE_MODE:
		case RECT_MODE:
		case ELLIPSE_MODE:
		case TRIANGLE_MODE:
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			break;
		case MEASURE_MODE:
			setCursor(rulerCursor);
			break;
		}
	}

	public byte getMode() {
		return mode;
	}

	public void setGridColor(Color c) {
		gridColor = c;
	}

	public Color getGridColor() {
		return gridColor;
	}

	public void setMeasureLineColor(Color c) {
		measureLineColor = c;
	}

	public Color getMeasureLineColor() {
		return measureLineColor;
	}

	protected void processMouseWheelMoved(MouseWheelEvent e) {
		if (e.getWheelRotation() > 0) {
			if (cellSize < 100)
				cellSize++;
		}
		else {
			if (cellSize > 10)
				cellSize--;
		}
		repaint();
	}

	protected void processMouseMoved(MouseEvent e) {
		if (!editable)
			return;
		if (mode != DEFAULT_MODE)
			return;
		if (componentList.isEmpty())
			return;
		boolean above = false;
		int x = e.getX();
		int y = e.getY();
		if (selectedElement instanceof TextContainer) {
			if (((TextContainer) selectedElement).nearCallOutPoint(x, y)) {
				above = true;
			}
		}
		else if (selectedElement instanceof AbstractLine) {
			if (((AbstractLine) selectedElement).nearEndPoint(x, y) > 0) {
				above = true;
			}
		}
		else if (selectedElement instanceof AbstractRectangle) {
			if (selectHandleCursorForRectangularShape(this, ((AbstractRectangle) selectedElement).nearHandle(x, y)))
				return;
		}
		else if (selectedElement instanceof AbstractEllipse) {
			if (selectHandleCursorForRectangularShape(this, ((AbstractEllipse) selectedElement).nearHandle(x, y)))
				return;
		}
		else if (selectedElement instanceof AbstractTriangle) {
			if (((AbstractTriangle) selectedElement).nearHandle(x, y) != -1) {
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				return;
			}
		}
		if (!above) {
			for (DrawingElement d : componentList) {
				if (d.contains(x, y)) {
					above = true;
					break;
				}
			}
		}
		setCursor(Cursor.getPredefinedCursor(above ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
	}

	public static boolean selectHandleCursorForRectangularShape(Component c, byte i) {
		switch (i) {
		case AbstractRectangle.UPPER_LEFT:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
			return true;
		case AbstractRectangle.UPPER_RIGHT:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
			return true;
		case AbstractRectangle.LOWER_RIGHT:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
			return true;
		case AbstractRectangle.LOWER_LEFT:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
			return true;
		case AbstractRectangle.TOP:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
			return true;
		case AbstractRectangle.RIGHT:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
			return true;
		case AbstractRectangle.BOTTOM:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
			return true;
		case AbstractRectangle.LEFT:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
			return true;
		case AbstractRectangle.ARC_HANDLE:
			c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			return true;
		}
		return false;
	}

	protected Point getPressedPoint() {
		return pressPoint;
	}

	private void setAnchorPointForRectangularShape(byte i, float x, float y, float w, float h) {
		switch (i) {
		case AbstractRectangle.UPPER_LEFT:
			anchorPoint.setLocation(x + w, y + h);
			break;
		case AbstractRectangle.UPPER_RIGHT:
			anchorPoint.setLocation(x, y + h);
			break;
		case AbstractRectangle.LOWER_RIGHT:
			anchorPoint.setLocation(x, y);
			break;
		case AbstractRectangle.LOWER_LEFT:
			anchorPoint.setLocation(x + w, y);
			break;
		case AbstractRectangle.TOP:
			anchorPoint.setLocation(x, y + h);
			break;
		case AbstractRectangle.RIGHT:
			anchorPoint.setLocation(x, y);
			break;
		case AbstractRectangle.BOTTOM:
			anchorPoint.setLocation(x, y);
			break;
		case AbstractRectangle.LEFT:
			anchorPoint.setLocation(x + w, y);
			break;
		}
	}

	protected void processMouseEntered(MouseEvent e) {
	}

	protected void processMouseExited(MouseEvent e) {
	}

	private void select(int x, int y) {
		if (selectedElement instanceof TextContainer) {
			TextContainer t = (TextContainer) selectedElement;
			boolean b = t.nearCallOutPoint(x, y);
			t.setChangingCallOut(b);
			if (b)
				return;
			t.setSelected(false);
			selectedElement = null;
		}
		else if (selectedElement instanceof AbstractLine) {
			AbstractLine l = (AbstractLine) selectedElement;
			int i = l.nearEndPoint(x, y);
			l.setSelectedEndPoint(i);
			if (i > 0)
				return;
			l.setSelected(false);
			selectedElement = null;
		}
		else if (selectedElement instanceof AbstractRectangle) {
			AbstractRectangle r = (AbstractRectangle) selectedElement;
			byte i = r.nearHandle(x, y);
			r.setSelectedHandle(i);
			setAnchorPointForRectangularShape(i, r.getX(), r.getY(), r.getWidth(), r.getHeight());
			if (i >= 0)
				return;
			r.setSelected(false);
			selectedElement = null;
		}
		else if (selectedElement instanceof AbstractEllipse) {
			AbstractEllipse r = (AbstractEllipse) selectedElement;
			byte i = r.nearHandle(x, y);
			r.setSelectedHandle(i);
			setAnchorPointForRectangularShape(i, r.getX(), r.getY(), r.getWidth(), r.getHeight());
			if (i >= 0)
				return;
			r.setSelected(false);
			selectedElement = null;
		}
		else if (selectedElement instanceof AbstractTriangle) {
			AbstractTriangle r = (AbstractTriangle) selectedElement;
			byte i = r.nearHandle(x, y);
			r.setSelectedHandle(i);
			Rectangle rt = r.getBounds();
			setAnchorPointForRectangularShape(i, rt.x, rt.y, rt.width, rt.height);
			if (i >= 0)
				return;
			r.setSelected(false);
			selectedElement = null;
		}
		int n = componentList.size();
		if (n > 0) {
			DrawingElement t;
			for (int i = n - 1; i >= 0; i--) {
				t = componentList.get(i);
				if (t.contains(x, y) && t != selectedElement) {
					setSelectedElement(t);
					clickPoint.setLocation(x - t.getRx(), y - t.getRy());
					break;
				}
			}
		}
		repaint();
	}

	protected void processMousePressed(MouseEvent e) {
		requestFocusInWindow();
		int x = e.getX();
		int y = e.getY();
		pressPoint.x = x;
		pressPoint.y = y;
		switch (mode) {
		case DEFAULT_MODE:
			if (editable) {
				select(x, y);
			}
			break;
		case LINE_MODE:
			if (editable) {
				setSelectedElement(null);
				clickPoint.setLocation(x, y);
			}
			break;
		case MEASURE_MODE:
			clickPoint.setLocation(x, y);
			break;
		case RECT_MODE:
		case ELLIPSE_MODE:
		case TRIANGLE_MODE:
			if (editable) {
				setSelectedElement(null);
				selectedArea.setLocation(x, y);
				anchorPoint.setLocation(x, y);
			}
			break;
		}
		repaint();
	}

	private void dragSelectedArea(int x, int y) {
		if (x > anchorPoint.x) {
			selectedArea.width = x - anchorPoint.x;
			selectedArea.x = anchorPoint.x;
		}
		else {
			selectedArea.width = anchorPoint.x - x;
			selectedArea.x = anchorPoint.x - selectedArea.width;
		}
		if (y > anchorPoint.y) {
			selectedArea.height = y - anchorPoint.y;
			selectedArea.y = anchorPoint.y;
		}
		else {
			selectedArea.height = anchorPoint.y - y;
			selectedArea.y = anchorPoint.y - selectedArea.height;
		}
		repaint();
	}

	protected void processMouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		switch (mode) {
		case DEFAULT_MODE:
			if (editable && selectedElement != null) {
				if (selectedElement instanceof TextContainer) {
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					TextContainer t = (TextContainer) selectedElement;
					if (t.isChangingCallOut()) {
						t.setCallOutLocation(x, y);
						repaint();
						return;
					}
					t.translateTo(x - clickPoint.x, y - clickPoint.y);
					repaint();
				}
				else if (selectedElement instanceof AbstractLine) {
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					AbstractLine l = (AbstractLine) selectedElement;
					if (l.getSelectedEndPoint() == 1) {
						l.setEndPoint1(x, y);
						repaint();
						return;
					}
					if (l.getSelectedEndPoint() == 2) {
						l.setEndPoint2(x, y);
						repaint();
						return;
					}
					l.translateTo(x - clickPoint.x, y - clickPoint.y);
					repaint();
				}
				else if (selectedElement instanceof AbstractRectangle) {
					AbstractRectangle r = (AbstractRectangle) selectedElement;
					switch (r.getSelectedHandle()) {
					case AbstractRectangle.LOWER_LEFT:
					case AbstractRectangle.LOWER_RIGHT:
					case AbstractRectangle.UPPER_LEFT:
					case AbstractRectangle.UPPER_RIGHT:
						r.setRect(Math.min(x, anchorPoint.x), Math.min(y, anchorPoint.y), Math.abs(x - anchorPoint.x),
								Math.abs(y - anchorPoint.y));
						break;
					case AbstractRectangle.TOP:
					case AbstractRectangle.BOTTOM:
						r.setY(Math.min(y, anchorPoint.y));
						r.setHeight(Math.abs(y - anchorPoint.y));
						break;
					case AbstractRectangle.LEFT:
					case AbstractRectangle.RIGHT:
						r.setX(Math.min(x, anchorPoint.x));
						r.setWidth(Math.abs(x - anchorPoint.x));
						break;
					case AbstractRectangle.ARC_HANDLE:
						float arc = x - r.getX();
						if (arc < 0)
							arc = 0;
						else arc = Math.min(arc, 0.5f * Math.min(r.getWidth(), r.getHeight()));
						r.setArcWidth(2 * arc);
						r.setArcHeight(2 * arc);
						break;
					default:
						r.translateTo(x - clickPoint.x, y - clickPoint.y);
					}
					repaint();
				}
				else if (selectedElement instanceof AbstractEllipse) {
					AbstractEllipse r = (AbstractEllipse) selectedElement;
					switch (r.getSelectedHandle()) {
					case AbstractRectangle.LOWER_LEFT:
					case AbstractRectangle.LOWER_RIGHT:
					case AbstractRectangle.UPPER_LEFT:
					case AbstractRectangle.UPPER_RIGHT:
						r.setOval(Math.min(x, anchorPoint.x), Math.min(y, anchorPoint.y), Math.abs(x - anchorPoint.x),
								Math.abs(y - anchorPoint.y));
						break;
					case AbstractRectangle.TOP:
					case AbstractRectangle.BOTTOM:
						r.setY(Math.min(y, anchorPoint.y));
						r.setHeight(Math.abs(y - anchorPoint.y));
						break;
					case AbstractRectangle.LEFT:
					case AbstractRectangle.RIGHT:
						r.setX(Math.min(x, anchorPoint.x));
						r.setWidth(Math.abs(x - anchorPoint.x));
						break;
					default:
						r.translateTo(x - clickPoint.x, y - clickPoint.y);
					}
					repaint();
				}
				else if (selectedElement instanceof AbstractTriangle) {
					AbstractTriangle r = (AbstractTriangle) selectedElement;
					byte i = r.getSelectedHandle();
					if (i != -1) {
						r.setVertex(i, x, y);
					}
					else {
						r.translateTo(x - clickPoint.x, y - clickPoint.y);
					}
					repaint();
				}
			}
			else {
				if (Math.abs(x - dragPoint.x) > 1) {
					if (x > dragPoint.x) {
						if (cellSize < 100)
							cellSize++;
					}
					else {
						if (cellSize > 10)
							cellSize--;
					}
					dragPoint.setLocation(x, y);
					repaint();
				}
			}
			break;
		case LINE_MODE:
			if (editable) {
				dragging = true;
				dragPoint.setLocation(x, y);
				repaint();
			}
			break;
		case MEASURE_MODE:
			dragging = true;
			dragPoint.setLocation(x, y);
			repaint();
			break;
		case RECT_MODE:
		case ELLIPSE_MODE:
		case TRIANGLE_MODE:
			if (editable)
				dragSelectedArea(x, y);
			break;
		}
	}

	protected void processMouseReleased(MouseEvent e) {
		if (!editable)
			return;
		switch (mode) {
		case DEFAULT_MODE:
			if (isRightClick(e) && !e.isShiftDown() && !e.isControlDown()) {
				int x = e.getX();
				int y = e.getY();
				select(x, y);
				if (popupMenu == null)
					createPopupMenu();
				popupMenu.show(this, x + 5, y + 5);
				return;
			}
			if (selectedElement != null && e.getClickCount() >= 2)
				showDialog(selectedElement);
			break;
		case LINE_MODE:
			if (dragging) {
				if (selectedElement != null)
					selectedElement.setSelected(false);
				DefaultLine l = new DefaultLine(clickPoint.x, clickPoint.y, dragPoint.x, dragPoint.y);
				l.setSelected(true);
				l.setComponent(this);
				if (lineHasArrowByDefault)
					l.setEndStyle(ArrowRectangle.STYLE1);
				selectedElement = l;
				addElement(l);
				repaint();
				dragging = false;
			}
			break;
		case RECT_MODE:
			if (selectedArea.width > 0 && selectedArea.height > 0) {
				if (selectedElement != null)
					selectedElement.setSelected(false);
				DefaultRectangle r = new DefaultRectangle(selectedArea.x, selectedArea.y, selectedArea.width,
						selectedArea.height);
				r.setSelected(true);
				r.setComponent(this);
				selectedElement = r;
				addElement(r);
				selectedArea.setSize(0, 0);
				repaint();
			}
			break;
		case ELLIPSE_MODE:
			if (selectedArea.width > 0 && selectedArea.height > 0) {
				if (selectedElement != null)
					selectedElement.setSelected(false);
				DefaultEllipse r = new DefaultEllipse(selectedArea.x, selectedArea.y, selectedArea.width,
						selectedArea.height);
				r.setSelected(true);
				r.setComponent(this);
				selectedElement = r;
				addElement(r);
				selectedArea.setSize(0, 0);
				repaint();
			}
			break;
		case TRIANGLE_MODE:
			if (selectedArea.width > 0 && selectedArea.height > 0) {
				if (selectedElement != null)
					selectedElement.setSelected(false);
				DefaultTriangle r = new DefaultTriangle(selectedArea.x + selectedArea.width / 2, selectedArea.y,
						selectedArea.x, selectedArea.y + selectedArea.height, selectedArea.x + selectedArea.width,
						selectedArea.y + selectedArea.height);
				r.setSelected(true);
				r.setComponent(this);
				selectedElement = r;
				addElement(r);
				selectedArea.setSize(0, 0);
				repaint();
			}
			break;
		}
	}

	protected void processKeyPressed(KeyEvent e) {
		if (!editable)
			return;
		if (selectedElement == null)
			return;
		int key = e.getKeyCode();
		switch (key) {
		case KeyEvent.VK_UP:
			selectedElement.translateBy(0, -1);
			break;
		case KeyEvent.VK_DOWN:
			selectedElement.translateBy(0, 1);
			break;
		case KeyEvent.VK_LEFT:
			selectedElement.translateBy(-1, 0);
			break;
		case KeyEvent.VK_RIGHT:
			selectedElement.translateBy(1, 0);
			break;
		case KeyEvent.VK_DELETE:
		case KeyEvent.VK_BACK_SPACE:
			removeElement(selectedElement);
			break;
		case KeyEvent.VK_ENTER:
			showDialog(selectedElement);
			break;
		}
		repaint();
	}

	protected void processKeyReleased(KeyEvent e) {
	}

	public void setShowGrid(boolean b) {
		showGrid = b;
		repaint();
	}

	public boolean getShowGrid() {
		return showGrid;
	}

	public void setGridCellSize(short s) {
		cellSize = s;
		repaint();
	}

	public short getGridCellSize() {
		return cellSize;
	}

	public void addElement(DrawingElement e) {
		if (componentList.contains(e))
			return;
		componentList.add(e);
		if (e instanceof TextContainer)
			((TextContainer) e).setComponent(this);
		repaint();
	}

	public void removeElement(DrawingElement e) {
		bufferedElement = e;
		copyCounter = 1;
		componentList.remove(e);
		repaint();
	}

	public void copySelectedElement() {
		bufferedElement = selectedElement;
		copyCounter = 1;
	}

	public Object getPastingObject() {
		return bufferedElement;
	}

	public void setPastingObject(Object o) {
		bufferedElement = o;
	}

	public void pasteElement(int x, int y) {
		DrawingElement e = duplicateElement();
		if (e == null)
			return;
		e.translateTo(x, y);
		componentList.add(e);
		repaint();
	}

	public void pasteElement() {
		DrawingElement e = duplicateElement();
		if (e == null)
			return;
		e.translateBy(5 * copyCounter, 5 * copyCounter);
		componentList.add(e);
		copyCounter++;
		repaint();
	}

	private DrawingElement duplicateElement() {
		if (bufferedElement instanceof DefaultLine) {
			DefaultLine l = new DefaultLine((DefaultLine) bufferedElement);
			l.setLine((DefaultLine) bufferedElement);
			return l;
		}
		else if (bufferedElement instanceof DefaultRectangle) {
			DefaultRectangle r = new DefaultRectangle((DefaultRectangle) bufferedElement);
			r.setRect((DefaultRectangle) bufferedElement);
			return r;
		}
		else if (bufferedElement instanceof DefaultEllipse) {
			DefaultEllipse e = new DefaultEllipse((DefaultEllipse) bufferedElement);
			e.setOval((DefaultEllipse) bufferedElement);
			return e;
		}
		else if (bufferedElement instanceof DefaultTriangle) {
			DefaultTriangle t = new DefaultTriangle((DefaultTriangle) bufferedElement);
			t.setVertices((DefaultTriangle) bufferedElement);
			return t;
		}
		else if (bufferedElement instanceof TextContainer) {
			DefaultTextContainer c = new DefaultTextContainer((TextContainer) bufferedElement);
			c.setLocation(((TextContainer) bufferedElement).getRx(), ((TextContainer) bufferedElement).getRy());
			return c;
		}
		return null;
	}

	public DrawingElement[] getElements() {
		if (componentList.isEmpty())
			return null;
		DrawingElement[] e = new DrawingElement[componentList.size()];
		for (int i = 0; i < e.length; i++)
			e[i] = componentList.get(i);
		return e;
	}

	public List<DrawingElement> getDrawList() {
		return componentList;
	}

	public int getElementCount() {
		return componentList.size();
	}

	public DrawingElement getSelectedElement() {
		return selectedElement;
	}

	public void setSelectedElement(DrawingElement e) {
		if (selectedElement != e && selectedElement != null) {
			selectedElement.setSelected(false);
		}
		selectedElement = e;
		if (selectedElement != null && !selectedElement.isSelected())
			selectedElement.setSelected(true);
	}

	public void clearSelection() {
		if (componentList.isEmpty())
			return;
		for (DrawingElement e : componentList)
			e.setSelected(false);
		selectedElement = null;
	}

	public void sendBack() {
		if (selectedElement == null)
			return;
		if (componentList.isEmpty())
			return;
		int i = componentList.indexOf(selectedElement);
		if (i <= 0)
			return;
		if (componentList.remove(selectedElement))
			componentList.add(i - 1, selectedElement);
		repaint();
	}

	public void bringForward() {
		if (selectedElement == null)
			return;
		if (componentList.isEmpty())
			return;
		int i = componentList.indexOf(selectedElement);
		if (i == componentList.size() - 1)
			return;
		if (componentList.remove(selectedElement))
			componentList.add(i + 1, selectedElement);
		repaint();
	}

	public void sendToBack() {
		if (selectedElement == null)
			return;
		if (componentList.isEmpty())
			return;
		int i = componentList.indexOf(selectedElement);
		if (i <= 0)
			return;
		if (componentList.remove(selectedElement))
			componentList.add(0, selectedElement);
		repaint();
	}

	public void bringToFront() {
		if (selectedElement == null)
			return;
		if (componentList.isEmpty())
			return;
		int i = componentList.indexOf(selectedElement);
		if (i == componentList.size() - 1)
			return;
		if (componentList.remove(selectedElement))
			componentList.add(selectedElement);
		repaint();
	}

	public void update(Graphics g) {
		if (showGrid) {
			g.setColor(gridColor);
			int n = (int) ((float) getWidth() / (float) cellSize);
			for (int i = 0; i <= n; i++)
				g.drawLine(i * cellSize, 0, i * cellSize, getHeight());
			n = (int) ((float) getHeight() / (float) cellSize);
			for (int i = 0; i <= n; i++)
				g.drawLine(0, i * cellSize, getWidth(), i * cellSize);
		}
		if (showElements) {
			if (!componentList.isEmpty()) {
				for (DrawingElement e : componentList) {
					e.paint(g);
				}
			}
		}
		switch (mode) {
		case LINE_MODE:
			if (dragging && editable) {
				g.setColor(Color.red);
				g.drawLine(clickPoint.x, clickPoint.y, dragPoint.x, dragPoint.y);
			}
			break;
		case RECT_MODE:
			if (editable) {
				g.setColor(Color.red);
				g.drawRect(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
			}
			break;
		case ELLIPSE_MODE:
			if (editable) {
				g.setColor(Color.red);
				g.drawOval(selectedArea.x, selectedArea.y, selectedArea.width, selectedArea.height);
			}
			break;
		case TRIANGLE_MODE:
			if (editable) {
				g.setColor(Color.red);
				g.drawLine(selectedArea.x + selectedArea.width / 2, selectedArea.y, selectedArea.x, selectedArea.y
						+ selectedArea.height);
				g.drawLine(selectedArea.x + selectedArea.width / 2, selectedArea.y,
						selectedArea.x + selectedArea.width, selectedArea.y + selectedArea.height);
				g.drawLine(selectedArea.x, selectedArea.y + selectedArea.height, selectedArea.x + selectedArea.width,
						selectedArea.y + selectedArea.height);
			}
			break;
		case MEASURE_MODE:
			if (dragging) {
				g.setColor(measureLineColor);
				g.drawLine(clickPoint.x, clickPoint.y, dragPoint.x, dragPoint.y);
				g.drawLine(clickPoint.x - 4, clickPoint.y, clickPoint.x + 4, clickPoint.y);
				g.drawLine(clickPoint.x, clickPoint.y - 4, clickPoint.x, clickPoint.y + 4);
				g.drawLine(dragPoint.x - 4, dragPoint.y, dragPoint.x + 4, dragPoint.y);
				g.drawLine(dragPoint.x, dragPoint.y - 4, dragPoint.x, dragPoint.y + 4);
				int dx = clickPoint.x - dragPoint.x;
				int dy = clickPoint.y - dragPoint.y;
				int d = (int) Math.hypot(dx, dy);
				dx = dragPoint.x + (dx >> 1);
				dy = dragPoint.y + (dy >> 1);
				g.setColor(Color.white);
				int sw = g.getFontMetrics().stringWidth(d + "");
				g.fillRect(dx - (sw >> 1) - 5, dy - 8, sw + 10, 16);
				g.setColor(Color.black);
				g.drawString(d + "", dx - (sw >> 1), dy + 4);
			}
			break;
		}
		if (isCallOutWindowShown && callOutWindow != null) {
			callOutWindow.paintCallOut(g);
		}
	}

	private void showDialog(DrawingElement e) {
		if (!EventQueue.isDispatchThread())
			throw new RuntimeException("must be called by the Event Dispatch Thread");
		JDialog d = null;
		if (e instanceof AbstractLine)
			d = createDialog((AbstractLine) e);
		else if (e instanceof AbstractRectangle)
			d = createDialog((AbstractRectangle) e);
		else if (e instanceof AbstractEllipse)
			d = createDialog((AbstractEllipse) e);
		else if (e instanceof AbstractTriangle)
			d = createDialog((AbstractTriangle) e);
		else if (e instanceof TextContainer)
			d = createDialog((TextContainer) e);
		if (d != null)
			d.setVisible(true);
	}

	private JDialog createDialog(final AbstractLine l) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this), "Line Properties", true);
		final LinePropertiesPanel p = new LinePropertiesPanel(l) {
			public int getIndex() {
				return componentList.indexOf(l);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.getContentPane().removeAll();
				LinePropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}
		});
		dialog.pack();
		if (LinePropertiesPanel.getOffset() == null)
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
		else dialog.setLocation(LinePropertiesPanel.getOffset());
		return dialog;
	}

	private JDialog createDialog(final AbstractRectangle r) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this), "Rectangle Properties", true);
		final RectanglePropertiesPanel p = new RectanglePropertiesPanel(r) {
			public int getIndex() {
				return componentList.indexOf(r);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.getContentPane().removeAll();
				RectanglePropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}
		});
		dialog.pack();
		if (RectanglePropertiesPanel.getOffset() == null)
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
		else dialog.setLocation(RectanglePropertiesPanel.getOffset());
		return dialog;
	}

	private JDialog createDialog(final AbstractEllipse r) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this), "Ellipse Properties", true);
		final EllipsePropertiesPanel p = new EllipsePropertiesPanel(r) {
			public int getIndex() {
				return componentList.indexOf(r);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.getContentPane().removeAll();
				EllipsePropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}
		});
		dialog.pack();
		if (EllipsePropertiesPanel.getOffset() == null)
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
		else dialog.setLocation(EllipsePropertiesPanel.getOffset());
		return dialog;
	}

	private JDialog createDialog(final AbstractTriangle r) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this), "Triangle Properties", true);
		final TrianglePropertiesPanel p = new TrianglePropertiesPanel(r) {
			public int getIndex() {
				return componentList.indexOf(r);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.getContentPane().removeAll();
				TrianglePropertiesPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}
		});
		dialog.pack();
		if (TrianglePropertiesPanel.getOffset() == null)
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
		else dialog.setLocation(TrianglePropertiesPanel.getOffset());
		return dialog;
	}

	private JDialog createDialog(final TextContainer c) {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this), "Text Box Properties", true);
		final TextBoxPanel p = new TextBoxPanel(c) {
			public int getIndex() {
				return componentList.indexOf(c);
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.getContentPane().removeAll();
				TextBoxPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		if (TextBoxPanel.getOffset() == null)
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
		else dialog.setLocation(TextBoxPanel.getOffset());
		return dialog;
	}

	public void inputTextBox() {
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(this), "Text Box Properties", true);
		TextContainer c = new DefaultTextContainer();
		c.setFillMode(new FillMode.ColorFill(Color.green));
		c.setBorderType((byte) 2);
		c.setShadowType((byte) 2);
		c.setCallOut(true);
		addElement(c);
		final TextBoxPanel p = new TextBoxPanel(c) {
			public int getIndex() {
				return getElementCount() - 1;
			}
		};
		dialog.setContentPane(p);
		p.setDialog(dialog);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.getContentPane().removeAll();
				TextBoxPanel.setOffset(dialog.getLocationOnScreen());
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		if (TextBoxPanel.getOffset() == null)
			dialog.setLocationRelativeTo(this);
		else dialog.setLocation(TextBoxPanel.getOffset());
		dialog.setVisible(true);
		if (p.isCancelled()) {
			removeElement(c);
		}
		else {
			if (getSelectedElement() != null)
				getSelectedElement().setSelected(false);
			setSelectedElement(c);
		}
	}

	public JPopupMenu getPopupMenu() {
		if (popupMenu == null)
			createPopupMenu();
		return popupMenu;
	}

	private void createPopupMenu() {

		popupMenu = new JPopupMenu("Default Draw");
		popupMenu.setInvoker(this);

		String s = getInternationalText("BringForward");
		final JMenuItem bringForwardMI = new JMenuItem(s != null ? s : "Bring Forward");
		s = getInternationalText("SendBackward");
		final JMenuItem sendBackwardMI = new JMenuItem(s != null ? s : "Send Backward");
		s = getInternationalText("BringToFront");
		final JMenuItem bringToFrontMI = new JMenuItem(s != null ? s : "Bring to Front");
		s = getInternationalText("SendToBack");
		final JMenuItem sendToBackMI = new JMenuItem(s != null ? s : "Send to Back");
		s = getInternationalText("Properties");
		final JMenuItem propertiesMI = new JMenuItem(s != null ? s : "Properties");
		final JMenuItem cutMI = new JMenuItem();
		final JMenuItem copyMI = new JMenuItem();
		final JMenuItem pasteMI = new JMenuItem();
		final JMenuItem printMI = new JMenuItem();
		s = getInternationalText("Order");
		final JMenu orderMenu = new JMenu(s != null ? s : "Order");

		popupMenu.addPopupMenuListener(new PopupMenuListener() {

			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				popupMenu.removeAll();
				if (selectedElement != null) {
					popupMenu.setLabel("element");
					if (componentList.size() <= 1) {
						bringForwardMI.setEnabled(false);
						sendBackwardMI.setEnabled(false);
						bringToFrontMI.setEnabled(false);
						sendToBackMI.setEnabled(false);
					}
					else {
						int order = componentList.indexOf(selectedElement);
						if (order == 0) {
							bringForwardMI.setEnabled(true);
							bringToFrontMI.setEnabled(true);
							sendBackwardMI.setEnabled(false);
							sendToBackMI.setEnabled(false);
						}
						else if (order == componentList.size() - 1) {
							bringForwardMI.setEnabled(false);
							bringToFrontMI.setEnabled(false);
							sendBackwardMI.setEnabled(true);
							sendToBackMI.setEnabled(true);
						}
						else {
							bringForwardMI.setEnabled(true);
							sendBackwardMI.setEnabled(true);
							bringToFrontMI.setEnabled(true);
							sendToBackMI.setEnabled(true);
						}
					}
					popupMenu.add(cutMI);
					popupMenu.add(copyMI);
					popupMenu.addSeparator();
					popupMenu.add(orderMenu);
					popupMenu.addSeparator();
					popupMenu.add(propertiesMI);
				}
				else {
					popupMenu.setLabel("default");
					pasteMI.setEnabled(bufferedElement != null);
					popupMenu.add(pasteMI);
					popupMenu.addSeparator();
					popupMenu.add(printMI);
				}
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				Draw.this.requestFocusInWindow();
			}

		});

		cutMI.setAction(getActionMap().get("Cut"));
		s = getInternationalText("Cut");
		if (s != null)
			cutMI.setText(s);
		popupMenu.add(cutMI);
		copyMI.setAction(getActionMap().get("Copy"));
		s = getInternationalText("Copy");
		if (s != null)
			copyMI.setText(s);
		popupMenu.add(copyMI);
		popupMenu.addSeparator();

		pasteMI.setAction(getActionMap().get("Paste"));
		s = getInternationalText("Paste");
		if (s != null)
			pasteMI.setText(s);

		printMI.setAction(getActionMap().get("Print"));
		s = getInternationalText("Print");
		if (s != null)
			printMI.setText(s);

		bringToFrontMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bringToFront();
			}
		});
		orderMenu.add(bringToFrontMI);

		sendToBackMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendToBack();
			}
		});
		orderMenu.add(sendToBackMI);

		bringForwardMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bringForward();
			}
		});
		orderMenu.add(bringForwardMI);

		sendBackwardMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendBack();
			}
		});
		orderMenu.add(sendBackwardMI);

		popupMenu.add(orderMenu);
		popupMenu.addSeparator();

		propertiesMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showDialog(selectedElement);
			}
		});
		popupMenu.add(propertiesMI);

	}

	static boolean isRightClick(MouseEvent e) {
		if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
			return true;
		if (System.getProperty("os.name").startsWith("Mac") && e.isControlDown())
			return true;
		return false;
	}

	static Cursor createCustomCursor(String url, Point hotSpot, String name) {
		if (url == null)
			return null;
		ImageIcon cursorIcon = new ImageIcon(Draw.class.getResource(url));
		if (hotSpot == null)
			hotSpot = new Point();
		Dimension prefDimension = Toolkit.getDefaultToolkit().getBestCursorSize(hotSpot.x, hotSpot.y);
		if (hotSpot.x > prefDimension.width - 1)
			hotSpot.x = prefDimension.width - 1;
		else if (hotSpot.x < 0)
			hotSpot.x = 0;
		if (hotSpot.y > prefDimension.height - 1)
			hotSpot.y = prefDimension.height - 1;
		else if (hotSpot.y < 0)
			hotSpot.y = 0;
		return Toolkit.getDefaultToolkit().createCustomCursor(cursorIcon.getImage(), hotSpot, name);
	}

	public void print() {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		if (printerJob == null) {
			printerJob = PrinterJob.getPrinterJob();
			printerJob.setCopies(1);
		}
		setCurrentPageFormat(printerJob.validatePage(getCurrentPageFormat()));
		printerJob.setPrintable(this, getCurrentPageFormat());
		printerJob.setPageable(this);
		if (printerJob.printDialog()) {
			try {
				printerJob.print();
			}
			catch (PrinterException pe) {
				pe.printStackTrace();
				printerJob.cancel();
			}
		}
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

}
