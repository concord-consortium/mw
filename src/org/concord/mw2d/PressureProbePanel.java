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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.mw2d.models.RectangularObstacle;

class PressureProbePanel extends JPanel {

	private RectangularObstacle obs;
	private byte side;

	PressureProbePanel() {

		super(new BorderLayout());
		setPreferredSize(new Dimension(200, 100));

		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		p.setBorder(BorderFactory.createTitledBorder("Customize measurement:"));
		add(p, BorderLayout.CENTER);

		p.add(new JLabel("Under construction......"), BorderLayout.CENTER);

	}

	private void confirm() {
		if (obs == null)
			return;
	}

	JDialog createDialog(RectangularObstacle o, byte i) {

		obs = o;
		side = i;

		String s = null;
		switch (side) {
		case RectangularObstacle.WEST:
			s = MDView.getInternationalText("West");
			if (s == null)
				s = "west";
			break;
		case RectangularObstacle.EAST:
			s = MDView.getInternationalText("East");
			if (s == null)
				s = "east";
			break;
		case RectangularObstacle.SOUTH:
			s = MDView.getInternationalText("South");
			if (s == null)
				s = "south";
			break;
		case RectangularObstacle.NORTH:
			s = MDView.getInternationalText("North");
			if (s == null)
				s = "north";
			break;
		}

		String s1 = MDView.getInternationalText("PressureProbe");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(obs.getHostModel().getView()), (s1 != null ? s1
				: "Pressure probe")
				+ ": " + s, true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		d.getContentPane().add(this, BorderLayout.CENTER);

		JPanel p = new JPanel();
		d.getContentPane().add(p, BorderLayout.SOUTH);

		s = MDView.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				d.dispose();
			}
		});
		p.add(button);

		s = MDView.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		p.add(button);

		d.pack();
		d.setLocationRelativeTo(obs.getHostModel().getView());

		return d;

	}

}