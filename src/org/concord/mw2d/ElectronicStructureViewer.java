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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.concord.modeler.ModelerUtilities;
import org.concord.mw2d.event.ParameterChangeEvent;
import org.concord.mw2d.event.ParameterChangeListener;
import org.concord.mw2d.event.UpdateListener;
import org.concord.mw2d.models.Electron;
import org.concord.mw2d.models.ElectronicStructure;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.EnergyLevel;

public abstract class ElectronicStructureViewer extends JPanel implements UpdateListener {

	private final static int V_MARGIN = 30;
	final static int H_MARGIN = 20;
	private final static Font NUMBER_FONT = new Font("MS Sans Serif", Font.PLAIN, 10);
	private static Color scaleColor = new Color(0, 104, 139, 50);
	private static int[] storedNTick = new int[4];
	private static float[] storedLowerBound = new float[4];
	private static float[] storedUpperBound = new float[4];

	private float lowerBound = -5, upperBound = 0;
	private boolean lowerBoundSet, upperBoundSet;
	private boolean drawTicks;
	private Element element;
	private List<EnergyLevelView> levelViewList;
	private EnergyLevelView selectedLevelView, rolloverLevelView;
	private ElectronView selectedElectronView;
	private int ntick = 20;
	private int oldY;
	private float oldValue;
	private float closestTickValue;
	private int mouseY;
	private String title;
	private boolean lockEnergyLevels;
	private boolean showElectronMoving;
	private Map<Float, Integer> scaleMap;
	private List<ParameterChangeListener> paramListenerList;

	public ElectronicStructureViewer() {

		setPreferredSize(new Dimension(200, 300));
		setBackground(Color.white);
		setForeground(Color.black);

		Action a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				removeSelectedEnergyLevel();
			}
		};
		a.putValue(Action.ACCELERATOR_KEY, System.getProperty("os.name").startsWith("Mac") ? KeyStroke.getKeyStroke(
				KeyEvent.VK_X, KeyEvent.META_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Delete");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "Delete");
		getActionMap().put("Delete", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				moveOneStep(true);
			}
		};
		a.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Move up");
		getActionMap().put("Move up", a);

		a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				moveOneStep(false);
			}
		};
		a.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
		getInputMap().put((KeyStroke) a.getValue(Action.ACCELERATOR_KEY), "Move down");
		getActionMap().put("Move down", a);

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				scaleViewer();
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				whenMousePressed(e);
			}

			public void mouseReleased(MouseEvent e) {
				whenMouseReleased(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				whenMouseDragged(e);
			}

			public void mouseMoved(MouseEvent e) {
				whenMouseMoved(e);
			}
		});

	}

	public void setTitle(String s) {
		title = s;
	}

	public String getTitle() {
		return title;
	}

	public void setLockEnergyLevels(boolean b) {
		lockEnergyLevels = b;
	}

	public boolean getLockEnergyLevels() {
		return lockEnergyLevels;
	}

	public void addParameterChangeListener(ParameterChangeListener pcl) {
		if (pcl == null)
			return;
		if (paramListenerList == null)
			paramListenerList = new CopyOnWriteArrayList<ParameterChangeListener>();
		paramListenerList.add(pcl);
	}

	public void removeParameterChangeListener(ParameterChangeListener pcl) {
		if (pcl == null)
			return;
		if (paramListenerList == null)
			return;
		paramListenerList.remove(pcl);
	}

	public void removeAllParameterChangeListeners() {
		if (paramListenerList == null)
			return;
		paramListenerList.clear();
	}

	protected void notifyListeners(ParameterChangeEvent e) {
		if (paramListenerList == null)
			return;
		for (ParameterChangeListener l : paramListenerList)
			l.parameterChanged(e);
	}

	/** Set the lower bound of the energy for this electronic structure viewer. */
	public void setLowerBound(float lb) {
		lowerBound = lb;
		lowerBoundSet = true;
	}

	/** Get the lower bound of the energy for this electronic structure viewer. */
	public float getLowerBound() {
		return lowerBound;
	}

	public boolean isLowerBoundSet() {
		return lowerBoundSet;
	}

	/** Set the upper bound of the energy for this electronic structure viewer. */
	public void setUpperBound(float ub) {
		upperBound = ub;
		upperBoundSet = true;
	}

	/** Get the upper bound of the energy for this electronic structure viewer. */
	public float getUpperBound() {
		return upperBound;
	}

	public boolean isUpperBoundSet() {
		return upperBoundSet;
	}

	/** Set the number of ticks to which the energy levels will snap. */
	public void setNumberOfTicks(int i) {
		ntick = i;
	}

	/** Get the number of ticks to which the energy levels will snap. */
	public int getNumberOfTicks() {
		return ntick;
	}

	/** If true, draw the ticks which the energy levels will snap. */
	public void setDrawTicks(boolean b) {
		drawTicks = b;
	}

	public boolean getDrawTicks() {
		return drawTicks;
	}

	public int getVerticalMargin() {
		return V_MARGIN;
	}

	public int getMouseY() {
		return mouseY;
	}

	/** Scale the energy levels when the viewer is resized. */
	public void scaleViewer() {

		if (element == null)
			return;

		Dimension size = getSize();

		if (scaleMap == null)
			scaleMap = Collections.synchronizedMap(new LinkedHashMap<Float, Integer>());
		else scaleMap.clear();

		int x0 = H_MARGIN * 3;
		int x1 = size.width - H_MARGIN * 4;
		int y0 = V_MARGIN;
		int y1 = size.height - V_MARGIN;
		float incr = (upperBound - lowerBound) / ntick;
		float dy = (float) (y1 - y0) / (float) ntick;

		for (int i = 0; i <= ntick; i++) {
			scaleMap.put(lowerBound + incr * i, (int) (y1 - dy * i));
		}

		int h = 0;
		for (EnergyLevelView x : levelViewList) {
			for (Float o : scaleMap.keySet()) {
				if (Math.abs(o - x.getModel().getEnergy()) < incr * 0.5f) {
					h = scaleMap.get(o);
					break;
				}
			}
			x.setRect(x0, h, x1, EnergyLevelView.THICKNESS);
		}

		repaint();

	}

	protected void createContinuum() {
		if (element == null)
			return;
		EnergyLevel level = null;
		for (Float x : scaleMap.keySet()) {
			level = new EnergyLevel(x);
			ElectronicStructure es = element.getElectronicStructure();
			if (!es.containsEnergyLevel(level)) {
				es.addEnergyLevel(level);
				levelViewList.add(new EnergyLevelView(level));
			}
		}
		scaleViewer();
		element.getElectronicStructure().sort();
		notifyListeners(new ParameterChangeEvent(this, "Excited state inserted", level, element));
	}

	/** Insert an energy level at the specified position. */
	public void insertEnergyLevel(int y) {
		if (element == null)
			return;
		float v = lowerBound + (float) (getHeight() - y - V_MARGIN) / (float) (getHeight() - V_MARGIN * 2)
				* (upperBound - lowerBound);
		if (snapValue(v)) {
			ElectronicStructure es = element.getElectronicStructure();
			EnergyLevel level = new EnergyLevel(closestTickValue);
			es.addEnergyLevel(level);
			levelViewList.add(new EnergyLevelView(level));
			Collections.sort(levelViewList);
			scaleViewer();
			element.getElectronicStructure().sort();
			notifyListeners(new ParameterChangeEvent(this, "Excited state inserted", level, element));
		}
	}

	private void moveOneStep(boolean up) {
		if (selectedLevelView == null)
			return;
		if (scaleMap == null)
			return;
		int y0 = V_MARGIN;
		int y1 = getHeight() - V_MARGIN;
		float dy = (float) (y1 - y0) / (float) ntick;
		if (up)
			dy = -dy;
		moveEnergyLevel((int) (selectedLevelView.getY() + dy));
	}

	/** Move the selected energy level to the specified position. */
	public void moveEnergyLevel(int y) {
		if (selectedLevelView == null)
			return;
		if (scaleMap == null)
			return;
		float dy = (float) (getHeight() - 2 * V_MARGIN) / (float) (2 * ntick);
		int h = 0;
		float v = 0.0f;
		boolean b = false;
		if (y > V_MARGIN && y < getHeight() - V_MARGIN) {
			synchronized (scaleMap) {
				for (Float o : scaleMap.keySet()) {
					h = scaleMap.get(o);
					if (Math.abs(y - h) <= dy) {
						v = o;
						b = true;
						break;
					}
				}
			}
		}
		else if (y <= V_MARGIN) {
			Set key = scaleMap.keySet();
			v = (Float) key.toArray()[key.size() - 1];
			h = scaleMap.get(v);
			b = true;
		}
		else if (y >= getHeight() - V_MARGIN) {
			Set key = scaleMap.keySet();
			v = (Float) key.toArray()[0];
			h = scaleMap.get(v);
			b = true;
		}

		if (b) {
			// return if the tick level has been used
			synchronized (levelViewList) {
				for (EnergyLevelView x : levelViewList) {
					if (x != selectedLevelView && x.getY() == h) {
						selectedLevelView.setY(oldY);
						getSelectedEnergyLevel().setEnergy(oldValue);
						repaint();
						return;
					}
					if (selectedLevelView == x) {
						selectedLevelView.setY(h);
						x.getModel().setEnergy(v);
						element.getElectronicStructure().sort();
						oldY = h;
						oldValue = v;
						repaint();
						notifyListeners(new ParameterChangeEvent(this, "Excited state moved", null, element));
						break;
					}
				}
			}
		}
	}

	protected void clearElectronView() {
		if (levelViewList != null) {
			for (EnergyLevelView v : levelViewList) {
				v.removeAllElectrons();
			}
		}
		repaint();
	}

	protected void addElectron(Electron e) {
		int i = element.getElectronicStructure().indexOf(e.getEnergyLevel());
		if (i < 0)
			return;
		EnergyLevelView v = levelViewList.get(i);
		if (v != null)
			v.addElectron(new ElectronView(e));
	}

	public int getPreferredNTick() {
		if (element == null)
			return ntick;
		return storedNTick[element.getID()];
	}

	public float getPreferredLowerBound() {
		if (element == null)
			return lowerBound;
		return storedLowerBound[element.getID()];
	}

	public float getPreferredUpperBound() {
		if (element == null)
			return upperBound;
		return storedUpperBound[element.getID()];
	}

	/** Set the element whose electronic structure will be rendered by this component. */
	public void setElement(Element element) {
		if (element == null) {
			this.element = null;
			return;
		}
		this.element = element;
		if (ntick != 100)
			storedNTick[element.getID()] = ntick;
		if (lowerBoundSet)
			storedLowerBound[element.getID()] = lowerBound;
		if (upperBoundSet)
			storedUpperBound[element.getID()] = upperBound;
		if (levelViewList == null)
			levelViewList = Collections.synchronizedList(new ArrayList<EnergyLevelView>());
		else levelViewList.clear();
		ElectronicStructure es = element.getElectronicStructure();
		int n = es.getNumberOfEnergyLevels();
		for (int i = 0; i < n; i++) {
			levelViewList.add(new EnergyLevelView(es.getEnergyLevel(i)));
		}
		scaleViewer();
	}

	public Element getElement() {
		return element;
	}

	public void removeAllExcitedEnergyLevels() {
		if (element == null)
			return;
		ElectronicStructure es = element.getElectronicStructure();
		int n = es.getNumberOfEnergyLevels();
		if (n <= 1)
			return;
		EnergyLevelView levelView;
		synchronized (levelViewList) {
			for (Iterator it = levelViewList.iterator(); it.hasNext();) {
				levelView = (EnergyLevelView) it.next();
				if (levelViewList.indexOf(levelView) != 0) {
					it.remove();
					es.removeEnergyLevel(levelView.getModel());
				}
			}
		}
		repaint();
		notifyListeners(new ParameterChangeEvent(this, "All excited states removed", null, element));
	}

	/** Remove the selected energy level. */
	public void removeSelectedEnergyLevel() {
		if (element == null)
			return;
		if (selectedLevelView == null)
			return;
		element.getElectronicStructure().removeEnergyLevel(selectedLevelView.getModel());
		levelViewList.remove(selectedLevelView);
		Collections.sort(levelViewList);
		repaint();
		notifyListeners(new ParameterChangeEvent(this, "Excited state removed", selectedLevelView.getModel(), element));
	}

	/** Return the selected energy level. */
	public EnergyLevel getSelectedEnergyLevel() {
		if (levelViewList == null || levelViewList.isEmpty() || selectedLevelView == null)
			return null;
		return selectedLevelView.getModel();
	}

	private boolean snapValue(float v) {
		if (element == null)
			return false;
		float d = (upperBound - lowerBound) / (2.0f * ntick);
		float x;
		ElectronicStructure es = element.getElectronicStructure();
		if (es.contains(v, d))
			return false;
		synchronized (scaleMap) {
			for (Float o : scaleMap.keySet()) {
				x = o.floatValue();
				if (Math.abs(v - x) < d) {
					closestTickValue = x;
					return true;
				}
			}
		}
		return false;
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g0) {

		Graphics2D g = (Graphics2D) g0;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Dimension size = getSize();

		g.setColor(getBackground());
		g.fillRect(0, 0, size.width, size.height);

		if (element == null)
			return;

		g.setFont(NUMBER_FONT);
		g.setColor(Color.black);
		int fontHeight = g.getFontMetrics().getHeight();
		String unitString = title != null ? title : "Unit: eV - Type: " + element.getName();
		g.drawString(unitString, (size.width - g.getFontMetrics().stringWidth(unitString)) / 2, fontHeight);

		synchronized (levelViewList) {
			for (EnergyLevelView levelView : levelViewList) {
				levelView.paint(g, getForeground(), levelView == selectedLevelView);
			}
		}

		if (drawTicks) {
			if (scaleMap != null && !scaleMap.isEmpty()) {
				g.setColor(scaleColor);
				int x0 = H_MARGIN + (size.width >> 1);
				int dx = (size.width >> 1) - (H_MARGIN << 1);
				int y;
				synchronized (scaleMap) {
					for (Float o : scaleMap.keySet()) {
						y = scaleMap.get(o) + (EnergyLevelView.THICKNESS >> 1);
						g.drawLine(x0 - dx, y, x0 - dx + 5, y);
					}
				}
			}
		}

		if (showElectronMoving && selectedElectronView != null) {
			g.setColor(Color.black);
			g.setStroke(ViewAttribute.THIN_DASHED);
			int r = ElectronView.getRadius();
			g.drawOval(selectedElectronView.getX(), mouseY - r, r * 2, r * 2);
			if (rolloverLevelView != null) {
				int d = (int) ((size.height - V_MARGIN * 2) / (2.0f * ntick));
				g.drawRect(rolloverLevelView.getX(), rolloverLevelView.getY() + rolloverLevelView.getHeight() / 2 - d,
						rolloverLevelView.getWidth(), d * 2);
			}
		}

	}

	private void whenMousePressed(MouseEvent e) {
		if (!isEnabled())
			return;
		if (levelViewList == null)
			return;
		requestFocusInWindow();
		int x = e.getX();
		int y = e.getY();
		selectedElectronView = null;
		synchronized (levelViewList) {
			for (EnergyLevelView levelView : levelViewList) {
				if ((selectedElectronView = levelView.whichElectron(x, y)) != null) {
					break;
				}
			}
		}
		selectedLevelView = null;
		if (selectedElectronView == null && !lockEnergyLevels) {
			synchronized (levelViewList) {
				for (EnergyLevelView levelView : levelViewList) {
					if (levelView.contains(x, y)) {
						selectedLevelView = levelView;
						oldY = levelView.getY();
						oldValue = levelView.getModel().getEnergy();
						break;
					}
				}
			}
		}
		repaint();
	}

	private void whenMouseReleased(MouseEvent e) {
		mouseY = e.getY();
		if (!isEnabled())
			return;
		if (ModelerUtilities.isRightClick(e))
			return;
		if (!lockEnergyLevels) {
			if (e.isShiftDown()) {
				insertEnergyLevel(mouseY);
			}
			else {
				moveEnergyLevel(mouseY);
			}
		}
		if (showElectronMoving && selectedElectronView != null) {
			EnergyLevelView lv = null;
			int d = (int) ((getHeight() - V_MARGIN * 2) / (2.0f * ntick));
			synchronized (levelViewList) {
				for (EnergyLevelView levelView : levelViewList) {
					if (levelView.contains(e.getX(), e.getY(), 0, d)) {
						lv = levelView;
						break;
					}
				}
			}
			if (lv != null) {
				synchronized (levelViewList) {
					for (EnergyLevelView levelView : levelViewList) {
						if (levelView.hasElectron(selectedElectronView)) {
							levelView.removeElectron(selectedElectronView);
							break;
						}
					}
				}
				selectedElectronView.getModel().setEnergyLevel(lv.getModel());
				lv.addElectron(selectedElectronView);
				selectedElectronView.getModel().getHostModel().getView().repaint();
			}
			showElectronMoving = false;
			rolloverLevelView = null;
		}
		repaint();
	}

	private void whenMouseDragged(MouseEvent e) {
		if (!isEnabled())
			return;
		if (ModelerUtilities.isRightClick(e))
			return;
		mouseY = e.getY();
		if (selectedElectronView != null) {
			showElectronMoving = true;
			rolloverLevelView = null;
			int d = (int) ((getHeight() - V_MARGIN * 2) / (2.0f * ntick));
			synchronized (levelViewList) {
				for (EnergyLevelView levelView : levelViewList) {
					if (levelView.contains(e.getX(), mouseY, 0, d)) {
						rolloverLevelView = levelView;
						break;
					}
				}
			}
		}
		else if (!lockEnergyLevels && selectedLevelView != null) {
			if (mouseY < V_MARGIN)
				mouseY = V_MARGIN;
			else if (mouseY > getHeight() - V_MARGIN)
				mouseY = getHeight() - V_MARGIN;
			selectedLevelView.setY(mouseY);
			float v = lowerBound + (float) (getHeight() - mouseY - V_MARGIN) / (float) (getHeight() - V_MARGIN * 2)
					* (upperBound - lowerBound);
			if (snapValue(v)) {
				getSelectedEnergyLevel().setEnergy(closestTickValue);
			}
		}
		repaint();
	}

	private void whenMouseMoved(MouseEvent e) {
		if (!isEnabled())
			return;
		if (levelViewList == null || levelViewList.isEmpty())
			return;
		int x = e.getX();
		int y = e.getY();
		boolean onElectron = false;
		synchronized (levelViewList) {
			for (EnergyLevelView levelView : levelViewList) {
				if (levelView.whichElectron(x, y) != null) {
					onElectron = true;
					break;
				}
			}
		}
		boolean onEnergyLevel = false;
		if (!onElectron && !lockEnergyLevels) {
			synchronized (levelViewList) {
				for (EnergyLevelView levelView : levelViewList) {
					if (levelView.contains(x, y)) {
						onEnergyLevel = true;
						break;
					}
				}
			}
		}
		if (onElectron) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else if (onEnergyLevel) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}
		else {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

}