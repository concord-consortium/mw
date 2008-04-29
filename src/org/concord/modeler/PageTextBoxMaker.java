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
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.HTMLPane;
import org.concord.modeler.ui.PastableTextArea;

/**
 * @author Charles Xie
 * 
 */
class PageTextBoxMaker extends ComponentMaker {

	private PageTextBox pageTextBox;
	private JDialog dialog;
	private JComboBox borderComboBox;
	private JTextArea textArea;
	private FloatNumberTextField widthField, heightField;
	private ColorComboBox bgComboBox;
	private JCheckBox transparentCheckBox;
	private JLabel indexLabel;
	private JScrollPane scrollPane;
	private JPanel contentPane;

	PageTextBoxMaker(PageTextBox ptb) {
		setObject(ptb);
	}

	void setObject(PageTextBox ptb) {
		pageTextBox = ptb;
	}

	private void confirm() {
		pageTextBox.setBorderType((String) borderComboBox.getSelectedItem());
		pageTextBox.putClientProperty("border", pageTextBox.getBorderType());
		pageTextBox.setBackground(bgComboBox.getSelectedColor());
		pageTextBox.setTransparent(transparentCheckBox.isSelected());
		String text = textArea.getText();
		if (text.trim().length() >= 6 && text.trim().substring(0, 6).toLowerCase().equals("<html>")) {
			pageTextBox.setContentType("text/html");
		}
		else {
			pageTextBox.setContentType("text/plain");
		}
		pageTextBox.setText(text);
		pageTextBox.setOriginalText(text);
		double w = widthField.getValue();
		double h = heightField.getValue();
		if (w > 0 && w < 1.05) {
			pageTextBox.setWidthRatio((float) w);
			w *= pageTextBox.page.getWidth();
			pageTextBox.setWidthRelative(true);
		}
		else {
			pageTextBox.setWidthRelative(false);
		}
		if (h > 0 && h < 1.05) {
			pageTextBox.setHeightRatio((float) h);
			h *= pageTextBox.page.getHeight();
			pageTextBox.setHeightRelative(true);
		}
		else {
			pageTextBox.setHeightRelative(false);
		}
		pageTextBox.setPreferredSize(new Dimension((int) w, (int) h));
		((HTMLPane) pageTextBox.getTextComponent()).removeLinkMonitor();
		pageTextBox.showBoundary(pageTextBox.page.isEditable());
		pageTextBox.page.getSaveReminder().setChanged(true);
		pageTextBox.page.reload();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pageTextBox.setEmbeddedComponentAttributes();
			}
		});
	}

	void invoke(Page page) {

		pageTextBox.setPage(page);
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			dialog = ModelerUtilities.getChildDialog(page, true);
			String s = Modeler.getInternationalText("CustomizeTextBoxDialogTitle");
			dialog.setTitle(s != null ? s : "Customize text box");
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					textArea.requestFocusInWindow();
				}
			});
		}

		indexLabel.setText(pageTextBox.index == -1 ? "to be decided" : "#" + (pageTextBox.index + 1));
		if (pageTextBox.widthIsRelative) {
			widthField.setValue(pageTextBox.widthRatio);
		}
		else {
			widthField.setValue(pageTextBox.getPreferredSize().width);
		}
		if (pageTextBox.heightIsRelative) {
			heightField.setValue(pageTextBox.heightRatio);
		}
		else {
			heightField.setValue(pageTextBox.getPreferredSize().height);
		}
		String text = pageTextBox.getText();
		textArea.setText(ModelerUtilities.deUnicode(text)); // Victor Vicovlov
		if (text != null && text.trim().length() >= 6) {
			pageTextBox.setContentType(text.trim().substring(0, 6).toLowerCase().equals("<html>") ? "text/html"
					: "text/plain");
		}
		else {
			pageTextBox.setContentType("text/plain");
		}
		textArea.setCaretPosition(0);
		borderComboBox.setSelectedItem(pageTextBox.getBorderType());
		transparentCheckBox.setSelected(pageTextBox.isTransparent());
		bgComboBox.setColor(pageTextBox.getTextComponent().getBackground());

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		Action okAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dialog.dispose();
				cancel = false;
			}
		};

		JPanel downPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(downPanel, BorderLayout.SOUTH);

		downPanel.add(new JLabel("* An input between 0 and 1 sets width relative to page."));

		JButton button = new JButton(okAction);
		String s = Modeler.getInternationalText("OKButton");
		button.setText(s != null ? s : "OK");
		downPanel.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		downPanel.add(button);

		JPanel topPanel = new JPanel(new BorderLayout(5, 5));
		topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(topPanel, BorderLayout.NORTH);

		JPanel p = new JPanel(new SpringLayout());
		topPanel.add(p, BorderLayout.CENTER);

		// row 1
		s = Modeler.getInternationalText("WidthLabel");
		JLabel label = new JLabel((s != null ? s : "Width") + " *", SwingConstants.LEFT);
		label.setToolTipText("A value in (0, 1] will be considered relative to the width of the page");
		p.add(label);
		widthField = new FloatNumberTextField(200, 0.01f, 2000);
		widthField.setToolTipText("A value in (0, 1] will be considered relative to the width of the page");
		widthField.setMaximumFractionDigits(4);
		widthField.setAction(okAction);
		p.add(widthField);

		s = Modeler.getInternationalText("HeightLabel");
		label = new JLabel(s != null ? s : "Height", SwingConstants.LEFT);
		p.add(label);
		heightField = new FloatNumberTextField(100, 0.01f, 1000);
		heightField.setMaximumFractionDigits(4);
		heightField.setAction(okAction);
		p.add(heightField);

		// row 2
		s = Modeler.getInternationalText("BorderLabel");
		label = new JLabel(s != null ? s : "Draw border", SwingConstants.LEFT);
		p.add(label);
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(topPanel.getBackground());
		borderComboBox.setSelectedIndex(0);
		p.add(borderComboBox);

		s = Modeler.getInternationalText("BackgroundColorLabel");
		label = new JLabel(s != null ? s : "Background color", SwingConstants.LEFT);
		p.add(label);
		bgComboBox = new ColorComboBox(pageTextBox);
		bgComboBox.setRequestFocusEnabled(false);
		p.add(bgComboBox);

		ModelerUtilities.makeCompactGrid(p, 2, 4, 5, 5, 15, 5);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(p, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("IndexToBeReferedInScriptsLabel");
		p.add(new JLabel((s != null ? s : "Index to be refered in scripts") + ": "));
		indexLabel = new JLabel(pageTextBox.index == -1 ? "to be decided" : "#" + (pageTextBox.index + 1));
		p.add(indexLabel);

		s = Modeler.getInternationalText("TransparencyCheckBox");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setSelected(true);
		p.add(transparentCheckBox);

		textArea = new PastableTextArea();
		scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(500, 300));
		contentPane.add(scrollPane, BorderLayout.CENTER);

	}

}