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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.PastableTextField;
import org.concord.mw2d.models.MDModel;

/**
 * @author Charles Xie
 * 
 */
class PageComboBoxMaker extends ComponentMaker {

	private PageComboBox pageComboBox;
	private static Font smallFont;
	private JDialog dialog;
	private JButton okButton;
	private JComboBox modelComboBox, actionComboBox;
	private JCheckBox disabledAtRunCheckBox, disabledAtScriptCheckBox;
	private JLabel optionLabel;
	private JTextField uidField;
	private JTextField optionField;
	private JTextField toolTipField;
	private JTextArea scriptArea;
	private JLabel scriptLabel;
	private PastableTextField exampleLabel;
	private JPanel contentPane;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Object o = modelComboBox.getSelectedItem();
			if (o instanceof BasicModel) {
				BasicModel m = (BasicModel) o;
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					m.removeModelListener(pageComboBox);
				}
				else {
					if (m instanceof MDModel) {
						pageComboBox.setModelID(pageComboBox.page.getComponentPool().getIndex(m));
					}
					else if (m instanceof Embeddable) {
						pageComboBox.setModelID(((Embeddable) m).getIndex());
					}
					m.addModelListener(pageComboBox);
					fillActionComboBox();
				}
			}
		}
	};

	private ItemListener actionSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED)
				return;
			Action a = (Action) actionComboBox.getSelectedItem();
			pageComboBox.setAction(a);
			String name = (String) a.getValue(Action.SHORT_DESCRIPTION);
			boolean b = name.equals("Import a model");
			optionLabel.setEnabled(b);
			optionField.setEnabled(b);
			if (b) {
				Object o = a.getValue("options");
				if (o instanceof String[]) {
					String s = "";
					String[] ss = (String[]) o;
					for (int i = 0; i < ss.length - 1; i++)
						s += ss[i] + ", ";
					s += ss[ss.length - 1];
					optionField.setText(s);
				}
			}
			else {
				optionField.setText(null);
			}
			toolTipField.setText(name);
			if (isScriptActionKey(name)) {
				Object o = pageComboBox.getClientProperty("Script");
				if (o instanceof String)
					pageComboBox.setupScripts((String) o);
			}
			setScriptArea();
		}
	};

	PageComboBoxMaker(PageComboBox pcb) {
		setObject(pcb);
	}

	void setObject(PageComboBox pcb) {
		pageComboBox = pcb;
	}

	private boolean confirm() {
		if (!checkAndSetUid(uidField.getText(), pageComboBox, dialog))
			return false;
		String s = scriptArea.getText();
		if (s != null && !s.trim().equals("")) {
			if (!checkBraceBalance(s)) {
				JOptionPane.showMessageDialog(dialog, "Unbalanced balances are found.", "Menu text-script pair error",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		Object o = modelComboBox.getSelectedItem();
		BasicModel m = (BasicModel) o;
		m.addModelListener(pageComboBox);
		pageComboBox.setModelClass(m.getClass().getName());
		if (o instanceof MDModel) {
			pageComboBox.setModelID(pageComboBox.page.getComponentPool().getIndex(m));
		}
		else if (o instanceof Embeddable) {
			pageComboBox.setModelID(((Embeddable) o).getIndex());
		}
		o = actionComboBox.getSelectedItem();
		if (o instanceof Action)
			pageComboBox.setAction((Action) o);
		pageComboBox.setOptionGroup(optionField.getText());
		if (scriptArea.getText() != null && !scriptArea.getText().trim().equals("")) {
			pageComboBox.putClientProperty("Script", scriptArea.getText());
			pageComboBox.setupScripts(scriptArea.getText());
		}
		pageComboBox.setToolTipText(toolTipField.getText());
		pageComboBox.setDisabledAtRun(disabledAtRunCheckBox.isSelected());
		pageComboBox.setDisabledAtScript(disabledAtScriptCheckBox.isSelected());
		pageComboBox.page.getSaveReminder().setChanged(true);
		pageComboBox.page.settleComponentSize();
		return true;
	}

	void invoke(Page page) {

		pageComboBox.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeComboBoxDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize combo box", true);
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

		if (pageComboBox.isTargetClass()) {
			if (pageComboBox.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageComboBox.modelClass), pageComboBox.modelID);
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
					pageComboBox.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageComboBox.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageComboBox.modelID);
				modelComboBox.setSelectedItem(mc.getMdContainer().getModel());
			}
			else {
				BasicModel m = (BasicModel) modelComboBox.getSelectedItem();
				pageComboBox.setModelID(componentPool.getIndex(m));
			}
		}
		modelComboBox.addItemListener(modelSelectionListener);

		fillActionComboBox();
		if (pageComboBox.getAction() != null) {
			actionComboBox.setSelectedItem(pageComboBox.getAction());
			boolean b = ((String) pageComboBox.getAction().getValue(Action.SHORT_DESCRIPTION)).equals("Import a model");
			optionLabel.setEnabled(b);
			optionField.setEnabled(b);
			if (pageComboBox.optionGroup != null)
				optionField.setText(pageComboBox.optionGroup);
		}
		else {
			if (actionComboBox.getSelectedItem() != null) {
				Action a = (Action) actionComboBox.getSelectedItem();
				boolean b = ((String) a.getValue(Action.SHORT_DESCRIPTION)).equals("Import a model");
				optionLabel.setEnabled(b);
				optionField.setEnabled(b);
			}
		}
		actionComboBox.addItemListener(actionSelectionListener);
		uidField.setText(pageComboBox.getUid());
		toolTipField.setText(pageComboBox.getToolTipText());
		disabledAtRunCheckBox.setSelected(pageComboBox.disabledAtRun);
		disabledAtScriptCheckBox.setSelected(pageComboBox.disabledAtScript);
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && actionComboBox.getItemCount() > 0);

		setScriptArea();

		dialog.setVisible(true);

	}

	private void setScriptArea() {
		String s = null;
		if (pageComboBox.getAction() != null) {
			s = (String) pageComboBox.getAction().getValue(Action.SHORT_DESCRIPTION);
		}
		else {
			if (actionComboBox.getSelectedItem() != null)
				s = actionComboBox.getSelectedItem().toString();
		}
		if (s == null) {
			scriptArea.setText(null);
			return;
		}
		boolean isScripted = isScriptActionKey(s);
		scriptArea.setEnabled(isScripted);
		scriptLabel.setEnabled(isScripted);
		exampleLabel.setEnabled(isScripted);
		if (isScripted) {
			Object o = pageComboBox.getClientProperty("Script");
			if (o instanceof String) {
				scriptArea.setText((String) o);
				scriptArea.requestFocusInWindow();
				scriptArea.setCaretPosition(0);
			}
			else {
				scriptArea.setText(null);
			}
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
			Map choiceMap = model.getChoices();
			if (choiceMap != null) {
				synchronized (choiceMap) {
					for (Iterator it = choiceMap.values().iterator(); it.hasNext();) {
						actionComboBox.addItem(it.next());
					}
				}
				Object scriptAction = getScriptAction(choiceMap);
				if (scriptAction != null) {
					actionComboBox.setSelectedItem(scriptAction);
				}
			}
		}
	}

	/* check if the braces contained in the string is balanced. */
	private boolean checkBraceBalance(String str) {
		int nlp = 0;
		int nrp = 0;
		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
			case '{':
				nlp++;
				break;
			case '}':
				nrp++;
				break;
			}
		}
		return nlp == nrp;
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

		s = Modeler.getInternationalText("Help");
		button = new JButton(s != null ? s : "Help");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Modeler.openWithNewInstance(pageComboBox.getPage().getNavigator().getHomeDirectory()
						+ "tutorial/insertComboBox.cml");
			}
		});
		p.add(button);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.NORTH);

		optionLabel = new JLabel("Model list", SwingConstants.LEFT);
		optionLabel.setEnabled(false);

		JPanel p1 = new JPanel(new GridLayout(5, 1, 3, 3));
		p.add(p1, BorderLayout.WEST);

		s = Modeler.getInternationalText("SelectModelLabel");
		p1.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		s = Modeler.getInternationalText("SelectActionLabel");
		p1.add(new JLabel(s != null ? s : "Select an action", SwingConstants.LEFT));
		s = Modeler.getInternationalText("UniqueIdentifier");
		p1.add(new JLabel(s != null ? s : "Unique identifier", SwingConstants.LEFT));
		s = Modeler.getInternationalText("ToolTipLabel");
		p1.add(new JLabel(s != null ? s : "Tool tip", SwingConstants.LEFT));
		p1.add(optionLabel);

		p1 = new JPanel(new GridLayout(5, 1, 3, 3));
		p.add(p1, BorderLayout.CENTER);

		modelComboBox = new JComboBox();
		if (smallFont == null)
			smallFont = new Font(modelComboBox.getFont().getFamily(), modelComboBox.getFont().getStyle(), 10);
		modelComboBox.setFont(smallFont);
		modelComboBox.setRenderer(new LabelRenderer());
		modelComboBox.setPreferredSize(new Dimension(200, 20));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one this combo box will interact with.");
		p1.add(modelComboBox);

		actionComboBox = new JComboBox();
		actionComboBox.setFont(smallFont);
		actionComboBox.setToolTipText("Select the choice action for this combo box.");
		p1.add(actionComboBox);

		uidField = new JTextField();
		uidField.setToolTipText("Type in a string to be used as the unique identifier of this combo box.");
		uidField.addActionListener(okListener);
		p1.add(uidField);

		toolTipField = new JTextField();
		toolTipField.setToolTipText("Type in the text that will appear as the tool tip.");
		toolTipField.addActionListener(okListener);
		p1.add(toolTipField);

		optionField = new PastableTextField();
		optionField.setEnabled(false);
		optionField.addActionListener(okListener);
		optionField.setToolTipText("Type file names, separated by commas.");
		p1.add(optionField);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		p1.setBorder(BorderFactory.createEtchedBorder());
		p.add(p1, BorderLayout.SOUTH);

		s = Modeler.getInternationalText("DisabledAtRunCheckBox");
		disabledAtRunCheckBox = new JCheckBox(s != null ? s : "Disabled while model is running");
		disabledAtRunCheckBox.setSelected(false);
		disabledAtRunCheckBox
				.setToolTipText("<html>Select if you wish this combo box to be disabled while the model is running,<br>and to be enabled when the model stops.</html>");
		p1.add(disabledAtRunCheckBox);

		s = Modeler.getInternationalText("DisabledAtScriptCheckBox");
		disabledAtScriptCheckBox = new JCheckBox(s != null ? s : "Disabled while scripts are running");
		disabledAtScriptCheckBox.setSelected(false);
		disabledAtScriptCheckBox
				.setToolTipText("<html>Select if you wish this combo box to be disabled while scripts are running,<br>and to be enabled when scripts end.</html>");
		p1.add(disabledAtScriptCheckBox);

		p = new JPanel(new BorderLayout(5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		s = Modeler.getInternationalText("EnterMenuTextScriptPairs");
		scriptLabel = new JLabel((s != null ? s : "Enter menu text-script pairs")
				+ " in {option=\"*\" script=\"*\"} format:");
		scriptLabel.setEnabled(false);
		p.add(scriptLabel, BorderLayout.NORTH);
		scriptArea = new PastableTextArea(5, 10);
		scriptArea.setEnabled(false);
		scriptArea.setBorder(BorderFactory.createLoweredBevelBorder());
		p.add(new JScrollPane(scriptArea), BorderLayout.CENTER);
		exampleLabel = new PastableTextField("e.g. {option=\"a\" script=\"do a\"} {option=\"b\" script=\"do b\"}");
		exampleLabel.setEnabled(false);
		exampleLabel.setEditable(false);
		exampleLabel.setBackground(scriptLabel.getBackground());
		exampleLabel.setBorder(BorderFactory.createEmptyBorder());
		p.add(exampleLabel, BorderLayout.SOUTH);

	}

}