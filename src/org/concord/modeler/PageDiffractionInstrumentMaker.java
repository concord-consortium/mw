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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.mw2d.models.MesoModel;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.StructureFactor;

/**
 * @author Charles Xie
 * 
 */
class PageDiffractionInstrumentMaker extends ComponentMaker {

	private PageDiffractionInstrument pageDiffractionInstrument;
	private JDialog dialog;
	private JComboBox modelComboBox;
	private JComboBox borderComboBox;
	private JComboBox methodComboBox;
	private JComboBox scalingComboBox;
	private JCheckBox loadScanCheckBox, scriptScanCheckBox;
	private JButton okButton;
	private JPanel contentPane;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(pageDiffractionInstrument);
			}
			else {
				pageDiffractionInstrument.setModelID(pageDiffractionInstrument.page.getComponentPool().getIndex(m));
				m.addModelListener(pageDiffractionInstrument);
			}
		}
	};

	PageDiffractionInstrumentMaker(PageDiffractionInstrument pdi) {
		setObject(pdi);
	}

	void setObject(PageDiffractionInstrument pdi) {
		pageDiffractionInstrument = pdi;
	}

	private boolean confirm() {
		Model m = (Model) modelComboBox.getSelectedItem();
		if (m instanceof MesoModel) {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(pageDiffractionInstrument),
					"Currently, the diffraction device does not apply to the Gay-Berne model.", "Info",
					JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		m.addModelListener(pageDiffractionInstrument);
		pageDiffractionInstrument.setModel((MolecularModel) m);
		pageDiffractionInstrument.setModelID(pageDiffractionInstrument.page.getComponentPool().getIndex(m));
		switch (methodComboBox.getSelectedIndex()) {
		case 0:
			pageDiffractionInstrument.setType(StructureFactor.X_RAY);
			break;
		case 1:
			pageDiffractionInstrument.setType(StructureFactor.NEUTRON);
			break;
		}
		switch (scalingComboBox.getSelectedIndex()) {
		case 0:
			pageDiffractionInstrument.setLevelOfDetails(StructureFactor.LINEAR_SCALING);
			break;
		case 1:
			pageDiffractionInstrument.setLevelOfDetails(StructureFactor.LOG_SCALING);
			break;
		}
		pageDiffractionInstrument.setBorderType((String) borderComboBox.getSelectedItem());
		pageDiffractionInstrument.setChangable(true);
		pageDiffractionInstrument.loadScan = loadScanCheckBox.isSelected();
		pageDiffractionInstrument.scriptScan = scriptScanCheckBox.isSelected();
		pageDiffractionInstrument.page.getSaveReminder().setChanged(true);
		return true;
	}

	void invoke(Page page) {

		pageDiffractionInstrument.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeDiffractionInstrument");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize diffraction instrument", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
				}
			});
		}

		final ComponentPool componentPool = page.getComponentPool();

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();

		if (componentPool != null) {

			synchronized (componentPool) {
				for (ModelCanvas mc : componentPool.getModels()) {
					if (mc.isUsed()) {
						modelComboBox.addItem(mc.getMdContainer().getModel());
					}
				}
			}
			if (pageDiffractionInstrument.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageDiffractionInstrument.modelID);
				mc.getMdContainer().getModel().addModelListener(pageDiffractionInstrument);
				modelComboBox.setSelectedItem(mc.getMdContainer().getModel());
			}
			else {
				Model m = (Model) modelComboBox.getSelectedItem();
				pageDiffractionInstrument.setModelID(componentPool.getIndex(m));
				if (m != null)
					m.addModelListener(pageDiffractionInstrument);
			}
			modelComboBox.addItemListener(modelSelectionListener);

		}

		borderComboBox.setSelectedItem(pageDiffractionInstrument.getBorderType());
		switch (pageDiffractionInstrument.getType()) {
		case StructureFactor.X_RAY:
			methodComboBox.setSelectedIndex(0);
			break;
		case StructureFactor.NEUTRON:
			methodComboBox.setSelectedIndex(1);
			break;
		}
		switch (pageDiffractionInstrument.getLevelOfDetails()) {
		case StructureFactor.LINEAR_SCALING:
			scalingComboBox.setSelectedIndex(0);
			break;
		case StructureFactor.LOG_SCALING:
			scalingComboBox.setSelectedIndex(1);
			break;
		}
		loadScanCheckBox.setSelected(pageDiffractionInstrument.loadScan);
		scriptScanCheckBox.setSelected(pageDiffractionInstrument.scriptScan);
		okButton.setEnabled(modelComboBox.getItemCount() > 0);

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel = !confirm();
				dialog.dispose();
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
				dialog.setVisible(false);
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new GridLayout(4, 1, 3, 3));
		s = Modeler.getInternationalText("SelectModelLabel");
		p1.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		s = Modeler.getInternationalText("SelectTechnique");
		p1.add(new JLabel(s != null ? s : "Select a technique", SwingConstants.LEFT));
		s = Modeler.getInternationalText("SelectLevelOfDetails");
		p1.add(new JLabel(s != null ? s : "Select level of details", SwingConstants.LEFT));
		s = Modeler.getInternationalText("BorderLabel");
		p1.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		p.add(p1, BorderLayout.WEST);

		p1 = new JPanel(new GridLayout(4, 1, 3, 3));

		modelComboBox = new JComboBox();
		modelComboBox.setPreferredSize(new Dimension(200, 18));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this diffraction instrument will work with.");
		p1.add(modelComboBox);

		methodComboBox = new JComboBox(new Object[] { "X-ray", "Neutron" });
		methodComboBox.setToolTipText("Select a diffraction technique");
		p1.add(methodComboBox);

		scalingComboBox = new JComboBox(new Object[] { "Linear", "Logarithmic" });
		scalingComboBox
				.setToolTipText("<html>Select a scaling method to show different levels of details.<br>The logrithmic mode will show more details than the linear one.</html>");
		p1.add(scalingComboBox);

		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p1.getBackground());
		borderComboBox.setToolTipText("Select the border type for this instrument.");
		p1.add(borderComboBox);

		p.add(p1, BorderLayout.CENTER);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		p1.setBorder(BorderFactory.createEtchedBorder());
		p.add(p1, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("ScanAfterLoading");
		loadScanCheckBox = new JCheckBox(s != null ? s : "Scan after loading");
		loadScanCheckBox.setSelected(false);
		loadScanCheckBox
				.setToolTipText("<html>Select if you wish an X-ray image be generated immediately after the model is loaded.</html>");
		p1.add(loadScanCheckBox);

		s = Modeler.getInternationalText("ScanAfterScriptExecution");
		scriptScanCheckBox = new JCheckBox(s != null ? s : "Scan after script execution");
		scriptScanCheckBox.setSelected(false);
		scriptScanCheckBox
				.setToolTipText("<html>Select if you wish an X-ray image be generated immediately after scripts have been executed.</html>");
		p1.add(scriptScanCheckBox);

	}

}