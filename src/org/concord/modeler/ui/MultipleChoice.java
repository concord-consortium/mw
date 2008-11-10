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

package org.concord.modeler.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

import org.concord.modeler.BasicPageTextBox;
import org.concord.modeler.HtmlService;
import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.Searchable;
import org.concord.modeler.event.HotlinkListener;
import org.concord.modeler.event.MultipleChoiceEvent;
import org.concord.modeler.event.MultipleChoiceListener;

public abstract class MultipleChoice extends JPanel implements HtmlService, Searchable {

	protected int[] answer;
	protected String[] scripts;
	protected AbstractButton[] choices;
	protected BasicPageTextBox questionBody;
	protected JButton checkAnswerButton, clearAnswerButton;
	private ButtonGroup buttonGroup;
	private AbstractButton invisibleButton;
	private JPanel choicePanel;
	protected JPanel buttonPanel;
	private TextComponentPopupMenu popupMenu;
	private List<MultipleChoiceListener> listeners;

	private MouseAdapter popupListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			if (!isTextSelected())
				return;
			if (ModelerUtilities.isRightClick(e)) {
				questionBody.requestFocusInWindow();
				if (popupMenu == null)
					popupMenu = new TextComponentPopupMenu(questionBody.getTextComponent());
				popupMenu.show(questionBody, e.getX(), e.getY());
			}
		}
	};

	private ItemListener itemListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource() instanceof JRadioButton) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					int[] si = getUserSelectedIndices();
					if (si != null && si.length == 1) {
						notifyChoicePicked(new MultipleChoiceEvent(MultipleChoice.this, si[0]));
					}
				}
			}
			else if (e.getSource() instanceof JCheckBox) {
				int[] si = getUserSelectedIndices();
				if (si != null) {
					notifyChoicePicked(new MultipleChoiceEvent(MultipleChoice.this, si));
				}
			}
		}
	};

	public MultipleChoice() {
		this(true, "<html><body marginwidth=5 marginheight=5>Question</body></html>", null);
	}

	public MultipleChoice(boolean singleSelection, String question, String[] options) {

		super(new BorderLayout(5, 5));

		if (options == null) {
			options = new String[4];
			options[0] = "A.";
			options[1] = "B.";
			options[2] = "C.";
			options[3] = "D.";
		}

		int n = options.length;
		scripts = new String[n];
		Arrays.fill(scripts, "");

		if (singleSelection) {
			invisibleButton = new JToggleButton();
			buttonGroup = new ButtonGroup();
			buttonGroup.add(invisibleButton);
			choices = new JRadioButton[n];
			for (int i = 0; i < n; i++) {
				choices[i] = new JRadioButton(options[i]);
				choices[i].addItemListener(itemListener);
				buttonGroup.add(choices[i]);
			}
		}
		else {
			choices = new JCheckBox[n];
			for (int i = 0; i < n; i++) {
				choices[i] = new JCheckBox(options[i]);
				choices[i].addItemListener(itemListener);
			}
		}

		questionBody = new BasicPageTextBox() {
			public void createPopupMenu() {
			}

			public JPopupMenu getPopupMenu() {
				return null;
			}
		};
		questionBody.setText(question);
		questionBody.setEditable(false);
		questionBody.addMouseListener(popupListener);

		add(questionBody, BorderLayout.NORTH);

		choicePanel = new JPanel();
		layChoices();
		add(choicePanel, BorderLayout.CENTER);

		buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		questionBody.setBackground(buttonPanel.getBackground());
		add(buttonPanel, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("CheckAnswer");
		checkAnswerButton = new JButton(s != null ? s : "Check Answer");
		checkAnswerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean selected = false;
				for (int i = 0; i < choices.length; i++) {
					if (choices[i].isSelected()) {
						selected = true;
						break;
					}
				}
				if (!selected) {
					String s = Modeler.getInternationalText("YouHaveNotChosenAnswer");
					JOptionPane.showMessageDialog(MultipleChoice.this, s != null ? s
							: "You haven't chosen your answer.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (answer == null) {
					String s = Modeler.getInternationalText("QuestionDesignerDidNotProvideAnswer");
					JOptionPane.showMessageDialog(MultipleChoice.this, s != null ? s
							: "The question designer didn't provide an answer.", "Answer unknown",
							JOptionPane.ERROR_MESSAGE);
				}
				else {
					if (getSingleSelection()) {
						int[] selectedIndices = getUserSelectedIndices();
						if (selectedIndices != null && selectedIndices.length == 1) {
							int si = selectedIndices[0];
							notifyAnswerChecked(new MultipleChoiceEvent(MultipleChoice.this, si));
							boolean b = si == answer[0];
							if (b) {
								if (hasNoScripts()) {
									String s = Modeler.getInternationalText("Correct");
									JOptionPane.showMessageDialog(MultipleChoice.this, s != null ? s : "Correct!",
											"Answer", JOptionPane.INFORMATION_MESSAGE);
								}
							}
							else {
								if (hasNoScripts()) {
									String s = Modeler.getInternationalText("TryAgain");
									JOptionPane.showMessageDialog(MultipleChoice.this, s != null ? s : "Try again!",
											"Answer", JOptionPane.INFORMATION_MESSAGE);
								}
								for (int i = 0; i < choices.length; i++) {
									if (choices[i].isSelected()) {
										addCheckAnswerHistory((char) ('a' + i) + "");
										break;
									}
								}
							}
						}
					}
					else { // multiple selection
						boolean b = true;
						boolean p;
						for (int i = 0; i < choices.length; i++) {
							p = false;
							for (int j = 0; j < answer.length; j++) {
								if (answer[j] == i) {
									p = true;
									break;
								}
							}
							if ((p && !choices[i].isSelected()) || (!p && choices[i].isSelected())) {
								b = false;
								break;
							}
						}
						notifyAnswerChecked(new MultipleChoiceEvent(MultipleChoice.this, b));
						if (b) {
							if (hasNoScripts()) {
								String s = Modeler.getInternationalText("Correct");
								JOptionPane.showMessageDialog(MultipleChoice.this, s != null ? s : "Correct!",
										"Answer", JOptionPane.INFORMATION_MESSAGE);
							}
						}
						else {
							if (hasNoScripts()) {
								String s = Modeler.getInternationalText("TryAgain");
								JOptionPane.showMessageDialog(MultipleChoice.this, s != null ? s : "Try again!",
										"Answer", JOptionPane.INFORMATION_MESSAGE);
							}
							String s = "";
							char c = 'a';
							for (int i = 0; i < choices.length; i++) {
								if (choices[i].isSelected()) {
									s += (char) (c + i) + " ";
								}
							}
							if (!s.equals(""))
								addCheckAnswerHistory(s.trim());
						}
					}
				}
			}
		});
		buttonPanel.add(checkAnswerButton);

		s = Modeler.getInternationalText("ClearAnswer");
		clearAnswerButton = new JButton(s != null ? s : "Clear Answer");
		clearAnswerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearAnswer();
			}
		});

	}

	protected void destroy() {
		questionBody.destroy();
	}

	protected boolean hasNoScripts() {
		if (scripts == null || scripts.length == 0)
			return true;
		for (String s : scripts) {
			if (s != null && !s.trim().equals(""))
				return false;
		}
		return true;
	}

	public void addMultipleChoiceListener(MultipleChoiceListener mcl) {
		if (listeners == null)
			listeners = new ArrayList<MultipleChoiceListener>();
		if (!listeners.contains(mcl))
			listeners.add(mcl);
	}

	public void removeMultipleChoiceListener(MultipleChoiceListener mcl) {
		if (listeners != null)
			listeners.remove(mcl);
	}

	private void notifyAnswerChecked(MultipleChoiceEvent e) {
		if (listeners == null)
			return;
		for (MultipleChoiceListener mcl : listeners) {
			mcl.answerChecked(e);
		}
	}

	private void notifyChoicePicked(MultipleChoiceEvent e) {
		if (listeners == null)
			return;
		for (MultipleChoiceListener mcl : listeners) {
			mcl.choicePicked(e);
		}
	}

	protected abstract void addCheckAnswerHistory(String s);

	protected void clearAnswer() {
		if (choices == null)
			return;
		if (getSingleSelection()) {
			invisibleButton.setSelected(true);
		}
		else {
			for (AbstractButton c : choices) {
				c.setSelected(false);
			}
		}
	}

	public synchronized void addHotlinkListener(HotlinkListener listener) {
		questionBody.addHotlinkListener(listener);
	}

	public synchronized void removeHotlinkListener(HotlinkListener listener) {
		questionBody.removeHotlinkListener(listener);
	}

	public synchronized HotlinkListener[] getHotlinkListeners() {
		return questionBody.getHotlinkListeners();
	}

	public String getBackgroundImage() {
		return questionBody.getBackgroundImage();
	}

	public List<String> getImageNames() {
		return questionBody.getImageNames();
	}

	public String getAttribute(String tag, String name) {
		return questionBody.getAttribute(tag, name);
	}

	public void setBase(File file) {
		questionBody.setBase(file);
	}

	public void setBase(URL u) {
		questionBody.setBase(u);
	}

	public URL getBase() {
		return questionBody.getBase();
	}

	public void cacheLinkedFiles(String codeBase) {
		questionBody.cacheLinkedFiles(codeBase);
	}

	public TextBox getQuestionTextBox() {
		return questionBody;
	}

	public JTextComponent getTextComponent() {
		return questionBody.getTextComponent();
	}

	public boolean isTextSelected() {
		return questionBody.getTextComponent().getSelectedText() != null;
	}

	public AbstractButton[] getChoiceButtons() {
		return choices;
	}

	public void setQuestion(String question) {
		if (questionBody == null)
			return;
		questionBody.setText(question);
	}

	public String getQuestion() {
		if (questionBody == null)
			return null;
		return questionBody.getText();
	}

	public void setChoice(int i, String s) {
		char c = (char) ('A' + i);
		s = s.trim();
		if (s.toLowerCase().startsWith("<html>")) {
			String s1 = s.substring(0, 6);
			String s2 = s.substring(6);
			choices[i].setText(s1 + c + ". " + s2);
		}
		else {
			choices[i].setText(c + ". " + s);
		}
	}

	public String getChoice(int i) {
		String s = choices[i].getText();
		if (s.toLowerCase().startsWith("<html>"))
			return s.substring(0, 6) + s.substring(9);
		return s.substring(3);
	}

	public String[] getChoices() {
		if (choices == null)
			return null;
		String[] s = new String[choices.length];
		for (int i = 0; i < s.length; i++)
			s[i] = getChoice(i);
		return s;
	}

	/** return a readable form of the choices */
	public String formatChoices() {
		String[] ss = getChoices();
		String s = "";
		char c = 'a';
		for (int i = 0; i < ss.length; i++) {
			if (ss[i].toLowerCase().indexOf("<html>") != -1) {
				s += "(" + c + ") " + ModelerUtilities.extractPlainText(ss[i]) + '\n';
			}
			else {
				s += "(" + c + ") " + ss[i] + '\n';
			}
			c++;
		}
		return s;
	}

	public int getChoiceCount() {
		if (choices == null)
			return 0;
		return choices.length;
	}

	public boolean isCorrect(int i) {
		if (answer == null)
			return false;
		for (int j = 0; j < answer.length; j++) {
			if (answer[j] == i)
				return true;
		}
		return false;
	}

	public boolean isSelected(int i) {
		if (choices == null)
			return false;
		if (i < 0 || i >= choices.length)
			return false;
		if (choices[i] == null)
			return false;
		return choices[i].isSelected();
	}

	public void setSelected(int i, boolean b) {
		setSelected(i, b, true);
	}

	public void setSelected(int i, boolean b, boolean notifyListeners) {
		if (choices == null)
			return;
		if (i < 0 || i >= choices.length)
			return;
		if (choices[i] == null)
			return;
		if (!notifyListeners) {
			ItemListener[] il = choices[i].getItemListeners();
			if (il != null) {
				for (ItemListener x : il)
					choices[i].removeItemListener(x);
			}
			ActionListener[] al = choices[i].getActionListeners();
			if (al != null) {
				for (ActionListener x : al)
					choices[i].removeActionListener(x);
			}
			choices[i].setSelected(b);
			if (il != null) {
				for (ItemListener x : il)
					choices[i].addItemListener(x);
			}
			if (al != null) {
				for (ActionListener x : al)
					choices[i].addActionListener(x);
			}
		}
		else {
			choices[i].setSelected(b);
		}
	}

	public void clearSelection() {
		if (choices == null)
			return;
		for (AbstractButton c : choices)
			c.setSelected(false);
	}

	public String getUserSelection() {
		String s = null;
		for (int i = 0; i < choices.length; i++) {
			if (choices[i].isSelected()) {
				if (s == null) {
					s = i + "";
				}
				else {
					s += " " + i;
				}
			}
		}
		return s;
	}

	public int[] getUserSelectedIndices() {
		int n = 0;
		for (int i = 0; i < choices.length; i++) {
			if (choices[i].isSelected())
				n++;
		}
		int[] k = new int[n];
		n = 0;
		for (int i = 0; i < choices.length; i++) {
			if (choices[i].isSelected()) {
				k[n] = i;
				n++;
			}
		}
		return k;
	}

	public void setBackground(Color c) {
		super.setBackground(c);
		if (questionBody != null)
			questionBody.setBackground(c);
		if (buttonPanel != null)
			buttonPanel.setBackground(c);
		if (choicePanel != null)
			choicePanel.setBackground(c);
		if (choices != null) {
			for (AbstractButton ab : choices)
				ab.setBackground(c);
		}
	}

	public void setOpaque(boolean b) {
		super.setOpaque(b);
		if (buttonPanel != null)
			buttonPanel.setOpaque(b);
		if (choicePanel != null)
			choicePanel.setOpaque(b);
		if (questionBody != null)
			questionBody.setOpaque(b);
		if (choices != null) {
			for (AbstractButton c : choices)
				c.setOpaque(b);
		}
	}

	public boolean isOpaque() {
		if (questionBody == null)
			return super.isOpaque();
		return questionBody.isOpaque();
	}

	private void layChoices() {

		if (choices == null)
			return;

		choicePanel.removeAll();
		// choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
		choicePanel.setLayout(new java.awt.GridLayout(choices.length, 1, 0, 0));
		choicePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		for (AbstractButton c : choices) {
			if (c != null) {
				c.setAlignmentX(LEFT_ALIGNMENT);
				choicePanel.add(c);
			}
		}

		validate();

	}

	public void setAnswer(int[] i) {
		answer = i;
	}

	public int[] getAnswer() {
		return answer;
	}

	public boolean getSingleSelection() {
		if (choices == null)
			return true;
		return choices[0] instanceof JRadioButton;
	}

	public static void clearButtonGroup(ButtonGroup g) {
		if (g == null || g.getButtonCount() == 0)
			return;
		Enumeration e = g.getElements();
		List<AbstractButton> list = new ArrayList<AbstractButton>();
		while (e.hasMoreElements())
			list.add((AbstractButton) e.nextElement());
		for (AbstractButton b : list) {
			g.remove(b);
		}
	}

	public boolean changeNumberOfChoices(int n) {
		if (n == choices.length)
			return false;
		AbstractButton[] ab = new AbstractButton[n];
		String[] s2 = null;
		if (n > choices.length) {
			for (int i = 0; i < choices.length; i++) {
				ab[i] = choices[i];
			}
			if (getSingleSelection()) {
				s2 = new String[n];
				Arrays.fill(s2, "");
				System.arraycopy(scripts, 0, s2, 0, choices.length);
			}
			try {
				for (int i = choices.length; i < n; i++) {
					ab[i] = choices[0].getClass().newInstance();
					ab[i].setOpaque(isOpaque());
					if (isOpaque())
						ab[i].setBackground(getBackground());
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			for (int i = 0; i < n; i++) {
				ab[i] = choices[i];
			}
			if (getSingleSelection()) {
				s2 = new String[n];
				System.arraycopy(scripts, 0, s2, 0, n);
			}
		}
		boolean redoBG = n > choices.length;
		choices = ab;
		if (s2 != null)
			scripts = s2;
		if (redoBG && getSingleSelection()) {
			if (buttonGroup == null) {
				buttonGroup = new ButtonGroup();
			}
			else {
				clearButtonGroup(buttonGroup);
			}
			for (AbstractButton c : choices) {
				buttonGroup.add(c);
			}
			buttonGroup.add(invisibleButton);
		}
		layChoices();
		return true;
	}

	public boolean setSingleSelection(boolean b) {
		if (choices == null)
			return false;
		if (b) {
			if (getSingleSelection())
				return false;
			if (invisibleButton == null)
				invisibleButton = new JRadioButton();
			if (buttonGroup == null) {
				buttonGroup = new ButtonGroup();
			}
			else {
				clearButtonGroup(buttonGroup);
			}
			buttonGroup.add(invisibleButton);
			JRadioButton[] rb = new JRadioButton[choices.length];
			for (int i = 0; i < choices.length; i++) {
				rb[i] = new JRadioButton();
				rb[i].setFont(getFont());
				rb[i].setText(choices[i].getText());
				rb[i].setIcon(choices[i].getIcon());
				ActionListener[] al = choices[i].getActionListeners();
				if (al != null) {
					for (ActionListener x : al)
						rb[i].addActionListener(x);
				}
				ItemListener[] il = choices[i].getItemListeners();
				if (il != null) {
					for (ItemListener x : il)
						rb[i].addItemListener(x);
				}
				buttonGroup.add(rb[i]);
			}
			choices = rb;
			for (AbstractButton c : choices) {
				c.setBackground(getBackground());
				c.setOpaque(isOpaque());
			}
		}
		else {
			if (!getSingleSelection())
				return false;
			JCheckBox[] cb = new JCheckBox[choices.length];
			for (int i = 0; i < choices.length; i++) {
				cb[i] = new JCheckBox();
				cb[i].setFont(getFont());
				cb[i].setText(choices[i].getText());
				cb[i].setIcon(choices[i].getIcon());
				ActionListener[] al = choices[i].getActionListeners();
				if (al != null) {
					for (ActionListener x : al)
						cb[i].addActionListener(x);
				}
				ItemListener[] il = choices[i].getItemListeners();
				if (il != null) {
					for (ItemListener x : il)
						cb[i].addItemListener(x);
				}
			}
			choices = cb;
			for (AbstractButton c : choices) {
				c.setBackground(getBackground());
				c.setOpaque(isOpaque());
			}
			clearButtonGroup(buttonGroup);
		}
		layChoices();
		return true;
	}

}