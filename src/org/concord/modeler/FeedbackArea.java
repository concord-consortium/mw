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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URLConnection;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.concord.modeler.event.CommentEvent;
import org.concord.modeler.event.CommentListener;
import org.concord.modeler.ui.TextBox;
import org.concord.modeler.util.SwingWorker;

class FeedbackArea extends JComponent {

	protected CommentInputPane inputArea;
	protected TextBox displayPane;

	private MouseWheelListener mouseWheelListener;
	private JScrollPane scroller;
	private String comments;

	public FeedbackArea() {

		setLayout(new BorderLayout());
		setOpaque(false);

		JPanel p = new JPanel(new BorderLayout(5, 5));
		p.setOpaque(false);
		add(p, BorderLayout.CENTER);

		displayPane = new TextBox("Loading......");
		displayPane.setEditable(false);

		scroller = new JScrollPane(displayPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setOpaque(false);
		scroller.setBorder(BorderFactory.createEmptyBorder());
		p.add(scroller, BorderLayout.CENTER);

		try {
			mouseWheelListener = scroller.getMouseWheelListeners()[0];
		}
		catch (Throwable t) {
			// ignore
		}
		removeScrollerMouseWheelListener();

		displayPane.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (scroller.getVerticalScrollBar().isShowing() || scroller.getHorizontalScrollBar().isShowing())
					addScrollerMouseWheelListener();
			}

			public void focusLost(FocusEvent e) {
				removeScrollerMouseWheelListener();
			}
		});

		JPanel buttonPanel = new JPanel();
		p.add(buttonPanel, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("Update");
		JButton button = new JButton(s != null ? s : "Update");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateData();
			}
		});
		buttonPanel.add(button);

		s = Modeler.getInternationalText("SortBy");
		JLabel label = new JLabel((s != null ? s : "Sort by") + ":");
		buttonPanel.add(label);

		s = Modeler.getInternationalText("Date");
		JComboBox comboBox = new JComboBox(new String[] { s != null ? s : "Date" });
		buttonPanel.add(comboBox);

		ButtonGroup bg = new ButtonGroup();

		s = Modeler.getInternationalText("Ascending");
		JRadioButton rb = new JRadioButton(s != null ? s : "Ascending");
		rb.setEnabled(false);
		buttonPanel.add(rb);
		bg.add(rb);

		s = Modeler.getInternationalText("Descending");
		rb = new JRadioButton(s != null ? s : "Descending");
		rb.setSelected(true);
		buttonPanel.add(rb);
		bg.add(rb);

		inputArea = new CommentInputPane(false);
		inputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		inputArea.addCommentListener(new CommentListener() {
			public void commentSubmitted(CommentEvent e) {
				updateData();
			}

			public void commentCanceled(CommentEvent e) {
			}
		});
		add(inputArea, BorderLayout.SOUTH);

	}

	public void setUser(Person user) {
		inputArea.setUser(user);
	}

	private void removeScrollerMouseWheelListener() {
		if (mouseWheelListener != null)
			scroller.removeMouseWheelListener(mouseWheelListener);
	}

	private void addScrollerMouseWheelListener() {
		if (mouseWheelListener != null)
			scroller.addMouseWheelListener(mouseWheelListener);
	}

	public void setOpaque(boolean b) {
		super.setOpaque(b);
		if (displayPane != null)
			displayPane.setOpaque(b);
		if (scroller != null)
			scroller.getViewport().setOpaque(b);
	}

	public void updateData() {
		new SwingWorker("Update Comments") {
			public Object construct() {
				return getData();
			}

			public void finished() {
				String s = (String) get();
				if (!s.startsWith("Error:")) {
					displayPane.setText(comments);
					displayPane.getTextComponent().setCaretPosition(0); // scroll back to the top
				}
				else {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(FeedbackArea.this), s,
							"Error Message", JOptionPane.ERROR_MESSAGE);
				}
			}
		}.start();
	}

	private String getData() {

		if (inputArea.getServletURL() == null)
			return "Error: No servlet address";

		String s = inputArea.getServletURL() + "?address=" + inputArea.getPageAddress();
		URLConnection connect = ConnectionManager.getConnection(s);
		if (connect == null)
			return "Error in connecting to " + inputArea.getServletURL();

		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			comments = "";
			String inputLine;
			while ((inputLine = in.readLine()) != null)
				comments += inputLine;
		}
		catch (IOException e) {
			e.printStackTrace();
			return "Error :" + e;
		}
		finally {
			if (in != null)
				try {
					in.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
		}
		return "";

	}

}