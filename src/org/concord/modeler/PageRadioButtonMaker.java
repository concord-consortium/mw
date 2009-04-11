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
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.mw2d.models.MDModel;

/**
 * @author Charles Xie
 * 
 */
class PageRadioButtonMaker extends ComponentMaker {

	private static Font smallFont;
	private PageRadioButton pageRadioButton;
	private JComboBox modelComboBox, actionComboBox;
	private JCheckBox transparentCheckBox, autoSizeCheckBox, disabledAtRunCheckBox, disabledAtScriptCheckBox;
	private ColorComboBox bgComboBox;
	private JTextField nameField;
	private JTextField imageSelectedField, imageDeselectedField;
	private JTextField toolTipField;
	private IntegerTextField widthField, heightField;
	private JDialog dialog;
	private JButton okButton;
	private JTextArea scriptArea;
	private JLabel scriptLabel;
	private JPanel contentPane;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Object o = modelComboBox.getSelectedItem();
			if (o instanceof BasicModel) {
				BasicModel m = (BasicModel) o;
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					m.removeModelListener(pageRadioButton);
				}
				else {
					if (m instanceof MDModel) {
						pageRadioButton.setModelID(pageRadioButton.page.getComponentPool().getIndex(m));
					}
					else if (m instanceof Embeddable) {
						pageRadioButton.setModelID(((Embeddable) m).getIndex());
					}
					m.addModelListener(pageRadioButton);
					fillActionComboBox();
				}
			}
		}
	};

	private ItemListener actionSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED)
				return;
			Object o = actionComboBox.getSelectedItem();
			if (o instanceof Action) {
				pageRadioButton.setAction((Action) o);
				nameField.setText(pageRadioButton.getAction().toString());
				toolTipField.setText(pageRadioButton.getAction().toString());
				setScriptArea();
			}
		}
	};

	PageRadioButtonMaker(PageRadioButton prb) {
		setObject(prb);
	}

	void setObject(PageRadioButton prb) {
		pageRadioButton = prb;
	}

	private void confirm() {
		pageRadioButton.setTransparent(transparentCheckBox.isSelected());
		pageRadioButton.setBackground(bgComboBox.getSelectedColor());
		Object o = modelComboBox.getSelectedItem();
		BasicModel m = (BasicModel) o;
		m.addModelListener(pageRadioButton);
		pageRadioButton.setModelClass(m.getClass().getName());
		if (o instanceof MDModel) {
			pageRadioButton.setModelID(pageRadioButton.page.getComponentPool().getIndex(m));
		}
		else if (o instanceof Embeddable) {
			pageRadioButton.setModelID(((Embeddable) o).getIndex());
		}
		o = actionComboBox.getSelectedItem();
		if (o instanceof Action)
			pageRadioButton.setAction((Action) o);
		pageRadioButton.setText(nameField.getText());
		pageRadioButton.setImageFileNameSelected(imageSelectedField.getText());
		pageRadioButton.setImageFileNameDeselected(imageDeselectedField.getText());
		if (pageRadioButton.isSelected()) {
			String s = pageRadioButton.getImageFileNameSelected();
			if (s != null && !s.trim().equals("")) {
				pageRadioButton.setIcon(loadLocalImage(pageRadioButton.page, s));
			}
			else {
				pageRadioButton.setIcon(null);
			}
		}
		else {
			String s = pageRadioButton.getImageFileNameDeselected();
			if (s != null && !s.trim().equals("")) {
				pageRadioButton.setIcon(loadLocalImage(pageRadioButton.page, s));
			}
			else {
				pageRadioButton.setIcon(null);
			}
		}
		pageRadioButton.setToolTipText(toolTipField.getText());
		pageRadioButton.putClientProperty("script", scriptArea.getText());
		pageRadioButton.setDisabledAtRun(disabledAtRunCheckBox.isSelected());
		pageRadioButton.setDisabledAtScript(disabledAtScriptCheckBox.isSelected());
		if (!pageRadioButton.autoSize) {
			pageRadioButton.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		}
		pageRadioButton.page.getSaveReminder().setChanged(true);
		pageRadioButton.page.settleComponentSize();
	}

	void invoke(Page page) {

		pageRadioButton.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeRadioButtonDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize radio button", true);
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
					nameField.requestFocusInWindow();
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

		if (pageRadioButton.isTargetClass()) {
			if (pageRadioButton.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageRadioButton.modelClass),
							pageRadioButton.modelID);
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
					pageRadioButton.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageRadioButton.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageRadioButton.modelID);
				modelComboBox.setSelectedItem(mc.getMdContainer().getModel());
			}
			else {
				BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
				pageRadioButton.setModelID(componentPool.getIndex(m));
			}
		}
		modelComboBox.addItemListener(modelSelectionListener);

		fillActionComboBox();
		if (pageRadioButton.getAction() != null)
			actionComboBox.setSelectedItem(pageRadioButton.getAction());
		actionComboBox.addItemListener(actionSelectionListener);

		String t = pageRadioButton.getText();
		nameField.setText(t != null ? t : (actionComboBox.getSelectedItem() != null ? actionComboBox.getSelectedItem()
				.toString() : null));
		imageSelectedField.setText(pageRadioButton.getImageFileNameSelected());
		imageDeselectedField.setText(pageRadioButton.getImageFileNameDeselected());
		toolTipField.setText(pageRadioButton.getToolTipText());
		transparentCheckBox.setSelected(pageRadioButton.isTransparent());
		disabledAtRunCheckBox.setSelected(pageRadioButton.disabledAtRun);
		disabledAtScriptCheckBox.setSelected(pageRadioButton.disabledAtScript);
		bgComboBox.setColor(pageRadioButton.getBackground());
		autoSizeCheckBox.setSelected(pageRadioButton.autoSize);
		if (pageRadioButton.isPreferredSizeSet()) {
			widthField.setValue(pageRadioButton.getWidth());
			heightField.setValue(pageRadioButton.getHeight());
		}
		widthField.setEnabled(!pageRadioButton.autoSize);
		heightField.setEnabled(!pageRadioButton.autoSize);
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && actionComboBox.getItemCount() > 0);

		setScriptArea();

		dialog.setVisible(true);

	}

	private void setScriptArea() {
		String s = null;
		if (pageRadioButton.getAction() != null)
			s = (String) pageRadioButton.getAction().getValue(Action.SHORT_DESCRIPTION);
		if (actionComboBox.getSelectedItem() != null)
			s = actionComboBox.getSelectedItem().toString();
		if (s == null)
			return;
		boolean isScriptButton = isScriptActionKey(s);
		scriptArea.setEnabled(isScriptButton);
		scriptLabel.setEnabled(isScriptButton);
		scriptArea.setText(isScriptButton ? (String) pageRadioButton.getClientProperty("script") : null);
		if (isScriptButton) {
			scriptArea.requestFocus();
			scriptArea.setCaretPosition(0);
		}
	}

	private void fillActionComboBox() {
		actionComboBox.removeAllItems();
		Object o = modelComboBox.getSelectedItem();
		if (o instanceof BasicModel) {
			BasicModel model = (BasicModel) o;
			Map switchMap = model.getMultiSwitches();
			if (switchMap != null) {
				synchronized (switchMap) {
					for (Iterator it = switchMap.keySet().iterator(); it.hasNext();) {
						actionComboBox.addItem(switchMap.get(it.next()));
					}
				}
				Object scriptAction = getScriptAction(switchMap);
				if (scriptAction != null) {
					actionComboBox.setSelectedItem(scriptAction);
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

		s = Modeler.getInternationalText("Help");
		button = new JButton(s != null ? s : "Help");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Modeler.openWithNewInstance(pageRadioButton.getPage().getNavigator().getHomeDirectory()
						+ "tutorial/insertRadioButton.cml");
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
		modelComboBox.setPreferredSize(new Dimension(200, 20));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this radio button will interact with.");
		p.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("SelectActionLabel");
		p.add(new JLabel(s != null ? s : "Select an action", SwingConstants.LEFT));
		actionComboBox = new JComboBox();
		actionComboBox.setFont(smallFont);
		actionComboBox.setToolTipText("Select the switching action for this radio button.");
		p.add(actionComboBox);

		// row 3
		s = Modeler.getInternationalText("TextLabel");
		p.add(new JLabel(s != null ? s : "Text", SwingConstants.LEFT));
		nameField = new JTextField();
		nameField.setToolTipText("Type in the text that will appear on this radio button.");
		nameField.addActionListener(okListener);
		p.add(nameField);

		// row 4
		s = Modeler.getInternationalText("ImageFileNameSelected");
		p.add(new JLabel(s != null ? s : "Image to show while selected", SwingConstants.LEFT));
		imageSelectedField = new JTextField();
		imageSelectedField
				.setToolTipText("Type in the file name of the image that will appear on this radio button while it is selected.");
		imageSelectedField.addActionListener(okListener);
		p.add(imageSelectedField);

		// row 5
		s = Modeler.getInternationalText("ImageFileNameDeSelected");
		p.add(new JLabel(s != null ? s : "Image to show while not selected", SwingConstants.LEFT));
		imageDeselectedField = new JTextField();
		imageDeselectedField
				.setToolTipText("Type in the file name of the image that will appear on this radio button while it is not selected.");
		imageDeselectedField.addActionListener(okListener);
		p.add(imageDeselectedField);

		// row 6
		s = Modeler.getInternationalText("ToolTipLabel");
		p.add(new JLabel(s != null ? s : "Tool tip", SwingConstants.LEFT));
		toolTipField = new JTextField();
		toolTipField.setToolTipText("Type in the text that will appear as the tool tip.");
		toolTipField.addActionListener(okListener);
		p.add(toolTipField);

		// row 7
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(pageRadioButton.getWidth() <= 0 ? 100 : pageRadioButton.getWidth(), 10, 400);
		widthField.setEnabled(false);
		widthField
				.setToolTipText("Type in an integer to set the width of this radio button, if it will not be auto-sized.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 8
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(pageRadioButton.getHeight() <= 0 ? 24 : pageRadioButton.getHeight(), 10, 400);
		heightField.setEnabled(false);
		heightField
				.setToolTipText("Type in an integer to set the height of this radio button, if it will not be auto-sized.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 9
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageRadioButton);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color for this radio button, if it is not transparent.");
		p.add(bgComboBox);

		ModelerUtilities.makeCompactGrid(p, 9, 2, 5, 5, 10, 2);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new GridLayout(2, 2));
		p1.setBorder(BorderFactory.createEtchedBorder());
		p.add(p1, BorderLayout.NORTH);

		s = Modeler.getInternationalText("TransparencyCheckBox");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		// transparentCheckBox.setEnabled(!Page.isNativeLookAndFeelUsed());
		transparentCheckBox.setSelected(false);
		transparentCheckBox.setToolTipText("Select to set this button to be transparent.");
		p1.add(transparentCheckBox);

		s = Modeler.getInternationalText("AutosizeCheckBox");
		autoSizeCheckBox = new JCheckBox(s != null ? s : "Size automatically");
		autoSizeCheckBox.setSelected(pageRadioButton.autoSize);
		autoSizeCheckBox
				.setToolTipText("<html>Select to make this button auto-size itself according to the text and image on it.<br>Deselect to set a custom size.</html>");
		autoSizeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				pageRadioButton.autoSize = autoSizeCheckBox.isSelected();
				widthField.setEnabled(!pageRadioButton.autoSize);
				heightField.setEnabled(!pageRadioButton.autoSize);
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
				.setToolTipText("<html>Select if you wish this button to be disabled while scripts are running,<br>and to be enabled when scripts end.</html>");
		p1.add(disabledAtScriptCheckBox);

		p1 = new JPanel(new BorderLayout(5, 5));
		p.add(p1, BorderLayout.CENTER);

		s = Modeler.getInternationalText("EnterScriptWhenSelectedLabel");
		scriptLabel = new JLabel(s != null ? s : "Enter the script to be run when selected:");
		scriptLabel.setEnabled(false);
		p1.add(scriptLabel, BorderLayout.NORTH);
		scriptArea = new PastableTextArea(5, 10);
		scriptArea.setEnabled(false);
		scriptArea.setBorder(BorderFactory.createLoweredBevelBorder());
		p1.add(new JScrollPane(scriptArea), BorderLayout.CENTER);

	}

}