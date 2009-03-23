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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.mw2d.PhotonSpectrometer;
import org.concord.mw2d.models.MesoModel;

/**
 * @author Charles Xie
 * 
 */
class PagePhotonSpectrometerMaker extends ComponentMaker {

	private PagePhotonSpectrometer pagePhotonSpectrometer;
	private JDialog dialog;
	private JComboBox modelComboBox, borderComboBox, methodComboBox;
	private FloatNumberTextField upperBoundField, lowerBoundField;
	private IntegerTextField widthField, heightField;
	private JSpinner ntickSpinner;
	private JButton okButton;
	private JPanel contentPane;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(pagePhotonSpectrometer);
			}
			else {
				pagePhotonSpectrometer.setModelID(pagePhotonSpectrometer.page.getComponentPool().getIndex(m));
				m.addModelListener(pagePhotonSpectrometer);
			}
		}
	};

	PagePhotonSpectrometerMaker(PagePhotonSpectrometer pps) {
		setObject(pps);
	}

	void setObject(PagePhotonSpectrometer pps) {
		pagePhotonSpectrometer = pps;
	}

	private boolean confirm() {
		Model m = (Model) modelComboBox.getSelectedItem();
		if (m instanceof MesoModel) {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(pagePhotonSpectrometer),
					"Currently, the photon spectrometer does not apply to the Gay-Berne model.", "Info",
					JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		m.addModelListener(pagePhotonSpectrometer);
		pagePhotonSpectrometer.setModelID(pagePhotonSpectrometer.page.getComponentPool().getIndex(m));
		switch (methodComboBox.getSelectedIndex()) {
		case 0:
			pagePhotonSpectrometer.setType(PhotonSpectrometer.EMISSION);
			break;
		case 1:
			pagePhotonSpectrometer.setType(PhotonSpectrometer.ABSORPTION);
			break;
		}
		pagePhotonSpectrometer.setBorderType((String) borderComboBox.getSelectedItem());
		pagePhotonSpectrometer.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pagePhotonSpectrometer.setNumberOfTicks(((Integer) ntickSpinner.getValue()).intValue());
		pagePhotonSpectrometer.setLowerBound(lowerBoundField.getValue());
		pagePhotonSpectrometer.setUpperBound(upperBoundField.getValue());
		pagePhotonSpectrometer.setChangable(true);
		pagePhotonSpectrometer.page.getSaveReminder().setChanged(true);
		pagePhotonSpectrometer.page.settleComponentSize();
		return true;
	}

	void invoke(Page page) {

		pagePhotonSpectrometer.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeSpectrometer");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize spectrometer", true);
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
			if (pagePhotonSpectrometer.modelID != -1) {
				ModelCanvas mc = componentPool.get(pagePhotonSpectrometer.modelID);
				mc.getMdContainer().getModel().addModelListener(pagePhotonSpectrometer);
				modelComboBox.setSelectedItem(mc.getMdContainer().getModel());
			}
			else {
				Model m = (Model) modelComboBox.getSelectedItem();
				pagePhotonSpectrometer.setModelID(componentPool.getIndex(m));
				if (m != null)
					m.addModelListener(pagePhotonSpectrometer);
			}
			modelComboBox.addItemListener(modelSelectionListener);

		}

		borderComboBox.setSelectedItem(pagePhotonSpectrometer.getBorderType());
		switch (pagePhotonSpectrometer.getType()) {
		case PhotonSpectrometer.EMISSION:
			methodComboBox.setSelectedIndex(0);
			break;
		case PhotonSpectrometer.ABSORPTION:
			methodComboBox.setSelectedIndex(1);
			break;
		}
		ntickSpinner.setValue(new Integer(pagePhotonSpectrometer.getNumberOfTicks()));
		lowerBoundField.setValue(pagePhotonSpectrometer.getLowerBound());
		upperBoundField.setValue(pagePhotonSpectrometer.getUpperBound());
		if (pagePhotonSpectrometer.isPreferredSizeSet()) {
			widthField.setValue(pagePhotonSpectrometer.getWidth());
			heightField.setValue(pagePhotonSpectrometer.getHeight());
		}
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
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new GridLayout(8, 1, 3, 3));
		s = Modeler.getInternationalText("SelectModelLabel");
		p1.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		s = Modeler.getInternationalText("SelectType");
		p1.add(new JLabel(s != null ? s : "Select type", SwingConstants.LEFT));
		s = Modeler.getInternationalText("WidthLabel");
		p1.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		s = Modeler.getInternationalText("HeightLabel");
		p1.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		s = Modeler.getInternationalText("FrequencyUpperBound");
		p1.add(new JLabel(s != null ? s : "Upper bound of frequency in the unit of eV", SwingConstants.LEFT));
		s = Modeler.getInternationalText("FrequencyLowerBound");
		p1.add(new JLabel(s != null ? s : "Lower bound of frequency in the unit of eV", SwingConstants.LEFT));
		s = Modeler.getInternationalText("NumberOfTickMarks");
		p1.add(new JLabel(s != null ? s : "Number of tick marks", SwingConstants.LEFT));
		s = Modeler.getInternationalText("BorderLabel");
		p1.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		p.add(p1, BorderLayout.WEST);

		p1 = new JPanel(new GridLayout(8, 1, 3, 3));

		modelComboBox = new JComboBox();
		modelComboBox.setPreferredSize(new Dimension(200, 18));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this diffraction instrument will work with.");
		p1.add(modelComboBox);

		methodComboBox = new JComboBox(new Object[] { "Emission", "Absorption" });
		methodComboBox.setToolTipText("Select a type");
		p1.add(methodComboBox);

		widthField = new IntegerTextField(pagePhotonSpectrometer.getWidth() <= 0 ? 400 : pagePhotonSpectrometer
				.getWidth(), 200, 800);
		widthField.setToolTipText("Type in an integer to set the width of this component.");
		widthField.addActionListener(okListener);
		p1.add(widthField);

		heightField = new IntegerTextField(pagePhotonSpectrometer.getHeight() <= 0 ? 50 : pagePhotonSpectrometer
				.getHeight(), 10, 200);
		heightField.setToolTipText("Type in an integer to set the height of this component.");
		heightField.addActionListener(okListener);
		p1.add(heightField);

		upperBoundField = new FloatNumberTextField(20, -100, 100);
		upperBoundField.setToolTipText("Type in the upper bound of frequency (in eV).");
		upperBoundField.addActionListener(okListener);
		p1.add(upperBoundField);

		lowerBoundField = new FloatNumberTextField(0, -100, 100);
		lowerBoundField.setToolTipText("Type in the lower bound of frequency (in eV).");
		lowerBoundField.addActionListener(okListener);
		p1.add(lowerBoundField);

		ntickSpinner = new JSpinner(new SpinnerNumberModel(new Integer(20), new Integer(10), new Integer(100),
				new Integer(1)));
		ntickSpinner.setToolTipText("Set the number of tick marks");
		p1.add(ntickSpinner);

		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p1.getBackground());
		borderComboBox.setToolTipText("Select the border type for this instrument.");
		p1.add(borderComboBox);

		p.add(p1, BorderLayout.CENTER);

	}

}