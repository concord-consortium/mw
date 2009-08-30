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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.PastableTextField;

public class SearchTextField extends JPanel implements Embeddable {

	private final static int SPACING = 8;
	private final static int MARGIN = 8;
	private final static String[] CATEGORY = new String[] { "Title", "Author", "Teacher", "School" };
	private final static String[] BASE_URL = new String[] {
			Modeler.getContextRoot() + "modelspace?client=mw&action=search",
			Modeler.getContextRoot() + "myreportspace?client=mw&action=search",
			"http://www.concord.org/cgi-bin/htsearch?restrict=mw2.concord.org%2Fpublic/tutorial",
			"http://www.concord.org/cgi-bin/htsearch?restrict=mw2.concord.org%2Fpublic/student",
			"http://www.concord.org/cgi-bin/htsearch?restrict=mw2.concord.org%2Fpublic" };

	Page page;
	private int index;
	private String uid;
	private boolean marked;
	private int databaseType = 0;
	private static int defaultColumn = 40;
	private static Color defaultBackground, defaultForeground;
	private JTextField textField;
	private JComboBox categoryComboBox;
	private JButton searchButton;
	private JPopupMenu popupMenu;
	private static SearchTextFieldMaker maker;
	private MouseAdapter mouseAdapter;

	private ActionListener searchAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (textField.getText() == null || textField.getText().trim().equals("")) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(SearchTextField.this),
						"Please input keywords.", "Keywords", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			if (textField.getText().indexOf("=") != -1 || textField.getText().indexOf("_") != -1
					|| textField.getText().indexOf("?") != -1 || textField.getText().indexOf("%") != -1
					|| textField.getText().indexOf("&") != -1) {
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(SearchTextField.this),
						"Characters '=', '_', '?', '%', and '&' should not be included in the keywords.", "Keywords",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			String key = page.getAddress() + "#" + ModelerUtilities.getSortableString(index, 3) + "%"
					+ SearchTextField.this.getClass().getName();
			UserData.sharedInstance().putData(
					key,
					new QuestionAndAnswer(categoryComboBox != null ? categoryComboBox.getSelectedItem().toString()
							: "content", textField.getText().trim()));
			try {
				if (databaseType >= 2) {
					page.openHyperlink(BASE_URL[databaseType] + "&words="
							+ URLEncoder.encode(textField.getText().trim(), "UTF-8"));
				}
				else {
					page.openHyperlink(BASE_URL[databaseType]
							+ "&category="
							+ URLEncoder.encode(categoryComboBox != null ? categoryComboBox.getSelectedItem()
									.toString() : "Title", "US-ASCII") + "&keyword="
							+ URLEncoder.encode(textField.getText().trim(), "UTF-8"));
				}
			}
			catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			}
		}
	};

	public SearchTextField() {

		super(new BorderLayout(SPACING, SPACING));

		setOpaque(false);

		textField = new PastableTextField(defaultColumn);
		textField.addActionListener(searchAction);
		add(textField, BorderLayout.CENTER);

		if (defaultBackground == null)
			defaultBackground = textField.getBackground();
		if (defaultForeground == null)
			defaultForeground = textField.getForeground();

		String s = Modeler.getInternationalText("Search");
		searchButton = new JButton(s != null ? s : "Search");
		searchButton.addActionListener(searchAction);
		add(searchButton, BorderLayout.EAST);

		mouseAdapter = new PopupMouseListener(this);
		addMouseListener(mouseAdapter);

	}

	public SearchTextField(SearchTextField t, Page parent) {
		this();
		setPage(parent);
		setDatabaseType(t.databaseType);
		setColumns(t.getColumns());
		setPreferredSize(t.getPreferredSize());
		setChangable(page.isEditable());
	}

	public void destroy() {
		page = null;
		if (maker != null)
			maker.setObject(null);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	public void createPopupMenu() {
		popupMenu = new JPopupMenu();
		popupMenu.setInvoker(this);
		JMenuItem mi = new JMenuItem("Customize This Search Text Field...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (maker == null) {
					maker = new SearchTextFieldMaker(SearchTextField.this);
				}
				else {
					maker.setObject(SearchTextField.this);
				}
				maker.invoke(page);
			}
		});
		popupMenu.add(mi);
		mi = new JMenuItem("Remove This Search Text Field");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.removeComponent(SearchTextField.this);
			}
		});
		popupMenu.add(mi);
		mi = new JMenuItem("Copy This Search Text Field");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.copyComponent(SearchTextField.this);
			}
		});
		popupMenu.add(mi);
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

	public void setPreferredSize(Dimension dim) {
		super.setPreferredSize(dim);
		super.setMaximumSize(dim);
		super.setMinimumSize(dim);
	}

	private int getDesirableHeight() {
		return textField.getFontMetrics(textField.getFont()).getHeight() + MARGIN;
	}

	public void setColumns(int i) {
		textField.setColumns(i);
		setPreferredSize(new Dimension(i * getColumnWidth(), getDesirableHeight()));
	}

	protected int getColumnWidth() {
		return textField.getFontMetrics(textField.getFont()).charWidth('m');
	}

	public int getColumns() {
		return textField.getColumns();
	}

	public void setDatabaseType(int i) {
		databaseType = i;
	}

	public int getDatabaseType() {
		return databaseType;
	}

	public void setMarked(boolean b) {
		marked = b;
		if (page == null)
			return;
		textField.setBackground(b ? page.getSelectionColor() : defaultBackground);
		textField.setForeground(b ? page.getSelectedTextColor() : defaultForeground);
	}

	public boolean isMarked() {
		return marked;
	}

	public void setChangable(boolean b) {
		if (b) {
			if (!isChangable()) {
				addMouseListener(mouseAdapter);
				searchButton.addMouseListener(mouseAdapter);
			}
		}
		else {
			if (isChangable()) {
				removeMouseListener(mouseAdapter);
				searchButton.removeMouseListener(mouseAdapter);
			}
		}
	}

	public boolean isChangable() {
		MouseListener[] ml = getMouseListeners();
		for (int i = 0; i < ml.length; i++) {
			if (ml[i] == mouseAdapter)
				return true;
		}
		return false;
	}

	public void setPage(Page p) {
		page = p;
		setPreferredSize(new Dimension(textField.getColumns() * getColumnWidth(), getDesirableHeight()));
	}

	public Page getPage() {
		return page;
	}

	public String getText() {
		return textField.getText();
	}

	public void setText(String s) {
		textField.setText(s);
	}

	public void setCategory(String s) {
		if (categoryComboBox == null)
			return;
		categoryComboBox.setSelectedItem(s);
	}

	public static SearchTextField create(Page page) {
		if (page == null)
			return null;
		SearchTextField field = new SearchTextField();
		if (maker == null) {
			maker = new SearchTextFieldMaker(field);
		}
		else {
			maker.setObject(field);
		}
		maker.invoke(page);
		if (maker.cancel)
			return null;
		return field;
	}

	public void addCategoryComboBox(boolean b) {
		if (b) {
			if (categoryComboBox == null)
				categoryComboBox = new JComboBox(CATEGORY);
			add(categoryComboBox, BorderLayout.WEST);
		}
		else {
			if (categoryComboBox != null)
				remove(categoryComboBox);
		}
	}

	private boolean hasCategoryComboBox() {
		int n = getComponentCount();
		for (int i = 0; i < n; i++) {
			if (getComponent(i) == categoryComboBox)
				return true;
		}
		return false;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("<class>" + getClass().getName() + "</class>\n");
		if (hasCategoryComboBox())
			sb.append("<hasmenu>true</hasmenu>");
		if (getDatabaseType() != 0)
			sb.append("<type>" + getDatabaseType() + "</type>\n");
		if (getColumns() != defaultColumn)
			sb.append("<column>" + getColumns() + "</column>");
		return sb.toString();
	}

}