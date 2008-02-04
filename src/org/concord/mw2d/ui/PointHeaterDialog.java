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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.concord.mw2d.MDView;
import org.concord.mw2d.PointHeater;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.models.MDModel;

class PointHeaterDialog extends JPanel {

	private JRadioButton[] sizeButton;

	JDialog showDialog(Frame parent) {

		final JDialog d = new JDialog(parent, "Action Zone", true);
		d.setResizable(false);
		d.setSize(220, 360);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		d.getContentPane().add(this, BorderLayout.CENTER);

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		String s = MDContainer.getInternationalText("Close");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.removeAll();
				d.dispose();
			}
		});
		panel.add(button);

		d.getContentPane().add(panel, BorderLayout.SOUTH);

		d.pack();

		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				d.removeAll();
				d.dispose();
			}
		});

		return d;

	}

	PointHeaterDialog(final MDModel model) {

		super(new BorderLayout());

		setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
				"Increasing selection zone >>>>>>"));

		sizeButton = new JRadioButton[5];

		JPanel panel = new JPanel(new GridLayout(1, sizeButton.length, 10, 10)) {
			public Insets getInsets() {
				return new Insets(10, 10, 10, 10);
			}
		};
		add(panel, BorderLayout.CENTER);

		ButtonGroup buttonGroup = new ButtonGroup();

		for (int i = 0; i < sizeButton.length; i++) {

			final int ii = i;
			sizeButton[i] = new JRadioButton(new Icon() {

				private GeneralPath path;

				private void drawSine(Graphics2D g) {
					int x = getIconWidth() / 2;
					int y = getIconHeight() / 2;
					int n = 40;
					if (path == null)
						path = new GeneralPath();
					else path.reset();
					path.moveTo(x, y);
					for (int i = 0; i < n; i += 2) {
						x++;
						path.lineTo(x, (int) (y + 4.0 * Math.sin(i * 0.1 * Math.PI)));
					}
					g.draw(path);
				}

				public int getIconWidth() {
					return 48;
				}

				public int getIconHeight() {
					return 48;
				}

				public void paintIcon(Component c, Graphics g, int x, int y) {
					Graphics2D g2 = (Graphics2D) g;
					g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(Color.white);
					g2.fillRect(2, 2, getIconWidth() - 5, getIconHeight() - 5);
					int d = (ii + 1) * 6;
					int xc = getIconWidth() / 2;
					int yc = getIconHeight() / 2;
					int action = ((MDView) model.getView()).getAction();
					Color color = Color.lightGray;
					if (action == UserAction.COOL_ID)
						color = Color.blue;
					else if (action == UserAction.HEAT_ID)
						color = Color.red;
					g2.setColor(color);
					g2.fillOval(xc - d / 2, yc - d / 2, d, d);
					g2.setColor(Color.gray);
					g2.drawOval(xc - d / 2, yc - d / 2, d, d);
					g2.drawRect(2, 2, getIconWidth() - 5, getIconHeight() - 5);
					g2.setColor(Color.black);
					if (((JRadioButton) c).isSelected()) {
						g2.drawRect(0, 0, getIconWidth() - 1, getIconHeight() - 1);
					}
					drawSine(g2);
					g2.rotate(0.66666667 * Math.PI, xc, yc);
					drawSine(g2);
					g2.rotate(0.66666667 * Math.PI, xc, yc);
					drawSine(g2);
					g2.dispose();
				}

			}) {
				public Insets getInsets() {
					return new Insets(10, 5, 5, 5);
				}
			};
			sizeButton[i].addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED)
						((MDView) model.getView()).getPointHeater().setSize((ii + 1) * PointHeater.MIN_DIAMETER);
				}
			});
			sizeButton[i].setToolTipText("Diameter " + (i + 1) * PointHeater.MIN_DIAMETER / 10 + " angstroms ("
					+ (i + 1) * PointHeater.MIN_DIAMETER + " pixels)");
			sizeButton[i].setPreferredSize(new Dimension(48, 48));
			buttonGroup.add(sizeButton[i]);
			panel.add(sizeButton[i]);
		}

		sizeButton[2].setSelected(true);

	}

}