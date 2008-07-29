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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.ui.IconPool;
import org.concord.mw3d.models.Atom;

class AtomPopupMenu extends JPopupMenu {

	private MolecularView view;
	private JMenuItem miTraj;
	private JMenuItem miVelo;
	private JMenuItem miInfo;
	private JMenuItem miCamera;

	public void show(Component invoker, int x, int y) {
		if (view.selectedComponent instanceof Atom) {
			Atom a = (Atom) view.selectedComponent;
			String s = MolecularContainer.getInternationalText("AtomHtml");
			miInfo.setText("<html><i>" + (s != null ? s : "Atom") + "</i> #" + a.getIndex() + ": " + a.getSymbol()
					+ "</html>");
		}
		super.show(invoker, x, y);
	}

	void setTrajSelected(boolean value) {
		miTraj.setSelected(value);
	}

	void setVeloSelected(boolean value) {
		miVelo.setSelected(value);
	}

	void setCameraAttached(boolean b) {
		miCamera.setEnabled(view.getViewer().getNavigationMode());
		miCamera.setSelected(b);
	}

	AtomPopupMenu(MolecularView v) {

		super("Atom");
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
		addSeparator();

		s = MolecularContainer.getInternationalText("AttachCamera");
		miCamera = new JCheckBoxMenuItem(s != null ? s : "Attach Camera");
		miCamera.setIcon(new ImageIcon(getClass().getResource("resources/CameraOnAtom.gif")));
		miCamera.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Atom) {
					Atom a = (Atom) view.selectedComponent;
					int k = view.getModel().getAtomIndex(a);
					view.setCameraAtom(k);
					view.model.notifyChange();
				}
			}
		});
		add(miCamera);

		s = MolecularContainer.getInternationalText("ShowVelocity");
		miVelo = new JCheckBoxMenuItem(s != null ? s : "Show Velocity");
		miVelo.setIcon(IconPool.getIcon("velocity"));
		miVelo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Atom) {
					Atom a = (Atom) view.selectedComponent;
					int k = view.getModel().getAtomIndex(a);
					view.showVelocity(k, miVelo.isSelected());
					view.model.notifyChange();
				}
			}
		});
		add(miVelo);

		s = MolecularContainer.getInternationalText("ShowTrajectory");
		miTraj = new JCheckBoxMenuItem(s != null ? s : "Show Trajectory");
		miTraj.setIcon(IconPool.getIcon("traj"));
		miTraj.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Atom) {
					Atom a = (Atom) view.selectedComponent;
					int k = view.getModel().getAtomIndex(a);
					view.showTrajectory(k, miTraj.isSelected());
					view.model.notifyChange();
				}
			}
		});
		add(miTraj);

		pack();

	}

}
