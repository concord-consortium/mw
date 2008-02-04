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
package org.concord.modeler.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * @author Charles Xie
 * 
 */
public class QueueGroupTable extends JComponent {

	private static ImageIcon icon;
	private JTable table;
	private Vector<String> columnNames;
	private Vector<Vector> rowData;
	private QueueGroup qGroup;

	public Insets getInsets() {
		return new Insets(10, 10, 10, 10);
	}

	public void updateData() {
		if (table == null)
			throw new NullPointerException("Table hasn't been initialized.");
		if (rowData.isEmpty())
			return;
		int irow = 0;
		DataQueue q = null;
		synchronized (qGroup.getSynchronizedLock()) {
			for (Iterator iterator = qGroup.iterator(); iterator.hasNext();) {
				q = (DataQueue) iterator.next();
				table.setValueAt(Integer.toString(q.getInterval()), irow, 2);
				table.setValueAt(Integer.toString(q.getLength()), irow, 3);
				irow++;
			}
		}
		table.repaint();
	}

	QueueGroupTable(QueueGroup g) {

		if (icon == null)
			icon = new ImageIcon(QueueGroupTable.class.getResource("images/TimeSeries.gif"));

		qGroup = g;
		table = new JTable();

		setLayout(new BorderLayout(0, 10));
		setBorder(BorderFactory.createEtchedBorder());

		columnNames = new Vector<String>();
		columnNames.add("");
		columnNames.add("Title");
		columnNames.add("Interval");
		columnNames.add("Length");

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);

		table.setShowGrid(false);
		table.setRowHeight(20);
		table.setRowMargin(2);
		table.setColumnSelectionAllowed(false);
		table.setBackground(Color.white);
		table.getTableHeader().setPreferredSize(new Dimension(200, 18));
		table.doLayout();

		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(300, 150));
		scroll.getViewport().setBackground(Color.white);

		this.add(scroll, BorderLayout.CENTER);

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

		table.getColumnModel().getColumn(0).setMaxWidth(24);
		table.getColumnModel().getColumn(1).setMinWidth(150);

		table.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				int nrow = table.getModel().getRowCount();
				int ncol = table.getModel().getColumnCount();
				Rectangle r;
				int i = 0, j = 0;
				search: for (i = 0; i < nrow; i++) {
					for (j = 0; j < ncol; j++) {
						r = table.getCellRect(i, j, true);
						if (r.contains(x, y)) {
							break search;
						}
					}
				}
				if (e.getClickCount() >= 2) {
					DataQueueUtilities.show((DataQueue) qGroup.get(i), JOptionPane
							.getFrameForComponent(QueueGroupTable.this));
				}
			}
		});

	}

	void fill() {
		synchronized (qGroup) {
			for (Iterator iterator = qGroup.iterator(); iterator.hasNext();) {
				insertRow((DataQueue) iterator.next());
			}
		}
	}

	QueueGroup getQueueGroup() {
		return qGroup;
	}

	public void insertRow(DataQueue q) {
		rowData.add(createRow(q));
		table.revalidate();
		table.repaint();
	}

	public void insertRow(int index, DataQueue q) {
		rowData.add(index, createRow(q));
		table.revalidate();
		table.repaint();
	}

	public void setRow(int index, DataQueue q) {
		rowData.set(index, createRow(q));
		table.revalidate();
		table.repaint();
	}

	public void insertRows(QueueGroup qg) {
		rowData.addAll(createRows(qg));
		table.revalidate();
		table.repaint();
	}

	public void insertRows(int index, QueueGroup qg) {
		rowData.addAll(index, createRows(qg));
		table.revalidate();
		table.repaint();
	}

	public void removeRow(DataQueue q) {
		for (Vector row : rowData) {
			if (row.elementAt(1).equals(q.getName())) {
				rowData.remove(row);
				break;
			}
		}
		table.revalidate();
		table.repaint();
	}

	public void clear() {
		rowData.clear();
		table.revalidate();
		table.repaint();
	}

	public int getRowCount() {
		if (table == null)
			return 0;
		return table.getRowCount();
	}

	private Vector<Vector> createRows(QueueGroup qg) {
		Vector<Vector> v = new Vector<Vector>();
		synchronized (qg.getSynchronizedLock()) {
			for (Iterator iterator = qg.iterator(); iterator.hasNext();) {
				v.add(createRow((DataQueue) iterator.next()));
			}
		}
		return v;
	}

	private Vector createRow(DataQueue q) {
		Vector<Object> row = new Vector<Object>();
		row.add(icon);
		row.add(q.getName());
		row.add(Integer.toString(q.getInterval()));
		row.add(Integer.toString(q.getLength()));
		return row;
	}

}