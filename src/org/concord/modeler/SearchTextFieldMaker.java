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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;

/**
 * @author Charles Xie
 * 
 */
class SearchTextFieldMaker extends ComponentMaker {

	private final static String[] DATABASE = new String[] { "User Upload Database", "Student Report Database",
			"User's Guide", "Library of Models", "Entire MW Site" };

	private SearchTextField searchTextField;
	private JDialog dialog;
	private JSpinner spinner;
	private JComboBox databaseComboBox;
	private JCheckBox categoryCheckBox;
	private JPanel contentPane;

	SearchTextFieldMaker(SearchTextField stf) {
		setObject(stf);
	}

	void setObject(SearchTextField stf) {
		searchTextField = stf;
	}

	private void confirm() {
		searchTextField.addCategoryComboBox(categoryCheckBox.isSelected());
		searchTextField.setColumns(((Integer) spinner.getValue()).intValue());
		searchTextField.setDatabaseType(databaseComboBox.getSelectedIndex());
		searchTextField.page.getSaveReminder().setChanged(true);
		searchTextField.page.settleComponentSize();
	}

	void invoke(Page page) {

		searchTextField.setPage(page);
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			dialog = ModelerUtilities.getChildDialog(page, "Customize search text field", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}
			});
		}

		spinner.setValue(new Integer(searchTextField.getColumns()));
		databaseComboBox.setSelectedIndex(searchTextField.getDatabaseType());

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dialog.dispose();
				cancel = false;
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		p.add(new JLabel("Database", SwingConstants.LEFT));
		databaseComboBox = new JComboBox(DATABASE);
		p.add(databaseComboBox);

		// row 2
		p.add(new JLabel("Columns", SwingConstants.LEFT));
		spinner = new JSpinner(new SpinnerNumberModel(searchTextField.getColumns() > 10 ? searchTextField.getColumns()
				: 10, 10, 100, 1));
		p.add(spinner);

		ModelerUtilities.makeCompactGrid(p, 2, 2, 5, 5, 15, 5);

		p = new JPanel();
		contentPane.add(p, BorderLayout.CENTER);

		categoryCheckBox = new JCheckBox("Add category combo box");
		p.add(categoryCheckBox);

	}

}