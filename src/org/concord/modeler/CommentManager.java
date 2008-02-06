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
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.HTMLPane;
import org.concord.modeler.util.SwingWorker;

class CommentManager {

	private final static Vector<String> COLUMN_NAMES = new Vector<String>();
	private JTable table;
	private static URL servletURL;
	private JDialog dialog;
	private JScrollPane scroller;
	private Page page;
	private JButton openButton;
	private JButton deleteButton;
	private JDialog viewDialog;
	private HTMLPane htmlPane;

	CommentManager(Page page) {
		this.page = page;
		COLUMN_NAMES.add("ID");
		COLUMN_NAMES.add("URL");
		COLUMN_NAMES.add("Title");
		COLUMN_NAMES.add("Author");
		COLUMN_NAMES.add("IP");
		COLUMN_NAMES.add("Time");
	}

	void showComments(final Frame owner) {
		new SwingWorker() {
			public Object construct() {
				return showComments();
			}

			public void finished() {
				String msg = get().toString();
				if (msg.toLowerCase().indexOf("fail") != -1 || msg.toLowerCase().indexOf("error") != -1) {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), msg, "Message from Server",
							JOptionPane.ERROR_MESSAGE);
				}
				else {
					createDialog(owner);
					dialog.setVisible(true);
				}
			}
		}.start();
	}

	private void createDialog(Frame owner) {

		if (dialog == null) {

			dialog = new JDialog(owner, true);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

			scroller = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroller.setPreferredSize(new Dimension(750, 400));
			dialog.getContentPane().add(scroller, BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

			openButton = new JButton("View");
			openButton.setToolTipText("View the selected comment");
			openButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					viewSelectedComment();
				}
			});
			buttonPanel.add(openButton);

			deleteButton = new JButton("Delete");
			deleteButton.setToolTipText("Delete the selected comment");
			deleteButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteSelectedComment();
				}
			});
			buttonPanel.add(deleteButton);

			JButton button = new JButton("Close");
			button.setToolTipText("Close this window");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dialog.dispose();
				}
			});
			buttonPanel.add(button);

			dialog.addWindowListener(new WindowAdapter() {
				public void windowActivated(WindowEvent e) {
					scroller.getViewport().setViewPosition(new Point(0, 0));
				}
			});

		}

		openButton.setEnabled(false);
		deleteButton.setEnabled(false);
		dialog.setTitle("Manage Recent Comments");
		dialog.pack();
		dialog.setLocationRelativeTo(owner);

	}

	private void viewSelectedComment() {
		if (table == null)
			return;
		final int row = table.getSelectedRow();
		if (row < 0)
			return;
		final int id = ((Integer) table.getValueAt(row, 0)).intValue();
		URL u = null;
		try {
			u = new URL(Modeler.getContextRoot() + "comment?client=mw&action=view&primarykey=" + id);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		URLConnection conn = ConnectionManager.getConnection(u);
		if (conn == null)
			return;
		InputStreamReader reader = null;
		StringBuffer sb = new StringBuffer();
		int c;
		try {
			reader = new InputStreamReader(conn.getInputStream());
			while ((c = reader.read()) != -1)
				sb.append((char) c);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException iox) {
				}
			}
		}
		viewComment(new String(sb));
	}

	private void viewComment(String text) {
		if (htmlPane == null) {
			htmlPane = new HTMLPane("text/html", text);
			htmlPane.setEditable(false);
			htmlPane.setPreferredSize(new Dimension(200, 100));
			htmlPane.setBorder(BorderFactory.createLoweredBevelBorder());
		}
		else {
			htmlPane.setText(text);
		}
		if (viewDialog == null) {
			viewDialog = new JDialog(JOptionPane.getFrameForComponent(page), "Comments", true);
			viewDialog.getContentPane().add(htmlPane, BorderLayout.CENTER);
			viewDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			JPanel p = new JPanel();
			viewDialog.getContentPane().add(p, BorderLayout.SOUTH);
			JButton b = new JButton("Close");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					viewDialog.dispose();
				}
			});
			p.add(b);
			b = new JButton("Delete");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteSelectedComment();
					viewDialog.dispose();
				}
			});
			p.add(b);
		}
		viewDialog.pack();
		viewDialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(page));
		viewDialog.setVisible(true);
	}

	private void deleteSelectedComment() {
		if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(page),
				"Do you really want to delete the selected comment?", "Deletion confirmation",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
			return;
		if (table == null)
			return;
		final int row = table.getSelectedRow();
		if (row < 0)
			return;
		int id = ((Integer) table.getValueAt(row, 0)).intValue();
		URL u = null;
		try {
			u = new URL(Modeler.getContextRoot() + "comment?client=mw&action=delete&primarykey=" + id);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		URLConnection conn = ConnectionManager.getConnection(u);
		if (conn == null)
			return;
		InputStreamReader reader = null;
		StringBuffer sb = new StringBuffer();
		int c;
		try {
			reader = new InputStreamReader(conn.getInputStream());
			while ((c = reader.read()) != -1)
				sb.append((char) c);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {
				}
			}
		}
		String s = new String(sb);
		if (s.length() > 0) {
			LogDumper.sharedInstance().dump(new Date(System.currentTimeMillis()) + " : <server message> " + s);
			if (s.indexOf("was deleted") != -1) {
				DefaultTableModel tm = (DefaultTableModel) table.getModel();
				tm.removeRow(row);
			}
		}
	}

	private String showComments() {

		if (servletURL == null) {
			try {
				servletURL = new URL(Modeler.getContextRoot() + "comment?client=mw&action=table");
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
				return "Error :" + e;
			}
		}

		URLConnection connect = ConnectionManager.getConnection(servletURL);
		if (connect == null)
			return "Error in connecting to " + servletURL;

		Vector data = null;
		ObjectInputStream in = null;
		String msg = null;
		try {
			in = new ObjectInputStream(connect.getInputStream());
			data = (Vector) in.readObject();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			msg = "Error :" + e;
		}
		catch (IOException e) {
			e.printStackTrace();
			msg = "Error :" + e;
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException iox) {
				}
			}
		}
		if (msg != null)
			return msg;

		if (table == null) {
			table = new JTable();
			table.setFont(new Font(null, Font.PLAIN, 10));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (table.getSelectedRow() >= 0) {
						openButton.setEnabled(true);
						deleteButton.setEnabled(true);
					}
				}
			});
			table.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() < 2)
						return;
					viewSelectedComment();
				}
			});
		}
		table.setModel(new DefaultTableModel(data, COLUMN_NAMES) {
			public boolean isCellEditable(int row, int col) {
				return false;
			}

			public void removeRow(int row) {
				super.removeRow(row);
				openButton.setEnabled(false);
				deleteButton.setEnabled(false);
			}
		});
		TableColumn tc = table.getColumnModel().getColumn(0);
		tc.setPreferredWidth(30);
		tc = table.getColumnModel().getColumn(1);
		tc.setPreferredWidth(200);

		return "";

	}

}