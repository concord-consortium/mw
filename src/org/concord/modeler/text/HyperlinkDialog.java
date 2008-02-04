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

package org.concord.modeler.text;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;

import org.concord.modeler.BookmarkManager;
import org.concord.modeler.HistoryManager;
import org.concord.modeler.Modeler;
import org.concord.modeler.ui.PastableTextField;
import org.concord.modeler.util.FileUtilities;

class HyperlinkDialog extends JDialog {

	private JCheckBox newWindowCheckBox;
	private JLabel textLabel;
	private JTextField urlField;
	private JList urlList;
	private Page page;
	private JRadioButton recent, bookmark;
	private JButton clearButton, advancedButton;
	private int caretPosition = -1;
	private AdvancedHyperlinkDialog advancedDialog;

	HyperlinkDialog(Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Insert hyperlink", true);
		String s = Modeler.getInternationalText("HyperlinkButton");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.page = page0;

		Container container = getContentPane();
		container.setLayout(new BorderLayout());

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
		container.add(BorderLayout.CENTER, panel);

		GridBagConstraints c = new GridBagConstraints();

		Dimension longField = new Dimension(240, 20);
		Dimension hugeField = new Dimension(240, 80);

		// Spacing between the label and the field
		EmptyBorder border = new EmptyBorder(new Insets(0, 0, 0, 10));

		// add some space around all my components to avoid cluttering
		c.insets = new Insets(2, 2, 2, 2);

		// anchors all my components to the west
		c.anchor = GridBagConstraints.WEST;

		// Short description label and field
		s = Modeler.getInternationalText("HyperlinkedText");
		JLabel label = new JLabel((s != null ? s : "Hyperlinked text") + ":");
		label.setBorder(border); // add some space on the right
		c.gridx = 0;
		c.gridy = 0;
		panel.add(label, c);
		textLabel = new JLabel();
		textLabel.setPreferredSize(longField);
		c.gridx = 1;
		c.weightx = 1.0; // use all available horizontal space
		c.gridwidth = 3; // spans across 3 columns
		c.fill = GridBagConstraints.HORIZONTAL; // fills up the 3 columns
		panel.add(textLabel, c);

		// "open in a new window" check box
		s = Modeler.getInternationalText("WhenClicked");
		label = new JLabel((s != null ? s : "When clicked") + ":");
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = 1;
		panel.add(label, c);
		s = Modeler.getInternationalText("OpenHyperlinkInNewWindow");
		newWindowCheckBox = new JCheckBox(s != null ? s : "Open the link in a new window");
		newWindowCheckBox.setPreferredSize(longField);
		newWindowCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					advancedButton.setEnabled(true);
				}
				else if (e.getStateChange() == ItemEvent.DESELECTED) {
					advancedButton.setEnabled(false);
				}
			}
		});
		c.gridx = 1;
		c.weightx = 1.0; // use all available horizontal space
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL; // fills up the 3 columns
		panel.add(newWindowCheckBox, c);

		// Description label and field
		s = Modeler.getInternationalText("InputAddress");
		label = new JLabel((s != null ? s : "Input address") + ":");
		label.setBorder(border);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1.0;
		panel.add(label, c);
		urlField = new PastableTextField();
		urlField.setPreferredSize(longField);
		urlField.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				urlList.clearSelection();
			}
		});
		urlField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						HyperlinkDialog.this.dispose();
						page.requestFocus();
					}
				});
			}
		});

		c.gridx = 1;
		c.weightx = 1.0; // use all available horizontal space
		c.weighty = 1.0; // use all available vertical space
		c.gridwidth = 3; // spans across 3 columns
		c.fill = GridBagConstraints.HORIZONTAL; // fills up the cols & rows
		panel.add(urlField, c);

		// list label and field
		s = Modeler.getInternationalText("OrSelectFromList");
		label = new JLabel((s != null ? s : "Or select from list") + ":");
		label.setBorder(border);
		c.anchor = GridBagConstraints.NORTH;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 0.0;
		panel.add(label, c);

		urlList = new JList();
		urlList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		urlList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					urlField.setText(urlList.getSelectedValue().toString());
				}
			}
		});

		JScrollPane scroller = new JScrollPane(urlList);
		scroller.setPreferredSize(hugeField);
		c.gridx = 1;
		c.weightx = 1.0; // use all available horizontal space
		c.weighty = 1.0; // use all available vertical space
		c.gridwidth = 3; // spans across 3 columns
		c.gridheight = 3; // spans across 3 rows
		c.fill = GridBagConstraints.BOTH; // fills up the cols & rows
		panel.add(scroller, c);

		// list sources
		s = Modeler.getInternationalText("ListSources");
		label = new JLabel((s != null ? s : "List sources") + ":");
		label.setBorder(border);
		c.gridx = 0;
		c.gridy = 6;
		panel.add(label, c);
		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		// Creates a FlowLayout layout JPanel with 5 pixel of horizontal gaps and no vertical gaps
		ButtonGroup group = new ButtonGroup();

		s = Modeler.getInternationalText("RecentURLs");
		recent = new JRadioButton(s != null ? s : "Recent URLs");
		recent.setSelected(true);
		recent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ListModel lm = page.getNavigator().getComboBox().getModel();
				Vector<String> v = new Vector<String>();
				for (int j = lm.getSize(), i = 0; i < j; i++) {
					if (FileUtilities.isRemote((String) lm.getElementAt(i))) {
						v.add((String) lm.getElementAt(i));
					}
				}
				urlList.setListData(v);
			}
		});
		group.add(recent);
		radioPanel.add(recent);

		s = Modeler.getInternationalText("Bookmark");
		bookmark = new JRadioButton(s != null ? s : "Bookmarks");
		bookmark.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Vector<String> v = new Vector<String>();
				for (Iterator it = BookmarkManager.sharedInstance().getBookmarks().keySet().iterator(); it.hasNext();) {
					String s = BookmarkManager.sharedInstance().getBookmarks().get(it.next());
					if (FileUtilities.isRemote(s))
						v.addElement(s);
				}
				urlList.setListData(v);
			}
		});
		group.add(bookmark);
		radioPanel.add(bookmark);
		c.gridx = 1;
		c.gridwidth = 3;
		panel.add(radioPanel, c);

		s = Modeler.getInternationalText("Clear");
		clearButton = new JButton(s != null ? s : "Clear");
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				urlField.setText(null);
				urlList.clearSelection();
			}
		});
		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(clearButton, c);

		s = Modeler.getInternationalText("MoreSettings");
		advancedButton = new JButton(s != null ? s : "More");
		advancedButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						advancedDialog.setCurrentValues();
						advancedDialog.pack();
						advancedDialog.setVisible(true);
					}
				});
			}
		});
		c.gridy = 1;
		panel.add(advancedButton, c);

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						HyperlinkDialog.this.dispose();
						page.requestFocus();
					}
				});
			}
		});
		c.anchor = GridBagConstraints.SOUTH; // anchor north
		c.gridy = 5;
		panel.add(button, c);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				urlField.setText(null);
				textLabel.setText(null);
				urlList.clearSelection();
				if (caretPosition > 0)
					page.setCaretPosition(caretPosition);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						HyperlinkDialog.this.dispose();
						page.requestFocus();
					}
				});
			}
		});
		c.gridy = 6;
		panel.add(button, c);

		pack();
		setLocationRelativeTo(getOwner());

		advancedDialog = new AdvancedHyperlinkDialog(JOptionPane.getFrameForComponent(page0));
		advancedDialog.setLocationRelativeTo(this);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						HyperlinkDialog.this.dispose();
					}
				});
				// set the caret to its original position
				if (caretPosition > 0)
					page.setCaretPosition(caretPosition);
				page.requestFocus();
			}

			public void windowActivated(WindowEvent e) {
				// transfer focus to the input URL field
				urlField.selectAll();
				urlField.requestFocus();
			}
		});

	}

	private void confirm() {

		String url = urlField.getText();

		if (url == null || url.trim().equals("")) {
			Element elem = page.getStyledDocument().getCharacterElement(page.getSelectionEnd() - 1);
			if (elem != null) {
				AttributeSet as = elem.getAttributes();
				if (as != null) {
					SimpleAttributeSet a = new SimpleAttributeSet(as);
					a.removeAttribute(HTML.Attribute.HREF);
					StyleConstants.setUnderline(a, false);
					StyleConstants.setForeground(a, page.getCaretColor());
					page.setCharacterAttributes(a, true);
					page.getSaveReminder().setChanged(true);
				}
			}
			return;
		}

		Element element = page.getStyledDocument().getCharacterElement(page.getSelectionEnd() - 1);
		SimpleAttributeSet a = new SimpleAttributeSet(element.getAttributes());
		a.addAttribute(HTML.Attribute.HREF, XMLCharacterEncoder.encode(url));
		if (newWindowCheckBox.isSelected()) {
			a.addAttribute(HTML.Attribute.TARGET, "_blank");
			a.addAttribute(HyperlinkParameter.ATTRIBUTE_RESIZABLE, advancedDialog.param.getResizable() ? Boolean.TRUE
					: Boolean.FALSE);
			a.addAttribute(HyperlinkParameter.ATTRIBUTE_FULLSCREEN, advancedDialog.param.getFullscreen() ? Boolean.TRUE
					: Boolean.FALSE);
			a.addAttribute(HyperlinkParameter.ATTRIBUTE_TOOLBAR, advancedDialog.param.getToolbar() ? Boolean.TRUE
					: Boolean.FALSE);
			a.addAttribute(HyperlinkParameter.ATTRIBUTE_MENUBAR, advancedDialog.param.getMenubar() ? Boolean.TRUE
					: Boolean.FALSE);
			a.addAttribute(HyperlinkParameter.ATTRIBUTE_STATUSBAR, advancedDialog.param.getStatusbar() ? Boolean.TRUE
					: Boolean.FALSE);
			a.addAttribute(HyperlinkParameter.ATTRIBUTE_LEFT, new Integer(advancedDialog.param.getLeft()));
			a.addAttribute(HyperlinkParameter.ATTRIBUTE_TOP, new Integer(advancedDialog.param.getTop()));
			if (advancedDialog.param.getWidth() > 0) {
				a.addAttribute(HyperlinkParameter.ATTRIBUTE_WIDTH, new Integer(advancedDialog.param.getWidth()));
			}
			if (advancedDialog.param.getHeight() > 0) {
				a.addAttribute(HyperlinkParameter.ATTRIBUTE_HEIGHT, new Integer(advancedDialog.param.getHeight()));
			}
		}
		else {
			a.removeAttribute(HTML.Attribute.TARGET);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_RESIZABLE);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_FULLSCREEN);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_TOOLBAR);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_MENUBAR);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_STATUSBAR);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_LEFT);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_TOP);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_WIDTH);
			a.removeAttribute(HyperlinkParameter.ATTRIBUTE_HEIGHT);
		}
		StyleConstants.setUnderline(a, Page.isLinkUnderlined());
		String str = XMLCharacterEncoder.encode(url);
		if (FileUtilities.isRelative(str))
			str = page.resolvePath(str);
		if (!FileUtilities.isRemote(str))
			str = FileUtilities.useSystemFileSeparator(str);
		StyleConstants.setForeground(a, HistoryManager.sharedInstance().wasVisited(str) ? Page.getVisitedColor() : Page
				.getLinkColor());
		page.setCharacterAttributes(a, true);
		page.getSaveReminder().setChanged(true);
		if (caretPosition > 0)
			page.setCaretPosition(caretPosition);

	}

	public void setCurrentValues() {

		if (recent.isSelected()) {
			recent.doClick();
		}
		else {
			bookmark.doClick();
		}

		if (page.getSelectedText() != null) {
			int start = page.getSelectionStart();
			int end = page.getSelectionEnd();
			page.moveCaretPosition(end);
			page.requestFocus();
			page.select(start, end);
		}
		caretPosition = page.getCaretPosition();
		if (caretPosition >= 0) {
			Element elem = page.getStyledDocument().getCharacterElement(caretPosition - 1 < 0 ? 0 : caretPosition - 1);
			if (elem != null) {
				int startOffset = elem.getStartOffset();
				int endOffset = elem.getEndOffset();
				AttributeSet a = elem.getAttributes();
				String href = (String) a.getAttribute(HTML.Attribute.HREF);
				if (href != null) {
					urlField.setText(href);
					try {
						textLabel.setText(page.getText(startOffset, endOffset - startOffset));
					}
					catch (BadLocationException e) {
						textLabel.setText(null);
						urlField.setText(null);
					}
					page.requestFocus();
					page.select(startOffset, endOffset);
					urlField.setEnabled(true);
					clearButton.setEnabled(true);
					urlList.setEnabled(true);
					newWindowCheckBox.setEnabled(true);
				}
				else {
					String t = page.getSelectedText();
					boolean b = t != null;
					textLabel.setText(b ? t : "No text is selected.");
					urlField.setText(null);
					urlList.clearSelection();
					urlField.setEnabled(b);
					clearButton.setEnabled(b);
					urlList.setEnabled(b);
					newWindowCheckBox.setEnabled(b);
				}
				setNewWindowOptions(a);
			}
		}

	}

	private void setNewWindowOptions(AttributeSet a) {
		Object o = a.getAttribute(HTML.Attribute.TARGET);
		if (o instanceof String) {
			newWindowCheckBox.setSelected(((String) o).equals("_blank"));
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_RESIZABLE);
			if (o instanceof Boolean) {
				advancedDialog.param.setResizable(((Boolean) o).booleanValue());
			}
			else {
				advancedDialog.param.setResizable(true);
			}
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_TOOLBAR);
			if (o instanceof Boolean) {
				advancedDialog.param.setToolbar(((Boolean) o).booleanValue());
			}
			else {
				advancedDialog.param.setToolbar(true);
			}
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_MENUBAR);
			if (o instanceof Boolean) {
				advancedDialog.param.setMenubar(((Boolean) o).booleanValue());
			}
			else {
				advancedDialog.param.setMenubar(true);
			}
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_STATUSBAR);
			if (o instanceof Boolean) {
				advancedDialog.param.setStatusbar(((Boolean) o).booleanValue());
			}
			else {
				advancedDialog.param.setStatusbar(true);
			}
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_FULLSCREEN);
			if (o instanceof Boolean) {
				advancedDialog.param.setFullscreen(((Boolean) o).booleanValue());
			}
			else {
				advancedDialog.param.setFullscreen(false);
			}
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_LEFT);
			if (o instanceof Integer) {
				advancedDialog.param.setLeft(((Integer) o).intValue());
			}
			else {
				advancedDialog.param.setLeft(0);
			}
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_TOP);
			if (o instanceof Integer) {
				advancedDialog.param.setTop(((Integer) o).intValue());
			}
			else {
				advancedDialog.param.setTop(0);
			}
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_WIDTH);
			if (o instanceof Integer) {
				advancedDialog.param.setWidth(((Integer) o).intValue());
			}
			else {
				advancedDialog.param.setWidth(0);
			}
			o = a.getAttribute(HyperlinkParameter.ATTRIBUTE_HEIGHT);
			if (o instanceof Integer) {
				advancedDialog.param.setHeight(((Integer) o).intValue());
			}
			else {
				advancedDialog.param.setHeight(0);
			}
		}
		else {
			newWindowCheckBox.setSelected(false);
			advancedDialog.param.setResizable(true);
			advancedDialog.param.setToolbar(true);
			advancedDialog.param.setMenubar(true);
			advancedDialog.param.setFullscreen(false);
			advancedDialog.param.setLeft(0);
			advancedDialog.param.setTop(0);
			advancedDialog.param.setWidth(0);
			advancedDialog.param.setHeight(0);
		}
		advancedButton.setEnabled(newWindowCheckBox.isSelected());
	}

}