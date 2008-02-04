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
class MiscToolPopupMenu extends JPopupMenu {

	private JMenuItem sboxMenuItem;
	private JMenuItem veloMenuItem;
	private JMenuItem extrudeMenuItem;

	MiscToolPopupMenu(final MolecularView view) {

		super();

		ButtonGroup bg = new ButtonGroup();

		extrudeMenuItem = new JRadioButtonMenuItem("Extrude Obstacle", new ImageIcon(getClass().getResource(
				"resources/Extrude.gif")));
		extrudeMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.EXOB_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.EXOB_ID);
			}
		});
		add(extrudeMenuItem);
		bg.add(extrudeMenuItem);

		sboxMenuItem = new JRadioButtonMenuItem("Change Simulation Box", new ImageIcon(getClass().getResource(
				"resources/ChangeBox.gif")));
		sboxMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.SBOX_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.SBOX_ID);
			}
		});
		add(sboxMenuItem);
		bg.add(sboxMenuItem);

		veloMenuItem = new JRadioButtonMenuItem("Change Velocity Vector", new ImageIcon(getClass().getResource(
				"resources/Vector.gif")));
		veloMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.VVEL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.VVEL_ID);
			}
		});
		add(veloMenuItem);
		bg.add(veloMenuItem);

		pack();

	}

	void setStateID(byte id) {
		switch (id) {
		case UserAction.SBOX_ID:
			sboxMenuItem.setSelected(true);
			break;
		case UserAction.VVEL_ID:
			veloMenuItem.setSelected(true);
			break;
		case UserAction.EXOB_ID:
			extrudeMenuItem.setSelected(true);
			break;
		}
	}

}