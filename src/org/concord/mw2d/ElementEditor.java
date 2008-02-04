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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.Affinity;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;

class ElementEditor {

	private final static double ZERO = 0.000001;
	private final static double MAX_SIG = 50.0;
	private final static double MIN_SIG = 5.0;
	private final static double MAX_EPS = 1.0;
	private final static double MIN_EPS = 0.0;
	private Element Nt, Pl, Ws, Ck;
	private Affinity affinity;

	static String aaString = "Nt-Nt";
	static String bbString = "Pl-Pl";
	static String ccString = "Ws-Ws";
	static String ddString = "Ck-Ck";
	static String abString = "Nt-Pl";
	static String acString = "Nt-Ws";
	static String adString = "Nt-Ck";
	static String bcString = "Pl-Ws";
	static String bdString = "Pl-Ck";
	static String cdString = "Ws-Ck";

	private LennardJones lj;
	private JRadioButton aaButton, bbButton, ccButton, ddButton;
	private RealNumberTextField epsAA, epsBB, epsCC, epsDD;
	private SigmaField sigAA, sigBB, sigCC, sigDD;
	private int selectedInteraction = 1;
	private MolecularModel model;
	private TabbedPane tabbedPane;
	private CrossPanel abPanel, acPanel, adPanel, bcPanel, bdPanel, cdPanel;
	private JButton closeButton;
	private JButton cutoffButton;
	private JPanel buttonPanel;

	public double getSigma(Element e1, Element e2) {
		if (e1 == e2)
			return e1.getSigma();
		if (e1.getName().equalsIgnoreCase(e2.getName()))
			return e1.getSigma();
		if (getLBMixing(e1, e2))
			return Math.sqrt(e1.getSigma() * e2.getSigma());
		return affinity.getSigma(e1, e2);
	}

	public double getEpsilon(Element e1, Element e2) {
		if (e1 == e2)
			return e1.getEpsilon();
		if (e1.getName().equalsIgnoreCase(e2.getName()))
			return e1.getSigma();
		if (getLBMixing(e1, e2))
			return 0.5 * (e1.getEpsilon() + e2.getEpsilon());
		return affinity.getEpsilon(e1, e2);
	}

	public void setPar(String s1, String s2, double eps, double sig) {
		if (s1.equalsIgnoreCase("Nt") && s2.equalsIgnoreCase("Nt")) {
			epsAA.setValue(eps);
			sigAA.setValue(sig);
			Nt.setEpsilon(eps);
			Nt.setSigma(sig);
		}
		else if (s1.equalsIgnoreCase("Pl") && s2.equalsIgnoreCase("Pl")) {
			epsBB.setValue(eps);
			sigBB.setValue(sig);
			Pl.setEpsilon(eps);
			Pl.setSigma(sig);
		}
		else if (s1.equalsIgnoreCase("Ws") && s2.equalsIgnoreCase("Ws")) {
			epsCC.setValue(eps);
			sigCC.setValue(sig);
			Ws.setEpsilon(eps);
			Ws.setSigma(sig);
		}
		else if (s1.equalsIgnoreCase("Ck") && s2.equalsIgnoreCase("Ck")) {
			epsDD.setValue(eps);
			sigDD.setValue(sig);
			Ck.setEpsilon(eps);
			Ck.setSigma(sig);
		}
		else if (s1.equalsIgnoreCase("Nt") && s2.equalsIgnoreCase("Pl")) {
			if (abPanel != null) {
				abPanel.epsilon.setValue(eps);
				abPanel.sigma.setValue(sig);
				affinity.setEpsilon(Nt, Pl, eps);
				affinity.setSigma(Nt, Pl, sig);
			}
		}
		else if (s1.equalsIgnoreCase("Nt") && s2.equalsIgnoreCase("Ws")) {
			if (acPanel != null) {
				acPanel.epsilon.setValue(eps);
				acPanel.sigma.setValue(sig);
				affinity.setEpsilon(Nt, Ws, eps);
				affinity.setSigma(Nt, Ws, sig);
			}
		}
		else if (s1.equalsIgnoreCase("Nt") && s2.equalsIgnoreCase("Ck")) {
			if (adPanel != null) {
				adPanel.epsilon.setValue(eps);
				adPanel.sigma.setValue(sig);
				affinity.setEpsilon(Nt, Ck, eps);
				affinity.setSigma(Nt, Ck, sig);
			}
		}
		else if (s1.equalsIgnoreCase("Pl") && s2.equalsIgnoreCase("Ws")) {
			if (bcPanel != null) {
				bcPanel.epsilon.setValue(eps);
				bcPanel.sigma.setValue(sig);
				affinity.setEpsilon(Pl, Ws, eps);
				affinity.setSigma(Pl, Ws, sig);
			}
		}
		else if (s1.equalsIgnoreCase("Pl") && s2.equalsIgnoreCase("Ck")) {
			if (bdPanel != null) {
				bdPanel.epsilon.setValue(eps);
				bdPanel.sigma.setValue(sig);
				affinity.setEpsilon(Pl, Ck, eps);
				affinity.setSigma(Pl, Ck, sig);
			}
		}
		else if (s1.equalsIgnoreCase("Ws") && s2.equalsIgnoreCase("Ck")) {
			if (cdPanel != null) {
				cdPanel.epsilon.setValue(eps);
				cdPanel.sigma.setValue(sig);
				affinity.setEpsilon(Ws, Ck, eps);
				affinity.setSigma(Ws, Ck, sig);
			}
		}
		model.getView().repaint();
	}

	public boolean getRepulsive(Element e1, Element e2) {
		return affinity.isRepulsive(e1, e2);
	}

	public boolean getRepulsive(String s1, String s2) {
		return affinity.isRepulsive(model.getElement(s1), model.getElement(s2));
	}

	public boolean getLBMixing(Element e1, Element e2) {
		return affinity.isLBMixed(e1, e2);
	}

	public boolean getLBMixing(String s1, String s2) {
		return affinity.isLBMixed(model.getElement(s1), model.getElement(s2));
	}

	public Element getElement(String s) {
		if (s.toLowerCase().equalsIgnoreCase("nt"))
			return Nt;
		if (s.toLowerCase().equalsIgnoreCase("pl"))
			return Pl;
		if (s.toLowerCase().equalsIgnoreCase("ws"))
			return Ws;
		if (s.toLowerCase().equalsIgnoreCase("ck"))
			return Ck;
		return null;
	}

	public Element getElement(byte i) {
		switch (i) {
		case Element.ID_NT:
			return Nt;
		case Element.ID_PL:
			return Pl;
		case Element.ID_WS:
			return Ws;
		case Element.ID_CK:
			return Ck;
		}
		return null;
	}

	public void setFocusedElement(String s) {
		tabbedPane.setSelectedIndex(0);
		if (s.equalsIgnoreCase("Nt"))
			aaButton.doClick();
		else if (s.equalsIgnoreCase("Pl"))
			bbButton.doClick();
		else if (s.equalsIgnoreCase("Ws"))
			ccButton.doClick();
		else if (s.equalsIgnoreCase("Ck"))
			ddButton.doClick();
	}

	public void setFocusedElement(int i) {
		tabbedPane.setSelectedIndex(0);
		if (i == Element.ID_NT)
			aaButton.doClick();
		else if (i == Element.ID_PL)
			bbButton.doClick();
		else if (i == Element.ID_WS)
			ccButton.doClick();
		else if (i == Element.ID_CK)
			ddButton.doClick();
	}

	public int getSelectedInteraction() {
		return selectedInteraction;
	}

	public void setSelectedInteraction(int i) {
		selectedInteraction = i;
		sigAA.setEnabled(selectedInteraction == 1);
		epsAA.setEnabled(selectedInteraction == 1);
		sigBB.setEnabled(selectedInteraction == 2);
		epsBB.setEnabled(selectedInteraction == 2);
		sigCC.setEnabled(selectedInteraction == 3);
		epsCC.setEnabled(selectedInteraction == 3);
		sigDD.setEnabled(selectedInteraction == 4);
		epsDD.setEnabled(selectedInteraction == 4);
	}

	public int getSelectedTabIndex() {
		return tabbedPane.getSelectedIndex();
	}

	public ElementEditor(final MolecularModel atomicModel) {

		setModel(atomicModel);

		lj = new LennardJones(this);

		aaButton = new JRadioButton(aaString);
		aaButton.setFont(ViewAttribute.SMALL_FONT);
		aaButton.setSelected(true);
		aaButton.setMnemonic(KeyEvent.VK_N);
		aaButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSelectedInteraction(1);
				lj.setLJFunction(Nt.getSigma(), Nt.getEpsilon());
				lj.repaint();
			}
		});
		bbButton = new JRadioButton(bbString);
		bbButton.setFont(ViewAttribute.SMALL_FONT);
		bbButton.setMnemonic(KeyEvent.VK_P);
		bbButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSelectedInteraction(2);
				lj.setLJFunction(Pl.getSigma(), Pl.getEpsilon());
				lj.repaint();
			}
		});
		ccButton = new JRadioButton(ccString);
		ccButton.setFont(ViewAttribute.SMALL_FONT);
		ccButton.setMnemonic(KeyEvent.VK_W);
		ccButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSelectedInteraction(3);
				lj.setLJFunction(Ws.getSigma(), Ws.getEpsilon());
				lj.repaint();
			}
		});
		ddButton = new JRadioButton(ddString);
		ddButton.setFont(ViewAttribute.SMALL_FONT);
		ddButton.setMnemonic(KeyEvent.VK_C);
		ddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSelectedInteraction(4);
				lj.setLJFunction(Ck.getSigma(), Ck.getEpsilon());
				lj.repaint();
			}
		});
		ButtonGroup group2 = new ButtonGroup();
		group2.add(aaButton);
		group2.add(bbButton);
		group2.add(ccButton);
		group2.add(ddButton);

		JLabel sigAALabel = new JLabel("<html><font size=3><i>&#963;</i></font> (&#197;)</html>", JLabel.CENTER);
		JLabel epsAALabel = new JLabel("<html><font size=3><i>&#949;</i></font> (eV)</html>", JLabel.CENTER);
		JLabel sigBBLabel = new JLabel("<html><font size=3><i>&#963;</i></font> (&#197;)</html>", JLabel.CENTER);
		JLabel epsBBLabel = new JLabel("<html><font size=3><i>&#949;</i></font> (eV)</html>", JLabel.CENTER);
		JLabel sigCCLabel = new JLabel("<html><font size=3><i>&#963;</i></font> (&#197;)</html>", JLabel.CENTER);
		JLabel epsCCLabel = new JLabel("<html><font size=3><i>&#949;</i></font> (eV)</html>", JLabel.CENTER);
		JLabel sigDDLabel = new JLabel("<html><font size=3><i>&#963;</i></font> (&#197;)</html>", JLabel.CENTER);
		JLabel epsDDLabel = new JLabel("<html><font size=3><i>&#949;</i></font> (eV)</html>", JLabel.CENTER);
		sigAALabel.setToolTipText("Collision diameter " + aaString);
		sigBBLabel.setToolTipText("Collision diameter " + bbString);
		sigCCLabel.setToolTipText("Collision diameter " + ccString);
		sigDDLabel.setToolTipText("Collision diameter " + ddString);

		sigAA = new SigmaField(Nt.getSigma(), MIN_SIG, MAX_SIG);
		sigAA.setFont(ViewAttribute.SMALL_FONT);
		sigAA.setMaximumFractionDigits(3);
		sigAA.setMaximumIntegerDigits(2);
		sigAA.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = sigAA.getValue();
				if (Math.abs(Nt.getSigma() - d) < ZERO)
					return;
				if (d > MAX_SIG)
					d = MAX_SIG;
				else if (d < MIN_SIG)
					d = MIN_SIG;
				lj.setLJFunction(d, epsAA.getValue());
				lj.repaint();
				lj.updateTextFields();
				model.notifyChange();
			}
		});

		epsAA = new RealNumberTextField(Nt.getEpsilon(), MIN_EPS, MAX_EPS);
		epsAA.setFont(ViewAttribute.SMALL_FONT);
		epsAA.setMaximumFractionDigits(4);
		epsAA.setMaximumIntegerDigits(2);
		epsAA.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = epsAA.getValue();
				if (Math.abs(Nt.getEpsilon() - d) < ZERO)
					return;
				if (d > MAX_EPS)
					d = MAX_EPS;
				else if (d < MIN_EPS)
					d = MIN_EPS;
				lj.setLJFunction(sigAA.getValue(), d);
				lj.repaint();
				lj.updateTextFields();
				model.notifyChange();
			}
		});

		sigBB = new SigmaField(Pl.getSigma(), MIN_SIG, MAX_SIG);
		sigBB.setFont(ViewAttribute.SMALL_FONT);
		sigBB.setMaximumFractionDigits(3);
		sigBB.setMaximumIntegerDigits(2);
		sigBB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = sigBB.getValue();
				if (Math.abs(Pl.getSigma() - d) < ZERO)
					return;
				if (d > MAX_SIG)
					d = MAX_SIG;
				else if (d < MIN_SIG)
					d = MIN_SIG;
				lj.setLJFunction(d, epsBB.getValue());
				lj.repaint();
				lj.updateTextFields();
				model.notifyChange();
			}
		});

		epsBB = new RealNumberTextField(Pl.getEpsilon(), MIN_EPS, MAX_EPS);
		epsBB.setFont(ViewAttribute.SMALL_FONT);
		epsBB.setMaximumFractionDigits(4);
		epsBB.setMaximumIntegerDigits(2);
		epsBB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = epsBB.getValue();
				if (Math.abs(Pl.getEpsilon() - d) < ZERO)
					return;
				if (d > MAX_EPS)
					d = MAX_EPS;
				else if (d < MIN_EPS)
					d = MIN_EPS;
				lj.setLJFunction(sigBB.getValue(), d);
				lj.repaint();
				lj.updateTextFields();
				model.notifyChange();
			}
		});

		sigCC = new SigmaField(Ws.getSigma(), MIN_SIG, MAX_SIG);
		sigCC.setFont(ViewAttribute.SMALL_FONT);
		sigCC.setMaximumFractionDigits(3);
		sigCC.setMaximumIntegerDigits(2);
		sigCC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = sigCC.getValue();
				if (Math.abs(Ws.getSigma() - d) < ZERO)
					return;
				if (d > MAX_SIG)
					d = MAX_SIG;
				else if (d < MIN_SIG)
					d = MIN_SIG;
				lj.setLJFunction(d, epsCC.getValue());
				lj.repaint();
				lj.updateTextFields();
				model.notifyChange();
			}
		});

		epsCC = new RealNumberTextField(Ws.getEpsilon(), MIN_EPS, MAX_EPS);
		epsCC.setFont(ViewAttribute.SMALL_FONT);
		epsCC.setMaximumFractionDigits(4);
		epsCC.setMaximumIntegerDigits(2);
		epsCC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = epsCC.getValue();
				if (Math.abs(Ws.getEpsilon() - d) < ZERO)
					return;
				if (d > MAX_EPS)
					d = MAX_EPS;
				else if (d < MIN_EPS)
					d = MIN_EPS;
				lj.setLJFunction(sigCC.getValue(), d);
				lj.repaint();
				lj.updateTextFields();
				model.notifyChange();
			}
		});

		sigDD = new SigmaField(Ck.getSigma(), MIN_SIG, MAX_SIG);
		sigDD.setFont(ViewAttribute.SMALL_FONT);
		sigDD.setMaximumFractionDigits(3);
		sigDD.setMaximumIntegerDigits(2);
		sigDD.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = sigDD.getValue();
				if (Math.abs(Ck.getSigma() - d) < ZERO)
					return;
				if (d > MAX_SIG)
					d = MAX_SIG;
				else if (d < MIN_SIG)
					d = MIN_SIG;
				lj.setLJFunction(d, epsDD.getValue());
				lj.repaint();
				lj.updateTextFields();
				model.notifyChange();
			}
		});

		epsDD = new RealNumberTextField(Ck.getEpsilon(), MIN_EPS, MAX_EPS);
		epsDD.setFont(ViewAttribute.SMALL_FONT);
		epsDD.setMaximumFractionDigits(4);
		epsDD.setMaximumIntegerDigits(2);
		epsDD.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = epsDD.getValue();
				if (Math.abs(Ck.getEpsilon() - d) < ZERO)
					return;
				if (d > MAX_EPS)
					d = MAX_EPS;
				else if (d < MIN_EPS)
					d = MIN_EPS;
				lj.setLJFunction(sigDD.getValue(), d);
				lj.repaint();
				lj.updateTextFields();
				model.notifyChange();
			}
		});

		JPanel panel = new JPanel(new GridLayout(4, 4, 5, 3));
		panel.add(aaButton);
		panel.add(sigAALabel);
		panel.add(sigAA);
		panel.add(epsAALabel);
		panel.add(epsAA);
		panel.add(bbButton);
		panel.add(sigBBLabel);
		panel.add(sigBB);
		panel.add(epsBBLabel);
		panel.add(epsBB);
		panel.add(ccButton);
		panel.add(sigCCLabel);
		panel.add(sigCC);
		panel.add(epsCCLabel);
		panel.add(epsCC);
		panel.add(ddButton);
		panel.add(sigDDLabel);
		panel.add(sigDD);
		panel.add(epsDDLabel);
		panel.add(epsDD);

		abPanel = new CrossPanel(Nt, Pl);
		acPanel = new CrossPanel(Nt, Ws);
		adPanel = new CrossPanel(Nt, Ck);
		bcPanel = new CrossPanel(Pl, Ws);
		bdPanel = new CrossPanel(Pl, Ck);
		cdPanel = new CrossPanel(Ws, Ck);

		tabbedPane = new TabbedPane();
		tabbedPane.setFont(new Font(null, Font.PLAIN, 9));
		String s = MDView.getInternationalText("SameElements");
		tabbedPane.addTab(s != null ? s : "Same Elements", null, panel);
		tabbedPane.addTab(abString, null, abPanel);
		tabbedPane.addTab(acString, null, acPanel);
		tabbedPane.addTab(adString, null, adPanel);
		tabbedPane.addTab(bcString, null, bcPanel);
		tabbedPane.addTab(bdString, null, bdPanel);
		tabbedPane.addTab(cdString, null, cdPanel);
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (tabbedPane.getSelectedIndex() != 0) {
					setSelectedInteraction(tabbedPane.getSelectedIndex() + 4);
					CrossPanel cp = (CrossPanel) tabbedPane.getSelectedComponent();
					if (getLBMixing(cp.elementA, cp.elementB)) {
						cp.mix1.setSelected(true);
						lj.setEnabled(false);
					}
					else {
						cp.mix1.setSelected(false);
						lj.setEnabled(true);
					}
					if (getRepulsive(cp.elementA, cp.elementB)) {
						cp.repul.setSelected(true);
					}
					else {
						cp.repul.setSelected(false);
					}
					double sig = getSigma(cp.elementA, cp.elementB);
					double eps = getEpsilon(cp.elementA, cp.elementB);
					lj.setLJFunction(sig, eps);
					lj.repaint();
					cp.sigma.setValue(sig);
					cp.epsilon.setValue(eps);
				}
				else {
					if (aaButton.isSelected()) {
						setSelectedInteraction(1);
						lj.setLJFunction(Nt.getSigma(), Nt.getEpsilon());
					}
					else if (bbButton.isSelected()) {
						setSelectedInteraction(2);
						lj.setLJFunction(Pl.getSigma(), Pl.getEpsilon());
					}
					else if (ccButton.isSelected()) {
						setSelectedInteraction(3);
						lj.setLJFunction(Ws.getSigma(), Ws.getEpsilon());
					}
					else if (ddButton.isSelected()) {
						setSelectedInteraction(4);
						lj.setLJFunction(Ck.getSigma(), Ck.getEpsilon());
					}
					lj.setEnabled(true);
					lj.repaint();
				}
			}
		});

		buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		cutoffButton = new JButton(model.getView().getActionMap().get("Set cut-off parameters"));
		cutoffButton.setText("Cutoff");
		buttonPanel.add(cutoffButton);

		s = MDView.getInternationalText("Reset");
		JButton button = new JButton(s != null ? s : "Reset");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Nt.setProperties(1.0, 7.0, 0.1);
				Pl.setProperties(2.0, 14.0, 0.1);
				Ws.setProperties(3.0, 21.0, 0.1);
				Ck.setProperties(4.0, 28.0, 0.1);
				affinity.setLBMixing(Nt, Pl, true);
				affinity.setLBMixing(Nt, Ws, true);
				affinity.setLBMixing(Nt, Ck, true);
				affinity.setLBMixing(Pl, Ws, true);
				affinity.setLBMixing(Pl, Ck, true);
				affinity.setLBMixing(Ws, Ck, true);
				affinity.setRepulsive(Nt, Pl, false);
				affinity.setRepulsive(Nt, Ws, false);
				affinity.setRepulsive(Nt, Ck, false);
				affinity.setRepulsive(Pl, Ws, false);
				affinity.setRepulsive(Pl, Ck, false);
				affinity.setRepulsive(Ws, Ck, false);
				epsAA.setValue(Nt.getEpsilon());
				sigAA.setValue(Nt.getSigma());
				epsBB.setValue(Pl.getEpsilon());
				sigBB.setValue(Pl.getSigma());
				epsCC.setValue(Ws.getEpsilon());
				sigCC.setValue(Ws.getSigma());
				epsDD.setValue(Ck.getEpsilon());
				sigDD.setValue(Ck.getSigma());
				abPanel.sigma.setValue(affinity.getSigma(Nt, Pl));
				acPanel.sigma.setValue(affinity.getSigma(Nt, Ws));
				adPanel.sigma.setValue(affinity.getSigma(Nt, Ck));
				bcPanel.sigma.setValue(affinity.getSigma(Pl, Ws));
				bdPanel.sigma.setValue(affinity.getSigma(Pl, Ck));
				cdPanel.sigma.setValue(affinity.getSigma(Ws, Ck));
				abPanel.epsilon.setValue(affinity.getEpsilon(Nt, Pl));
				acPanel.epsilon.setValue(affinity.getEpsilon(Nt, Ws));
				adPanel.epsilon.setValue(affinity.getEpsilon(Nt, Ck));
				bcPanel.epsilon.setValue(affinity.getEpsilon(Pl, Ws));
				bdPanel.epsilon.setValue(affinity.getEpsilon(Pl, Ck));
				cdPanel.epsilon.setValue(affinity.getEpsilon(Ws, Ck));
				int ntab = tabbedPane.getTabCount();
				for (int i = 1; i < ntab; i++) {
					((CrossPanel) tabbedPane.getComponentAt(i)).mix1.setSelected(true);
				}
				switch (getSelectedTabIndex()) {
				case 0:
					if (aaButton.isSelected())
						lj.setLJFunction(Nt.getSigma(), Nt.getEpsilon());
					else if (bbButton.isSelected())
						lj.setLJFunction(Pl.getSigma(), Pl.getEpsilon());
					else if (ccButton.isSelected())
						lj.setLJFunction(Ws.getSigma(), Ws.getEpsilon());
					else if (ddButton.isSelected())
						lj.setLJFunction(Ck.getSigma(), Ck.getEpsilon());
					break;
				case 1:
					lj.setLJFunction(affinity.getSigma(Nt, Pl), affinity.getEpsilon(Nt, Pl));
					break;
				case 2:
					lj.setLJFunction(affinity.getSigma(Nt, Ws), affinity.getEpsilon(Nt, Ws));
					break;
				case 3:
					lj.setLJFunction(affinity.getSigma(Nt, Ck), affinity.getEpsilon(Nt, Ck));
					break;
				case 4:
					lj.setLJFunction(affinity.getSigma(Pl, Ws), affinity.getEpsilon(Pl, Ws));
					break;
				case 5:
					lj.setLJFunction(affinity.getSigma(Pl, Ck), affinity.getEpsilon(Pl, Ck));
					break;
				case 6:
					lj.setLJFunction(affinity.getSigma(Ws, Ck), affinity.getEpsilon(Ws, Ck));
					break;
				}
				lj.repaint();
				model.getView().repaint();
				model.notifyChange();
			}
		});
		buttonPanel.add(button);

		s = MDView.getInternationalText("CloseButton");
		closeButton = new JButton(s != null ? s : "Close");
		buttonPanel.add(closeButton);

		tabbedPane.setPreferredSize(new Dimension(300, 120));

		lj.setPreferredSize(new Dimension(350, 280));

	}

	public JDialog createDialog(Component owner, boolean modal) {
		switch (selectedInteraction) {
		case 1:
			lj.setLJFunction(Nt.getSigma(), Nt.getEpsilon());
			break;
		case 2:
			lj.setLJFunction(Pl.getSigma(), Pl.getEpsilon());
			break;
		case 3:
			lj.setLJFunction(Ws.getSigma(), Ws.getEpsilon());
			break;
		case 4:
			lj.setLJFunction(Ck.getSigma(), Ck.getEpsilon());
			break;
		}
		if (model != null)
			setModel(model);
		String s = MDView.getInternationalText("LennardJonesInteractionSetting");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(owner), s != null ? s
				: "Set Lennard-Jones Interactions", modal);
		d.setResizable(false);
		d.setSize(550, 400);
		d.getContentPane().setLayout(new BorderLayout());
		d.getContentPane().add(lj, BorderLayout.CENTER);
		d.getContentPane().add(tabbedPane, BorderLayout.NORTH);
		d.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				d.getContentPane().removeAll();
				d.dispose();
			}
		});
		ActionListener[] al = closeButton.getActionListeners();
		for (int i = 0; i < al.length; i++)
			closeButton.removeActionListener(al[i]);
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.getContentPane().removeAll();
				d.dispose();
			}
		});
		d.pack();
		d.setLocationRelativeTo(owner);
		return d;
	}

	public MolecularModel getModel() {
		return model;
	}

	public void setModel(MolecularModel m) {

		if (model != m)
			model = m;

		if (Nt != model.getElement("Nt"))
			Nt = model.getElement("Nt");
		if (sigAA != null)
			sigAA.setValue(Nt.getSigma());
		if (epsAA != null)
			epsAA.setValue(Nt.getEpsilon());

		if (Pl != model.getElement("Pl"))
			Pl = model.getElement("Pl");
		if (sigBB != null)
			sigBB.setValue(Pl.getSigma());
		if (epsBB != null)
			epsBB.setValue(Pl.getEpsilon());

		if (Ws != model.getElement("Ws"))
			Ws = model.getElement("Ws");
		if (sigCC != null)
			sigCC.setValue(Ws.getSigma());
		if (epsCC != null)
			epsCC.setValue(Ws.getEpsilon());

		if (Ck != model.getElement("Ck"))
			Ck = model.getElement("Ck");
		if (sigDD != null)
			sigDD.setValue(Ck.getSigma());
		if (epsDD != null)
			epsDD.setValue(Ck.getEpsilon());

		if (affinity != model.getAffinity())
			affinity = model.getAffinity();

		if (abPanel != null) {
			abPanel.elementA = Nt;
			abPanel.elementB = Pl;
			abPanel.mix1.setSelected(affinity.isLBMixed(Nt, Pl));
			abPanel.attra.setSelected(!affinity.isLBMixed(Nt, Pl));
			abPanel.repul.setSelected(affinity.isRepulsive(Nt, Pl));
			abPanel.sigma.setValue(affinity.getSigma(Nt, Pl));
			abPanel.epsilon.setValue(affinity.getEpsilon(Nt, Pl));
			abPanel.sigma.setEnabled(!affinity.isLBMixed(Nt, Pl));
			abPanel.epsilon.setEnabled(!affinity.isLBMixed(Nt, Pl));
		}
		if (acPanel != null) {
			acPanel.elementA = Nt;
			acPanel.elementB = Ws;
			acPanel.mix1.setSelected(affinity.isLBMixed(Nt, Ws));
			acPanel.attra.setSelected(!affinity.isLBMixed(Nt, Ws));
			acPanel.repul.setSelected(affinity.isRepulsive(Nt, Ws));
			acPanel.sigma.setValue(affinity.getSigma(Nt, Ws));
			acPanel.epsilon.setValue(affinity.getEpsilon(Nt, Ws));
			acPanel.sigma.setEnabled(!affinity.isLBMixed(Nt, Ws));
			acPanel.epsilon.setEnabled(!affinity.isLBMixed(Nt, Ws));
		}
		if (adPanel != null) {
			adPanel.elementA = Nt;
			adPanel.elementB = Ck;
			adPanel.mix1.setSelected(affinity.isLBMixed(Nt, Ck));
			adPanel.attra.setSelected(!affinity.isLBMixed(Nt, Ck));
			adPanel.repul.setSelected(affinity.isRepulsive(Nt, Ck));
			adPanel.sigma.setValue(affinity.getSigma(Nt, Ck));
			adPanel.epsilon.setValue(affinity.getEpsilon(Nt, Ck));
			adPanel.sigma.setEnabled(!affinity.isLBMixed(Nt, Ck));
			adPanel.epsilon.setEnabled(!affinity.isLBMixed(Nt, Ck));
		}
		if (bcPanel != null) {
			bcPanel.elementA = Pl;
			bcPanel.elementB = Ws;
			bcPanel.mix1.setSelected(affinity.isLBMixed(Pl, Ws));
			bcPanel.attra.setSelected(!affinity.isLBMixed(Pl, Ws));
			bcPanel.repul.setSelected(affinity.isRepulsive(Pl, Ws));
			bcPanel.sigma.setValue(affinity.getSigma(Pl, Ws));
			bcPanel.epsilon.setValue(affinity.getEpsilon(Pl, Ws));
			bcPanel.sigma.setEnabled(!affinity.isLBMixed(Pl, Ws));
			bcPanel.epsilon.setEnabled(!affinity.isLBMixed(Pl, Ws));
		}
		if (bdPanel != null) {
			bdPanel.elementA = Pl;
			bdPanel.elementB = Ck;
			bdPanel.mix1.setSelected(affinity.isLBMixed(Pl, Ck));
			bdPanel.attra.setSelected(!affinity.isLBMixed(Pl, Ck));
			bdPanel.repul.setSelected(affinity.isRepulsive(Pl, Ck));
			bdPanel.sigma.setValue(affinity.getSigma(Pl, Ck));
			bdPanel.epsilon.setValue(affinity.getEpsilon(Pl, Ck));
			bdPanel.sigma.setEnabled(!affinity.isLBMixed(Pl, Ck));
			bdPanel.epsilon.setEnabled(!affinity.isLBMixed(Pl, Ck));
		}
		if (cdPanel != null) {
			cdPanel.elementA = Ws;
			cdPanel.elementB = Ck;
			cdPanel.mix1.setSelected(affinity.isLBMixed(Ws, Ck));
			cdPanel.attra.setSelected(!affinity.isLBMixed(Ws, Ck));
			cdPanel.repul.setSelected(affinity.isRepulsive(Ws, Ck));
			cdPanel.sigma.setValue(affinity.getSigma(Ws, Ck));
			cdPanel.epsilon.setValue(affinity.getEpsilon(Ws, Ck));
			cdPanel.sigma.setEnabled(!affinity.isLBMixed(Ws, Ck));
			cdPanel.epsilon.setEnabled(!affinity.isLBMixed(Ws, Ck));
		}

		if (cutoffButton != null) {
			cutoffButton.setAction(model.getView().getActionMap().get("Set cut-off parameters"));
			String s = MDView.getInternationalText("Cutoff");
			cutoffButton.setText(s != null ? s : "Cutoff");
		}

	}

	class CrossPanel extends JPanel {

		Element elementA, elementB;
		RealNumberTextField epsilon;
		SigmaField sigma;
		JRadioButton repul, attra, mix1;

		public CrossPanel(Element eA, Element eB) {

			super(new GridLayout(5, 1));

			elementA = eA;
			elementB = eB;

			ButtonGroup bg = new ButtonGroup();

			String s = MDView.getInternationalText("MeanLennardJonesPotential");
			mix1 = new JRadioButton(s != null ? s : "Mean Lennard-Jones Potential");
			if (s == null)
				mix1.setFont(ViewAttribute.SMALL_FONT);
			mix1.setSelected(true);
			mix1.setFocusPainted(false);
			mix1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					affinity.setRepulsive(elementA, elementB, repul.isSelected());
					affinity.setLBMixing(elementA, elementB, mix1.isSelected());
					lj.setLJFunction(affinity.getSigma(elementA, elementB), affinity.getEpsilon(elementA, elementB));
					lj.setEnabled(false);
					lj.repaint();
					sigma.setValue(Math.sqrt(elementA.getSigma() * elementB.getSigma()));
					epsilon.setValue(0.5 * (elementA.getEpsilon() + elementB.getEpsilon()));
					affinity.setSigma(elementA, elementB, sigma.getValue());
					affinity.setEpsilon(elementA, elementB, epsilon.getValue());
					sigma.setEnabled(false);
					epsilon.setEnabled(false);
					model.notifyChange();
				}
			});
			bg.add(mix1);
			add(mix1);

			s = MDView.getInternationalText("UserDefinedLennardJonesPotential");
			attra = new JRadioButton(s != null ? s : "User-Defined Lennard-Jones Potential");
			if (s == null)
				attra.setFont(ViewAttribute.SMALL_FONT);
			attra.setFocusPainted(false);
			attra.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					affinity.setRepulsive(elementA, elementB, repul.isSelected());
					affinity.setLBMixing(elementA, elementB, mix1.isSelected());
					lj.setEnabled(true);
					lj.repaint();
					sigma.setEnabled(true);
					epsilon.setEnabled(true);
					model.notifyChange();
				}
			});
			bg.add(attra);
			add(attra);

			s = MDView.getInternationalText("UserDefinedRepulsivePotential");
			repul = new JRadioButton(s != null ? s : "User-Defined Repulsive Potential");
			if (s == null)
				repul.setFont(ViewAttribute.SMALL_FONT);
			repul.setFocusPainted(false);
			repul.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					affinity.setRepulsive(elementA, elementB, repul.isSelected());
					affinity.setLBMixing(elementA, elementB, mix1.isSelected());
					lj.setEnabled(true);
					lj.repaint();
					sigma.setEnabled(true);
					epsilon.setEnabled(true);
					model.notifyChange();
				}
			});
			bg.add(repul);
			add(repul);

			JPanel p = new JPanel(new GridLayout(1, 4));

			p.add(new JLabel("<html><font size=3><i>&#963;</i></font> (&#197;)</html>", JLabel.CENTER));

			sigma = new SigmaField(Math.sqrt(elementA.getSigma() * elementB.getSigma()), MIN_SIG, MAX_SIG);
			sigma.setFont(ViewAttribute.SMALL_FONT);
			sigma.setEnabled(false);
			sigma.setMaximumFractionDigits(3);
			sigma.setMaximumIntegerDigits(2);
			sigma.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					double d = sigma.getValue();
					if (d > MAX_SIG)
						d = MAX_SIG;
					else if (d < MIN_SIG)
						d = MIN_SIG;
					lj.setLJFunction(d, epsilon.getValue());
					lj.repaint();
					lj.updateTextFields();
				}
			});
			p.add(sigma);

			p.add(new JLabel("<html><font size=3><i>&#949;</i></font> (eV)</html>", JLabel.CENTER));

			epsilon = new RealNumberTextField(0.5 * (elementA.getEpsilon() + elementB.getEpsilon()), MIN_EPS, MAX_EPS);
			epsilon.setFont(ViewAttribute.SMALL_FONT);
			epsilon.setEnabled(false);
			epsilon.setMaximumFractionDigits(4);
			epsilon.setMaximumIntegerDigits(2);
			epsilon.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					double d = epsilon.getValue();
					if (d > MAX_EPS)
						d = MAX_EPS;
					else if (d < MIN_EPS)
						d = MIN_EPS;
					lj.setLJFunction(sigma.getValue(), d);
					lj.repaint();
					lj.updateTextFields();
				}
			});
			p.add(epsilon);

			add(p);

		}

	}

	private class TabbedPane extends JTabbedPane {

		public TabbedPane() {
			super();
		}

		public Component getComponentWithTitle(String title) {
			int n = getTabCount();
			for (int i = 0; i < n; i++) {
				if (getTitleAt(i).equals(title))
					return getComponentAt(i);
			}
			return null;
		}

	}

	private class SigmaField extends RealNumberTextField {

		public SigmaField(double value, double min, double max) throws IllegalArgumentException {
			super(value, min, max);
			setMaximumFractionDigits(5);
			setMaximumIntegerDigits(2);
			setCheckBounds(false);
		}

		public void setValue(double d) {
			super.setValue(0.1 * d);
		}

		public double getValue() {
			return super.getValue() * 10.0;
		}

	}

}