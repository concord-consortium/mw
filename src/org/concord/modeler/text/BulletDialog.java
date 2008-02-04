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

package org.concord.modeler.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.BevelBorder;

import org.concord.modeler.Modeler;

class BulletDialog extends JDialog {

	private Page page;
	private JPanel bulletPane;
	private int bulletType = BulletIcon.OPEN_SQUARE_BULLET;

	BulletDialog(final Page page0) {

		super(JOptionPane.getFrameForComponent(page0), "Bullets", false);
		String s = Modeler.getInternationalText("Bullet");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(false);

		this.page = page0;
		Container container = getContentPane();
		container.setLayout(new BorderLayout());

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		container.add(BorderLayout.CENTER, tabbedPane);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		container.add(BorderLayout.SOUTH, p);

		s = Modeler.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BulletDialog.this.dispose();
			}
		});
		p.add(button);

		bulletPane = new JPanel(new GridLayout(2, 4, 5, 5));
		bulletPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		s = Modeler.getInternationalText("Bullet");
		tabbedPane.addTab(s != null ? s : "Bulleted", bulletPane);

		bulletPane.add(new BulletView(BulletIcon.NO_BULLET));
		bulletPane.add(new BulletView(BulletIcon.SOLID_CIRCLE_BULLET));
		bulletPane.add(new BulletView(BulletIcon.OPEN_CIRCLE_BULLET));
		bulletPane.add(new BulletView(BulletIcon.SOLID_SQUARE_BULLET));
		bulletPane.add(new BulletView(BulletIcon.OPEN_SQUARE_BULLET));
		bulletPane.add(new BulletView(BulletIcon.POS_TICK_BULLET));
		bulletPane.add(new BulletView(BulletIcon.NEG_TICK_BULLET));
		bulletPane.add(new BulletView(BulletIcon.DIAMOND_BULLET));

		bulletPane.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				Component c = bulletPane.getComponentAt(e.getX(), e.getY());
				if (c == null)
					return;
				selectView((BulletView) c);
			}
		});

		pack();
		setLocationRelativeTo(getOwner());

	}

	void setBulletType(int type) {
		bulletType = type;
		if (bulletPane == null)
			return;
		if (bulletPane.getComponentCount() == 0)
			return;
		BulletView bv = null;
		for (int i = 0; i < bulletPane.getComponentCount(); i++) {
			bv = (BulletView) bulletPane.getComponent(i);
			bv.setSelected(bv.getType() == type);
			bv.repaint();
		}
	}

	int getBulletType() {
		return bulletType;
	}

	void selectView(BulletView view) {
		if (bulletPane == null)
			return;
		if (bulletPane.getComponentCount() == 0)
			return;
		BulletView bv = null;
		for (int i = 0; i < bulletPane.getComponentCount(); i++) {
			bv = (BulletView) bulletPane.getComponent(i);
			bv.setSelected(bv == view);
			if (bv.isSelected())
				page.setBulletType(bv.getType());
			bv.repaint();
		}
	}

	BulletView getBulletView(int type) {
		if (bulletPane == null)
			return null;
		if (bulletPane.getComponentCount() == 0)
			return null;
		BulletView bv = null;
		for (int i = 0; i < bulletPane.getComponentCount(); i++) {
			bv = (BulletView) bulletPane.getComponent(i);
			if (bv.getType() == type)
				return bv;
		}
		return bv;
	}

	private class BulletView extends JPanel {

		private int type = BulletIcon.OPEN_SQUARE_BULLET;
		private boolean selected;

		BulletView(int type) {
			super();
			this.type = type;
			setPreferredSize(new Dimension(90, 100));
			setBorder(new BevelBorder(BevelBorder.LOWERED));
		}

		int getType() {
			return type;
		}

		void setSelected(boolean b) {
			selected = b;
		}

		boolean isSelected() {
			return selected;
		}

		public void paintComponent(Graphics g) {

			super.paintComponent(g);

			Dimension dim = getSize();
			g.setColor(Color.white);
			g.fillRect(0, 0, dim.width, dim.height);

			if (type == BulletIcon.NO_BULLET) {

				FontMetrics fm = g.getFontMetrics();
				g.setFont(new Font(null, Font.PLAIN, 16));
				g.setColor(Color.black);
				String s = Modeler.getInternationalText("None");
				if (s == null)
					s = "None";
				g.drawString(s, (dim.width - fm.stringWidth(s)) / 2, dim.height / 2 + fm.getAscent() / 4);

			}
			else {

				int x1 = dim.width / 7;
				int y1 = dim.height / 7;
				Icon icon = null;

				for (int i = 0; i < 3; i++) {

					switch (type) {

					case BulletIcon.SOLID_CIRCLE_BULLET:
						icon = BulletIcon.SolidCircleBulletIcon.sharedInstance();
						break;

					case BulletIcon.OPEN_CIRCLE_BULLET:
						icon = BulletIcon.OpenCircleBulletIcon.sharedInstance();
						break;

					case BulletIcon.SOLID_SQUARE_BULLET:
						icon = BulletIcon.SolidSquareBulletIcon.sharedInstance();
						break;

					case BulletIcon.OPEN_SQUARE_BULLET:
						icon = BulletIcon.SquareBulletIcon.sharedInstance();
						break;

					case BulletIcon.POS_TICK_BULLET:
						icon = BulletIcon.PosTickBulletIcon.sharedInstance();
						break;

					case BulletIcon.NEG_TICK_BULLET:
						icon = BulletIcon.NegTickBulletIcon.sharedInstance();
						break;

					case BulletIcon.DIAMOND_BULLET:
						icon = BulletIcon.DiamondBulletIcon.sharedInstance();
						break;

					}

					if (icon != null) {
						icon.paintIcon(this, g, x1, y1);
						g.setColor(Color.lightGray);
						drawHorizontalLines(g, x1 + icon.getIconWidth() + 10, dim.width - 10, y1 + icon.getIconHeight()
								/ 2);
						y1 += 2 * dim.height / 7;
					}

				}

			}

			if (selected) {
				g.setColor(Color.black);
				g.drawRect(5, 5, dim.width - 10, dim.height - 10);
				g.drawRect(6, 6, dim.width - 12, dim.height - 12);
			}
			g.setColor(Color.black);
			g.drawLine(2, 2, dim.width, 2);
			g.drawLine(2, 2, 2, dim.height);
			g.setColor(Color.lightGray);
			g.drawLine(dim.width - 2, dim.height - 3, 0, dim.height - 3);
			g.drawLine(dim.width - 3, 0, dim.width - 3, dim.height - 2);
			paintBorder(g);

		}

		private void drawHorizontalLines(Graphics g, int x1, int x2, int y) {
			g.drawLine(x1, y - 1, x2, y - 1);
			g.drawLine(x1, y, x2, y);
			y += getHeight() / 7;
			g.drawLine(x1, y - 1, x2, y - 1);
			g.drawLine(x1, y, x2, y);
		}

	}

}