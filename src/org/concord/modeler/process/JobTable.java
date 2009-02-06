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

package org.concord.modeler.process;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

class JobTable {

	private static Icon clockIcon, taskIcon;
	private static ResourceBundle bundle;
	private static boolean isUSLocale = Locale.getDefault().equals(Locale.US);
	private JTable table;
	private Vector<Vector<Object>> rowData;
	private JTextArea note;
	private Job job;
	private TaskCreator taskCreator;
	private JButton removeTaskButton, editTaskButton;

	JobTable(Job job) {
		this.job = job;
		rowData = new Vector<Vector<Object>>();
		if (bundle == null && !isUSLocale) {
			try {
				bundle = ResourceBundle.getBundle("org.concord.modeler.process.images.JobBundle", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}
	}

	static String getInternationalText(String s) {
		if (bundle == null)
			return null;
		if (isUSLocale)
			return null;
		if (s == null)
			return null;
		String t = null;
		try {
			t = bundle.getString(s);
		}
		catch (MissingResourceException e) {
			t = null;
		}
		return t;
	}

	/* create a dialog box showing the subtasks */
	void show(Component owner) {
		createTasks(owner).setVisible(true);
	}

	/* create a dialog box showing the subtasks with the specified task selected to edit. */
	void show(Component owner, String s) {
		JDialog dialog = createTasks(owner);
		int n = table.getRowCount();
		String name;
		for (int i = 0; i < n; i++) {
			name = (String) table.getValueAt(i, 1);
			if (name != null && name.equals(s)) {
				table.setRowSelectionInterval(i, i);
				table.setEditingColumn(3);
				table.setEditingRow(i);
			}
		}
		dialog.setVisible(true);
	}

	private void initIcons() {
		if (clockIcon == null) {
			clockIcon = new ImageIcon(Job.class.getResource("images/clock.gif"));
			taskIcon = new ImageIcon(Job.class.getResource("images/Subtask.gif"));
		}
	}

	void insertRow(Loadable l) {
		if (l == null)
			return;
		initIcons();
		Vector<Object> v = new Vector<Object>();
		v.add(l.getLifetime() == Loadable.ETERNAL ? taskIcon : clockIcon);
		v.add(l.getName());
		v.add(Integer.toString(l.getPriority()));
		v.add(Integer.toString(l.getInterval()));
		v.add(l.getLifetime() == Loadable.ETERNAL ? "Permanent" : Integer.toString(l.getLifetime()));
		rowData.add(v);
		if (table != null) {
			table.revalidate();
			table.repaint();
		}
	}

	void removeRow(Loadable l) {
		if (l == null)
			return;
		for (Vector v : rowData) {
			if (v.elementAt(1).equals(l.getName())) {
				rowData.remove(v);
				l.setCompleted(false);
				if (table != null)
					table.repaint();
				break;
			}
		}
	}

	private JDialog createTasks(Component owner) {

		job.processPendingRequests();

		String s = getInternationalText("TaskManager");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(owner), s != null ? s : "Task Manager",
				false);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panel.setPreferredSize(new Dimension(450, 320));
		dialog.setContentPane(panel);

		Vector<String> columnNames = new Vector<String>();
		columnNames.add("");
		s = getInternationalText("Task");
		columnNames.add(s != null ? s : "Task");
		s = getInternationalText("Priority");
		columnNames.add(s != null ? s : "Priority");
		s = getInternationalText("Interval");
		columnNames.add(s != null ? s : "Interval");
		s = getInternationalText("Lifetime");
		columnNames.add(s != null ? s : "Lifetime");

		table = new JTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setSelectionBackground(Color.lightGray);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setPreferredSize(new Dimension(200, 18));
		table.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
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
				if (j != 3 && e.getClickCount() >= 2) {
					editTaskButton.doClick();
				}
			}
		});

		rowData.clear();
		for (Loadable l : job.taskPool) {
			insertRow(l);
		}

		DefaultTableModel tm = new DefaultTableModel(rowData, columnNames) {
			public Class<?> getColumnClass(int columnIndex) {
				return getValueAt(0, columnIndex).getClass();
			}

			public boolean isCellEditable(int row, int col) {
				if (col < 3)
					return false;
				if ("Permanent".equals(getValueAt(row, col)))
					return false;
				return true;
			}
		};
		table.setModel(tm);
		tm.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				DefaultTableModel t = (DefaultTableModel) e.getSource();
				int col = e.getColumn();
				int row = e.getFirstRow();
				Loadable l = job.getTask((String) t.getValueAt(row, 1));
				if (col == 3) {
					String s = (String) t.getValueAt(row, col);
					if (s != null)
						l.setInterval(Integer.parseInt(s));
				}
				else if (col == 4) {
					String s = (String) t.getValueAt(row, col);
					l.setLifetime("Permanent".equals(s) ? Loadable.ETERNAL : Integer.parseInt(s));
				}
			}
		});

		ListSelectionModel lsm = table.getSelectionModel();
		lsm.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				ListSelectionModel sm = (ListSelectionModel) e.getSource();
				if (!sm.isSelectionEmpty()) {
					int row = sm.getMinSelectionIndex();
					Loadable l = job.getTask((String) table.getValueAt(row, 1));
					note.setText(l.getDescription());
					boolean b = !l.isSystemTask();
					removeTaskButton.setEnabled(b);
					editTaskButton.setEnabled(b);
				}
			}
		});

		table.getColumnModel().getColumn(0).setMaxWidth(24);
		table.getColumnModel().getColumn(1).setMinWidth(160);
		table.setShowGrid(false);
		table.setRowHeight(24);
		table.setRowMargin(2);
		table.setColumnSelectionAllowed(false);
		table.setBackground(Color.white);

		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(300, 150));
		scroll.getViewport().setBackground(Color.white);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setTopComponent(scroll);
		splitPane.setPreferredSize(new Dimension(300, 230));
		splitPane.setOneTouchExpandable(true);

		panel.add(splitPane, BorderLayout.NORTH);

		note = new JTextArea("Display description of a task.");
		note.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		note.setEditable(false);
		note.setFont(new Font("Verdana", Font.PLAIN, 9));
		scroll = new JScrollPane(note);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		splitPane.setBottomComponent(scroll);

		JPanel p = new JPanel(new BorderLayout(0, 10));

		JPanel p1 = new JPanel(new BorderLayout());
		s = getInternationalText("ChangeIntervalOfSelectedTask");
		p1.add(new JLabel("<html><font size=2>"
				+ (s != null ? s : "To change the interval of the selected system task:") + "</font></html>"),
				BorderLayout.CENTER);
		s = getInternationalText("DoubleClickIntervalCellChangeNumberAndHitEnter");
		p1.add(new JLabel("<html><font size=2>"
				+ (s != null ? s
						: "Double-click the <i>Interval</i> Cell, change the number, and press ENTER to verify it.")
				+ "</font></html>"), BorderLayout.SOUTH);
		p.add(p1, BorderLayout.CENTER);

		p1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		p.add(p1, BorderLayout.SOUTH);

		JButton button;

		s = getInternationalText("AddTask");
		button = new JButton(s != null ? s : "Add Task");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (taskCreator == null)
					taskCreator = new TaskCreator(job);
				taskCreator.show(table, null);
			}
		});
		p1.add(button);

		s = getInternationalText("RemoveTask");
		removeTaskButton = new JButton(s != null ? s : "Remove Task");
		removeTaskButton.setEnabled(false);
		removeTaskButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ListSelectionModel sm = table.getSelectionModel();
				if (!sm.isSelectionEmpty()) {
					int row = sm.getMinSelectionIndex();
					Loadable l = job.getTask((String) table.getValueAt(row, 1));
					if (l != null) {
						job.remove(l);
						job.processPendingRequests();
					}
				}
			}
		});
		p1.add(removeTaskButton);

		s = getInternationalText("EditTask");
		editTaskButton = new JButton(s != null ? s : "Edit Task");
		editTaskButton.setEnabled(false);
		editTaskButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ListSelectionModel sm = table.getSelectionModel();
				if (!sm.isSelectionEmpty()) {
					int row = sm.getMinSelectionIndex();
					Loadable l = job.getTask((String) table.getValueAt(row, 1));
					if (l != null) {
						if (l.isSystemTask())
							return;
						if (taskCreator == null)
							taskCreator = new TaskCreator(job);
						taskCreator.show(table, l);
						note.setText(l.getDescription());
						table.setValueAt(l.getLifetime() == Loadable.ETERNAL ? taskIcon : clockIcon, row, 0);
						table.setValueAt(l.getName(), row, 1);
						table.setValueAt(l.getPriority() + "", row, 2);
						table.setValueAt(l.getInterval() + "", row, 3);
						table.setValueAt(l.getLifetime() == Loadable.ETERNAL ? "Permanent" : l.getLifetime() + "", row,
								4);
						table.repaint();
					}
				}
			}
		});
		p1.add(editTaskButton);

		s = getInternationalText("Close");
		button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		p1.add(button);

		panel.add(p, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(owner);

		return dialog;

	}

}