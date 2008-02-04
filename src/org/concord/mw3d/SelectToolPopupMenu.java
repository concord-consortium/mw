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

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * @author Charles Xie
 * 
 */
class SelectToolPopupMenu extends JPopupMenu {

	private JMenuItem selectRectMenuItem;
	private JMenuItem selectOvalMenuItem;
	private JMenuItem selectSingleMenuItem;
	private JMenuItem translucentMenuItem;
	private JMenuItem opaqueMenuItem;
	private JMenuItem hidenMenuItem;

	SelectToolPopupMenu(final MolecularView view) {

		super();

		ButtonGroup bg = new ButtonGroup();

		selectRectMenuItem = new JRadioButtonMenuItem("Select Atoms In Rectangle", new ImageIcon(getClass()
				.getResource("resources/selectrect.gif")));
		selectRectMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.putClientProperty("action_id", new Byte(UserAction.SLRT_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.SLRT_ID);
			}
		});
		add(selectRectMenuItem);
		bg.add(selectRectMenuItem);

		selectOvalMenuItem = new JRadioButtonMenuItem("Select Atoms In Ellipse", new ImageIcon(getClass().getResource(
				"resources/selectoval.gif")));
		selectOvalMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.putClientProperty("action_id", new Byte(UserAction.SLOV_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.SLOV_ID);
			}
		});
		add(selectOvalMenuItem);
		bg.add(selectOvalMenuItem);

		selectSingleMenuItem = new JRadioButtonMenuItem("Select Individual Atoms", new ImageIcon(getClass()
				.getResource("resources/SelectSingle.gif")));
		selectSingleMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.putClientProperty("action_id", new Byte(UserAction.SLAT_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.SLAT_ID);
			}
		});
		add(selectSingleMenuItem);
		bg.add(selectSingleMenuItem);

		hidenMenuItem = new JRadioButtonMenuItem("Hide Atoms In Rectangle", new ImageIcon(getClass().getResource(
				"resources/Hiden.gif")));
		hidenMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.putClientProperty("action_id", new Byte(UserAction.HIDE_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.HIDE_ID);
			}
		});
		add(hidenMenuItem);
		bg.add(hidenMenuItem);

		translucentMenuItem = new JRadioButtonMenuItem("Set Atoms In Rectangle Translucent", new ImageIcon(getClass()
				.getResource("resources/Translucent.gif")));
		translucentMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.putClientProperty("action_id", new Byte(UserAction.TSLC_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.TSLC_ID);
			}
		});
		add(translucentMenuItem);
		bg.add(translucentMenuItem);

		opaqueMenuItem = new JRadioButtonMenuItem("Set Atoms In Rectangle Opaque", new ImageIcon(getClass()
				.getResource("resources/Opaque.gif")));
		opaqueMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.putClientProperty("action_id", new Byte(UserAction.CLST_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.CLST_ID);
			}
		});
		add(opaqueMenuItem);
		bg.add(opaqueMenuItem);

		pack();

	}

	void setStateID(byte id) {
		switch (id) {
		case UserAction.SLRT_ID:
			selectRectMenuItem.setSelected(true);
			break;
		case UserAction.SLOV_ID:
			selectOvalMenuItem.setSelected(true);
			break;
		case UserAction.TSLC_ID:
			translucentMenuItem.setSelected(true);
			break;
		case UserAction.CLST_ID:
			opaqueMenuItem.setSelected(true);
		}
	}

}