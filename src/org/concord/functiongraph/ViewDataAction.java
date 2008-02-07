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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * @author Charles Xie
 * 
 */
class ViewDataAction extends AbstractAction {

	private Graph graph;

	ViewDataAction(Graph graph) {
		super();
		this.graph = graph;
		putValue(NAME, "View Data");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
	}

	public String toString() {
		return (String) getValue(NAME);
	}

	public void actionPerformed(ActionEvent event) {

		if (graph.data.isEmpty()) {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(graph), "No data to view.", "No Data",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JOptionPane op = new JOptionPane();
		op.setLayout(new BorderLayout());
		String s = Graph.getInternationalText("DataView");
		final JDialog dialog = op.createDialog(JOptionPane.getFrameForComponent(graph), s != null ? s : "Data View");
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				graph.deselectAllPoints();
				dialog.dispose();
			}
		});

		final JTable table = new JTable();

		if (graph.data.size() > 1) {
			// if there are more than one data sources, put them in a JComboBox

			final JComboBox comboBox = new JComboBox(graph.data.toArray());
			comboBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					DataSource ds = (DataSource) comboBox.getSelectedItem();
					if (ds == null)
						return;
					ds.populateTable();
					table.setModel(ds.getTableModel());
					table.repaint();
					graph.deselectAllPoints();
				}
			});
			DataSource ds = (DataSource) comboBox.getSelectedItem();
			ds.populateTable();
			table.setModel(ds.getTableModel());
			final ListSelectionModel selectionModel = new DefaultListSelectionModel();
			selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setSelectionModel(selectionModel);
			selectionModel.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (selectionModel.isSelectionEmpty())
						return;
					int row = table.getSelectedRow();
					DataSource ds1 = (DataSource) comboBox.getSelectedItem();
					ds1.setShowSelectedPoint(true);
					try {
						ds1.setSelectedX(Float.parseFloat((String) table.getModel().getValueAt(row, 1)));
						ds1.setSelectedY(Float.parseFloat((String) table.getModel().getValueAt(row, 2)));
						graph.repaint();
					}
					catch (Exception exception) {
						exception.printStackTrace(System.err);
					}
				}
			});
			table.setRowSelectionInterval(0, 0);

			JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
			s = Graph.getInternationalText("ChooseDataSource");
			JLabel label = new JLabel(s != null ? s : "Choose a data source: ");
			p.add(label);
			comboBox.setPreferredSize(new Dimension(100, 20));
			p.add(comboBox);
			op.add(p, BorderLayout.NORTH);

		}
		else {

			DataSource ds = graph.data.get(0);
			ds.populateTable();
			table.setModel(ds.getTableModel());
			final ListSelectionModel selectionModel = new DefaultListSelectionModel();
			selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setSelectionModel(selectionModel);
			selectionModel.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (selectionModel.isSelectionEmpty())
						return;
					int row = table.getSelectedRow();
					DataSource ds1 = graph.data.get(0);
					ds1.setShowSelectedPoint(true);
					try {
						ds1.setSelectedX(Float.parseFloat((String) table.getModel().getValueAt(row, 1)));
						ds1.setSelectedY(Float.parseFloat((String) table.getModel().getValueAt(row, 2)));
						graph.repaint();
					}
					catch (Exception exception) {
						exception.printStackTrace(System.err);
					}
				}
			});

			JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JLabel label = new JLabel();
			p.add(label);
			String tableColumnNames[] = ds.getTableColumnNames();
			if (tableColumnNames != null && tableColumnNames[1] != null && !tableColumnNames[1].equals("")) {
				label.setText(tableColumnNames[1]);
			}
			else {
				label.setText(ds.getExpression());
			}
			op.add(p, BorderLayout.NORTH);

		}

		table.setRowSelectionInterval(0, 0);

		JPanel panel = new JPanel();
		s = Graph.getInternationalText("Close");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graph.deselectAllPoints();
				dialog.dispose();
			}
		});
		panel.add(button);
		op.add(panel, BorderLayout.SOUTH);

		JScrollPane scrollPane = new JScrollPane(table);
		table.setPreferredScrollableViewportSize(new Dimension(150, 150));
		op.add(scrollPane, BorderLayout.CENTER);

		dialog.pack();
		dialog.setLocationRelativeTo(graph);
		dialog.setVisible(true);

	}

}