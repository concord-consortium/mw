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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.ui.IconPool;
import org.concord.mw3d.models.TBond;

class TBondPopupMenu extends JPopupMenu {

	private MolecularView view;
	private JMenuItem miInfo;

	public void show(Component invoker, int x, int y) {
		if (view.selectedComponent instanceof TBond) {
			TBond t = (TBond) view.selectedComponent;
			miInfo.setText("<html><i>Torsional Bond</i> #" + view.getModel().getTBonds().indexOf(t) + ": ( "
					+ t.getAtom1() + " - " + t.getAtom2() + " - " + t.getAtom3() + " - " + t.getAtom4() + " ) "
					+ MolecularView.FORMAT.format(t.getAngle() * 180 / Math.PI) + "&#176;</html>");
		}
		super.show(invoker, x, y);
	}

	TBondPopupMenu(MolecularView v) {

		super("Torsional Bond");
		view = v;

		miInfo = new JMenuItem("Info", new ImageIcon(getClass().getResource("resources/info.gif")));
		miInfo.setBackground(new Color(0xFFFFD070));
		miInfo.setEnabled(false);
		add(miInfo);
		addSeparator();

		JMenuItem mi = new JMenuItem(view.getActionMap().get("cut"));
		String s = MolecularContainer.getInternationalText("Cut");
		if (s != null)
			mi.setText(s);
		add(mi);
		addSeparator();

		s = MolecularContainer.getInternationalText("Properties");
		mi = new JMenuItem(s != null ? s : "Properties", IconPool.getIcon("properties"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DialogFactory.showDialog(view.selectedComponent);
			}
		});
		add(mi);

		pack();

	}

}