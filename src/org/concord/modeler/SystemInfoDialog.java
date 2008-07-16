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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;

class SystemInfoDialog extends JDialog {

	SystemInfoDialog(Modeler modeler) {

		super(modeler, "System Information", false);
		String s = Modeler.getInternationalText("SystemInfo");
		if (s != null)
			setTitle(s);

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(panel, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SystemInfoDialog.this.dispose();
			}
		});
		panel.add(button);

		JTabbedPane tabbedPane = new JTabbedPane();
		getContentPane().add(BorderLayout.CENTER, tabbedPane);

		// JVM information
		s = Modeler.getInternationalText("VirtualMachine");
		tabbedPane.addTab("Java " + (s != null ? s : "Virtual Machine"), createJvmPanel());

		// MW information
		s = Modeler.getInternationalText("MolecularWorkbench");
		tabbedPane.addTab(s != null ? s : "Molecular Workbench", createMwPanel());

		// threads
		s = Modeler.getInternationalText("Thread");
		tabbedPane.addTab(s != null ? s : "Threads", createThreadPanel());

		pack();
		setLocationRelativeTo(modeler);

	}

	private JPanel createJvmPanel() {

		JPanel total = new JPanel(new BorderLayout());
		total.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel panel = new JPanel(new BorderLayout());
		total.add(panel, BorderLayout.CENTER);

		DefaultTableModel tableModel = new DefaultTableModel() {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		String[] columnNames = { " ", " " };
		String[][] rowData = new String[7][2];

		// row 1
		String s = Modeler.getInternationalText("Vendor");
		rowData[0][0] = (s != null ? s : "Vendor") + ":";
		rowData[0][1] = System.getProperty("java.vendor");

		// row 2
		s = Modeler.getInternationalText("Version");
		rowData[1][0] = (s != null ? s : "Version") + ":";
		rowData[1][1] = System.getProperty("java.version");

		// row 3
		s = Modeler.getInternationalText("Location");
		rowData[2][0] = (s != null ? s : "Location") + ":";
		rowData[2][1] = System.getProperty("java.home");

		Runtime rt = Runtime.getRuntime();

		// row 4
		s = Modeler.getInternationalText("AvailableProcessors");
		rowData[3][0] = (s != null ? s : "Number of Processors") + ":";
		rowData[3][1] = "" + rt.availableProcessors();

		// row 5
		s = Modeler.getInternationalText("MaximumMemoryAllocated");
		rowData[4][0] = (s != null ? s : "Maximum Memory Allocated") + ":";
		rowData[4][1] = Math.round(rt.maxMemory() / 1048576.f) + " MB";

		// row 6
		s = Modeler.getInternationalText("TotalMemoryUsed");
		rowData[5][0] = (s != null ? s : "Total Memory Used") + ":";
		rowData[5][1] = Math.round(rt.totalMemory() / 1048576.f) + " MB";

		// row 7
		s = Modeler.getInternationalText("FreeMemory");
		rowData[6][0] = (s != null ? s : "Free Memory") + ":";
		rowData[6][1] = Math.round(rt.freeMemory() / 1048576.f) + " MB";

		tableModel.setDataVector(rowData, columnNames);
		JTable table = new JTable(tableModel);
		table.setBorder(BorderFactory.createLineBorder(table.getGridColor()));
		table.setRowMargin(10);
		table.setRowHeight(table.getRowHeight() + 5);
		((DefaultTableColumnModel) table.getColumnModel()).setColumnMargin(10);
		table.getColumnModel().getColumn(0).setPreferredWidth(150);
		table.getColumnModel().getColumn(1).setPreferredWidth(250);
		JScrollPane sp = new JScrollPane(table);
		sp.setPreferredSize(new Dimension(400, 300));
		panel.add(sp, BorderLayout.NORTH);

		return total;

	}

	private JPanel createMwPanel() {

		Date date = null;
		try {
			ResourceBundle bundle = ResourceBundle.getBundle("org.concord.modeler.properties.build", Locale.US);
			date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US).parse(
					bundle.getString("timestamp"));
		}
		catch (Exception e) {
			e.printStackTrace();
			date = null;
		}

		JPanel total = new JPanel(new BorderLayout());
		total.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel panel = new JPanel(new BorderLayout(8, 8));
		total.add(panel, BorderLayout.CENTER);
		DefaultTableModel tableModel = new DefaultTableModel() {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		String[] columnNames = { " ", " " };
		Object[][] rowData = new Object[4][2];
		// row 1
		String s = Modeler.getInternationalText("ReleaseTime");
		rowData[0][0] = (s != null ? s : "Release Time") + ":";
		rowData[0][1] = date != null ? date.toString() : "Unknown";
		// row 2
		s = Modeler.getInternationalText("Vendor");
		rowData[1][0] = (s != null ? s : "Vendor") + ":";
		rowData[1][1] = "Concord Consortium, Inc.";
		// row 3
		s = Modeler.getInternationalText("License");
		rowData[2][0] = (s != null ? s : "License") + ":";
		rowData[2][1] = "GNU General Public License (v.2 or later)";
		// row 4
		s = Modeler.getInternationalText("JarLocation");
		rowData[3][0] = (s != null ? s : "Jar Location") + ":";
		rowData[3][1] = System.getProperty("java.class.path");
		tableModel.setDataVector(rowData, columnNames);
		JTable table = new JTable(tableModel);
		table.setBorder(BorderFactory.createLineBorder(table.getGridColor()));
		table.setRowMargin(10);
		table.setRowHeight(table.getRowHeight() + 5);
		((DefaultTableColumnModel) table.getColumnModel()).setColumnMargin(10);
		table.getColumnModel().getColumn(0).setPreferredWidth(150);
		table.getColumnModel().getColumn(1).setPreferredWidth(250);
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder("General Information:"));
		p.add(table, BorderLayout.CENTER);
		panel.add(p, BorderLayout.NORTH);

		int n = InstancePool.sharedInstance().getSize();
		if (n > 0) {
			tableModel = new DefaultTableModel() {
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			columnNames = new String[] { "Instance", "Status" };
			rowData = new Object[n][2];
			String[][] t = InstancePool.sharedInstance().getSnapshotInfo();
			for (int i = 0; i < n; i++) {
				rowData[i][0] = t[i][0];
				rowData[i][1] = t[i][1];
			}
			tableModel.setDataVector(rowData, columnNames);
			table = new JTable(tableModel);
			table.setBorder(BorderFactory.createLineBorder(table.getGridColor()));
			table.setRowMargin(10);
			table.setRowHeight(table.getRowHeight() + 5);
			((DefaultTableColumnModel) table.getColumnModel()).setColumnMargin(10);
			table.getColumnModel().getColumn(0).setPreferredWidth(250);
			table.getColumnModel().getColumn(1).setPreferredWidth(150);
			JScrollPane sp = new JScrollPane(table);
			sp.setPreferredSize(new Dimension(400, 200));
			p = new JPanel(new BorderLayout());
			s = Modeler.getInternationalText("InstancePool");
			p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Instance Pool:"));
			p.add(sp, BorderLayout.CENTER);
			panel.add(p, BorderLayout.CENTER);
		}

		return total;

	}

	private JPanel createThreadPanel() {

		JPanel total = new JPanel(new BorderLayout());
		total.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel panel = new JPanel(new BorderLayout());
		total.add(panel, BorderLayout.CENTER);

		final DefaultTableModel threadTableModel = new DefaultTableModel() {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		updateThreadTableModel(threadTableModel);
		JTable table = new JTable(threadTableModel);
		table.setBorder(BorderFactory.createLineBorder(table.getGridColor()));
		table.setRowMargin(10);
		table.setRowHeight(table.getRowHeight() + 5);
		((DefaultTableColumnModel) table.getColumnModel()).setColumnMargin(10);
		table.getColumnModel().getColumn(0).setPreferredWidth(250);
		JScrollPane sp = new JScrollPane(table);
		sp.setPreferredSize(new Dimension(400, 300));
		panel.add(sp, BorderLayout.NORTH);

		JPanel buttonPanel = new JPanel();
		panel.add(buttonPanel, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("Update");
		JButton button = new JButton(s != null ? s : "Update");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateThreadTableModel(threadTableModel);
			}
		});
		buttonPanel.add(button);

		return total;

	}

	private void updateThreadTableModel(DefaultTableModel threadTableModel) {
		Thread[] list = new Thread[Thread.activeCount()];
		int n2 = Thread.enumerate(list);
		String[] columnNames = { "Name", "Priority" };
		String s = Modeler.getInternationalText("Name");
		if (s != null)
			columnNames[0] = s;
		s = Modeler.getInternationalText("Priority");
		if (s != null)
			columnNames[1] = s;
		String[][] rowData = new String[n2][2];
		for (int i = 0; i < n2; i++) {
			rowData[i][0] = list[i].getName();
			rowData[i][1] = list[i].getPriority() + (list[i].isDaemon() ? " (Daemon)" : "");
		}
		threadTableModel.setDataVector(rowData, columnNames);
	}

}