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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.LineSymbols;
import org.concord.modeler.draw.LineWidth;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.event.MovieEvent;
import org.concord.modeler.event.MovieListener;
import org.concord.modeler.g2d.Curve;
import org.concord.modeler.g2d.CurveGroup;
import org.concord.modeler.g2d.DataSet;
import org.concord.modeler.g2d.Tracer;
import org.concord.modeler.g2d.XYGrapher;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FloatQueue;

public class PageXYGraph extends XYGrapher implements Embeddable, Scriptable, ModelCommunicator, MovieListener {

	/** maximum number of data sets allowed in this graph */
	public final static byte MAX = 5;

	Page page;
	String modelClass;
	int modelID = -1;
	byte[] smoothers;
	String[] descriptions;
	float xMultiplier = 1;
	float[] yMultiplier;
	float xAddend;
	float[] yAddend;
	boolean autoScaleX = true, autoScaleY = true;
	double dataWindow_xmin, dataWindow_xmax = 1;
	double dataWindow_ymin, dataWindow_ymax = 1;
	Color[] lineColors;
	int[] lineStyles;
	float[] lineWidths;
	int[] lineSymbols;
	int[] symbolSizes;
	int[] symbolSpacings;
	boolean autoUpdate = true;
	String labelForXAxis, labelForYAxis;
	private int index;
	private String uid;
	private boolean marked;
	private boolean changable;
	private Action syncAction, refreshAction;
	private JToggleButton toggle;
	private JPopupMenu popupMenu;

	private static ImageIcon desyncIcon, syncIcon;
	private static Border defaultBorder = BorderFactory.createRaisedBevelBorder();
	private static Border markedBorder = BorderFactory.createLineBorder(SystemColor.textHighlight, 2);
	private static PageXYGraphMaker maker;
	private MouseListener popupMouseListener;
	private XYGraphScripter scripter;

	public PageXYGraph() {

		super();

		setBorder(BorderFactory.createEmptyBorder());
		Font font = new Font(null, Font.PLAIN, Page.getDefaultFontSize() - 1);
		graph.setAxisFont(font);
		graph.setLegendFont(font);

		descriptions = new String[MAX + 1];
		lineColors = new Color[MAX];
		Arrays.fill(lineColors, Color.black);
		lineStyles = new int[MAX];
		Arrays.fill(lineStyles, LineStyle.STROKE_NUMBER_1);
		lineWidths = new float[MAX];
		Arrays.fill(lineWidths, LineWidth.STROKE_WIDTH_1);
		lineSymbols = new int[MAX];
		Arrays.fill(lineSymbols, LineSymbols.SYMBOL_NUMBER_1);
		symbolSizes = new int[MAX];
		Arrays.fill(symbolSizes, 4);
		symbolSpacings = new int[MAX];
		Arrays.fill(symbolSpacings, 5);
		yMultiplier = new float[MAX];
		Arrays.fill(yMultiplier, 1);
		yAddend = new float[MAX];
		Arrays.fill(yAddend, 0);
		smoothers = new byte[MAX];
		Arrays.fill(smoothers, Curve.INSTANTANEOUS_VALUE);

		if (syncIcon == null)
			syncIcon = new ImageIcon(XYGrapher.class.getResource("images/Sync.gif"));
		if (desyncIcon == null)
			desyncIcon = new ImageIcon(XYGrapher.class.getResource("images/Desync.gif"));
		syncAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Object src = e.getSource();
				if (src instanceof JToggleButton) {
					JToggleButton button = (JToggleButton) src;
					button.setIcon(button.isSelected() ? syncIcon : desyncIcon);
					autoUpdate = button.isSelected();
					refreshAction.setEnabled(!autoUpdate);
				}
			}
		};
		syncAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_G);
		syncAction.putValue(Action.SMALL_ICON, syncIcon);
		syncAction.putValue(Action.SHORT_DESCRIPTION, "Auto update");
		syncAction.putValue(Action.NAME, "Auto Update");
		toggle = new JToggleButton(syncAction);
		toggle.setSelected(true);
		toggle.setText(null);
		toggle.setPreferredSize(ModelerUtilities.getSystemToolBarButtonSize());
		toolBar.add(toggle);

		refreshAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				refresh(false);
			}
		};
		refreshAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
		refreshAction.putValue(Action.SHORT_DESCRIPTION, "Refresh graph");
		refreshAction.putValue(Action.NAME, "Refresh Graph");
		refreshAction.putValue(Action.SMALL_ICON, IconPool.getIcon("update"));
		refreshAction.setEnabled(false);
		toolBar.add(createTextlessButton(refreshAction));

		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		graph.addMouseListener(popupMouseListener);

	}

	public PageXYGraph(PageXYGraph g, Page parent) {
		this();
		setUid(g.uid);
		setPage(parent);
		setModelID(g.modelID);
		xMultiplier = g.xMultiplier;
		System.arraycopy(g.yMultiplier, 0, yMultiplier, 0, MAX);
		xAddend = g.xAddend;
		System.arraycopy(g.yAddend, 0, yAddend, 0, MAX);
		System.arraycopy(g.smoothers, 0, smoothers, 0, MAX);
		getGraph().setGraphBackground(g.getGraph().getGraphBackground());
		getGraph().setDataBackground(g.getGraph().getDataBackground());
		setLegendLocation(g.getLegendLocation());
		setDrawGrid(g.gridIsDrawn());
		setLabelForXAxis(g.getLabelForXAxis());
		setLabelForYAxis(g.getLabelForYAxis());
		int n = g.getCurveGroup().curveCount();
		setDescription(0, g.getDescription(0));
		for (int k = 0; k < n; k++) {
			setDescription(k + 1, g.getDescription(k + 1));
			append(g.getCurveGroup().getCurve(k));
		}
		setAutoScaleX(g.getAutoScaleX());
		setAutoScaleY(g.getAutoScaleY());
		setAutoUpdate(g.getAutoUpdate());
		if (!getAutoScaleX()) {
			setDataWindowXmin(g.getDataWindowXmin());
			setDataWindowXmax(g.getDataWindowXmax());
		}
		if (!getAutoScaleY()) {
			setDataWindowYmin(g.getDataWindowYmin());
			setDataWindowYmax(g.getDataWindowYmax());
		}
		if (g.hasToolBar()) {
			addToolBar();
		}
		else {
			removeToolBar();
		}
		if (g.hasMenuBar()) {
			addMenuBar();
		}
		else {
			removeMenuBar();
		}
		setPreferredSize(g.getPreferredSize());
		setBorderType(g.getBorderType());
		setChangable(page.isEditable());
		Model m = getModel();
		if (m != null) {
			m.addModelListener(this);
			if (!m.getRecorderDisabled())
				m.getMovie().addMovieListener(this);
		}
	}

	boolean isTargetClass() {
		return ComponentMaker.isTargetClass(modelClass);
	}

	private Model getModel() {
		return ComponentMaker.getModel(page, modelClass, modelID);
	}

	public String runScript(String script) {
		if (scripter == null)
			scripter = new XYGraphScripter(this);
		return scripter.runScript(script);
	}

	public String runScriptImmediately(String script) {
		return runScript(script);
	}

	public Object get(String variable) {
		return null;
	}

	public void destroy() {
		removeSnapshotListeners();
		Model m = getModel();
		if (m != null) {
			m.removeModelListener(this);
			if (m.getMovie() != null)
				m.getMovie().removeMovieListener(this);
		}
		page = null;
		if (maker != null)
			maker.setObject(null);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		if (popupMenu != null)
			return;

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("CustomizeXYGraph");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This X-Y Graph") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageXYGraphMaker(PageXYGraph.this);
				}
				else {
					maker.setObject(PageXYGraph.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveXYGraph");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This X-Y Graph");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageXYGraph.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyXYGraph");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This X-Y Graph");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageXYGraph.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("TakeSnapshot");
		mi = new JMenuItem((s != null ? s : "Take a Snapshot") + "...");
		mi.putClientProperty("graph", this);
		mi.addActionListener(getSnapshotListener());
		popupMenu.add(mi);
		popupMenu.addSeparator();

		mi = new JMenuItem(getActionMap().get("Show Growing-Point Running Averages"));
		mi.setIcon(null);
		Object o = mi.getAction().getValue("i18n");
		if (o instanceof String)
			mi.setText((String) o);
		popupMenu.add(mi);

		mi = new JMenuItem(getActionMap().get("Show Exponential Running Averages"));
		mi.setIcon(null);
		o = mi.getAction().getValue("i18n");
		if (o instanceof String)
			mi.setText((String) o);
		popupMenu.add(mi);

		mi = new JMenuItem(getActionMap().get("Show First-Order Derivative"));
		mi.setIcon(null);
		o = mi.getAction().getValue("i18n");
		if (o instanceof String)
			mi.setText((String) o);
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("ShowMenuBar");
		final JMenuItem miMenuBar = new JCheckBoxMenuItem(s != null ? s : "Show Menu Bar");
		miMenuBar.setSelected(true);
		miMenuBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					addMenuBar();
				}
				else {
					removeMenuBar();
				}
				PageXYGraph.this.validate();
			}
		});
		popupMenu.add(miMenuBar);

		s = Modeler.getInternationalText("ShowToolBar");
		final JMenuItem miToolBar = new JCheckBoxMenuItem(s != null ? s : "Show Tool Bar");
		miToolBar.setSelected(true);
		miToolBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					addToolBar();
				}
				else {
					removeToolBar();
				}
				PageXYGraph.this.validate();
			}
		});
		popupMenu.add(miToolBar);

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miMenuBar.setSelected(hasMenuBar());
				miToolBar.setSelected(hasToolBar());
				miCustom.setEnabled(changable);
				miRemove.setEnabled(changable);
				miMenuBar.setEnabled(changable);
				miToolBar.setEnabled(changable);
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		popupMenu.pack();

	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setAutoScale(boolean b) {
		autoScaleX = autoScaleY = b;
	}

	/** return true when and only when both x and y axis are autoscaled */
	public boolean getAutoScale() {
		return autoScaleX && autoScaleY;
	}

	public void setAutoScaleX(boolean b) {
		autoScaleX = b;
	}

	public boolean getAutoScaleX() {
		return autoScaleX;
	}

	public void setAutoScaleY(boolean b) {
		autoScaleY = b;
	}

	public boolean getAutoScaleY() {
		return autoScaleY;
	}

	public void setAutoUpdate(boolean b) {
		autoUpdate = b;
		toggle.setSelected(b);
		toggle.setIcon(b ? syncIcon : desyncIcon);
		refreshAction.setEnabled(!autoUpdate);
	}

	public boolean getAutoUpdate() {
		return autoUpdate;
	}

	public void setDescription(int slot, String s) {
		if (slot < 0 || slot > MAX)
			throw new IllegalArgumentException("wrong slot");
		descriptions[slot] = s;
		if (slot == 0) {
			enableRunningAveragesComputing(descriptions[0].toLowerCase().startsWith("time"));
		}
	}

	public String getDescription(int slot) {
		if (slot < 0 || slot > MAX)
			throw new IllegalArgumentException("wrong slot");
		return descriptions[slot];
	}

	/** this method is used by the parser */
	public void setXMultiplier(float m) {
		xMultiplier = m;
	}

	/** this method is used by the parser */
	public void setXAddend(float a) {
		xAddend = a;
	}

	/** this method is used by the parser */
	public void setMultiplier(int slot, float m) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		yMultiplier[slot] = m;
	}

	/** this method is used by the parser */
	public void setAddend(int slot, float a) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		yAddend[slot] = a;
	}

	/** this method is used by the parser */
	public void setSmoothFilter(int slot, byte i) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		smoothers[slot] = i;
	}

	/** this method is used by the parser */
	public void setLineColor(int slot, Color c) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		lineColors[slot] = c;
	}

	/** this method is used by the parser */
	public Color getLineColor(int slot) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		return lineColors[slot];
	}

	/** this method is used by the parser */
	public void setLineStyle(int slot, int i) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		lineStyles[slot] = i;
	}

	/** this method is used by the parser */
	public int getLineStyle(int slot) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		return lineStyles[slot];
	}

	/** this method is used by the parser */
	public void setLineWidth(int slot, float f) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		lineWidths[slot] = f;
	}

	/** this method is used by the parser */
	public float getLineWidth(int slot) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		return lineWidths[slot];
	}

	/** this method is used by the parser */
	public void setLineSymbol(int slot, int i) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		lineSymbols[slot] = i;
	}

	/** this method is used by the parser */
	public int getLineSymbol(int slot) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		return lineSymbols[slot];
	}

	/** this method is used by the parser */
	public void setSymbolSize(int slot, int i) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		symbolSizes[slot] = i;
	}

	/** this method is used by the parser */
	public int getSymbolSize(int slot) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		return symbolSizes[slot];
	}

	/** this method is used by the parser */
	public void setSymbolSpacing(int slot, int i) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		symbolSpacings[slot] = i;
	}

	/** this method is used by the parser */
	public int getSymbolSpacing(int slot) {
		if (slot < 0 || slot >= MAX)
			throw new IllegalArgumentException("wrong slot");
		return symbolSpacings[slot];
	}

	/** this method is used by the parser */
	public void setLabelForXAxis(String s) {
		labelForXAxis = s;
	}

	/** this method is used by the parser */
	public String getLabelForXAxis() {
		return labelForXAxis;
	}

	/** this method is used by the parser */
	public void setLabelForYAxis(String s) {
		labelForYAxis = s;
	}

	/** this method is used by the parser */
	public String getLabelForYAxis() {
		return labelForYAxis;
	}

	/** this method is used by the parser */
	public void setDataWindowXmin(double d) {
		dataWindow_xmin = d;
	}

	/** this method is used by the parser */
	public double getDataWindowXmin() {
		return dataWindow_xmin;
	}

	/** this method is used by the parser */
	public void setDataWindowXmax(double d) {
		dataWindow_xmax = d;
	}

	/** this method is used by the parser */
	public double getDataWindowXmax() {
		return dataWindow_xmax;
	}

	/** this method is used by the parser */
	public void setDataWindowYmin(double d) {
		dataWindow_ymin = d;
	}

	/** this method is used by the parser */
	public double getDataWindowYmin() {
		return dataWindow_ymin;
	}

	/** this method is used by the parser */
	public void setDataWindowYmax(double d) {
		dataWindow_ymax = d;
	}

	/** this method is used by the parser */
	public double getDataWindowYmax() {
		return dataWindow_ymax;
	}

	public void setPage(Page p) {
		page = p;
		addSnapshotListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComponent c = (JComponent) e.getSource();
				final Object g = c.getClientProperty("graph");
				if (g instanceof XYGrapher) {
					SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), (((XYGrapher) g).getGraph()));
				}
			}
		});
	}

	public Page getPage() {
		return page;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	protected void showRunningAverages(CurveGroup cg) {

		final XYGrapher g = new XYGrapher();
		g.setPreferredSize(new Dimension(300, 300));
		g.input(cg);

		g.addSnapshotListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComponent c = (JComponent) e.getSource();
				final Object o = c.getClientProperty("graph");
				if (o instanceof XYGrapher) {
					SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), (((XYGrapher) o).getGraph()));
				}
			}
		});

		JDialog dialog = ModelerUtilities.getChildDialog(PageXYGraph.this, "Running averages", true);
		g.setDialog(dialog);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(g, BorderLayout.CENTER);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setLocationRelativeTo(dialog.getOwner());
		dialog.setVisible(true);

	}

	public void setModelClass(String s) {
		modelClass = s;
	}

	public String getModelClass() {
		return modelClass;
	}

	public void setModelID(int i) {
		modelID = i;
	}

	public int getModelID() {
		return modelID;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page != null) {
			if (!((LineBorder) markedBorder).getLineColor().equals(page.getSelectionColor()))
				markedBorder = BorderFactory.createLineBorder(page.getSelectionColor(), 2);
		}
		setBorder(b ? markedBorder : defaultBorder);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setChangable(boolean b) {
		changable = b;
	}

	public boolean isChangable() {
		return changable;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setMaximumSize(dim);
		super.setMinimumSize(dim);
		super.setPreferredSize(dim);
	}

	public static PageXYGraph create(Page page) {
		if (page == null)
			return null;
		PageXYGraph gxy = new PageXYGraph();
		if (maker == null) {
			maker = new PageXYGraphMaker(gxy);
		}
		else {
			maker.setObject(gxy);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return gxy;
	}

	private void setDataSetAttributes(DataSet set, int slot) {
		if (set == null)
			throw new IllegalArgumentException("Null data set");
		set.setLineStroke(getLineWidth(slot));
		set.setLineStroke(getLineStyle(slot));
		set.setSymbol(getLineSymbol(slot));
		set.setLineColor(getLineColor(slot));
		set.setSymbolSize(getSymbolSize(slot));
		set.setSymbolSpacing(getSymbolSpacing(slot));
		set.legendColor(getLineColor(slot));
	}

	private void refresh(boolean initialize) {
		Model m = getModel();
		if (m == null) {
			System.err.println("The model this graph is supposed to serve does not exist");
			return;
		}
		if (initialize) {
			m.addModelListener(this);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					PageXYGraph.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
			});
		}
		removeAllCurves();
		String tsName = getDescription(0);
		if (tsName == null)
			throw new RuntimeException("X-Y graph does not have x data!");
		DataQueue qx = null, qy = null;
		boolean xIsTime = tsName.toLowerCase().startsWith("time");
		if (!xIsTime) {
			qx = m.getQueue(tsName);
			if (qx != null) {
				qx.setFunctionalSlot(0);
				qx.setMultiplier(xMultiplier);
				qx.setAddend(xAddend);
				for (int i = 0; i < MAX; i++) {
					tsName = getDescription(i + 1);
					if (tsName != null) {
						qy = m.getQueue(tsName);
						if (qy != null) {
							qy.setFunctionalSlot(i + 1);
							qy.setMultiplier(yMultiplier[i]);
							qy.setAddend(yAddend[i]);
							append(qx.getName() + "-" + qy.getName(), qx, qy, smoothers[i]);
							setDataSetAttributes((DataSet) getDataSets().lastElement(), i);
						}
					}
				}
				appendStoredDataPoints();
			}
		}
		else {
			m.getModelTimeQueue().setMultiplier(xMultiplier);
			m.getModelTimeQueue().setAddend(xAddend);
			for (int i = 0; i < MAX; i++) {
				tsName = getDescription(i + 1);
				if (tsName != null) {
					qy = m.getQueue(tsName);
					if (qy != null) {
						qy.setFunctionalSlot(i + 1);
						qy.setMultiplier(yMultiplier[i]);
						qy.setAddend(yAddend[i]);
						append(qy.getName(), m.getModelTimeQueue(), qy, smoothers[i]);
						setDataSetAttributes((DataSet) getDataSets().lastElement(), i);
					}
				}
			}
		}
		graph.setLegendLocation(getLegendLocation());
		if (initialize) {
			xAxis.setTitleText(getLabelForXAxis());
			yAxis.setTitleText(getLabelForYAxis());
			curveGroup.getLabelOfX().setText(getLabelForXAxis());
			curveGroup.getLabelOfY().setText(getLabelForYAxis());
		}
		if (!autoScaleX) {
			fixScopeOfX(dataWindow_xmin, dataWindow_xmax);
		}
		else {
			if (initialize)
				fixScopeOfX(-0.5, 0.5);
		}
		if (!autoScaleY) {
			fixScopeOfY(dataWindow_ymin, dataWindow_ymax);
		}
		else {
			if (initialize)
				fixScopeOfY(-0.5, 0.5);
		}
		repaint();
		if (initialize) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					PageXYGraph.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			});
		}
	}

	public void modelUpdate(ModelEvent e) {
		graph.setTracer(null);
		switch (e.getID()) {
		case ModelEvent.MODEL_INPUT:
		case ModelEvent.MODEL_RESET:
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					refresh(true);
				}
			});
			break;
		case ModelEvent.MODEL_CHANGED:
			if (autoUpdate)
				refresh(false);
			break;
		case ModelEvent.MODEL_RUN:
			if (getModel() == e.getSource()) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						enableActions(false);
					}
				});
			}
			break;
		case ModelEvent.MODEL_STOP:
			if (getModel() == e.getSource()) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						enableActions(true);
					}
				});
			}
			break;
		}
	}

	public void frameChanged(MovieEvent e) {

		Model m = getModel();
		if (m == null)
			return;

		int frame = e.getFrame();

		String tsName = getDescription(0);
		boolean xIsTime = tsName.toLowerCase().startsWith("time");

		int x = -1;
		int[] y = new int[MAX];
		Arrays.fill(y, -1);
		DataQueue qx = null;
		DataQueue[] qy = new DataQueue[MAX];

		if (!xIsTime) {
			qx = m.getQueue(getDescription(0));
			for (int i = 0; i < MAX; i++) {
				qy[i] = m.getQueue(getDescription(i + 1));
			}
		}
		else {
			qx = m.getModelTimeQueue();
			for (int i = 0; i < MAX; i++) {
				qy[i] = m.getQueue(getDescription(i + 1));
			}
		}
		if (qx instanceof FloatQueue) {
			DataSet ds = (DataSet) graph.getDataSets().get(0);
			if (frame * 2 < ds.getData().length) {
				double a = ds.getData()[frame * 2];
				x = getXAxis().getInteger(a);
				// x = getXAxis().getInteger(((FloatQueue) qx).getData(frame) * xMultiplier + xAddend);
				for (int i = 0; i < MAX; i++) {
					if (qy[i] instanceof FloatQueue) {
						ds = (DataSet) graph.getDataSets().get(i);
						a = ds.getData()[frame * 2 + 1];
						y[i] = getYAxis().getInteger(a);
						// y[i] = getYAxis().getInteger(((FloatQueue) qy[i]).getData(frame) * yMultiplier[i] +
						// yAddend[i]);
					}
				}
			}
		}
		if (x == -1)
			return;
		int n = 0;
		for (int i = 0; i < MAX; i++) {
			if (y[i] != -1)
				n++;
		}
		Point[] p = new Point[n];
		int k = 0;
		for (int i = 0; i < MAX; i++) {
			if (y[i] != -1) {
				p[k++] = new Point(x, y[i]);
			}
		}
		graph.setTracer(new Tracer(p));
		graph.repaint();

	}

	public String toString() {

		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < MAX; i++) {
			if (descriptions[i + 1] != null) {
				buffer.append("<time_series_y" + (i + 1));
				buffer.append(" color=\"" + Integer.toString(lineColors[i].getRGB(), 16) + "\"");
				buffer.append(" style=\"" + lineStyles[i] + "\"");
				buffer.append(" width=\"" + lineWidths[i] + "\"");
				buffer.append(" symbol=\"" + lineSymbols[i] + "\"");
				buffer.append(" size=\"" + symbolSizes[i] + "\"");
				buffer.append(" spacing=\"" + symbolSpacings[i] + "\"");
				if (yMultiplier[i] != 1.0f)
					buffer.append(" multiplier=\"" + yMultiplier[i] + "\"");
				if (yAddend[i] != 0.0f)
					buffer.append(" addend=\"" + yAddend[i] + "\"");
				if (smoothers[i] != Curve.INSTANTANEOUS_VALUE)
					buffer.append(" smoother=\"" + smoothers[i] + "\"");
				buffer.append(">");
				buffer.append(XMLCharacterEncoder.encode(descriptions[i + 1]) + "</time_series_y" + (i + 1) + ">\n");
			}
		}

		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (modelClass != null)
			sb.append("<modelclass>" + modelClass + "</modelclass>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		sb.append("<model>" + getModelID() + "</model>\n");
		sb.append("<time_series_x>" + XMLCharacterEncoder.encode(descriptions[0]) + "</time_series_x>\n");
		if (xMultiplier != 1.0f)
			sb.append("<multiplier>" + xMultiplier + "</multiplier>\n");
		if (xAddend != 0.0f)
			sb.append("<addend>" + xAddend + "</addend>\n");
		if (buffer.length() > 0)
			sb.append(buffer);
		if (!autoScaleX) {
			sb.append("<autofitx>false</autofitx>\n");
			sb.append("<axis_x_min>" + dataWindow_xmin + "</axis_x_min>\n");
			sb.append("<axis_x_max>" + dataWindow_xmax + "</axis_x_max>\n");
		}
		if (!autoScaleY) {
			sb.append("<autofity>false</autofity>\n");
			sb.append("<axis_y_min>" + dataWindow_ymin + "</axis_y_min>\n");
			sb.append("<axis_y_max>" + dataWindow_ymax + "</axis_y_max>\n");
		}
		if (!autoUpdate)
			sb.append("<autoupdate>false</autoupdate>\n");
		sb.append("<width>" + getWidth() + "</width>\n");
		sb.append("<height>" + getHeight() + "</height>\n");
		sb.append("<axis_x_title>" + XMLCharacterEncoder.encode(xAxis.getTitleText()) + "</axis_x_title>\n");
		sb.append("<axis_y_title>" + XMLCharacterEncoder.encode(yAxis.getTitleText()) + "</axis_y_title>\n");
		if (gridIsDrawn())
			sb.append("<hline>true</hline>\n");
		if (getDataSets().size() > 0) {
			sb.append("<legend_x>" + graph.getSet(0).getLegendLocation().x + "</legend_x>\n");
			sb.append("<legend_y>" + graph.getSet(0).getLegendLocation().y + "</legend_y>\n");
		}
		sb.append("<bgcolor>" + Integer.toString(graph.getGraphBackground().getRGB(), 16) + "</bgcolor>\n");
		sb.append("<fgcolor>" + Integer.toString(graph.getDataBackground().getRGB(), 16) + "</fgcolor>\n");
		if (!hasMenuBar())
			sb.append("<menubar>false</menubar>\n");
		if (!hasToolBar())
			sb.append("<toolbar>false</toolbar>\n");
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>\n");

		return sb.toString();

	}

}