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

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.PastableTextField;

/**
 * @author Charles Xie
 * 
 */
class PageMultipleChoiceMaker extends ComponentMaker {

	private PageMultipleChoice pageMultipleChoice;
	private JPanel contentPane;
	private JDialog dialog;
	private JRadioButton topRadioButton, leftRadioButton;
	private JCheckBox multipleCheckBox, transparentCheckBox;
	private ColorComboBox bgComboBox;
	private JComboBox borderComboBox;
	private JTextField uidField;
	private JTextArea questionArea;
	private IntegerTextField widthField, heightField;
	private JPanel choiceButtonPanel, choiceFieldPanel, scriptPanel;
	private JPanel scriptPanelForSingleSelection, scriptPanelForMultiSelection;
	private JPanel downPanel;
	private JTextField[] choiceField;
	private JTextArea wrongAnswerScriptArea, correctAnswerScriptArea;
	private AbstractButton[] choiceButton;
	private JSpinner nChoiceSpinner;
	private JCheckBox answerButtonCheckBox, clearButtonCheckBox;
	private ButtonGroup bg;
	private JTabbedPane scriptTabbedPane;
	private JTextArea[] scriptArea;

	private static GridLayout gridLayout = new GridLayout();
	static {
		gridLayout.setColumns(1);
		gridLayout.setHgap(3);
		gridLayout.setVgap(3);
	}

	PageMultipleChoiceMaker(PageMultipleChoice pmc) {
		setObject(pmc);
	}

	void setObject(PageMultipleChoice pmc) {
		pageMultipleChoice = pmc;
	}

	private boolean confirm() {

		if (!checkAndSetUid(uidField.getText(), pageMultipleChoice, dialog))
			return false;

		if (questionArea.getText() == null || questionArea.getText().trim().equals("")) {
			JOptionPane.showMessageDialog(dialog, "You must set the question for this multiple choice.",
					"Missing question", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		pageMultipleChoice.setQuestion(questionArea.getText());
		pageMultipleChoice.setOpaque(!transparentCheckBox.isSelected());
		pageMultipleChoice.setBorderType((String) borderComboBox.getSelectedItem());
		if (pageMultipleChoice.isOpaque())
			pageMultipleChoice.setBackground(bgComboBox.getSelectedColor());

		pageMultipleChoice.setSingleSelection(!multipleCheckBox.isSelected());
		int n = (Integer) nChoiceSpinner.getValue();
		pageMultipleChoice.changeNumberOfChoices(n);
		pageMultipleChoice.setQuestionPosition(topRadioButton.isSelected() ? BorderLayout.NORTH : BorderLayout.WEST);

		int m = 0;
		for (int i = 0; i < n; i++) {
			pageMultipleChoice.setChoice(i, choiceField[i].getText());
			if (choiceButton[i].isSelected())
				m++;
		}
		int[] key = new int[m];
		m = 0;
		for (int i = 0; i < n; i++) {
			if (choiceButton[i].isSelected())
				key[m++] = i;
		}
		pageMultipleChoice.setAnswer(key);
		if (pageMultipleChoice.getSingleSelection()) {
			String[] s = new String[n];
			for (int i = 0; i < n; i++) {
				s[i] = scriptArea[i].getText();
			}
			pageMultipleChoice.setScripts(s);
		}
		else {
			String[] s = new String[] { correctAnswerScriptArea.getText(), wrongAnswerScriptArea.getText() };
			pageMultipleChoice.setScripts(s);
		}

		pageMultipleChoice.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));

		if (answerButtonCheckBox.isSelected()) {
			pageMultipleChoice.addCheckAnswerButton();
		}
		else {
			pageMultipleChoice.removeCheckAnswerButton();
		}
		if (clearButtonCheckBox.isSelected()) {
			pageMultipleChoice.addClearAnswerButton();
		}
		else {
			pageMultipleChoice.removeClearAnswerButton();
		}

		String address = pageMultipleChoice.page.getAddress() + "#"
				+ ModelerUtilities.getSortableString(pageMultipleChoice.index, 3) + "%"
				+ PageMultipleChoice.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(address);
		if (q != null) {
			q.setQuestion(pageMultipleChoice.getQuestion() + '\n' + pageMultipleChoice.formatChoices()
					+ "\nMy answer is ");
			q.setReferenceAnswer(pageMultipleChoice.answerToString());
		}

		pageMultipleChoice.page.getSaveReminder().setChanged(true);
		pageMultipleChoice.page.settleComponentSize();

		return true;

	}

	void invoke(Page page) {

		pageMultipleChoice.setPage(page);
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeMultipleChoiceDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize multiple choice", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					widthField.selectAll();
					widthField.requestFocusInWindow();
				}
			});
		}

		AbstractButton[] choiceButtons = pageMultipleChoice.getChoiceButtons();
		nChoiceSpinner.setValue(new Integer(choiceButtons.length));
		uidField.setText(pageMultipleChoice.getUid());
		widthField.setValue(pageMultipleChoice.getPreferredSize().width);
		heightField.setValue(pageMultipleChoice.getPreferredSize().height);
		questionArea.setText(ModelerUtilities.deUnicode(pageMultipleChoice.getQuestion()));
		questionArea.setCaretPosition(0);
		// clear the script areas before populating them
		if (correctAnswerScriptArea != null)
			correctAnswerScriptArea.setText("");
		if (wrongAnswerScriptArea != null)
			wrongAnswerScriptArea.setText("");
		if (scriptArea != null) {
			for (JTextArea ta : scriptArea) {
				if (ta != null)
					ta.setText("");
			}
		}
		String[] s = pageMultipleChoice.getScripts();
		if (s != null && s.length > 1) {
			if (pageMultipleChoice.getSingleSelection()) {
				if (!initScriptPanelForSingleSelection())
					setupScriptTabbedPane();
				for (int i = 0; i < s.length; i++) {
					scriptArea[i].setText(s[i]);
					scriptArea[i].setCaretPosition(0);
				}
			}
			else {
				initScriptPanelForMultiSelection();
				correctAnswerScriptArea.setText(s[0]);
				correctAnswerScriptArea.setCaretPosition(0);
				wrongAnswerScriptArea.setText(s[1]);
				wrongAnswerScriptArea.setCaretPosition(0);
			}
		}
		borderComboBox.setSelectedItem(pageMultipleChoice.getBorderType());
		transparentCheckBox.setSelected(!pageMultipleChoice.isOpaque());
		bgComboBox.setColor(pageMultipleChoice.getBackground());
		multipleCheckBox.setSelected(!pageMultipleChoice.getSingleSelection());
		boolean northLayout = pageMultipleChoice.getQuestionPosition().equals(BorderLayout.NORTH);
		for (int i = 0; i < choiceButtons.length; i++) {
			choiceButton[i].setSelected(pageMultipleChoice.isCorrect(i));
			if (northLayout) {
				if (choiceButtons[i].getText() == null || choiceButtons[i].getText().length() < 3) {
					choiceField[i].setText(null);
					continue;
				}
			}
			choiceField[i].setText(pageMultipleChoice.getChoice(i));
			choiceField[i].setCaretPosition(0);
		}
		answerButtonCheckBox.setSelected(pageMultipleChoice.hasCheckAnswerButton());
		clearButtonCheckBox.setSelected(pageMultipleChoice.hasClearAnswerButton());
		if (pageMultipleChoice.getQuestionPosition().equals(BorderLayout.NORTH)) {
			topRadioButton.setSelected(true);
		}
		else {
			leftRadioButton.setSelected(true);
		}

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int m = 0;
				for (int i = 0; i < choiceButton.length; i++) {
					if (choiceButton[i].isSelected())
						m++;
				}
				if (m == 0) {
					String s = Modeler.getInternationalText("DidYouForgetToSetAnswers");
					String s2 = Modeler.getInternationalText("Reminder");
					JOptionPane.showMessageDialog(pageMultipleChoice, s != null ? s
							: "Did you forget to set the answers?", s2 != null ? s2 : "Reminder",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!confirm())
					return;
				dialog.dispose();
				cancel = false;
			}
		};

		downPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(downPanel, BorderLayout.SOUTH);

		final JLabel tip = new JLabel();
		downPanel.add(tip);

		String s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
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

		s = Modeler.getInternationalText("Help");
		button = new JButton(s != null ? s : "Help");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Modeler.openWithNewInstance(pageMultipleChoice.getPage().getNavigator().getHomeDirectory()
						+ "tutorial/choice.cml");
			}
		});
		downPanel.add(button);

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				switch (tabbedPane.getSelectedIndex()) {
				case 1:
					tip.setText("Design a question  ");
					break;
				case 2:
					tip.setText("Set the correct answer(s)  ");
					break;
				default:
					tip.setText(null);
				}
				downPanel.validate();
			}
		});
		contentPane.add(tabbedPane, BorderLayout.CENTER);

		Box box = Box.createVerticalBox();

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
		p1.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
		box.add(p1);

		s = Modeler.getInternationalText("QuestionPosition");
		p1.add(new JLabel((s != null ? s : "Question position") + " :", SwingConstants.LEFT));

		ButtonGroup bg = new ButtonGroup();
		s = Modeler.getInternationalText("Top");
		topRadioButton = new JRadioButton(s != null ? s : "Top");
		topRadioButton.setSelected(true);
		p1.add(topRadioButton);
		bg.add(topRadioButton);

		s = Modeler.getInternationalText("Left");
		leftRadioButton = new JRadioButton(s != null ? s : "Left");
		p1.add(leftRadioButton);
		bg.add(leftRadioButton);

		s = Modeler.getInternationalText("UniqueIdentifier");
		p1.add(new JLabel((s != null ? s : "Unique identifier") + ": ", SwingConstants.LEFT));
		uidField = new JTextField(10);
		uidField
				.setToolTipText("Type in a string to be used as the unique identifier of this multiple choice question.");
		uidField.addActionListener(okListener);
		p1.add(uidField);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
		p1.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		box.add(p1);

		s = Modeler.getInternationalText("MultipleSelection");
		multipleCheckBox = new JCheckBox(s != null ? s : "Multiple selection");
		multipleCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				changeChoiceButtonPanel(!multipleCheckBox.isSelected());
			}
		});
		p1.add(multipleCheckBox);

		s = Modeler.getInternationalText("NumberOfChoices");
		p1.add(new JLabel((s != null ? s : "Number of choices") + " :", SwingConstants.LEFT));
		nChoiceSpinner = new JSpinner(new SpinnerNumberModel(4, 2, 10, 1));
		nChoiceSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				changeChoicePanel((Integer) nChoiceSpinner.getValue());
				if (!multipleCheckBox.isSelected()) {
					setupScriptTabbedPane();
				}
			}
		});
		p1.add(nChoiceSpinner);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
		p1.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		box.add(p1);

		s = Modeler.getInternationalText("WidthLabel");
		p1.add(new JLabel((s != null ? s : "Width") + " :", SwingConstants.LEFT));
		widthField = new IntegerTextField(200, 20, 800, 5);
		widthField.addActionListener(okListener);
		p1.add(widthField);

		s = Modeler.getInternationalText("HeightLabel");
		p1.add(new JLabel((s != null ? s : "Height") + " :", SwingConstants.LEFT));
		heightField = new IntegerTextField(100, 20, 800, 5);
		heightField.addActionListener(okListener);
		p1.add(heightField);

		s = Modeler.getInternationalText("BorderLabel");
		p1.add(new JLabel((s != null ? s : "Border") + ":"));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p1.getBackground());
		borderComboBox.setPreferredSize(new Dimension(140, 25));
		borderComboBox.setSelectedIndex(0);
		p1.add(borderComboBox);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
		p1.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		box.add(p1);

		s = Modeler.getInternationalText("Transparent");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setSelected(true);
		p1.add(transparentCheckBox);

		s = Modeler.getInternationalText("BackgroundColorLabel");
		p1.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageMultipleChoice);
		p1.add(bgComboBox);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
		p1.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		box.add(p1);
		s = Modeler.getInternationalText("CheckAnswerButton");
		answerButtonCheckBox = new JCheckBox(s != null ? s : "Place a \"Check Answer\" button");
		p1.add(answerButtonCheckBox);

		s = Modeler.getInternationalText("ClearAnswerButton");
		clearButtonCheckBox = new JCheckBox(s != null ? s : "Place a \"Clear Answer\" button");
		p1.add(clearButtonCheckBox);

		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(box, BorderLayout.NORTH);

		s = Modeler.getInternationalText("Settings");
		tabbedPane.addTab(s != null ? s : "Settings", p2);

		JPanel p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		s = Modeler.getInternationalText("QuestionText");
		tabbedPane.addTab(s != null ? s : "Question", p);

		questionArea = new PastableTextArea();
		JScrollPane sp = new JScrollPane(questionArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setPreferredSize(new Dimension(400, 200));
		p.add(sp, BorderLayout.CENTER);

		p1 = new JPanel(new BorderLayout(10, 10));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		p = new JPanel(new BorderLayout());
		p.add(p1, BorderLayout.NORTH);

		initSetChoicePanel();
		p1.add(choiceButtonPanel, BorderLayout.WEST);
		p1.add(choiceFieldPanel, BorderLayout.CENTER);

		s = Modeler.getInternationalText("Choices");
		tabbedPane.addTab(s != null ? s : "Choices", new JScrollPane(p));

		scriptPanel = new JPanel(new BorderLayout());
		scriptPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setupScriptPanel();
		s = Modeler.getInternationalText("MultipleChoiceScripts");
		tabbedPane.addTab(s != null ? s : "Scripts", scriptPanel);

	}

	private void setupScriptPanel() {
		scriptPanel.removeAll();
		if (multipleCheckBox.isSelected()) {
			initScriptPanelForMultiSelection();
			scriptPanel.add(scriptPanelForMultiSelection, BorderLayout.CENTER);
		}
		else {
			initScriptPanelForSingleSelection();
			scriptPanel.add(scriptPanelForSingleSelection, BorderLayout.CENTER);
		}
	}

	private boolean initScriptPanelForSingleSelection() {
		if (scriptPanelForSingleSelection != null)
			return false;
		scriptPanelForSingleSelection = new JPanel(new BorderLayout(5, 5));
		String s = Modeler.getInternationalText("ScriptsToRunWhenAnswerIsChecked");
		JLabel label = new JLabel((s != null ? s : "Scripts to run when the Check Answer Button is hit") + ":");
		scriptPanelForSingleSelection.add(label, BorderLayout.NORTH);
		scriptTabbedPane = new JTabbedPane();
		setupScriptTabbedPane();
		scriptPanelForSingleSelection.add(scriptTabbedPane, BorderLayout.CENTER);
		scriptPanelForSingleSelection.add(createTipPane(), BorderLayout.SOUTH);
		return true;
	}

	private void setupScriptTabbedPane() {
		int n = (Integer) nChoiceSpinner.getValue();
		if (n == scriptTabbedPane.getTabCount())
			return;
		scriptTabbedPane.removeAll();
		if (scriptArea == null) {
			scriptArea = new PastableTextArea[n];
			for (int i = 0; i < n; i++)
				scriptArea[i] = new PastableTextArea();
		}
		if (scriptArea.length < n) {
			JTextArea[] ta = new PastableTextArea[n];
			System.arraycopy(scriptArea, 0, ta, 0, scriptArea.length);
			for (int i = scriptArea.length; i < n; i++) {
				ta[i] = new PastableTextArea();
			}
			scriptArea = ta;
		}
		char c = 'A';
		for (int i = 0; i < n; i++) {
			scriptTabbedPane.addTab("" + (c++), new JScrollPane(scriptArea[i]));
		}
	}

	private boolean initScriptPanelForMultiSelection() {
		if (scriptPanelForMultiSelection != null)
			return false;
		scriptPanelForMultiSelection = new JPanel(new SpringLayout());
		String s = Modeler.getInternationalText("ScriptsToRunWhenAnswerIsCheckedWrong");
		JLabel label = new JLabel((s != null ? s
				: "Scripts to run when the Check Answer Button is hit and the answer is wrong")
				+ ":");
		scriptPanelForMultiSelection.add(label);
		wrongAnswerScriptArea = new PastableTextArea();
		scriptPanelForMultiSelection.add(new JScrollPane(wrongAnswerScriptArea));
		s = Modeler.getInternationalText("ScriptsToRunWhenAnswerIsCheckedCorrect");
		label = new JLabel((s != null ? s
				: "Scripts to run when the Check Answer Button is hit and the answer is correct")
				+ ":");
		scriptPanelForMultiSelection.add(label);
		correctAnswerScriptArea = new PastableTextArea();
		scriptPanelForMultiSelection.add(new JScrollPane(correctAnswerScriptArea));
		scriptPanelForMultiSelection.add(createTipPane());
		ModelerUtilities.makeCompactGrid(scriptPanelForMultiSelection, 5, 1, 0, 0, 0, 5);
		return true;
	}

	private JEditorPane createTipPane() {
		JEditorPane ep = new JEditorPane(
				"text/html",
				"<html><body face=Verdana><table><tr><td align=center valign=center><b>Example >>> </b></td><td valign=top><font size=2>script:textbox:uid1:load filename1.html;<br>script:textbox:uid2:load filename2.html;</tr><tr><td colspan=2><font size=2>Tip: Copy and paste a line from the above example into the text areas and edit them accordingly.</td></tr></table></html>");
		ep.setEditable(false);
		ep.setBackground(scriptPanel.getBackground());
		return ep;
	}

	private void initSetChoicePanel() {

		int n = (Integer) nChoiceSpinner.getValue();
		gridLayout.setRows(n);
		boolean single = !multipleCheckBox.isSelected();

		choiceButtonPanel = new JPanel(gridLayout);

		if (choiceButton == null)
			choiceButton = new AbstractButton[n];
		changeChoiceButtonPanel(single);

		choiceFieldPanel = new JPanel(gridLayout);

		choiceField = new PastableTextField[n];
		for (int i = 0; i < choiceField.length; i++) {
			choiceField[i] = new PastableTextField();
			choiceFieldPanel.add(choiceField[i]);
		}

	}

	private void changeChoiceButtonPanel(boolean single) {
		if (scriptPanel != null) {
			setupScriptPanel();
			scriptPanel.validate();
		}
		if (bg != null)
			PageMultipleChoice.clearButtonGroup(bg);
		String s = Modeler.getInternationalText("Choice");
		if (single) {
			if (choiceButtonPanel.getComponentCount() > 0
					&& (choiceButtonPanel.getComponent(0) instanceof JRadioButton))
				return;
			choiceButtonPanel.removeAll();
			char c = 'A';
			if (bg == null)
				bg = new ButtonGroup();
			for (int i = 0; i < choiceButton.length; i++) {
				choiceButton[i] = new JRadioButton((s != null ? s : "Choice ") + c);
				choiceButton[i].setToolTipText("Click if this choice is the right answer");
				choiceButtonPanel.add(choiceButton[i]);
				bg.add(choiceButton[i]);
				c++;
			}
		}
		else {
			if (choiceButtonPanel.getComponentCount() > 0 && (choiceButtonPanel.getComponent(0) instanceof JCheckBox))
				return;
			choiceButtonPanel.removeAll();
			char c = 'A';
			for (int i = 0; i < choiceButton.length; i++) {
				choiceButton[i] = new JCheckBox((s != null ? s : "Choice ") + c);
				choiceButton[i].setToolTipText("Click if this choice is the right answer");
				choiceButtonPanel.add(choiceButton[i]);
				c++;
			}
		}
		choiceButtonPanel.validate();
	}

	private void changeChoicePanel(int n) {

		if (n == choiceButton.length)
			return;

		if (bg != null)
			PageMultipleChoice.clearButtonGroup(bg);

		gridLayout.setRows(n);
		choiceButtonPanel.setLayout(gridLayout);
		choiceFieldPanel.setLayout(gridLayout);

		AbstractButton[] ab = new AbstractButton[n];
		JTextField[] tf = new PastableTextField[n];
		if (n > choiceButton.length) {
			char c = 'A';
			for (int i = 0; i < choiceButton.length; i++) {
				ab[i] = choiceButton[i];
				tf[i] = choiceField[i];
				c++;
			}
			try {
				String s = Modeler.getInternationalText("Choice");
				for (int i = choiceButton.length; i < n; i++) {
					ab[i] = choiceButton[0].getClass().newInstance();
					ab[i].setText((s != null ? s : "Choice ") + c);
					ab[i].setToolTipText("Click if this choice is the right answer");
					tf[i] = new PastableTextField();
					c++;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			for (int i = 0; i < n; i++) {
				ab[i] = choiceButton[i];
				tf[i] = choiceField[i];
			}
		}
		choiceButton = ab;
		choiceField = tf;

		choiceButtonPanel.removeAll();
		choiceFieldPanel.removeAll();

		for (int i = 0; i < n; i++) {
			choiceButtonPanel.add(choiceButton[i]);
			choiceFieldPanel.add(choiceField[i]);
		}

		if (choiceButton[0] instanceof JRadioButton) {
			if (bg == null)
				bg = new ButtonGroup();
			for (int i = 0; i < n; i++)
				bg.add(choiceButton[i]);
		}

		choiceButtonPanel.validate();
		choiceFieldPanel.validate();

	}

}