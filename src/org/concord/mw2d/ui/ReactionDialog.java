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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.event.ParameterChangeEvent;
import org.concord.mw2d.event.ParameterChangeListener;
import org.concord.mw2d.models.Reaction;

class ReactionDialog extends JDialog {

	private List<ParameterChangeListener> listenerList = new ArrayList<ParameterChangeListener>();

	private Reaction reactionType;

	private RealNumberTextField fieldVAA, fieldVBB, fieldVAB, fieldVAC, fieldVBC;
	private RealNumberTextField fieldVBA2, fieldVAB2, fieldVCA2, fieldVCB2, fieldVABC, fieldVBAC;
	private JComponent pane;
	private ActionListener okListener;

	public ReactionDialog(Frame parent) {

		super(parent, "Reaction Parameters", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (reactionType instanceof Reaction.A2_B2__2AB) {
					reactionType.putParameter(Reaction.A2_B2__2AB.VAA, new Double(fieldVAA.getValue()));
					reactionType.putParameter(Reaction.A2_B2__2AB.VBB, new Double(fieldVBB.getValue()));
					reactionType.putParameter(Reaction.A2_B2__2AB.VAB, new Double(fieldVAB.getValue()));
					reactionType.putParameter(Reaction.A2_B2__2AB.VAB2, new Double(fieldVAB2.getValue()));
					reactionType.putParameter(Reaction.A2_B2__2AB.VA2B, new Double(fieldVBA2.getValue()));
					fireParameterChange("Energy Parameters", null, reactionType);
				}
				else if (reactionType instanceof Reaction.A2_B2_C__2AB_C) {
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VAA, new Double(fieldVAA.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VBB, new Double(fieldVBB.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VAB, new Double(fieldVAB.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VAC, new Double(fieldVAC.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VBC, new Double(fieldVBC.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VAB2, new Double(fieldVAB2.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VBA2, new Double(fieldVBA2.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VCA2, new Double(fieldVCA2.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VCB2, new Double(fieldVCB2.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VABC, new Double(fieldVABC.getValue()));
					reactionType.putParameter(Reaction.A2_B2_C__2AB_C.VBAC, new Double(fieldVBAC.getValue()));
					fireParameterChange("Energy Parameters", null, reactionType);
				}
				else if (reactionType instanceof Reaction.O2_2H2__2H2O) {
					reactionType.putParameter(Reaction.O2_2H2__2H2O.VHH, new Double(fieldVAA.getValue()));
					reactionType.putParameter(Reaction.O2_2H2__2H2O.VOO, new Double(fieldVBB.getValue()));
					reactionType.putParameter(Reaction.O2_2H2__2H2O.VHO, new Double(fieldVAB.getValue()));
					reactionType.putParameter(Reaction.O2_2H2__2H2O.VHO2, new Double(fieldVAB2.getValue()));
					reactionType.putParameter(Reaction.O2_2H2__2H2O.VOH2, new Double(fieldVBA2.getValue()));
					fireParameterChange("Energy Parameters", null, reactionType);
				}
				ReactionDialog.this.dispose();
			}
		};

		Container container = getContentPane();
		container.setLayout(new BorderLayout());

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		p.add(new JLabel("Energy unit: eV"));

		String s = MDContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		p.add(button);

		s = MDContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ReactionDialog.this.dispose();
			}
		});
		p.add(button);

		container.add(p, BorderLayout.SOUTH);

	}

	public void addParameterChangeListener(ParameterChangeListener pcl) {
		listenerList.add(pcl);
	}

	public void removeParameterChangeListener(ParameterChangeListener pcl) {
		listenerList.remove(pcl);
	}

	protected void fireParameterChange(String name, Object oldValue, Object newValue) {
		if (oldValue != null && newValue != null && oldValue.equals(newValue))
			return;
		ParameterChangeEvent e = new ParameterChangeEvent(this, name, oldValue, newValue);
		for (ParameterChangeListener l : listenerList)
			l.parameterChanged(e);
	}

	public void setType(Reaction r) {
		reactionType = r;
		if (pane != null)
			getContentPane().remove(pane);
		if (reactionType instanceof Reaction.A2_B2__2AB) {
			pane = createA2B2_2AB();
			getContentPane().add(pane, BorderLayout.CENTER);
		}
		else if (reactionType instanceof Reaction.A2_B2_C__2AB_C) {
			pane = createA2B2C_2ABC();
			getContentPane().add(pane, BorderLayout.CENTER);
		}
		else if (reactionType instanceof Reaction.O2_2H2__2H2O) {
			pane = createH2O2_H2O();
			getContentPane().add(pane, BorderLayout.CENTER);
		}
	}

	public Reaction getType() {
		return reactionType;
	}

	private JComponent createA2B2_2AB() {

		JPanel p = new JPanel(new GridLayout(5, 2, 5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		p.add(new JLabel("<html>&#160; Dissociation energy: A<sub>2</sub> &#8660; 2A&#183;</html>"));
		fieldVAA = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue(), 0.001,
				1000.0);
		fieldVAA.addActionListener(okListener);
		p.add(fieldVAA);

		p.add(new JLabel("<html>&#160; Dissociation energy: B<sub>2</sub> &#8660; 2B&#183;</html>"));
		fieldVBB = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue(), 0.001,
				1000.0);
		fieldVBB.addActionListener(okListener);
		p.add(fieldVBB);

		p.add(new JLabel("<html>&#160; Dissociation energy: AB &#8660; A&#183; + B&#183;</html>"));
		fieldVAB = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue(), 0.001,
				1000.0);
		fieldVAB.addActionListener(okListener);
		p.add(fieldVAB);

		p.add(new JLabel("<html>&#160; Activation energy: A<sub>2</sub> + B&#183; &#8660; A&#183; + AB</html>"));
		fieldVBA2 = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2__2AB.VA2B).doubleValue(), 0.0,
				1000.0);
		fieldVBA2.addActionListener(okListener);
		p.add(fieldVBA2);

		p.add(new JLabel("<html>&#160; Activation energy: A&#183; + B<sub>2</sub> &#8660; AB + B&#183;</html>"));
		fieldVAB2 = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2__2AB.VAB2).doubleValue(), 0.0,
				1000.0);
		fieldVAB2.addActionListener(okListener);
		p.add(fieldVAB2);

		return p;

	}

	private JComponent createA2B2C_2ABC() {

		JPanel p = new JPanel(new GridLayout(11, 2, 5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// A-A
		p.add(new JLabel("<html>&#160; Dissociation energy: A<sub>2</sub> &#8660; 2A&#183;</html>"));
		fieldVAA = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VAA).doubleValue(), 0.001,
				1000.0);
		fieldVAA.addActionListener(okListener);
		p.add(fieldVAA);

		// B-B
		p.add(new JLabel("<html>&#160; Dissociation energy: B<sub>2</sub> &#8660; 2B&#183;</html>"));
		fieldVBB = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VBB).doubleValue(), 0.001,
				1000.0);
		fieldVBB.addActionListener(okListener);
		p.add(fieldVBB);

		// A-B
		p.add(new JLabel("<html>&#160; Dissociation energy: AB &#8660; A&#183; + B&#183;</html>"));
		fieldVAB = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VAB).doubleValue(), 0.001,
				1000.0);
		fieldVAB.addActionListener(okListener);
		p.add(fieldVAB);

		// A-C
		p.add(new JLabel("<html>&#160; Dissociation energy: AC &#8660; A&#183; + C&#183;</html>"));
		fieldVAC = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VAC).doubleValue(), 0.001,
				1000.0);
		fieldVAC.addActionListener(okListener);
		p.add(fieldVAC);

		// B-C
		p.add(new JLabel("<html>&#160; Dissociation energy: BC &#8660; B&#183; + C&#183;</html>"));
		fieldVBC = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VBC).doubleValue(), 0.001,
				1000.0);
		fieldVBC.addActionListener(okListener);
		p.add(fieldVBC);

		// A2-B
		p.add(new JLabel("<html>&#160; Activation energy: A<sub>2</sub> + B&#183; &#8660; A&#183; + AB</html>"));
		fieldVBA2 = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VBA2).doubleValue(), 0.0,
				1000.0);
		fieldVBA2.addActionListener(okListener);
		p.add(fieldVBA2);

		// A-B2
		p.add(new JLabel("<html>&#160; Activation energy: A&#183; + B<sub>2</sub> &#8660; AB + B&#183;</html>"));
		fieldVAB2 = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VAB2).doubleValue(), 0.0,
				1000.0);
		fieldVAB2.addActionListener(okListener);
		p.add(fieldVAB2);

		// C-A2
		p.add(new JLabel("<html>&#160; Activation energy: C + A<sub>2</sub> &#8660; AC + A&#183;</html>"));
		fieldVCA2 = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VCA2).doubleValue(), 0.0,
				1000.0);
		fieldVCA2.addActionListener(okListener);
		p.add(fieldVCA2);

		// C-B2
		p.add(new JLabel("<html>&#160; Activation energy: C + B<sub>2</sub> &#8660; BC + B&#183;</html>"));
		fieldVCB2 = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VCB2).doubleValue(), 0.0,
				1000.0);
		fieldVCB2.addActionListener(okListener);
		p.add(fieldVCB2);

		// A-BC
		p.add(new JLabel("<html>&#160; Activation energy: A&#183; + BC &#8660; AB + C</html>"));
		fieldVABC = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VABC).doubleValue(), 0.0,
				1000.0);
		fieldVABC.addActionListener(okListener);
		p.add(fieldVABC);

		// B-AC
		p.add(new JLabel("<html>&#160; Activation energy: B&#183; + AC &#8660; AB + C</html>"));
		fieldVBAC = new RealNumberTextField(reactionType.getParameter(Reaction.A2_B2_C__2AB_C.VBAC).doubleValue(), 0.0,
				1000.0);
		fieldVBAC.addActionListener(okListener);
		p.add(fieldVBAC);

		JScrollPane sp = new JScrollPane(p);
		sp.setPreferredSize(new Dimension(500, 200));
		return sp;

	}

	private JComponent createH2O2_H2O() {

		JPanel p = new JPanel(new GridLayout(5, 2, 5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		p.add(new JLabel("<html>&#160; Dissociation energy: H<sub>2</sub> &#8660; 2H&#183;</html>"));
		fieldVAA = new RealNumberTextField(reactionType.getParameter(Reaction.O2_2H2__2H2O.VHH).doubleValue(), 0.001,
				1000.0);
		fieldVAA.addActionListener(okListener);
		p.add(fieldVAA);

		p.add(new JLabel("<html>&#160; Dissociation energy: O<sub>2</sub> &#8660; 2O&#183;</html>"));
		fieldVBB = new RealNumberTextField(reactionType.getParameter(Reaction.O2_2H2__2H2O.VOO).doubleValue(), 0.001,
				1000.0);
		fieldVBB.addActionListener(okListener);
		p.add(fieldVBB);

		p.add(new JLabel("<html>&#160; Dissociation energy: HO &#8660; H&#183; + O&#183;</html>"));
		fieldVAB = new RealNumberTextField(reactionType.getParameter(Reaction.O2_2H2__2H2O.VHO).doubleValue(), 0.001,
				1000.0);
		fieldVAB.addActionListener(okListener);
		p.add(fieldVAB);

		p.add(new JLabel("<html>&#160; Activation energy: H<sub>2</sub> + O&#183; &#8660; H&#183; + HO</html>"));
		fieldVBA2 = new RealNumberTextField(reactionType.getParameter(Reaction.O2_2H2__2H2O.VOH2).doubleValue(), 0.0,
				1000.0);
		fieldVBA2.addActionListener(okListener);
		p.add(fieldVBA2);

		p.add(new JLabel("<html>&#160; Activation energy: H&#183; + O<sub>2</sub> &#8660; HO + O&#183;</html>"));
		fieldVAB2 = new RealNumberTextField(reactionType.getParameter(Reaction.O2_2H2__2H2O.VHO2).doubleValue(), 0.0,
				1000.0);
		fieldVAB2.addActionListener(okListener);
		p.add(fieldVAB2);

		return p;

	}

}