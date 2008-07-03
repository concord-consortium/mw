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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.SwingWorker;

class ReportDialog extends JDialog {

	private Page page;
	private Person student;

	private JLabel descriptionLabel;
	private JTextField titleField;
	private JTextField userIDField;
	private JTextField userNameField;
	private JTextField collaboratorField;
	private JPanel panel;
	private GridBagConstraints c;

	public ReportDialog(final Frame owner) {

		super(owner, "Submit this report", true);
		String s = Modeler.getInternationalText("SubmitThisReport");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setContentPane(panel);

		c = new GridBagConstraints();

		Dimension mediumField = new Dimension(120, 20);
		Dimension longField = new Dimension(240, 20);

		// Spacing between the label and the field
		EmptyBorder border = new EmptyBorder(0, 0, 0, 8);
		EmptyBorder border1 = new EmptyBorder(0, 8, 0, 8);

		// add some space around all my components to avoid cluttering
		c.insets = new Insets(2, 2, 2, 2);

		// anchors all my components to the west
		c.anchor = GridBagConstraints.WEST;

		s = Modeler.getInternationalText("ThisReportIsCreatedJointlyWith");
		JLabel label = new JLabel((s != null ? s : "This report is created jointly with") + ":");
		label.setBorder(border);
		c.gridx = 0;
		c.gridwidth = 2;
		panel.add(label, c);
		collaboratorField = new JTextField();
		collaboratorField.setEditable(false);
		c.gridx = 2;
		c.gridwidth = 2; // spans across 3 columns
		c.fill = GridBagConstraints.HORIZONTAL; // fills up the 3 columns
		c.weightx = 1.0; // use all available horizontal space
		panel.add(collaboratorField, c);

		// Short description label and field
		s = Modeler.getInternationalText("Description");
		descriptionLabel = new JLabel(s != null ? s : "Description");
		descriptionLabel.setBorder(border); // add some space on the right
		c.gridx = 0;
		c.gridwidth = 1;
		panel.add(descriptionLabel, c);
		titleField = new PastableTextField();
		titleField.setPreferredSize(longField);
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 3; // spans across 3 columns
		panel.add(titleField, c);

		// user name and password
		s = Modeler.getInternationalText("UserName");
		label = new JLabel(s != null ? s : "User Name");
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		panel.add(label, c);
		userNameField = new JTextField(student == null ? null : student.getFullName());
		userNameField.setPreferredSize(mediumField);
		userNameField.setEditable(false);
		s = Modeler.getInternationalText("AccountID");
		c.gridx = 1;
		c.gridwidth = 1;
		panel.add(userNameField, c);
		label = new JLabel(s != null ? s : "Account ID");
		label.setBorder(border1);
		c.gridx = 2;
		c.gridwidth = 1;
		panel.add(label, c);
		userIDField = new JTextField(student == null ? null : student.getUserID());
		userIDField.setPreferredSize(mediumField);
		userIDField.setEditable(false);
		c.gridx = 3;
		c.gridwidth = 1;
		panel.add(userIDField, c);

		// Classmate look-up button
		s = Modeler.getInternationalText("LookupCollaborators");
		JButton button = new JButton(s != null ? s : "Look up Collaborators");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (student == null || student.isEmpty()) {
					JOptionPane
							.showMessageDialog(
									ReportDialog.this,
									"You must log in. Close this window and a window will appear for you to log in. After you\nhave logged in, close that window and come back to click the Submission Button again.",
									"Error", JOptionPane.ERROR_MESSAGE);
					dispose();
					page.openPageInNewWindow(Modeler.getContextRoot() + "myhome.jsp?client=mw");
					return;
				}
				lookupCollaborators();
			}
		});
		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTH;
		panel.add(button, c);

		// Cancel button
		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		c.gridy = 1;
		panel.add(button, c);

		// submit button
		s = Modeler.getInternationalText("Submit");
		button = new JButton(s != null ? s : "Submit");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (student.getCollaboratorIdArray() == null) {
					if (JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(page),
							"Did you collaborate with someone in creating this report?", "Any collaborator?",
							JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						JOptionPane
								.showMessageDialog(JOptionPane.getFrameForComponent(page),
										"Please go back to click the \"Look up Collaborators\" button to include your collaborators.");
						return;
					}
				}
				submit();
			}
		});
		c.gridy = 2;
		panel.add(button, c);

		pack();
		setLocationRelativeTo(owner);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

	}

	void setStudent(Person student) {
		this.student = student;
		if (student != null) {
			userIDField.setText(student.getUserID());
			userNameField.setText(student.getFullName());
		}
	}

	void setPage(Page page) {
		this.page = page;
		titleField.setText(page.getTitle());
	}

	private void submit() {

		if (page == null)
			return;

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setVisible(false);
			}
		});

		StringBuffer sb = new StringBuffer(Modeler.getContextRoot() + "reportupload?client=mw&action=upload");
		sb.append("&filename=" + encode(FileUtilities.getFileName(page.getAddress())));
		sb.append("&title=" + encode(titleField.getText()));
		sb.append("&userid=" + encode(userIDField.getText()));
		if (student != null && student.getCollaboratorIdArray() != null) {
			sb.append("&collaborator=" + encode(student.getCollaboratorIdString()));
		}

		URL url = null;
		try {
			url = new URL(sb.toString());
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			final String s = e.toString();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ReportDialog.this), s, "URL Error",
							JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}

		final URL url2 = url;

		page.uploadPage(new Upload() {

			public byte getType() {
				return Upload.UPLOAD_REPORT;
			}

			public URL getURL() {
				return url2;
			}

			public String getEntryPage() {
				return "";
			}

		});

		student.setCollaboratorIdArray(null);

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

	private void lookupCollaborators() {
		SwingWorker worker = new SwingWorker("Lookup Collaborators") {
			public Object construct() {
				return lookupClassmates();
			}

			public void finished() {
				String classmates = (String) get();
				if (classmates == null || classmates.trim().equals("")) {
					JOptionPane.showMessageDialog(ReportDialog.this, "Sorry, we did not find any classmate for you.");
					return;
				}
				String[] x = classmates.split(",");
				int n = x.length;
				String[] id = new String[n];
				String[] name = new String[n];
				for (int i = 0; i < n; i++) {
					String[] y = x[i].split(":");
					id[i] = y[0].trim();
					name[i] = y[1].trim();
				}
				String current = collaboratorField.getText();
				CollaboratorSelection cs = new CollaboratorSelection(name, current != null ? current.split(",") : null);
				cs.show(ReportDialog.this);
				if (cs.isOK()) {
					String s = cs.getSelection();
					String t = s;
					for (int i = 0; i < n; i++) {
						t = t.replace(name[i], id[i]);
					}
					student.setCollaboratorIdArray(t.split(","));
					collaboratorField.setText(s);
					if (s != null && !s.trim().equals("")) {
						changeNames(student.getFullName() + ", " + s);
					}
				}
			}
		};
		worker.start();
	}

	private void changeNames(String newNames) {
		String text = page.getText();
		int i = text.indexOf("Student name:");
		int j = text.indexOf("Teacher name:");
		page.setEditable(true);
		if (System.getProperty("os.name").startsWith("Windows")) {
			page.select(i + 12, j - 3);
		}
		else {
			page.select(i + 14, j); // line break on OS X differs from that on Windows
		}
		page.replaceSelection(newNames + "\n");
		page.setEditable(false);
		page.setSelection(0, 0, false);
		page.getSaveReminder().setChanged(false);
	}

	private String lookupClassmates() {
		if (student == null || student.isEmpty())
			return null;
		URL url = null;
		try {
			url = new URL(Modeler.getContextRoot() + "classmate?userid=" + student.getUserID() + "&password="
					+ student.getPassword());
		}
		catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		if (url == null)
			return null;
		HttpURLConnection conn = ConnectionManager.getConnection(url);
		if (conn == null)
			return null;
		InputStream is = null;
		try {
			is = conn.getInputStream();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		if (is == null)
			return null;
		StringBuffer buffer = new StringBuffer();
		byte[] b = new byte[1024];
		int n = -1;
		try {
			while ((n = is.read(b)) != -1) {
				buffer.append(new String(b, 0, n));
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		finally {
			try {
				is.close();
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return buffer.toString();
	}

}