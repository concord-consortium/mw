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

import org.concord.modeler.ui.IconPool;

/**
 * @author Charles Xie
 * 
 */
class BondToolPopupMenu extends JPopupMenu {

	private JMenuItem rbondMenuItem;
	private JMenuItem abondMenuItem;
	private JMenuItem tbondMenuItem;
	private JMenuItem poschMenuItem;
	private JMenuItem negchMenuItem;

	BondToolPopupMenu(final MolecularView view) {

		super();

		ButtonGroup bg = new ButtonGroup();

		rbondMenuItem = new JRadioButtonMenuItem("Build Radial Bond Between Atoms", IconPool.getIcon("radial bond"));
		rbondMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.RBND_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.RBND_ID);
			}
		});
		add(rbondMenuItem);
		bg.add(rbondMenuItem);

		abondMenuItem = new JRadioButtonMenuItem("Build Angular Bond Among Atoms", IconPool.getIcon("angular bond"));
		abondMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.ABND_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.ABND_ID);
			}
		});
		add(abondMenuItem);
		bg.add(abondMenuItem);

		tbondMenuItem = new JRadioButtonMenuItem("Build Torsional Bond Among Atoms", new ImageIcon(
				MolecularContainer.class.getResource("resources/torsionbond.gif")));
		tbondMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.TBND_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.TBND_ID);
			}
		});
		add(tbondMenuItem);
		bg.add(tbondMenuItem);

		poschMenuItem = new JRadioButtonMenuItem("Add a Positive Charge to an Atom", new ImageIcon(
				MolecularContainer.class.getResource("resources/ChargePositive.gif")));
		poschMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.PCHG_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.PCHG_ID);
			}
		});
		add(poschMenuItem);
		bg.add(poschMenuItem);

		negchMenuItem = new JRadioButtonMenuItem("Add a Negative Charge to an Atom", new ImageIcon(
				MolecularContainer.class.getResource("resources/ChargeNegative.gif")));
		negchMenuItem.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Component c = getInvoker();
				JMenuItem m = (JMenuItem) e.getSource();
				if (c instanceof AbstractButton) {
					AbstractButton b = (AbstractButton) c;
					b.setIcon(m.getIcon());
					b.getAction().putValue("action_id", new Byte(UserAction.NCHG_ID));
					b.setToolTipText(m.getText());
					if (!b.isSelected())
						b.setSelected(true);
				}
				view.setActionID(UserAction.NCHG_ID);
			}
		});
		add(negchMenuItem);
		bg.add(negchMenuItem);

		pack();

	}

	void setStateID(byte id) {
		switch (id) {
		case UserAction.RBND_ID:
			rbondMenuItem.setSelected(true);
			break;
		case UserAction.ABND_ID:
			abondMenuItem.setSelected(true);
			break;
		case UserAction.TBND_ID:
			tbondMenuItem.setSelected(true);
			break;
		case UserAction.PCHG_ID:
			poschMenuItem.setSelected(true);
			break;
		case UserAction.NCHG_ID:
			negchMenuItem.setSelected(true);
			break;
		}
	}

}