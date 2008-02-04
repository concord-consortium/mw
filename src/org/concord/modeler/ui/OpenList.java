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

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class OpenList extends JComponent implements ListSelectionListener, ActionListener {

	private JLabel header;
	private JTextField text;
	private JList list;
	private JScrollPane scroll;

	public OpenList(Object[] data, String title) {
		setLayout(null);
		header = new JLabel(title, JLabel.LEFT);
		add(header);
		text = new JTextField();
		text.addActionListener(this);
		add(text);
		list = new JList(data);
		list.setVisibleRowCount(4);
		list.addListSelectionListener(this);
		scroll = new JScrollPane(list);
		add(scroll);
	}

	public JTextField getTextField() {
		return text;
	}

	public void setSelected(String sel) {
		list.setSelectedValue(sel, true);
		text.setText(sel);
	}

	public String getSelected() {
		return text.getText();
	}

	public void setSelectedInt(int value) {
		setSelected(Integer.toString(value));
	}

	public int getSelectedInt() {
		if (getSelected() == null || getSelected().trim().equals(""))
			return -1;
		int i = -1;
		try {
			i = Integer.parseInt(getSelected());
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
			return -1;
		}
		ListModel model = list.getModel();
		try {
			int min = (Integer) model.getElementAt(0);
			int max = (Integer) model.getElementAt(model.getSize() - 1);
			if (i < min)
				i = min;
			else if (i > max)
				i = max;
		}
		catch (Exception e) {
			return i;
		}
		return i;
	}

	public void valueChanged(ListSelectionEvent e) {
		Object obj = list.getSelectedValue();
		if (obj != null)
			text.setText(obj.toString());
	}

	public void actionPerformed(ActionEvent e) {
		ListModel model = list.getModel();
		String key = text.getText().toLowerCase();
		for (int k = 0; k < model.getSize(); k++) {
			Object data = model.getElementAt(k);
			if (data.toString().toLowerCase().startsWith(key)) {
				list.setSelectedValue(data, true);
				break;
			}
		}
	}

	public void addListSelectionListener(ListSelectionListener lst) {
		list.addListSelectionListener(lst);
	}

	public Dimension getPreferredSize() {
		Insets ins = getInsets();
		Dimension d1 = header.getPreferredSize();
		Dimension d2 = text.getPreferredSize();
		Dimension d3 = scroll.getPreferredSize();
		int w = Math.max(Math.max(d1.width, d2.width), d3.width);
		int h = d1.height + d2.height + d3.height;
		return new Dimension(w + ins.left + ins.right, h + ins.top + ins.bottom);
	}

	public Dimension getMaximumSize() {
		Insets ins = getInsets();
		Dimension d1 = header.getMaximumSize();
		Dimension d2 = text.getMaximumSize();
		Dimension d3 = scroll.getMaximumSize();
		int w = Math.max(Math.max(d1.width, d2.width), d3.width);
		int h = d1.height + d2.height + d3.height;
		return new Dimension(w + ins.left + ins.right, h + ins.top + ins.bottom);
	}

	public Dimension getMinimumSize() {
		Insets ins = getInsets();
		Dimension d1 = header.getMinimumSize();
		Dimension d2 = text.getMinimumSize();
		Dimension d3 = scroll.getMinimumSize();
		int w = Math.max(Math.max(d1.width, d2.width), d3.width);
		int h = d1.height + d2.height + d3.height;
		return new Dimension(w + ins.left + ins.right, h + ins.top + ins.bottom);
	}

	public void doLayout() {
		Insets ins = getInsets();
		Dimension d = getSize();
		int x = ins.left;
		int y = ins.top;
		int w = d.width - ins.left - ins.right;
		int h = d.height - ins.top - ins.bottom;

		Dimension d1 = header.getPreferredSize();
		header.setBounds(x, y, w, d1.height);
		y += d1.height;
		Dimension d2 = text.getPreferredSize();
		text.setBounds(x, y, w, d2.height);
		y += d2.height;
		scroll.setBounds(x, y, w, h - y);
	}

	public static String titleCase(String source) {
		return Character.toUpperCase(source.charAt(0)) + source.substring(1);
	}

}
