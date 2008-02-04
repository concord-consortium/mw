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
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.Wall;

class AtomFlowPanel extends JPanel {

	private MolecularModel model;
	private JRadioButton rbFlowInWest;
	private JRadioButton rbFlowInEast;
	private JRadioButton rbFlowInSouth;
	private JRadioButton rbFlowInNorth;
	private JRadioButton rbFlowOutWest;
	private JRadioButton rbFlowOutEast;
	private JRadioButton rbFlowOutSouth;
	private JRadioButton rbFlowOutNorth;
	private JRadioButton rbFlowOutNone;
	private JCheckBox cbFlowInNt;
	private JCheckBox cbFlowInPl;
	private JCheckBox cbFlowInWs;
	private JCheckBox cbFlowInCk;
	private JCheckBox cbFlowOutNt;
	private JCheckBox cbFlowOutPl;
	private JCheckBox cbFlowOutWs;
	private JCheckBox cbFlowOutCk;
	private JSlider flowSlider;

	AtomFlowPanel(MolecularModel m) {

		super(new BorderLayout());

		model = m;

		JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
		add(panel, BorderLayout.NORTH);

		JPanel p = new JPanel(new GridLayout(5, 1, 5, 5));
		p.setBorder(BorderFactory.createTitledBorder("The side atoms flow in:"));
		panel.add(p, BorderLayout.WEST);

		ButtonGroup bg = new ButtonGroup();

		rbFlowInWest = new JRadioButton("West");
		rbFlowInWest.setSelected(true);
		p.add(rbFlowInWest);
		bg.add(rbFlowInWest);

		rbFlowInSouth = new JRadioButton("South");
		p.add(rbFlowInSouth);
		bg.add(rbFlowInSouth);

		rbFlowInEast = new JRadioButton("East");
		p.add(rbFlowInEast);
		bg.add(rbFlowInEast);

		rbFlowInNorth = new JRadioButton("North");
		p.add(rbFlowInNorth);
		bg.add(rbFlowInNorth);

		p = new JPanel(new GridLayout(5, 1, 5, 5));
		p.setBorder(BorderFactory.createTitledBorder("The side atoms flow out:"));
		panel.add(p, BorderLayout.CENTER);

		bg = new ButtonGroup();

		rbFlowOutWest = new JRadioButton("West");
		p.add(rbFlowOutWest);
		bg.add(rbFlowOutWest);

		rbFlowOutSouth = new JRadioButton("South");
		p.add(rbFlowOutSouth);
		bg.add(rbFlowOutSouth);

		rbFlowOutEast = new JRadioButton("East");
		rbFlowOutEast.setSelected(true);
		p.add(rbFlowOutEast);
		bg.add(rbFlowOutEast);

		rbFlowOutNorth = new JRadioButton("North");
		p.add(rbFlowOutNorth);
		bg.add(rbFlowOutNorth);

		rbFlowOutNone = new JRadioButton("None");
		p.add(rbFlowOutNone);
		bg.add(rbFlowOutNone);

		panel = new JPanel(new BorderLayout());
		add(panel, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(p, BorderLayout.NORTH);

		p.add(new JLabel("Flow-in atom types :    "));

		cbFlowInNt = new JCheckBox("Nt");
		cbFlowInNt.setSelected(true);
		p.add(cbFlowInNt);

		cbFlowInPl = new JCheckBox("Pl");
		p.add(cbFlowInPl);

		cbFlowInWs = new JCheckBox("Ws");
		p.add(cbFlowInWs);

		cbFlowInCk = new JCheckBox("Ck");
		p.add(cbFlowInCk);

		// cbCharge=new JCheckBox("Charged");
		// p.add(cbCharge);

		p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(p, BorderLayout.CENTER);

		p.add(new JLabel("Flow-out atom types : "));

		cbFlowOutNt = new JCheckBox("Nt");
		cbFlowOutNt.setSelected(true);
		p.add(cbFlowOutNt);

		cbFlowOutPl = new JCheckBox("Pl");
		cbFlowOutPl.setSelected(true);
		p.add(cbFlowOutPl);

		cbFlowOutWs = new JCheckBox("Ws");
		cbFlowOutWs.setSelected(true);
		p.add(cbFlowOutWs);

		cbFlowOutCk = new JCheckBox("Ck");
		cbFlowOutCk.setSelected(true);
		p.add(cbFlowOutCk);

		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Adjust flux:"));
		add(panel, BorderLayout.SOUTH);

		Hashtable<Integer, JLabel> ht = new Hashtable<Integer, JLabel>();
		ht.put(10, new JLabel("More flow"));
		ht.put(1000, new JLabel("Less flow"));
		flowSlider = new JSlider(10, 1010, model.getFlowInterval());
		flowSlider.setLabelTable(ht);
		flowSlider.setPaintLabels(true);
		flowSlider.setMajorTickSpacing(100);
		flowSlider.setMinorTickSpacing(10);
		flowSlider.setPaintTicks(true);
		panel.add(flowSlider, BorderLayout.CENTER);

	}

	private void confirm() {

		byte i = Wall.WEST;
		if (rbFlowInEast.isSelected())
			i = Wall.EAST;
		else if (rbFlowInSouth.isSelected())
			i = Wall.SOUTH;
		else if (rbFlowInNorth.isSelected())
			i = Wall.NORTH;
		model.setFlowInSide(i);

		i = -1;
		if (rbFlowOutWest.isSelected())
			i = Wall.WEST;
		else if (rbFlowOutEast.isSelected())
			i = Wall.EAST;
		else if (rbFlowOutSouth.isSelected())
			i = Wall.SOUTH;
		else if (rbFlowOutNorth.isSelected())
			i = Wall.NORTH;
		model.setFlowOutSide(i);

		i = 0;
		if (cbFlowInNt.isSelected())
			i++;
		if (cbFlowInPl.isSelected())
			i++;
		if (cbFlowInWs.isSelected())
			i++;
		if (cbFlowInCk.isSelected())
			i++;
		byte[] e = new byte[i];
		i = 0;
		if (cbFlowInNt.isSelected())
			e[i++] = Element.ID_NT;
		if (cbFlowInPl.isSelected())
			e[i++] = Element.ID_PL;
		if (cbFlowInWs.isSelected())
			e[i++] = Element.ID_WS;
		if (cbFlowInCk.isSelected())
			e[i++] = Element.ID_CK;
		model.setFlowInType(e);

		i = 0;
		if (cbFlowOutNt.isSelected())
			i++;
		if (cbFlowOutPl.isSelected())
			i++;
		if (cbFlowOutWs.isSelected())
			i++;
		if (cbFlowOutCk.isSelected())
			i++;
		e = new byte[i];
		i = 0;
		if (cbFlowOutNt.isSelected())
			e[i++] = Element.ID_NT;
		if (cbFlowOutPl.isSelected())
			e[i++] = Element.ID_PL;
		if (cbFlowOutWs.isSelected())
			e[i++] = Element.ID_WS;
		if (cbFlowOutCk.isSelected())
			e[i++] = Element.ID_CK;
		model.setFlowOutType(e);

		model.setFlowInterval(flowSlider.getValue());

	}

	JDialog createDialog() {

		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(model.getView()), "Flow Control Panel", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		d.getContentPane().add(this, BorderLayout.CENTER);

		switch (model.getFlowInSide()) {
		case Wall.WEST:
			rbFlowInWest.setSelected(true);
			break;
		case Wall.EAST:
			rbFlowInEast.setSelected(true);
			break;
		case Wall.SOUTH:
			rbFlowInSouth.setSelected(true);
			break;
		case Wall.NORTH:
			rbFlowInNorth.setSelected(true);
			break;
		}

		switch (model.getFlowOutSide()) {
		case Wall.WEST:
			rbFlowOutWest.setSelected(true);
			break;
		case Wall.EAST:
			rbFlowOutEast.setSelected(true);
			break;
		case Wall.SOUTH:
			rbFlowOutSouth.setSelected(true);
			break;
		case Wall.NORTH:
			rbFlowOutNorth.setSelected(true);
			break;
		case -1:
			rbFlowOutNone.setSelected(true);
			break;
		}

		cbFlowInNt.setSelected(false);
		cbFlowInPl.setSelected(false);
		cbFlowInWs.setSelected(false);
		cbFlowInCk.setSelected(false);

		byte[] type = model.getFlowInType();
		for (byte i = 0; i < type.length; i++) {
			switch (type[i]) {
			case Element.ID_NT:
				cbFlowInNt.setSelected(true);
				break;
			case Element.ID_PL:
				cbFlowInPl.setSelected(true);
				break;
			case Element.ID_WS:
				cbFlowInWs.setSelected(true);
				break;
			case Element.ID_CK:
				cbFlowInCk.setSelected(true);
				break;
			}
		}

		cbFlowOutNt.setSelected(false);
		cbFlowOutPl.setSelected(false);
		cbFlowOutWs.setSelected(false);
		cbFlowOutCk.setSelected(false);

		type = model.getFlowOutType();
		for (byte i = 0; i < type.length; i++) {
			switch (type[i]) {
			case Element.ID_NT:
				cbFlowOutNt.setSelected(true);
				break;
			case Element.ID_PL:
				cbFlowOutPl.setSelected(true);
				break;
			case Element.ID_WS:
				cbFlowOutWs.setSelected(true);
				break;
			case Element.ID_CK:
				cbFlowOutCk.setSelected(true);
				break;
			}
		}

		flowSlider.setValue(model.getFlowInterval());

		JPanel p = new JPanel();
		d.getContentPane().add(p, BorderLayout.SOUTH);

		String s = MDContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				d.dispose();
			}
		});
		p.add(button);

		s = MDContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		p.add(button);

		d.pack();
		d.setLocationRelativeTo(model.getView());

		return d;

	}

}