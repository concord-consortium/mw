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
class RotateToolPopupMenu extends JPopupMenu {

	private JMenuItem rotateMenuItem;
	private JMenuItem translateMenuItem;
	private JMenuItem duplicateMenuItem;

	RotateToolPopupMenu(final MolecularView view) {

		super();

		ButtonGroup bg = new ButtonGroup();

		rotateMenuItem = new JRadioButtonMenuItem("Rotate a Molecule or a Group of Atoms", new ImageIcon(getClass()
				.getResource("resources/RotateAtoms.gif")));
		rotateMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.ROTA_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.ROTA_ID);
			}
		});
		add(rotateMenuItem);
		bg.add(rotateMenuItem);

		translateMenuItem = new JRadioButtonMenuItem("Translate a Molecule or a Group of Atoms", new ImageIcon(
				getClass().getResource("resources/TranslateAtoms.gif")));
		translateMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.TRAN_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.TRAN_ID);
			}
		});
		add(translateMenuItem);
		bg.add(translateMenuItem);

		duplicateMenuItem = new JRadioButtonMenuItem("Duplicate a Molecule", new ImageIcon(getClass().getResource(
				"resources/DuplicateMolecule.gif")));
		duplicateMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.DUPL_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.DUPL_ID);
			}
		});
		add(duplicateMenuItem);
		bg.add(duplicateMenuItem);

		pack();

	}

	void setStateID(byte id) {
		switch (id) {
		case UserAction.ROTA_ID:
			rotateMenuItem.setSelected(true);
			break;
		case UserAction.TRAN_ID:
			translateMenuItem.setSelected(true);
			break;
		case UserAction.DUPL_ID:
			duplicateMenuItem.setSelected(true);
			break;
		}
	}

}