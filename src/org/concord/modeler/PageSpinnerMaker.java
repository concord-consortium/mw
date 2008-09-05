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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import org.concord.modeler.event.AbstractChange;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.MDModel;

/**
 * @author Charles Xie
 * 
 */
class PageSpinnerMaker extends ComponentMaker {

	private PageSpinner pageSpinner;
	private static Font smallFont;
	private JDialog dialog;
	private JTextField nameField;
	private JTextField toolTipField;
	private RealNumberTextField maxField, minField, valueField, stepField;
	private JComboBox modelComboBox, actionComboBox;
	private JCheckBox disabledAtRunCheckBox, disabledAtScriptCheckBox;
	private JButton okButton;
	private JTextArea scriptArea;
	private JLabel scriptLabel, exampleLabel;
	private JPanel contentPane;
	private JTextComponent focusTextComponent;

	PageSpinnerMaker(PageSpinner ps) {
		setObject(ps);
	}

	void setObject(PageSpinner ps) {
		pageSpinner = ps;
	}

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Object o = modelComboBox.getSelectedItem();
			if (o instanceof BasicModel) {
				BasicModel m = (BasicModel) o;
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					m.removeModelListener(pageSpinner);
				}
				else {
					if (m instanceof MDModel) {
						pageSpinner.setModelID(pageSpinner.page.getComponentPool().getIndex(m));
					}
					else if (m instanceof Embeddable) {
						pageSpinner.setModelID(((Embeddable) m).getIndex());
					}
					m.addModelListener(pageSpinner);
					fillActionComboBox();
				}
			}
		}
	};

	private ItemListener actionSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED)
				return;
			AbstractChange c = (AbstractChange) actionComboBox.getSelectedItem();
			minField.setValue(c.getMinimum());
			maxField.setValue(c.getMaximum());
			valueField.setValue(c.getValue());
			stepField.setValue(c.getStepSize());
			pageSpinner.removeChangeListeners();
			pageSpinner.setMinimum(c.getMinimum());
			pageSpinner.setMaximum(c.getMaximum());
			// the small factor 10^-6 in the following is added to ensure that the number
			// is rounded correctly to the number model of the spinner
			pageSpinner.setValue(c.getValue() - (c.getValue() + 0.000001) % c.getStepSize());
			pageSpinner.setStepSize(c.getStepSize());
			pageSpinner.spinner.addChangeListener(c);
			pageSpinner.setLabel(c.toString());
			nameField.setText(c.toString());
			toolTipField.setText(c.toString());
			setScriptArea();
		}
	};

	private boolean confirm() {
		if (maxField.getValue() <= minField.getValue()) {
			JOptionPane.showMessageDialog(dialog, "Upper bound must be greater than lower bound.", "Input error",
					JOptionPane.ERROR_MESSAGE);
			focusTextComponent = maxField;
			return false;
		}
		pageSpinner.setLabel(nameField.getText());
		SpinnerNumberModel m = (SpinnerNumberModel) pageSpinner.spinner.getModel();
		m.setMaximum(new Double(maxField.getValue()));
		m.setMinimum(new Double(minField.getValue()));
		m.setStepSize(new Double(stepField.getValue()));
		BasicModel bm = (BasicModel) modelComboBox.getSelectedItem();
		bm.addModelListener(pageSpinner);
		pageSpinner.setModelClass(bm.getClass().getName());
		if (bm instanceof MDModel) {
			pageSpinner.setModelID(pageSpinner.page.getComponentPool().getIndex(bm));
		}
		else if (bm instanceof Embeddable) {
			pageSpinner.setModelID(((Embeddable) bm).getIndex());
		}
		pageSpinner.spinner.addChangeListener((AbstractChange) actionComboBox.getSelectedItem());
		pageSpinner.autoSize();
		pageSpinner.setDisabledAtRun(disabledAtRunCheckBox.isSelected());
		pageSpinner.setDisabledAtScript(disabledAtScriptCheckBox.isSelected());
		if (scriptArea.getText() != null && !scriptArea.getText().trim().equals("")) {
			pageSpinner.setScript(scriptArea.getText());
		}
		pageSpinner.setToolTipText(toolTipField.getText());
		pageSpinner.page.getSaveReminder().setChanged(true);
		pageSpinner.page.settleComponentSize();
		return true;
	}

	void invoke(Page page) {

		pageSpinner.page = page;
		page.deselect();
		focusTextComponent = null;
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeSpinnerDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize spinner", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					if (focusTextComponent == null) {
						nameField.selectAll();
						nameField.requestFocusInWindow();
					}
					else {
						focusTextComponent.selectAll();
						focusTextComponent.requestFocusInWindow();
					}
				}
			});
		}

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();
		actionComboBox.removeItemListener(actionSelectionListener);

		minField.setValue((Double) pageSpinner.getMinimum());
		maxField.setValue((Double) pageSpinner.getMaximum());
		valueField.setValue((Double) pageSpinner.getValue());
		stepField.setValue((Double) pageSpinner.getStepSize());

		// add legacy MD models to the model list
		final ComponentPool componentPool = page.getComponentPool();
		synchronized (componentPool) {
			for (ModelCanvas mc : componentPool.getModels()) {
				if (mc.isUsed()) {
					modelComboBox.addItem(mc.getContainer().getModel());
				}
			}
		}

		// add target models to the model list
		for (Class c : ModelCommunicator.targetClass) {
			Map map = page.getEmbeddedComponent(c);
			if (map != null && !map.isEmpty()) {
				for (Object o : map.keySet()) {
					modelComboBox.addItem(map.get(o));
				}
			}
		}

		if (pageSpinner.isTargetClass()) {
			if (pageSpinner.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageSpinner.modelClass), pageSpinner.modelID);
					if (o != null)
						modelComboBox.setSelectedItem(o);
				}
				catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			else {
				BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
				if (m instanceof Embeddable)
					pageSpinner.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageSpinner.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageSpinner.modelID);
				modelComboBox.setSelectedItem(mc.getContainer().getModel());
			}
			else {
				BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
				pageSpinner.setModelID(componentPool.getIndex(m));
			}
		}
		modelComboBox.addItemListener(modelSelectionListener);

		fillActionComboBox();
		actionComboBox.addItemListener(actionSelectionListener);

		nameField.setText(pageSpinner.label.getText());
		toolTipField.setText(pageSpinner.getToolTipText());
		disabledAtRunCheckBox.setSelected(pageSpinner.disabledAtRun);
		disabledAtScriptCheckBox.setSelected(pageSpinner.disabledAtScript);
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && actionComboBox.getItemCount() > 0);

		setScriptArea();

		dialog.setVisible(true);

	}

	private void setScriptArea() {
		String s = null;
		AbstractChange c = pageSpinner.getChange();
		if (c != null) {
			s = (String) c.getProperty(AbstractChange.SHORT_DESCRIPTION);
		}
		else {
			if (actionComboBox.getSelectedItem() != null)
				s = actionComboBox.getSelectedItem().toString();
		}
		if (s == null)
			return;
		boolean isScripted = isScriptActionKey(s);
		scriptArea.setEnabled(isScripted);
		scriptLabel.setEnabled(isScripted);
		exampleLabel.setEnabled(isScripted);
		if (isScripted) {
			scriptArea.setText(pageSpinner.getScript());
			scriptArea.setCaretPosition(0);
		}
		else {
			scriptArea.setText(null);
		}
	}

	private void fillActionComboBox() {
		actionComboBox.removeAllItems();
		Object o = modelComboBox.getSelectedItem();
		if (o instanceof BasicModel) {
			BasicModel model = (BasicModel) o;
			Map changeMap = model.getChanges();
			if (changeMap != null) {
				synchronized (changeMap) {
					for (Object x : changeMap.values()) {
						actionComboBox.addItem(x);
					}
				}
				Object scriptAction = getScriptAction(changeMap);
				if (scriptAction != null) {
					actionComboBox.setSelectedItem(scriptAction);
				}
				ChangeListener[] listeners = pageSpinner.spinner.getChangeListeners();
				if (listeners.length > 1) {
					for (ChangeListener cl : listeners) {
						if (!(cl instanceof JSpinner.DefaultEditor)) {
							actionComboBox.setSelectedItem(cl);
						}
					}
				}
				else {
					AbstractChange c = (AbstractChange) actionComboBox.getSelectedItem();
					minField.setValue(c.getMinimum());
					maxField.setValue(c.getMaximum());
					valueField.setValue(c.getValue());
					stepField.setValue(c.getStepSize());
					pageSpinner.removeChangeListeners();
					pageSpinner.setMinimum(c.getMinimum());
					pageSpinner.setMaximum(c.getMaximum());
					pageSpinner.setStepSize(c.getStepSize());
					pageSpinner.setValue(c.getValue());
				}
			}
		}
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

		p = new JPanel(new BorderLayout(10, 5));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.NORTH);

		JPanel p1 = new JPanel(new GridLayout(4, 1, 5, 5));
		s = Modeler.getInternationalText("SelectModelLabel");
		p1.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		s = Modeler.getInternationalText("SelectVariableLabel");
		p1.add(new JLabel(s != null ? s : "Select a variable", SwingConstants.LEFT));
		s = Modeler.getInternationalText("TextLabel");
		p1.add(new JLabel(s != null ? s : "Text", SwingConstants.LEFT));
		s = Modeler.getInternationalText("ToolTipLabel");
		p1.add(new JLabel(s != null ? s : "Tool tip", SwingConstants.LEFT));
		p.add(p1, BorderLayout.WEST);

		p1 = new JPanel(new GridLayout(4, 1, 5, 5));

		modelComboBox = new JComboBox();
		if (smallFont == null)
			smallFont = new Font(modelComboBox.getFont().getFamily(), modelComboBox.getFont().getStyle(), 10);
		modelComboBox.setFont(smallFont);
		modelComboBox.setRenderer(new LabelRenderer());
		modelComboBox.setPreferredSize(new Dimension(200, 20));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this spinner button will interact with.");
		p1.add(modelComboBox);

		actionComboBox = new JComboBox();
		actionComboBox.setFont(smallFont);
		actionComboBox.setToolTipText("Select the variable this spinner button will control.");
		p1.add(actionComboBox);

		nameField = new JTextField();
		nameField.setToolTipText("Type in the text that will appear to the left of this spinner button.");
		nameField.addActionListener(okListener);
		p1.add(nameField);

		toolTipField = new JTextField();
		toolTipField.setToolTipText("Type in the text that will appear as the tool tip.");
		toolTipField.addActionListener(okListener);
		p1.add(toolTipField);

		p.add(p1, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new BorderLayout(10, 10));
		p.add(p2, BorderLayout.SOUTH);

		p1 = new JPanel(new GridLayout(2, 4, 5, 5));
		p2.add(p1, BorderLayout.CENTER);

		s = Modeler.getInternationalText("UpperBoundLabel");
		p1.add(new JLabel(s != null ? s : "Upper bound", SwingConstants.LEFT));
		maxField = new RealNumberTextField(1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		maxField.setToolTipText("Type in the upper bound of the range this spinner controls.");
		maxField.addActionListener(okListener);
		p1.add(maxField);

		s = Modeler.getInternationalText("LowerBoundLabel");
		p1.add(new JLabel(s != null ? s : "Lower bound", SwingConstants.LEFT));
		minField = new RealNumberTextField(-1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		minField.setToolTipText("Type in the lower bound of the range this spinner controls.");
		minField.addActionListener(okListener);
		p1.add(minField);

		s = Modeler.getInternationalText("CurrentValueLabel");
		p1.add(new JLabel(s != null ? s : "Current value", SwingConstants.LEFT));
		valueField = new RealNumberTextField(-1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		valueField.setEnabled(false);
		valueField.setToolTipText("The current value of this spinner.");
		p1.add(valueField);

		s = Modeler.getInternationalText("StepSizeLabel");
		p1.add(new JLabel(s != null ? s : "Set step size", SwingConstants.LEFT));
		stepField = new RealNumberTextField(0.1f, -Float.MAX_VALUE, Float.MAX_VALUE);
		stepField.setToolTipText("Type in the step size for this spinner.");
		stepField.addActionListener(okListener);
		p1.add(stepField);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		p1.setBorder(BorderFactory.createEtchedBorder());

		s = Modeler.getInternationalText("DisabledAtRunCheckBox");
		disabledAtRunCheckBox = new JCheckBox(s != null ? s : "Disabled while model is running");
		disabledAtRunCheckBox.setSelected(false);
		disabledAtRunCheckBox
				.setToolTipText("<html>Select if you wish this spinner to be disabled while the model is running,<br>and to be enabled when the model stops.</html>");
		p1.add(disabledAtRunCheckBox);

		s = Modeler.getInternationalText("DisabledAtScriptCheckBox");
		disabledAtScriptCheckBox = new JCheckBox(s != null ? s : "Disabled while scripts are running");
		disabledAtScriptCheckBox.setSelected(false);
		disabledAtScriptCheckBox
				.setToolTipText("<html>Select if you wish this spinner to be disabled while scripts are running,<br>and to be enabled when scripts end.</html>");
		p1.add(disabledAtScriptCheckBox);

		p2.add(p1, BorderLayout.SOUTH);

		p = new JPanel(new BorderLayout(5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		s = Modeler.getInternationalText("EnterScriptForSpinnerLabel");
		scriptLabel = new JLabel(s != null ? s : "Enter scripts (refer current value as %val):");
		scriptLabel.setEnabled(false);
		p.add(scriptLabel, BorderLayout.NORTH);
		scriptArea = new PastableTextArea(6, 10);
		scriptArea.setEnabled(false);
		scriptArea.setBorder(BorderFactory.createLoweredBevelBorder());
		p.add(new JScrollPane(scriptArea), BorderLayout.CENTER);
		exampleLabel = new JLabel("e.g. set atom[2].charge %val");
		exampleLabel.setEnabled(false);
		p.add(exampleLabel, BorderLayout.SOUTH);

	}

}