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

package org.concord.modeler.text;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.concord.modeler.Modeler;
import org.concord.modeler.ui.PastableTextField;

class GradeSubmissionDialog extends JDialog {

	private JTextField firstNameField;
	private JTextField lastNameField;
	private JTextField teacherField;
	private JTextField schoolField;
	private JTextField emailField;
	private JPasswordField passwordField;
	private boolean cancelled;

	public GradeSubmissionDialog(final Frame owner) {

		super(owner, "Student Info", true);
		String s = Modeler.getInternationalText("StudentInfo");
		if (s != null)
			setTitle(s);

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean b = true;
				String s = firstNameField.getText();
				if (s == null || s.trim().equals("")) {
					b = false;
				}
				else {
					Modeler.user.setFirstName(s);
				}
				s = lastNameField.getText();
				if (s == null || s.trim().equals("")) {
					b = false;
				}
				else {
					Modeler.user.setLastName(s);
				}
				s = schoolField.getText();
				if (s == null || s.trim().equals("")) {
					b = false;
				}
				else {
					Modeler.user.setInstitution(s);
				}
				s = emailField.getText();
				if (s == null || s.trim().equals("")) {
					b = false;
				}
				else {
					Modeler.user.setEmailAddress(s);
				}
				char[] c = passwordField.getPassword();
				if (c == null || c.length == 0) {
					b = false;
				}
				else {
					// Modeler.user.setPassword(c);
				}
				s = teacherField.getText();
				if (s == null || s.trim().equals("")) {
					b = false;
				}
				else {
					Modeler.user.setTeacher(s);
				}
				if (b) {
					dispose();
				}
				else {
					JOptionPane.showMessageDialog(owner, "Required information must be provided.");
				}
			}
		};

		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		getContentPane().add(p, BorderLayout.NORTH);

		s = Modeler.getInternationalText("YouMustProvideInfoToBeGraded");
		p
				.add(new JLabel(
						"<html><table border=\"1\" bgcolor=\"#ffff00\"><tr><td><font color=\"#ff0000\">"
								+ (s != null ? s
										: "You <b>must</b> provide the following information<br>for your test to be graded under your name<br>(* required):")
								+ "</td></tr></table></font></html>"));

		p = new JPanel(new BorderLayout(10, 0));
		p.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

		JPanel p1 = new JPanel(new GridLayout(6, 1, 5, 5));
		p.add(p1, BorderLayout.WEST);

		s = Modeler.getInternationalText("FirstName");
		p1.add(new JLabel((s != null ? s : "First Name") + " *"));
		s = Modeler.getInternationalText("LastName");
		p1.add(new JLabel((s != null ? s : "Last Name") + " *"));
		s = Modeler.getInternationalText("TeacherName");
		p1.add(new JLabel((s != null ? s : "Teacher Name") + " *"));
		s = Modeler.getInternationalText("SchoolName");
		p1.add(new JLabel((s != null ? s : "School") + " *"));
		s = Modeler.getInternationalText("EmailAddress");
		p1.add(new JLabel((s != null ? s : "E-mail") + " *"));
		s = Modeler.getInternationalText("Password");
		p1.add(new JLabel((s != null ? s : "Password") + " *"));

		p1 = new JPanel(new GridLayout(6, 1, 5, 5));
		p.add(p1, BorderLayout.CENTER);

		firstNameField = new PastableTextField(20);
		firstNameField.setText(Modeler.user.getFirstName());
		firstNameField.addActionListener(al);
		p1.add(firstNameField);

		lastNameField = new PastableTextField(20);
		lastNameField.setText(Modeler.user.getLastName());
		lastNameField.addActionListener(al);
		p1.add(lastNameField);

		teacherField = new PastableTextField();
		teacherField.setText(Modeler.user.getTeacher());
		teacherField.addActionListener(al);
		p1.add(teacherField);

		schoolField = new PastableTextField();
		schoolField.setText(Modeler.user.getInstitution());
		schoolField.addActionListener(al);
		p1.add(schoolField);

		emailField = new PastableTextField();
		emailField.setText(Modeler.user.getEmailAddress());
		emailField.addActionListener(al);
		p1.add(emailField);

		passwordField = new JPasswordField();
		passwordField.addActionListener(al);
		p1.add(passwordField);

		getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel();

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(al);
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelled = true;
				dispose();
			}
		});
		p.add(button);

		getContentPane().add(p, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				cancelled = true;
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				firstNameField.requestFocus();
				firstNameField.selectAll();
			}
		});

	}

	boolean isCancelled() {
		return cancelled;
	}

}