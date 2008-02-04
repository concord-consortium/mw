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
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;

/**
 * @author Charles Xie
 * 
 */
class PagePeriodicTableMaker extends ComponentMaker {

	private PagePeriodicTable pagePeriodicTable;
	private JDialog dialog;
	private ColorComboBox bgComboBox;
	private JComboBox borderComboBox;
	private JCheckBox transparentCheckBox, muteCheckBox;
	private JPanel contentPane;

	PagePeriodicTableMaker(PagePeriodicTable ppt) {
		setObject(ppt);
	}

	void setObject(PagePeriodicTable ppt) {
		pagePeriodicTable = ppt;
	}

	private void confirm() {
		pagePeriodicTable.setOpaque(!transparentCheckBox.isSelected());
		pagePeriodicTable.mute(muteCheckBox.isSelected());
		pagePeriodicTable.setBorderType((String) borderComboBox.getSelectedItem());
		pagePeriodicTable.setBackground(bgComboBox.getSelectedColor());
		pagePeriodicTable.page.getSaveReminder().setChanged(true);
		pagePeriodicTable.page.reload();
	}

	void invoke(Page page) {

		pagePeriodicTable.setPage(page);
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizePeriodicTableDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize periodic table", true);
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

		transparentCheckBox.setSelected(!pagePeriodicTable.isOpaque());
		muteCheckBox.setSelected(pagePeriodicTable.isMuted());
		borderComboBox.setSelectedItem(pagePeriodicTable.getBorderType());
		bgComboBox.setColor(pagePeriodicTable.getBackground());

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
		s = Modeler.getInternationalText("BackgroundColorLabel");
		JLabel label = new JLabel(s != null ? s : "Background color", SwingConstants.LEFT);
		p.add(label);
		bgComboBox = new ColorComboBox(pagePeriodicTable);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color.");
		p.add(bgComboBox);

		// row 2
		s = Modeler.getInternationalText("BorderLabel");
		label = new JLabel(s != null ? s : "Border", SwingConstants.LEFT);
		p.add(label);
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type.");
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 2, 2, 5, 5, 15, 5);

		p = new JPanel();
		p.setBorder(BorderFactory.createEtchedBorder());
		contentPane.add(p, BorderLayout.CENTER);

		s = Modeler.getInternationalText("TransparencyCheckBox");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setToolTipText("Set to be transparent.");
		p.add(transparentCheckBox);

		s = Modeler.getInternationalText("Mute");
		muteCheckBox = new JCheckBox(s != null ? s : "Mute");
		muteCheckBox.setToolTipText("Mute (no sound effect).");
		p.add(muteCheckBox);

	}

}