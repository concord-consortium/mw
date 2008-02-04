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
class DropToolPopupMenu extends JPopupMenu {

	private JMenuItem xAddAtomMenuItem;
	private JMenuItem yAddAtomMenuItem;
	private JMenuItem zAddAtomMenuItem;
	private JMenuItem xAddMoleMenuItem;
	private JMenuItem yAddMoleMenuItem;
	private JMenuItem zAddMoleMenuItem;
	private JMenuItem xAddFileMenuItem;
	private JMenuItem yAddFileMenuItem;
	private JMenuItem zAddFileMenuItem;

	DropToolPopupMenu(final MolecularView view) {

		super();

		ButtonGroup bg = new ButtonGroup();

		xAddAtomMenuItem = new JRadioButtonMenuItem("Drop an Atom on Plane Perpendicular to x-Axis", new ImageIcon(
				getClass().getResource("resources/AddAtomOnXPlane.gif")));
		xAddAtomMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.XADD_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.XADD_ID);
			}
		});
		add(xAddAtomMenuItem);
		bg.add(xAddAtomMenuItem);

		yAddAtomMenuItem = new JRadioButtonMenuItem("Drop an Atom on Plane Perpendicular to y-Axis", new ImageIcon(
				getClass().getResource("resources/AddAtomOnYPlane.gif")));
		yAddAtomMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.YADD_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.YADD_ID);
			}
		});
		add(yAddAtomMenuItem);
		bg.add(yAddAtomMenuItem);

		zAddAtomMenuItem = new JRadioButtonMenuItem("Drop an Atom on Plane Perpendicular to z-Axis", new ImageIcon(
				getClass().getResource("resources/AddAtomOnZPlane.gif")));
		zAddAtomMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.ZADD_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.ZADD_ID);
			}
		});
		add(zAddAtomMenuItem);
		bg.add(zAddAtomMenuItem);

		xAddMoleMenuItem = new JRadioButtonMenuItem("Drop a Molecule on Plane Perpendicular to x-Axis", new ImageIcon(
				getClass().getResource("resources/AddMolOnXPlane.gif")));
		xAddMoleMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.XMOL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.XMOL_ID);
			}
		});
		add(xAddMoleMenuItem);
		bg.add(xAddMoleMenuItem);

		yAddMoleMenuItem = new JRadioButtonMenuItem("Drop a Molecule on Plane Perpendicular to y-Axis", new ImageIcon(
				getClass().getResource("resources/AddMolOnYPlane.gif")));
		yAddMoleMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.YMOL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.YMOL_ID);
			}
		});
		add(yAddMoleMenuItem);
		bg.add(yAddMoleMenuItem);

		zAddMoleMenuItem = new JRadioButtonMenuItem("Drop a Molecule on Plane Perpendicular to z-Axis", new ImageIcon(
				getClass().getResource("resources/AddMolOnZPlane.gif")));
		zAddMoleMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.ZMOL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.ZMOL_ID);
			}
		});
		add(zAddMoleMenuItem);
		bg.add(zAddMoleMenuItem);

		xAddFileMenuItem = new JRadioButtonMenuItem(
				"Load a Model from File and Drop It on Plane Perpendicular to x-Axis", new ImageIcon(getClass()
						.getResource("resources/AddModelOnXPlane.gif")));
		xAddFileMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.XFIL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.XFIL_ID);
			}
		});
		add(xAddFileMenuItem);
		bg.add(xAddFileMenuItem);

		yAddFileMenuItem = new JRadioButtonMenuItem(
				"Load a Model from File and Drop It on Plane Perpendicular to y-Axis", new ImageIcon(getClass()
						.getResource("resources/AddModelOnYPlane.gif")));
		yAddFileMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.YFIL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.YFIL_ID);
			}
		});
		add(yAddFileMenuItem);
		bg.add(yAddFileMenuItem);

		zAddFileMenuItem = new JRadioButtonMenuItem(
				"Load a Model from File and Drop It on Plane Perpendicular to z-Axis", new ImageIcon(getClass()
						.getResource("resources/AddModelOnZPlane.gif")));
		zAddFileMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.ZFIL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.ZFIL_ID);
			}
		});
		add(zAddFileMenuItem);
		bg.add(zAddFileMenuItem);

		pack();

	}

	void setStateID(byte id) {
		switch (id) {
		case UserAction.XADD_ID:
			xAddAtomMenuItem.setSelected(true);
			break;
		case UserAction.YADD_ID:
			yAddAtomMenuItem.setSelected(true);
			break;
		case UserAction.ZADD_ID:
			zAddAtomMenuItem.setSelected(true);
			break;
		case UserAction.XMOL_ID:
			xAddMoleMenuItem.setSelected(true);
			break;
		case UserAction.YMOL_ID:
			yAddMoleMenuItem.setSelected(true);
			break;
		case UserAction.ZMOL_ID:
			zAddMoleMenuItem.setSelected(true);
			break;
		case UserAction.XFIL_ID:
			xAddFileMenuItem.setSelected(true);
			break;
		case UserAction.YFIL_ID:
			yAddFileMenuItem.setSelected(true);
			break;
		case UserAction.ZFIL_ID:
			zAddFileMenuItem.setSelected(true);
			break;
		}
	}

}