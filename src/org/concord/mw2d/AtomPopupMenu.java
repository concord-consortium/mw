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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.IconPool;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.UserField;

class AtomPopupMenu extends JPopupMenu {

	private AtomisticView view;
	private JMenuItem miSteer, miUnsteer, miTraj, miRMean, miFMean;
	private JMenuItem miDraggable;
	private Action releaseAction;

	void setCoor(int x, int y) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.clearEditor(false);
				if (view.selectedComponent instanceof Atom) {
					Atom a = (Atom) view.selectedComponent;
					releaseAction.setEnabled(a.getRestraint() != null);
					// miSteer.setEnabled(view.model.getRecorderDisabled() && a.getUserField()==null);
					// miUnsteer.setEnabled(view.model.getRecorderDisabled() && a.getUserField()!=null);
					miSteer.setEnabled(a.getUserField() == null);
					miUnsteer.setEnabled(a.getUserField() != null);
					miTraj.setEnabled(!view.model.getRecorderDisabled());
					miRMean.setEnabled(miTraj.isEnabled());
					miFMean.setEnabled(miTraj.isEnabled());
					ModelerUtilities.selectWithoutNotifyingListeners(miDraggable, a.isDraggable());
				}
			}
		});
	}

	void setTrajSelected(boolean value) {
		if (miTraj != null)
			miTraj.setSelected(value);
	}

	void setRMeanSelected(boolean value) {
		if (miRMean != null)
			miRMean.setSelected(value);
	}

	void setFMeanSelected(boolean value) {
		if (miFMean != null)
			miFMean.setSelected(value);
	}

	private void anchorSelectedAtom(boolean b) {
		if (!(view.selectedComponent instanceof Atom))
			return;
		if (b) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					((Atom) view.selectedComponent).inputRestraint();
				}
			});
			view.model.notifyChange();
		}
		else {
			((Atom) view.selectedComponent).setRestraint(null);
			view.model.notifyChange();
		}
		view.repaint();
	}

	private void chargeSelectedAtom() {
		if (!(view.selectedComponent instanceof Atom))
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				((Atom) view.selectedComponent).inputCharge();
				view.model.checkCharges();
				view.repaint();
			}
		});
		view.model.notifyChange();
	}

	AtomPopupMenu(AtomisticView v) {

		super("Atom");
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

		s = MDView.getInternationalText("Properties");
		mi = new JMenuItem(s != null ? s : "Properties", IconPool.getIcon("properties"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DialogFactory.showDialog(view.selectedComponent);
			}
		});
		add(mi);
		addSeparator();

		Action a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				anchorSelectedAtom(true);
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.IRES_ID));
		a.putValue(Action.NAME, "Restrain");
		a.putValue(Action.MNEMONIC_KEY, UserAction.getMnemonicKey(UserAction.IRES_ID));
		a.putValue(Action.SHORT_DESCRIPTION, "Restrain this atom");
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("Restrain");
		if (s != null)
			mi.setText(s);
		add(mi);

		releaseAction = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				anchorSelectedAtom(false);
			}
		});
		releaseAction.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.DRES_ID));
		releaseAction.putValue(Action.NAME, "Release");
		releaseAction.putValue(Action.MNEMONIC_KEY, UserAction.getMnemonicKey(UserAction.DRES_ID));
		releaseAction.putValue(Action.SHORT_DESCRIPTION, "Release this atom");
		mi = new JMenuItem(releaseAction);
		s = MDView.getInternationalText("Release");
		if (s != null)
			mi.setText(s);
		add(mi);

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				chargeSelectedAtom();
			}
		});
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("charge"));
		a.putValue(Action.NAME, "Charge");
		a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_G));
		a.putValue(Action.SHORT_DESCRIPTION, "Charge this atom");
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("Charge");
		if (s != null)
			mi.setText(s);
		add(mi);

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				view.mutateSelectedAtom();
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.MUTA_ID));
		a.putValue(Action.NAME, UserAction.getName(UserAction.MUTA_ID));
		a.putValue(Action.MNEMONIC_KEY, UserAction.getMnemonicKey(UserAction.MUTA_ID));
		a.putValue(Action.SHORT_DESCRIPTION, "Change element for this atom");
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("Mutate");
		if (s != null)
			mi.setText(s);
		add(mi);

		s = MDView.getInternationalText("Measure");
		mi = new JMenuItem(s != null ? s : "Measure Distance", UserAction.getIcon(UserAction.MEAS_ID));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!(view.selectedComponent instanceof Atom))
					return;
				view.setAction(UserAction.MEAS_ID);
				Atom at = (Atom) view.selectedComponent;
				at.addMeasurement(new Point(2 * at.getRx() < getWidth() ? (int) (at.getRx() + 20)
						: (int) (at.getRx() - 20), (int) at.getRy()));
				view.repaint();
			}
		});
		add(mi);

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				view.bondBeingMade = true;
				view.setAction(UserAction.BBON_ID);
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.BBON_ID));
		a.putValue(Action.NAME, UserAction.getName(UserAction.BBON_ID));
		a.putValue(Action.MNEMONIC_KEY, UserAction.getMnemonicKey(UserAction.BBON_ID));
		a.putValue(Action.SHORT_DESCRIPTION, UserAction.getDescription(UserAction.BBON_ID));
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("BuildBond");
		if (s != null)
			mi.setText(s);
		add(mi);
		addSeparator();

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				view.setAction(UserAction.VELO_ID);
				if (view.selectedComponent instanceof Atom) {
					view.selectVelocity((Atom) view.selectedComponent);
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.VELO_ID));
		a.putValue(Action.NAME, UserAction.getName(UserAction.VELO_ID));
		a.putValue(Action.MNEMONIC_KEY, UserAction.getMnemonicKey(UserAction.VELO_ID));
		a.putValue(Action.SHORT_DESCRIPTION, "Change velocity of this atom");
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("ChangeVelocity");
		if (s != null)
			mi.setText(s);
		add(mi);

		s = MDView.getInternationalText("Steer");
		miSteer = new JMenuItem(s != null ? s : "Steer", UserAction.steerIcon);
		miSteer.setEnabled(false);
		miSteer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Atom) {
					view.setAction(UserAction.SELE_ID);
					byte interactMode = UserField.FORCE_MODE;
					if (!view.model.heatBathActivated()) {
						interactMode = view.showFrictionOptions(true);
						if (interactMode == -1)
							return;
					}
					UserField uf = new UserField(0, view.getBounds());
					uf.setMode(interactMode);
					((Atom) view.selectedComponent).setUserField(uf);
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
				if (view.selectedComponent instanceof Atom) {
					((Atom) view.selectedComponent).setUserField(null);
					if (!view.model.heatBathActivated()) {
						boolean b = false;
						int n = view.model.getNumberOfAtoms();
						for (int i = 0; i < n; i++) {
							if (view.atom[i].getUserField() != null) {
								b = true;
								break;
							}
						}
						if (!b)
							view.showFrictionOptions(false);
					}
					view.repaint();
				}
			}
		});
		add(miUnsteer);
		addSeparator();

		s = MDView.getInternationalText("ShowTrajectory");
		miTraj = new JCheckBoxMenuItem(s != null ? s : "Show Its Trajectory");
		miTraj.setIcon(IconPool.getIcon("traj"));
		miTraj.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Atom) {
					((Atom) view.selectedComponent).setShowRTraj(miTraj.isSelected());
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		add(miTraj);

		s = MDView.getInternationalText("ShowCurrentAveragePosition");
		miRMean = new JCheckBoxMenuItem(s != null ? s : "Show Its Current Average Position");
		miRMean.setIcon(UserAction.meanposIcon);
		miRMean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Atom) {
					((Atom) view.selectedComponent).setShowRMean(miRMean.isSelected());
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		add(miRMean);

		s = MDView.getInternationalText("ShowCurrentAverageForce");
		miFMean = new JCheckBoxMenuItem(s != null ? s : "Show Current Average Force On It");
		miFMean.setIcon(UserAction.meanforIcon);
		miFMean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof Atom) {
					((Atom) view.selectedComponent).setShowFMean(miFMean.isSelected());
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		add(miFMean);
		addSeparator();

		s = MDView.getInternationalText("DraggableByUserInNonEditingMode");
		miDraggable = new JCheckBoxMenuItem(s != null ? s : "Draggable by User in Non-Editing Mode");
		miDraggable.setIcon(IconPool.getIcon("user draggable"));
		miDraggable.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (view.selectedComponent instanceof Atom) {
					Atom a = (Atom) view.selectedComponent;
					a.setDraggable(e.getStateChange() == ItemEvent.SELECTED);
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		add(miDraggable);

		pack();

	}

}