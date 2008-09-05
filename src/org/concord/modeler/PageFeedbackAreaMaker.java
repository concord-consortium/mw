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
import org.concord.modeler.ui.FloatNumberTextField;

/**
 * @author Charles Xie
 * 
 */
class PageFeedbackAreaMaker extends ComponentMaker {

	private PageFeedbackArea pageFeedbackArea;
	private JDialog dialog;
	private JComboBox borderComboBox, opaqueComboBox;
	private FloatNumberTextField widthField, heightField;
	private JPanel contentPane;

	PageFeedbackAreaMaker(PageFeedbackArea pfa) {
		setObject(pfa);
	}

	void setObject(PageFeedbackArea pfa) {
		pageFeedbackArea = pfa;
	}

	private void confirm() {
		pageFeedbackArea.getInputArea().setPageAddress(pageFeedbackArea.page.getAddress());
		pageFeedbackArea.setTransparent(opaqueComboBox.getSelectedIndex() == 0);
		pageFeedbackArea.setBorderType((String) borderComboBox.getSelectedItem());
		double w = widthField.getValue();
		double h = heightField.getValue();
		if (w > 0 && w < 1.05) {
			pageFeedbackArea.setWidthRatio((float) w);
			w *= pageFeedbackArea.page.getWidth();
			pageFeedbackArea.setWidthRelative(true);
		}
		if (h > 0 && h < 1.05) {
			pageFeedbackArea.setHeightRatio((float) h);
			h *= pageFeedbackArea.page.getHeight();
			pageFeedbackArea.setHeightRelative(true);
		}
		pageFeedbackArea.setPreferredSize(new Dimension((int) w, (int) h));
		pageFeedbackArea.page.getSaveReminder().setChanged(true);
		pageFeedbackArea.page.settleComponentSize();
	}

	void invoke(Page page) {

		pageFeedbackArea.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			dialog = ModelerUtilities.getChildDialog(page, "Customize feedback area", true);
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
					widthField.requestFocus();
				}
			});
		}

		opaqueComboBox.setSelectedIndex(pageFeedbackArea.isTransparent() ? 0 : 1);
		borderComboBox.setSelectedItem(pageFeedbackArea.getBorderType());
		if (pageFeedbackArea.isPreferredSizeSet()) {
			if (pageFeedbackArea.widthIsRelative) {
				widthField.setValue(pageFeedbackArea.widthRatio);
			}
			else {
				widthField.setValue(pageFeedbackArea.getPreferredSize().width);
			}
			if (pageFeedbackArea.heightIsRelative) {
				heightField.setValue(pageFeedbackArea.heightRatio);
			}
			else {
				heightField.setValue(pageFeedbackArea.getPreferredSize().height);
			}
		}

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
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
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
		contentPane.add(p, BorderLayout.CENTER);

		// row 1
		p.add(new JLabel("Width", SwingConstants.LEFT));
		widthField = new FloatNumberTextField(pageFeedbackArea.getWidth() <= 0 ? 600 : pageFeedbackArea.getWidth(),
				600, 1200);
		widthField.setToolTipText("A value in (0, 1] will be considered relative to the width of the page");
		widthField.setMaximumFractionDigits(4);
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 2
		p.add(new JLabel("Height", SwingConstants.LEFT));
		heightField = new FloatNumberTextField(pageFeedbackArea.getHeight() <= 0 ? 600 : pageFeedbackArea.getHeight(),
				600, 1200);
		heightField.setMaximumFractionDigits(4);
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 3
		p.add(new JLabel("Transparent", SwingConstants.LEFT));
		opaqueComboBox = new JComboBox(new Object[] { "Yes", "No" });
		opaqueComboBox.setSelectedIndex(1);
		p.add(opaqueComboBox);

		// row 4
		p.add(new JLabel("Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setPreferredSize(new Dimension(200, 24));
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 4, 2, 5, 5, 15, 5);

	}

}