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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.event.HotlinkListener;
import org.concord.modeler.event.MultipleChoiceEvent;
import org.concord.modeler.event.MultipleChoiceListener;
import org.concord.modeler.text.Page;
import org.concord.modeler.text.XMLCharacterEncoder;
import org.concord.modeler.ui.MultipleChoice;

public class PageMultipleChoice extends MultipleChoice implements Embeddable, MultipleChoiceListener {

	Page page;
	int index;
	String uid;
	private boolean marked;
	private boolean changable;
	private boolean wasOpaque;
	private Color textBackground;
	private static Color defaultTextBackground, defaultTextForeground;
	private MouseListener popupMouseListener;
	private JPopupMenu popupMenu;
	private static PageMultipleChoiceMaker maker;

	public PageMultipleChoice() {
		super();
		setPreferredSize(new Dimension(400, 200));
		setOpaque(true);
		if (defaultTextBackground == null)
			defaultTextBackground = questionBody.getBackground();
		if (defaultTextForeground == null)
			defaultTextForeground = questionBody.getForeground();
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				storeAnswer();
			}
		};
		if (choices != null) {
			for (AbstractButton x : choices)
				x.addActionListener(listener);
		}
		addMultipleChoiceListener(this);
		popupMouseListener = new PopupMouseListener(this);
		addMouseListener(popupMouseListener);
		questionBody.addMouseListener(popupMouseListener);
	}

	public PageMultipleChoice(PageMultipleChoice choice, Page parent) {
		this();
		setPage(parent);
		setSingleSelection(choice.getSingleSelection());
		int n = choice.getChoiceCount();
		changeNumberOfChoices(n);
		for (int i = 0; i < n; i++)
			setChoice(i, choice.getChoice(i));
		setAnswer(choice.getAnswer());
		setQuestion(choice.getQuestion());
		setOpaque(choice.textBackground == null ? choice.isOpaque() : choice.wasOpaque);
		if (choice.hasClearAnswerButton()) {
			addClearAnswerButton();
		}
		else {
			removeClearAnswerButton();
		}
		if (choice.hasCheckAnswerButton()) {
			addCheckAnswerButton();
		}
		else {
			removeCheckAnswerButton();
		}
		setBorderType(choice.getBorderType());
		setPreferredSize(choice.getPreferredSize());
		setBackground(choice.getBackground());
		setChangable(page.isEditable());
		String[] s = choice.scripts;
		if (s != null && s.length > 0) {
			scripts = new String[s.length];
			System.arraycopy(s, 0, scripts, 0, s.length);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		removeHotlinkListener(page);
		if (maker != null)
			maker.setObject(null); // make sure this object is not held by someone
		page = null;
	}

	private void storeAnswer() {
		if (page == null)
			return;
		String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
				+ PageMultipleChoice.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(key);
		if (q == null) {
			q = new QuestionAndAnswer(questionBody.getText() + '\n' + formatChoices() + "\nMy answer is ",
					getUserSelection(), answerToString());
			UserData.sharedInstance().putData(key, q);
		}
		else {
			q.setAnswer(getUserSelection());
		}
		q.setTimestamp(System.currentTimeMillis());
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		String s = Modeler.getInternationalText("CustomizeMultipleChoice");
		final JMenuItem miCustomize = new JMenuItem((s != null ? s : "Customize This Multiple Choice") + "...");
		miCustomize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new PageMultipleChoiceMaker(PageMultipleChoice.this);
				}
				else {
					maker.setObject(PageMultipleChoice.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(miCustomize);
		s = Modeler.getInternationalText("RemoveMultipleChoice");
		final JMenuItem miRemove = new JMenuItem(s != null ? s : "Remove This Multiple Choice");
		miRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(PageMultipleChoice.this);
				String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
						+ PageMultipleChoice.class.getName();
				UserData.sharedInstance().removeData(key);
			}
		});
		popupMenu.add(miRemove);
		s = Modeler.getInternationalText("CopyMultipleChoice");
		JMenuItem mi = new JMenuItem(s != null ? s : "Copy This Multiple Choice");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(PageMultipleChoice.this);
			}
		});
		popupMenu.add(mi);
		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				miCustomize.setEnabled(changable);
				miRemove.setEnabled(changable);
			}
		});
		popupMenu.pack();
	}

	public void setIndex(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		if (b) {
			wasOpaque = questionBody.isOpaque();
			setOpaque(true);
			textBackground = questionBody.getBackground();
		}
		else {
			setOpaque(wasOpaque);
		}
		setBackground(b ? page.getSelectionColor() : textBackground);
		questionBody.setForeground(b ? page.getSelectedTextColor() : defaultTextForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setBackground(Color c) {
		super.setBackground(c);
		if (Page.isNativeLookAndFeelUsed()) {
			if (checkAnswerButton != null)
				checkAnswerButton.setBackground(c);
			if (clearAnswerButton != null)
				clearAnswerButton.setBackground(c);
		}
	}

	public void setChangable(boolean b) {
		changable = b;
		if (b) {
			questionBody.getHtmlPane().removeLinkMonitor();
			if (!isChangable()) {
				if (choices != null) {
					for (AbstractButton x : choices)
						x.addMouseListener(popupMouseListener);
				}
			}
		}
		else {
			questionBody.getHtmlPane().addLinkMonitor();
			if (isChangable()) {
				if (choices != null) {
					for (AbstractButton x : choices)
						x.removeMouseListener(popupMouseListener);
				}
			}
		}
	}

	public boolean isChangable() {
		return changable;
	}

	public void setPage(Page p) {
		page = p;
		HotlinkListener[] listeners = getHotlinkListeners();
		if (listeners != null) {
			for (HotlinkListener x : listeners)
				removeHotlinkListener(x);
		}
		addHotlinkListener(page);
		/* Sun's HyperlinkListener added to make image map work */
		questionBody.getHtmlPane().addHyperlinkListener(page);
		setBase(page.getURL());
		questionBody.setFont(new Font(null, Font.PLAIN, Page.getDefaultFontSize()));
	}

	public Page getPage() {
		return page;
	}

	public void addCheckAnswerButton() {
		if (hasCheckAnswerButton())
			return;
		buttonPanel.add(checkAnswerButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	public void removeCheckAnswerButton() {
		if (hasCheckAnswerButton())
			buttonPanel.remove(checkAnswerButton);
		if (buttonPanel.getComponentCount() == 0)
			remove(buttonPanel);
	}

	public boolean hasCheckAnswerButton() {
		Component[] c = buttonPanel.getComponents();
		for (Component x : c) {
			if (x == checkAnswerButton)
				return true;
		}
		return false;
	}

	protected void addCheckAnswerHistory(String s) {
		String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
				+ PageMultipleChoice.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(key);
		if (q == null)
			return;
		q.addGuess(s);
	}

	public void addClearAnswerButton() {
		if (hasClearAnswerButton())
			return;
		buttonPanel.add(clearAnswerButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	public void removeClearAnswerButton() {
		if (hasClearAnswerButton())
			buttonPanel.remove(clearAnswerButton);
		if (buttonPanel.getComponentCount() == 0)
			remove(buttonPanel);
	}

	public boolean hasClearAnswerButton() {
		Component[] c = buttonPanel.getComponents();
		for (Component x : c) {
			if (x == clearAnswerButton)
				return true;
		}
		return false;
	}

	protected void clearAnswer() {
		super.clearAnswer();
		String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
				+ PageMultipleChoice.class.getName();
		UserData.sharedInstance().removeData(key);
	}

	public boolean changeNumberOfChoices(int n) {
		boolean b = super.changeNumberOfChoices(n);
		if (choices != null) {
			ActionListener listener = choices[0].getActionListeners()[0];
			if (listener != null) {
				for (AbstractButton x : choices) {
					if (x.getActionListeners().length > 0)
						continue;
					x.addActionListener(listener);
				}
			}
		}
		return b;
	}

	public static PageMultipleChoice create(Page page) {
		if (page == null)
			return null;
		PageMultipleChoice choice = new PageMultipleChoice();
		choice.setChangable(true);
		if (maker == null) {
			maker = new PageMultipleChoiceMaker(choice);
		}
		else {
			maker.setObject(choice);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return choice;
	}

	public String getBorderType() {
		return BorderManager.getBorder(this);
	}

	public void setBorderType(String s) {
		BorderManager.setBorder(this, s, page.getBackground());
	}

	public void setPreferredSize(Dimension dim) {
		super.setPreferredSize(dim);
		super.setMinimumSize(dim);
		super.setMaximumSize(dim);
		// workaround on Mac: Force JEditorPane to calculate the size of the current content
		if (Modeler.isMac())
			questionBody.getPreferredSize();
	}

	private StringBuffer choicesToString() {
		StringBuffer s = new StringBuffer();
		int n = getChoiceCount();
		for (int i = 0; i < n; i++) {
			s.append("<choice>" + XMLCharacterEncoder.encode(getChoice(i)) + "</choice>\n");
		}
		return s;
	}

	public String answerToString() {
		String s = "";
		for (int i : answer)
			s += i + " ";
		return s.trim();
	}

	void setScripts(String[] s) {
		scripts = s;
	}

	String[] getScripts() {
		return scripts;
	}

	public String scriptsToString() {
		int n = getChoiceCount();
		if (n < 1)
			return "";
		if (scripts == null || scripts.length <= 0)
			return "";
		StringBuffer sb = new StringBuffer(scripts[0]);
		if (getSingleSelection()) {
			if (n < 2)
				return sb.toString().trim();
			for (int i = 1; i < n; i++) {
				sb.append(" -choiceseparator- ");
				sb.append(scripts[i]);
			}
		}
		else {
			if (scripts.length > 1) {
				sb.append(" -choiceseparator- ");
				sb.append(scripts[1]);
			}
		}
		return sb.toString();
	}

	public void stringToScripts(String s) {
		if (s == null)
			return;
		String[] t = s.split("-choiceseparator-");
		for (int i = 0; i < t.length; i++)
			t[i] = t[i].trim();
		scripts = t;
	}

	private boolean isScriptOK(int i) {
		return scripts.length > i && scripts[i] != null && !scripts[i].trim().equals("");
	}

	public void choicePicked(MultipleChoiceEvent e) {
		// int[] x = e.getSelectedIndices();
		// for (int i = 0; i < x.length; i++) System.out.print(x[i]);
		// System.out.println("");
	}

	public void answerChecked(MultipleChoiceEvent e) {
		if (hasNoScripts())
			return;
		if (e.isSingleSelection()) {
			int i = e.getSelectedIndex();
			if (isScriptOK(i))
				page.openHyperlink(scripts[i]);
		}
		else {
			if (e.isAnswerCorrect()) {
				if (isScriptOK(0)) // scripts[0] stores the scripts for correct answer
					page.openHyperlink(scripts[0]);
			}
			else {
				if (isScriptOK(1)) // scripts[1] stores the scripts for wrong answer
					page.openHyperlink(scripts[1]);
			}
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (uid != null)
			sb.append("<uid>" + uid + "</uid>\n");
		sb.append("<single>" + getSingleSelection() + "</single>\n");
		sb.append("<row>" + getChoiceCount() + "</row>\n");
		sb.append("<width>" + getWidth() + "</width>\n");
		sb.append("<height>" + getHeight() + "</height>\n");
		sb.append("<title>" + XMLCharacterEncoder.encode(questionBody.getText()) + "</title>\n");
		if (hasCheckAnswerButton())
			sb.append("<submit>true</submit>\n");
		if (hasClearAnswerButton())
			sb.append("<clear>true</clear>\n");
		sb.append(choicesToString());
		sb.append("<answer>" + answerToString() + "</answer>\n");
		if (isOpaque()) {
			sb.append("<bgcolor>" + Integer.toString(getBackground().getRGB(), 16) + "</bgcolor>\n");
		}
		else {
			sb.append("<opaque>false</opaque>\n");
		}
		if (!getBorderType().equals(BorderManager.BORDER_TYPE[0])) {
			sb.append("<border>" + getBorderType() + "</border>\n");
		}
		String s = scriptsToString();
		if (s != null && !s.trim().equals("")) {
			sb.append("<script>" + XMLCharacterEncoder.encode(s) + "</script>\n");
		}
		return sb.toString();
	}

}
