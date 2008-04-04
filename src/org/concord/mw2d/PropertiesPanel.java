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

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicBorders;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.HyperlinkLabel;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.Particle;

abstract class PropertiesPanel extends JPanel {

	final static double ZERO = 0.000001;

	final static BasicBorders.ButtonBorder BUTTON_BORDER = new BasicBorders.ButtonBorder(Color.lightGray, Color.white,
			Color.black, Color.gray);

	final static NumberFormat DECIMAL_FORMAT = NumberFormat.getNumberInstance();
	final static DecimalFormat SCIENTIFIC_FORMAT = new DecimalFormat();

	static {
		DECIMAL_FORMAT.setMaximumFractionDigits(6);
		DECIMAL_FORMAT.setMaximumIntegerDigits(6);
		SCIENTIFIC_FORMAT.applyPattern("0.###E00");
	}

	PropertiesPanel(LayoutManager layout) {
		super(layout);
	}

	abstract void windowActivated();

	static void removeListenersForLabel(JLabel b) {
		if (b == null)
			return;
		MouseListener[] ml = b.getMouseListeners();
		if (ml != null) {
			for (MouseListener i : ml)
				b.removeMouseListener(i);
		}
		if (b instanceof HyperlinkLabel) {
			((HyperlinkLabel) b).setAction(null);
		}
	}

	static void removeListenersForAbstractButton(AbstractButton b) {
		if (b == null)
			return;
		b.setAction(null);
		ActionListener[] al = b.getActionListeners();
		if (al != null) {
			for (ActionListener i : al)
				b.removeActionListener(i);
		}
		MouseListener[] ml = b.getMouseListeners();
		if (ml != null) {
			for (MouseListener i : ml)
				b.removeMouseListener(i);
		}
	}

	static void removeListenersForComboBox(JComboBox b) {
		if (b == null)
			return;
		b.setAction(null);
		ActionListener[] al = b.getActionListeners();
		if (al != null) {
			for (ActionListener i : al)
				b.removeActionListener(i);
		}
		ItemListener[] il = b.getItemListeners();
		if (il != null) {
			for (ItemListener i : il)
				b.removeItemListener(i);
		}
	}

	static void removeListenersForTextField(JTextField t) {
		if (t == null)
			return;
		t.setAction(null);
		ActionListener[] al = t.getActionListeners();
		if (al != null) {
			for (ActionListener i : al)
				t.removeActionListener(i);
		}
		MouseListener[] ml = t.getMouseListeners();
		if (ml != null) {
			for (MouseListener i : ml)
				t.removeMouseListener(i);
		}
	}

	static void applyBounds(RealNumberTextField t) {
		double x = t.getValue();
		if (x > t.getMaxValue()) {
			x = t.getMaxValue();
			t.setValue(x);
		}
		else if (x < t.getMinValue()) {
			x = t.getMinValue();
			t.setValue(x);
		}
	}

	static void applyBounds(FloatNumberTextField t) {
		float x = t.getValue();
		if (x > t.getMaxValue()) {
			x = t.getMaxValue();
			t.setValue(x);
		}
		else if (x < t.getMinValue()) {
			x = t.getMinValue();
			t.setValue(x);
		}
	}

	static void setColorComboBox(JComboBox cb, Color c) {
		int k = ColorRectangle.COLORS.length;
		for (int i = 0; i < ColorRectangle.COLORS.length; i++) {
			if (c.equals(ColorRectangle.COLORS[i]))
				k = i;
		}
		if (k == ColorRectangle.COLORS.length) {
			((ColorRectangle) cb.getRenderer()).setMoreColor(c);
		}
		setComboBox(cb, k);
	}

	static void setComboBox(JComboBox cb, int k) {
		ActionListener[] al = cb.getActionListeners();
		if (al != null) {
			for (ActionListener i : al)
				cb.removeActionListener(i);
		}
		ItemListener[] il = cb.getItemListeners();
		if (il != null) {
			for (ItemListener i : il)
				cb.removeItemListener(i);
		}
		cb.setSelectedIndex(k);
		if (al != null) {
			for (ActionListener i : al)
				cb.addActionListener(i);
		}
		if (il != null) {
			for (ItemListener i : il)
				cb.addItemListener(i);
		}
	}

	static JLabel createLabel(int i) {
		JLabel l = new JLabel(Integer.toString(i));
		l.setBorder(BUTTON_BORDER);
		return l;
	}

	static JLabel createLabel(double d) {
		JLabel l = new JLabel(DECIMAL_FORMAT.format(d));
		l.setBorder(BUTTON_BORDER);
		return l;
	}

	static JLabel createLabel2(double d) {
		JLabel l = new JLabel(SCIENTIFIC_FORMAT.format(d));
		l.setBorder(BUTTON_BORDER);
		return l;
	}

	static JLabel createLabel(String s) {
		JLabel l = new JLabel(s);
		l.setBorder(BUTTON_BORDER);
		return l;
	}

	static JLabel createSmallerFontLabel(String s) {
		JLabel l = new JLabel(s);
		Font font = l.getFont();
		l.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 1));
		return l;
	}

	static class ColorListener implements ActionListener {

		private Color color6 = Color.white;
		private Particle p;

		ColorListener(Particle p) {
			this.p = p;
		}

		public void actionPerformed(ActionEvent e) {
			final JComboBox cb = (JComboBox) e.getSource();
			int id = ((Integer) cb.getSelectedItem()).intValue();
			if (id == ColorComboBox.INDEX_COLOR_CHOOSER) {
				JColorChooser.createDialog(p.getHostModel().getView(), "More Colors", true,
						ModelerUtilities.colorChooser, new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								color6 = ModelerUtilities.colorChooser.getColor();
								p.setColor(color6);
								cb.setSelectedIndex(6);
								ColorRectangle cr = (ColorRectangle) cb.getRenderer();
								cr.setMoreColor(color6);
							}
						}, null).setVisible(true);
			}
			else if (id == ColorComboBox.INDEX_HEX_INPUTTER) {
				if (cb instanceof ColorComboBox) {
					final ColorComboBox colorComboBox = (ColorComboBox) cb;
					colorComboBox.updateColor(new Runnable() {
						public void run() {
							p.setColor(colorComboBox.getMoreColor());
						}
					});
				}
			}
			else if (id == ColorComboBox.INDEX_MORE_COLOR) {
				p.setColor(color6);
			}
			else {
				p.setColor(ColorRectangle.COLORS[id]);
			}
			p.getHostModel().getView().repaint();
		}

	}

	static class ChargeColorListener implements ActionListener {

		private Color color6 = Color.white;
		private Particle particle;

		ChargeColorListener(Particle particle) {
			this.particle = particle;
		}

		public void actionPerformed(ActionEvent e) {
			final JComboBox cb = (JComboBox) e.getSource();
			int id = ((Integer) cb.getSelectedItem()).intValue();
			if (id == ColorComboBox.INDEX_COLOR_CHOOSER) {
				JColorChooser.createDialog(particle.getHostModel().getView(), "More Colors", true,
						ModelerUtilities.colorChooser, new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								color6 = ModelerUtilities.colorChooser.getColor();
								particle.setChargeColor(color6);
								cb.setSelectedIndex(ColorComboBox.INDEX_MORE_COLOR);
								ColorRectangle cr = (ColorRectangle) cb.getRenderer();
								cr.setMoreColor(color6);
							}
						}, null).setVisible(true);
			}
			else if (id == ColorComboBox.INDEX_HEX_INPUTTER) {
				if (cb instanceof ColorComboBox) {
					final ColorComboBox colorComboBox = (ColorComboBox) cb;
					colorComboBox.updateColor(new Runnable() {
						public void run() {
							particle.setChargeColor(colorComboBox.getMoreColor());
						}
					});
				}
			}
			else if (id == ColorComboBox.INDEX_MORE_COLOR) {
				particle.setChargeColor(color6);
			}
			else {
				particle.setChargeColor(ColorRectangle.COLORS[id]);
			}
			particle.getHostModel().getView().repaint();
		}

	}

	static void makeCompactGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
		ModelerUtilities.makeCompactGrid(parent, rows, cols, initialX, initialY, xPad, yPad);
	}

}