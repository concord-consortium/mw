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
import java.awt.Color;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.draw.FillMode;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.PastableTextArea;

/**
 * @author Charles Xie
 * 
 */
class PageMolecularViewerMaker extends ComponentMaker {

	private PageMolecularViewer pageMolecularViewer;
	private JDialog dialog;
	private JComboBox borderComboBox;
	private BackgroundComboBox bgComboBox;
	private FloatNumberTextField widthField, heightField;
	private JButton okButton;
	private JTextArea scriptArea1, scriptArea2;
	private JPanel contentPane;

	PageMolecularViewerMaker(PageMolecularViewer pmv) {
		setObject(pmv);
	}

	void setObject(PageMolecularViewer pmv) {
		pageMolecularViewer = pmv;
	}

	private void confirm() {
		pageMolecularViewer.setChangable(true);
		pageMolecularViewer.setBorderType((String) borderComboBox.getSelectedItem());
		pageMolecularViewer.setCustomInitializationScript(scriptArea2.getText());
		pageMolecularViewer.page.getSaveReminder().setChanged(true);
		float w = widthField.getValue();
		float h = heightField.getValue();
		if (w > 0 && w < 1.05f) {
			pageMolecularViewer.setWidthRatio(w);
			w *= pageMolecularViewer.page.getWidth();
			pageMolecularViewer.setWidthRelative(true);
		}
		else {
			pageMolecularViewer.setWidthRelative(false);
		}
		if (h > 0 && h < 1.05f) {
			pageMolecularViewer.setHeightRatio(h);
			h *= pageMolecularViewer.page.getHeight();
			pageMolecularViewer.setHeightRelative(true);
		}
		else {
			pageMolecularViewer.setHeightRelative(false);
		}
		pageMolecularViewer.setViewerSize(new Dimension((int) w, (int) h));
	}

	void invoke(Page page) {

		pageMolecularViewer.setPage(page);
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("MolecularViewerDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize Jmol Viewer", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					scriptArea2.requestFocusInWindow();
				}
			});
		}

		scriptArea1.setText(pageMolecularViewer.getInitializationScript());
		scriptArea2.setText(pageMolecularViewer.getCustomInitializationScript());
		scriptArea2.setCaretPosition(0);
		borderComboBox.setSelectedItem(pageMolecularViewer.getBorderType());
		bgComboBox.setFillMode(pageMolecularViewer.getFillMode());
		if (pageMolecularViewer.widthIsRelative) {
			widthField.setValue(pageMolecularViewer.widthRatio);
		}
		else {
			widthField.setValue(pageMolecularViewer.getPreferredSize().width);
		}
		if (pageMolecularViewer.heightIsRelative) {
			heightField.setValue(pageMolecularViewer.heightRatio);
		}
		else {
			heightField.setValue(pageMolecularViewer.getPreferredSize().height);
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

		JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		contentPane.add(bottomPanel, BorderLayout.SOUTH);

		bottomPanel
				.add(
						new JLabel(
								"<html>&#8224; <font size=2>An input between 0 and 1 sets width relative to page.<br>&#8225; Custom initialization script will be executed following the system-set initialization script<br>after data loading.</font></html>"),
						BorderLayout.CENTER);
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottomPanel.add(p, BorderLayout.SOUTH);

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

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new SpringLayout());

		// row 1
		s = Modeler.getInternationalText("WidthLabel");
		p1.add(new JLabel((s != null ? s : "Width") + " \u2020", SwingConstants.LEFT));
		widthField = new FloatNumberTextField(400, 0.01f, 1200);
		widthField.setToolTipText("A value in (0, 1] will be considered relative to the width of the page.");
		widthField.setMaximumFractionDigits(4);
		widthField.addActionListener(okListener);
		p1.add(widthField);

		// row 2
		s = Modeler.getInternationalText("HeightLabel");
		p1.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new FloatNumberTextField(400, 0.01f, 1000);
		heightField.setToolTipText("A value in (0, 1] will be considered relative to the height of the page.");
		heightField.setMaximumFractionDigits(4);
		heightField.addActionListener(okListener);
		p1.add(heightField);

		// row 3
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p1.add(new JLabel(s != null ? s : "Background effect", SwingConstants.LEFT));
		bgComboBox = new BackgroundComboBox(pageMolecularViewer, ModelerUtilities.colorChooser,
				ModelerUtilities.fillEffectChooser);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background effect.");
		bgComboBox.getColorMenu().addNoFillListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pageMolecularViewer.changeFillMode(FillMode.getNoFillMode());
			}
		});
		bgComboBox.getColorMenu().addFillEffectListeners(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pageMolecularViewer.changeFillMode(bgComboBox.getColorMenu().getFillEffectChooser().getFillMode());
			}
		}, null);
		bgComboBox.getColorMenu().addColorArrayListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pageMolecularViewer.changeFillMode(new FillMode.ColorFill(bgComboBox.getColorMenu().getColor()));
			}
		});
		bgComboBox.getColorMenu().addMoreColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pageMolecularViewer.changeFillMode(new FillMode.ColorFill(bgComboBox.getColorMenu().getColorChooser()
						.getColor()));
			}
		});
		bgComboBox.getColorMenu().addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = pageMolecularViewer.getFillMode();
				Color c = bgComboBox.getColorMenu().getHexInputColor(
						fm instanceof FillMode.ColorFill ? ((FillMode.ColorFill) fm).getColor() : null);
				if (c == null)
					return;
				pageMolecularViewer.changeFillMode(new FillMode.ColorFill(c));
			}
		});
		p1.add(bgComboBox);

		// row 4
		s = Modeler.getInternationalText("BorderLabel");
		p1.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setPreferredSize(new Dimension(150, 20));
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p1.getBackground());
		borderComboBox.setToolTipText("Select the border type for this component.");
		p1.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p1, 4, 2, 5, 5, 10, 2);

		p.add(p1, BorderLayout.NORTH);

		p1 = new JPanel(new BorderLayout(10, 10));
		p.add(p1, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createTitledBorder("Initialization script (system-set, non-editable):"), BorderFactory
				.createEmptyBorder(8, 8, 8, 8)));
		scriptArea1 = new PastableTextArea(5, 5);
		scriptArea1.setBorder(BorderFactory.createLoweredBevelBorder());
		scriptArea1.setEditable(false);
		scriptArea1.setEnabled(false);
		p2.add(new JScrollPane(scriptArea1), BorderLayout.CENTER);
		p1.add(p2, BorderLayout.NORTH);

		p2 = new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createTitledBorder("Custom initialization script (user-set, editable):\u2021"), BorderFactory
				.createEmptyBorder(8, 8, 8, 8)));
		scriptArea2 = new PastableTextArea(5, 10);
		scriptArea2.setBorder(BorderFactory.createLoweredBevelBorder());
		p2.add(new JScrollPane(scriptArea2), BorderLayout.CENTER);
		p1.add(p2, BorderLayout.CENTER);

	}

}