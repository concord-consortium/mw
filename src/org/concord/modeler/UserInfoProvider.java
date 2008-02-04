/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @ThreadSafe
 * @author Charles Xie
 * 
 */
class UserInfoProvider extends JDialog {

	boolean isOK = false;

	UserInfoProvider(Frame owner) {

		super(owner, "Student Information", true);
		String s = Modeler.getInternationalText("StudentInfo");
		if (s != null)
			setTitle(s);

		s = System.getProperty("student name");
		final JTextField studentField = new JTextField(s != null ? s : "", 20);

		s = System.getProperty("teacher name");
		final JTextField teacherField = new JTextField(s != null ? s : "", 20);

		s = System.getProperty("school name");
		final JTextField schoolField = new JTextField(s != null ? s : "", 20);

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.setProperty("student name", studentField.getText());
				System.setProperty("teacher name", teacherField.getText());
				System.setProperty("school name", schoolField.getText());
				isOK = true;
				dispose();
			}
		};

		JPanel p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		s = Modeler.getInternationalText("TypeYourInfoForReportInBoxes");
		p
				.add(
						new JLabel(
								s != null ? s
										: "<html>Please type your information, to be printed in the report,<br>in the following boxes.</html>"),
						BorderLayout.NORTH);

		JPanel p1 = new JPanel(new GridLayout(3, 1, 5, 5));
		p.add(p1, BorderLayout.WEST);

		s = Modeler.getInternationalText("StudentName");
		p1.add(new JLabel(s != null ? s : "Your name"));

		s = Modeler.getInternationalText("TeacherName");
		p1.add(new JLabel(s != null ? s : "Your teacher's name"));

		s = Modeler.getInternationalText("SchoolName");
		p1.add(new JLabel(s != null ? s : "Your school's name"));

		p1 = new JPanel(new GridLayout(3, 1, 5, 5));
		p.add(p1, BorderLayout.CENTER);

		studentField.addActionListener(al);
		p1.add(studentField);

		teacherField.addActionListener(al);
		p1.add(teacherField);

		schoolField.addActionListener(al);
		p1.add(schoolField);

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
				System.setProperty("student name", studentField.getText());
				System.setProperty("teacher name", teacherField.getText());
				System.setProperty("school name", schoolField.getText());
				dispose();
			}
		});
		p.add(button);

		getContentPane().add(p, BorderLayout.SOUTH);

		pack();

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.setProperty("student name", studentField.getText());
				System.setProperty("teacher name", teacherField.getText());
				System.setProperty("school name", schoolField.getText());
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				studentField.requestFocus();
				studentField.selectAll();
			}
		});

	}

}