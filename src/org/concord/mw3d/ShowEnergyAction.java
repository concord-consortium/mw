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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.AbstractLoadable;
import org.concord.modeler.process.Loadable;
import org.concord.modeler.ui.IconPool;
import org.concord.mw3d.models.MolecularModel;

class ShowEnergyAction extends AbstractAction {

	private MolecularContainer container;
	private MolecularModel model;
	private EnergyVisualizer vizEnergy;

	/* the subtask of drawing the potential, kinetic and total energy curves */
	private Loadable updateCurves;

	private void initLoadable() {
		if (updateCurves != null)
			return;
		updateCurves = new AbstractLoadable(2000) {
			public void execute() {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						if (vizEnergy.isShowing())
							update();
					}
				});
			}

			public int getPriority() {
				return Thread.NORM_PRIORITY - 1;
			}

			public String getName() {
				return "Drawing energy time series";
			}

			public int getLifetime() {
				return ETERNAL;
			}

			public String getDescription() {
				return "This task draws the kinetic, potential and total energy curves of the model\nwhen the curve boxes appear on the screen.";
			}
		};
	}

	ShowEnergyAction(MolecularContainer container) {
		super();
		this.container = container;
		this.model = container.model;
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
		putValue(NAME, "Show Energy");
		putValue(SMALL_ICON, IconPool.getIcon("e curve"));
		putValue(SHORT_DESCRIPTION, "Show kinetic, potential and total energies");
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		showEnergyTimeSeries();
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

	void update() {
		vizEnergy.setTime(model.getModelTime());
		vizEnergy.setText(0, model.getKine().getCurrentValue());
		vizEnergy.setText(1, model.getPote().getCurrentValue());
		vizEnergy.setText(2, model.getTote().getCurrentValue());
		vizEnergy.refresh();
		vizEnergy.repaint();
	}

	private void showEnergyTimeSeries() {
		if (model.getKine().isEmpty() || model.getKine().getPointer() <= 0 || model.getPote().isEmpty()
				|| model.getPote().getPointer() <= 0 || model.getTote().isEmpty() || model.getTote().getPointer() <= 0) {
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(model.getView()),
					"No data have been collected for the energy time series.", "No data error",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		initLoadable();
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(model.getView()));
		d.setTitle("Energy Time Series");
		d.setModal(false);
		d.setSize(400, 300);
		if (vizEnergy == null)
			vizEnergy = new EnergyVisualizer(container);
		d.getContentPane().add(vizEnergy);
		vizEnergy.setTime(model.getModelTime());
		vizEnergy.setText(0, model.getKine().getCurrentValue());
		vizEnergy.setText(1, model.getPote().getCurrentValue());
		vizEnergy.setText(2, model.getTote().getCurrentValue());
		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				d.dispose();
				updateCurves.setCompleted(true);
			}

			public void windowOpened(WindowEvent e) {
				updateCurves.setCompleted(false);
				if (!model.getJob().contains(updateCurves))
					model.getJob().add(updateCurves);
			}
		});
		d.pack();
		d.setLocationRelativeTo(model.getView());
		d.setVisible(true);
		vizEnergy.refresh();
	}

}