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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.MismatchException;

public class XYGrapher extends JPanel {

	private static ResourceBundle bundle;
	private static boolean isUSLocale = Locale.getDefault().equals(Locale.US);

	protected Graph2D graph;
	protected Axis xAxis, yAxis;
	protected CurveGroup curveGroup;
	protected JComponent toolBar;
	protected JMenuBar menuBar;

	private CurvePool curvePool;
	private Point legendLocation;
	private Map<String, List<DataPoint>> storedData;
	private JPanel topPanel;

	private Action openAction, saveAction, newAction, takeAverageAction, printAction, gridAction, derivativeAction,
			propertiesAction, listAction, runningAverageAction, runningAverage2Action, scopingAction;

	private GraphDialog gDialog;
	private JDialog listDialog;
	private JTable table;
	private Vector<String> columnNames;
	private Vector<Vector> rowData;
	private JMenuItem gridMenuItem, snapshotMenuItem;
	private JButton snapshotButton;
	private JDialog host;
	private static JFileChooser fileChooser;
	private RealNumberTextField xminField, xmaxField, yminField, ymaxField;

	private final static DecimalFormat format = new DecimalFormat("0.###E0");
	private static Icon SCOPE_ICON, TAKE_ICON, GRID_ICON, LIST_ICON, AVERAGE_ICON, AVERAGE2_ICON, DERIV_ICON;

	private final static FileFilter yoxFilter = new FileFilter() {

		public boolean accept(File file) {
			if (file.isDirectory())
				return true;
			String filename = file.getName();
			int index = filename.lastIndexOf('.');
			String postfix = filename.substring(index + 1);
			if (postfix != null && postfix.equalsIgnoreCase("yox"))
				return true;
			return false;
		}

		public String getDescription() {
			return "YOX";
		}

	};

	public XYGrapher() {

		super(new BorderLayout());
		setPreferredSize(new Dimension(300, 350));

		if (bundle == null && !isUSLocale) {
			try {
				bundle = ResourceBundle.getBundle("org.concord.modeler.g2d.images.Resource", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		loadIcons();

		curvePool = new CurvePool(5);

		graph = new Graph2D();
		xAxis = graph.createAxis(Axis.BOTTOM);
		yAxis = graph.createAxis(Axis.LEFT);
		init();

		Font font = new Font(null, Font.PLAIN, 11);
		graph.setAxisFont(font);
		graph.setLegendFont(font);
		graph.drawzero = false;
		graph.drawgrid = false;
		graph.gridcolor = Color.lightGray;
		graph.framecolor = Color.black;
		graph.borderTop = 30;
		graph.borderBottom = 10;
		graph.borderLeft = 10;
		graph.borderRight = 30;

		xAxis.axiscolor = yAxis.axiscolor = Color.black;

		graph.addCurveDialog(xAxis, yAxis);
		add(graph, BorderLayout.CENTER);

		topPanel = new JPanel(new BorderLayout());
		add(topPanel, BorderLayout.NORTH);

		menuBar = createMenuBar();
		topPanel.add(menuBar, BorderLayout.NORTH);
		toolBar = createToolBar();
		topPanel.add(toolBar, BorderLayout.CENTER);

	}

	private static void loadIcons() {
		if (SCOPE_ICON != null)
			return;
		SCOPE_ICON = new ImageIcon(XYGrapher.class.getResource("images/Scope.gif"));
		TAKE_ICON = new ImageIcon(XYGrapher.class.getResource("images/Take.gif"));
		GRID_ICON = new ImageIcon(XYGrapher.class.getResource("images/Grid.gif"));
		LIST_ICON = new ImageIcon(XYGrapher.class.getResource("images/List.gif"));
		AVERAGE_ICON = new ImageIcon(XYGrapher.class.getResource("images/Average.gif"));
		AVERAGE2_ICON = new ImageIcon(XYGrapher.class.getResource("images/Average2.gif"));
		DERIV_ICON = new ImageIcon(XYGrapher.class.getResource("images/Derivative.gif"));
	}

	static String getInternationalText(String name) {
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

	/**
	 * compute the current average values of the time series stored in the <code>CurveGroup</code> of this graph, and
	 * store the results. To display the results, call <code>appendStoredDataPoints()</code>.
	 * 
	 * @see org.concord.modeler.g2d.XYGrapher#appendStoredDataPoints
	 */
	protected void computeCurrentAverages() {
		int n = curveGroup.curveCount();
		if (n == 0)
			return;
		Curve c;
		for (int i = 0; i < n; i++) {
			c = curveGroup.getCurve(i);
			if (c.getLegend() == null)
				continue;
			storeDataPoint(c.getLegend().getText(), c.getAveragePoint());
		}
	}

	/*
	 * add a data point <code>p</code> to the stored data list. A stored data list is different from the curve's
	 * normal data array, which will be updated and can be lost. A stored data list won't be lost when the curve's
	 * normal data array is filled up.
	 */
	private void storeDataPoint(String key, DataPoint p) {
		if (key == null || p == null)
			return;
		if (storedData == null)
			storedData = new HashMap<String, List<DataPoint>>();
		List<DataPoint> list = storedData.get(key);
		if (list == null) {
			list = new ArrayList<DataPoint>();
			storedData.put(key, list);
		}
		list.add(p);
	}

	/** append the stored data points as a curve of this graph */
	protected void appendStoredDataPoints() {
		if (storedData == null)
			return;
		if (storedData.isEmpty())
			return;
		int n = curveGroup.curveCount();
		if (n == 0)
			return;
		Curve c;
		CurveFlavor cf;
		Color color;
		List list;
		for (int i = 0; i < n; i++) {
			c = curveGroup.getCurve(i);
			if (c.getLegend() == null)
				continue;
			cf = c.getFlavor();
			color = cf.getColor();
			color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 64);
			list = storedData.get(c.getLegend().getText());
			if (list != null) {
				c = new Curve(list);
				c.setFlavor(new CurveFlavor(color));
				append(c);
			}
		}
	}

	public static void setFileChooser(JFileChooser fc) {
		if (fc == null)
			throw new RuntimeException("file chooser can't be null");
		fileChooser = fc;
	}

	public Insets getGraphInsets() {
		return new Insets(graph.borderTop, graph.borderLeft, graph.borderBottom, graph.borderRight);
	}

	public FontMetrics getFontMetrics() {
		if (graph.getGraphics() != null)
			return graph.getGraphics().getFontMetrics();
		return null;
	}

	/** this method is used by the parser */
	public void setLegendLocation(int x, int y) {
		if (legendLocation == null) {
			legendLocation = new Point(x, y);
		}
		else {
			legendLocation.setLocation(x, y);
		}
		graph.setLegendLocation(x, y);
	}

	public void setLegendLocation(Point p) {
		if (p == null)
			return;
		setLegendLocation(p.x, p.y);
	}

	/** this method is used by the parser */
	public Point getLegendLocation() {
		return legendLocation;
	}

	public void setDialog(JDialog d) {
		host = d;
	}

	public JDialog getDialog() {
		return host;
	}

	protected void enableActions(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				openAction.setEnabled(b);
				saveAction.setEnabled(b);
				newAction.setEnabled(b);
				takeAverageAction.setEnabled(b);
				printAction.setEnabled(b);
				gridAction.setEnabled(b);
				propertiesAction.setEnabled(b);
				listAction.setEnabled(b);
				runningAverageAction.setEnabled(b);
				runningAverage2Action.setEnabled(b);
				scopingAction.setEnabled(b);
				snapshotMenuItem.setEnabled(b);
				snapshotButton.setEnabled(b);
			}
		});
	}

	public void removeAllCurves() {
		curvePool.reset();
		curveGroup.removeAllCurves();
		graph.detachDataSets();
	}

	public void removeCurve(Curve c) {
		if (c == null)
			return;
		curveGroup.removeCurve(c);
		graph.detachDataSet(c.getDataSet());
	}

	public CurveGroup getCurveGroup() {
		return curveGroup;
	}

	public void setDrawGrid(boolean b) {
		graph.drawgrid = b;
		if (gridMenuItem != null)
			gridMenuItem.setSelected(b);
	}

	public boolean gridIsDrawn() {
		return graph.drawgrid;
	}

	public Vector getDataSets() {
		return graph.getDataSets();
	}

	public Graph2D getGraph() {
		return graph;
	}

	public Axis getYAxis() {
		return yAxis;
	}

	public Axis getXAxis() {
		return xAxis;
	}

	/** append a function generated by two queues */
	public void append(String name, DataQueue qx, DataQueue qy, byte smoothFilter) {
		// append2(new Curve(qx, qy, null, new Legend(name)));
		Curve c = curvePool.getCurve();
		c.setSmoothFilter(smoothFilter);
		try {
			c.setCurve(qx, qy);
		}
		catch (MismatchException e) {
			final String s = e.getMessage();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(XYGrapher.this), s, "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			});
			throw new RuntimeException(s);
		}
		c.getLegend().setText(name);
		append2(c);
	}

	private DataSet append2(Curve curve) {

		DataSet dataSet = null;

		try {
			dataSet = graph.loadDataSet(curve.getData());
			dataSet.setCurveModel(curve);
			if (curve.getLegend() != null) {
				dataSet.setLegendText(curve.getLegend().getText());
				dataSet.setLegendLocation(graph.getWidth() - 80, graph.borderTop + 15 * graph.getDataSets().size());
			}
			xAxis.attachDataSet(dataSet);
			yAxis.attachDataSet(dataSet);
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
					"No data to plot. Collect some and come back.", "Empty Data Sets", JOptionPane.ERROR_MESSAGE);
			return null;
		}

		curveGroup.addCurve(curve);

		return dataSet;

	}

	public void append(Curve curve) {
		DataSet dataSet = append2(curve);
		if (dataSet == null)
			return;
		if (curve.getFlavor() != null) {
			dataSet.setLineColor(curve.getFlavor().getColor());
			dataSet.setLineStroke(curve.getFlavor().getThickness());
			dataSet.setLineStroke(curve.getFlavor().getLineStyle());
			dataSet.setSymbol(curve.getFlavor().getSymbol().getType());
			dataSet.setSymbolSize(curve.getFlavor().getSymbol().getSize());
			dataSet.setSymbolSpacing(curve.getFlavor().getSymbol().getSpacing());
		}
		if (curve.getLegend() != null) {
			dataSet.legendColor(curve.getLegend().getColor());
			dataSet.legendFont(curve.getLegend().getFont());
			dataSet.setLegendLocation(curve.getLegend().getLocation().x, curve.getLegend().getLocation().y);
		}
		dataSet.linestyle = 1;
		dataSet.marker = 1;
		dataSet.markerscale = 1.5;
		dataSet.markercolor = Color.red;
	}

	public void input(CurveGroup cg) {
		removeAllCurves();
		synchronized (cg.getSynchronizationLock()) {
			for (Iterator it = cg.iterator(); it.hasNext();) {
				append((Curve) it.next());
			}
		}
		curveGroup.setLabelOfX(cg.getLabelOfX());
		curveGroup.setLabelOfY(cg.getLabelOfY());
		xAxis.setTitleText(cg.getLabelOfX().getText());
		yAxis.setTitleText(cg.getLabelOfY().getText());
	}

	public void fixScopeOfX(double x0, double x1) {
		xAxis.minimum = x0;
		xAxis.maximum = x1;
	}

	public void fixScopeOfY(double y0, double y1) {
		yAxis.minimum = y0;
		yAxis.maximum = y1;
	}

	private void setScopeFields() {
		if (xminField == null) {
			xminField = new RealNumberTextField(xAxis.minimum, -Double.MAX_VALUE, Double.MAX_VALUE);
		}
		else {
			xminField.setValue(xAxis.minimum);
			ActionListener[] al = xminField.getActionListeners();
			for (ActionListener a : al)
				xminField.removeActionListener(a);
		}
		if (xmaxField == null) {
			xmaxField = new RealNumberTextField(xAxis.maximum, -Double.MAX_VALUE, Double.MAX_VALUE);
		}
		else {
			xmaxField.setValue(xAxis.maximum);
			ActionListener[] al = xmaxField.getActionListeners();
			for (ActionListener a : al)
				xmaxField.removeActionListener(a);
		}
		if (yminField == null) {
			yminField = new RealNumberTextField(yAxis.minimum, -Double.MAX_VALUE, Double.MAX_VALUE);
		}
		else {
			yminField.setValue(yAxis.minimum);
			ActionListener[] al = yminField.getActionListeners();
			for (ActionListener a : al)
				yminField.removeActionListener(a);
		}
		if (ymaxField == null) {
			ymaxField = new RealNumberTextField(yAxis.maximum, -Double.MAX_VALUE, Double.MAX_VALUE);
		}
		else {
			ymaxField.setValue(yAxis.maximum);
			ActionListener[] al = ymaxField.getActionListeners();
			for (ActionListener a : al)
				ymaxField.removeActionListener(a);
		}
	}

	private void init() {

		xAxis.setTitleText("x"); // RTextLine needs this to initialize correctly
		yAxis.setTitleText("y");

		curveGroup = new CurveGroup();

		openAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				if (fileChooser == null)
					return;
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(yoxFilter);
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
				fileChooser.setDialogTitle("Open");
				fileChooser.setApproveButtonText("Open");
				fileChooser.setApproveButtonMnemonic('O');
				int returnValue = fileChooser.showSaveDialog(XYGrapher.this);
				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					input(file);
					repaint();
				}
				fileChooser.resetChoosableFileFilters();
				fileChooser.setAccessory(null);
			}
		};
		openAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		openAction.putValue(Action.SMALL_ICON, IconPool.getIcon("open"));
		openAction.putValue(Action.SHORT_DESCRIPTION, "Open graph");
		openAction.putValue(Action.NAME, "Open Graph");
		getActionMap().put(openAction.toString(), openAction);

		saveAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				if (fileChooser == null)
					return;
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(yoxFilter);
				fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
				fileChooser.setDialogTitle("Save");
				fileChooser.setApproveButtonText("Save");
				fileChooser.setApproveButtonMnemonic('S');
				int returnValue = fileChooser.showSaveDialog(XYGrapher.this);
				if (returnValue == JFileChooser.APPROVE_OPTION) {
					String filename = FileUtilities.fileNameAutoExtend(fileChooser.getFileFilter(), fileChooser
							.getSelectedFile());
					File file = new File(filename);
					if (file.exists()) {
						if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(XYGrapher.this), "File "
								+ file.getName() + " exists, overwrite?", "File exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							fileChooser.resetChoosableFileFilters();
							return;
						}
					}
					XYGrapher.this.output(file);
				}
				fileChooser.resetChoosableFileFilters();
				fileChooser.setAccessory(null);
			}
		};
		saveAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		saveAction.putValue(Action.SMALL_ICON, IconPool.getIcon("save"));
		saveAction.putValue(Action.SHORT_DESCRIPTION, "Save graph");
		saveAction.putValue(Action.NAME, "Save Graph");
		getActionMap().put(saveAction.toString(), saveAction);

		newAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				XYGrapher g = new XYGrapher();
				g.setPreferredSize(new Dimension(300, 300));
				JDialog dialog = ModelerUtilities.getChildDialog(XYGrapher.this, "X-Y Graph", true);
				Dimension siz = getSize();
				Point loc = getLocationOnScreen();
				dialog.setLocation(loc.x + siz.width / 5, loc.y + siz.height / 5);
				g.setDialog(dialog);
				dialog.getContentPane().add(g, BorderLayout.CENTER);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.pack();
				dialog.setVisible(true);
			}

		};
		newAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		newAction.putValue(Action.SMALL_ICON, IconPool.getIcon("new"));
		newAction.putValue(Action.SHORT_DESCRIPTION, "New graph");
		newAction.putValue(Action.NAME, "New Graph");
		getActionMap().put(newAction.toString(), newAction);

		scopingAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				setScopeFields();
				Object o = getValue("i18n");
				final JDialog dialog = ModelerUtilities.getChildDialog(XYGrapher.this, o != null ? (String) o
						: "Set Scope", true);
				JPanel p = new JPanel(new GridLayout(4, 2, 5, 5));
				p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
				p.add(new JLabel("xmin:"));
				p.add(xminField);
				p.add(new JLabel("xmax:"));
				p.add(xmaxField);
				p.add(new JLabel("ymin:"));
				p.add(yminField);
				p.add(new JLabel("ymax:"));
				p.add(ymaxField);
				dialog.getContentPane().add(p, BorderLayout.CENTER);
				ActionListener listener = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						fixScopeOfX(xminField.getValue(), xmaxField.getValue());
						fixScopeOfY(yminField.getValue(), ymaxField.getValue());
						XYGrapher.this.repaint();
						dialog.dispose();
					}
				};
				xminField.addActionListener(listener);
				xmaxField.addActionListener(listener);
				yminField.addActionListener(listener);
				ymaxField.addActionListener(listener);
				p = new JPanel();
				String s = getInternationalText("OK");
				JButton button = new JButton(s != null ? s : "OK");
				button.addActionListener(listener);
				p.add(button);
				s = getInternationalText("Cancel");
				button = new JButton(s != null ? s : "Cancel");
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dialog.dispose();
					}
				});
				p.add(button);
				dialog.getContentPane().add(p, BorderLayout.SOUTH);
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.pack();
				dialog.setLocationRelativeTo(XYGrapher.this);
				dialog.setVisible(true);
			}

		};
		scopingAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		scopingAction.putValue(Action.SMALL_ICON, SCOPE_ICON);
		scopingAction.putValue(Action.SHORT_DESCRIPTION, "Set scope");
		scopingAction.putValue(Action.NAME, "Set Scope");
		getActionMap().put(scopingAction.toString(), scopingAction);

		takeAverageAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				computeCurrentAverages();
			}
		};
		takeAverageAction.putValue(Action.SMALL_ICON, TAKE_ICON);
		takeAverageAction.putValue(Action.SHORT_DESCRIPTION,
				"Take a time average sample for x and y (when x is not time)");
		takeAverageAction.putValue(Action.NAME, "Take Time Average Sample");
		getActionMap().put(takeAverageAction.toString(), takeAverageAction);

		printAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				PrinterJob printerJob = PrinterJob.getPrinterJob();
				graph.setCurrentPageFormat(printerJob.validatePage(graph.getCurrentPageFormat()));
				printerJob.setPrintable(graph, graph.getCurrentPageFormat());
				printerJob.setPageable(graph);
				printerJob.setCopies(1);
				if (printerJob.printDialog()) {
					try {
						printerJob.print();
					}
					catch (PrinterException pe) {
						printerJob.cancel();
					}
				}
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		};
		printAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		printAction.putValue(Action.SMALL_ICON, IconPool.getIcon("printer"));
		printAction.putValue(Action.SHORT_DESCRIPTION, "Print graph");
		printAction.putValue(Action.NAME, "Print Graph");
		getActionMap().put(printAction.toString(), printAction);

		gridAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				Object src = e.getSource();
				if (src instanceof AbstractButton) {
					graph.drawgrid = ((AbstractButton) src).isSelected();
					graph.repaint();
				}
			}
		};
		gridAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
		gridAction.putValue(Action.SMALL_ICON, GRID_ICON);
		gridAction.putValue(Action.SHORT_DESCRIPTION, "Show grid");
		gridAction.putValue(Action.NAME, "Show Grid");
		getActionMap().put(gridAction.toString(), gridAction);

		propertiesAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				if (gDialog == null)
					gDialog = new GraphDialog(graph);
				gDialog.setCurrentValues();
				gDialog.pack();
				gDialog.setLocationRelativeTo(XYGrapher.this);
				gDialog.setVisible(true);
			}
		};
		propertiesAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
		propertiesAction.putValue(Action.SMALL_ICON, IconPool.getIcon("properties"));
		propertiesAction.putValue(Action.SHORT_DESCRIPTION, "Graph properties");
		propertiesAction.putValue(Action.NAME, "Set Graph Properties");
		getActionMap().put(propertiesAction.toString(), propertiesAction);

		listAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				listDataSets();
			}
		};
		listAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
		listAction.putValue(Action.SMALL_ICON, LIST_ICON);
		listAction.putValue(Action.SHORT_DESCRIPTION, "List data sets");
		listAction.putValue(Action.NAME, "List Data Sets");
		getActionMap().put(listAction.toString(), listAction);

		runningAverageAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				if (curveGroup == null || curveGroup.hasNoCurve())
					return;
				int n = curveGroup.curveCount();
				if (n == 0)
					return;
				Curve curve = null, curve1 = null;
				CurveGroup cg = new CurveGroup(curveGroup.getTitle(), curveGroup.getLabelOfX(), curveGroup
						.getLabelOfY());
				synchronized (curveGroup.getSynchronizationLock()) {
					for (Iterator it = curveGroup.iterator(); it.hasNext();) {
						curve = (Curve) it.next();
						curve1 = curve.getCumulativeRunningAverage();
						curve1.setFlavor(curve.getFlavor());
						curve1.setLegend(curve.getLegend());
						cg.addCurve(curve1);
					}
				}
				String s = getInternationalText("GrowingPointRunningAverage");
				showCurveGroup(s != null ? s : "Growing-point running averages", cg, true);
			}
		};
		runningAverageAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		runningAverageAction.putValue(Action.SMALL_ICON, AVERAGE_ICON);
		runningAverageAction.putValue(Action.SHORT_DESCRIPTION,
				"Show growing-point running averages on a separate window (for ensemble average)");
		runningAverageAction.putValue(Action.NAME, "Show Growing-Point Running Averages");
		getActionMap().put(runningAverageAction.toString(), runningAverageAction);

		runningAverage2Action = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				if (curveGroup == null || curveGroup.hasNoCurve())
					return;
				int n = curveGroup.curveCount();
				if (n == 0)
					return;
				Curve curve = null, curve1 = null;
				CurveGroup cg = new CurveGroup(curveGroup.getTitle(), curveGroup.getLabelOfX(), curveGroup
						.getLabelOfY());
				synchronized (curveGroup.getSynchronizationLock()) {
					for (Iterator it = curveGroup.iterator(); it.hasNext();) {
						curve = (Curve) it.next();
						curve1 = curve.getExponentialRunningAverage(0.05);
						curve1.setFlavor(curve.getFlavor());
						curve1.setLegend(curve.getLegend());
						cg.addCurve(curve1);
					}
				}
				String s = getInternationalText("ExponentialRunningAverage");
				showCurveGroup(s != null ? s : "Exponential running averages", cg, true);
			}
		};
		runningAverage2Action.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		runningAverage2Action.putValue(Action.SMALL_ICON, AVERAGE2_ICON);
		runningAverage2Action.putValue(Action.SHORT_DESCRIPTION,
				"Show exponential running averages on a separate window (for filtering noise)");
		runningAverage2Action.putValue(Action.NAME, "Show Exponential Running Averages");
		getActionMap().put(runningAverage2Action.toString(), runningAverage2Action);

		derivativeAction = new AbstractAction() {
			public String toString() {
				return (String) getValue(NAME);
			}

			public void actionPerformed(ActionEvent e) {
				if (curveGroup == null || curveGroup.hasNoCurve())
					return;
				int n = curveGroup.curveCount();
				if (n == 0)
					return;
				Curve curve = null, curve1 = null;
				CurveGroup cg = new CurveGroup(curveGroup.getTitle(), curveGroup.getLabelOfX(), curveGroup
						.getLabelOfY());
				synchronized (curveGroup.getSynchronizationLock()) {
					for (Iterator it = curveGroup.iterator(); it.hasNext();) {
						curve = (Curve) it.next();
						curve1 = curve.getDerivative();
						curve1.setFlavor(curve.getFlavor());
						curve1.setLegend(curve.getLegend());
						cg.addCurve(curve1);
					}
				}
				String s = getInternationalText("FirstOrderDerivative");
				showCurveGroup(s != null ? s : "First-order derivative", cg, false);
			}
		};
		derivativeAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		derivativeAction.putValue(Action.SMALL_ICON, DERIV_ICON);
		derivativeAction.putValue(Action.SHORT_DESCRIPTION, "Show the first-order derivative on a separate window");
		derivativeAction.putValue(Action.NAME, "Show First-Order Derivative");
		getActionMap().put(derivativeAction.toString(), derivativeAction);

	}

	void showCurveGroup(String title, CurveGroup cg, boolean inherit) {
		XYGrapher g = new XYGrapher();
		g.setPreferredSize(new Dimension(300, 300));
		g.input(cg);
		g.setLegendLocation(getLegendLocation());
		if (inherit)
			g.fixScopeOfY(yAxis.minimum, yAxis.maximum);
		g.addSnapshotListener(getSnapshotListener());
		JDialog dialog = ModelerUtilities.getChildDialog(XYGrapher.this, title, false);
		Dimension siz = getSize();
		Point loc = getLocationOnScreen();
		dialog.setLocation(loc.x + siz.width / 10, loc.y + siz.height / 10);
		g.setDialog(dialog);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(g, BorderLayout.CENTER);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setVisible(true);
	}

	/** if the x variable is not time, running averages should not be computed. */
	protected void enableRunningAveragesComputing(boolean b) {
		runningAverageAction.setEnabled(b);
		runningAverage2Action.setEnabled(b);
		takeAverageAction.setEnabled(!b);
	}

	public void input(File file) {
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
		}
		if (in == null)
			return;
		CurveGroup cg = null;
		try {
			ObjectInputStream s = new ObjectInputStream(in);
			try {
				cg = (CurveGroup) s.readObject();
			}
			catch (ClassNotFoundException e1) {
				e1.printStackTrace(System.err);
			}
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace(System.err);
		}
		if (cg != null) {
			input(cg);
			if (host != null)
				host.setTitle(file.getName());
		}
	}

	public void output(File file) {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
		}
		if (out == null)
			return;
		try {
			ObjectOutputStream s = new ObjectOutputStream(out);
			s.writeObject(curveGroup);
			s.flush();
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	public void addSnapshotListener(ActionListener listener) {
		removeSnapshotListeners();
		if (snapshotMenuItem != null) {
			snapshotMenuItem.addActionListener(listener);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					snapshotMenuItem.setEnabled(true);
				}
			});
		}
		if (snapshotButton != null) {
			snapshotButton.addActionListener(listener);
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					snapshotButton.setEnabled(true);
				}
			});
		}
	}

	public void removeSnapshotListeners() {
		if (snapshotMenuItem != null) {
			ActionListener[] a = snapshotMenuItem.getActionListeners();
			if (a != null && a.length > 0) {
				for (ActionListener x : a) {
					snapshotMenuItem.removeActionListener(x);
				}
			}
		}
		if (snapshotButton != null) {
			ActionListener[] a = snapshotButton.getActionListeners();
			if (a != null && a.length > 0) {
				for (ActionListener x : a) {
					snapshotButton.removeActionListener(x);
				}
			}
		}
	}

	public ActionListener getSnapshotListener() {
		if (snapshotButton.getActionListeners() == null || snapshotButton.getActionListeners().length == 0)
			return null;
		return snapshotButton.getActionListeners()[0];
	}

	public void addMenuBar() {
		if (topPanel == null || menuBar == null)
			return;
		topPanel.add(menuBar, BorderLayout.NORTH);
	}

	public void removeMenuBar() {
		if (topPanel == null || menuBar == null)
			return;
		topPanel.remove(menuBar);
	}

	protected boolean hasMenuBar() {
		if (topPanel == null)
			return false;
		int n = topPanel.getComponentCount();
		for (int i = 0; i < n; i++) {
			if (topPanel.getComponent(i) == menuBar)
				return true;
		}
		return false;
	}

	private JMenuBar createMenuBar() {

		JMenuBar menuBar = new JMenuBar();
		menuBar.setOpaque(false);

		String s = getInternationalText("File");
		JMenu menu = new JMenu(s != null ? s : "File");
		menuBar.add(menu);

		JMenuItem menuItem = new JMenuItem(newAction);
		s = getInternationalText("NewGraph");
		if (s != null)
			menuItem.setText(s);
		menu.add(menuItem);

		if (openAction != null) {
			menuItem = new JMenuItem(openAction);
			s = getInternationalText("OpenGraph");
			if (s != null)
				menuItem.setText(s);
			menu.add(menuItem);
		}

		menuItem = new JMenuItem(saveAction);
		s = getInternationalText("SaveGraph");
		if (s != null)
			menuItem.setText(s);
		menu.add(menuItem);

		menuItem = new JMenuItem(printAction);
		s = getInternationalText("PrintGraph");
		if (s != null)
			menuItem.setText(s);
		menu.add(menuItem);

		s = getInternationalText("Option");
		menu = new JMenu(s != null ? s : "Option");

		s = getInternationalText("TakeSnapshot");
		snapshotMenuItem = new JMenuItem(s != null ? s : "Take a Snapshot");
		snapshotMenuItem.setIcon(IconPool.getIcon("camera"));
		snapshotMenuItem.setEnabled(false);
		snapshotMenuItem.putClientProperty("graph", XYGrapher.this);
		menu.add(snapshotMenuItem);

		gridMenuItem = new JCheckBoxMenuItem(gridAction);
		s = getInternationalText("ShowGrid");
		if (s != null)
			gridMenuItem.setText(s);
		gridMenuItem.setSelected(false);
		menu.add(gridMenuItem);

		menuItem = new JMenuItem(propertiesAction);
		s = getInternationalText("GraphSettings");
		menuItem.setText(s != null ? s : "Legend and label settings");
		menu.add(menuItem);

		menuItem = new JMenuItem(scopingAction);
		s = getInternationalText("ScopeSettings");
		if (s != null) {
			menuItem.setText(s);
			scopingAction.putValue("i18n", s);
		}
		menu.add(menuItem);

		menuItem = new JMenuItem(runningAverageAction);
		s = getInternationalText("GrowingPointRunningAverage");
		if (s != null) {
			menuItem.setText(s);
			runningAverageAction.putValue("i18n", s);
		}
		menu.add(menuItem);

		menuItem = new JMenuItem(runningAverage2Action);
		s = getInternationalText("ExponentialRunningAverage");
		if (s != null) {
			menuItem.setText(s);
			runningAverage2Action.putValue("i18n", s);
		}
		menu.add(menuItem);

		menuItem = new JMenuItem(derivativeAction);
		s = getInternationalText("FirstOrderDerivative");
		if (s != null) {
			menuItem.setText(s);
			derivativeAction.putValue("i18n", s);
		}
		menu.add(menuItem);

		menuBar.add(menu);

		return menuBar;

	}

	public void addToolBar() {
		if (topPanel == null || toolBar == null)
			return;
		topPanel.add(toolBar, BorderLayout.CENTER);
	}

	public void removeToolBar() {
		if (topPanel == null || toolBar == null)
			return;
		topPanel.remove(toolBar);
	}

	protected boolean hasToolBar() {
		if (topPanel == null)
			return false;
		int n = topPanel.getComponentCount();
		for (int i = 0; i < n; i++) {
			if (topPanel.getComponent(i) == toolBar)
				return true;
		}
		return false;
	}

	protected JButton createTextlessButton(Action a) {
		JButton button = new JButton(a);
		button.setText(null);
		int m = System.getProperty("os.name").startsWith("Mac") ? 6 : 2;
		Insets margin = new Insets(m, m, m, m);
		button.setMargin(margin);
		return button;
	}

	private JComponent createToolBar() {
		JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		toolBar.add(createTextlessButton(listAction));
		toolBar.add(createTextlessButton(takeAverageAction));
		toolBar.add(createTextlessButton(runningAverageAction));
		toolBar.add(createTextlessButton(runningAverage2Action));
		snapshotButton = new JButton(IconPool.getIcon("camera"));
		snapshotButton.setToolTipText("Take a Snapshot");
		snapshotButton.setEnabled(false);
		snapshotButton.putClientProperty("graph", XYGrapher.this);
		int m = System.getProperty("os.name").startsWith("Mac") ? 6 : 2;
		Insets margin = new Insets(m, m, m, m);
		snapshotButton.setMargin(margin);
		toolBar.add(snapshotButton);
		return toolBar;
	}

	private Vector createRow(int n) {
		DataSet ds = graph.getSet(n);
		if (ds == null)
			return null;
		Vector<String> row = new Vector<String>();
		row.add("" + n);
		row.add(ds.getLegendText());
		row.add("" + ds.getData().length / 2);
		row.add(format.format(ds.getXmin()));
		row.add(format.format(ds.getXmax()));
		row.add(format.format(ds.getXave()));
		row.add(format.format(ds.getYmin()));
		row.add(format.format(ds.getYmax()));
		row.add(format.format(ds.getYave()));
		return row;
	}

	private boolean showData(Curve c) {

		if (c == null)
			return false;
		double[] data = c.getData();
		if (data == null)
			return false;
		int n = data.length;
		if (n <= 2)
			return false;

		final JDialog dataWindow = new JDialog(JOptionPane.getFrameForComponent(XYGrapher.this), "Data", true);
		dataWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dataWindow.setLocation(200, 200);

		Object[][] rd = new Object[n >> 1][3];
		String[] cn = new String[] { "i", "x", "y" };
		Legend legend = c.getLegend();
		if (legend != null && legend.getText() != null) {
			String t = legend.getText();
			dataWindow.setTitle(t);
			int q = t.indexOf("-");
			if (q != -1) {
				cn[1] = t.substring(0, q);
				cn[2] = t.substring(q + 1);
			}
		}
		else {
			dataWindow.setTitle("Data");
		}
		int k2;
		for (int k = 0; k < n; k += 2) {
			k2 = k >> 1;
			rd[k2][0] = k2;
			rd[k2][1] = data[k];
			rd[k2][2] = data[k + 1];
		}

		JTable table = new JTable(rd, cn);
		table.setModel(new DefaultTableModel(rd, cn) {
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		});
		dataWindow.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel p = new JPanel();
		dataWindow.getContentPane().add(p, BorderLayout.SOUTH);
		String s = getInternationalText("Close");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dataWindow.dispose();
			}
		});
		p.add(button);

		dataWindow.pack();
		dataWindow.setVisible(true);
		return true;

	}

	private void listDataSets() {

		if (listDialog == null) {

			String s = getInternationalText("DataSet");
			listDialog = ModelerUtilities.getChildDialog(this, s != null ? s : "Data Sets", true);

			table = new JTable();

			columnNames = new Vector<String>();
			columnNames.add("#");
			s = getInternationalText("Name");
			columnNames.add(s != null ? s : "Name");
			s = getInternationalText("Length");
			columnNames.add(s != null ? s : "Length");
			columnNames.add("Xmin");
			columnNames.add("Xmax");
			columnNames.add("<x>");
			columnNames.add("Ymin");
			columnNames.add("Ymax");
			columnNames.add("<y>");

			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table.setSelectionBackground(Color.gray);
			table.getTableHeader().setReorderingAllowed(false);
			table.setShowGrid(false);
			table.setRowHeight(20);
			table.setRowMargin(2);
			table.setColumnSelectionAllowed(false);
			table.setBackground(Color.white);
			table.getTableHeader().setPreferredSize(new Dimension(200, 18));

			rowData = new Vector<Vector>();

			DefaultTableModel tm = new DefaultTableModel(rowData, columnNames) {
				public Class<?> getColumnClass(int columnIndex) {
					return getValueAt(0, columnIndex).getClass();
				}

				public boolean isCellEditable(int row, int col) {
					return false;
				}
			};
			table.setModel(tm);

			table.getColumnModel().getColumn(0).setMaxWidth(10);
			table.getColumnModel().getColumn(1).setPreferredWidth(200);
			table.getColumnModel().getColumn(2).setMaxWidth(50);
			table.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (e.getClickCount() < 2)
						return;
					Curve c = curveGroup.getCurve(table.getSelectedRow());
					if (!showData(c))
						JOptionPane.showMessageDialog(XYGrapher.this, "No data to show.");
				}
			});

			JScrollPane scroller = new JScrollPane(table);
			scroller.setPreferredSize(new Dimension(500, 200));
			scroller.getViewport().setBackground(Color.white);

			listDialog.getContentPane().add(scroller, BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

			s = getInternationalText("DoubleClickToViewNumeric");
			buttonPanel.add(new JLabel(s != null ? s : "Double-click to view the numeric data"));

			s = getInternationalText("Close");
			JButton button = new JButton(s != null ? s : "Close");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					listDialog.dispose();
				}
			});
			buttonPanel.add(button);

			listDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

			listDialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					listDialog.dispose();
				}
			});

		}

		rowData.clear();
		int n = graph.getDataSets().size();
		for (int i = 0; i < n; i++)
			rowData.add(createRow(i));
		table.revalidate();

		listDialog.pack();
		listDialog.setLocationRelativeTo(this);
		listDialog.setVisible(true);

	}

}