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
package org.concord.mw3d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.concord.mw3d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class ElementSelectionPanel extends JPanel {

	private JComboBox comboBox;
	private static Object[] elements;
	private Popup popup;

	ElementSelectionPanel(final MolecularView view) {

		super(new BorderLayout());

		comboBox = new JComboBox();
		comboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					view.setCurrentElementToAdd((String) comboBox.getSelectedItem());
				}
			}
		});
		fillComboBox(view.model);
		comboBox.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				hidePopup();
			}
		});
		add(comboBox, BorderLayout.CENTER);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		p.setBackground(SystemColor.info);
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray), BorderFactory
				.createEmptyBorder(2, 2, 2, 2)));
		add(p, BorderLayout.NORTH);

		final JButton button = new JButton();
		button.setIcon(new Icon() {
			public int getIconHeight() {
				return button.getHeight();
			}

			public int getIconWidth() {
				return button.getWidth();
			}

			public void paintIcon(Component c, Graphics g, int w, int h) {
				g.setColor(Color.red);
				int iw = getIconWidth();
				int ih = getIconHeight();
				g.draw3DRect(1, 1, iw - 2, ih - 2, true);
				g.drawLine(4, 4, iw - 4, ih - 4);
				g.drawLine(4, ih - 4, iw - 4, 4);
			}
		});
		button.setBorder(BorderFactory.createEmptyBorder());
		button.setPreferredSize(new Dimension(12, 12));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hidePopup();
			}
		});
		p.add(button);

		String s = MolecularContainer.getInternationalText("SelectElement");
		p.add(new JLabel(s != null ? s : "Select element:"));

	}

	private void fillComboBox(MolecularModel model) {
		if (elements == null)
			elements = model.getSupportedElements().toArray();
		for (Object o : elements)
			comboBox.addItem(o);
		comboBox.setSelectedItem("X1");
	}

	void showPopup(Component owner) {
		Point p = owner.getLocationOnScreen();
		popup = PopupFactory.getSharedInstance().getPopup(owner, this, p.x + 5, p.y + 5);
		popup.show();
		comboBox.requestFocusInWindow();
	}

	void hidePopup() {
		if (popup != null) {
			popup.hide();
		}
	}

}