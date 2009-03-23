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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import org.concord.modeler.event.AbstractChange;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.MDModel;

/**
 * @author Charles Xie
 * 
 */
class PageSliderMaker extends ComponentMaker {

	private PageSlider pageSlider;
	private JDialog dialog;
	private JComboBox modelComboBox, actionComboBox;
	private JCheckBox transparentCheckBox, drawTickCheckBox, disabledAtRunCheckBox, disabledAtScriptCheckBox;
	private JRadioButton horizontalRadioButton, verticalRadioButton;
	private JComboBox borderComboBox;
	private ColorComboBox bgComboBox;
	private JTextField nameField;
	private JTextField toolTipField;
	private RealNumberTextField maxField, minField, valueField;
	private IntegerTextField widthField, heightField;
	private JSpinner stepSpinner;
	private JButton okButton;
	private JTextArea scriptArea, labelArea;
	private JLabel scriptLabel, scriptExampleLabel, labelLabel;
	private PastableTextField labelExampleLabel;
	private JPanel contentPane;
	private static Font smallFont;
	private JTextComponent focusTextComponent;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Object o = modelComboBox.getSelectedItem();
			if (o instanceof BasicModel) {
				BasicModel m = (BasicModel) o;
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					m.removeModelListener(pageSlider);
				}
				else {
					if (m instanceof MDModel) {
						pageSlider.setModelID(pageSlider.page.getComponentPool().getIndex(m));
					}
					else if (m instanceof Embeddable) {
						pageSlider.setModelID(((Embeddable) m).getIndex());
					}
					m.addModelListener(pageSlider);
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
			String name = (String) c.getProperty(AbstractChange.SHORT_DESCRIPTION);
			pageSlider.addChangeListener(c);
			pageSlider.setNumberOfSteps(50);
			pageSlider.setToolTipText(name);
			nameField.setText(c.toString());
			toolTipField.setText(c.toString());
			minField.setValue(c.getMinimum());
			maxField.setValue(c.getMaximum());
			valueField.setValue(c.getValue());
			pageSlider.fmin = c.getMinimum();
			pageSlider.fmax = c.getMaximum();
			pageSlider.value = c.getValue();
			pageSlider.adjustScale();
			String s = pageSlider.getTitle();
			if (s != null)
				pageSlider.setTitle(s);
			if (pageSlider.actionLabelMap != null) {
				pageSlider.setupLabels(pageSlider.actionLabelMap.get(name));
			}
			else {
				pageSlider.setupLabels(null);
			}
			setScriptArea();
			setLabelArea();
		}
	};

	PageSliderMaker(PageSlider ps) {
		setObject(ps);
	}

	void setObject(PageSlider ps) {
		pageSlider = ps;
	}

	private boolean confirm() {
		if (maxField.getValue() <= minField.getValue()) {
			JOptionPane.showMessageDialog(dialog, "Upper bound must be greater than lower bound.", "Input error",
					JOptionPane.ERROR_MESSAGE);
			focusTextComponent = maxField;
			return false;
		}
		pageSlider.setTitle(nameField.getText());
		pageSlider.setOrientation(horizontalRadioButton.isSelected() ? JSlider.HORIZONTAL : JSlider.VERTICAL);
		pageSlider.setBorderType((String) borderComboBox.getSelectedItem());
		pageSlider.setBackground(bgComboBox.getSelectedColor());
		pageSlider.setPaintTicks(drawTickCheckBox.isSelected());
		pageSlider.fmax = (float) maxField.getValue();
		pageSlider.fmin = (float) minField.getValue();
		if (pageSlider.getPaintTicks())
			pageSlider.setMajorTickSpacing((pageSlider.getMaximum() - pageSlider.getMinimum()) / pageSlider.nstep);
		pageSlider.nstep = ((Integer) stepSpinner.getValue()).intValue();
		pageSlider.adjustScale();
		pageSlider.setDisabledAtRun(disabledAtRunCheckBox.isSelected());
		pageSlider.setDisabledAtScript(disabledAtScriptCheckBox.isSelected());
		BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
		m.addModelListener(pageSlider);
		pageSlider.setModelClass(m.getClass().getName());
		if (m instanceof MDModel) {
			pageSlider.setModelID(pageSlider.page.getComponentPool().getIndex(m));
		}
		else if (m instanceof Embeddable) {
			pageSlider.setModelID(((Embeddable) m).getIndex());
		}
		pageSlider.addChangeListener((AbstractChange) actionComboBox.getSelectedItem());
		pageSlider.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		String labelText = labelArea.getText();
		if (labelText != null && !labelText.trim().equals("")) {
			pageSlider.putClientProperty("Label", labelText);
			try {
				pageSlider.setupLabels(labelText);
			}
			catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(dialog,
						"Please check if the label values are between the upper and lower bounds.", "Label error",
						JOptionPane.ERROR_MESSAGE);
				focusTextComponent = labelArea;
				return false;
			}
		}
		else {
			pageSlider.putClientProperty("Label", null);
			pageSlider.setupLabels(null);
			if (pageSlider.actionLabelMap != null)
				pageSlider.actionLabelMap.remove(pageSlider.getName());
		}
		if (scriptArea.getText() != null && !scriptArea.getText().trim().equals("")) {
			pageSlider.putClientProperty("Script", scriptArea.getText());
		}
		pageSlider.setOpaque(!transparentCheckBox.isSelected());
		pageSlider.setToolTipText(toolTipField.getText());
		pageSlider.page.getSaveReminder().setChanged(true);
		pageSlider.page.settleComponentSize();
		return true;
	}

	void invoke(Page page) {

		pageSlider.page = page;
		page.deselect();
		focusTextComponent = null;
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeSliderDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize slider", true);
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
						nameField.requestFocus();
					}
					else {
						focusTextComponent.selectAll();
						focusTextComponent.requestFocus();
					}
				}
			});
		}

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();
		actionComboBox.removeItemListener(actionSelectionListener);

		pageSlider.adjustScale();

		// add legacy MD models to the model list
		final ComponentPool componentPool = page.getComponentPool();
		synchronized (componentPool) {
			for (ModelCanvas mc : componentPool.getModels()) {
				if (mc.isUsed()) {
					modelComboBox.addItem(mc.getMdContainer().getModel());
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

		if (pageSlider.isTargetClass()) {
			if (pageSlider.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageSlider.modelClass), pageSlider.modelID);
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
					pageSlider.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageSlider.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageSlider.modelID);
				modelComboBox.setSelectedItem(mc.getMdContainer().getModel());
			}
			else {
				BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
				pageSlider.setModelID(componentPool.getIndex(m));
			}
		}
		modelComboBox.addItemListener(modelSelectionListener);

		fillActionComboBox();
		actionComboBox.addItemListener(actionSelectionListener);

		String t = pageSlider.getTitle();
		nameField.setText(t != null && !t.trim().equals("") ? t
				: (actionComboBox.getSelectedItem() != null ? actionComboBox.getSelectedItem().toString() : null));
		toolTipField.setText(pageSlider.getToolTipText());
		minField.setValue(pageSlider.getDoubleMinimum());
		maxField.setValue(pageSlider.getDoubleMaximum());
		valueField.setValue(pageSlider.getDoubleValue());
		if (pageSlider.isPreferredSizeSet()) {
			widthField.setValue(pageSlider.getWidth());
			heightField.setValue(pageSlider.getHeight());
		}
		stepSpinner.setValue(new Integer(pageSlider.nstep));
		disabledAtRunCheckBox.setSelected(pageSlider.disabledAtRun);
		disabledAtScriptCheckBox.setSelected(pageSlider.disabledAtScript);
		transparentCheckBox.setSelected(!pageSlider.isOpaque());
		drawTickCheckBox.setSelected(pageSlider.getPaintTicks());
		borderComboBox.setSelectedItem(pageSlider.getBorderType());
		horizontalRadioButton.setSelected(pageSlider.getOrientation() == JSlider.HORIZONTAL);
		verticalRadioButton.setSelected(pageSlider.getOrientation() == JSlider.VERTICAL);
		bgComboBox.setColor(pageSlider.getBackground());
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && actionComboBox.getItemCount() > 0);

		setLabelArea();
		setScriptArea();

		dialog.setVisible(true);

	}

	private void fillActionComboBox() {
		actionComboBox.removeAllItems();
		BasicModel model = (BasicModel) modelComboBox.getSelectedItem();
		if (model != null) {
			Map changeMap = model.getChanges();
			if (changeMap != null) {
				synchronized (changeMap) {
					for (Iterator it = changeMap.values().iterator(); it.hasNext();) {
						actionComboBox.addItem(it.next());
					}
				}
				Object scriptAction = getScriptAction(changeMap);
				if (scriptAction != null) {
					actionComboBox.setSelectedItem(scriptAction);
				}
			}
			ChangeListener[] listeners = pageSlider.getChangeListeners();
			if (listeners != null && listeners.length >= 1) {
				actionComboBox.setSelectedItem(listeners[0]);
			}
			else {
				AbstractChange c = (AbstractChange) actionComboBox.getSelectedItem();
				minField.setValue(c.getMinimum());
				maxField.setValue(c.getMaximum());
				valueField.setValue(c.getValue());
				pageSlider.fmin = c.getMinimum();
				pageSlider.fmax = c.getMaximum();
				pageSlider.value = c.getValue();
				pageSlider.adjustScale();
			}
		}
	}

	private void setLabelArea() {
		if (!pageSlider.getPaintLabels()) {
			labelArea.setText(null);
			return;
		}
		Hashtable map = (Hashtable) pageSlider.getLabelTable();
		String s = "";
		Object value;
		JLabel label;
		for (Iterator i = map.keySet().iterator(); i.hasNext();) {
			value = i.next();
			label = (JLabel) map.get(value);
			s += "{value=\"" + (((Integer) value).intValue() / pageSlider.scaleFactor) + "\" label=\""
					+ label.getText() + "\"}\n";
		}
		labelArea.setText(s);
		labelArea.setCaretPosition(0);
	}

	private void setScriptArea() {
		String s = null;
		AbstractChange c = pageSlider.getChange();
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
		scriptExampleLabel.setEnabled(isScripted);
		if (isScripted) {
			scriptArea.setText((String) pageSlider.getClientProperty("Script"));
			scriptArea.setCaretPosition(0);
		}
		else {
			scriptArea.setText(null);
		}
	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (confirm()) {
					cancel = false;
					dialog.dispose();
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

		s = Modeler.getInternationalText("Help");
		button = new JButton(s != null ? s : "Help");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Modeler.openWithNewInstance(pageSlider.getPage().getNavigator().getHomeDirectory()
						+ "tutorial/insertSlider.cml");
			}
		});
		p.add(button);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.NORTH);

		JPanel p2 = new JPanel(new SpringLayout());
		p.add(p2, BorderLayout.CENTER);

		// row 1
		s = Modeler.getInternationalText("SelectModelLabel");
		p2.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		modelComboBox = new JComboBox();
		if (smallFont == null)
			smallFont = new Font(modelComboBox.getFont().getFamily(), modelComboBox.getFont().getStyle(), 10);
		modelComboBox.setFont(smallFont);
		modelComboBox.setRenderer(new LabelRenderer());
		modelComboBox.setPreferredSize(new Dimension(200, 20));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this slider will interact with.");
		p2.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("SelectVariableLabel");
		p2.add(new JLabel(s != null ? s : "Select a variable", SwingConstants.LEFT));
		actionComboBox = new JComboBox();
		actionComboBox.setFont(smallFont);
		actionComboBox.setToolTipText("Select the variable this slider will invoke.");
		p2.add(actionComboBox);

		// row 3
		s = Modeler.getInternationalText("TextLabel");
		p2.add(new JLabel(s != null ? s : "Title", SwingConstants.LEFT));
		nameField = new JTextField(pageSlider.getTitle());
		nameField.setToolTipText("Type in the text that will appear on this slider.");
		nameField.addActionListener(okListener);
		p2.add(nameField);

		// row 4
		s = Modeler.getInternationalText("ToolTipLabel");
		p2.add(new JLabel(s != null ? s : "Tool tip", SwingConstants.LEFT));
		toolTipField = new JTextField();
		toolTipField.setToolTipText("Type in the text that will appear as the tool tip.");
		toolTipField.addActionListener(okListener);
		p2.add(toolTipField);

		// row 5
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p2.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageSlider);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color for this slider, if it is not transparent.");
		p2.add(bgComboBox);

		// row 6
		s = Modeler.getInternationalText("BorderLabel");
		p2.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p2.getBackground());
		borderComboBox.setToolTipText("Select the border type for this button.");
		p2.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p2, 6, 2, 5, 5, 10, 2);

		p2 = new JPanel(new SpringLayout());
		p.add(p2, BorderLayout.EAST);

		// row 1
		s = Modeler.getInternationalText("WidthLabel");
		p2.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(200, 30, 600);
		widthField.setToolTipText("Type in an integer to set the width of this slider, if it will not be auto-sized.");
		widthField.addActionListener(okListener);
		p2.add(widthField);

		// row 2
		s = Modeler.getInternationalText("HeightLabel");
		p2.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(60, 20, 600);
		heightField
				.setToolTipText("Type in an integer to set the height of this slider, if it will not be auto-sized.");
		heightField.addActionListener(okListener);
		p2.add(heightField);

		// row 3
		s = Modeler.getInternationalText("UpperBoundLabel");
		p2.add(new JLabel(s != null ? s : "Upper bound", SwingConstants.LEFT));
		maxField = new RealNumberTextField(pageSlider.fmax, -Float.MAX_VALUE, Float.MAX_VALUE);
		maxField.setToolTipText("Type in the upper bound of the range this slider controls.");
		maxField.addActionListener(okListener);
		p2.add(maxField);

		// row 4
		s = Modeler.getInternationalText("LowerBoundLabel");
		p2.add(new JLabel(s != null ? s : "Lower bound", SwingConstants.LEFT));
		minField = new RealNumberTextField(pageSlider.fmin, -Float.MAX_VALUE, Float.MAX_VALUE);
		minField.setToolTipText("Type in the lower bound of the range this slider controls.");
		minField.addActionListener(okListener);
		p2.add(minField);

		// row 5
		s = Modeler.getInternationalText("CurrentValueLabel");
		p2.add(new JLabel(s != null ? s : "Current value", SwingConstants.LEFT));
		valueField = new RealNumberTextField(pageSlider.fmin, -Float.MAX_VALUE, Float.MAX_VALUE);
		valueField.setToolTipText("The current value of this slider.");
		valueField.setEnabled(false);
		p2.add(valueField);

		// row 6
		s = Modeler.getInternationalText("NumberOfStepsLabel");
		p2.add(new JLabel(s != null ? s : "Number of steps", SwingConstants.LEFT));
		stepSpinner = new JSpinner(new SpinnerNumberModel(pageSlider.nstep, 2, 100, 1));
		((JSpinner.DefaultEditor) stepSpinner.getEditor()).getTextField().setToolTipText(
				"Select step of the slider control.");
		((JSpinner.DefaultEditor) stepSpinner.getEditor()).getTextField().addActionListener(okListener);
		p2.add(stepSpinner);

		ModelerUtilities.makeCompactGrid(p2, 6, 2, 5, 5, 10, 2);

		p = new JPanel(new BorderLayout(8, 8));
		p.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p3 = new JPanel(new BorderLayout(5, 5));
		p3.setBorder(BorderFactory.createEtchedBorder());
		p.add(p3, BorderLayout.NORTH);

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		p3.add(p1, BorderLayout.NORTH);

		s = Modeler.getInternationalText("OrientationLabel");
		p1.add(new JLabel(s != null ? s : "Orientation:"));

		ButtonGroup orieGroup = new ButtonGroup();

		s = Modeler.getInternationalText("Horizontal");
		horizontalRadioButton = new JRadioButton(s != null ? s : "Horizontal");
		horizontalRadioButton.setToolTipText("Set the orientation of this slider to be horizontal.");
		horizontalRadioButton.setSelected(pageSlider.getOrientation() == JSlider.HORIZONTAL);
		horizontalRadioButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				switch (e.getStateChange()) {
				case ItemEvent.DESELECTED:
					String w = widthField.getText();
					String h = heightField.getText();
					if (w.compareTo(h) < 0) {
						widthField.setText(h);
						heightField.setText(w);
					}
					break;
				case ItemEvent.SELECTED:
					w = widthField.getText();
					h = heightField.getText();
					if (w.compareTo(h) > 0) {
						widthField.setText(h);
						heightField.setText(w);
					}
					break;
				}
			}
		});
		p1.add(horizontalRadioButton);
		orieGroup.add(horizontalRadioButton);

		s = Modeler.getInternationalText("Vertical");
		verticalRadioButton = new JRadioButton(s != null ? s : "Vertical");
		verticalRadioButton.setToolTipText("Set the orientation of this slider to be vertical.");
		verticalRadioButton.setSelected(pageSlider.getOrientation() == JSlider.VERTICAL);
		p1.add(verticalRadioButton);
		orieGroup.add(verticalRadioButton);

		p1 = new JPanel(new GridLayout(2, 2, 5, 5));
		p3.add(p1, BorderLayout.CENTER);

		s = Modeler.getInternationalText("TransparencyCheckBox");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setSelected(false);
		transparentCheckBox.setToolTipText("Select to set this slider to be transparent.");
		p1.add(transparentCheckBox);

		s = Modeler.getInternationalText("DisabledAtRunCheckBox");
		disabledAtRunCheckBox = new JCheckBox(s != null ? s : "Disabled while model is running");
		disabledAtRunCheckBox.setSelected(false);
		disabledAtRunCheckBox
				.setToolTipText("<html>Select if you wish this slider to be disabled while the model is running,<br>and to be enabled when the model stops.</html>");
		p1.add(disabledAtRunCheckBox);

		s = Modeler.getInternationalText("DrawTickMarksCheckBox");
		drawTickCheckBox = new JCheckBox(s != null ? s : "Draw tick marks");
		drawTickCheckBox.setSelected(pageSlider.getPaintTicks());
		drawTickCheckBox.setToolTipText("Select if tick marks should be drawn; deselect otherwise.");
		p1.add(drawTickCheckBox);

		s = Modeler.getInternationalText("DisabledAtScriptCheckBox");
		disabledAtScriptCheckBox = new JCheckBox(s != null ? s : "Disabled while scripts are running");
		disabledAtScriptCheckBox.setSelected(false);
		disabledAtScriptCheckBox
				.setToolTipText("<html>Select if you wish this slider to be disabled while scripts are running,<br>and to be enabled when scripts end.</html>");
		p1.add(disabledAtScriptCheckBox);

		// script setting area
		p1 = new JPanel(new BorderLayout(4, 4));
		p.add(p1, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("EnterScriptForSliderLabel");
		scriptLabel = new JLabel("2. "
				+ (s != null ? s : "Enter scripts (refer value as %val, maximum as %max & minimum as %min):"));
		scriptLabel.setEnabled(false);
		p1.add(scriptLabel, BorderLayout.NORTH);
		scriptArea = new PastableTextArea(5, 10);
		scriptArea.setEnabled(false);
		scriptArea.setBorder(BorderFactory.createLoweredBevelBorder());
		p1.add(new JScrollPane(scriptArea), BorderLayout.CENTER);
		scriptExampleLabel = new JLabel("e.g. \"set temperature %val\"");
		scriptExampleLabel.setEnabled(false);
		p1.add(scriptExampleLabel, BorderLayout.SOUTH);

		// label setting area
		p1 = new JPanel(new BorderLayout(4, 4));
		p.add(p1, BorderLayout.CENTER);

		s = Modeler.getInternationalText("EnterValueLabelPairs");
		labelLabel = new JLabel("1. " + (s != null ? s : "Enter value-label pairs:"));
		p1.add(labelLabel, BorderLayout.NORTH);
		labelArea = new PastableTextArea(5, 10);
		labelArea.setBorder(BorderFactory.createLoweredBevelBorder());
		p1.add(new JScrollPane(labelArea), BorderLayout.CENTER);
		labelExampleLabel = new PastableTextField("e.g. {value=\"1\" label=\"a\"} {value=\"2\" label=\"b\"}");
		labelExampleLabel.setEditable(false);
		labelExampleLabel.setBackground(labelLabel.getBackground());
		labelExampleLabel.setBorder(BorderFactory.createEmptyBorder());
		p1.add(labelExampleLabel, BorderLayout.SOUTH);

	}

}