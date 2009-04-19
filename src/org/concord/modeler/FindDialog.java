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
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.DialogLayout;
import org.concord.modeler.util.StringFinder;

class FindDialog extends JDialog {

	private Page page;
	private int selectedTab;
	private JTabbedPane tb;
	private JPanel replacePanel;
	private JTextField findField1;
	private JTextField findField2;
	private JTextField replaceField;
	private ButtonModel modelWord;
	private ButtonModel modelCase;
	private ButtonModel modelUp;
	private ButtonModel modelDown;

	private Map embeddedTextComponents;
	private Highlighter.HighlightPainter highlightPainter;

	public FindDialog(final Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Find and Replace", false);
		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.page = page0;
		highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(page.getSelectionColor());

		tb = new JTabbedPane();

		// "Find" panel

		ActionListener findNextAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				findNext(false, true);
			}
		};

		JPanel p1 = new JPanel(new BorderLayout());
		JPanel pc1 = new JPanel(new BorderLayout());

		JPanel pf = new JPanel(new DialogLayout(20, 5));
		pf.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 0));
		String s = Modeler.getInternationalText("Find");
		pf.add(new JLabel((s != null ? s : "Find what") + ":"));

		findField1 = new JTextField();
		findField1.addActionListener(findNextAction);
		pf.add(findField1);
		pc1.add(pf, BorderLayout.CENTER);

		JPanel po = new JPanel(new GridLayout(2, 2, 8, 2));
		s = Modeler.getInternationalText("Option");
		po.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Options"));

		s = Modeler.getInternationalText("WholeWordsOnly");
		JCheckBox checkBox = new JCheckBox(s != null ? s : "Whole words only");
		checkBox.setMnemonic('w');
		modelWord = checkBox.getModel();
		po.add(checkBox);

		ButtonGroup bg = new ButtonGroup();
		s = Modeler.getInternationalText("SearchUp");
		JRadioButton rButton = new JRadioButton(s != null ? s : "Search up");
		rButton.setMnemonic('u');
		modelUp = rButton.getModel();
		bg.add(rButton);
		po.add(rButton);

		s = Modeler.getInternationalText("MatchCase");
		checkBox = new JCheckBox(s != null ? s : "Match case");
		checkBox.setMnemonic('c');
		modelCase = checkBox.getModel();
		po.add(checkBox);

		s = Modeler.getInternationalText("SearchDown");
		rButton = new JRadioButton(s != null ? s : "Search down", true);
		rButton.setMnemonic('d');
		modelDown = rButton.getModel();
		bg.add(rButton);
		po.add(rButton);
		pc1.add(po, BorderLayout.SOUTH);

		p1.add(pc1, BorderLayout.CENTER);

		JPanel p01 = new JPanel(new FlowLayout());
		JPanel p = new JPanel(new GridLayout(2, 1, 2, 8));

		s = Modeler.getInternationalText("FindNext");
		JButton button = new JButton(s != null ? s : "Find Next");
		button.setMnemonic('f');
		button.addActionListener(findNextAction);
		p.add(button);

		s = Modeler.getInternationalText("CloseButton");
		button = new JButton(s != null ? s : "Close");
		button.setDefaultCapable(true);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedTab = tb.getSelectedIndex();
				dispose();
			}
		});
		p.add(button);

		p01.add(p);
		p1.add(p01, BorderLayout.EAST);

		s = Modeler.getInternationalText("Find");
		tb.addTab(s != null ? s : "Find", p1);

		// "Replace" panel

		ActionListener replaceAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeHighlights();
				findNext(true, true);
			}
		};

		replacePanel = new JPanel(new BorderLayout());

		JPanel pc2 = new JPanel(new BorderLayout());

		JPanel pc = new JPanel(new DialogLayout(20, 5));
		pc.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 0));

		s = Modeler.getInternationalText("ReplaceWhat");
		pc.add(new JLabel((s != null ? s : "Replace what") + ":"));
		findField2 = new JTextField();
		findField2.setDocument(findField1.getDocument());
		pc.add(findField2);

		s = Modeler.getInternationalText("ReplaceWith");
		pc.add(new JLabel((s != null ? s : "Replace with") + ":"));
		replaceField = new JTextField();
		replaceField.addActionListener(replaceAction);
		pc.add(replaceField);
		pc2.add(pc, BorderLayout.CENTER);

		po = new JPanel(new GridLayout(2, 2, 8, 2));
		s = Modeler.getInternationalText("Option");
		po.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Options"));

		s = Modeler.getInternationalText("WholeWordsOnly");
		checkBox = new JCheckBox(s != null ? s : "Whole words only");
		checkBox.setMnemonic('w');
		checkBox.setModel(modelWord);
		po.add(checkBox);

		bg = new ButtonGroup();
		s = Modeler.getInternationalText("SearchUp");
		rButton = new JRadioButton(s != null ? s : "Search up");
		rButton.setMnemonic('u');
		rButton.setModel(modelUp);
		bg.add(rButton);
		po.add(rButton);

		s = Modeler.getInternationalText("MatchCase");
		checkBox = new JCheckBox(s != null ? s : "Match case");
		checkBox.setMnemonic('c');
		checkBox.setModel(modelCase);
		po.add(checkBox);

		s = Modeler.getInternationalText("SearchDown");
		rButton = new JRadioButton(s != null ? s : "Search down", true);
		rButton.setMnemonic('d');
		rButton.setModel(modelDown);
		bg.add(rButton);
		po.add(rButton);
		pc2.add(po, BorderLayout.SOUTH);

		replacePanel.add(pc2, BorderLayout.CENTER);

		JPanel p02 = new JPanel(new FlowLayout());
		p = new JPanel(new GridLayout(3, 1, 2, 8));

		s = Modeler.getInternationalText("Replace");
		button = new JButton(s != null ? s : "Replace");
		button.setMnemonic('r');
		button.addActionListener(replaceAction);
		p.add(button);

		s = Modeler.getInternationalText("ReplaceAll");
		button = new JButton(s != null ? s : "Replace All");
		button.setMnemonic('a');
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				replaceAll();
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("CloseButton");
		button = new JButton(s != null ? s : "Close");
		button.setDefaultCapable(true);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectedTab = tb.getSelectedIndex();
				dispose();
			}
		});
		p.add(button);

		p02.add(p);
		replacePanel.add(p02, BorderLayout.EAST);

		// Make button columns the same size
		p01.setPreferredSize(p02.getPreferredSize());

		s = Modeler.getInternationalText("Replace");
		tb.addTab(s != null ? s : "Replace", replacePanel);

		getContentPane().add(tb, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				selectedTab = tb.getSelectedIndex();
			}

			public void windowActivated(WindowEvent e) {
				if (tb.getSelectedIndex() == 0) {
					findField1.requestFocusInWindow();
					findField1.selectAll();
				}
				else {
					findField2.requestFocusInWindow();
					findField2.selectAll();
				}
			}
		});

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(page));

	}

	private void replaceAll() {
		int counter = 0;
		while (true) {
			int result = findNext(true, false, page);
			if (result == StringFinder.STRING_FOUND) {
				counter++;
			}
			else if (result == StringFinder.SEARCH_ENDED) {
				break;
			}
			else {
				page.setCaretPosition(0);
				break;
			}
		}
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), counter + " replacements have been done",
				"Replacement", JOptionPane.INFORMATION_MESSAGE);
	}

	private void highlight(JTextComponent tc, int p1, int p2) {
		if (tc == null)
			return;
		Highlighter h = tc.getHighlighter();
		h.removeAllHighlights();
		try {
			h.addHighlight(p1, p2, highlightPainter);
		}
		catch (BadLocationException ble) {
			ble.printStackTrace(System.err);
			h.removeAllHighlights();
		}
	}

	private void findNext(boolean doReplace, boolean showWarnings) {
		if (embeddedTextComponents == null || embeddedTextComponents.isEmpty()) {
			findNext(doReplace, showWarnings, page);
			return;
		}
		if (modelUp.isSelected()) {
			searchUp(doReplace, showWarnings);
		}
		else {
			searchDown(doReplace, showWarnings);
		}
	}

	private void searchDown(boolean doReplace, boolean showWarnings) {
		if (findNext(doReplace, false, page) == StringFinder.STRING_FOUND)
			return;
		page.getHighlighter().removeAllHighlights();
		Object[] entry = embeddedTextComponents.values().toArray();
		int n = entry.length - 1;
		for (int i = 0; i < n; i++) {
			if (entry[i] instanceof JTextComponent) {
				if (findNext(doReplace, false, (JTextComponent) entry[i]) == StringFinder.STRING_FOUND)
					return;
				((JTextComponent) entry[i]).getHighlighter().removeAllHighlights();
			}
			else if (entry[i] instanceof Searchable) {
				if (findNext(doReplace, false, ((Searchable) entry[i]).getTextComponent()) == StringFinder.STRING_FOUND)
					return;
				((Searchable) entry[i]).getTextComponent().getHighlighter().removeAllHighlights();
			}
		}
		if (entry[n] instanceof JTextComponent) {
			findNext(doReplace, showWarnings, (JTextComponent) entry[n]);
		}
		else if (entry[n] instanceof Searchable) {
			JTextComponent tc = ((Searchable) entry[n]).getTextComponent();
			findNext(doReplace, showWarnings, tc);
		}
	}

	private void searchUp(boolean doReplace, boolean showWarnings) {
		page.getHighlighter().removeAllHighlights();
		Object[] entry = embeddedTextComponents.values().toArray();
		int n = entry.length - 1;
		for (int i = n; i > 0; i--) {
			if (entry[i] instanceof JTextComponent) {
				if (findNext(doReplace, false, (JTextComponent) entry[i]) == StringFinder.STRING_FOUND)
					return;
				((JTextComponent) entry[i]).getHighlighter().removeAllHighlights();
			}
			else if (entry[i] instanceof Searchable) {
				if (findNext(doReplace, false, ((Searchable) entry[i]).getTextComponent()) == StringFinder.STRING_FOUND)
					return;
				((Searchable) entry[i]).getTextComponent().getHighlighter().removeAllHighlights();
			}
		}
		if (entry[0] instanceof JTextComponent) {
			if (findNext(doReplace, false, (JTextComponent) entry[0]) == StringFinder.STRING_FOUND)
				return;
			((JTextComponent) entry[0]).getHighlighter().removeAllHighlights();
		}
		else if (entry[0] instanceof Searchable) {
			JTextComponent tc = ((Searchable) entry[0]).getTextComponent();
			if (findNext(doReplace, false, tc) == StringFinder.STRING_FOUND)
				return;
			tc.getHighlighter().removeAllHighlights();
		}

		findNext(doReplace, showWarnings, page);
	}

	private int findNext(boolean doReplace, boolean showWarnings, JTextComponent c) {

		Document doc = c.getDocument();
		String text = null;
		try {
			text = doc.getText(0, doc.getLength());
		}
		catch (BadLocationException e) {
			e.printStackTrace(System.err);
			return StringFinder.STRING_ERROR;
		}

		if (text == null)
			return StringFinder.STRING_ERROR;

		StringFinder stringFinder = new StringFinder(text);
		stringFinder.setKey(findField1.getText());
		stringFinder.setSearchUp(modelUp.isSelected());
		stringFinder.setMatchCase(modelCase.isSelected());
		stringFinder.setMatchWholeWord(modelWord.isSelected());
		stringFinder.setPosition(c.getCaretPosition());

		int i = stringFinder.findNext();

		if (i == StringFinder.STRING_FOUND) {
			c.select(stringFinder.getSelectionStart(), stringFinder.getSelectionEnd());
			if (doReplace) {
				String replacement = replaceField.getText();
				c.replaceSelection(replacement);
				stringFinder.select(stringFinder.getSelectionStart(), stringFinder.getSelectionStart()
						+ replacement.length());
				c.select(stringFinder.getSelectionStart(), stringFinder.getSelectionEnd());
			}
			if (stringFinder.isSearchUp()) {
				c.setCaretPosition(stringFinder.getSelectionEnd());
				c.moveCaretPosition(stringFinder.getSelectionStart());
			}
			else {
				c.moveCaretPosition(stringFinder.getSelectionEnd());
			}
			if (!c.hasFocus() && !(c instanceof Page)) {
				highlight(c, c.getSelectionStart(), c.getSelectionEnd());
			}
		}
		else {
			if (showWarnings) {
				switch (i) {
				case StringFinder.STRING_ERROR:
					warning("Please enter a valid string to search.");
					break;
				case StringFinder.SEARCH_ERROR:
					warning("Error in searching the document.");
					break;
				case StringFinder.SEARCH_ENDED:
					warning("Finished searching the document.");
					break;
				case StringFinder.STRING_NOT_FOUND:
					warning("No result was found.");
					break;
				}
			}
		}

		return i;

	}

	private void warning(final String message) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page), message, Modeler.NAME,
						JOptionPane.WARNING_MESSAGE);
			}
		});
	}

	public void setCurrentValues() {

		embeddedTextComponents = page.getEmbeddedComponent(Searchable.class);

		if (page.isEditable()) {
			String s = Modeler.getInternationalText("Replace");
			tb.addTab(s != null ? s : "Replace", replacePanel);
			s = Modeler.getInternationalText("FindAndReplace");
			setTitle(s == null ? "Find and Replace" : s);
			tb.setSelectedIndex(selectedTab);
		}
		else {
			tb.remove(replacePanel);
			String s = Modeler.getInternationalText("Find");
			setTitle(s == null ? "Find" : s);
			tb.setSelectedIndex(0);
		}
		modelDown.setSelected(true);
		modelWord.setSelected(false);
		modelCase.setSelected(false);

	}

}