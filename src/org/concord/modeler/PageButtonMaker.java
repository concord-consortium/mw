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
import java.util.Iterator;
import java.util.Map;

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
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class PageButtonMaker extends ComponentMaker {

	private static Font smallFont;

	private PageButton pageButton;
	private JDialog dialog;
	private JCheckBox transparentCheckBox, autoSizeCheckBox, fireOnHoldCheckBox;
	private JCheckBox disabledAtRunCheckBox, disabledAtScriptCheckBox;
	private JComboBox modelComboBox, actionComboBox, borderComboBox;
	private ColorComboBox bgComboBox;
	private JLabel incrementLabel;
	private RealNumberTextField incrementField;
	private IntegerTextField widthField, heightField;
	private JButton okButton;
	private JTextField nameField;
	private JTextField toolTipField;
	private JTextArea scriptArea;
	private JLabel scriptLabel;
	private JPanel contentPane;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Object o = ((JComboBox) e.getSource()).getSelectedItem();
			if (o instanceof BasicModel) {
				BasicModel m = (BasicModel) o;
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					m.removeModelListener(pageButton);
				}
				else {
					if (m instanceof MDModel) {
						pageButton.setModelID(pageButton.page.getComponentPool().getIndex(m));
					}
					else if (m instanceof Embeddable) {
						pageButton.setModelID(((Embeddable) m).getIndex());
					}
					m.addModelListener(pageButton);
					fillActionComboBox();
				}
			}
		}
	};

	private ItemListener actionSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED)
				return;
			pageButton.setAction((Action) actionComboBox.getSelectedItem());
			setScriptArea();
			setIncrementParameter();
			nameField.setText(pageButton.getAction().toString());
			toolTipField.setText(pageButton.getAction().toString());
		}
	};

	PageButtonMaker(PageButton pb) {
		setObject(pb);
	}

	void setObject(PageButton pb) {
		pageButton = pb;
	}

	private void confirm() {
		if (!Page.isNativeLookAndFeelUsed()) {
			pageButton.setOpaque(!transparentCheckBox.isSelected());
			pageButton.setBorderType((String) borderComboBox.getSelectedItem());
		}
		pageButton.setBackground(bgComboBox.getSelectedColor());
		pageButton.setContinuousFire(fireOnHoldCheckBox.isSelected());
		pageButton.setDisabledAtRun(disabledAtRunCheckBox.isSelected());
		pageButton.setDisabledAtScript(disabledAtScriptCheckBox.isSelected());
		Object o = modelComboBox.getSelectedItem();
		BasicModel m = (BasicModel) o;
		m.addModelListener(pageButton);
		pageButton.setModelClass(m.getClass().getName());
		if (o instanceof MDModel) {
			pageButton.setModelID(pageButton.page.getComponentPool().getIndex(m));
		}
		else if (o instanceof Embeddable) {
			pageButton.setModelID(((Embeddable) o).getIndex());
		}
		o = actionComboBox.getSelectedItem();
		if (o instanceof Action)
			pageButton.setAction((Action) o);
		pageButton.putClientProperty("script", scriptArea.getText());
		pageButton.setText(nameField.getText());
		pageButton.setToolTipText(toolTipField.getText());
		pageButton.getAction().putValue("source", this);
		if (incrementField.isEnabled() && !incrementField.getText().trim().equals("")) {
			pageButton.getAction().putValue("increment", new Double(incrementField.getValue()));
		}
		if (!pageButton.autoSize) {
			pageButton.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		}
		pageButton.page.getSaveReminder().setChanged(true);
		pageButton.page.settleComponentSize();
	}

	void invoke(Page page) {

		pageButton.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeButtonDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize button", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					nameField.selectAll();
					nameField.requestFocus();
				}
			});
		}

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();
		actionComboBox.removeItemListener(actionSelectionListener);

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

		if (pageButton.isTargetClass()) {
			if (pageButton.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageButton.modelClass), pageButton.modelID);
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
					pageButton.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageButton.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageButton.modelID);
				modelComboBox.setSelectedItem(mc.getContainer().getModel());
			}
			else {
				BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
				pageButton.setModelID(componentPool.getIndex(m));
			}
		}
		modelComboBox.addItemListener(modelSelectionListener);

		fillActionComboBox();
		if (pageButton.getAction() != null)
			actionComboBox.setSelectedItem(pageButton.getAction());
		actionComboBox.addItemListener(actionSelectionListener);

		String t = pageButton.getText();
		nameField.setText(t != null && !t.trim().equals("") ? t
				: (actionComboBox.getSelectedItem() != null ? actionComboBox.getSelectedItem().toString() : null));
		toolTipField.setText(pageButton.getToolTipText());
		if (!Page.isNativeLookAndFeelUsed()) {
			transparentCheckBox.setSelected(!pageButton.isOpaque());
			borderComboBox.setSelectedItem(pageButton.getBorderType());
		}
		bgComboBox.setColor(pageButton.getBackground());
		fireOnHoldCheckBox.setSelected(pageButton.continuousFire);
		disabledAtRunCheckBox.setSelected(pageButton.disabledAtRun);
		disabledAtScriptCheckBox.setSelected(pageButton.disabledAtScript);
		autoSizeCheckBox.setSelected(pageButton.autoSize);
		if (pageButton.isPreferredSizeSet()) {
			widthField.setValue(pageButton.getWidth());
			heightField.setValue(pageButton.getHeight());
		}
		widthField.setEnabled(!pageButton.autoSize);
		heightField.setEnabled(!pageButton.autoSize);
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && actionComboBox.getItemCount() > 0);

		setScriptArea();
		setIncrementParameter();

		dialog.setVisible(true);

	}

	private void fillActionComboBox() {
		actionComboBox.removeAllItems();
		Object o = modelComboBox.getSelectedItem();
		if (o instanceof BasicModel) {
			BasicModel model = (BasicModel) o;
			Map actionMap = model.getActions();
			if (actionMap == null)
				return;
			if (model instanceof MDModel) {
				Object key;
				Object actionObject;
				synchronized (actionMap) {
					for (Iterator it = actionMap.keySet().iterator(); it.hasNext();) {
						key = it.next();
						actionObject = actionMap.get(key);
						if (!((MDModel) model).getRecorderDisabled()) {
							if (key.equals("Remove a particle")) {
								continue;
							}
							if (model instanceof MolecularModel) {
								if (key.equals("Insert Nt") || key.equals("Insert Pl") || key.equals("Insert Ws")
										|| key.equals("Insert Ck")) {
									continue;
								}
							}
						}
						else {
							if (model instanceof MDModel) {
								if (key.equals("Show kinetic, potential and total energies")) {
									continue;
								}
							}
						}
						actionComboBox.addItem(actionObject);
					}
				}
			}
			else {
				synchronized (actionMap) {
					for (Iterator it = actionMap.values().iterator(); it.hasNext();) {
						actionComboBox.addItem(it.next());
					}
				}
			}
			Object scriptAction = getScriptAction(actionMap);
			if (scriptAction != null) {
				actionComboBox.setSelectedItem(scriptAction);
			}
		}
	}

	private void setScriptArea() {
		String s = null;
		if (pageButton.getAction() != null) {
			s = (String) pageButton.getAction().getValue(Action.SHORT_DESCRIPTION);
		}
		else {
			if (actionComboBox.getSelectedItem() != null)
				s = actionComboBox.getSelectedItem().toString();
		}
		if (s == null)
			return;
		boolean isScriptButton = isScriptActionKey(s);
		scriptArea.setEnabled(isScriptButton);
		scriptLabel.setEnabled(isScriptButton);
		scriptArea.setText(isScriptButton ? (String) pageButton.getClientProperty("script") : null);
		if (isScriptButton) {
			scriptArea.requestFocusInWindow();
			scriptArea.setCaretPosition(0);
		}
	}

	private void setIncrementParameter() {
		if (pageButton.getAction() == null)
			return;
		Object o = pageButton.getAction().getValue("increment");
		if (o instanceof Double) {
			incrementLabel.setEnabled(true);
			incrementField.setEnabled(true);
			incrementField.setValue(((Double) o).doubleValue());
		}
		else {
			incrementLabel.setEnabled(false);
			incrementField.setEnabled(false);
			incrementField.setText(null);
		}
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
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		s = Modeler.getInternationalText("SelectModelLabel");
		p.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		modelComboBox = new JComboBox();
		if (smallFont == null)
			smallFont = new Font(modelComboBox.getFont().getFamily(), modelComboBox.getFont().getStyle(), 10);
		modelComboBox.setFont(smallFont);
		modelComboBox.setRenderer(new LabelRenderer());
		modelComboBox.setPreferredSize(new Dimension(200, 16));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this button will interact with.");
		p.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("SelectActionLabel");
		p.add(new JLabel(s != null ? s : "Select an action", SwingConstants.LEFT));
		actionComboBox = new JComboBox();
		actionComboBox.setFont(smallFont);
		actionComboBox.setToolTipText("Select the action for this button.");
		p.add(actionComboBox);

		// row 3
		s = Modeler.getInternationalText("TextLabel");
		p.add(new JLabel(s != null ? s : "Text", SwingConstants.LEFT));
		nameField = new JTextField();
		nameField.setToolTipText("Type in the text that will appear on this button.");
		nameField.addActionListener(okListener);
		p.add(nameField);

		// row 4
		s = Modeler.getInternationalText("ToolTipLabel");
		p.add(new JLabel(s != null ? s : "Tool tip", SwingConstants.LEFT));
		toolTipField = new JTextField();
		toolTipField.setToolTipText("Type in the text that will appear as the tool tip.");
		toolTipField.addActionListener(okListener);
		p.add(toolTipField);

		// row 5
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(pageButton.getWidth() <= 0 ? 100 : pageButton.getWidth(), 10, 400);
		widthField.setEnabled(false);
		widthField.setToolTipText("Type in an integer to set the width of this button, if it will not be auto-sized.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 6
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(pageButton.getHeight() <= 0 ? 24 : pageButton.getHeight(), 10, 400);
		heightField.setEnabled(false);
		heightField
				.setToolTipText("Type in an integer to set the height of this button, if it will not be auto-sized.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 7
		s = Modeler.getInternationalText("BackgroundColorLabel");
		JLabel label = new JLabel(s != null ? s : "Background color", SwingConstants.LEFT);
		label.setEnabled(!Page.isNativeLookAndFeelUsed());
		p.add(label);
		bgComboBox = new ColorComboBox(pageButton);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color for this button, if it is not transparent.");
		bgComboBox.setEnabled(!Page.isNativeLookAndFeelUsed());
		p.add(bgComboBox);

		// row 8
		s = Modeler.getInternationalText("BorderLabel");
		label = new JLabel(s != null ? s : "Border", SwingConstants.LEFT);
		label.setEnabled(!Page.isNativeLookAndFeelUsed());
		p.add(label);
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type for this button.");
		borderComboBox.setEnabled(!Page.isNativeLookAndFeelUsed());
		p.add(borderComboBox);

		// row 9
		incrementLabel = new JLabel("Increment", SwingConstants.LEFT);
		incrementLabel.setEnabled(false);
		p.add(incrementLabel);
		incrementField = new RealNumberTextField();
		incrementField.addActionListener(okListener);
		incrementField.setEnabled(false);
		incrementField
				.setToolTipText("For some actions, type in the increment to be added each time this button is clicked.");
		p.add(incrementField);

		ModelerUtilities.makeCompactGrid(p, 9, 2, 5, 5, 10, 2);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new GridLayout(3, 2));
		p1.setBorder(BorderFactory.createEtchedBorder());
		p.add(p1, BorderLayout.NORTH);

		s = Modeler.getInternationalText("TransparencyCheckBox");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setEnabled(!Page.isNativeLookAndFeelUsed());
		transparentCheckBox.setSelected(false);
		transparentCheckBox.setToolTipText("Select to set this button to be transparent.");
		p1.add(transparentCheckBox);

		s = Modeler.getInternationalText("AutosizeCheckBox");
		autoSizeCheckBox = new JCheckBox(s != null ? s : "Size automatically");
		autoSizeCheckBox.setSelected(pageButton.autoSize);
		autoSizeCheckBox
				.setToolTipText("<html>Select to make this button auto-size itself according to the text and image on it.<br>Deselect to set a custom size.</html>");
		autoSizeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				pageButton.autoSize = autoSizeCheckBox.isSelected();
				widthField.setEnabled(!pageButton.autoSize);
				heightField.setEnabled(!pageButton.autoSize);
			}
		});
		p1.add(autoSizeCheckBox);

		s = Modeler.getInternationalText("DisabledAtRunCheckBox");
		disabledAtRunCheckBox = new JCheckBox(s != null ? s : "Disabled while model is running");
		disabledAtRunCheckBox.setSelected(false);
		disabledAtRunCheckBox
				.setToolTipText("<html>Select if you wish this button to be disabled while the model is running,<br>and to be enabled when the model stops.</html>");
		p1.add(disabledAtRunCheckBox);

		s = Modeler.getInternationalText("DisabledAtScriptCheckBox");
		disabledAtScriptCheckBox = new JCheckBox(s != null ? s : "Disabled while scripts are running");
		disabledAtScriptCheckBox.setSelected(false);
		disabledAtScriptCheckBox
				.setToolTipText("<html>Select if you wish this button to be disabled while the scripts are being executed,<br>and to be enabled when the execution of scripts ends.</html>");
		p1.add(disabledAtScriptCheckBox);

		s = Modeler.getInternationalText("FireOnHoldCheckBox");
		fireOnHoldCheckBox = new JCheckBox(s != null ? s : "Continuous firing while pressed");
		fireOnHoldCheckBox.setSelected(false);
		fireOnHoldCheckBox
				.setToolTipText("<html>Select if you wish this button to continously act when it is pressed.<br>Deselect if you wish this button to act once and only once before the mouse button is released.</html>");
		p1.add(fireOnHoldCheckBox);

		p1 = new JPanel(new BorderLayout(5, 5));
		p.add(p1, BorderLayout.CENTER);

		s = Modeler.getInternationalText("EnterScriptWhenClickedLabel");
		scriptLabel = new JLabel(s != null ? s : "Enter the script to be run when clicked:");
		scriptLabel.setEnabled(false);
		p1.add(scriptLabel, BorderLayout.NORTH);
		scriptArea = new PastableTextArea(5, 10);
		scriptArea.setEnabled(false);
		scriptArea.setBorder(BorderFactory.createLoweredBevelBorder());
		p1.add(new JScrollPane(scriptArea), BorderLayout.CENTER);

	}

}