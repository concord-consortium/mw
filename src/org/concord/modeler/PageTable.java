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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;

public class PageTable extends JScrollPane implements Embeddable, AutoResizable {

	private final static String REGEX_SEPARATOR = ",[\\s&&[^\\r\\n]]*";

	Page page;
	JTable table;
	float widthRatio = 1, heightRatio = 1;
	boolean widthIsRelative, heightIsRelative;
	private JTableHeader columnHeader;
	private JList rowHeader;
	private JButton corner;
	private boolean hasTableRowHeader;
	private int index;
	private String id;
	private boolean marked;
	private MouseWheelListener mouseWheelListener;
	private DefaultListModel listModel;
	private JPopupMenu popupMenu;
	private static PageTableMaker maker;

	private MouseListener popupMouseListener = new MouseAdapter() {
		private boolean popupTrigger;

		public void mousePressed(MouseEvent e) {
			popupTrigger = e.isPopupTrigger();
		}

		public void mouseReleased(MouseEvent e) {
			if (popupTrigger || e.isPopupTrigger()) {
				Point p = new Point(e.getX(), e.getY());
				int irow = table.rowAtPoint(p);
				table.setRowSelectionInterval(irow, irow);
				int icol = table.columnAtPoint(p);
				table.setColumnSelectionInterval(icol, icol);
				if (popupMenu == null)
					createPopupMenu();
				popupMenu.show(popupMenu.getInvoker(), p.x + 5, p.y + 5);
			}
			else {
				table.clearSelection();
			}
		}
	};

	private TableModelListener tableListener = new TableModelListener() {
		public void tableChanged(TableModelEvent e) {
			if (page != null)
				page.getSaveReminder().setChanged(true);
		}
	};

	private TableColumnModelListener columnListener = new TableColumnModelListener() {
		public void columnAdded(TableColumnModelEvent e) {
			if (page != null)
				page.getSaveReminder().setChanged(true);
		}

		public void columnMoved(TableColumnModelEvent e) {
			if (page != null)
				page.getSaveReminder().setChanged(true);
		}

		public void columnRemoved(TableColumnModelEvent e) {
			if (page != null)
				page.getSaveReminder().setChanged(true);
		}

		public void columnMarginChanged(ChangeEvent e) {
			if (page != null)
				page.getSaveReminder().setChanged(true);
		}

		public void columnSelectionChanged(ListSelectionEvent e) {
		}
	};

	public PageTable() {
		this(true, true);
	}

	PageTable(boolean hasColumnHeader, boolean hasRowHeader) {

		super(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_NEVER);
		setBorder(BorderFactory.createEmptyBorder());

		table = new JTable();
		setViewportView(table);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(true);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(true);
		table.setIntercellSpacing(new Dimension(10, 10));
		table.addMouseListener(popupMouseListener);
		setGridColor(Color.black);
		columnHeader = table.getTableHeader();
		columnHeader.addMouseListener(popupMouseListener);
		columnHeader.setPreferredSize(new Dimension(400, 24));
		if (!hasColumnHeader)
			removeTableColumnHeader();
		listModel = new DefaultListModel();
		rowHeader = new JList(listModel);
		rowHeader.setFixedCellHeight(table.getRowHeight());
		rowHeader.setCellRenderer(new RowHeaderRenderer(this));
		if (hasRowHeader)
			addTableRowHeader();

		corner = new JButton();
		corner.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isChangable() && page != null) {
					if (maker == null) {
						maker = new PageTableMaker(PageTable.this);
					}
					else {
						maker.setObject(PageTable.this);
					}
					maker.invoke(page);
				}
			}
		});
		setCorner(UPPER_LEFT_CORNER, corner);
		corner.setEnabled(false);

		setTableBackground(Color.white);

		try {
			mouseWheelListener = getMouseWheelListeners()[0];
		}
		catch (Throwable t) {
			// ignore
		}
		removeScrollerMouseWheelListener();

		table.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (getVerticalScrollBar().isShowing() || getHorizontalScrollBar().isShowing())
					addScrollerMouseWheelListener();
			}

			public void focusLost(FocusEvent e) {
				removeScrollerMouseWheelListener();
			}
		});

	}

	public PageTable(String[][] o, boolean hasColumnHeader, boolean hasRowHeader) {
		this(hasColumnHeader, hasRowHeader);
		int row = o.length, col = o[0].length;
		setRowCount(row);
		setColumnCount(col);
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				table.setValueAt(o[i][j], i, j);
			}
		}
	}

	public PageTable(PageTable t, Page parent) {
		this(t.hasTableColumnHeader(), t.hasTableRowHeader());
		setPage(parent);
		setTableBackground(t.getTableBackground());
		int nrow = t.getRowCount();
		int ncol = t.getColumnCount();
		setColumnCount(ncol);
		setRowCount(nrow);
		for (int i = 0; i < nrow; i++) {
			for (int j = 0; j < ncol; j++) {
				table.setValueAt(t.table.getValueAt(i, j), i, j);
			}
		}
		int w = t.getPreferredSize().width;
		int h = t.getPreferredSize().height;
		table.setShowHorizontalLines(t.table.getShowHorizontalLines());
		table.setShowVerticalLines(t.table.getShowVerticalLines());
		table.setIntercellSpacing(t.table.getIntercellSpacing());
		setGridColor(t.table.getGridColor());
		setBorderType(t.getBorderType());
		setWidthRelative(t.isWidthRelative());
		setHeightRelative(t.isHeightRelative());
		if (isWidthRelative()) {
			setWidthRatio(t.getWidthRatio());
			w = (int) (page.getWidth() * getWidthRatio());
		}
		if (isHeightRelative()) {
			setHeightRatio(t.getHeightRatio());
			h = (int) (page.getHeight() * getHeightRatio());
		}
		setRowHeight(t.table.getRowHeight());
		setPreferredSize(new Dimension(w, h));
		if (t.hasTableColumnHeader())
			setColumnNames(t.getColumnNames());
		if (t.hasTableRowHeader())
			setRowNames(t.getRowNames());
		setCellAlignment(t.getCellAlignment());
		setChangable(page.isEditable());
		setId(t.id);
	}

	public void setOpaque(boolean b) {
		super.setOpaque(b);
		getViewport().setOpaque(b);
		if (table == null)
			return;
		table.setOpaque(b);
		int n = getColumnCount();
		TableCellRenderer r;
		for (int i = 0; i < n; i++) {
			r = table.getCellRenderer(0, i);
			if (r instanceof JComponent) {
				((JComponent) r).setOpaque(b);
			}
		}
	}

	private void removeScrollerMouseWheelListener() {
		if (mouseWheelListener != null)
			removeMouseWheelListener(mouseWheelListener);
	}

	private void addScrollerMouseWheelListener() {
		if (mouseWheelListener != null)
			addMouseWheelListener(mouseWheelListener);
	}

	public void destroy() {
		page = null;
	}

	public void setTableBackground(Color c) {
		super.setBackground(c);
		getViewport().setBackground(c);
		table.setBackground(c);
		rowHeader.setBackground(c);
	}

	public Color getTableBackground() {
		return table.getBackground();
	}

	public void removeTableColumnHeader() {
		if (!hasTableColumnHeader())
			return;
		table.setTableHeader(null);
		validate();
	}

	public void removeTableRowHeader() {
		if (!hasTableRowHeader())
			return;
		setRowHeaderView(null);
		validate();
	}

	public void addTableColumnHeader() {
		if (hasTableColumnHeader())
			return;
		table.setTableHeader(columnHeader);
		validate();
	}

	public void addTableRowHeader() {
		listModel.clear();
		int n = table.getRowCount();
		for (int i = 0; i < n; i++) {
			listModel.addElement((char) ('a' + i) + "");
		}
		setRowHeaderView(rowHeader);
		validate();
	}

	public boolean hasTableRowHeader() {
		return hasTableRowHeader;
	}

	public boolean hasTableColumnHeader() {
		return table.getTableHeader() == columnHeader;
	}

	public Component getTableRowHeader() {
		return rowHeader;
	}

	public JTableHeader getTableColumnHeader() {
		return columnHeader;
	}

	public void setRowHeaderView(Component view) {
		super.setRowHeaderView(view);
		hasTableRowHeader = view != null;
	}

	private void removeSelectedRow() {
		if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(this), "Do you really want to remove the "
				+ (table.getSelectedRow() + 1) + " row?", "Confirm", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
			return;
		((DefaultTableModel) table.getModel()).removeRow(table.getSelectedRow());
		setRowHeight(getTableBodyHeight() / table.getRowCount());
	}

	private void removeSelectedColumn() {
		if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(this), "Do you really want to remove the "
				+ (table.getSelectedColumn() + 1) + " column?", "Confirm", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
			return;
		table.getColumnModel().removeColumn(table.getColumnModel().getColumn(table.getSelectedColumn()));
	}

	private void insertRowAbove() {
		((DefaultTableModel) table.getModel()).insertRow(table.getSelectedRow(), new Object[getColumnCount()]);
		setRowHeight(getTableBodyHeight() / getRowCount());
	}

	private void insertRowBelow() {
		((DefaultTableModel) table.getModel()).insertRow(table.getSelectedRow() + 1, new Object[getColumnCount()]);
		setRowHeight(getTableBodyHeight() / getRowCount());
	}

	private void insertColumnLeft() {
		String[] colNames = getColumnNames().split(REGEX_SEPARATOR);
		int i = table.getSelectedColumn();
		int c = getColumnCount();
		TableColumnModel tcm = table.getColumnModel();
		int[] w = new int[c + 1];
		for (int k = 0; k < c; k++) {
			w[k] = tcm.getColumn(k).getWidth();
		}
		DefaultTableModel tm = (DefaultTableModel) table.getModel();
		tm.addColumn("Col. " + c);
		table.moveColumn(c, i);
		validateData();
		int x = 10;
		for (int k = 0; k <= c; k++) {
			if (k == i) {
				x = getWidth() / (c + 1);
				tcm.getColumn(k).setHeaderValue("Col. " + c);
			}
			else if (k > i) {
				x = w[k - 1];
				tcm.getColumn(k).setHeaderValue(colNames[k - 1]);
			}
			else if (k < i) {
				x = w[k];
				tcm.getColumn(k).setHeaderValue(colNames[k]);
			}
			tcm.getColumn(k).setPreferredWidth(x);
		}
	}

	private void insertColumnRight() {
		String[] colNames = getColumnNames().split(REGEX_SEPARATOR);
		int i = table.getSelectedColumn();
		int c = getColumnCount();
		TableColumnModel tcm = table.getColumnModel();
		int[] w = new int[c + 1];
		for (int k = 0; k < c; k++) {
			w[k] = tcm.getColumn(k).getWidth();
		}
		DefaultTableModel tm = (DefaultTableModel) table.getModel();
		tm.addColumn("Col. " + c);
		table.moveColumn(c, i + 1);
		validateData();
		int x = 10;
		for (int k = 0; k <= c; k++) {
			if (k == i + 1) {
				x = getWidth() / (c + 1);
				tcm.getColumn(k).setHeaderValue("Col. " + c);
			}
			else if (k > i + 1) {
				x = w[k - 1];
				tcm.getColumn(k).setHeaderValue(colNames[k - 1]);
			}
			else if (k < i + 1) {
				x = w[k];
				tcm.getColumn(k).setHeaderValue(colNames[k]);
			}
			tcm.getColumn(k).setPreferredWidth(x);
		}
	}

	private void validateData() {
		DefaultTableModel tm = (DefaultTableModel) table.getModel();
		int m = getRowCount();
		int n = getColumnCount();
		Object[][] data = new Object[m][n];
		String[] colName = getColumnNames().split(REGEX_SEPARATOR);
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				data[i][j] = table.getValueAt(i, j);
			}
		}
		tm.setDataVector(data, colName);
	}

	public int getCellAlignment() {
		TableCellRenderer renderer = table.getCellRenderer(0, 0);
		if (renderer instanceof DefaultTableCellRenderer)
			return ((DefaultTableCellRenderer) renderer).getHorizontalAlignment();
		return SwingConstants.LEFT;
	}

	public void setCellAlignment(int alignment) {
		TableCellRenderer renderer = table.getCellRenderer(0, 0);
		if (renderer instanceof DefaultTableCellRenderer) {
			((DefaultTableCellRenderer) renderer).setHorizontalAlignment(alignment);
			table.repaint();
		}
		renderer = columnHeader.getDefaultRenderer();
		if (renderer instanceof DefaultTableCellRenderer) {
			((DefaultTableCellRenderer) renderer).setHorizontalAlignment(alignment);
			columnHeader.repaint();
		}
		ListCellRenderer lcr = rowHeader.getCellRenderer();
		if (lcr instanceof JLabel) {
			((JLabel) lcr).setHorizontalAlignment(alignment);
			rowHeader.repaint();
		}
	}

	private static int getHeaderBodyGap() {
		return 4;
	}

	public int getTableBodyHeight() {
		Insets insets = getBorder().getBorderInsets(this);
		if (hasTableColumnHeader())
			return getPreferredSize().height - insets.top - insets.bottom - getHeaderBodyGap()
					- getTableColumnHeader().getPreferredSize().height;
		return getPreferredSize().height - insets.top - insets.bottom;
	}

	public void setColumnNames(String s) {
		if (s == null) {
			removeTableColumnHeader();
			return;
		}
		s = s.trim();
		if (s.equals("")) {
			removeTableColumnHeader();
			return;
		}
		addTableColumnHeader();
		String[] str = s.split(REGEX_SEPARATOR);
		TableColumnModel cm = table.getColumnModel();
		int n = Math.min(getColumnCount(), str.length);
		TableColumn column;
		for (int i = 0; i < n; i++) {
			column = cm.getColumn(i);
			column.setHeaderValue(str[i].trim());
		}
	}

	public void setRowNames(String s) {
		if (s == null) {
			removeTableRowHeader();
			return;
		}
		s = s.trim();
		if (s.equals("")) {
			removeTableRowHeader();
			return;
		}
		addTableRowHeader();
		String[] str = s.split(REGEX_SEPARATOR);
		int n = Math.min(getRowCount(), str.length);
		listModel.clear();
		for (int i = 0; i < n; i++)
			listModel.addElement(str[i]);
	}

	public String getColumnNames() {
		String s = "";
		TableColumnModel cm = table.getColumnModel();
		int ncol = cm.getColumnCount();
		if (ncol > 1) {
			for (int i = 0; i < ncol - 1; i++)
				s += cm.getColumn(i).getHeaderValue() + ", ";
			s += cm.getColumn(ncol - 1).getHeaderValue().toString();
		}
		else {
			s = cm.getColumn(0).getHeaderValue().toString();
		}
		return s;
	}

	public String getRowNames() {
		String s = "";
		int n = listModel.size();
		if (n > 1) {
			for (int i = 0; i < n - 1; i++) {
				s += listModel.get(i) + ", ";
			}
			s += listModel.get(n - 1);
		}
		else if (n == 1) {
			s += listModel.get(0);
		}
		return s;
	}

	public void setColumnWidths(String s) {
		if (s == null)
			return;
		s = s.trim();
		if (s.equals(""))
			return;
		String[] str = s.split(REGEX_SEPARATOR);
		TableColumnModel cm = table.getColumnModel();
		int n = Math.min(getColumnCount(), str.length);
		TableColumn column;
		for (int i = 0; i < n; i++) {
			column = cm.getColumn(i);
			column.setMinWidth(Float.valueOf(str[i].trim()).intValue());
			column.setMaxWidth(column.getMinWidth());
		}
		table.doLayout();
	}

	private void relaxColumns() {
		TableColumnModel cm = table.getColumnModel();
		int n = getColumnCount();
		for (int i = 0; i < n; i++) {
			cm.getColumn(i).setMinWidth(0);
			cm.getColumn(i).setMaxWidth(10000);
		}
	}

	public String getColumnWidths() {
		String s = "";
		TableColumnModel cm = table.getColumnModel();
		int ncol = cm.getColumnCount();
		if (ncol > 1) {
			for (int i = 0; i < ncol - 1; i++)
				s += cm.getColumn(i).getWidth() + ", ";
			s += "" + cm.getColumn(ncol - 1).getWidth();
		}
		else {
			s = "" + cm.getColumn(0).getWidth();
		}
		return s;
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

	public void setChangable(boolean b) {
		setEnabled(b);
		table.setEnabled(b);
		corner.setEnabled(b);
		if (table.getTableHeader() != null)
			table.getTableHeader().setReorderingAllowed(b);
		if (b) {
			table.getModel().addTableModelListener(tableListener);
			table.getColumnModel().addColumnModelListener(columnListener);
			relaxColumns();
		}
		else {
			table.getModel().removeTableModelListener(tableListener);
			table.getColumnModel().removeColumnModelListener(columnListener);
		}
	}

	public boolean isChangable() {
		return isEnabled();
	}

	public void setGridColor(Color c) {
		table.setGridColor(c);
		table.setBorder(BorderFactory.createLineBorder(table.getGridColor()));
	}

	public void setRowHeight(int i) {
		rowHeader.setFixedCellHeight(i);
		table.setRowHeight(i);
	}

	public void setShowVerticalLines(boolean b) {
		table.setShowVerticalLines(b);
	}

	public void setShowHorizontalLines(boolean b) {
		table.setShowHorizontalLines(b);
	}

	public void setIntercellSpacing(Dimension d) {
		table.setIntercellSpacing(d);
	}

	public Dimension getIntercellSpacing() {
		return table.getIntercellSpacing();
	}

	public int getRowMargin() {
		return table.getRowMargin();
	}

	public void setRowMargin(int i) {
		table.setRowMargin(i);
	}

	public void setColumnCount(int i) {
		((DefaultTableModel) table.getModel()).setColumnCount(i);
	}

	public int getColumnCount() {
		return table.getColumnModel().getColumnCount();
	}

	public void setRowCount(int i) {
		((DefaultTableModel) table.getModel()).setRowCount(i);
	}

	public int getRowCount() {
		return table.getModel().getRowCount();
	}

	public void setMarked(boolean b) {
		if (table == null)
			return;
		marked = b;
		if (marked) {
			table.selectAll();
		}
		else {
			table.clearSelection();
		}
	}

	public boolean isMarked() {
		return marked;
	}

	public void setWidthRelative(boolean b) {
		widthIsRelative = b;
	}

	public boolean isWidthRelative() {
		return widthIsRelative;
	}

	public void setWidthRatio(float wr) {
		widthRatio = wr;
	}

	public float getWidthRatio() {
		return widthRatio;
	}

	public void setHeightRelative(boolean b) {
		heightIsRelative = b;
	}

	public boolean isHeightRelative() {
		return heightIsRelative;
	}

	public void setHeightRatio(float hr) {
		heightRatio = hr;
	}

	public float getHeightRatio() {
		return heightRatio;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setPage(Page p) {
		page = p;
	}

	public Page getPage() {
		return page;
	}

	public static PageTable create(Page page) {
		if (page == null)
			return null;
		PageTable table = new PageTable();
		table.setPreferredSize(new Dimension(400, 200));
		table.setColumnCount(4);
		table.setRowCount(4);
		if (maker == null) {
			maker = new PageTableMaker(table);
		}
		else {
			maker.setObject(table);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return table;
	}

	public void setPreferredSize(Dimension dim) {
		if (dim == null)
			throw new IllegalArgumentException("null object");
		if (dim.width == 0 || dim.height == 0)
			throw new IllegalArgumentException("zero dimension");
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
		table.setPreferredSize(new Dimension(dim.width, getTableBodyHeight()));
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {

		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);

		String s = Modeler.getInternationalText("RemoveThisRow");
		final JMenuItem miRemoveRow = new JMenuItem(s != null ? s : "Remove This Row");
		miRemoveRow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeSelectedRow();
			}
		});
		popupMenu.add(miRemoveRow);

		s = Modeler.getInternationalText("RemoveThisColumn");
		final JMenuItem miRemoveColumn = new JMenuItem(s != null ? s : "Remove This Column");
		miRemoveColumn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeSelectedColumn();
			}
		});
		popupMenu.add(miRemoveColumn);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("InsertARowAbove");
		final JMenuItem miInsertRowAbove = new JMenuItem(s != null ? s : "Insert a Row Above");
		miInsertRowAbove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				insertRowAbove();
			}
		});
		popupMenu.add(miInsertRowAbove);

		s = Modeler.getInternationalText("InsertARowBelow");
		final JMenuItem miInsertRowBelow = new JMenuItem(s != null ? s : "Insert a Row Below");
		miInsertRowBelow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				insertRowBelow();
			}
		});
		popupMenu.add(miInsertRowBelow);

		s = Modeler.getInternationalText("InsertAColumnLeft");
		final JMenuItem miInsertColumnLeft = new JMenuItem(s != null ? s : "Insert a Column to the Left");
		miInsertColumnLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				insertColumnLeft();
			}
		});
		popupMenu.add(miInsertColumnLeft);

		s = Modeler.getInternationalText("InsertAColumnRight");
		final JMenuItem miInsertColumnRight = new JMenuItem(s != null ? s : "Insert a Column to the Right");
		miInsertColumnRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				insertColumnRight();
			}
		});
		popupMenu.add(miInsertColumnRight);
		popupMenu.addSeparator();

		s = Modeler.getInternationalText("SetColumnsResizable");
		JMenuItem mi = new JMenuItem(s != null ? s : "Set Columns Resizable");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				relaxColumns();
			}
		});
		popupMenu.add(mi);

		s = Modeler.getInternationalText("Alignment");
		final JMenu alignMenu = new JMenu(s != null ? s : "Alignment");
		popupMenu.add(alignMenu);

		s = Modeler.getInternationalText("Left");
		mi = new JMenuItem(s != null ? s : "Left");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setCellAlignment(SwingConstants.LEFT);
			}
		});
		alignMenu.add(mi);

		s = Modeler.getInternationalText("Center");
		mi = new JMenuItem(s != null ? s : "Center");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setCellAlignment(SwingConstants.CENTER);
			}
		});
		alignMenu.add(mi);

		s = Modeler.getInternationalText("Right");
		mi = new JMenuItem(s != null ? s : "Right");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setCellAlignment(SwingConstants.RIGHT);
			}
		});
		alignMenu.add(mi);

		popupMenu.addSeparator();

		s = Modeler.getInternationalText("CustomizeThisTable");
		final JMenuItem miCustom = new JMenuItem((s != null ? s : "Customize This Table") + "...");
		miCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageTableMaker(PageTable.this);
				}
				else {
					maker.setObject(PageTable.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustom);

		s = Modeler.getInternationalText("RemoveThisTable");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Table");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageTable.this);
			}
		});
		popupMenu.add(miRemove);

		s = Modeler.getInternationalText("CopyThisTable");
		mi = new JMenuItem(s != null ? s : "Copy This Table");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageTable.this);
			}
		});
		popupMenu.add(mi);

		popupMenu.pack();
		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				alignMenu.setEnabled(isChangable());
				miCustom.setEnabled(isChangable());
				miRemove.setEnabled(isChangable());
				miRemoveRow.setEnabled(isChangable());
				miRemoveColumn.setEnabled(isChangable());
				miInsertRowAbove.setEnabled(isChangable());
				miInsertRowBelow.setEnabled(isChangable());
				miInsertColumnLeft.setEnabled(isChangable());
				miInsertColumnRight.setEnabled(isChangable());
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

	}

	public String toString() {

		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");

		if (hasTableColumnHeader())
			sb.append("<columnname>" + XMLCharacterEncoder.encode(getColumnNames()) + "</columnname>\n");
		if (hasTableRowHeader())
			sb.append("<rowname>" + XMLCharacterEncoder.encode(getRowNames()) + "</rowname>\n");

		int nrow = getRowCount();
		int ncol = getColumnCount();
		sb.append("<row>" + nrow + "</row>\n");
		sb.append("<column>" + ncol + "</column>\n");
		sb.append("<layout>" + getColumnWidths() + "</layout>\n");
		int align = getCellAlignment();
		if (align != SwingConstants.LEFT)
			sb.append("<cellalign>" + align + "</cellalign>\n");
		if (!table.isOpaque())
			sb.append("<opaque>false</opaque>\n");

		Dimension intercell = table.getIntercellSpacing();
		if (intercell.height != 10)
			sb.append("<rowmargin>" + intercell.height + "</rowmargin>\n");
		if (intercell.width != 10)
			sb.append("<columnmargin>" + intercell.width + "</columnmargin>\n");

		sb.append("<width>" + (widthIsRelative ? (widthRatio > 1.0f ? 1 : widthRatio) : getWidth()) + "</width>\n");
		sb.append("<height>" + (heightIsRelative ? (heightRatio > 1.0f ? 1 : heightRatio) : getHeight())
				+ "</height>\n");

		Color c = table.getGridColor();
		if (!c.equals(Color.black))
			sb.append("<fgcolor>" + Integer.toString(c.getRGB(), 16) + "</fgcolor>\n");

		c = table.getBackground();
		if (!c.equals(Color.white))
			sb.append("<bgcolor>" + Integer.toString(c.getRGB(), 16) + "</bgcolor>\n");

		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0]))
			sb.append("<border>" + getBorderType() + "</border>\n");

		sb.append("<hline>" + table.getShowHorizontalLines() + "</hline>\n");
		sb.append("<vline>" + table.getShowVerticalLines() + "</vline>\n");

		sb.append("<elementarray>");
		for (int i = 0; i < nrow; i++) {
			for (int j = 0; j < ncol; j++) {
				sb.append("<te>"
						+ (table.getValueAt(i, j) != null ? XMLCharacterEncoder.encode(table.getValueAt(i, j)
								.toString()) : "") + "</te>");
			}
		}
		sb.append("</elementarray>\n");

		return sb.toString();

	}

}