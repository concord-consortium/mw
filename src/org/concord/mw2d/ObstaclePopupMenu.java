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

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;
import org.concord.mw2d.models.RectangularObstacle;
import org.concord.mw2d.models.UserField;

class ObstaclePopupMenu extends JPopupMenu {

	private AtomisticView view;
	private JMenuItem miSteer, miUnsteer, miDraggable;

	void setCoor(int x, int y) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.clearEditor(false);
				if (view.selectedComponent instanceof RectangularObstacle) {
					RectangularObstacle o = (RectangularObstacle) view.selectedComponent;
					// miSteer.setEnabled(model.getRecorderDisabled() && o.getUserField()==null);
					// miUnsteer.setEnabled(model.getRecorderDisabled() && o.getUserField()!=null);
					miSteer.setEnabled(o.getUserField() == null);
					miUnsteer.setEnabled(o.getUserField() != null);
					if (view.selectedComponent instanceof RectangularObstacle) {
						ModelerUtilities
								.setWithoutNotifyingListeners(miDraggable, view.selectedComponent.isDraggable());
					}
				}
			}
		});
	}

	ObstaclePopupMenu(AtomisticView v) {

		super("Obstacle");

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

		s = MDView.getInternationalText("Steer");
		miSteer = new JMenuItem(s != null ? s : "Steer", UserAction.steerIcon);
		miSteer.setEnabled(false);
		miSteer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof RectangularObstacle) {
					view.setAction(UserAction.SELE_ID);
					((RectangularObstacle) view.selectedComponent).setUserField(new UserField(0, view.getBounds()));
					view.repaint();
				}
			}
		});
		add(miSteer);

		s = MDView.getInternationalText("SteeringOff");
		miUnsteer = new JMenuItem(s != null ? s : "Steering Off", UserAction.unsteerIcon);
		miUnsteer.setEnabled(false);
		miUnsteer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof RectangularObstacle) {
					((RectangularObstacle) view.selectedComponent).setUserField(null);
				}
			}
		});
		add(miUnsteer);
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
				if (view.selectedComponent instanceof RectangularObstacle) {
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