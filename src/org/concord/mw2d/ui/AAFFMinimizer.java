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

package org.concord.mw2d.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

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

import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.models.Minimizer;
import org.concord.mw2d.models.MolecularModel;

class AAFFMinimizer extends JDialog {

	private final static DecimalFormat FORMAT = new DecimalFormat();
	private MinimizeJob job;

	static {
		FORMAT.applyPattern("###.####");
	}

	public AAFFMinimizer(MolecularModel m) {

		super(JOptionPane.getFrameForComponent(m.getView()), "Energy Minimizer", false);
		String s = MDContainer.getInternationalText("EnergyMinimizer");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		job = new MinimizeJob(m);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEtchedBorder());

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		s = MDContainer.getInternationalText("ChooseMethod");
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s
				: "Choose a method"));

		ButtonGroup bg = new ButtonGroup();

		s = MDContainer.getInternationalText("SteepestDescents");
		JRadioButton rb = new JRadioButton(s != null ? s : "Steepest Descents");
		rb.setSelected(true);
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				job.method = MinimizeJob.SD;
			}
		});
		bg.add(rb);
		p.add(rb);

		s = MDContainer.getInternationalText("ConjugateGradients");
		rb = new JRadioButton(s != null ? s : "Conjugate Gradients");
		rb.setEnabled(false);
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				job.method = MinimizeJob.CG;
			}
		});
		bg.add(rb);
		p.add(rb);

		panel.add(p, BorderLayout.WEST);

		p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		s = MDContainer.getInternationalText("Parameter");
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Parameters"));

		Box box = new Box(BoxLayout.X_AXIS);
		s = MDContainer.getInternationalText("NumberOfSteps");
		box.add(new JLabel((s != null ? s : "Number of Steps") + ": ", SwingConstants.LEFT));
		JComboBox cb = new JComboBox(new Object[] { new Short((short) 100), new Short((short) 500),
				new Short((short) 1000), new Short((short) 5000), new Short((short) 10000) });
		cb.setPreferredSize(new Dimension(80, 20));
		cb.setSelectedIndex(0);
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox c = (JComboBox) e.getSource();
				job.nStep = ((Short) c.getSelectedItem()).shortValue();
			}
		});
		box.add(cb);
		p.add(box);

		box = new Box(BoxLayout.X_AXIS);
		s = MDContainer.getInternationalText("Steplength");
		box.add(new JLabel((s != null ? s : "Steplength") + ": ", SwingConstants.LEFT));
		cb = new JComboBox(new Object[] { new Float(1.0f), new Float(1.5f), new Float(2.0f), new Float(2.5f),
				new Float(3.0f), new Float(3.5f), new Float(4.0f), new Float(4.5f), new Float(5.0f) });
		cb.setPreferredSize(new Dimension(80, 20));
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox c = (JComboBox) e.getSource();
				job.delta = ((Float) c.getSelectedItem()).floatValue();
			}
		});
		box.add(cb);
		p.add(box);

		panel.add(p, BorderLayout.CENTER);

		getContentPane().add(panel, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		s = MDContainer.getInternationalText("RunButton");
		JButton b = new JButton(s != null ? s : "Run");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				job.stop = false;
				Thread t = new Thread(job);
				t.setName("Energy Minimizer");
				t.setPriority(Thread.NORM_PRIORITY);
				t.start();
			}
		});
		p.add(b);

		s = MDContainer.getInternationalText("Dismiss");
		b = new JButton(s != null ? s : "Dismiss");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				job.stop = true;
				dispose();
			}
		});
		p.add(b);

		getContentPane().add(p, BorderLayout.SOUTH);

	}

	class MinimizeJob implements Runnable {

		private final static byte SD = 1, CG = 2;
		private Minimizer m;
		private MolecularModel model;
		private short nStep = 100;
		private double delta = 1.0;
		private byte method = SD;
		private boolean stop;
		private int i;
		private double pot;

		private Runnable refresh = new Runnable() {
			public void run() {
				((AtomisticView) model.getView()).refreshJmol();
				model.getView().getGraphics().drawString(i + " steps, V = " + FORMAT.format(pot) + " eV.", 5, 20);
			}
		};

		MinimizeJob(MolecularModel model) {
			this.model = model;
			m = new Minimizer(model);
		}

		public void run() {
			for (i = 0; i < nStep; i++) {
				if (stop)
					break;
				switch (method) {
				case SD:
					pot = m.sd(delta);
					break;
				case CG:
					pot = m.cg(delta);
					break;
				}
				if (i % 20 == 0) {
					EventQueue.invokeLater(refresh);
					model.getView().repaint();
				}
			}
			model.notifyChange();
		}

	}

}