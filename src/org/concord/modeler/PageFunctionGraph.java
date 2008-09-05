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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.functiongraph.Axis;
import org.concord.functiongraph.DataSource;
import org.concord.functiongraph.Graph;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.text.Page;

/**
 * @author Charles Xie
 * 
 */
public class PageFunctionGraph extends JPanel implements Embeddable {

	Graph graph;
	Page page;
	private int index;
	private String id;
	private boolean marked;
	private String borderType;
	private static Color defaultBackground;
	private JPopupMenu popupMenu;
	private static PageFunctionGraphMaker maker;
	private MouseListener popupMouseListener;

	public PageFunctionGraph() {
		super(new BorderLayout());
		graph = new Graph();
		add(graph, BorderLayout.CENTER);
		graph.init();
		graph.enablePopupMenu(false);
		JPanel p = new JPanel();
		p.setOpaque(false);
		add(p, BorderLayout.SOUTH);
		String s = Modeler.getInternationalText("InputExpression");
		p.add(new JLabel(s != null ? s : "Type expression (e.g. x*sin(x)):"));
		final JTextField textField = new JTextField(20);
		textField
				.setToolTipText("<html>Please input a function, using x as the variable.<br>Examples: 3*x+4, 5sin(x), 2*e^((-x/10)^2), and then press ENTER.</html>");
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String expression = textField.getText();
				if (expression == null || expression.trim().equals(""))
					return;
				graph.addFunction(expression, false);
			}
		});
		p.add(textField);
		/*
		 * s = Modeler.getInternationalText("Clear"); JButton button = new JButton(s != null ? s : "Clear");
		 * button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { if
		 * (graph.isEmpty()) { return; } String s1 = Modeler.getInternationalText("AreYouSureToRemoveAllFunctions");
		 * String s2 = Modeler.getInternationalText("Remove"); int response =
		 * JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(page), s1 != null ? s1 : "Are you sure to
		 * remove all the functions?", s2 != null ? s2 : "Remove", JOptionPane.YES_NO_OPTION,
		 * JOptionPane.QUESTION_MESSAGE); if (response == JOptionPane.YES_OPTION) graph.removeAll(); } });
		 * p.add(button);
		 */
		if (defaultBackground == null)
			defaultBackground = graph.getBackground();
		setPreferredSize(new Dimension(400, 400));
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		graph.addMouseListener(popupMouseListener);
	}

	public PageFunctionGraph(PageFunctionGraph g, Page parent) {
		this();
		setPage(parent);
		setBackground(g.getBackground());
		setPreferredSize(g.getPreferredSize());
		setBorderType(g.getBorderType());
		setOpaque(g.isOpaque());
		setChangable(page.isEditable());
		int n = g.graph.getNumberOfDataSources();
		if (n > 0) {
			for (int i = 0; i < n; i++) {
				graph.addDataSource(g.graph.getDataSource(i));
			}
		}
		setId(g.id);
	}

	public void addFunction(String expression) {
		graph.addFunction(expression, false);
	}

	public void addDataSource(DataSource ds) {
		graph.addDataSource(ds);
		Axis axis = graph.getAxis(Axis.X_AXIS);
		ds.generateData(axis.getMin(), axis.getMax());
		float x0 = 0.5f * (axis.getMin() + axis.getMax());
		float y0 = ds.evaluate(x0);
		ds.setHotSpot(x0, y0, 10, 10);
	}

	public void setOrigin(int x, int y) {
		graph.getOrigin().setLocation(x, y);
	}

	public void setScope(float xmin, float xmax, float ymin, float ymax) {
		graph.getAxis(Axis.X_AXIS).setMin(xmin);
		graph.getAxis(Axis.X_AXIS).setMax(xmax);
		graph.getAxis(Axis.Y_AXIS).setMin(ymin);
		graph.getAxis(Axis.Y_AXIS).setMax(ymax);
		graph.setOriginalScope(xmin, xmax, ymin, ymax);
	}

	public void setBackground(Color c) {
		super.setBackground(c);
		if (graph != null)
			graph.setBackground(c);
	}

	public void setOpaque(boolean b) {
		super.setOpaque(b);
		if (graph != null)
			graph.setOpaque(b);
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public Page getPage() {
		return page;
	}

	public void setChangable(boolean b) {
		if (b) {
			if (!isChangable())
				addMouseListener(popupMouseListener);
		}
		else {
			if (isChangable())
				removeMouseListener(popupMouseListener);
		}
	}

	public boolean isChangable() {
		MouseListener[] ml = getMouseListeners();
		for (MouseListener x : ml) {
			if (x == popupMouseListener)
				return true;
		}
		return false;
	}

	public void destroy() {
		page = null;
		if (maker != null)
			maker.setObject(null);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		if (graph.getPopupMenu() == null) {
			popupMenu = new JPopupMenu();
		}
		else {
			popupMenu = graph.getPopupMenu();
			popupMenu.addSeparator();
		}
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("TakeSnapshot");
		JMenuItem mi = new JMenuItem((s != null ? s : "Take a Snapshot") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), graph);
			}
		});
		popupMenu.add(mi);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("CustomizeFunctionGraph");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This Function Graph") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageFunctionGraphMaker(PageFunctionGraph.this);
				}
				else {
					maker.setObject(PageFunctionGraph.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveFunctionGraph");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Function Graph");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageFunctionGraph.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyFunctionGraph");
		mi = new JMenuItem(s != null ? s : "Copy This Function Graph");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageFunctionGraph.this);
			}
		});
		popupMenu.add(mi);

		popupMenu.pack();

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustom.setEnabled(isChangable());
				miRemove.setEnabled(isChangable());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getBorderType() {
		if (borderType == null)
			return BorderManager.getBorder(this);
		return borderType;
	}

	public void setBorderType(String s) {
		borderType = s;
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setPreferredSize(Dimension dim) {
		super.setPreferredSize(dim);
		super.setMaximumSize(dim);
		super.setMinimumSize(dim);
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		setBackground(b ? page.getSelectionColor() : defaultBackground);
	}

	public boolean isMarked() {
		return marked;
	}

	public static PageFunctionGraph create(Page page) {
		if (page == null)
			return null;
		PageFunctionGraph g = new PageFunctionGraph();
		if (maker == null) {
			maker = new PageFunctionGraphMaker(g);
		}
		else {
			maker.setObject(g);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return g;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		sb.append("<width>" + getWidth() + "</width><height>" + getHeight() + "</height>");
		if (!graph.isOpaque()) {
			sb.append("<opaque>false</opaque>\n");
		}
		else {
			if (!graph.getBackground().equals(defaultBackground))
				sb.append("<bgcolor>" + Integer.toString(graph.getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		if (borderType != null)
			sb.append("<border>" + borderType + "</border>\n");
		if (!graph.isEmpty()) {
			DataSource ds;
			int n = graph.getNumberOfDataSources();
			for (int i = 0; i < n; i++) {
				sb.append("<function>");
				ds = graph.getDataSource(i);
				sb.append("<expression>" + ds.getExpression() + "</expression>\n");
				sb.append("<color>" + Integer.toString(ds.getColor().getRGB(), 16) + "</color>");
				sb.append("<weight>" + ds.getLineWeight() + "</weight>");
				sb.append("<style>" + LineStyle.getStrokeNumber(ds.getStroke()) + "</style>");
				int datapoint = ds.getPreferredPointNumber();
				if (datapoint != 50)
					sb.append("<datapoint>" + datapoint + "</datapoint>");
				sb.append("</function>");
			}
		}
		Axis axis = graph.getAxis(Axis.X_AXIS);
		if (axis.getMin() != -5)
			sb.append("<axis_x_min>" + axis.getMin() + "</axis_x_min>");
		if (axis.getMax() != 5)
			sb.append("<axis_x_max>" + axis.getMax() + "</axis_x_max>");
		axis = graph.getAxis(Axis.Y_AXIS);
		if (axis.getMin() != -5)
			sb.append("<axis_y_min>" + axis.getMin() + "</axis_y_min>");
		if (axis.getMax() != 5)
			sb.append("<axis_y_max>" + axis.getMax() + "</axis_y_max>");
		return sb.toString();
	}

}