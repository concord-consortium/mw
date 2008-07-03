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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.concord.modeler.event.CommentEvent;
import org.concord.modeler.event.CommentListener;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.SwingWorker;

class CommentInputPane extends JPanel {

	private String address;
	private URL servletURL;
	private Person user;
	private int statusCode;
	private Runnable registerAction;

	private List<CommentListener> commentListeners;

	private JTextField userField;
	private JPasswordField passwordField;
	private JTextField titleField;
	private JTextArea textArea;

	CommentInputPane(final boolean popup) {

		super(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		Dimension mediumField = new Dimension(120, 20);
		Dimension longField = new Dimension(240, 20);

		// Spacing between the label and the field
		Border border = BorderFactory.createEmptyBorder(0, 0, 0, 10);
		Border border1 = BorderFactory.createEmptyBorder(0, 20, 0, 10);

		// add some space around all my components to avoid cluttering
		c.insets = new Insets(2, 2, 2, 2);

		// anchors all my components to the west
		c.anchor = GridBagConstraints.WEST;

		// Short description label and field
		String s = Modeler.getInternationalText("CommentTitle");
		JLabel label = new JLabel(s != null ? s : "Title");
		label.setBorder(border); // add some space on the right
		add(label, c);
		titleField = new PastableTextField();
		titleField.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0; // use all available horizontal space
		c.gridwidth = 3; // spans across 3 columns
		c.fill = GridBagConstraints.HORIZONTAL; // fills up the 3 columns
		add(titleField, c);

		// Description label and field
		s = Modeler.getInternationalText("CommentBody");
		label = new JLabel(
				s != null ? s
						: "<html><body>Comment<br><font size=\"-2\">(Plain or HTML text.<br>Don't enclose in<br>a pair of &lt;html&gt;<br>and &lt;/html&gt; tags.)</font></body></html>");
		label.setBorder(border);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.NORTH;
		add(label, c);
		textArea = new PastableTextArea();
		JScrollPane scroller = new JScrollPane(textArea);
		Dimension dim = new Dimension(360, 120);
		scroller.setMinimumSize(dim);
		scroller.setPreferredSize(dim);
		c.gridx = 1;
		c.weightx = 1.0; // use all available horizontal space
		c.weighty = 1.0; // use all available vertical space
		c.gridwidth = 3; // spans across 3 columns
		c.gridheight = 1; // spans across 2 rows
		c.fill = GridBagConstraints.HORIZONTAL; // fills up the cols & rows
		add(scroller, c);

		if (!popup) {

			// User ID and password
			s = Modeler.getInternationalText("AccountID");
			label = new JLabel(s != null ? s : "User ID");
			label.setBorder(border);
			c.gridx = 0;
			c.gridy = 3;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.weightx = 1;
			c.weighty = 0;
			add(label, c);
			userField = new JTextField(user == null ? null : user.getUserID());
			userField.setPreferredSize(mediumField);
			c.gridx = 1;
			add(userField, c);

			s = Modeler.getInternationalText("Password");
			label = new JLabel(s != null ? s : "Password");
			label.setBorder(border1);
			c.gridx = 2;
			add(label, c);
			passwordField = new JPasswordField(user == null ? null : user.getPassword());
			passwordField.setPreferredSize(mediumField);
			c.gridx = 3;
			add(passwordField, c);

			label = new JLabel(
					"<html><hr><b>Registration is required.</b><br>If you haven't got a user ID, click the Register Button now. If you have registered but do not see your user ID and password above, please type them in.<hr></html>");
			c.gridx = 0;
			c.gridy = 4;
			c.weightx = 1.0; // use all available horizontal space
			c.weighty = 1.0; // use all available vertical space
			c.gridwidth = 4;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(label, c);
		}

		// submit button
		s = Modeler.getInternationalText("Submit");
		JButton button = new JButton(s != null ? s : "Submit");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SwingWorker("Submit Comment") {
					public Object construct() {
						return submitComment();
					}

					public void finished() {
						switch (statusCode) {
						case HttpURLConnection.HTTP_INTERNAL_ERROR:
							JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CommentInputPane.this),
									"Internal server error: Can't store comments.", "Server Message",
									JOptionPane.ERROR_MESSAGE);
							break;
						case HttpURLConnection.HTTP_UNAUTHORIZED:
							JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CommentInputPane.this),
									"Authorization error: Check your user name and password.", "Server Message",
									JOptionPane.ERROR_MESSAGE);
							break;
						case HttpURLConnection.HTTP_CREATED:
							processCommentSubmittedEvent();
							if (!popup)
								JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CommentInputPane.this),
										get().toString(), "Server Message", JOptionPane.INFORMATION_MESSAGE);
							break;
						case 0:
							String s = get().toString();
							if (s.startsWith("Error:"))
								JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(CommentInputPane.this),
										s, "Error", JOptionPane.ERROR_MESSAGE);
							break;
						}
						statusCode = 0;
					}
				}.start();
			}
		});
		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(button, c);

		// Clear button
		s = Modeler.getInternationalText("Clear");
		button = new JButton(s != null ? s : "Clear");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textArea.setText(null);
				titleField.setText(null);
			}
		});
		c.gridy = 1;
		add(button, c);

		if (popup) {
			// Cancel button
			s = Modeler.getInternationalText("CancelButton");
			button = new JButton(s != null ? s : "Cancel");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					processCommentCanceledEvent();
				}
			});
			c.gridy = 2;
			c.anchor = GridBagConstraints.NORTH;
			add(button, c);
		}
		else {
			// Register button
			s = Modeler.getInternationalText("Register");
			button = new JButton(s != null ? s : "Register");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (registerAction != null)
						registerAction.run();
				}
			});
			c.gridy = 4;
			c.anchor = GridBagConstraints.CENTER;
			add(button, c);
		}

	}

	public void setRegisterAction(Runnable r) {
		registerAction = r;
	}

	public void setUser(Person user) {
		this.user = user;
		if (user != null) {
			if (userField != null)
				userField.setText(user.getUserID());
			if (passwordField != null)
				passwordField.setText(user.getPassword());
		}
	}

	public void addCommentListener(CommentListener cl) {
		if (commentListeners == null)
			commentListeners = new ArrayList<CommentListener>();
		commentListeners.add(cl);
	}

	public void removeCommentListener(CommentListener cl) {
		if (commentListeners == null)
			return;
		commentListeners.remove(cl);
	}

	protected void processCommentSubmittedEvent() {
		if (commentListeners == null)
			return;
		CommentEvent e = new CommentEvent(this);
		for (CommentListener l : commentListeners)
			l.commentSubmitted(e);
	}

	protected void processCommentCanceledEvent() {
		if (commentListeners == null)
			return;
		CommentEvent e = new CommentEvent(this);
		for (CommentListener l : commentListeners)
			l.commentCanceled(e);
	}

	public void requestFocus() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				titleField.selectAll();
				titleField.requestFocusInWindow();
			}
		});
	}

	public void setServletURL(URL url) {
		servletURL = url;
	}

	public URL getServletURL() {
		return servletURL;
	}

	public void setPageAddress(String s) {
		address = s;
	}

	public String getPageAddress() {
		return address;
	}

	private String submitComment() {

		if (servletURL == null)
			return "Error: no servlet URL.";
		if (!FileUtilities.isRemote(address))
			return "Error: " + address + " is local.";

		String userID = userField == null ? Modeler.user.getUserID() : userField.getText();
		String password = passwordField == null ? Modeler.user.getPassword() : new String(passwordField.getPassword());
		MwAuthenticator auth = new MwAuthenticator();
		if (!auth.isAuthorized(encode(userID), encode(password))) {
			statusCode = HttpURLConnection.HTTP_UNAUTHORIZED;
			return "Error: unauthorized";
		}

		Object[] info = new Object[] { "submit", address, ModelerUtilities.getUnicode(titleField.getText()),
				ModelerUtilities.getUnicode(textArea.getText()), userID };
		HttpURLConnection connect = ConnectionManager.getConnection(servletURL);
		if (connect == null)
			return "Error: can't connect to " + servletURL;
		String msg = null;
		connect.setDoOutput(true);
		try {
			connect.setRequestMethod("POST");
		}
		catch (ProtocolException e) {
			e.printStackTrace();
		}
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new BufferedOutputStream(connect.getOutputStream()));
			out.writeObject(info);
		}
		catch (IOException e) {
			e.printStackTrace();
			msg = "Error: " + e;
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException e) {
				}
			}
		}
		if (msg != null)
			return msg;

		try {
			statusCode = connect.getResponseCode();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return statusCode == HttpURLConnection.HTTP_CREATED ? "Thank you for your comment."
				: "Error: comment not created";

	}

	private static String encode(String s) {
		try {
			return URLEncoder.encode(ModelerUtilities.getUnicode(s), "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return s;
		}
	}

}