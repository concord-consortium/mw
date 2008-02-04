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

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.IconPool;
import org.concord.mw2d.models.GayBerneParticle;
import org.concord.mw2d.models.UserField;

class GbPopupMenu extends JPopupMenu {

	private MesoView view;
	private JMenuItem miRelease, miSteer, miUnsteer, miTraj, miRMean, miFMean;

	void setCoor(int x, int y) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.clearEditor(false);
				if (view.selectedComponent instanceof GayBerneParticle) {
					GayBerneParticle p = (GayBerneParticle) view.selectedComponent;
					miRelease.setEnabled(p.getRestraint() != null);
					miTraj.setEnabled(!view.model.getRecorderDisabled());
					miRMean.setEnabled(miTraj.isEnabled());
					miFMean.setEnabled(miTraj.isEnabled());
					miSteer.setEnabled(p.getUserField() == null);
					miUnsteer.setEnabled(p.getUserField() != null);
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

	GbPopupMenu(MesoView v) {

		super("Gay-Berne");
		view = v;

		JMenuItem mi = new JMenuItem(view.getActionMap().get(MDView.COPY));
		String s = MDView.getInternationalText("Copy");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(view.getActionMap().get(MDView.CUT));
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
				if (view.selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) view.selectedComponent).inputRestraint();
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.IRES_ID));
		a.putValue(Action.NAME, "Restrain");
		a.putValue(Action.SHORT_DESCRIPTION, "Restrain this particle");
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("Restrain");
		if (s != null)
			mi.setText(s);
		add(mi);

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof GayBerneParticle) {
					GayBerneParticle p = (GayBerneParticle) view.selectedComponent;
					p.setRestraint(null);
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.DRES_ID));
		a.putValue(Action.NAME, "Release");
		a.putValue(Action.SHORT_DESCRIPTION, "Release restraints on this particle");
		miRelease = new JMenuItem(a);
		s = MDView.getInternationalText("Release");
		if (s != null)
			miRelease.setText(s);
		add(miRelease);

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) view.selectedComponent).inputCharge();
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, IconPool.getIcon("charge"));
		a.putValue(Action.NAME, "Charge");
		a.putValue(Action.SHORT_DESCRIPTION, "Charge this Gay-Berne particle");
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("Charge");
		if (s != null)
			mi.setText(s);
		add(mi);

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) view.selectedComponent).inputDipole();
					view.repaint();
				}
			}
		});
		a.putValue(Action.SMALL_ICON, UserAction.polarIcon);
		a.putValue(Action.NAME, "Polarize");
		a.putValue(Action.SHORT_DESCRIPTION, "Polarize this Gay-Berne particle");
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("Polarize");
		if (s != null)
			mi.setText(s);
		add(mi);

		s = MDView.getInternationalText("Measure");
		mi = new JMenuItem(s != null ? s : "Measure Distance", UserAction.getIcon(UserAction.MEAS_ID));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!(view.selectedComponent instanceof GayBerneParticle))
					return;
				view.setAction(UserAction.MEAS_ID);
				GayBerneParticle p = (GayBerneParticle) view.selectedComponent;
				p.addMeasurement(new Point(
						2 * p.getRx() < getWidth() ? (int) (p.getRx() + 20) : (int) (p.getRx() - 20), (int) p.getRy()));
				view.repaint();
			}
		});
		add(mi);
		addSeparator();

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				view.setAction(UserAction.ROTA_ID);
				view.repaint();
			}
		});
		a.putValue(Action.NAME, "Rotate");
		a.putValue(Action.SHORT_DESCRIPTION, "Rotate this particle");
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.ROTA_ID));
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("Rotate");
		if (s != null)
			mi.setText(s);
		add(mi);

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof GayBerneParticle) {
					view.selectVelocity((GayBerneParticle) view.selectedComponent);
					view.setAction(UserAction.VELO_ID);
					view.repaint();
				}
			}
		});
		a.putValue(Action.NAME, "Change COM Velocity");
		a.putValue(Action.SHORT_DESCRIPTION, "Change the velocity of the center of mass of this particle");
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.VELO_ID));
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("ChangeCOMVelocity");
		if (s != null)
			mi.setText(s);
		add(mi);

		a = new DefaultModelAction(view.model, new Executable() {
			public void execute() {
				if (view.selectedComponent instanceof GayBerneParticle) {
					view.selectOmega((GayBerneParticle) view.selectedComponent);
					view.setAction(UserAction.OMEG_ID);
					view.repaint();
				}
			}
		});
		a.putValue(Action.NAME, "Change Angular Velocity");
		a.putValue(Action.SHORT_DESCRIPTION, "Change the angular velocity of this particle");
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(UserAction.OMEG_ID));
		mi = new JMenuItem(a);
		s = MDView.getInternationalText("ChangeAngularVelocity");
		if (s != null)
			mi.setText(s);
		add(mi);

		s = MDView.getInternationalText("Steer");
		miSteer = new JMenuItem(s != null ? s : "Steer", UserAction.steerIcon);
		miSteer.setEnabled(false);
		miSteer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof GayBerneParticle) {
					boolean b = false;
					for (int i = 0; i < view.model.getNumberOfParticles(); i++) {
						if (view.gb[i].getUserField() != null) {
							b = true;
							break;
						}
					}
					if (!b)
						view.showFrictionOptions(true);
					((GayBerneParticle) view.selectedComponent).setUserField(new UserField(0, view.getBounds()));
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
				if (view.selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) view.selectedComponent).setUserField(null);
					boolean b = false;
					for (int i = 0; i < view.model.getNumberOfParticles(); i++) {
						if (view.gb[i].getUserField() != null) {
							b = true;
							break;
						}
					}
					if (!b)
						view.showFrictionOptions(false);
					view.repaint();
				}
			}
		});
		add(miUnsteer);
		addSeparator();

		s = MDView.getInternationalText("ShowTrajectory");
		miTraj = new JCheckBoxMenuItem(s != null ? s : "Show Trajectory Of Its Center");
		miTraj.setIcon(IconPool.getIcon("traj"));
		miTraj.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) view.selectedComponent).setShowRTraj(miTraj.isSelected());
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		add(miTraj);

		s = MDView.getInternationalText("ShowCurrentAveragePosition");
		miRMean = new JCheckBoxMenuItem(s != null ? s : "Show Current Average Position Of Its Center");
		miRMean.setIcon(UserAction.meanposIcon);
		miRMean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) view.selectedComponent).setShowRMean(miRMean.isSelected());
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		add(miRMean);

		s = MDView.getInternationalText("ShowCurrentAverageForce");
		miFMean = new JCheckBoxMenuItem(s != null ? s : "Show Current Average Force On Its Center");
		miFMean.setIcon(UserAction.meanforIcon);
		miFMean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof GayBerneParticle) {
					((GayBerneParticle) view.selectedComponent).setShowFMean(miFMean.isSelected());
					view.repaint();
					view.model.notifyChange();
				}
			}
		});
		add(miFMean);

		pack();

	}

}