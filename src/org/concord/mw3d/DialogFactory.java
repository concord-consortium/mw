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

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.concord.mw3d.models.ABond;
import org.concord.mw3d.models.Atom;
import org.concord.mw3d.models.Molecule;
import org.concord.mw3d.models.Obstacle;
import org.concord.mw3d.models.RBond;
import org.concord.mw3d.models.TBond;

public final class DialogFactory {

	public static void showDialog(final Object obj) {
		if (obj == null)
			return;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (obj instanceof Atom) {
					createDialog((Atom) obj).setVisible(true);
				}
				else if (obj instanceof RBond) {
					createDialog((RBond) obj).setVisible(true);
				}
				else if (obj instanceof ABond) {
					createDialog((ABond) obj).setVisible(true);
				}
				else if (obj instanceof TBond) {
					createDialog((TBond) obj).setVisible(true);
				}
				else if (obj instanceof Molecule) {
					createDialog((Molecule) obj).setVisible(true);
				}
				else if (obj instanceof Obstacle) {
					createDialog((Obstacle) obj).setVisible(true);
				}
			}
		});
	}

	/** create a dialog box for an atom */
	private static JDialog createDialog(final Atom atom) {
		String s = MolecularContainer.getInternationalText("AtomProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(atom.getModel().getView()), s != null ? s
				: "Atom Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final AtomPropertiesPanel p = new AtomPropertiesPanel(atom);
		p.setDialog(dialog);
		dialog.setContentPane(p);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(atom.getModel().getView());
		return dialog;
	}

	/** create a dialog box for a radial bond */
	private static JDialog createDialog(final RBond rbond) {
		String s = MolecularContainer.getInternationalText("RadialBondProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(rbond.getAtom1().getModel().getView()),
				s != null ? s : "Radial Bond Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final RBondPropertiesPanel p = new RBondPropertiesPanel(rbond);
		p.setDialog(dialog);
		dialog.setContentPane(p);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(rbond.getAtom1().getModel().getView());
		return dialog;
	}

	/** create a dialog box for an angular bond */
	private static JDialog createDialog(final ABond abond) {
		String s = MolecularContainer.getInternationalText("AngularBondProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(abond.getAtom1().getModel().getView()),
				s != null ? s : "Angular Bond Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final ABondPropertiesPanel p = new ABondPropertiesPanel(abond);
		p.setDialog(dialog);
		dialog.setContentPane(p);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(abond.getAtom1().getModel().getView());
		return dialog;
	}

	/** create a dialog box for a torsional bond */
	private static JDialog createDialog(final TBond tbond) {
		String s = MolecularContainer.getInternationalText("TorsionalBondProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(tbond.getAtom1().getModel().getView()),
				s != null ? s : "Torsional Bond Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final TBondPropertiesPanel p = new TBondPropertiesPanel(tbond);
		p.setDialog(dialog);
		dialog.setContentPane(p);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(tbond.getAtom1().getModel().getView());
		return dialog;
	}

	/** create a dialog box for a molecule */
	private static JDialog createDialog(final Molecule molecule) {
		String s = MolecularContainer.getInternationalText("MoleculeProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(molecule.getAtom(0).getModel().getView()),
				s != null ? s : "Molecule Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final MoleculePropertiesPanel p = new MoleculePropertiesPanel(molecule);
		p.setDialog(dialog);
		dialog.setContentPane(p);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(molecule.getAtom(0).getModel().getView());
		return dialog;
	}

	/** create a dialog box for an obstacle */
	private static JDialog createDialog(final Obstacle obs) {
		String s = MolecularContainer.getInternationalText("ObstacleProperties");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(obs.getModel().getView()), s != null ? s
				: "Obstacle Properties", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final ObstaclePropertiesPanel p = new ObstaclePropertiesPanel(obs);
		p.setDialog(dialog);
		dialog.setContentPane(p);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				p.destroy();
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				p.windowActivated();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(obs.getModel().getView());
		return dialog;
	}

}