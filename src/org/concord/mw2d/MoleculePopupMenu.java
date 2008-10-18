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

package org.concord.mw2d;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.IconPool;
import org.concord.mw2d.models.Molecule;

class MoleculePopupMenu extends JPopupMenu {

	AtomisticView view;
	int xpos, ypos;
	private JMenu menuTS;
	private JMenuItem miDraggable;

	void setCoor(int x, int y) {
		xpos = x;
		ypos = y;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.clearEditor(false);
				if (menuTS != null)
					menuTS.setEnabled(!view.model.getRecorderDisabled());
				if (view.selectedComponent instanceof Molecule) {
					ModelerUtilities.setWithoutNotifyingListeners(miDraggable, view.selectedComponent.isDraggable());
				}
			}
		});
	}

	MoleculePopupMenu(AtomisticView v) {

		super("Molecule");
		view = v;

		JMenuItem mi = new JMenuItem(view.getActionMap().get(AtomisticView.COPY));
		String s = MDView.getInternationalText("Copy");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(view.getActionMap().get(AtomisticView.CUT));
		s = MDView.getInternationalText("Cut");
		if (s != null)
			mi.setText(s);
		add(mi);
		addSeparator();

		Action a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				view.setAction(UserAction.ROTA_ID);
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.ROTA_ID));
		a.putValue(Action.NAME, UserAction.getName(UserAction.ROTA_ID));
		a.putValue(Action.SHORT_DESCRIPTION, "Rotate this molecule");
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("FreeRotation");
		mi.setText(s != null ? s : "Free Rotation");
		add(mi);

		s = MDView.getInternationalText("Rotate180Degrees");
		mi = new JMenuItem(s != null ? s : "<html>Rotate 180&#176;</html>");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Molecule) {
					Molecule mol = (Molecule) view.selectedComponent;
					mol.rotateBy(180);
				}
			}
		});
		add(mi);

		s = MDView.getInternationalText("Rotate90DegreesCW");
		mi = new JMenuItem(s != null ? s : "<html>Rotate 90&#176; CW</html>");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Molecule) {
					Molecule mol = (Molecule) view.selectedComponent;
					mol.rotateBy(90);
				}
			}
		});
		add(mi);

		s = MDView.getInternationalText("Rotate90DegreesCCW");
		mi = new JMenuItem(s != null ? s : "<html>Rotate 90&#176; CCW</html>");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Molecule) {
					Molecule mol = (Molecule) view.selectedComponent;
					mol.rotateBy(-90);
				}
			}
		});
		add(mi);

		s = MDView.getInternationalText("FlipHorizontal");
		mi = new JMenuItem(s != null ? s : "Flip Horizontal");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Molecule) {
					Molecule mol = (Molecule) view.selectedComponent;
					mol.flipHorizontal();
				}
			}
		});
		add(mi);

		s = MDView.getInternationalText("FlipVertical");
		mi = new JMenuItem(s != null ? s : "Flip Vertical");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Molecule) {
					Molecule mol = (Molecule) view.selectedComponent;
					mol.flipVertical();
				}
			}
		});
		add(mi);

		addSeparator();

		s = MDView.getInternationalText("Properties");
		mi = new JMenuItem(s != null ? s : "Properties", IconPool.getIcon("properties"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DialogFactory.showDialog(view.selectedComponent);
			}
		});
		add(mi);

		s = MDView.getInternationalText("DraggableByUserInNonEditingMode");
		miDraggable = new JCheckBoxMenuItem("Draggable by User in Non-Editing Mode");
		miDraggable.setIcon(IconPool.getIcon("user draggable"));
		miDraggable.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (view.selectedComponent instanceof Molecule) {
					view.selectedComponent.setDraggable(e.getStateChange() == ItemEvent.SELECTED);
					view.repaint();
					view.getModel().notifyChange();
				}
			}
		});
		add(miDraggable);

		pack();

	}

}