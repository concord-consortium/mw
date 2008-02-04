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
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.concord.modeler.ui.IconPool;

/**
 * @author Charles Xie
 * 
 */
class DeleteToolPopupMenu extends JPopupMenu {

	private JMenuItem rectMenuItem;
	private JMenuItem ovalMenuItem;
	private JMenuItem fixRectMenuItem;
	private JMenuItem fixOvalMenuItem;
	private JMenuItem unfixRectMenuItem;
	private JMenuItem unfixOvalMenuItem;

	DeleteToolPopupMenu(final MolecularView view) {

		super();

		ButtonGroup bg = new ButtonGroup();

		rectMenuItem = new JRadioButtonMenuItem("Remove Atoms In Rectangle", IconPool.getIcon("remove rect"));
		rectMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.DELR_ID));
					b.getAction().putValue(Action.SHORT_DESCRIPTION, rectMenuItem.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.DELR_ID);
			}
		});
		add(rectMenuItem);
		bg.add(rectMenuItem);

		ovalMenuItem = new JRadioButtonMenuItem("Remove Atoms In Ellipse", new ImageIcon(getClass().getResource(
				"resources/removeoval.gif")));
		ovalMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.DELC_ID));
					b.getAction().putValue(Action.SHORT_DESCRIPTION, ovalMenuItem.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.DELC_ID);
			}
		});
		add(ovalMenuItem);
		bg.add(ovalMenuItem);

		fixRectMenuItem = new JRadioButtonMenuItem("Fix Atoms In Rectangle", new ImageIcon(getClass().getResource(
				"resources/fixrect.gif")));
		fixRectMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.FIXR_ID));
					b.getAction().putValue(Action.SHORT_DESCRIPTION, fixRectMenuItem.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.FIXR_ID);
			}
		});
		add(fixRectMenuItem);
		bg.add(fixRectMenuItem);

		fixOvalMenuItem = new JRadioButtonMenuItem("Fix Atoms In Ellipse", new ImageIcon(getClass().getResource(
				"resources/fixoval.gif")));
		fixOvalMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.FIXC_ID));
					b.getAction().putValue(Action.SHORT_DESCRIPTION, fixOvalMenuItem.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.FIXC_ID);
			}
		});
		add(fixOvalMenuItem);
		bg.add(fixOvalMenuItem);

		unfixRectMenuItem = new JRadioButtonMenuItem("Set Atoms In Rectangle Unfixed", new ImageIcon(getClass()
				.getResource("resources/unfixrect.gif")));
		unfixRectMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.XIFR_ID));
					b.getAction().putValue(Action.SHORT_DESCRIPTION, unfixRectMenuItem.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.XIFR_ID);
			}
		});
		add(unfixRectMenuItem);
		bg.add(unfixRectMenuItem);

		unfixOvalMenuItem = new JRadioButtonMenuItem("Set Atoms In Ellipse Unfixed", new ImageIcon(getClass()
				.getResource("resources/unfixoval.gif")));
		unfixOvalMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.XIFC_ID));
					b.getAction().putValue(Action.SHORT_DESCRIPTION, unfixOvalMenuItem.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.XIFC_ID);
			}
		});
		add(unfixOvalMenuItem);
		bg.add(unfixOvalMenuItem);

		pack();

	}

	void setStateID(byte id) {
		switch (id) {
		case UserAction.DELR_ID:
			rectMenuItem.setSelected(true);
			break;
		case UserAction.DELC_ID:
			ovalMenuItem.setSelected(true);
			break;
		case UserAction.FIXR_ID:
			fixRectMenuItem.setSelected(true);
			break;
		case UserAction.FIXC_ID:
			fixOvalMenuItem.setSelected(true);
			break;
		case UserAction.XIFR_ID:
			unfixRectMenuItem.setSelected(true);
			break;
		case UserAction.XIFC_ID:
			unfixOvalMenuItem.setSelected(true);
			break;
		}
	}

}