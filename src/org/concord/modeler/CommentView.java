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
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.concord.modeler.ui.HTMLPane;
import org.concord.modeler.util.SwingWorker;

class CommentView extends JComponent {

	private HTMLPane displayPane;
	private String address;
	private String title;
	private String comments;
	private JDialog dialog;
	private JScrollPane scroller;

	CommentView() {
		displayPane = new HTMLPane("text/html", "There is currently no comment on this page.");
		displayPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		displayPane.setEditable(false);
		displayPane.setPreferredSize(new Dimension(700, 450));
	}

	void showComments(String s, String title, final Frame owner) {

		address = s;
		this.title = title;

		new SwingWorker() {
			public Object construct() {
				return getComments();
			}

			public void finished() {
				String msg = get().toString();
				if (msg.toLowerCase().indexOf("fail") != -1 || msg.toLowerCase().indexOf("error") != -1) {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CommentView.this), msg,
							"Message from Server", JOptionPane.ERROR_MESSAGE);
				}
				else {
					createDialog(owner);
					dialog.setVisible(true);
				}
			}
		}.start();

	}

	private void createDialog(Frame owner) {

		displayPane.setText(comments);

		if (dialog == null) {

			dialog = new JDialog(owner, true);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

			scroller = new JScrollPane(displayPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			dialog.getContentPane().add(scroller, BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

			String s = Modeler.getInternationalText("CloseButton");
			JButton button = new JButton(s != null ? s : "Close");
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

		String s = Modeler.getInternationalText("Comment");
		dialog.setTitle(title + ": " + (s != null ? s : "Comments"));
		dialog.pack();
		dialog.setLocationRelativeTo(owner);

	}

	private String getComments() {

		URL servletURL = null;
		try {
			servletURL = new URL(Modeler.getContextRoot() + "comment?address=" + URLEncoder.encode(address, "UTF-8"));
		}
		catch (Exception e) {
			e.printStackTrace();
			return "Error :" + e;
		}

		HttpURLConnection connect = ConnectionManager.getConnection(servletURL);
		if (connect == null)
			return "Error: can't connect to " + servletURL;

		String msg = null;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
		}
		catch (IOException e) {
			e.printStackTrace();
			msg = "Error :" + e;
		}
		if (msg != null)
			return msg;

		comments = "";
		String inputLine;
		try {
			while ((inputLine = in.readLine()) != null)
				comments += inputLine;
			msg = comments;
		}
		catch (IOException e) {
			e.printStackTrace();
			msg = "Error :" + e;
		}
		finally {
			try {
				in.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		String s1 = "There is currently no discussion about this page.";
		if (comments.indexOf(s1) != -1) {
			String s2 = Modeler.getInternationalText("NoCommentOnThisPage");
			if (s2 != null)
				comments = "<html><body>" + s2 + "</body></html>";
		}

		return msg;

	}

}