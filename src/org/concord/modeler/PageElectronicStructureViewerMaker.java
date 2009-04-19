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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.mw2d.event.UpdateEvent;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class PageElectronicStructureViewerMaker extends ComponentMaker {

	private PageElectronicStructureViewer pageElectronicStructureViewer;
	private JComboBox modelComboBox, elementComboBox, borderComboBox, drawTicksComboBox;
	private FloatNumberTextField upperBoundField, lowerBoundField;
	private IntegerTextField widthField, heightField;
	private JTextField titleField;
	private JSpinner ntickSpinner;
	private JCheckBox lockEnergyLevelsCheckBox;
	private JDialog dialog;
	private JButton okButton;
	private ColorComboBox bgColorComboBox;
	private ColorComboBox fgColorComboBox;
	private JPanel contentPane;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(pageElectronicStructureViewer);
			}
			else {
				pageElectronicStructureViewer.setModelID(pageElectronicStructureViewer.page.getComponentPool()
						.getIndex(m));
				m.addModelListener(pageElectronicStructureViewer);
			}
		}
	};

	PageElectronicStructureViewerMaker(PageElectronicStructureViewer pesv) {
		setObject(pesv);
	}

	void setObject(PageElectronicStructureViewer pesv) {
		pageElectronicStructureViewer = pesv;
	}

	private boolean confirm() {
		Model m = (Model) modelComboBox.getSelectedItem();
		if (!(m instanceof MolecularModel)) {
			JOptionPane.showMessageDialog(pageElectronicStructureViewer,
					"This controller is not applied to\nthe mesoscale model yet.", "Electronic Structure Viewer",
					JOptionPane.ERROR_MESSAGE);
			modelComboBox.requestFocusInWindow();
			return false;
		}
		int curTicks = (Integer) ntickSpinner.getValue();
		if (curTicks > heightField.getValue() / 2) {
			JOptionPane
					.showMessageDialog(
							pageElectronicStructureViewer,
							"The spacing between adjacent ticks cannot be less than 2 pixels.\nPlease decrease the number of ticks or set a larger height.",
							"Tick spacing error", JOptionPane.ERROR_MESSAGE);
			heightField.selectAll();
			heightField.requestFocusInWindow();
			return false;
		}
		pageElectronicStructureViewer.removeAllParameterChangeListeners();
		pageElectronicStructureViewer.addParameterChangeListener((MolecularModel) m);
		((MolecularModel) m).addUpdateListener(pageElectronicStructureViewer);
		pageElectronicStructureViewer.setUpperBound(upperBoundField.getValue());
		pageElectronicStructureViewer.setLowerBound(lowerBoundField.getValue());
		if (pageElectronicStructureViewer.getUpperBound() <= pageElectronicStructureViewer.getLowerBound()) {
			JOptionPane.showMessageDialog(pageElectronicStructureViewer,
					"Input error: upper bound must be greater than lower bound.", "Electronic Structure Viewer",
					JOptionPane.ERROR_MESSAGE);
			upperBoundField.selectAll();
			upperBoundField.requestFocusInWindow();
			return false;
		}
		pageElectronicStructureViewer.setTitle(titleField.getText());
		pageElectronicStructureViewer.setNumberOfTicks(curTicks);
		pageElectronicStructureViewer.setDrawTicks(drawTicksComboBox.getSelectedIndex() == 0);
		m.addModelListener(pageElectronicStructureViewer);
		pageElectronicStructureViewer.elementID = elementComboBox.getSelectedIndex();
		pageElectronicStructureViewer.setElement(((MolecularModel) m)
				.getElement(pageElectronicStructureViewer.elementID));
		pageElectronicStructureViewer.scaleViewer();
		pageElectronicStructureViewer.setModelID(pageElectronicStructureViewer.page.getComponentPool().getIndex(m));
		pageElectronicStructureViewer.setBorderType((String) borderComboBox.getSelectedItem());
		pageElectronicStructureViewer.setBackground(bgColorComboBox.getSelectedColor());
		pageElectronicStructureViewer.setForeground(fgColorComboBox.getSelectedColor());
		pageElectronicStructureViewer.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()
				+ pageElectronicStructureViewer.getVerticalMargin() * 2));
		pageElectronicStructureViewer.setChangable(true);
		pageElectronicStructureViewer.page.getSaveReminder().setChanged(true);
		pageElectronicStructureViewer.page.settleComponentSize();
		pageElectronicStructureViewer.viewUpdated(new UpdateEvent(m));
		return true;
	}

	void invoke(Page page) {

		pageElectronicStructureViewer.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeElectronicStructure");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize electronic structure", true);
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

		final ComponentPool componentPool = page.getComponentPool();

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();

		float min = -5, max = 0;

		if (componentPool != null) {

			synchronized (componentPool) {
				for (ModelCanvas mc : componentPool.getModels()) {
					if (mc.isUsed()) {
						modelComboBox.addItem(mc.getMdContainer().getModel());
					}
				}
			}
			if (pageElectronicStructureViewer.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageElectronicStructureViewer.modelID);
				modelComboBox.setSelectedItem(mc.getMdContainer().getModel());
			}
			else {
				Model m = (Model) modelComboBox.getSelectedItem();
				pageElectronicStructureViewer.setModelID(componentPool.getIndex(m));
			}
			if (modelComboBox.getSelectedItem() != null) {
				Element elem = ((MolecularModel) modelComboBox.getSelectedItem())
						.getElement(pageElectronicStructureViewer.elementID);
				min = elem.getElectronicStructure().getLowestEnergy();
				max = elem.getElectronicStructure().getHighestEnergy();
			}
			modelComboBox.addItemListener(modelSelectionListener);

		}

		if (pageElectronicStructureViewer.isPreferredSizeSet()) {
			widthField.setValue(pageElectronicStructureViewer.getPreferredSize().width);
			heightField.setValue(pageElectronicStructureViewer.getPreferredSize().height
					- pageElectronicStructureViewer.getVerticalMargin() * 2);
		}
		ntickSpinner.setValue(new Integer(pageElectronicStructureViewer.getNumberOfTicks()));
		drawTicksComboBox.setSelectedIndex(pageElectronicStructureViewer.getDrawTicks() ? 0 : 1);
		elementComboBox.setSelectedIndex(pageElectronicStructureViewer.elementID);
		titleField.setText(pageElectronicStructureViewer.getTitle());
		upperBoundField.setValue(pageElectronicStructureViewer.isUpperBoundSet() ? pageElectronicStructureViewer
				.getUpperBound() : max);
		lowerBoundField.setValue(pageElectronicStructureViewer.isLowerBoundSet() ? pageElectronicStructureViewer
				.getLowerBound() : min);
		borderComboBox.setSelectedItem(pageElectronicStructureViewer.getBorderType());
		bgColorComboBox.setColor(pageElectronicStructureViewer.getBackground());
		fgColorComboBox.setColor(pageElectronicStructureViewer.getForeground());
		lockEnergyLevelsCheckBox.setSelected(pageElectronicStructureViewer.getLockEnergyLevels());
		okButton.setEnabled(modelComboBox.getItemCount() > 0);

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (confirm()) {
					dialog.dispose();
					cancel = false;
				}
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

		p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		s = Modeler.getInternationalText("LockEnergyLevels");
		lockEnergyLevelsCheckBox = new JCheckBox(s != null ? s : "Lock Energy Levels");
		lockEnergyLevelsCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				pageElectronicStructureViewer.setLockEnergyLevels(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		p.add(lockEnergyLevelsCheckBox);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		s = Modeler.getInternationalText("SelectModelLabel");
		p.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		modelComboBox = new JComboBox();
		modelComboBox.setPreferredSize(new Dimension(200, 20));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this component will interact with.");
		p.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("SelectGroup");
		p.add(new JLabel(s != null ? s : "Select a group", SwingConstants.LEFT));
		elementComboBox = new JComboBox(new String[] { "Nt", "Pl", "Ws", "Ck" });
		elementComboBox.setToolTipText("Select a group of particles that form this electronic structure.");
		elementComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.DESELECTED)
					return;
				if (!(modelComboBox.getSelectedItem() instanceof MolecularModel))
					return;
				MolecularModel m = (MolecularModel) modelComboBox.getSelectedItem();
				Element element = m.getElement((String) elementComboBox.getSelectedItem());
				upperBoundField.setValue(element.getElectronicStructure().getHighestEnergy());
				lowerBoundField.setValue(element.getElectronicStructure().getLowestEnergy());
			}
		});
		p.add(elementComboBox);

		// row 3
		s = Modeler.getInternationalText("SetAShortTitle");
		p.add(new JLabel(s != null ? s : "Set a short title", SwingConstants.LEFT));
		titleField = new JTextField();
		titleField.setToolTipText("Type in a short string that describes this component.");
		titleField.addActionListener(okListener);
		p.add(titleField);

		// row 4
		s = Modeler.getInternationalText("EnergyUpperBound");
		p.add(new JLabel(s != null ? s : "Upper bound of energy", SwingConstants.LEFT));
		upperBoundField = new FloatNumberTextField(-0.5f, -10, 0);
		upperBoundField.setToolTipText("Type in the upper bound of energy (in eV).");
		upperBoundField.addActionListener(okListener);
		p.add(upperBoundField);

		// row 5
		s = Modeler.getInternationalText("EnergyLowerBound");
		p.add(new JLabel(s != null ? s : "Lower bound of energy", SwingConstants.LEFT));
		lowerBoundField = new FloatNumberTextField(-1.0f, -100, 0);
		lowerBoundField.setToolTipText("Type in the lower bound of energy (in eV).");
		lowerBoundField.addActionListener(okListener);
		p.add(lowerBoundField);

		// row 6
		s = Modeler.getInternationalText("DisplaySnapTicks");
		p.add(new JLabel(s != null ? s : "Display snap ticks", SwingConstants.LEFT));
		drawTicksComboBox = new JComboBox(new Object[] { "Yes", "No" });
		drawTicksComboBox.setSelectedIndex(1);
		drawTicksComboBox.setToolTipText("Select yes to draw tick marks that the energy levels will snap to.");
		p.add(drawTicksComboBox);

		// row 7
		s = Modeler.getInternationalText("NumberOfSnapTicks");
		p.add(new JLabel(s != null ? s : "Number of snap ticks", SwingConstants.LEFT));
		ntickSpinner = new JSpinner(new SpinnerNumberModel(new Integer(20), new Integer(10), new Integer(100),
				new Integer(1)));
		ntickSpinner
				.setToolTipText("Set the number of ticks to divide the energy scope between the lower bound and the upper bound.\nAn energy level will snap to the closest tick next to the position it is dropped.");
		// ntickSpinner.addActionListener(okListener);
		p.add(ntickSpinner);

		// row 8
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(200, 100, 800);
		widthField.setToolTipText("Type in an integer to set the width of this component.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 9
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height of level zone", SwingConstants.LEFT));
		heightField = new IntegerTextField(200, 100, 800);
		heightField.setToolTipText("Type in an integer to set the height of the energy level zone.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 10
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p.add(new JLabel(s != null ? s : "Background", SwingConstants.LEFT));
		bgColorComboBox = new ColorComboBox(pageElectronicStructureViewer);
		bgColorComboBox.setSelectedIndex(6);
		bgColorComboBox.setRequestFocusEnabled(false);
		bgColorComboBox.setToolTipText("Select background color.");
		p.add(bgColorComboBox);

		// row 11
		s = Modeler.getInternationalText("ForegroundColorLabel");
		p.add(new JLabel(s != null ? s : "Foreground", SwingConstants.LEFT));
		fgColorComboBox = new ColorComboBox(pageElectronicStructureViewer);
		fgColorComboBox.setSelectedIndex(6);
		fgColorComboBox.setRequestFocusEnabled(false);
		fgColorComboBox.setToolTipText("Select foreground color.");
		p.add(fgColorComboBox);

		// row 12
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type for this component.");
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 12, 2, 5, 5, 15, 5);

	}

}