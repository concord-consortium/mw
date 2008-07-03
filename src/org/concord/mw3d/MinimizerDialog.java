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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.BitSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.concord.mw3d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class MinimizerDialog extends JDialog {

	private final static DecimalFormat FORMAT = new DecimalFormat();
	private short nstep = 100;
	private float stepLength = 0.1f;
	private MolecularModel model;

	static {
		FORMAT.applyPattern("###.####");
	}

	public MinimizerDialog(MolecularModel m) {

		super(JOptionPane.getFrameForComponent(m.getView()), "Energy Minimizer", true);
		String s = MolecularContainer.getInternationalText("EnergyMinimizer");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		model = m;

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEtchedBorder());

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		s = MolecularContainer.getInternationalText("ChooseMethod");
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s
				: "Choose a method"));

		ButtonGroup bg = new ButtonGroup();

		s = MolecularContainer.getInternationalText("SteepestDescents");
		JRadioButton rb = new JRadioButton(s != null ? s : "Steepest Descents");
		rb.setSelected(true);
		bg.add(rb);
		p.add(rb);

		s = MolecularContainer.getInternationalText("ConjugateGradients");
		rb = new JRadioButton(s != null ? s : "Conjugate Gradients");
		rb.setEnabled(false);
		bg.add(rb);
		p.add(rb);

		panel.add(p, BorderLayout.WEST);

		p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		s = MolecularContainer.getInternationalText("Parameter");
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Parameters"));

		Box box = new Box(BoxLayout.X_AXIS);
		s = MolecularContainer.getInternationalText("NumberOfSteps");
		box.add(new JLabel((s != null ? s : "Number of Steps") + ": ", SwingConstants.LEFT));
		JComboBox cb = new JComboBox(new Object[] { new Short((short) 100), new Short((short) 500),
				new Short((short) 1000), new Short((short) 5000), new Short((short) 10000) });
		cb.setPreferredSize(new Dimension(80, 20));
		cb.setSelectedIndex(0);
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox c = (JComboBox) e.getSource();
				nstep = ((Short) c.getSelectedItem()).shortValue();
			}
		});
		box.add(cb);
		p.add(box);

		box = new Box(BoxLayout.X_AXIS);
		s = MolecularContainer.getInternationalText("Steplength");
		box.add(new JLabel((s != null ? s : "Steplength") + ": ", SwingConstants.LEFT));
		cb = new JComboBox(new Object[] { new Float(0.1f), new Float(0.2f), new Float(0.3f), new Float(0.4f),
				new Float(0.5f) });
		cb.setPreferredSize(new Dimension(80, 20));
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox c = (JComboBox) e.getSource();
				stepLength = ((Float) c.getSelectedItem()).floatValue();
			}
		});
		box.add(cb);
		p.add(box);

		panel.add(p, BorderLayout.CENTER);

		getContentPane().add(panel, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		s = MolecularContainer.getInternationalText("OK");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(b);

		getContentPane().add(p, BorderLayout.SOUTH);

		pack();

	}

	void runMinimizer() {
		Thread t = new Thread(new Runnable() {
			public void run() {
				model.minimize(nstep, stepLength);
				model.notifyChange();
			}
		});
		t.setName("Energy Minimizer For 3D");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	void runMinimizer(final BitSet bs) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				model.minimize(nstep, stepLength, bs);
				model.notifyChange();
			}
		});
		t.setName("Energy Minimizer For Selected Particles (3D)");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

}