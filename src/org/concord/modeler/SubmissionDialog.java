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
import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.FileUtilities;

class SubmissionDialog extends JDialog {

	private Page page;
	private String firstPage;
	private byte taskType = Upload.UPLOAD_PAGE;

	private JLabel descriptionLabel;
	private JLabel entryPageLabel;
	private JTextField titleField;
	private JComboBox levelComboBox;
	private JComboBox subjectComboBox;
	private JComboBox entryPageList;

	public SubmissionDialog(final Frame owner) {

		super(owner, "Submit this page", true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setContentPane(panel);

		GridBagConstraints c = new GridBagConstraints();

		Dimension mediumField = new Dimension(120, 20);
		Dimension longField = new Dimension(240, 20);

		// Spacing between the label and the field
		EmptyBorder border = new EmptyBorder(0, 0, 0, 8);
		EmptyBorder border1 = new EmptyBorder(0, 8, 0, 8);

		// add some space around all my components to avoid cluttering
		c.insets = new Insets(2, 2, 2, 2);

		// anchors all my components to the west
		c.anchor = GridBagConstraints.WEST;

		// Short description label and field
		String s = Modeler.getInternationalText("Description");
		descriptionLabel = new JLabel(s != null ? s : "Description");
		descriptionLabel.setBorder(border); // add some space on the right
		panel.add(descriptionLabel, c);
		titleField = new PastableTextField();
		titleField.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0; // use all available horizontal space
		c.gridwidth = 3; // spans across 3 columns
		c.fill = GridBagConstraints.HORIZONTAL; // fills up the 3 columns
		panel.add(titleField, c);

		// Level label and combo box
		s = Modeler.getInternationalText("UserLevel");
		JLabel label = new JLabel(s != null ? s : "Level");
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		panel.add(label, c);
		levelComboBox = new JComboBox();
		s = Modeler.getInternationalText("GeneralPublic");
		levelComboBox.addItem(s != null ? s : "General Public");
		s = Modeler.getInternationalText("ElementarySchool");
		levelComboBox.addItem(s != null ? s : "Elementary School");
		s = Modeler.getInternationalText("MiddleSchool");
		levelComboBox.addItem(s != null ? s : "Middle School");
		s = Modeler.getInternationalText("HighSchool");
		levelComboBox.addItem(s != null ? s : "High School");
		s = Modeler.getInternationalText("College");
		levelComboBox.addItem(s != null ? s : "College");
		s = Modeler.getInternationalText("GraduateSchool");
		levelComboBox.addItem(s != null ? s : "Graduate School");
		levelComboBox.setPreferredSize(mediumField);
		c.gridx = 1;
		panel.add(levelComboBox, c);

		// Subject
		s = Modeler.getInternationalText("Subject");
		label = new JLabel(s != null ? s : "Subject");
		label.setBorder(border1);
		c.gridx = 2;
		panel.add(label, c);
		subjectComboBox = new JComboBox();
		s = Modeler.getInternationalText("Generic");
		subjectComboBox.addItem(s != null ? s : "Generic");
		s = Modeler.getInternationalText("Physics");
		subjectComboBox.addItem(s != null ? s : "Physics");
		s = Modeler.getInternationalText("Chemistry");
		subjectComboBox.addItem(s != null ? s : "Chemistry");
		s = Modeler.getInternationalText("Biology");
		subjectComboBox.addItem(s != null ? s : "Biology");
		s = Modeler.getInternationalText("Others");
		subjectComboBox.addItem(s != null ? s : "Others");
		subjectComboBox.setPreferredSize(mediumField);
		c.gridx = 3;
		panel.add(subjectComboBox, c);

		s = Modeler.getInternationalText("Important");
		entryPageLabel = new JLabel(s != null ? s : "Important");
		entryPageLabel.setBorder(border);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		panel.add(entryPageLabel, c);
		entryPageList = new JComboBox();
		entryPageList.setPreferredSize(longField);
		c.gridx = 1;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(entryPageList, c);

		// submit button
		s = Modeler.getInternationalText("Submit");
		JButton button = new JButton(s != null ? s : "Submit");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (taskType == Upload.UPLOAD_FOLDER) {
					firstPage = getFirstPage();
					if (firstPage == null) {
						JOptionPane
								.showMessageDialog(JOptionPane.getFrameForComponent(page),
										"You must specify correctly the first page of your activity, so that\n"
												+ "MW can upload all the necessary pages and open the activity in\n"
												+ "the right order.", "Please select the first page",
										JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				submit();
			}
		});
		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
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
		c.anchor = GridBagConstraints.NORTH; // anchor north
		panel.add(button, c);

		pack();
		setLocationRelativeTo(owner);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				titleField.selectAll();
				titleField.requestFocus();
			}
		});

	}

	void setTaskType(byte i) {
		taskType = i;
		switch (taskType) {
		case Upload.UPLOAD_PAGE:
			String s = Modeler.getInternationalText("SubmitCurrentPage");
			setTitle(s == null ? "Submit current page" : s);
			entryPageLabel.setEnabled(false);
			entryPageList.setEnabled(false);
			entryPageList.removeAllItems();
			descriptionLabel.setEnabled(true);
			titleField.setEnabled(true);
			break;
		case Upload.UPLOAD_FOLDER:
			s = Modeler.getInternationalText("SubmitCurrentFolder");
			setTitle(s == null ? "Submit current folder" : s);
			entryPageLabel.setEnabled(true);
			entryPageList.setEnabled(true);
			descriptionLabel.setEnabled(true);
			titleField.setEnabled(true);
			break;
		}
	}

	void setPage(Page page) {
		this.page = page;
		titleField.setText(page.getTitle());
		entryPageList.removeAllItems();
		String s = Modeler.getInternationalText("SelectFirstPage");
		entryPageList.addItem(s != null ? s : "Please select the first page");
		if (!page.isRemote()) {
			try {
				File dir = new File(FileUtilities.getCodeBase(page.getAddress()));
				if (dir.isDirectory()) {
					String[] fn = dir.list(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.toLowerCase().endsWith(".cml");
						}
					});
					if (fn != null) {
						for (String i : fn)
							entryPageList.addItem(i);
					}
				}
			}
			catch (Exception e) {
				// ignore
			}
		}
	}

	String getFirstPage() {
		if (taskType == Upload.UPLOAD_PAGE)
			return page.getAddress();
		int n = entryPageList.getItemCount() - 1;
		if (n == 1)
			return (String) entryPageList.getItemAt(1);
		if (entryPageList.getSelectedIndex() == 0)
			return null;
		return (String) entryPageList.getSelectedItem();
	}

	private void submit() {

		if (page == null)
			return;

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setVisible(false);
			}
		});

		StringBuffer sb = new StringBuffer(Modeler.getContextRoot() + "modelupload?client=mw&action=upload");
		String zipfile = FileUtilities.changeExtension(FileUtilities.getFileName(page.getAddress()), "zip");
		sb.append("&zipfile=" + encode(zipfile));
		if (taskType == Upload.UPLOAD_FOLDER)
			sb.append("&firstpage=" + encode(FileUtilities.getFileName(firstPage)));
		sb.append("&title=" + encode(titleField.getText()));
		sb.append("&level=" + encode(levelComboBox.getSelectedItem().toString()));
		sb.append("&subject=" + encode(subjectComboBox.getSelectedItem().toString()));
		sb.append("&userid=" + encode(Modeler.user.getUserID()));

		URL url = null;
		try {
			url = new URL(sb.toString());
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			final String s = e.toString();
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(SubmissionDialog.this), s,
							"URL Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}

		final URL url2 = url;

		page.uploadPage(new Upload() {

			public byte getType() {
				return taskType;
			}

			public URL getURL() {
				return url2;
			}

			public String getEntryPage() {
				return firstPage;
			}

		});

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