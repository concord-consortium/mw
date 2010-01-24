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

package org.concord.modeler.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicBorders;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.GradientFactory;

public class BackgroundComboBox extends JComponent implements FocusListener, PropertyChangeListener {

	protected SelectionPanel selectionPanel;
	protected JButton popButton;
	protected ColorMenu colorMenu;

	public BackgroundComboBox(Component parent, JColorChooser colorChooser, FillEffectChooser fillEffectChooser) {

		setLayout(new BorderLayout());
		setBorder(new BasicBorders.ButtonBorder(Color.lightGray, Color.white, Color.black, Color.gray));

		colorMenu = new ColorMenu(parent, "Background", colorChooser, fillEffectChooser);
		colorMenu.addPropertyChangeListener(this);

		selectionPanel = new SelectionPanel(ModelerUtilities.fileChooser);
		selectionPanel.setPreferredSize(new Dimension(80, 18));
		selectionPanel.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (selectionPanel.isEnabled()) {
					colorMenu.getPopupMenu().show(selectionPanel, 0, selectionPanel.getHeight());
				}
			}
		});
		add(selectionPanel, BorderLayout.CENTER);

		popButton = new JButton(new DownTriangleIcon());
		popButton.setBorder(new BasicBorders.ButtonBorder(Color.gray, Color.black, Color.white, Color.lightGray));
		popButton.setPreferredSize(new Dimension(20, 18));
		popButton.setFocusPainted(false);
		popButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				colorMenu.getPopupMenu().show(selectionPanel, 0, selectionPanel.getHeight());
			}
		});

		add(popButton, BorderLayout.EAST);

		selectionPanel.addFocusListener(this);
		popButton.addFocusListener(this);

	}

	public void destroy() {

		colorMenu.destroy();

		ActionListener[] al = popButton.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				popButton.removeActionListener(al[i]);
		}
		popButton.removeFocusListener(this);

		MouseListener[] ml = selectionPanel.getMouseListeners();
		if (ml != null) {
			for (int i = 0; i < ml.length; i++)
				selectionPanel.removeMouseListener(ml[i]);
		}
		selectionPanel.removeFocusListener(this);
		ModelerUtilities.fileChooser.removePropertyChangeListener(selectionPanel);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				removeAll();
			}
		});

		colorMenu = null;
		selectionPanel = null;
		popButton = null;

	}

	public void setParent(Component parent) {
		colorMenu.setParent(parent);
	}

	public ColorMenu getColorMenu() {
		return colorMenu;
	}

	public void setFillMode(FillMode fm) {
		selectionPanel.setFillMode(fm);
	}

	public FillMode getFillMode() {
		return selectionPanel.getFillMode();
	}

	public void setEnabled(boolean b) {
		selectionPanel.setEnabled(b);
		popButton.setEnabled(b);
	}

	public void focusLost(FocusEvent e) {
		selectionPanel.setBorder(new LineBorder(selectionPanel.unselectedColor, 2));
		selectionPanel.repaint();
	}

	public void focusGained(FocusEvent e) {
		selectionPanel.setBorder(new LineBorder(selectionPanel.selectedColor, 2));
		selectionPanel.repaint();
	}

	public void propertyChange(PropertyChangeEvent e) {

		String name = e.getPropertyName();

		if (name.equals(ColorMenu.FILLING)) {

			Object obj = e.getNewValue();

			if (obj == FillMode.getNoFillMode()) {
				selectionPanel.setFillMode(FillMode.getNoFillMode());
			}
			else if (obj instanceof FillMode.ColorFill) {
				selectionPanel.setFillMode((FillMode.ColorFill) obj);
			}
			else if (obj instanceof FillMode.GradientFill) {
				selectionPanel.setFillMode((FillMode.GradientFill) obj);
			}
			else if (obj instanceof FillMode.PatternFill) {
				selectionPanel.setFillMode((FillMode.PatternFill) obj);
			}
			else if (obj instanceof FillMode.ImageFill) {
				selectionPanel.setFillMode((FillMode.ImageFill) obj);
			}

		}

	}

	protected class SelectionPanel extends ImagePreview {

		private Color selectedColor = Color.black;
		private Color unselectedColor = Color.white;
		private FillMode fillMode = FillMode.getNoFillMode();

		public SelectionPanel(JFileChooser fc) {
			super(fc);
			setBorder(new LineBorder(unselectedColor, 2));
		}

		public boolean getLockRatio() {
			return false;
		}

		public void setFillMode(FillMode fm) {
			fillMode = fm;
			if (fillMode instanceof FillMode.ColorFill) {
				setBackground(((FillMode.ColorFill) fillMode).getColor());
				setPath(null);
			}
			else if (fillMode instanceof FillMode.ImageFill) {
				setPath(((FillMode.ImageFill) fillMode).getURL());
			}
			else {
				setPath(null);
			}
			repaint();
		}

		public FillMode getFillMode() {
			return fillMode;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (fillMode == FillMode.getNoFillMode()) {
				g.setColor(Color.white);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(SelectionPanel.this.isEnabled() ? SystemColor.textText : SystemColor.textInactiveText);
				FontMetrics fm = g.getFontMetrics();
				int w = fm.stringWidth("No Fill");
				int h = fm.getAscent();
				g.drawString("No Fill", (getWidth() - w) / 2, getHeight() / 2 + h / 3);
			}
			else if (fillMode instanceof FillMode.GradientFill) {
				FillMode.GradientFill gfm = (FillMode.GradientFill) fillMode;
				GradientFactory.paintRect((Graphics2D) g, gfm.getStyle(), gfm.getVariant(), gfm.getColor1(), gfm
						.getColor2(), 0, 0, getWidth(), getHeight());
			}
			else if (fillMode instanceof FillMode.PatternFill) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setPaint(((FillMode.PatternFill) fillMode).getPaint());
				g2.fillRect(0, 0, getWidth(), getHeight());
			}
		}

	}

	private class DownTriangleIcon implements Icon {

		public void paintIcon(Component c, Graphics g, int x, int y) {
			int w = getIconWidth();
			int h = getIconHeight();
			Polygon triangle = new Polygon();
			triangle.addPoint(x, y);
			triangle.addPoint(x + w, y);
			triangle.addPoint(x + w / 2, y + h);
			g.setColor(popButton.isEnabled() ? SystemColor.textText : SystemColor.textInactiveText);
			g.fillPolygon(triangle);
		}

		public int getIconWidth() {
			return 6;
		}

		public int getIconHeight() {
			return 4;
		}

	}

}