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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * @author Charles Xie
 * 
 */
class CollaboratorSelection {

	private DefaultListModel classmateModel, collaboratorModel;
	private JList classmateList, collaboratorList;
	private JButton addButton, removeButton;
	private String[] classmateNames, collaboratorNames;
	private boolean ok;

	private boolean isCollaborator(String name) {
		if (collaboratorNames == null)
			return false;
		for (String s : collaboratorNames) {
			if (s.equals(name))
				return true;
		}
		return false;
	}

	CollaboratorSelection(String[] classmates, String[] collaborators) {

		classmateNames = classmates;
		collaboratorNames = collaborators;
		if (collaboratorNames != null) {
			for (int i = 0; i < collaboratorNames.length; i++)
				collaboratorNames[i] = collaboratorNames[i].trim();
		}

		classmateModel = new DefaultListModel();
		for (String x : classmateNames) {
			if (x.equals(""))
				continue;
			if (isCollaborator(x))
				continue;
			classmateModel.addElement(x);
		}
		classmateList = new JList(classmateModel);
		classmateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		String s = Modeler.getInternationalText("MyClassmates");
		classmateList.setBorder(BorderFactory.createTitledBorder(s != null ? s : "My Classmates"));

		collaboratorModel = new DefaultListModel();
		for (String x : collaboratorNames) {
			if (x.equals(""))
				continue;
			collaboratorModel.addElement(x);
		}
		collaboratorList = new JList(collaboratorModel);
		collaboratorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		s = Modeler.getInternationalText("MyCollaborators");
		collaboratorList.setBorder(BorderFactory.createTitledBorder(s != null ? s : "My Collaborators"));

		s = Modeler.getInternationalText("AddCollaborator");
		addButton = new JButton(s != null ? s : "Add");
		addButton.setEnabled(false);
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object o = classmateList.getSelectedValue();
				if (o == null)
					return;
				classmateModel.removeElement(o);
				collaboratorModel.addElement(o);
				addButton.setEnabled(false);
			}
		});
		s = Modeler.getInternationalText("RemoveCollaborator");
		removeButton = new JButton(s != null ? s : "Remove");
		removeButton.setEnabled(false);
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object o = collaboratorList.getSelectedValue();
				if (o == null)
					return;
				collaboratorModel.removeElement(o);
				classmateModel.addElement(o);
				removeButton.setEnabled(false);
			}
		});

		classmateList.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				collaboratorList.clearSelection();
				addButton.setEnabled(true);
				removeButton.setEnabled(false);
			}
		});

		collaboratorList.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				classmateList.clearSelection();
				addButton.setEnabled(false);
				removeButton.setEnabled(true);
			}
		});

	}

	void show(Component parent) {

		String s = Modeler.getInternationalText("SelectCollaboratorsFromClassmates");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), s != null ? s
				: "Select collaborators from classmates", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel contentPane = new JPanel(new BorderLayout());
		dialog.setContentPane(contentPane);

		Dimension dimension = new Dimension(200, 200);

		JScrollPane scroller = new JScrollPane(classmateList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setPreferredSize(dimension);
		contentPane.add(scroller, BorderLayout.EAST);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		contentPane.add(panel, BorderLayout.CENTER);

		JPanel p = new JPanel(new GridLayout(2, 1, 5, 5));
		panel.add(p, BorderLayout.NORTH);
		p.add(addButton);
		p.add(removeButton);

		scroller = new JScrollPane(collaboratorList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setPreferredSize(dimension);
		contentPane.add(scroller, BorderLayout.WEST);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		contentPane.add(buttonPanel, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				ok = true;
			}
		});
		buttonPanel.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				ok = false;
			}
		});
		buttonPanel.add(button);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

	}

	boolean isOK() {
		return ok;
	}

	String getSelection() {
		String s = collaboratorModel.toString();
		if (s != null) {
			if (s.startsWith("["))
				s = s.substring(1);
			if (s.endsWith("]"))
				s = s.substring(0, s.length() - 1);
		}
		return s;
	}

}
