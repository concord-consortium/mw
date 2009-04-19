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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;

/**
 * @author Charles Xie
 * 
 */
class PageMd3dMaker extends ComponentMaker {

	private PageMd3d pageMd3d;
	private JDialog dialog;
	private JComboBox borderComboBox;
	private IntegerTextField widthField, heightField;
	private JButton okButton;
	private JPanel contentPane;

	PageMd3dMaker(PageMd3d pm) {
		setObject(pm);
	}

	void setObject(PageMd3d pm) {
		pageMd3d = pm;
	}

	private void confirm() {
		pageMd3d.setChangable(true);
		pageMd3d.setBorderType((String) borderComboBox.getSelectedItem());
		pageMd3d.setViewerSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageMd3d.page.getSaveReminder().setChanged(true);
	}

	void invoke(Page page) {

		pageMd3d.setPage(page);
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("3DMolecularSimulatorDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize 3D Molecular Simulator", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					widthField.selectAll();
					widthField.requestFocusInWindow();
				}
			});
		}

		borderComboBox.setSelectedItem(pageMd3d.getBorderType());
		widthField.setValue(pageMd3d.getWidth() <= 0 ? 300 : pageMd3d.getWidth());
		heightField.setValue(pageMd3d.getHeight() <= 0 ? 300 : pageMd3d.getHeight());

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dialog.dispose();
				cancel = false;
			}
		};

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		okButton = new JButton(s != null ? s : "OK");
		okButton.addActionListener(okListener);
		p.add(okButton);

		s = Modeler.getInternationalText("CancelButton");
		JButton button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		// row 1
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(300, 100, 1000);
		widthField.setToolTipText("Type in an integer to set the width.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 2
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(300, 100, 800);
		heightField.setToolTipText("Type in an integer to set the height.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 3
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setPreferredSize(new Dimension(150, 20));
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type for this component.");
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 3, 2, 5, 5, 10, 2);

	}

}