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
class DropObstaclePopupMenu extends JPopupMenu {

	private JMenuItem xrecMenuItem;
	private JMenuItem yrecMenuItem;
	private JMenuItem zrecMenuItem;
	private JMenuItem xovlMenuItem;
	private JMenuItem yovlMenuItem;
	private JMenuItem zovlMenuItem;

	DropObstaclePopupMenu(final MolecularView view) {

		super();

		ButtonGroup bg = new ButtonGroup();

		xrecMenuItem = new JRadioButtonMenuItem("Drop a Cuboid Obstacle on Plane Perpendicular to x-Axis",
				new ImageIcon(getClass().getResource("resources/AddRectangleOnXPlane.gif")));
		xrecMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.XREC_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.XREC_ID);
			}
		});
		add(xrecMenuItem);
		bg.add(xrecMenuItem);

		yrecMenuItem = new JRadioButtonMenuItem("Drop a Cuboid Obstacle on Plane Perpendicular to y-Axis",
				new ImageIcon(getClass().getResource("resources/AddRectangleOnYPlane.gif")));
		yrecMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.YREC_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.YREC_ID);
			}
		});
		add(yrecMenuItem);
		bg.add(yrecMenuItem);

		zrecMenuItem = new JRadioButtonMenuItem("Drop a Cuboid Obstacle on Plane Perpendicular to z-Axis",
				new ImageIcon(getClass().getResource("resources/AddRectangleOnZPlane.gif")));
		zrecMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.ZREC_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.ZREC_ID);
			}
		});
		add(zrecMenuItem);
		bg.add(zrecMenuItem);

		xovlMenuItem = new JRadioButtonMenuItem("Drop a Cylindrical Obstacle on Plane Perpendicular to x-Axis",
				new ImageIcon(getClass().getResource("resources/AddOvalOnXPlane.gif")));
		xovlMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.XOVL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.XOVL_ID);
			}
		});
		add(xovlMenuItem);
		bg.add(xovlMenuItem);

		yovlMenuItem = new JRadioButtonMenuItem("Drop a Cylindrical Obstacle on Plane Perpendicular to y-Axis",
				new ImageIcon(getClass().getResource("resources/AddOvalOnYPlane.gif")));
		yovlMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.YOVL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.YOVL_ID);
			}
		});
		add(yovlMenuItem);
		bg.add(yovlMenuItem);

		zovlMenuItem = new JRadioButtonMenuItem("Drop a Cylindrical Obstacle on Plane Perpendicular to z-Axis",
				new ImageIcon(getClass().getResource("resources/AddOvalOnZPlane.gif")));
		zovlMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.ZOVL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.ZOVL_ID);
			}
		});
		add(zovlMenuItem);
		bg.add(zovlMenuItem);

		pack();

	}

	void setStateID(byte id) {
		switch (id) {
		case UserAction.XREC_ID:
			xrecMenuItem.setSelected(true);
			break;
		case UserAction.YREC_ID:
			yrecMenuItem.setSelected(true);
			break;
		case UserAction.ZREC_ID:
			zrecMenuItem.setSelected(true);
			break;
		case UserAction.XOVL_ID:
			xovlMenuItem.setSelected(true);
			break;
		case UserAction.YOVL_ID:
			yovlMenuItem.setSelected(true);
			break;
		case UserAction.ZOVL_ID:
			zovlMenuItem.setSelected(true);
			break;
		}
	}

}